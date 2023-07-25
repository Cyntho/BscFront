package org.cyntho.bscfront.ui.main

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import org.cyntho.bscfront.MainActivity
import org.cyntho.bscfront.R


/**
 * This fragment handles the dynamic loading of saved preferences
 */
class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)

        val notificationsRoot = findPreference<SwitchPreference>("switch_preference_notifications_global")
        val screen = preferenceScreen
        if (notificationsRoot != null){
            val arr = arguments?.getStringArray(LOCATION)
            val user = arguments?.getString(USERNAME)

            if (arr != null && user != null){

                // Load all previously saved settings for the current [user]
                for (entry in arr){
                    val opt = SwitchPreference(requireContext()).apply {
                        key = MainActivity.hash("$user@$entry")
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