package com.example.watpool.ui.ongoingTrip

import android.Manifest
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.example.watpool.R
import com.example.watpool.databinding.FragmentOngoingTripBinding
import com.example.watpool.services.FirebaseService
import com.example.watpool.services.LocationService
import com.example.watpool.services.models.Trips
import com.example.watpool.ui.safetyBottomSheet.SafetyBottomSheetDialog
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.material.button.MaterialButton
import com.google.android.material.textview.MaterialTextView
import kotlin.math.max


class OngoingTripFragment : Fragment(), OnMapReadyCallback {

    private lateinit var tripId: String
    private var tripOrigin: LatLng = LatLng(43.470630, -80.541380)
    private var tripDestination: LatLng = LatLng(43.4698361, -80.5164223)
    private var _binding: FragmentOngoingTripBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private val mapsViewModel: OngoingTripViewModel by viewModels()

    // off-route constants
    private val checkInterval: Long = 5000 // 5 seconds
    private val offRouteThreshold: Double = 50.0 // 50 meters
    private val offRouteCountLimit: Int = 3
    // off-route variables
    private val offRouteHandler = Handler(Looper.getMainLooper())
    private var checkOffRouteRunner: Runnable? = null
    private var isPopupDisplayed = false
    private var offRouteCount = 0

    // Create google map object to be used for modification within fragment
    // Don't show map until location is set
    private var map : GoogleMap? = null
    private var isMapReady = false
    private var isCenteredOnUser = true

    // Stored locations
    private var userLocation: Location? = null
    private var currentPoints = ArrayList<LatLng>()
    private var distanceAndDuration = ArrayList<Pair<Double,Double>>()
    private var distanceRemain = 0.0
    private var durationRemain = 0.0
    private lateinit var textViewDistanceRemain: MaterialTextView
    private lateinit var textViewDurationRemain: MaterialTextView


    // Create location service and bool value for to know when to bind it and clean up
    private var locationService: LocationService? = null
    private var locationBound: Boolean = false

    private var firebaseService: FirebaseService? = null
    private var firebaseBound: Boolean = false

