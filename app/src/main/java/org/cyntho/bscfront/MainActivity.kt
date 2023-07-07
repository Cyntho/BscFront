package org.cyntho.bscfront

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.RingtoneManager
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.google.android.material.tabs.TabLayout
import androidx.viewpager.widget.ViewPager
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.createViewModelLazy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.preference.PreferenceManager
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import kotlinx.coroutines.runBlocking
import org.cyntho.bscfront.data.LocationWrapper
import org.cyntho.bscfront.data.StatusMessage
import org.cyntho.bscfront.ui.main.SectionsPagerAdapter
import org.cyntho.bscfront.databinding.ActivityMainBinding
import org.cyntho.bscfront.exceptions.AuthException
import org.cyntho.bscfront.net.KotlinMqtt
import org.cyntho.bscfront.ui.main.PlaceholderFragment
import org.cyntho.bscfront.ui.main.SettingsFragment
import java.sql.Time
import java.time.Instant
import kotlin.math.abs

class MainActivity : AppCompatActivity() {

    private val TAG: String = "BscFront/MainActivity2"
    private val CHANNEL_ID: Int = 0


    private lateinit var binding: ActivityMainBinding
    private lateinit var mqtt: KotlinMqtt

    private var menu: Menu? = null
    private val data: MutableCollection<StatusMessage> = mutableListOf()
    private val fragments: MutableMap<Int, PlaceholderFragment> = mutableMapOf()

    private var lastNotification: Long = 0

    private lateinit var sectionsPagerAdapter: SectionsPagerAdapter

    private lateinit var _username: String
    private lateinit var _password: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        _username = intent.getStringExtra(USERNAME) ?: "empty"
        _password = intent.getStringExtra(PASSWORD) ?: "empty"
        println("CREATED MainActivity with: username = $_username, password = $_password")

        println("Called by: ${callingActivity}")

        // Assign default settings
        PreferenceManager.setDefaultValues(this, R.xml.root_preferences, true)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sectionsPagerAdapter = SectionsPagerAdapter(this, supportFragmentManager)
        sectionsPagerAdapter.setActivity(this)

        val viewPager: ViewPager = binding.viewPager2
        viewPager.adapter = sectionsPagerAdapter

        // Create notification channel
        with (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager){
            createNotificationChannel(
                NotificationChannel(
                    "BSC_FRONT_NOTIFICATIONS",
                    "My notification channel",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "App notification"
                }
            )
        }

        val tabs: TabLayout = binding.tabs
        tabs.setupWithViewPager(viewPager)

        //mqtt = KotlinMqtt()
/*
        val frmLogin = binding.frmLogin
        val btnConnect: Button = binding.btnConnect*/
        val parentLayout = findViewById<View>(android.R.id.content)
/*
        btnConnect.setOnClickListener {
            //Snackbar.make(view, "Something happened!", Snackbar.LENGTH_LONG).setAction("Action", null).show()
            //connect(view)

            if (mqtt.isOnline()){
                println("Mqtt Service running.. Attempting to stop it")
                runBlocking {
                    mqtt.disconnect()
                }
            } else {
                println("Attempting to start listener")
                if (binding.txtUsername.text.toString() == "" || binding.txtPassword.text.toString() == ""){
                    Snackbar.make(parentLayout, "Username or Password are empty!", Snackbar.LENGTH_LONG).setAction("CLOSE") {}.show()
                } else {
                    runBlocking {
                        try {
                            mqtt.connect(applicationContext,
                                binding.txtUsername.text.toString(),
                                binding.txtPassword.text.toString(),
                                ::addMessage,
                                ::removeMessage,
                                ::onSettingsReceived,
                                ::onMessagesReceived)
                        } catch (auth: AuthException){
                            println(auth.message)
                            Snackbar.make(parentLayout, "Invalid Username or Password", Snackbar.LENGTH_LONG).setAction("CLOSE") {}.show()
                            return@runBlocking
                        } catch (any: Exception){
                            any.printStackTrace()
                        }
                    }.also {
                        if (mqtt.isOnline()){
                            frmLogin.visibility = View.GONE
                            // Enable logout button
                            menu?.getItem(3)?.setEnabled(true)
                        }
                    }
                }
            }
        }*/
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        //return super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.menu_main, menu)
        this.menu = menu

