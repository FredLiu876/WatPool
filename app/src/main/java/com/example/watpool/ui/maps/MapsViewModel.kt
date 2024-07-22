package com.example.watpool.ui.maps

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.watpool.repository.DirectionsRepository
import com.example.watpool.services.FirebaseService
import com.example.watpool.services.models.Coordinate
import com.example.watpool.services.models.Postings
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject

class MapsViewModel: ViewModel() {
    private val _postingsInLocation : MutableLiveData<MutableList<Postings>> = MutableLiveData(
        mutableListOf())
    val postingsInRadius: LiveData<MutableList<Postings>> get() = _postingsInLocation
    private val directionsRepository = DirectionsRepository()
    private val _directions = MutableLiveData<String>()
    val directions: LiveData<String> = _directions

    private val _routePoints = MutableLiveData<ArrayList<LatLng>>()
    val routePoints: LiveData<ArrayList<LatLng>> = _routePoints

    private val _distanceDuration = MutableLiveData<ArrayList<Pair<Double,Double>>>()
    val distanceDuration: LiveData<ArrayList<Pair<Double,Double>>> = _distanceDuration

    fun fetchDirections(origin: LatLng, destination: LatLng) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                directionsRepository.getDirections(origin, destination)
            }
            _directions.postValue(result.value)
            result.value?.let { parseRoute(it) }
        }
    }

    private fun parseRoute(directions: String) {
        try {
            val jsonObject = JSONObject(directions)
            val routes = jsonObject.getJSONArray("routes")
            val points = ArrayList<LatLng>()
            val distanceDurationList = ArrayList<Pair<Double, Double>>()
            var accumulatedDistance = 0.0
            var accumulatedDuration = 0.0

            for (i in 0 until routes.length()) {
                val route = routes.getJSONObject(i)
                val legs = route.getJSONArray("legs")

                for (j in 0 until legs.length()) {
                    val leg = legs.getJSONObject(j)
                    val steps = leg.getJSONArray("steps")

                    for (k in 0 until steps.length()) {
                        val step = steps.getJSONObject(k)
                        val totalDistance = step.getJSONObject("distance").getDouble("value")
                        val totalDuration = step.getJSONObject("duration").getDouble("value")
                        val polyline = step.getJSONObject("polyline").getString("points")
                        val decodedPoints = decodePolyline(polyline)
                        points.addAll(decodedPoints)
                        for (p in decodedPoints.indices) {
                            val distance = totalDistance * ((p+1)/decodedPoints.size)
                            val duration = totalDuration * ((p+1)/decodedPoints.size)
                            accumulatedDistance += distance
                            accumulatedDuration += duration
                            distanceDurationList.add(Pair(accumulatedDistance, accumulatedDuration))
                        }
                    }
                }
            }
            _routePoints.postValue(points)
            _distanceDuration.postValue(distanceDurationList)
        } catch (e: Exception) {
            Log.e("Draw route Exception", e.toString())
        }
    }

    // Function to decode polyline points, decode code from https://stackoverflow.com/questions/39851243/android-ios-decode-polyline-string
    private fun decodePolyline(encoded: String): List<LatLng> {
        val poly = ArrayList<LatLng>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0

        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dlat

            shift = 0
            result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lng += dlng

            val p = LatLng(lat / 1E5, lng / 1E5)
            poly.add(p)
        }

        return poly
    }


    fun fetchPostingsByStartAndEnd(firebaseService: FirebaseService, latitudeStart:Double, longitudeStart: Double, radiusInKmStart: Double, latitudeEnd: Double, longitudeEnd: Double, radiusInKmEnd: Double){
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val postingsSnapshot = firebaseService.fetchTripsByStartAndEnd(
                    latitudeStart, longitudeStart, radiusInKmStart,
                    latitudeEnd, longitudeEnd, radiusInKmEnd
                ).await()

                val postingList = mutableListOf<Postings>()
                val deferredTasks = postingsSnapshot.map{ document ->
                    async {
                        val dataModel = document.toObject(Postings::class.java)
                        if (dataModel != null) {
                            Log.e("MapsViewModel", "Coord " + dataModel.startingCoordinateId)
                            val startCoordTask = async {
                                val startSnapshot = firebaseService.fetchCoordinatesById(dataModel.startingCoordinateId).await()
                                startSnapshot.documents[0].toObject(Coordinate::class.java)
                            }
                            val endCoordTask = async {
                                val endSnapshot = firebaseService.fetchCoordinatesById(dataModel.endingCoordinateId).await()
                                endSnapshot.documents[0].toObject(Coordinate::class.java)
                            }

                            val startCoord = startCoordTask.await()
                            val endCoord = endCoordTask.await()

                            if (startCoord != null && endCoord != null) {
                                dataModel.startCoords = startCoord
                                dataModel.endCoords = endCoord
                                Log.e("MapsViewModel", "Coord " + startCoord.location)
                                synchronized(postingList) {
                                    postingList.add(dataModel)
                                }
                            }
                        }
                    }
                }

                deferredTasks.forEach { it.await() }
                _postingsInLocation.postValue(postingList)
            } catch (e: Exception) {
                Log.e("MapsViewModel", "Unable to fetch postings: ${e.message}", e)
            }
        }

        /* viewModelScope.launch {
            val postingList : MutableList<Postings> = mutableListOf()

            val postings = firebaseService.fetchTripsByStartAndEnd(
                latitudeStart, longitudeStart, radiusInKmStart,
                latitudeEnd, longitudeEnd, radiusInKmEnd
            )
            postings.addOnSuccessListener { documentSnapshot ->
                val totalDocuments = documentSnapshot.size
                var processedDocuments = 0

                for (document in documentSnapshot) {
                    val dataModel = document.toObject(Postings::class.java)
                    if (dataModel != null) {
                        Log.e("Search", "Coordinate1 " + dataModel.endingCoordinateId)

                        val startData = firebaseService.fetchCoordinatesByDocumentId(dataModel.endingCoordinateId)
                        val endData = firebaseService.fetchCoordinatesByDocumentId(dataModel.endingCoordinateId)

                        startData.addOnCompleteListener() { snap ->
                            val startCoordData = snap.result.toObject(Coordinate::class.java)
                            if (startCoordData != null) {
                                dataModel.startCoords = startCoordData
                            }

                        }.addOnFailureListener {
                            Log.e("Search", "Error fetching start coordinates: ", it)
                        }

                        endData.addOnCompleteListener() { snap ->
                            val endCoordData = snap.result.toObject(Coordinate::class.java)
                            if (endCoordData != null) {
                                dataModel.endCoords = endCoordData
                            }
                        }.addOnFailureListener {
                            Log.e("Search", "Error fetching end coordinates: ", it)
                        }
                        postingList.add(dataModel)
                    }
                }
                _postingsInLocation.value = postingList
            }.addOnFailureListener {
                Log.e("MapsViewModel", "Unable To Fetch Postings: " + it.message)
            }
        }*/
    }



}