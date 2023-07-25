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

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)

        val notificationsRoot = findPreference<SwitchPreference>("switch_preference_notifications_global")
        val screen = preferenceScreen
        if (notificationsRoot != null){
            val arr = arguments?.getStringArray(LOCATION)
            val user = arguments?.getString(USERNAME)

            if (arr != null && user != null){

                for (entry in arr){
                    val opt = SwitchPreference(requireContext()).apply {
                        key = MainActivity.hash("$user@$entry")

                        println("Hashing user@entry = $user@$entry = $key")
                        title = entry
                        isPersistent = true
                    }

                    screen.addPreference(opt)
                    opt.dependency = notificationsRoot.key
                }
            }
        }
    }


    companion object {
        private const val USERNAME = "USERNAME"
        private const val LOCATION = "LOCATIONS"

        @JvmStatic
        fun newInstance(username: String, arr: Array<String>?): SettingsFragment {
            val value = SettingsFragment().apply {
                arguments = Bundle().apply {
                    putString(USERNAME, username)
                    putStringArray(LOCATION, arr)
                }
            }
            return value
        }
    }

}