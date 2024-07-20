package com.example.watpool.ui.tripList

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.watpool.services.FirebaseService
import com.example.watpool.services.models.TripConfirmationDetails
import com.example.watpool.services.models.Trip
import kotlinx.coroutines.launch

class TripListViewModel : ViewModel() {

    private val _trips = MutableLiveData<List<TripConfirmationDetails>>()
    private val _tripsInLocation = MutableLiveData<List<Trip>>()
    val trips: LiveData<List<TripConfirmationDetails>> get() = _trips
    val tripsInRadius: LiveData<List<Trip>> get() = _tripsInLocation
    fun fetchConfirmedTrips(firebaseService: FirebaseService, riderId: String) {
        viewModelScope.launch {
            try {
                val tripsList = firebaseService.fetchAllConfirmedTripsByRiderId(riderId)
                Log.e("Posting View Model", "Is broke 2" + tripsList.get(0).id)
                _trips.value = tripsList
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}