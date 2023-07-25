package org.cyntho.bscfront.ui.main

import androidx.lifecycle.*

/**
 * ViewModel for tabs inside MainActivity's TabLayout
 */
class PageViewModel : ViewModel() {

    private val _index = MutableLiveData<Int>()

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