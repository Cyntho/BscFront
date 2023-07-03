package org.cyntho.bscfront

import android.os.Bundle
import android.util.Log
import android.view.View
import com.google.android.material.tabs.TabLayout
import androidx.viewpager.widget.ViewPager
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.TableLayout
import androidx.transition.Visibility
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.runBlocking
import org.cyntho.bscfront.data.LocationWrapper
import org.cyntho.bscfront.data.StatusMessage
import org.cyntho.bscfront.ui.main.SectionsPagerAdapter
import org.cyntho.bscfront.databinding.ActivityMain2Binding
import org.cyntho.bscfront.net.KotlinMqtt
import org.cyntho.bscfront.ui.main.PlaceholderFragment

class MainActivity2 : AppCompatActivity() {

    private val TAG: String = "BscFront/MainActivity2"

    private lateinit var binding: ActivityMain2Binding
    private lateinit var mqtt: KotlinMqtt

    private val data: MutableCollection<StatusMessage> = mutableListOf()
    private val fragments: MutableMap<Int, PlaceholderFragment> = mutableMapOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
                }
            }
        }
    }

    private fun addMessage(message: String) : Unit{
        try {

            val wrapper = Gson().fromJson(message.trimIndent(), StatusMessage::class.java)
            data.add(wrapper)

            val f = fragments[wrapper.location]

            if (f != null){
                Log.d(TAG, "Fragment with id ${wrapper.location} found")
                f.onAddMessage(wrapper)
            } else {
                Log.w(TAG, "Unable to resolve Fragment by location id ${wrapper.location}")
            }
        } catch (any: Exception) {
            Log.d(TAG, "Error parsing message: $message")
            any.printStackTrace()
        }
    }

    private fun removeMessage(message: String) : Unit {
        try {
            val wrapper = Gson().fromJson(message.trimIndent(), StatusMessage::class.java)
            val fragment = fragments[wrapper.location]

            if (fragment != null){
                Log.d(TAG, "Fragment with id ${wrapper.location} found")
                fragment.onRemoveMessage(wrapper)
            } else {
                Log.w(TAG, "Unable to resolve Fragment by location id ${wrapper.location}")
            }

        } catch (any: Exception){
            Log.d(TAG, "Error parsing message: $message")
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

                // Clear any existing elements
                //pager.removeAllViews()
                //tabs.removeAllTabs()
                //fragments.clear()

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
            Log.d(TAG, "Error parsing settings: $settings")
            any.printStackTrace()
        }
        return false
    }

    private fun onMessagesReceived(m: String) : Boolean {
        return true
    }


    fun addFragment(fragment: PlaceholderFragment, id: Int){
        fragments[id] = fragment
        println("Added fragment $id")
    }

    fun removeFragment(id: Int){
        fragments.remove(id)
        println("Removed fragment $id")
    }


}