        // Load settings for notifications
        if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean("switch_preference_notifications_global", true)){
            menu?.getItem(0)?.setIcon(ContextCompat.getDrawable(applicationContext, R.drawable.ic_action_bell_thick_on))
        } else {
            menu?.getItem(0)?.setIcon(ContextCompat.getDrawable(applicationContext, R.drawable.ic_action_bell_thick_off))
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId){
            R.id.menu_bell -> {
                // Toggle the notifications from here
                toggleNotifications(true)
            }
            R.id.menu_settings -> {
                // Open settings


                val intent = Intent(this@MainActivity, SettingsActivity::class.java)
                //val intent = SettingsActivity.newIntent(applicationContext, _username, _password)
                val bundle: Bundle = Bundle().apply {
                    putString(USERNAME, _username)
                    putString(PASSWORD, _password)
                }
                intent.putExtras(bundle)
                startActivity(intent)
                finish()

                // supportFragmentManager.beginTransaction().replace(android.R.id.content, SettingsFragment()).commit()

            }
            R.id.menu_logout -> {
                if (mqtt.isOnline()){
                    runBlocking {
                        mqtt.disconnect()
                    }.also {
                        Snackbar.make(findViewById<View>(android.R.id.content), "You are now offline", Snackbar.LENGTH_LONG).setAction("CLOSE") {}.show()
                    }.also {
                        menu?.getItem(3)?.setEnabled(true)
                    }
                }
            }

            else -> {
                return super.onOptionsItemSelected(item)
            }
        }
        return true
    }

    override fun onResume() {
        super.onResume()
        toggleNotifications(false)
    }

    private fun toggleNotifications(save: Boolean){

        val settings = PreferenceManager.getDefaultSharedPreferences(this)
        val editor = settings.edit()

        when (settings.getBoolean("switch_preference_notifications_global", true)){
            true -> {
                // Used to be turned on, deactivate it now
                editor.putBoolean("switch_preference_notifications_global", false)
                menu?.getItem(0)?.setIcon(ContextCompat.getDrawable(applicationContext, R.drawable.ic_action_bell_thick_off))
            }
            false -> {
                // Used to be turned off, activate it now
                editor.putBoolean("switch_preference_notifications_global", true)
                menu?.getItem(0)?.setIcon(ContextCompat.getDrawable(applicationContext, R.drawable.ic_action_bell_thick_on))
            }
        }
        // Save persistently
        if (save){
            editor.apply()
        }
    }

    private fun addMessage(message: String) : Unit{
        try {
            val wrapper = Gson().fromJson(message.trimIndent(), StatusMessage::class.java)
            data.add(wrapper)
            try {
                notifyUser(wrapper)
            } catch (ex: Exception){
                ex.printStackTrace()
            }

            val f = fragments[wrapper.location]

            if (f != null){
                f.onAddMessage(wrapper)
            } else {
                Log.w(TAG, "Unable to resolve Fragment by location id ${wrapper.location}")
            }
        } catch (any: Exception) {
            Log.e(TAG, "Error parsing message: $message")
            any.printStackTrace()
        }
    }

    private fun removeMessage(message: String) : Unit {
        try {
            val wrapper = Gson().fromJson(message.trimIndent(), StatusMessage::class.java)
            val fragment = fragments[wrapper.location]

            if (fragment != null){
                fragment.onRemoveMessage(wrapper)
            } else {
                Log.w(TAG, "Unable to resolve Fragment by location id ${wrapper.location}")
            }

        } catch (any: Exception){
            Log.e(TAG, "Error parsing message: $message")
            any.printStackTrace()
        }
    }

    private fun onSettingsReceived(settings: String): Boolean {
        try {
            val wrapper: List<LocationWrapper> = Gson().fromJson(settings, Array<LocationWrapper>::class.java).toList()

            runOnUiThread {
                // Initialize components
                val spAdapter = SectionsPagerAdapter(this, supportFragmentManager)
                val pager: ViewPager = binding.viewPager2
                val tabs: TabLayout = binding.tabs

                spAdapter.setActivity(this)
                val names = mutableListOf<String>()

                for (page in wrapper){
                    names.add(page.id!!, page.name!!)
                }
                spAdapter.setTitles(names.toTypedArray())
                spAdapter.setCounter(names.size)

                pager.adapter = spAdapter
                pager.offscreenPageLimit = wrapper.size + 1

                tabs.setupWithViewPager(pager, true)

                Log.i(TAG, "Setup TabLayout for ${wrapper.size} Locations: ${names.toString()}")
            }.also {
                return true
            }

        } catch (any:Exception){
            Log.e(TAG, "Error parsing settings: $settings")
            any.printStackTrace()
        }
        return false
    }

    private fun onMessagesReceived(messages: String) : Boolean {

        try {
            val entry: StatusMessage = Gson().fromJson(messages, StatusMessage::class.java)

            val f = fragments[entry.location]
            if (f == null){
                Log.e(TAG, "Unable to resolve fragment by location id ${entry.location}")
                return false
            }
            println("Added message with id ${entry.id} after the fact")

            f.onAddMessage(entry)

            return true
        } catch (any:Exception){
            Log.e(TAG, "Error parsing message list: $messages")
            any.printStackTrace()
        }

        return false
    }


    fun addFragment(fragment: PlaceholderFragment, id: Int){
        fragments[id] = fragment
    }

    fun removeFragment(id: Int){
        fragments.remove(id)
    }

    @SuppressLint("ServiceCast")
    private fun notifyUser(message: StatusMessage){

        // Only notify user if app is in background
        if (ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)){
            return
        }

        // Send push notifications at least 10 seconds apart to avoid spam
        val currentTime = Time.from(Instant.now()).time
        val diff = abs(currentTime - lastNotification)
        if (diff > 10 * 1000){
            lastNotification = currentTime
        } else {
            Log.d(TAG,"Skipping notification because last one was only ${diff / 1000} seconds ago")
            return
        }

        val builder = NotificationCompat.Builder(this, "BSC_FRONT_NOTIFICATIONS")
            .setVibrate(longArrayOf(1000, 1000, 1000))
            .setLights(Color.RED, 3000, 3000)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setSmallIcon(R.drawable.ic_action_bell_thick_on)
            .setContentTitle(message.status.toString())
            .setContentText(message.message)
            .setOnlyAlertOnce(true)

        // Check for permissions
        with (NotificationManagerCompat.from(this)){
            if (ActivityCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(this@MainActivity, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 0)
                return
            }
            notify(System.currentTimeMillis().toInt(), builder.build())
        }
    }

    companion object {
        private const val USERNAME = "USERNAME"
        private const val PASSWORD = "PASSWORD"

        @JvmStatic
        fun newIntent(context: Context, username: String, password: String): Intent {
            val intent = Intent(context, MainActivity::class.java)
            intent.putExtra(USERNAME, username)
            intent.putExtra(PASSWORD, password)
            return intent
        }
    }


}