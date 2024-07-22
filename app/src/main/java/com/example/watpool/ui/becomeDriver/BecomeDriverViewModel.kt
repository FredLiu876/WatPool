package com.example.watpool.ui.becomeDriver

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

data class UserDetails(
    val email: String,
    val licenseNumber: String,
    val carModel: String,
    val carColor: String
)

class BecomeDriverViewModel : ViewModel() {
    private val _userLiveData = MutableLiveData<FirebaseUser?>()
    val userLiveData: LiveData<FirebaseUser?> = _userLiveData

    init {
        val user = FirebaseAuth.getInstance().currentUser
        _userLiveData.value = user
    }
}
