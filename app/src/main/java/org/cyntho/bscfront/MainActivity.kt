package org.cyntho.bscfront

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.google.android.material.tabs.TabLayout
import androidx.viewpager.widget.ViewPager
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.registerForActivityResult
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.preference.PreferenceManager
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import kotlinx.coroutines.*
import org.cyntho.bscfront.data.LocationWrapper
import org.cyntho.bscfront.data.StatusMessage
import org.cyntho.bscfront.ui.main.SectionsPagerAdapter
import org.cyntho.bscfront.databinding.ActivityMainBinding
import org.cyntho.bscfront.net.KotlinMqtt
import org.cyntho.bscfront.net.MqttCallbackRuntime
import org.cyntho.bscfront.ui.main.PlaceholderFragment
import org.eclipse.paho.mqttv5.client.IMqttToken
import org.eclipse.paho.mqttv5.client.MqttDisconnectResponse
import org.eclipse.paho.mqttv5.common.MqttException
import java.security.MessageDigest
import java.sql.Time
import java.time.Instant
import java.util.Base64
import java.util.concurrent.TimeoutException
import kotlin.math.abs

class MainActivity : AppCompatActivity() {

    private val TAG: String = "BscFront/MainActivity"
    private var menu: Menu? = null

    private val data: MutableCollection<StatusMessage> = mutableListOf()
    private val fragments: MutableMap<Int, PlaceholderFragment> = mutableMapOf()
    private var lastNotification: Long = 0

    private var reconnectMaxAttempts = 5
    private var reconnectTimeout = 1000L
    private var reconnectAttempts = 0

    private lateinit var binding: ActivityMainBinding
    private lateinit var mqtt: KotlinMqtt
    private lateinit var callbacks: MqttCallbackRuntime

    private lateinit var sectionsPagerAdapter: SectionsPagerAdapter
    private lateinit var locations: List<String>

    private lateinit var username: String
    private lateinit var password: String

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        username = intent.getStringExtra(USERNAME) ?: "empty"
        password = intent.getStringExtra(PASSWORD) ?: "empty"
        locations = mutableListOf()

        // Assign default settings
        PreferenceManager.setDefaultValues(this, R.xml.root_preferences, true)

        // Build UI
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

        // Setup empty taps
        val tabs: TabLayout = binding.tabs
        tabs.setupWithViewPager(viewPager)

        // Initialize connection
        mqtt = KotlinMqtt(username, password)
        callbacks = MqttCallbackRuntime(::addMessage,
                                            ::removeMessage,
                                            ::onSettingsReceived,
                                            ::onMessagesReceived,
                                            ::onDisconnect,
                                            ::onErrorOccurred,
                                            mqtt.getClient())
        try {
            mqtt.connect(context = applicationContext, callbacks)
        } catch (any: Exception){
            println(any.message)
        }

