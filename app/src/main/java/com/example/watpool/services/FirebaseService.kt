package com.example.watpool.services
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import com.example.watpool.services.firebase_services.FirebaseAuthService
import com.example.watpool.services.firebase_services.FirebaseCoordinateService
import com.example.watpool.services.firebase_services.FirebaseDriverService
import com.example.watpool.services.interfaces.AuthService
import com.example.watpool.services.interfaces.CoordinateService
import com.example.watpool.services.interfaces.DriverService
import com.example.watpool.services.models.Coordinate
import com.google.firebase.Firebase
import com.google.firebase.auth.AuthResult
import com.google.firebase.database.database
import com.google.android.gms.tasks.Task
import com.google.firebase.database.DataSnapshot
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.QuerySnapshot


// TODO: Add helper functions here to interact with the DB
class FirebaseService : Service() {
    private lateinit var authService: AuthService
    private lateinit var driverService: DriverService
    private lateinit var coordinateService: CoordinateService

    inner class FirebaseBinder : Binder() {
        fun getService() = this@FirebaseService
    }

    private val binder: IBinder = FirebaseBinder()

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onCreate() {
        super.onCreate()
        authService = FirebaseAuthService()
        driverService = FirebaseDriverService()
        coordinateService = FirebaseCoordinateService()
    }

    fun signIn(email: String, password: String): Task<AuthResult> {
        return authService.signIn(email, password)
    }

    fun signUp(email: String, password: String): Task<AuthResult> {
        return authService.signUp(email, password)
    }

    fun fetchCoordinatesByDriverId(driverId: String): Task<QuerySnapshot> {
        return coordinateService.fetchCoordinatesByDriverId(driverId)
    }

    fun fetchCoordinatesByLocation(latitude: Double, longitude: Double, radiusInKm: Double): Task<MutableList<DocumentSnapshot>> {
        return coordinateService.fetchCoordinatesByLocation(latitude, longitude, radiusInKm)
    }

    fun addCoordinate(driverId: String, latitude: Double, longitude: Double): Task<DocumentReference> {
        return coordinateService.addCoordinate(driverId, latitude, longitude)
    }

}