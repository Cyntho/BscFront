package org.cyntho.bscfront.net

import android.content.Context
import android.util.Log
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.*
import org.cyntho.bscfront.exceptions.AuthException
import org.eclipse.paho.mqttv5.client.*
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence
import org.eclipse.paho.mqttv5.common.MqttException
import org.eclipse.paho.mqttv5.common.MqttMessage
import org.eclipse.paho.mqttv5.common.packet.MqttProperties
import java.io.File
import java.io.IOException
import java.util.*
import java.util.concurrent.TimeoutException

class KotlinMqtt(user: String, pass: String) {

    private val TAG: String = "KotlinMqtt"

    private var host: String = "ssl://10.66.66.1"
    private val username: String = user
    private val password: String = pass

    private val sslCA: String = "certs/ca.pem"
    private val clientId: String = "phone-01"

    private var client: MqttAsyncClient? = null

    private var running: Boolean = false
    private var online: Boolean = false
    private var listener: Job? = null

    private var context: Context? = null

    fun isOnline(): Boolean { return running && online}

    fun connect(context: Context,
                callbacks: MqttCallbackRuntime){
        try {

            if (this.context == null){
                this.context = context
            }

            // Don't connect twice
            if (running) return
            running = true


            // Assign callbacks
            client = MqttAsyncClient(host, clientId, MemoryPersistence())
            client!!.setCallback(callbacks)

            runBlocking {
                listener = launch {
                    listen(client!!)
                }
                listener!!.start()
            }

        } catch (mqttException: MqttException) {
            mqttException.printStackTrace()
        }catch (auth: AuthException){
            throw auth
        }
    }

    suspend fun disconnect(){
        if (running){
            running = false
        }

        if (listener != null && client != null){
            try {
                listener!!.join()
                client!!.disconnect()
                client!!.close()

                Log.i(TAG, "Disconnected form the Server.")
            } catch (any: Exception){
                println("Error: ${any.message}")
            } finally {
                online = false
            }
        }
    }

    public fun getClient(): MqttAsyncClient? { return client }

    private fun listen(client: MqttAsyncClient){

        println("Connecting..")

        val options = MqttConnectionOptions()
        options.isCleanStart = false

        // Setup credentials
        options.userName = username
        options.password = password.toByteArray()

        // Assign encryption
        // Disable hostname verification since we're using a self-signed certificate
        // ToDo: Change this to 'true' for a production environment
        options.isHttpsHostnameVerificationEnabled = false


        // Setup SSL
        try {
            // ToDo: Change this to use a two-way certificate exchange.
            // Unable to do so for this project since my ssl-certificates are self-signed
            val factory = SslUtils.getSingleSocketFactory(File(context!!.filesDir, sslCA).inputStream())

            options.socketFactory = factory
            Log.d(TAG, "SSL factory setup")

        } catch (io: IOException){
            io.printStackTrace()
            running = false
        }

        val token = client.connect(options)
        Log.d(TAG, "Awaiting connection")
        try {
            token.waitForCompletion(1000)

        } catch (any: Exception){
            Log.w(TAG,"Failed to connect to the server! $any")

            running = false
            online = false

            if (any.message == null){
                throw Exception("Unknown error occurred")
            } else if (any.message!!.contains("Not authorized")){
                throw AuthException("Unable to authenticate user [$username]")
            } else {
                throw TimeoutException("Unable to connect to the server. It may be offline?")
            }
        }

        client.subscribe("res/settings", 2)
        client.subscribe("res/messages", 2)

        client.publish("req/settings", MqttMessage(clientId.toByteArray(),2, false, MqttProperties()))
        client.publish("req/messages", MqttMessage(clientId.toByteArray(), 2, false, MqttProperties()))

        Log.d(TAG, "Everything is setup. Waiting for messages..")

        client.subscribe("messages/add", 2)
        client.subscribe("messages/remove", 2)


        println("Granted QoS: ${token.grantedQos.toList().toString()}")

        online = true
        running = true
    }


    companion object {
        @OptIn(DelicateCoroutinesApi::class)
        suspend fun testConnection(context: Context, host: String, user: String, pass: String): Boolean {
            val instance = KotlinMqtt(user, pass)
            val callbacks = MqttCallbackRuntime({_ -> Unit}, {_ -> Unit}, {_ -> true}, {_ -> true}, null, null, null)
            var success = false
            val connection = GlobalScope.launch(CoroutineExceptionHandler {_, exception -> Log.w("KotlinMqtt", exception) }) {
                try {
                    instance.host = host
                    instance.connect(context, callbacks)
                    println("connected: ${instance.isOnline()}")
                } catch (any: Exception){
                    if (any.message == null){
                        throw Exception("Unknown error occurred")
                    } else if (any.message!!.contains("Not authorized")){
                        throw AuthException("Unable to authenticate user [$user]")
                    } else {
                        throw TimeoutException("Unable to connect to the server. It may be offline?")
                    }
                }
                if (instance.isOnline()){
                    instance.disconnect()
                    success = true
                }
            }

            connection.join()
            return success
        }
    }


}