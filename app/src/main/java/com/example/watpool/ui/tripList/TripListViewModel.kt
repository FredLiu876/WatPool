package com.example.watpool.ui.tripList

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.watpool.services.FirebaseService
import com.example.watpool.services.models.TripConfirmationDetails
import com.example.watpool.services.models.Trip
import com.example.watpool.services.models.toTrip
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
                _trips.value = tripsList
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun fetchTripsByLocation(firebaseService: FirebaseService, latitude: Double, longitude: Double, radiusInKm: Double, startFilter: Boolean){
        viewModelScope.launch {
            try {
                val dbTripList = firebaseService.fetchTripsByLocation(latitude, longitude, radiusInKm, startFilter)
                dbTripList.addOnSuccessListener { documentSnapshots ->
                    val tripList = documentSnapshots.map { it.toTrip() }
                    _tripsInLocation.value = tripList
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}