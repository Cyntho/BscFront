package org.cyntho.bscfront

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.google.android.material.tabs.TabLayout
import androidx.viewpager.widget.ViewPager
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import com.google.gson.Gson
import kotlinx.coroutines.runBlocking
import org.cyntho.bscfront.data.LocationWrapper
import org.cyntho.bscfront.data.StatusMessage
import org.cyntho.bscfront.ui.main.SectionsPagerAdapter
import org.cyntho.bscfront.databinding.ActivityMain2Binding
import org.cyntho.bscfront.net.KotlinMqtt
import org.cyntho.bscfront.ui.main.PlaceholderFragment
import org.cyntho.bscfront.ui.main.SettingsFragment

class MainActivity2 : AppCompatActivity() {

    private val TAG: String = "BscFront/MainActivity2"

    private lateinit var binding: ActivityMain2Binding
    private lateinit var mqtt: KotlinMqtt

    private var menu: Menu? = null
    private val data: MutableCollection<StatusMessage> = mutableListOf()
    private val fragments: MutableMap<Int, PlaceholderFragment> = mutableMapOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Assign default settings
        PreferenceManager.setDefaultValues(this, R.xml.root_preferences, false)

        binding = ActivityMain2Binding.inflate(layoutInflater)
        setContentView(binding.root)

        val sectionsPagerAdapter = SectionsPagerAdapter(this, supportFragmentManager)
        sectionsPagerAdapter.setActivity(this)

        val viewPager: ViewPager = binding.viewPager2
        viewPager.adapter = sectionsPagerAdapter

        // Increasing the offscreenPageLimit prevents the adapter from creating/deleting pages at runtime
        // This forces it to load them at startup and keep them in ram (default: 1)

        viewPager.offscreenPageLimit = 10

        val tabs: TabLayout = binding.tabs
        tabs.setupWithViewPager(viewPager)

        mqtt = KotlinMqtt(tabs)

        val frmLogin = binding.frmLogin


        val btnConnect: Button = binding.btnConnect
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
                runBlocking {
                    mqtt.connect(applicationContext,
                        ::addMessage,
                        ::removeMessage,
                        ::onSettingsReceived,
                        ::onMessagesReceived)
                }.also {
                    frmLogin.visibility = View.GONE
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        //return super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.menu_main, menu)
        this.menu = menu

        // Load settings for notifications
        if (getPreferences(Context.MODE_PRIVATE).getBoolean("globalNotifications", true)){
            menu?.getItem(0)?.setIcon(ContextCompat.getDrawable(applicationContext, R.drawable.ic_action_bell_thick_on))
        } else {
            menu?.getItem(0)?.setIcon(ContextCompat.getDrawable(applicationContext, R.drawable.ic_action_bell_thick_off))
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId){
            R.id.menu_bell -> {
                toggleNotifications()
            }
            R.id.menu_settings -> {
                println("Settings clicked")
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
            }
            else -> {
                return super.onOptionsItemSelected(item)
            }
        }
        return true
    }

    private fun toggleNotifications(){

        val settings = getPreferences(Context.MODE_PRIVATE)
        val editor = settings.edit()

        when (settings.getBoolean("globalNotifications", true)){
            true -> {
                // Used to be turned on, deactivate it now
                editor.putBoolean("globalNotifications", false)
                menu?.getItem(0)?.setIcon(ContextCompat.getDrawable(applicationContext, R.drawable.ic_action_bell_thick_off))

                Log.d(TAG, "Notifications are now turned OFF")
            }
            false -> {
                // Used to be turned off, activate it now
                editor.putBoolean("globalNotifications", true)
                menu?.getItem(0)?.setIcon(ContextCompat.getDrawable(applicationContext, R.drawable.ic_action_bell_thick_on))
                Log.d(TAG, "Notifications are now turned ON")
            }
        }
        // Save persistently
        editor.apply()
    }

    private fun addMessage(message: String) : Unit{
        try {

            val wrapper = Gson().fromJson(message.trimIndent(), StatusMessage::class.java)
            data.add(wrapper)

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


}