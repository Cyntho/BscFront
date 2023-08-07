package org.cyntho.bscfront.net

import android.content.Context
import android.util.Log
import androidx.preference.PreferenceManager
import kotlinx.coroutines.*
import org.cyntho.bscfront.exceptions.AuthException
import org.eclipse.paho.mqttv5.client.*
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence
import org.eclipse.paho.mqttv5.common.MqttException
import org.eclipse.paho.mqttv5.common.MqttMessage
import org.eclipse.paho.mqttv5.common.packet.MqttProperties
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeoutException

class KotlinMqtt(private val username: String, private val password: String) {

    private var host: String? = null
    private var clientId: String = "uninitialized_client"

    private val sslPathCA: String = "certs/ca.pem"
    private var client: MqttAsyncClient? = null

    private var running: Boolean = false
    private var online: Boolean = false
    private var listener: Job? = null

    private var context: Context? = null


    /**
     * Attempts to connect the async MQTT client to the server.
     *
     * [host]       needs to be specified in preferences under 'connection_server_ip'
     * [clientId]   needs to be specified in preferences under 'connection_client_id'
     *
     * @param context   usually application context
     * @param callbacks implementation of [MqttCallbackRuntime] that forwards callbacks accordingly
     *
     * @throws AuthException
     */
    fun connect(context: Context,
                callbacks: MqttCallbackRuntime){
        try {

            if (this.context == null){
                this.context = context
            }

            // Don't connect twice
            if (running) return
            running = true

            host = PreferenceManager.getDefaultSharedPreferences(context).getString("connection_server_ip", "") ?: ""
            clientId = PreferenceManager.getDefaultSharedPreferences(context).getString("connection_client_id", "") ?: ""

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

    /**
     * Attempt to disconnect from the server.
     */
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
                Log.w(TAG, "Error: ${any.message}")
            } finally {
                online = false
            }
        }
    }


    /**
     * Handling of ongoing connection to the server.
     *
     * @param client The client used for the connection
     *
     * @throws  AuthException    Thrown when username or password are incorrect
     * @throws  TimeoutException Thrown when the connection timed out
     */
    private fun listen(client: MqttAsyncClient){

        Log.i(TAG, "Connecting..")

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
            val factory = SslUtils.getSingleSocketFactory(File(context!!.filesDir, sslPathCA).inputStream())

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

        online = true
        running = true
    }

    /**
     * Check whether the client is online or not
     */
    fun isOnline(): Boolean { return running && online}


    /**
     * Public getter for the currently used MqttAsyncClient [client]
     */
    fun getClient(): MqttAsyncClient? { return client }

    companion object {

        private const val TAG: String = "KotlinMqtt"

        /**
         * Static method to test the connection without using callbacks
         */
        @OptIn(DelicateCoroutinesApi::class)
        @JvmStatic
        suspend fun testConnection(context: Context, host: String, user: String, pass: String): Boolean {
            val instance = KotlinMqtt(user, pass)
            val callbacks = MqttCallbackRuntime({_ -> }, {_ -> }, {_ -> true}, {_ -> true}, null, null, null)
            var success = false
            val connection = GlobalScope.launch(CoroutineExceptionHandler {_, exception -> Log.w("KotlinMqtt", exception) }) {
                try {
                    instance.host = host
                    instance.connect(context, callbacks)

                    if (instance.isOnline()){
                        Log.i("KotlinMqtt", "Connection successful")
                        success = true
                    } else {
                        Log.w("KotlinMqtt", "Connection failed")
                    }

                } catch (any: Exception){
                    if (any.message == null){
                        throw Exception("Unknown error occurred")
                    } else if (any.message!!.contains("Not authorized")){
                        throw AuthException("Unable to authenticate user [$user]")
                    } else {
                        throw TimeoutException("Unable to connect to the server. It may be offline?")
                    }
                } finally {
                    instance.disconnect()
                }
            }

            connection.join()
            return success
        }
    }


}