    private fun moveMapCamera(location: Location){
        map?.let { googleMap ->
            val latLng = LatLng(location.latitude, location.longitude)
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
        }
    }
    private fun getCoordinates(tripsId: String) {
        val trip = firebaseService?.fetchTripsByTripsId(listOf(tripsId))
        trip?.addOnSuccessListener { documentSnapshots ->
            for (document in documentSnapshots) {
                val foundTrip = document.toObject(Trips::class.java)
                if (foundTrip != null) {
                    tripOrigin = foundTrip.startLatLng
                    tripDestination = foundTrip.startLatLng
                }
            }
        }?.addOnFailureListener {
            Toast.makeText(requireContext(), "Error Finding Trip", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentOngoingTripBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Get arguments
        arguments?.getString("tripId")?.let{
            tripId = it
        }

        // get views
        textViewDistanceRemain = binding.textDistanceRemain

        // Bottom of map safety sheet bindings and listener
        val buttonSafety: MaterialButton = binding.btnSafety
        buttonSafety.setOnClickListener {
            val bottomSheet = SafetyBottomSheetDialog()
            bottomSheet.show(requireActivity().supportFragmentManager, "safetyBottomSheet")
        }

        val buttonStartTrip: MaterialButton = binding.findRoute
        buttonStartTrip.setOnClickListener {
            // Fetch and draw the route once the map is ready
            val results = FloatArray(1)
            userLocation?.let { loc ->
                Location.distanceBetween(
                    loc.latitude, loc.longitude,
                    tripOrigin.latitude, tripOrigin.longitude,
                    results
                )
            }
            if (results[0] <= offRouteThreshold) {
                calculateRoute(tripOrigin, tripDestination)
                startOffRouteCheck()
                // make visible and invisible
                textViewDistanceRemain.visibility = View.VISIBLE
                buttonSafety.visibility = View.VISIBLE
                buttonStartTrip.visibility = View.GONE
            } else {
                AlertDialog.Builder(requireContext())
                    .setTitle("Warning")
                    .setMessage("You are not within $offRouteThreshold meters of trip start location. Please move closer to the start location shown on map.")
                    .setPositiveButton(android.R.string.ok, null)
                    .show()
            }
        }

        mapsViewModel.routePoints.observe(viewLifecycleOwner, Observer { routePoints ->
            routePoints?.let {
                drawRoute(it)
            }
        })
        mapsViewModel.distanceDuration.observe(viewLifecycleOwner, Observer { distanceDuration ->
            distanceDuration?.let {
                distanceAndDuration = it
            }
        })

        initializeMap()

        getCoordinates(tripId)
        return root
    }

    private fun initializeMap(){
        checkLocationPermissions()
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync { googleMap ->
            map = googleMap
            googleMap.isBuildingsEnabled = true
            try {
                googleMap.isMyLocationEnabled = true
            } catch (e: SecurityException) {
                Log.e("Location disabled", e.toString())
            }
            googleMap.setOnCameraMoveStartedListener { reason ->
                if (reason == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE) {
                    isCenteredOnUser = false
                }
            }
            googleMap.setOnMyLocationButtonClickListener {
                isCenteredOnUser = true
                userLocation?.let { location ->
                    val userLatLng = LatLng(location.latitude, location.longitude)
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 15f))
                }
                true
            }
            isMapReady = true
            userLocation?.let { moveMapCamera(it) }
            // add trip origin marker
            map?.addMarker(MarkerOptions().position(tripOrigin))
            // add destination marker
            map?.addMarker(MarkerOptions().position(tripDestination))
        }
    }
    override fun onMapReady(p0: GoogleMap) {
        map = p0
        checkLocationPermissions()
    }
    override fun onStart() {
        super.onStart()
        if(arePermissionsGranted()){
            // Bind to LocationService, uses Bind_Auto_create to create service if it does not exist
            val serviceIntent = Intent(requireContext(), LocationService::class.java)
            requireContext().bindService(serviceIntent, locationConnection, Context.BIND_AUTO_CREATE)
            locationBound = true
        }
        val serviceIntent = Intent(requireContext(), FirebaseService::class.java)
        requireContext().bindService(serviceIntent, firebaseConnection, Context.BIND_AUTO_CREATE)
        firebaseBound = true
    }
    // Stop location service if still bound
    override fun onStop() {
        super.onStop()
        if (locationBound) {
            requireContext().unbindService(locationConnection)
            locationBound = false
        }
        if (firebaseBound){
            requireContext().unbindService(firebaseConnection)
            firebaseBound = false
        }
        stopOffRouteMonitoring()
    }
    // Stop services if still bound
    override fun onDestroyView() {
        super.onDestroyView()
        if (locationBound) {
            requireContext().unbindService(locationConnection)
            locationBound = false
        }
        if (firebaseBound){
            requireContext().unbindService(firebaseConnection)
            firebaseBound = false
        }
        stopOffRouteMonitoring()
    }

    // TODO: cleanup location connection to use location service data properly
    // Create service connection to get location data to maps
    private val locationConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as LocationService.LocationBinder
            locationService = binder.getService()
            locationBound = true

