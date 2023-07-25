package org.cyntho.bscfront

import android.os.Bundle
import android.util.Log
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import org.cyntho.bscfront.databinding.ActivityLoginBinding
import org.cyntho.bscfront.exceptions.AuthException
import org.cyntho.bscfront.net.KotlinMqtt
import org.cyntho.bscfront.net.MqttCallbackRuntime
import java.util.concurrent.TimeoutException


/**
 * Activity that handles the login and forwards the user to the MainActivity on success
 */
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private var connect: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val btnLogin = binding.btnConnect

        btnLogin.setOnClickListener {
            clicked()
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun clicked(){
        val mqtt = KotlinMqtt(binding.txtUsername.text.toString(), binding.txtPassword.text.toString())
        val callbacks = MqttCallbackRuntime({_ -> }, {_ -> }, {_ -> true}, {_ -> true}, null, null, null)

        connect = GlobalScope.launch(CoroutineExceptionHandler {_, exception -> Log.w(TAG, exception) }) {
            try {
                mqtt.connect(applicationContext, callbacks)
            } catch (ex: AuthException){
                Snackbar.make(binding.viewPager, getString(R.string.msg_err_connection_auth), Snackbar.LENGTH_LONG).setAction("CLOSE") {}.show()
            } catch (timeout: TimeoutException) {
                Snackbar.make(binding.viewPager, getString(R.string.msg_err_connection_failed), Snackbar.LENGTH_LONG).setAction("CLOSE") {}.show()
            }

            if (mqtt.isOnline()){
                Log.d(TAG, "Connection successful")
                mqtt.disconnect()

                val intent = MainActivity.newIntent(applicationContext, binding.txtUsername.text.toString(), binding.txtPassword.text.toString())
                startActivity(intent)
                finish()
            }

        }
    }

    companion object {
        private const val TAG = "LoginActivity"
    }

}