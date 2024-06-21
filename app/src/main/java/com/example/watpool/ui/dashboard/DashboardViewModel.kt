package com.example.watpool.ui.dashboard

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import com.example.watpool.R

class DashboardViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is dashboard Fragment"
    }
    val text: LiveData<String> = _text

    fun onCreateTrip(navController: NavController) {
        // Handle the button click and navigate to the dashboard screen
        navController.navigate(R.id.fragment_create_trip)
    }
}