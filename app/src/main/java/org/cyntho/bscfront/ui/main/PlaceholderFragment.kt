package org.cyntho.bscfront.ui.main

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat.getColor
import androidx.core.view.allViews
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import org.cyntho.bscfront.MainActivity2
import org.cyntho.bscfront.R
import org.cyntho.bscfront.data.MessageType
import org.cyntho.bscfront.data.StatusMessage
import org.cyntho.bscfront.databinding.FragmentMainBinding
import org.cyntho.bscfront.databinding.ListEntryBinding
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * A placeholder fragment containing a simple view.
 */
class PlaceholderFragment : Fragment() {

    private val TAG = "PlaceholderFragment"

    private lateinit var pageViewModel: PageViewModel
    private var _binding: FragmentMainBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private lateinit var onAddMessage: (msg: StatusMessage) -> Unit?
    private lateinit var onRemoveMessage: (id: Int) -> Unit?

    private var main: MainActivity2? = null
    private var inf: LayoutInflater? = null

    private val entries: List<EntryViewModel> = listOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pageViewModel = ViewModelProvider(this).get(PageViewModel::class.java).apply {
            setIndex(arguments?.getInt(ARG_SECTION_NUMBER) ?: 1)
        }

        println("PlaceholderFragment.OnCreate() ${pageViewModel.getIndex()}")

        if (main != null){
            println("PlaceholderFragment.onCreate() with id ${pageViewModel.getIndex()}")
            main!!.addFragment(this, pageViewModel.getIndex())
        }

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        pageViewModel = ViewModelProvider(this).get(PageViewModel::class.java).apply {
            setIndex(arguments?.getInt(ARG_SECTION_NUMBER) ?: 1)
        }

        println("PlaceholderFragment.OnCreateView() ${pageViewModel.getIndex()}")

        inf = inflater
        _binding = FragmentMainBinding.inflate(inflater, container, false)



    /*    println("found ${(wrapper.findViewById<ConstraintLayout>(R.id.msg_container_prefab).allViews).toList().size} entries")
        for (c in wrapper.findViewById<ConstraintLayout>(R.id.msg_container_prefab).allViews){
            when (c is TextView){
                true -> println("${c.text}")
                false -> println("$c")
            }
        }*/


        return binding.root
    }


    fun onMessageReceived(message: StatusMessage){
        /*val view: TextView = TextView(context)
        view.text = message
        binding.layoutWrapper.addView(view)*/
        println("Called onMessageReceived() on fragment ${pageViewModel.getIndex()} with message [$message]")
        //pageViewModel.addMessage(message)

        main?.runOnUiThread {

            val v = inf?.inflate(R.layout.list_entry, null)

            val container = binding.layoutWrapper

            val txtTime = v?.findViewById<TextView>(R.id.msg_time_prefab)
            val txtDevice = v?.findViewById<TextView>(R.id.msg_device_prefab)
            val txtMessage = v?.findViewById<TextView>(R.id.msg_text_prefab)
            val bg = v?.findViewById<ConstraintLayout>(R.id.msg_container_prefab)
            val tagTime = v?.findViewById<TextView>(R.id.timer_time)

            if (v != null && txtTime != null && txtDevice != null && txtMessage != null && bg != null && tagTime != null){

                // Assign message text
                val tmp = "${message.sps}${message.group}${message.device}${message.part}"
                txtDevice.text = tmp
                txtMessage.text = "${message.message}"
                txtTime.text = message.time.toString()

                try {
                    val ttime = DateTimeFormatter.
                            ofPattern("HH:mm:ss").
                            format(Instant.ofEpochSecond(message.time).
                            atZone(ZoneId.of("Europe/Berlin")))
                    txtTime.text = ttime.toString()

                    println(DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneOffset.UTC).format(Instant.now()))

                } catch (nf: NumberFormatException){
                    Log.w(TAG, "Unable to parse time given: ${message.time}")
                    txtTime.text = getText(R.string.txt_time_null)
                }

                // Assign background color depending on the type of message received
                when (message.status){
                    MessageType.ERROR -> bg.setBackgroundColor(getColor(requireContext(), R.color.background_error))
                    MessageType.INFO -> bg.setBackgroundColor(getColor(requireContext(), R.color.background_info))
                    MessageType.STATUS -> bg.setBackgroundColor(getColor(requireContext(), R.color.background_status))
                    else -> {
                        // message.status MAY BE NULL
                        // This is due to the gson parsing..
                        bg.setBackgroundColor(getColor(requireContext(), R.color.background_error))
                        Log.w(TAG, "Unable to resolve error code: ${message.status}")
                    }
                }

                // Determine at which position the entry should be added


                container.addView(v, determineIndexFor(v))

            } else {
                Log.w(TAG, "Unable to resolve one or more elements in the UI")
            }
        }
    }

    private fun determineIndexFor(view: View): Int {
        for (c in binding.layoutWrapper.findViewById<ConstraintLayout>(R.id.layoutWrapper).allViews) {
            Log.i(TAG, "Date = ${c.findViewById<TextView>(R.id.timer_time)?.text}")
        }

        return 0
    }


    /**
     * Gets called every time the app is brought back to foreground
     */
    override fun onResume() {
        super.onResume()
        // ToDo
    }


    companion object {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private const val ARG_SECTION_NUMBER = "section_number"

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        @JvmStatic
        fun newInstance(sectionNumber: Int, m: MainActivity2?): PlaceholderFragment {
            val value = PlaceholderFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_SECTION_NUMBER, sectionNumber)
                }
            }

            println("PlaceholderFragment.newInstance($sectionNumber)")
            value.main = m
            return value
        }

    }

    override fun onDestroyView() {

        println("PlaceholderFragment.OnDestroyView() ${pageViewModel.getIndex()}")

        /*if (main != null){
            println("PlaceholderFragment.onDelete() with id ${pageViewModel.getIndex()}")
            main!!.removeFragment(pageViewModel.getIndex())
        }*/

        super.onDestroyView()
        _binding = null
    }
}