package org.cyntho.bscfront.ui.main

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import androidx.preference.MultiSelectListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import androidx.preference.SwitchPreference
import org.cyntho.bscfront.MainActivity
import org.cyntho.bscfront.R
import org.cyntho.bscfront.SettingsActivity

class SettingsFragment : PreferenceFragmentCompat() {

    private var _username: String = ""
    private var _password: String = ""


    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)

        _username = arguments?.getString(USERNAME) ?: "empty"
        _password = arguments?.getString(PASSWORD) ?: "empty"

        println("CREATED SettingsFragment with: username = $_username, password = $_password")

        val notificationsRoot = findPreference<SwitchPreference>("switch_preference_notifications_global")
        if (notificationsRoot != null){
            println("found it: ${notificationsRoot.key}")

            val storageOnePref = SwitchPreference(requireContext())
            storageOnePref.key = "elf1_key"
            storageOnePref.title = "ELF 1"
            storageOnePref.isPersistent = true

            preferenceScreen.addPreference(storageOnePref)

            storageOnePref.dependency = notificationsRoot.key
        }

        // ToDo: Display selection, load locations
        /*liste.setOnPreferenceChangeListener { _, _ ->
            return@setOnPreferenceChangeListener true
        }*/
    }

    private fun setListPreferenceData(list: MultiSelectListPreference){
        val entries: Array<CharSequence> = arrayOf("Something", "Else")
        val values: Array<CharSequence> = arrayOf("1", "2")

        list.entries = entries
        list.setDefaultValue("1")
        list.entryValues = values
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        println("CREATED SETTINGS FRAGMENT")
    }


    companion object {
        private const val USERNAME = "USERNAME"
        private const val PASSWORD = "PASSWORD"

        @JvmStatic
        fun newInstance(context: Context, username: String, password: String): SettingsFragment {
            val value = SettingsFragment().apply {
                arguments = Bundle().apply {
                    putString(USERNAME, username)
                    putString(PASSWORD, password)
                }
            }
            return value
        }
    }

}