        // Since onBackPressed() is deprecated:
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                Log.d(TAG, "Skipping 'onBackPressed()' to avoid going back to Login")
            }
        })
    }

    /**
     * Create the menu and load saved preferences for notifications to display the bell icon accordingly
     */
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        // Create the menu in accordance with '/res/menu/menu_main.xml'
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


    /**
     * Change the view to 'SettingsActivity' while keeping the mqtt connection
     */
    private fun startSettings(){
        val i = Intent(this@MainActivity, SettingsActivity::class.java)
        val b = Bundle().apply {
            putString(USERNAME, username)
            putString(PASSWORD, password)
            putStringArray("LOCATIONS", locations.toTypedArray())
        }
        i.putExtras(b)

        // Since startActivityForResult is deprecated, this seems to be the way to go
        resultLauncher.launch(i)
    }
    private var resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
        if (it.resultCode == Activity.RESULT_OK){
            val data: Intent? = it.data
            println("resultLauncher sagt Feierabend")
        }
    }


    /**
     * Handle click on menu items
     *
     * Bell     -> Toggle notification status
     * Logout   -> Terminate connection and return to login screen
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId){
            R.id.menu_bell -> {
                // Toggle the notifications from here
                toggleNotifications()
            }
            R.id.menu_settings -> {
                // Open settings
                startSettings()
            }
            R.id.menu_logout -> {
                // Logout and return to Login screen
                if (mqtt.isOnline()){
                    runBlocking {
                        mqtt.disconnect()
                    }.also {
                        val i = Intent(applicationContext, LoginActivity::class.java)
                        startActivity(i)
                        finish()
                    }
                }
            }

            else -> {
                return super.onOptionsItemSelected(item)
            }
        }
        return true
    }

    /**
     * Gets called when the application returns from being minimized
     * but also whenever other activities (e.g. settings) are being closed.
     *
     * In case notification status changed, update icon accordingly
     */
    override fun onResume() {
        super.onResume()
        updateNotificationIcon()
    }


    /**
     * Whenever this activity is destroyed, terminate the connection
     */
    override fun onDestroy() {
        if (mqtt.isOnline()){
            runBlocking {
                mqtt.disconnect()
            }
        }

        super.onDestroy()
    }


    /**
     * Update the notification icon (bell) based on the saved preferences
     */
    private fun updateNotificationIcon(){
        val settings = PreferenceManager.getDefaultSharedPreferences(this)
        if (settings.getBoolean("switch_preference_notifications_global", true)){
            menu?.getItem(0)?.setIcon(ContextCompat.getDrawable(applicationContext, R.drawable.ic_action_bell_thick_on))
        } else {
            menu?.getItem(0)?.setIcon(ContextCompat.getDrawable(applicationContext, R.drawable.ic_action_bell_thick_off))
        }
    }


    /**
     * Toggle the notifications as name implies (on --> off, off -> on)
     */
    private fun toggleNotifications(){

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
        editor.apply()

        // Update icon
        updateNotificationIcon()
    }


    /**
     * Callback for adding messages [org.cyntho.bscfront.net.MqttCallbackRuntime]
     *
     * Unwraps received json to @see[StatusMessage] and forwards it to the corresponding fragment
     * [PlaceholderFragment.onAddMessage]
     */
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
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

    /**
     * Callback for removing messages [org.cyntho.bscfront.net.MqttCallbackRuntime]
     *
     * Unwraps received json to [StatusMessage] and forwards it to the corresponding fragment
     * [PlaceholderFragment.onRemoveMessage]
     */
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

    /**
     * Callback for receiving settings [org.cyntho.bscfront.net.MqttCallbackRuntime]
     *
     * Unwraps received json to [List] of [LocationWrapper] builds the [TabLayout] accordingly
     *
     * Locations received for the first time get hashed ([MainActivity.hash]) as 'username@location'
     * and saved to default preferences to be able to set notifications per location
     */
    private fun onSettingsReceived(settings: String): Boolean {
        try {
            val wrapper: List<LocationWrapper> = Gson().fromJson(settings, Array<LocationWrapper>::class.java).toList()

            runOnUiThread {
                // Initialize components
                val spAdapter = SectionsPagerAdapter(this, supportFragmentManager)
                val pager: ViewPager = binding.viewPager2
                val tabs: TabLayout = binding.tabs

                // Prepare
                spAdapter.setActivity(this)
                val names = mutableListOf<String>()
                val manager = PreferenceManager.getDefaultSharedPreferences(applicationContext)
                val editor = manager.edit()

                // Iterate over received locations and add them
                for (page in wrapper){
                    names.add(page.id!!, page.name!!)

                    // Generate hash of 'username@location' for settings
                    val h = hash("$username@" + page.name!!)
                    if (!manager.contains(h)){
                        editor.putBoolean(h, true)
                    }
                }
                editor.apply()
                spAdapter.setTitles(names.toTypedArray())
                spAdapter.setCounter(names.size)
                locations = names

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


    /**
     * Callback for receiving settings [org.cyntho.bscfront.net.MqttCallbackRuntime]
     *
     * Unwraps received json to [List] of [StatusMessage] and adds them to the corresponding fragment.
     * This is used for when the application starts and there may be 'old' messages that where not received
     * during downtime.
     */
    private fun onMessagesReceived(messages: String) : Boolean {

        try {
            val entry: StatusMessage = Gson().fromJson(messages, StatusMessage::class.java)

            val f: PlaceholderFragment? = fragments[entry.location] as PlaceholderFragment?
            if (f == null){
                Log.e(TAG, "Unable to resolve fragment by location id ${entry.location}")
                return false
            }
            f.onAddMessage(entry)
            return true

        } catch (any:Exception){
            Log.e(TAG, "Error parsing message list: $messages")
            any.printStackTrace()
        }

        return false
    }

    /**
     * Callback for disconnects in [org.cyntho.bscfront.net.MqttCallbackRuntime]
     *
     * Attempt to reconnect [reconnectMaxAttempts] times with a timeout of [reconnectTimeout]
     * The current [reconnectAttempts] are being reset to 0 on success. If the attempt to reconnect
     * fails to often, the user is being forwarded back to the login screen.
     */
    @OptIn(DelicateCoroutinesApi::class)
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun onDisconnect(response: MqttDisconnectResponse?) {

        Log.w(TAG, "::onDisconnect received ${response.toString()}")

        // Check if the maximum number of attempts is reached
        if (reconnectAttempts++ == reconnectMaxAttempts){

            Log.d(TAG, "reconnectAttempts = $reconnectAttempts")

            // Notify user that the connection has been lost
            // This happens regardless of notification settings, since it is important no notice it.
            val builder = NotificationCompat.Builder(this, "BSC_FRONT_NOTIFICATIONS")
                .setLights(Color.RED, 3000, 3000)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setSmallIcon(R.drawable.ic_action_bell_thick_on)
                .setContentTitle(getString(R.string.msg_err_connection_failed))
                .setContentText(getString(R.string.msg_err_connection_lost_long))
                .setOnlyAlertOnce(true)
                .setVibrate(longArrayOf(1000, 1000, 1000, 1000, 1000))
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))

            // Still have to check permissions though
            requestNotification(builder)

            // Forward to Login screen and finish this activity
            startActivity(Intent(applicationContext, LoginActivity::class.java))
            finish()
            return
        } else {
            GlobalScope.launch { reconnect() }
        }
    }


    @OptIn(DelicateCoroutinesApi::class)
    private suspend fun reconnect(){
        // Attempt to reconnect
        GlobalScope.launch(CoroutineExceptionHandler {_, exception -> Log.w(TAG, exception) }) {
            try {
                mqtt.disconnect()
                Log.d(TAG, "Attempting to reconnect.. ")
                mqtt.connect(applicationContext, callbacks)
            } catch (timeout: TimeoutException){
                Log.d(TAG, "Caught error: ${timeout.message}")
                return@launch
            } catch (any: Exception){
                // [any] includes AuthException here, although it SHOULD not occur
                println("Received unknown exception:")
                any.printStackTrace()
            }
        }

        // Connection attempt was successful
        if (mqtt.isOnline()){
            Log.d(TAG, "Reconnect successful after $reconnectAttempts attempts")
            reconnectAttempts = 0
        } else {
            reconnectTimeout = ((reconnectTimeout * 1.2).toLong())
            println("Reconnect failed. Waiting [$reconnectAttempts / $reconnectMaxAttempts]${reconnectTimeout / 1000} seconds")
            runBlocking {
                Thread.sleep(reconnectTimeout)
                reconnect()
            }
        }
    }

    private fun onErrorOccurred(exception: MqttException?){
        println("onErrorOccurred: ${exception?.message}")
    }


    /**
     * Used by [PlaceholderFragment] to add itself to local list of [fragments]
     */
    fun addFragment(fragment: PlaceholderFragment, id: Int){
        fragments[id] = fragment
    }


    /**
     * Used by [PlaceholderFragment] to remove itself from local list of [fragments]
     */
    fun removeFragment(id: Int){
        fragments.remove(id)
    }

    /**
     * Notifies the user with a push notification of an incoming message
     * it may be deactivated in [SettingsActivity] or restricted
     */
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @SuppressLint("ServiceCast")
    private fun notifyUser(message: StatusMessage){

        // cache
        val preferences = PreferenceManager.getDefaultSharedPreferences(this)

        // Only notify user if app is in background
        if (ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)){
            Log.d(TAG, "MainActivity::notifyUser --> not in background")
            return
        }

        // Check if global notifications are turned off
        if (!preferences.getBoolean("switch_preference_notifications_global", false)){
            Log.d(TAG, "MainActivity::notifyUser --> globally deactivated")
            return
        }

        // Check if notifications for this specific location are turned off
        assert(locations.isNotEmpty())

        val locationName = locations[message.location ?: 0]
        val locationHash = hash("$username@$locationName")
        if (!preferences.getBoolean(locationHash, false)){
            Log.d(TAG, "MainActivity::notifyUser --> locally deactivated for $username@$locationName = $locationHash")
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
            .setLights(Color.RED, 3000, 3000)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setSmallIcon(R.drawable.ic_action_bell_thick_on)
            .setContentTitle("${message.sps}${message.group}${message.device}${message.part}")
            .setContentText(message.message)
            .setOnlyAlertOnce(true)


        // Should it vibrate?
        if (preferences.getBoolean("switch_preference_notifications_vibrate", false)){
            builder.setVibrate(longArrayOf(1000, 1000, 1000))
        }

        // Should it include a sound?
        if (preferences.getBoolean("switch_preference_notifications_sound", false)){
            builder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
        }

        // Check for permissions
        requestNotification(builder)
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun requestNotification(builder: NotificationCompat.Builder){
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

        @JvmStatic
        fun hash(message: String) : String {
            return Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-256").digest(message.toByteArray()))
        }
    }


}