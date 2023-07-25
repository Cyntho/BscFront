package org.cyntho.bscfront

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import org.cyntho.bscfront.ui.main.SettingsFragment

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val arr: Array<String>? = intent.extras?.getStringArray("LOCATIONS")
        val username: String = intent.extras?.getString("USERNAME", "empty") ?: "empty"
        val f = SettingsFragment.newInstance(username, arr)
        supportFragmentManager.beginTransaction().replace(android.R.id.content, f).commit()
    }

    /**
     * Overriding this to handle the click on the 'back arrow' in the top left of the ActionBar
     */
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