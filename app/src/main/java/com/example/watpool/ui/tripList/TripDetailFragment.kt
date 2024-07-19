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
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class TripDetailFragment : BottomSheetDialogFragment() {

    private lateinit var tripId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        if(dialog == null){
            arguments?.let {
                tripId = TripDetailFragmentArgs.fromBundle(it).tripId
            }
        } else {
            tag?.let {
                tripId = it
            }
        }
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
            if(dialog == null){
                val action = TripDetailFragmentDirections.actionTripDetailFragmentToTripListFragment()
                findNavController().navigate(action)
            } else {
                dismiss()
            }
        }
    }
}
