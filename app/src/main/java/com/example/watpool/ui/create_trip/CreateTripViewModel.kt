package com.example.watpool.ui.create_trip

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.example.watpool.R
import android.util.Log
import com.example.watpool.services.FirebaseService
import java.time.LocalDate
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.format.DateTimeFormatter

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

    private val _tripCreationStatus = MutableLiveData<String>()
    val tripCreationStatus: LiveData<String> get() = _tripCreationStatus

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

    fun saveTrip(firebaseService: FirebaseService) {
        // TODO: Add loading animation (saving a trip to the db takes ~1 min)
        viewModelScope.launch {
            try {
                val driverId = firebaseService.currentUser()

                // get from the maps UI
                val startLatitude = 0.0
                val startLongitude = 0.0
                val endLatitude = 0.0
                val endLongitude = 0.0

                val startLocation = _pickupLocation.value ?: ""
                val endLocation = _destination.value ?: ""

                val dateFormatter = DateTimeFormatter.ofPattern("d-M-yyyy")
                val tripDate = LocalDate.parse(_selectedDate.value, dateFormatter)
                val maxPassengers = _numAvailableSeats.value ?: "0"

                val result = firebaseService.createTrip(
                    driverId,
                    startLatitude,
                    endLatitude,
                    startLongitude,
                    endLongitude,
                    startLocation,
                    endLocation,
                    tripDate,
                    maxPassengers
                ).await()

                result.addOnSuccessListener { documentReference ->
                    _tripCreationStatus.postValue("Trip created successfully with ID: ${documentReference.id}")
                }.addOnFailureListener { e ->
                    _tripCreationStatus.postValue("Error creating trip: ${e.message}")
                }

                val currentUserId = firebaseService.currentUser()
                Log.d("CreateTripViewModel", "current user ID is ${currentUserId}")

                Log.d("CreateTripViewModel","Trip created successfully")

            } catch (e: Exception) {
                _tripCreationStatus.postValue("Error creating trip: ${e.message}")
                Log.e("CreateTripViewModel","Error creating trip: ${e.message}")
            }
        }

            // logged data can be seen in Logcat tab
            Log.d("CreateTripViewModel", "Pickup Location: ${_pickupLocation.value}")
            Log.d("CreateTripViewModel", "Destination: ${_destination.value}")
            Log.d("CreateTripViewModel", "Selected Date: ${_selectedDate.value}")
            Log.d("CreateTripViewModel", "Selected Time: ${_selectedTime.value}")
            Log.d("CreateTripViewModel", "Number of Available Seats: ${_numAvailableSeats.value}")
        }

    // Used for debugging to make sure that trips are being added correctly
    fun fetchTripsForCurrentUser(firebaseService: FirebaseService) {
        viewModelScope.launch {
            try {
                val currentUserId = firebaseService.currentUser()
                if (currentUserId.isNotEmpty()) {
                    val trips = firebaseService.fetchTripsByDriverId(currentUserId).await()
                    Log.d("CreateTripViewModel", "Fetched ${trips.size} trips for user $currentUserId")

                    for (document in trips) {
                        val userId = document.id
                        val userData = document.data
                        Log.d("CreateTripViewModel", "User ID: $userId, Data: $userData")
                    }
                } else {
                    Log.e("CreateTripViewModel", "No current user found")
                }
            } catch (e: Exception) {
                Log.e("CreateTripViewModel", "Error fetching all user trips", e)
            }
        }
    }
}
