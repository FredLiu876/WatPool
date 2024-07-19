package com.example.watpool.ui.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class ProfileViewModel : ViewModel() {
    private val _userLiveData = MutableLiveData<FirebaseUser?>()
    val userLiveData: LiveData<FirebaseUser?> = _userLiveData

    private val _text = MutableLiveData<String>().apply {
        value = "This is the profile Fragment"
    }
    val text: LiveData<String> = _text

    fun logoutUser() {
        FirebaseAuth.getInstance().signOut()
        _userLiveData.value = null
    }
}