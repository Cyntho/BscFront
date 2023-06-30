package org.cyntho.bscfront

import android.os.Bundle
import com.google.android.material.tabs.TabLayout
import androidx.viewpager.widget.ViewPager
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import androidx.core.view.iterator
import androidx.fragment.app.Fragment
import com.google.gson.Gson
import kotlinx.coroutines.runBlocking
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
    //private val fragments: MutableCollection<PlaceholderFragment> = mutableListOf()
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
        viewPager.offscreenPageLimit = 4

        println("Limit: ${viewPager.offscreenPageLimit}")

        val tabs: TabLayout = binding.tabs
        tabs.setupWithViewPager(viewPager)

        mqtt = KotlinMqtt()

        val btnConnect: Button = binding.btnConnect
        btnConnect.setOnClickListener {
            //Snackbar.make(view, "Something happened!", Snackbar.LENGTH_LONG).setAction("Action", null).show()
            //connect(view)

            if (mqtt.isOnline()){
                println("Mqtt Service running.. Attempting to stop it")
                runBlocking {
                    mqtt.disconnect()
                }
                println("Done")
            } else {
                println("Attempting to start listener")
                runBlocking {
                    mqtt.connect(::addText)
                }
                println("Done")
            }
        }
    }

    private fun addText(message: String) : Unit{
        /*val view: TextView = TextView(context)
        view.text = message
        container.addView(view)*/

        try {
            val wrapper = Gson().fromJson(message.trimIndent(), StatusMessage::class.java)
            data.add(wrapper)

            val f = fragments[wrapper.id]
            // fragments[wrapper.id]?.onMessageReceived(wrapper)

            if (f != null){
                println("Fragment with id ${wrapper.id} found")
                f.onMessageReceived(wrapper)
            }

        } catch (any: Exception) {
            println("Error parsing message")
            any.printStackTrace()
        }
    }

    fun addFragment(fragment: PlaceholderFragment, id: Int){
        fragments[id] = fragment
        println("Adding new fragment with id $id")
    }

    fun removeFragment(id: Int){
        fragments.remove(id)
    }

}