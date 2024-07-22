package com.example.watpool.ui.tripDetails

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import com.example.watpool.R
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import android.graphics.Color
import android.os.IBinder
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.example.watpool.services.FirebaseService
import com.example.watpool.ui.tripList.TripListViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import com.google.maps.android.PolyUtil
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.example.watpool.services.models.TripConfirmationDetails
import com.example.watpool.services.models.Trips
import com.example.watpool.ui.tripList.TripListFragmentDirections
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

class TripDetailFragment : Fragment(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var tripId: String
    private lateinit var driverName: String
    private lateinit var tripDate: String
    private lateinit var to: String
    private lateinit var from: String
    private val tripDetailViewModel: TripDetailViewModel by viewModels()
    private var startLatLng: LatLng = LatLng(43.4698361, -80.5164223)
    private var endLatLng: LatLng = LatLng(43.4698361, -80.5164223)

    private var firebaseService: FirebaseService? = null
    private var firebaseBound: Boolean = false

    private var mapReady: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            tripId = TripDetailFragmentArgs.fromBundle(it).tripId
        }
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_trip_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = childFragmentManager
            .findFragmentById(R.id.trip_detail_map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        view.findViewById<Button>(R.id.leave_trip_button).setOnClickListener {
            lifecycleScope.launch {
                val riderId = firebaseService?.currentUser() ?: "Unknown"
                firebaseService?.deleteTripConfirmation(tripId, riderId)
                val action = TripDetailFragmentDirections.actionTripDetailFragmentToTripListFragment()
                findNavController().navigate(action)
            }
        }
    }

    private fun getCoordinates() {
        Log.i("OngoingTripFragment", tripId)
        val trip = firebaseService?.fetchTripsByTripsId(listOf(tripId))
        trip?.addOnSuccessListener { documentSnapshots ->
            val document = documentSnapshots[0]
            val documentData = document.data
            if (documentData != null) {
                val startLatLngData = documentData["startLatLng"] as Map<*, *>
                val endLatLngData = documentData["endLatLng"] as Map<*, *>
                startLatLng = LatLng(startLatLngData["latitude"] as Double, startLatLngData["longitude"] as Double)
                endLatLng = LatLng(endLatLngData["latitude"] as Double, endLatLngData["longitude"] as Double)
                to = documentData["to"] as String
                view?.findViewById<TextView>(R.id.trip_to)?.text = "To: $to"
                from = documentData["from"] as String
                view?.findViewById<TextView>(R.id.trip_from)?.text = "From: $from"
                tripDate = documentData["trip_date"] as String
                view?.findViewById<TextView>(R.id.trip_date)?.text = "Date: $tripDate"
                firebaseService?.fetchUsersById(documentData["driver_id"] as String)?.addOnSuccessListener { querySnapshot ->
                    val tempDriverName = querySnapshot.documents[0].getString("Name")
                    driverName = tempDriverName?:"Unknown"
                    view?.findViewById<TextView>(R.id.driver_name)?.text = "Driver: $driverName"
                }
            }

            Log.i("TripDetailFragment", "HERER SJDLKFJKSDLFJ")
            while (!mapReady) {

            }
            draw()

        }?.addOnFailureListener {
            Toast.makeText(requireContext(), "Error Finding Trip", Toast.LENGTH_SHORT).show()
        }
    }

    private fun draw() {
        // Observe the LiveData from the ViewModel
        Log.i("TripDetailFragment", "In here by the way")
        Log.i("TripDetailFragment", "This is the starting coordinate $startLatLng")

        // Add markers for start and end points
        mMap.addMarker(MarkerOptions().position(startLatLng).title("Start Location"))
        mMap.addMarker(MarkerOptions().position(endLatLng).title("End Location"))

        // Move the camera to the start location
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(startLatLng, 10f))

        // Fetch the route and draw it on the map
        fetchRoute(startLatLng, endLatLng)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mapReady = true
        mMap = googleMap
    }

    private fun fetchRoute(start: LatLng, end: LatLng) {
        val apiKey = "AIzaSyCOUm5uLrFqX49t3YBxHsMoxn4ZyspxCBM"
        val url = "https://maps.googleapis.com/maps/api/directions/json?origin=${start.latitude},${start.longitude}&destination=${end.latitude},${end.longitude}&key=$apiKey"

        Log.i("TripDetailFragment", "Sending request")
        val request = Request.Builder().url(url).build()
        val client = OkHttpClient()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                Log.i("TripDetailFragment", "In response")
                val jsonData = response.body?.string()
                val jsonObject = JSONObject(jsonData)
                val routes = jsonObject.getJSONArray("routes")
                val polyline = routes.getJSONObject(0)
                    .getJSONObject("overview_polyline")
                    .getString("points")

                activity?.runOnUiThread {
                    drawPolyline(polyline)
                }
            }
        })
    }

    private fun drawPolyline(encodedPolyline: String) {
        val polylineOptions = PolylineOptions()
            .addAll(PolyUtil.decode(encodedPolyline))
            .width(10f)
            .color(Color.BLUE)
            .geodesic(true)

        mMap.addPolyline(polylineOptions)
    }

    override fun onStart() {
        super.onStart()
        val serviceIntent = Intent(requireContext(), FirebaseService::class.java)
        requireContext().bindService(serviceIntent, firebaseConnection, Context.BIND_AUTO_CREATE)
        firebaseBound = true
    }

    // Stop location service if still bound
    override fun onStop() {
        super.onStop()
        if (firebaseBound){
            requireContext().unbindService(firebaseConnection)
            firebaseBound = false
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (firebaseBound) {
            requireContext().unbindService(firebaseConnection)
            firebaseBound = false
        }
    }

    private val firebaseConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as FirebaseService.FirebaseBinder
            firebaseService = binder.getService()
            firebaseBound = true

            getCoordinates()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            firebaseBound = false
        }
    }
}

