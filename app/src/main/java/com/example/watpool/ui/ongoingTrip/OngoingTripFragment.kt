package com.example.watpool.ui.ongoingTrip

import android.Manifest
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
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
import com.example.watpool.services.LocationService
import com.example.watpool.ui.safetyBottomSheet.SafetyBottomSheetDialog
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.material.button.MaterialButton
import org.json.JSONObject


class OngoingTripFragment : Fragment(), OnMapReadyCallback {

    private lateinit var tripId: String
    private var tripOrigin: LatLng = LatLng(43.4698361, -80.5164223)
    private var tripDestination: LatLng = LatLng(43.4698361, -80.5164223)
    private var _binding: FragmentOngoingTripBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private val mapsViewModel: OngoingTripViewModel by viewModels()

    // off-route constants
    private val checkInterval: Long = 5000 // 5 seconds
    private val offRouteThreshold: Double = 50.0 // 50 meters
    // off-route runner
    private val offRouteHandler = Handler(Looper.getMainLooper())
    private var checkOffRouteRunner: Runnable? = null
    private var isPopupDisplayed = false

    // Create google map object to be used for modification within fragment
    // Don't show map until location is set
    private var map : GoogleMap? = null
    private var isMapReady = false
    private var isCenteredOnUser = true

    // Stored locations
    private var userLocation: Location? = null
    private var currentPoints = ArrayList<LatLng>()

    // Create location service and bool value for to know when to bind it and clean up
    private var locationService: LocationService? = null
    private var locationBound: Boolean = false

    private fun moveMapCamera(location: Location){
        map?.let { googleMap ->
            val latLng = LatLng(location.latitude, location.longitude)
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
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
        arguments?.getParcelable<LatLng>("origin")?.let{
            tripOrigin = it
        }
        arguments?.getParcelable<LatLng>("destination")?.let{
            tripDestination = it
        }

        // Bottom of map safety sheet bindings and listener
        val buttonOpenBottomSheet: MaterialButton = binding.btnSafety
        buttonOpenBottomSheet.setOnClickListener {
            val bottomSheet = SafetyBottomSheetDialog()
            bottomSheet.show(requireActivity().supportFragmentManager, "safetyBottomSheet")
        }

        val buttonFindRoute: MaterialButton = binding.findRoute
        buttonFindRoute.setOnClickListener {
            // Fetch and draw the route once the map is ready
            val origin =  map?.cameraPosition?.target// Example origin
            origin?.let {
                val originLatLng = LatLng(origin.latitude, origin.longitude)
                calculateRoute(originLatLng, tripDestination)
            }
            startOffRouteCheck()
        }

        mapsViewModel.directions.observe(viewLifecycleOwner, Observer { directions ->
            directions?.let {
                drawRoute(it)
            }
        })

        initializeMap()
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
                true
            }
            isMapReady = true
            userLocation?.let { moveMapCamera(it) }
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
    }
    // Stop location service if still bound
    override fun onStop() {
        super.onStop()
        if (locationBound) {
            requireContext().unbindService(locationConnection)
            locationBound = false
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

    // Register for location permissions result
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            // Bind to LocationService, uses Bind_Auto_create to create service if it does not exist
            // TODO: Take binding out of specific functions and make it its own private function
            val serviceIntent = Intent(requireContext(), LocationService::class.java)
            requireContext().bindService(serviceIntent, locationConnection, Context.BIND_AUTO_CREATE)
        } else {
            Toast.makeText(requireContext(), "Location permissions are required", Toast.LENGTH_SHORT).show()
        }
    }

    // TODO: Possibly make permission manager so dont need to handle it all in one fragment
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

    private fun drawRoute(directions: String) {
        try {
            val jsonObject = JSONObject(directions)
            val routes = jsonObject.getJSONArray("routes")
            val points = ArrayList<LatLng>()
            val polylineOptions = PolylineOptions()

            for (i in 0 until routes.length()) {
                val route = routes.getJSONObject(i)
                val legs = route.getJSONArray("legs")

                for (j in 0 until legs.length()) {
                    val leg = legs.getJSONObject(j)
                    val steps = leg.getJSONArray("steps")

                    for (k in 0 until steps.length()) {
                        val step = steps.getJSONObject(k)
                        val polyline = step.getJSONObject("polyline").getString("points")
                        val decodedPoints = decodePolyline(polyline)
                        points.addAll(decodedPoints)
//                        Legacy route drawn was too rough
//                        val point = step.getJSONObject("start_location")
//                        val lat: Double = point.getDouble("lat")
//                        val lng: Double = point.getDouble("lng")
//                        val position = LatLng(lat, lng)
//                        points.add(position)
                    }
                }
            }
            polylineOptions.addAll(points)
            polylineOptions.width(12f)
            polylineOptions.geodesic(true)
            map?.addPolyline(polylineOptions)
            currentPoints = points
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

    // check for user going off-route
    private fun startOffRouteCheck() {
        checkOffRouteRunner = Runnable {
            userLocation?.let { currentLocation ->
                if (isOffRoute(currentLocation)) {
                    Log.d("OffRouteCheck", "User is off route!")
                    // show offroute pop up
                    showOffRoutePopup()
                } else {
                    Log.d("OffRouteCheck", "User is on route!")
                }
            }
            offRouteHandler.postDelayed(checkOffRouteRunner!!, checkInterval)
        }
        offRouteHandler.post(checkOffRouteRunner!!)
    }

    private fun isOffRoute(userLocation: Location): Boolean {
        if (currentPoints.isEmpty()) return false
        for (routePoint in currentPoints) {
            val distance = FloatArray(1)
            Location.distanceBetween(
                userLocation.latitude, userLocation.longitude,
                routePoint.latitude, routePoint.longitude,
                distance
            )
            if (distance[0] <= offRouteThreshold) {
                return false
            }
        }
        return true
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
            (LatLng(origin.latitude, origin.longitude)),
            destination
        )
        map?.clear()
        map?.addMarker(MarkerOptions().position(origin))
        map?.addMarker(MarkerOptions().position(destination))
        map?.let { googleMap ->
            val latLng = LatLng(destination.latitude, destination.longitude)
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 14f))
        }
    }


}