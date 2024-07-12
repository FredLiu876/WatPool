package com.example.watpool.ui.create_trip

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import com.example.watpool.R
import android.util.Log

class CreateTripViewModel : ViewModel() {

    private val _pickupLocation = MutableLiveData<String>()
    val pickupLocation: LiveData<String> get() = _pickupLocation

    private val _destination = MutableLiveData<String>()
    val destination: LiveData<String> get() = _destination

    private val _selectedDate = MutableLiveData<String>()
    val selectedDate: LiveData<String> get() = _selectedDate

    private val _selectedTime = MutableLiveData<String>()
    val selectedTime: LiveData<String> get() = _selectedTime

    private val _numAvailableSeats = MutableLiveData<String>()
    val numAvailableSeats: LiveData<String> get() = _numAvailableSeats

    fun setPickupLocation(location: String) {
        _pickupLocation.value = location
    }

    fun setDestination(dest: String) {
        _destination.value = dest
    }

    fun setSelectedDate(date: String) {
        _selectedDate.value = date
    }

    fun setSelectedTime(time: String) {
        _selectedTime.value = time
    }

    fun setNumAvailableSeats(seats: String) {
        _numAvailableSeats.value = seats
    }

    fun onCreateTrip(navController: NavController) {
        // Handle the button click and navigate to the dashboard screen
        navController.navigate(R.id.navigation_dashboard)
    }

    fun saveTrip() {
        // end goal of this function is to save trip data to database

        // logged data can be seen in Logcat tab
        Log.d("CreateTripViewModel", "Pickup Location: ${_pickupLocation.value}")
        Log.d("CreateTripViewModel", "Destination: ${_destination.value}")
        Log.d("CreateTripViewModel", "Selected Date: ${_selectedDate.value}")
        Log.d("CreateTripViewModel", "Selected Time: ${_selectedTime.value}")
        Log.d("CreateTripViewModel", "Number of Available Seats: ${_numAvailableSeats.value}")
    }
}
