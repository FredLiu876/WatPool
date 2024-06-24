package com.example.watpool.ui.maps

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.watpool.repository.DirectionsRepository
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MapsViewModel: ViewModel() {
    private val directionsRepository = DirectionsRepository()
    private val _directions = MutableLiveData<String>()
    val directions: LiveData<String> = _directions

    fun fetchDirections(origin: LatLng, destination: LatLng) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                directionsRepository.getDirections(origin, destination)
            }
            _directions.postValue(result.value)
        }
    }
}