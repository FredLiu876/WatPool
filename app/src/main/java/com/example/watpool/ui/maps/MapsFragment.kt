package com.example.watpool.ui.maps

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import androidx.fragment.app.Fragment

import android.os.Bundle
import android.os.IBinder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.Observer
import com.example.watpool.R
import com.example.watpool.services.LocationService
import com.example.watpool.databinding.FragmentMapsBinding
import com.example.watpool.ui.safetyBottomSheet.SafetyBottomSheetDialog

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.button.MaterialButton
import java.io.IOException
import java.util.Locale

class MapsFragment : Fragment(), OnMapReadyCallback {

    // TODO: Create better map initilization to display user current location
    // TODO: Create button to recenter map on user location
    // TODO: Ability to create markers for user and other functionality

    // TODO: Cleanup map initilization and location usage

    private var _binding: FragmentMapsBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    // Search view for maps
    private lateinit var searchView : SearchView;

    // Create google map object to be used for modification within fragment
    // Dont show map until location is set
    private var map : GoogleMap? = null
    private var isMapReady = false
    private var userLocation: Location? = null

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
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Bottom of map safety sheet bindings and listener
        val buttonOpenBottomSheet: MaterialButton = binding.btnInfo
        buttonOpenBottomSheet.setOnClickListener {
            val bottomSheet = SafetyBottomSheetDialog()
            bottomSheet.show(requireActivity().supportFragmentManager, "safetyBottomSheet")
        }

        // Recenter button bindings and listener
        val recenterButton: MaterialButton = binding.btnRecenter
        recenterButton.setOnClickListener {
            userLocation?.let { moveMapCamera(it) }
        }

        // Top search bar binding and listener
        searchView = binding.mapSearchView
        // allow for clicking anywhere on search view to search
        searchView.isIconified = false
        searchView.setOnQueryTextListener(object: SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let { locationSearch(it) }
                return false
            }
            // TODO: possibly implement recommendations based on search results
            override fun onQueryTextChange(newText: String?): Boolean {
                return false
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
            map?.isBuildingsEnabled = true
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
    }
    // Stop location service if still bound
    override fun onDestroyView() {
        super.onDestroyView()
        if (locationBound) {
            requireContext().unbindService(locationConnection)
            locationBound = false
        }
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

    // Search for location using geocoder
    private fun locationSearch(location: String){
        // locale.getdefault gets users deafult language and other preferences
        // Use requireContext() to ensure context is not null
        val geocoder = Geocoder(requireContext(), Locale.getDefault())
        try {
            // Using deprecated function because newer version does not work with out min sdk setup
            // If min sdk updated replace with geocoder listener setup in android documentation
            val addressList = geocoder.getFromLocationName(location, 1)
            if (!addressList.isNullOrEmpty()) {
                val address = addressList[0]
                val latLng = LatLng(address.latitude, address.longitude)
                // Clear map so that old markers dont remain when moving across the map
                map?.clear()
                map?.addMarker(MarkerOptions().position(latLng).title(location))
                map?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
            } else {
                Toast.makeText(requireContext(), "Location not found", Toast.LENGTH_SHORT).show()
            }
        } catch (e: IOException) {
            Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}