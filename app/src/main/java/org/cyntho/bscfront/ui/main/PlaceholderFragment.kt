package org.cyntho.bscfront.ui.main

import android.annotation.SuppressLint
import android.os.Bundle
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
import org.cyntho.bscfront.MainActivity
import org.cyntho.bscfront.R
import org.cyntho.bscfront.data.MessageType
import org.cyntho.bscfront.data.StatusMessage
import org.cyntho.bscfront.databinding.FragmentMainBinding
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * A placeholder fragment containing a simple view.
 */
class PlaceholderFragment : Fragment() {


    private lateinit var pageViewModel: PageViewModel
    private val binding get() = _binding!!

    private var _binding: FragmentMainBinding? = null
    private var main: MainActivity? = null
    private var inf: LayoutInflater? = null

    private lateinit var messages: MutableList<StatusMessage>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pageViewModel = ViewModelProvider(this).get(PageViewModel::class.java).apply {
            setIndex(arguments?.getInt(ARG_SECTION_NUMBER) ?: 1)
        }

        if (main != null){
            main!!.addFragment(this, pageViewModel.getIndex())
        }
        messages = mutableListOf()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        pageViewModel = ViewModelProvider(this).get(PageViewModel::class.java).apply {
            setIndex(arguments?.getInt(ARG_SECTION_NUMBER) ?: 1)
        }

        inf = inflater
        _binding = FragmentMainBinding.inflate(inflater, container, false)

        return binding.root
    }


    @SuppressLint("InflateParams")
    fun onAddMessage(message: StatusMessage){
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

                    //println(DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneOffset.UTC).format(Instant.now()))

                } catch (nf: NumberFormatException){
                    Log.w(TAG, "Unable to parse time given: ${message.time}")
                    txtTime.text = getText(R.string.txt_time_null)
                }

                // Assign background color depending on the type of message received
                when (message.status){
                    MessageType.ERROR -> bg.setBackgroundColor(getColor(requireContext(), R.color.background_error))
                    MessageType.WARNING -> bg.setBackgroundColor(getColor(requireContext(), R.color.background_info))
                    else -> {
                        // message.status MAY BE NULL
                        // This is due to the gson parsing...
                        bg.setBackgroundColor(getColor(requireContext(), R.color.background_error))
                        Log.w(TAG, "Unable to resolve error code: ${message.status}")
                    }
                }

                // Determine at which position the entry should be added
                // Add the message to the list and sort it. Query the just added message by its id.
                // The returned index corresponds to the new index on the Ui

                messages.add(message)
                messages.sortBy { it.time }

                val index = messages.indexOf(message)

                // allViews.count() counts itself as 1
                if (index > (container.allViews.count() - 1) || index < 0){
                    Log.wtf(TAG, "Something went definitely wrong! Array out of bound while adding message to container. Index: $index, Count: ${container.allViews.count()}")
                }

                container.addView(v, messages.indexOf(message))
            } else {
                Log.w(TAG, "Unable to resolve one or more elements in the UI")
            }
        }
    }

    fun onRemoveMessage(message: StatusMessage){

        val index = messages.indexOf(message)
        if (index == -1) {
            Log.e(TAG, "Attempting to remove non-existing message: ${message.id}")
            return
        }

        val container = binding.layoutWrapper
        main?.runOnUiThread {
            try {
                messages.removeAt(index)
                container.removeViewAt(index)
                // Log.d(TAG, "Removed message with id ${message.id} from view at index $index")
            } catch (any: Exception){
                Log.e(TAG, "Unable to remove element at index $index on fragment ${pageViewModel.getIndex()}")
            }
        }
    }

    companion object {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private const val ARG_SECTION_NUMBER = "section_number"

        private const val TAG = "PlaceholderFragment"

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        @JvmStatic
        fun newInstance(sectionNumber: Int, m: MainActivity?): PlaceholderFragment {
            val value = PlaceholderFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_SECTION_NUMBER, sectionNumber)
                }
            }

            value.main = m
            return value
        }

    }

    override fun onDestroyView() {

        // The fragment should remove itself from the MainActivity's list
        if (main != null){
            main!!.removeFragment(pageViewModel.getIndex())
        }

        super.onDestroyView()
        _binding = null
    }
}