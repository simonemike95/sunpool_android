package com.example.sunpool.ui.workers

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class WorkersViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is workers Fragment"
    }
    val text: LiveData<String> = _text
}