            // Observe LiveData from the service
            locationService?.getLiveLocationData()?.observe(viewLifecycleOwner, Observer<Location>{
                userLocation = it
                if (isCenteredOnUser) moveMapCamera(it)
            })
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            locationBound = false
        }
    }

    private val firebaseConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as FirebaseService.FirebaseBinder
            firebaseService = binder.getService()
            firebaseBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            firebaseBound = false
        }
    }

    // Register for location permissions result
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            // Bind to LocationService, uses Bind_Auto_create to create service if it does not exist
            val serviceIntent = Intent(requireContext(), LocationService::class.java)
            requireContext().bindService(serviceIntent, locationConnection, Context.BIND_AUTO_CREATE)
        } else {
            Toast.makeText(requireContext(), "Location permissions are required", Toast.LENGTH_SHORT).show()
        }
    }

    // Check if location permissions are granted
    private fun arePermissionsGranted(): Boolean {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    // Request location permissions
    private fun checkLocationPermissions() {
        if (!arePermissionsGranted()) {
            requestPermissionLauncher.launch(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ))
        }
    }

    private fun drawRoute(points: ArrayList<LatLng>) {
        try {
            val polylineOptions = PolylineOptions()
            polylineOptions.addAll(points)
            polylineOptions.width(12f)
            polylineOptions.geodesic(true)
            polylineOptions.color(Color.parseColor("#4285F4"))
            map?.addPolyline(polylineOptions)
            // add origin marker
            map?.addCircle(CircleOptions()
                .center(points[0])
                .radius(5.0)
                .strokeColor(Color.GRAY)
                .fillColor(Color.WHITE)
                .strokeWidth(5f)
            )
            currentPoints = points
        } catch (e: Exception) {
            Log.e("Draw route Exception", e.toString())
        }
    }

    // check for user going off-route
    private fun startOffRouteCheck() {
        checkOffRouteRunner = Runnable {
            userLocation?.let { currentLocation ->
                if (isOffRoute(currentLocation)) {
                    Log.d("OffRouteCheck", "User is off route! Offense $offRouteCount")
                    if (offRouteCount < offRouteCountLimit) {
                        userLocation?.let {
                            val originLatLng = LatLng(userLocation!!.latitude, userLocation!!.longitude)
                            calculateRoute(originLatLng, tripDestination)
                        }
                        offRouteCount++
                    } else {
                        // show offroute pop up
                        showOffRoutePopup()
                        offRouteCount = 0
                    }
                } else {
                    Log.d("OffRouteCheck", "User is on route!")
                    // reduce offroute offense if user stay on track
                    offRouteCount = max(offRouteCount-1, 0)
                }
            }
            offRouteHandler.postDelayed(checkOffRouteRunner!!, checkInterval)
        }
        offRouteHandler.post(checkOffRouteRunner!!)
    }

    private fun isOffRoute(userLocation: Location): Boolean {
        if (currentPoints.isEmpty()) return false
        for (i in currentPoints.indices) {
            val distance = FloatArray(1)
            Location.distanceBetween(
                userLocation.latitude, userLocation.longitude,
                currentPoints[i].latitude, currentPoints[i].longitude,
                distance
            )
            if (distance[0] <= offRouteThreshold) {
                distanceRemain = distanceAndDuration.last().first - distanceAndDuration[i].first
                durationRemain = distanceAndDuration.last().second - distanceAndDuration[i].second
                textViewDistanceRemain.text = formatDistance(distanceRemain)
                return false
            }
        }
        return true
    }
    private fun formatDistance(distanceMeters: Double): String {
        // Convert meters to kilometers
        val distanceKm = distanceMeters / 1000.0
        return getString(R.string.distance_remaining, distanceKm)
    }
    private fun formatDuration(seconds: Double): String {
        val minutes = seconds / 60
        return if (minutes < 60) {
            getString(R.string.duration_remaining_minutes, minutes)
        } else {
            val hours = minutes / 60
            val remainingMinutes = minutes % 60
            getString(R.string.duration_remaining, hours, remainingMinutes)
        }
    }
    private fun stopOffRouteMonitoring() {
        checkOffRouteRunner?.let {
            offRouteHandler.removeCallbacks(it)
            checkOffRouteRunner = null
        }
    }

    // Method to show the popup when offRoute
    private fun showOffRoutePopup() {
        if (isPopupDisplayed) return

        // Inflate the popup layout
        val inflater: LayoutInflater = layoutInflater
        val popupView = inflater.inflate(R.layout.popup_off_route, null)

        // Create the popup dialog
        val dialogBuilder = AlertDialog.Builder(requireContext())
        dialogBuilder.setView(popupView)

        val alertDialog = dialogBuilder.create()
        alertDialog.show()
        isPopupDisplayed = true

        // Get references to the UI components and set listeners
        val buttonRecalculateRoute: MaterialButton = popupView.findViewById(R.id.buttonRecalculateRoute)
        buttonRecalculateRoute.setOnClickListener {
            // Handle route recalculation
            alertDialog.dismiss()
            isPopupDisplayed = false
            userLocation?.let {
                val originLatLng = LatLng(userLocation!!.latitude, userLocation!!.longitude)
                calculateRoute(originLatLng, tripDestination)
            }
        }

        val buttonDial911: MaterialButton = popupView.findViewById(R.id.btn_911)
        buttonDial911.setOnClickListener {
            // Handle 911 call
            alertDialog.dismiss()
            isPopupDisplayed = false
            val callIntent = Intent(Intent.ACTION_DIAL)
            callIntent.data = Uri.parse("tel:123-456-7890")
            startActivity(callIntent)
        }
    }

    // Find route
    private fun calculateRoute(origin: LatLng, destination: LatLng) {
        mapsViewModel.fetchDirections(
            origin,
            destination
        )
        map?.clear()

        // add trip origin marker
        map?.addMarker(MarkerOptions().position(tripOrigin))
        // add destination marker
        map?.addMarker(MarkerOptions().position(destination))
    }


}