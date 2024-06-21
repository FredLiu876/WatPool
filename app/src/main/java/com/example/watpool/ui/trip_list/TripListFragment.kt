package com.example.watpool.ui.trip_list

import com.example.watpool.R
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class TripListFragment : Fragment() {

    private lateinit var tripAdapter: TripAdapter
    private lateinit var trips: List<Trip>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_trip_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Example data
        trips = listOf(
            Trip("1", "Costco (Waterloo)", "ICON", "06/19/2024", "Fred"),
            Trip("2", "Conestoga Mall", "ICON", "06/19/2024", "Fred"),
            Trip("3", "ICON", "Square One", "06/19/2024", "Fred")
        )

        // Initialize RecyclerView and Adapter
        tripAdapter = TripAdapter(trips) { trip ->
            val action = TripListFragmentDirections.actionTripListFragmentToTripDetailFragment(trip.id)
            findNavController().navigate(action)
        }

        val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = tripAdapter
    }
}