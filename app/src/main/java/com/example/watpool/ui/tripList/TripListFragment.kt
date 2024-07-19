package com.example.watpool.ui.tripList

import android.content.ComponentName
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.watpool.R
import com.example.watpool.services.FirebaseService
import com.example.watpool.services.models.TripConfirmationDetails
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton

class TripListFragment :  BottomSheetDialogFragment() {

    private lateinit var tripAdapter: TripAdapter
    private val tripListViewModel: TripListViewModel by viewModels()

    private var firebaseService: FirebaseService? = null
    private var firebaseBound: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_trip_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize RecyclerView and Adapter
        tripAdapter = TripAdapter(emptyList()) { trip ->
            if(dialog == null){
                val action = TripListFragmentDirections.actionTripListFragmentToIsCurrentTripFragment(trip.id)
                findNavController().navigate(action)
            } else {
                showTripDetailsSheet(trip.id)
            }
        }

        val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = tripAdapter

        // Observe the LiveData from the ViewModel
        tripListViewModel.trips.observe(viewLifecycleOwner, Observer { trips ->
            tripAdapter.updateTrips(trips)
        })

        // Bind to FirebaseService
        val serviceIntent = Intent(requireContext(), FirebaseService::class.java)
        requireContext().bindService(serviceIntent, firebaseConnection, Context.BIND_AUTO_CREATE)
    }

    private fun showTripDetailsSheet(tripId: String) {
        val tripDetailFragment = TripDetailFragment()
        tripDetailFragment.show(childFragmentManager, tripId)
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

            // Fetch trips once the service is connected
            firebaseService?.let {
                //val riderId = it.currentUser()
                val riderId = "user_id_1"
                tripListViewModel.fetchConfirmedTrips(it, riderId)
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            firebaseBound = false
        }
    }
}