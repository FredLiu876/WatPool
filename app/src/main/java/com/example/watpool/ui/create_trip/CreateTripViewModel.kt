package com.example.watpool.ui.create_trip

import android.os.Build
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.example.watpool.BuildConfig
import com.example.watpool.R
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.watpool.services.FirebaseService
import java.time.LocalDate
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

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

    private val _isRecurring = MutableLiveData<Boolean>()
    val isRecurring: LiveData<Boolean> get() = _isRecurring

    private val _recurringDays = MutableLiveData<BooleanArray>(BooleanArray(7) {false})
    val recurringDays: LiveData<BooleanArray> get() = _recurringDays

    private val _recurringEndDate = MutableLiveData<String>()
    val recurringEndDate: LiveData<String> get() = _recurringEndDate

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

    fun setIsRecurring(isRecurring: Boolean) {
        _isRecurring.value = isRecurring
    }

    fun setRecurringDay(dayIndex: Int, isSelected: Boolean) {
        val currentDays = _recurringDays.value ?: BooleanArray(7)
        currentDays[dayIndex] = isSelected
        _recurringDays.value = currentDays

        Log.d("setRecurringDay", "Day $dayIndex is ${currentDays[dayIndex]}")
    }

    fun setRecurringEndDate(date: String) {
        _recurringEndDate.value = date
    }

    fun onCreateTrip(navController: NavController) {
        // Handle the button click and navigate to the dashboard screen
        navController.navigate(R.id.navigation_dashboard)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun saveTrip(firebaseService: FirebaseService) {
        viewModelScope.launch {
            try {
                val driverId = firebaseService.currentUser()

                val startLocation = _pickupLocation.value ?: ""
                val endLocation = _destination.value ?: ""

                // TODO: get coords from the maps UI?
                var start = getCoordinates(startLocation)
                val startLatitude = start.first
                val startLongitude = start.second

                val end = getCoordinates(endLocation)
                val endLatitude = end.first
                val endLongitude = end.second

                Log.d("CreateTripViewModel", "Start location is ${startLatitude}, ${startLongitude}")
                Log.d("CreateTripViewModel", "End location is ${endLatitude}, ${endLongitude}")

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

    fun fetchAllCoordinatesForCurrentUser(firebaseService: FirebaseService) {
        viewModelScope.launch {
            val driverId = firebaseService.currentUser()
            firebaseService.fetchCoordinatesByDriverId(driverId)
                .addOnSuccessListener { querySnapshot ->
                    for (document in querySnapshot.documents) {
                        val documentId = document.id
                        val documentData = document.data
                        Log.d("Coordinates", "Document ID: $documentId, Data: $documentData")
                    }
                }.addOnFailureListener { e ->
                Log.e("Coordinates", "Error fetching coordinates: ${e.message}", e)
            }
        }
    }

    suspend fun getCoordinates(address: String): Pair<Double, Double> {
        return withContext(Dispatchers.IO) {
            val apiKey = BuildConfig.MAPS_API_KEY

            val url = "https://maps.googleapis.com/maps/api/geocode/json?address=${address.replace(" ", "+")}&key=$apiKey"
            val response = URL(url).readText()
            val json = JSONObject(response)

            val location = json.getJSONArray("results")
                .getJSONObject(0)
                .getJSONObject("geometry")
                .getJSONObject("location")

            Pair(location.getDouble("lat"), location.getDouble("lng"))
        }
    }
}
