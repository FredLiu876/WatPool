package com.example.watpool.ui.welcome

import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import com.example.watpool.R

class WelcomeViewModel : ViewModel() {
    fun onGetStartedClicked(navController: NavController) {
        // Handle the button click and navigate to the dashboard screen
        navController.navigate(R.id.navigation_login)
    }
}