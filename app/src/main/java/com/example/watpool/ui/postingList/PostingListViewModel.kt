package com.example.watpool.ui.postingList
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.watpool.services.FirebaseService
import com.example.watpool.services.models.Postings
import com.google.android.gms.maps.model.LatLng

import kotlinx.coroutines.launch

class PostingListViewModel: ViewModel() {

    private val _postings : MutableLiveData<Postings> = MutableLiveData()
    val postings: LiveData<Postings> get() = _postings
    @RequiresApi(Build.VERSION_CODES.O)
    fun confirmTrip(firebaseService: FirebaseService, tripId: String){
        viewModelScope.launch {
            try {
                val riderId = firebaseService.currentUser()
                Log.e("PostingListView", "TripConfirm 1" + riderId)
                /*val confirm = firebaseService.fetchAllConfirmedTripsByRiderId(riderId)
                Log.e("PostingListView", "TripConfirm alkready created") */
                firebaseService.addPassenger(tripId,riderId)
                firebaseService.createTripConfirmation(tripId, java.time.LocalDate.now(), riderId)

            } catch (e: Exception){
               /* Log.e("PostingListViewModel", "Trip Confirmation Fail " + e.message)
                val riderId = firebaseService.currentUser()
                firebaseService.addPassenger(tripId,riderId)
                firebaseService.createTripConfirmation(tripId, java.time.LocalDate.now(), riderId)*/
            }
        }
    }

    fun getPostingDetails(firebaseService: FirebaseService, postingId: String){
        val trip = firebaseService.fetchTripsByTripsId(listOf(postingId))
        trip.addOnSuccessListener {documentSnapshots ->
            val document = documentSnapshots[0]
            val dataModel = document.toObject(Postings::class.java)
            dataModel?.let {
                Log.e("ViewModelTest", "To and From" + it.to)
                _postings.value = it
            }


        }.addOnFailureListener { exception ->
            Log.e("ViewModelTest", "Error fetching posting details", exception)
        }
    }
}