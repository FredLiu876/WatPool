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

class IsCurrentTripFragment : Fragment() {

    private lateinit var tripId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            tripId = IsCurrentTripFragmentArgs.fromBundle(it).tripId
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_is_current_trip, container, false)
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Find views
        val yesButton = view.findViewById<Button>(R.id.buttonYes)
        val noButton = view.findViewById<Button>(R.id.buttonNo)

        yesButton.setOnClickListener {
            // Navigate back to previous fragment
            val action = IsCurrentTripFragmentDirections.actionIsCurrentTripFragmentToOngoingTripFragment(tripId)
            findNavController().navigate(action)
        }
        noButton.setOnClickListener {
            val action = IsCurrentTripFragmentDirections.actionIsCurrentTripFragmentToTripDetailFragment(tripId)
            findNavController().navigate(action)
        }
    }
}
