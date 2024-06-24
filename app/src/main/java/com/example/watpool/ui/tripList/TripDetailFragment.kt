package com.example.watpool.ui.tripList

import android.annotation.SuppressLint
import com.example.watpool.R
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController

class TripDetailFragment : Fragment() {

    private lateinit var tripId: String

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

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Find views
        val tripIdTextView = view.findViewById<TextView>(R.id.trip_id_text_view)
        val backButton = view.findViewById<Button>(R.id.back_button)

        // Set the trip ID text
        tripIdTextView.text = "Trip ID: ${tripId}"

        backButton.setOnClickListener {
            // Navigate back to previous fragment
            val action = TripDetailFragmentDirections.actionTripDetailFragmentToTripListFragment()
            findNavController().navigate(action)
        }
    }
}
