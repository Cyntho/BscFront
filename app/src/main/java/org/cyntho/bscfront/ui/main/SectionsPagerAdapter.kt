package org.cyntho.bscfront.ui.main

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import org.cyntho.bscfront.MainActivity2
import org.cyntho.bscfront.R



/**
 * A [FragmentPagerAdapter] that returns a fragment corresponding to
 * one of the sections/tabs/pages.
 */
class SectionsPagerAdapter(private val context: Context, fm: FragmentManager) : FragmentPagerAdapter(fm) {

    private var main: MainActivity2? = null
    private var titles: Array<String> = arrayOf()
    private var counter: Int = titles.size

    override fun getItem(position: Int): Fragment {
        // getItem is called to instantiate the fragment for the given page.
        // Return a PlaceholderFragment (defined as a static inner class below).
        return PlaceholderFragment.newInstance(position, main)
    }

    override fun getPageTitle(position: Int): CharSequence? {
        if (position >= 0 && position < titles.size){
            return titles[position]
        }
        return "Unknown"
    }

    override fun getCount(): Int {
        return counter
    }

    fun setCounter(value: Int){
        counter = value
    }

    fun setActivity(a: MainActivity2){
        main = a
    }

    fun setTitles(arr: Array<String>){
        titles = arr
    }
}