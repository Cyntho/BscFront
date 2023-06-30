package org.cyntho.bscfront.net

import android.content.Context
import android.util.Log
import androidx.core.content.ContentProviderCompat.requireContext
import kotlinx.coroutines.*
import org.eclipse.paho.mqttv5.client.*
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence
import org.eclipse.paho.mqttv5.common.MqttException
import org.eclipse.paho.mqttv5.common.MqttMessage
import org.eclipse.paho.mqttv5.common.packet.MqttProperties
import java.io.File
import kotlin.coroutines.coroutineContext

class KotlinMqtt() {

    private val host: String = "tcp://10.66.66.1"
    private val port: Int = 1883

    private val username: String = "emqx-phone-001"
    private val password: String = "32tz7u8mM"

    private val clientId: String = "phone-01"

    private var client: MqttAsyncClient = MqttAsyncClient(host, clientId, MemoryPersistence())

    private var running: Boolean = false
    private var listener: Job? = null

    lateinit var adder: (m: String) -> Unit?

    fun isOnline(): Boolean { return running }

    fun connect(f: (m: String) -> Unit?){
        try {

            if (running) return
            running = true
            adder = f

            println("Currently not listening. Starting now..")

            client.setCallback(object : MqttCallback {
                override fun disconnected(disconnectResponse: MqttDisconnectResponse?) {
                    println("Disconnected: ${disconnectResponse.toString()}")
                }

                override fun mqttErrorOccurred(exception: MqttException?) {
                    println("Error: ${exception?.message}")
                }

                override fun messageArrived(topic: String?, message: MqttMessage?) {
                    if (message != null){
                        println("Received message for topic [${topic}]: [${String(message.payload)}]")
                        try {
                            adder(String(message.payload))
                        } catch (any: Exception){
                            any.printStackTrace()
                            println("Something went wrong")
                        }
                    } else {
                        println("Received message for topic [${topic}]: [EMPTY]")
                    }
                }

                override fun deliveryComplete(token: IMqttToken?) {
                    TODO("Not yet implemented")
                }

                override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                    println("Connected to Server at [${serverURI}] with reconnect set to '$reconnect'")
                }

                override fun authPacketArrived(reasonCode: Int, properties: MqttProperties?) {
                    TODO("Not yet implemented")
                }
            })

            runBlocking {
                listener = launch {
                    listen(client)
                }

                listener!!.start()
            }

        } catch (mqttException: MqttException) {
            mqttException.printStackTrace()
        } catch (any: Exception){
            any.printStackTrace()
        }
    }

    suspend fun disconnect(){
        if (running){
            running = false
        }

        if (listener != null){
            try {
                listener!!.join()
                client.close()
            } catch (any: Exception){
                println("Error: ${any.message}")
            }
        }
    }

    public fun readCert(c: Context){
        val path = File(c.filesDir, "certs/ca.pem")
        val data = path.bufferedReader().readLine()

        Log.d("KotlinMqtt", "Found ${data.length} lines")
    }

    private fun listen(client: MqttAsyncClient){

        println("Connecting..")

        val options = MqttConnectionOptions()
        options.isCleanStart = true
        options.userName = username
        options.password = password.toByteArray()

        runBlocking {

            val token = client.connect(options)
            token.waitForCompletion()

            client.subscribe("mqtt/test", 2)
            println("Subscribed to mqtt/test!")
        }
    }
}