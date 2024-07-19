package com.example.watpool.ui.maps

import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.watpool.repository.DirectionsRepository
import com.example.watpool.services.FirebaseService
import com.example.watpool.services.models.Coordinate
import com.example.watpool.services.models.toCoordinate
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MapsViewModel: ViewModel() {

    private val _coordinates = MutableLiveData<List<Coordinate>>()
    val coordinates: LiveData<List<Coordinate>> get() = _coordinates
    fun fetchCoordinatesInRadius(firebaseService: FirebaseService,latitude: Double, longitude: Double, radiusInKm: Double){
        viewModelScope.launch {
            val postings = firebaseService.fetchCoordinatesByLocation(latitude, longitude, radiusInKm)
            postings.addOnSuccessListener { documentSnapshot ->
                val coordinateList = documentSnapshot.map { it.toCoordinate() }
                _coordinates.value = coordinateList
            }.addOnFailureListener {
                it.printStackTrace()
            }
        }
    }

    fun createTrip(){

    }
}