package com.example.watpool.ui.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.watpool.services.firebase_services.FirebaseAuthService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class LoginViewModel : ViewModel() {
    private val authService = FirebaseAuthService()

    private val _userLiveData = MutableLiveData<FirebaseUser?>()
    val userLiveData: LiveData<FirebaseUser?> = _userLiveData

    private val _errorLiveData = MutableLiveData<String?>()
    val errorLiveData: LiveData<String?> = _errorLiveData

    fun loginUser(email: String, password: String) {
        authService.signIn(email, password).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                _userLiveData.value = FirebaseAuth.getInstance().currentUser
            } else {
                _errorLiveData.value = task.exception?.message
            }
        }
    }

    fun logoutUser() {
        FirebaseAuth.getInstance().signOut()
        _userLiveData.value = null
    }
}
