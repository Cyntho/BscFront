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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val arr: Array<String>? = intent.extras?.getStringArray("LOCATIONS")
        val username: String = intent.extras?.getString("USERNAME", "empty") ?: "empty"
        val f = SettingsFragment.newInstance(username, arr)
        supportFragmentManager.beginTransaction().replace(android.R.id.content, f).commit()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId){
            android.R.id.home ->{
                // Finish activity to prevent MainActivity from loading again and requiring additional login
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}