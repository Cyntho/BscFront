package org.cyntho.bscfront.net

import android.util.Log
import org.eclipse.paho.mqttv5.client.IMqttToken
import org.eclipse.paho.mqttv5.client.MqttAsyncClient
import org.eclipse.paho.mqttv5.client.MqttCallback
import org.eclipse.paho.mqttv5.client.MqttDisconnectResponse
import org.eclipse.paho.mqttv5.common.MqttException
import org.eclipse.paho.mqttv5.common.MqttMessage
import org.eclipse.paho.mqttv5.common.packet.MqttProperties
import java.util.*

class MqttCallbackRuntime(addCallback: (m: String) -> Unit?,
                          removeCallback: (m: String) -> Unit?,
                          settingsCallback: (m: String) -> Boolean,
                          messagesCallback: (m: String) -> Boolean,
                          clientInstance: MqttAsyncClient) : MqttCallback{


    private val TAG: String = "MqttCallbackRuntime"
    private var initialized: Boolean = false
    private val messageQueue: Queue<String> = LinkedList<String>()

    private val callbackMessageAdd: (m: String) -> Unit? = addCallback
    private val callbackMessageRemove: (m: String) -> Unit? = removeCallback
    private val callbackSettings: (m: String) -> Boolean = settingsCallback
    private val callbackMessages: (m: String) -> Boolean = messagesCallback

    private val client: MqttAsyncClient = clientInstance

    override fun messageArrived(topic: String?, message: MqttMessage?) {
        if (message != null){
            try {
                when (topic){
                    "messages/add" -> {
                        if (initialized){
                            callbackMessageAdd(String(message.payload))
                        } else {
                            messageQueue.add(String(message.payload))
                        }
                    }
                    "messages/remove" -> {
                        if (initialized){
                            callbackMessageRemove(String(message.payload))
                        } else {
                            messageQueue.add(String(message.payload))
                        }
                    }
                    "res/messages" -> {
                        callbackMessages(String(message.payload))
                    }
                    "res/settings" -> {
                        if (initialized){
                            Log.w(TAG, "Already initialized!")
                            client.unsubscribe("res/settings")
                        } else {
                            if (callbackSettings(String(message.payload))){
                                client.unsubscribe("res/settings")

                                // Dequeue all messages already received
                                var entry = messageQueue.poll()
                                while (entry != null){
                                    Log.d(TAG, "Pushing queued message: $entry")
                                    callbackMessageAdd(entry)
                                    entry = messageQueue.poll()
                                }
                                initialized = true
                            }
                        }
                    }
                    else -> {
                        Log.d(TAG, "Received message for topic [${topic}]: [${String(message.payload)}]")
                    }
                }
            } catch (any: Exception){
                any.printStackTrace()
            }
        } else {
            Log.d(TAG, "Received message for topic [${topic}]: [EMPTY]")
        }
    }

    override fun disconnected(disconnectResponse: MqttDisconnectResponse?) {
        Log.i(TAG, "Disconnected: ${disconnectResponse.toString()}")
    }

    override fun mqttErrorOccurred(exception: MqttException?) {
        Log.e(TAG, "Error: ${exception?.message}")
    }

    override fun connectComplete(reconnect: Boolean, serverURI: String?) {
        Log.i(TAG, "Connected to Server at [${serverURI}] with reconnect set to '$reconnect'")
    }

    override fun deliveryComplete(token: IMqttToken?) {}
    override fun authPacketArrived(reasonCode: Int, properties: MqttProperties?) {}
}