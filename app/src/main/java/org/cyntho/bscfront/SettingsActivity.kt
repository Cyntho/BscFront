package org.cyntho.bscfront

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.SyncStateContract
import androidx.preference.Preference
import androidx.preference.PreferenceManager
import androidx.preference.SwitchPreference
import org.cyntho.bscfront.ui.main.SettingsFragment

class SettingsActivity : AppCompatActivity() {

    private lateinit var _username: String
    private lateinit var _password: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //setContentView(R.layout.activity_settings)


        _username = intent.extras?.getString(USERNAME) ?: "empty"
        _password = intent.extras?.getString(PASSWORD) ?: "empty"

        println("CREATED SettingsActivity with: username = $_username, password = $_password")

        //val f = SettingsFragment.newInstance(applicationContext, _username, _password)
        supportFragmentManager.beginTransaction().replace(android.R.id.content, SettingsFragment()).commit()
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