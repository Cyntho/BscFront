package org.cyntho.bscfront

import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Toast
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import androidx.viewpager.widget.ViewPager
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import kotlinx.coroutines.*
import org.cyntho.bscfront.databinding.ActivityLoginBinding
import org.cyntho.bscfront.exceptions.AuthException
import org.cyntho.bscfront.net.KotlinMqtt
import org.cyntho.bscfront.ui.main.SectionsPagerAdapter
import java.util.concurrent.TimeoutException

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private var connect: Job? = null
    private val TAG: String = "LoginActivity"

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
        connect = GlobalScope.launch(CoroutineExceptionHandler {_, exception -> Log.w(TAG, exception) }) {
            try {
                mqtt.connect(applicationContext, ::callback, ::callback, ::boolCB, ::boolCB)
            } catch (ex: AuthException){
                Snackbar.make(binding.viewPager, "Username or password wrong", Snackbar.LENGTH_LONG).setAction("CLOSE") {}.show()
            } catch (timeout: TimeoutException) {
                Snackbar.make(binding.viewPager, "Unable to connect to the server", Snackbar.LENGTH_LONG).setAction("CLOSE") {}.show()
            }

            if (mqtt.isOnline()) {
                println("We are online!")
                mqtt.disconnect()

                val editor = PreferenceManager.getDefaultSharedPreferences(applicationContext).edit()
                editor.putString("runtime_username", binding.txtUsername.text.toString())
                editor.putString("runtime_password", binding.txtPassword.text.toString())
                editor.apply()

                val intent = MainActivity.newIntent(applicationContext, binding.txtUsername.text.toString(), binding.txtPassword.text.toString())
                startActivity(intent)
                finish()
            }
        }
    }

    private fun callback(m: String) : Unit {
        println(m)
    }

    private fun boolCB(m: String) : Boolean { return true }



}