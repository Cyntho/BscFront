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

    fun addMessage(msg: StatusMessage){
        println("Tab [$_index] attempting to add msg with id [${msg.id}]")
        messages.add(msg)
        //messagesLive.value = messages
        messagesLive.postValue(messages)

        for (m in messages.iterator()){
            println(m.toString())
        }
    }

    fun removeMessage(id: Int){
        println("Tab [$_index] attempting to remove msg with id [$id]")
        for (m in messages){
            if (m.id == id){
                messages.remove(m)
                println("Removed message")
                messagesLive.value = messages
                return
            }
        }
        println("Could not find message")
    }



}