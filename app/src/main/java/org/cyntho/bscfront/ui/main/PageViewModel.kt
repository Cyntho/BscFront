package org.cyntho.bscfront.ui.main

import android.view.View
import androidx.lifecycle.*
import org.cyntho.bscfront.data.StatusMessage
import org.eclipse.paho.android.service.MqttAndroidClient

class PageViewModel : ViewModel() {

    private val _index = MutableLiveData<Int>()

    private var messages = ArrayList<StatusMessage>()
    var messagesLive: MutableLiveData<List<StatusMessage>> = MutableLiveData()



    fun setIndex(index: Int) {
        _index.value = index
    }
    fun getIndex(): Int {
        return if (_index.value == null){
            -1
        } else {
            _index.value!!
        }
    }


}