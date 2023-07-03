package org.cyntho.bscfront.net

import android.content.Context
import android.util.Log
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.*
import org.eclipse.paho.mqttv5.client.*
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence
import org.eclipse.paho.mqttv5.common.MqttException
import org.eclipse.paho.mqttv5.common.MqttMessage
import org.eclipse.paho.mqttv5.common.packet.MqttProperties
import java.io.File
import java.io.IOException
import java.util.*

class KotlinMqtt(tabs: TabLayout) {

    private val TAG: String = "KotlinMqtt"

    private val host: String = "ssl://10.66.66.1"

    private val username: String = "emqx-phone-001"
    private val password: String = "32tz7u8mM"

    private val sslCA: String = "certs/ca.pem"
    private val clientId: String = "phone-01"

    private var client: MqttAsyncClient? = null

    private var running: Boolean = false
    private var listener: Job? = null

    private lateinit var mqttCallback: MqttCallbackRuntime

    private var context: Context? = null

    fun isOnline(): Boolean { return running }

    fun connect(context: Context,
                funAdd: (m: String) -> Unit?,
                funRemove: (m: String) -> Unit?,
                funSettings: (m: String) -> Boolean,
                funMessages: (m: String) -> Boolean){
        try {

            if (this.context == null){
                this.context = context
            }

            // Don't connect twice
            if (running) return
            running = true

            // Assign callbacks
            client = MqttAsyncClient(host, clientId, MemoryPersistence())
            mqttCallback = MqttCallbackRuntime(funAdd, funRemove, funSettings, funMessages, client!!)

            client!!.setCallback(mqttCallback)

            runBlocking {
                listener = launch {
                    listen(client!!)
                }

                listener!!.start()
            }

        } catch (mqttException: MqttException) {
            mqttException.printStackTrace()
        } catch (any: Exception){
            any.printStackTrace()
        }
    }

    private suspend fun initialize(client: MqttAsyncClient){

        client.subscribe("res/settings", 1)
        client.subscribe("res/messages", 1)

        client.publish("req/settings", MqttMessage(clientId.toByteArray(),1, false, MqttProperties()))
        client.publish("req/messages", MqttMessage(clientId.toByteArray(), 1, false, MqttProperties()))

        yield()
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
            }
        }
    }



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
        }

        runBlocking {
            val token = client.connect(options)
            Log.d(TAG, "Awaiting connection")
            token.waitForCompletion()

            val setup = async { initialize(client) }
            setup.await()

            Log.d(TAG, "Everything is setup. Waiting for messages..")

            client.subscribe("messages/add", 1)
            client.subscribe("messages/remove", 1)



        }
    }

    private fun test(m: String) : Boolean {
        return true
    }

}