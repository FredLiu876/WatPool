package com.example.watpool.services

import android.app.Service
import android.content.Intent
import android.location.Location
import android.os.Binder
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.*

class LocationService : Service() {

    // TODO: Test Location Services
    // TODO: Handle no permissions granted rather than just logging

    // Create Location Client and Request
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest

    // Create binder to allow for fragments etc to bind to the service
    inner class LocationBinder : Binder() {
        fun getService(): LocationService = this@LocationService
    }
    private val binder: IBinder = LocationBinder()

    // Live data for other components that require location
    private val locationLiveData = MutableLiveData<Location>()
    private val lastKnownLocationLiveData = MutableLiveData<Location?>()

    // Bind service
    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    // LocationCallback to receive location updates used to update locations
    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            for (location in locationResult.locations) {
                locationLiveData.postValue(location)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        // Setup location client and request with basic intervals for recieving updates
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        startLocationTracking()
        getLastLocation()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startLocationTracking()
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        stopLocationTracking()
    }

    // make public function to get live data
    fun getLiveLocationData(): LiveData<Location> = locationLiveData

    // make public function to get live last known location
    fun getLastKnownLocationLiveData(): LiveData<Location?> = lastKnownLocationLiveData


    // Use fused client to start tracking
    private fun startLocationTracking() {
        try {
            locationRequest = LocationRequest.Builder(5000).setPriority(Priority.PRIORITY_HIGH_ACCURACY).setMinUpdateIntervalMillis(1000).build()
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        } catch (e: SecurityException) {
            // Handle exception
            Log.e("LocationService", "Failed to request location updates", e)
        }
    }
    // Use fused client to start tracking
    private fun getLastLocation(){
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                lastKnownLocationLiveData.postValue(location)
            }.addOnFailureListener {
                lastKnownLocationLiveData.postValue(null)
            }
        } catch (e: SecurityException) {
            // Handle exception
            Log.e("LocationService", "Failed to request last location updates", e)
        }
    }

    private fun stopLocationTracking() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

}