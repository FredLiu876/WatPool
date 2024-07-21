package com.example.watpool.ui.signup

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.watpool.services.firebase_services.FirebaseAuthService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class SignUpViewModel : ViewModel() {
    private val authService = FirebaseAuthService()

    private val _userLiveData = MutableLiveData<FirebaseUser?>()
    val userLiveData: LiveData<FirebaseUser?> = _userLiveData

    private val _errorLiveData = MutableLiveData<String?>()
    val errorLiveData: LiveData<String?> = _errorLiveData

    init {
        if (FirebaseAuth.getInstance().currentUser != null) {
            _userLiveData.value = FirebaseAuth.getInstance().currentUser
        }
    }

    fun registerUser(email: String, password: String, name: String) {
        authService.signUp(email, password).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                authService.updateUserProfile(name).addOnCompleteListener { profileUpdateTask ->
                    if (profileUpdateTask.isSuccessful) {
                        _userLiveData.value = FirebaseAuth.getInstance().currentUser
                    } else {
                        _errorLiveData.value = profileUpdateTask.exception?.message
                    }
                }
            } else {
                _errorLiveData.value = task.exception?.message
            }
        }
    }
}
