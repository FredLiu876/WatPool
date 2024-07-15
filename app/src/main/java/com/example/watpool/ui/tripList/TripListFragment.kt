package com.example.watpool.ui.tripList

import com.example.watpool.R
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.watpool.services.TripListService
import com.example.watpool.services.TripConfirmation
import com.example.watpool.services.FirebaseService
import kotlinx.coroutines.launch

class TripListFragment : Fragment() {

    private lateinit var tripAdapter: TripAdapter
    private lateinit var trips: List<TripConfirmation>
    private lateinit var tripListService: TripListService
    private lateinit var firebaseService: FirebaseService

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_trip_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tripListService = TripListService()
        firebaseService = FirebaseService()
        fetchConfirmedTripsForUser()

        // Initialize RecyclerView and Adapter
        tripAdapter = TripAdapter(trips) { trip ->
            val action = TripListFragmentDirections.actionTripListFragmentToIsCurrentTripFragment(trip.id)
            findNavController().navigate(action)
        }

        val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = tripAdapter
    }

    private fun fetchConfirmedTripsForUser() {
        // Launch a coroutine in the view's lifecycle scope
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val riderId = firebaseService.currentUser()
                // Call the suspend function
                trips = tripListService.fetchAllConfirmedTripsByRiderId(riderId)
            } catch (e: Exception) {
                // Handle any errors
                e.printStackTrace()
            }
        }
    }
}