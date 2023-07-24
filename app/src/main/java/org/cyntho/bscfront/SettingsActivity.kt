package org.cyntho.bscfront

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.SyncStateContract
import android.view.MenuItem
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.core.app.NavUtils
import androidx.preference.Preference
import androidx.preference.PreferenceManager
import androidx.preference.SwitchPreference
import org.cyntho.bscfront.ui.main.SettingsFragment

class SettingsActivity : AppCompatActivity() {

    private lateinit var _username: String
    private lateinit var _password: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        _username = intent.extras?.getString("USERNAME") ?: "empty"
        _password = intent.extras?.getString("PASSWORD") ?: "empty"

        println("CREATED SettingsActivity with: username = $_username, password = $_password")
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val arr: Array<String>? = intent.extras?.getStringArray("LOCATIONS")
        val f = SettingsFragment.newInstance(_username, _password, arr)
        supportFragmentManager.beginTransaction().replace(android.R.id.content, f).commit()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId){
            android.R.id.home ->{
                println("Moving up")
                finish()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }




}