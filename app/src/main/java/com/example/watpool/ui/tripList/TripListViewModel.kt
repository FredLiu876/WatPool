package com.example.watpool.ui.tripList

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.watpool.services.FirebaseService
import com.example.watpool.services.models.TripConfirmationDetails
import kotlinx.coroutines.launch

class TripListViewModel : ViewModel() {

    private val _trips = MutableLiveData<List<TripConfirmationDetails>>()
    val trips: LiveData<List<TripConfirmationDetails>> get() = _trips

    fun fetchConfirmedTrips(firebaseService: FirebaseService) {
        viewModelScope.launch {
            try {
                val riderId = firebaseService.currentUser()
                val tripsList = firebaseService.fetchAllConfirmedTripsByRiderId(riderId)
                _trips.value = tripsList
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}