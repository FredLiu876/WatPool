package com.example.watpool.ui.postingList

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.watpool.services.FirebaseService
import com.example.watpool.services.models.Trips
//import com.example.watpool.services.models.toTrip
import kotlinx.coroutines.launch

class PostingListViewModel: ViewModel() {

    private val _postingsInLocation = MutableLiveData<List<Trips>>()
    val postingsInRadius: LiveData<List<Trips>> get() = _postingsInLocation

    fun fetchPostingsByLocation(firebaseService: FirebaseService, latitude: Double, longitude: Double, radiusInKm: Double, startFilter: Boolean){
        Log.e("Posting View Model", "Is broke " + latitude)
        viewModelScope.launch {
            try {
                val dbTripList = firebaseService.fetchTripsByLocation(latitude, longitude, radiusInKm, startFilter)
                dbTripList.addOnSuccessListener { documentSnapshots ->
                    val tripList = documentSnapshots.map { /*it.toTrip()*/ }
                    //Log.e("Posting View Model", "Is broke 2" + tripList.get(0).id)
                    //_postingsInLocation.value = tripList
                    for (trip in tripList){
                        //Log.e("Posting View Model", "Trip found " + trip.id)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}