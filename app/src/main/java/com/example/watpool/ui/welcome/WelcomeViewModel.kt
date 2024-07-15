package com.example.watpool.ui.welcome

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import com.example.watpool.R
import com.example.watpool.services.firebase_services.FirebaseAuthService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class WelcomeViewModel : ViewModel() {
    private val authService = FirebaseAuthService()

    private val _userLiveData = MutableLiveData<FirebaseUser?>()
    val userLiveData: LiveData<FirebaseUser?> = _userLiveData

    fun checkUserLoggedIn() {
        _userLiveData.value = authService.getCurrentUser()
    }
    fun onGetStartedClicked(navController: NavController) {
        // Handle the button click and navigate to the dashboard screen
        navController.navigate(R.id.navigation_login)
    }

    fun onRegisterClicked(navController: NavController) {
        // Handle the button click and navigate to the dashboard screen
        navController.navigate(R.id.navigation_signup)
    }
}