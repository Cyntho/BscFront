package org.cyntho.bscfront.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.cyntho.bscfront.databinding.ListEntryBinding

class EntryViewModel : ViewModel() {


    private val _text: MutableLiveData<String> = MutableLiveData()

    val text: LiveData<String> = _text

}