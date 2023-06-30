package org.cyntho.bscfront.net

import android.content.Context
import kotlinx.coroutines.*
import org.cyntho.bscfront.R
import org.eclipse.paho.mqttv5.client.*
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence
import org.eclipse.paho.mqttv5.common.MqttException
import org.eclipse.paho.mqttv5.common.MqttMessage
import org.eclipse.paho.mqttv5.common.packet.MqttProperties
import java.io.File
import java.io.IOException

class KotlinMqtt() {

    private val host: String = "ssl://10.66.66.1"
    private val port: Int = 8883

    private val username: String = "emqx-phone-001"
    private val password: String = "32tz7u8mM"

    private val sslCA: String = "certs/ca.pem"
    private val sslPrivate: String = "certs/phone-001.key"
    private val sslPublic: String = "certs/phone-001.pem"


    private val clientId: String = "phone-01"

    private var client: MqttAsyncClient = MqttAsyncClient(host, clientId, MemoryPersistence())

    private var running: Boolean = false
    private var listener: Job? = null

    lateinit var adder: (m: String) -> Unit?
    private  var context: Context? = null

    fun isOnline(): Boolean { return running }

    fun connect(c: Context, f: (m: String) -> Unit?){
        try {

            if (context == null){
                context = c
            }

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



    private fun listen(client: MqttAsyncClient){

        println("Connecting..")

        val options = MqttConnectionOptions()
        options.isCleanStart = true

        // Setup credentials
        options.userName = username
        options.password = password.toByteArray()

        // Assign encryption
        // Here: Disable hostname verification since we're using a self-signed certificate
        options.isHttpsHostnameVerificationEnabled = false

        try {
            /*val factory = SslUtils.getSocketFactory(
                File(context!!.filesDir, sslCA).inputStream(),
                File(context!!.filesDir, sslPublic).inputStream(),
                File(context!!.filesDir, sslPrivate).inputStream(),
                ""
            )*/
            /*val caFile = context!!.resources.openRawResource(R.raw.ca)
            val crtFile = context!!.resources.openRawResource(R.raw.phone_crt)
            val keyFile = context!!.resources.openRawResource(R.raw.phone_pem)

            val factory = SslUtils.getSocketFactory(caFile, crtFile, keyFile, "")*/

            val factory = SslUtils.getSingleSocketFactory(File(context!!.filesDir, sslCA).inputStream())

            options.socketFactory = factory

            println("SSL Setup complete")

        } catch (io: IOException){
            io.printStackTrace()
        }



        runBlocking {

            val token = client.connect(options)
            token.waitForCompletion()

            client.subscribe("mqtt/test", 1)
            println("Subscribed to mqtt/test!")
        }
    }

}