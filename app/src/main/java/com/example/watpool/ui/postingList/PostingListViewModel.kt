package com.example.watpool.ui.postingList

import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.watpool.services.FirebaseService
import com.example.watpool.services.models.Trips
import com.google.android.libraries.places.api.model.LocalDate
//import com.example.watpool.services.models.toTrip
import kotlinx.coroutines.launch

class PostingListViewModel: ViewModel() {

    @RequiresApi(Build.VERSION_CODES.O)
    fun confirmTrip(firebaseService: FirebaseService, tripId: String){
        viewModelScope.launch {
            try {
                var isTripConfirmed = false
                val riderId = firebaseService.currentUser()
                val confirm = firebaseService.fetchAllConfirmedTripsByRiderId(riderId)
                for (trip in confirm){
                    val confirmTripId = trip.id
                    if(confirmTripId == tripId){
                        isTripConfirmed = true
                    }
                }
                if(!isTripConfirmed){
                    firebaseService.createTripConfirmation(tripId, java.time.LocalDate.now(), riderId)
                }
            } catch (e: Exception){
                Log.e("PostingListViewModel", "Trip Confirmation Fail " + e.message)
            }
        }

    }

}