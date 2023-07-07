package org.cyntho.bscfront.ui.main

import android.os.Bundle
import androidx.preference.MultiSelectListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import androidx.preference.SwitchPreference
import org.cyntho.bscfront.R

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)

     /*   val liste = findPreference<MultiSelectListPreference>("multi_select_list_preference_1")
        if (liste == null){
            println("Unable to resolve list")
            return
        }
        setListPreferenceData(liste)*/

        println("Root key: $rootKey")

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
    }
}