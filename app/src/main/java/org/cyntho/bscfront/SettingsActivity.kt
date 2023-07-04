package org.cyntho.bscfront

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.preference.PreferenceManager
import org.cyntho.bscfront.ui.main.SettingsFragment

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //setContentView(R.layout.activity_settings)

        val pref = PreferenceManager.getDefaultSharedPreferences(this)


        supportFragmentManager.beginTransaction().replace(android.R.id.content, SettingsFragment()).commit()
    }
}