package com.example.watpool.ui.postingList

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.navigation.fragment.findNavController
import com.example.watpool.R
import com.example.watpool.ui.tripList.TripDetailFragmentDirections

import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class PostingDetailFragment : BottomSheetDialogFragment() {

    private lateinit var postingId: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        if(dialog == null){
            arguments?.let {
               // postingId = TripDetailFragmentArgs.fromBundle(it).tripId
            }
        } else {
            tag?.let {
                postingId = it
            }
        }
        return inflater.inflate(R.layout.fragment_posting_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Find views
        val tripIdTextView = view.findViewById<TextView>(R.id.trip_id_text_view)
        val backButton = view.findViewById<Button>(R.id.back_button)

        // Set the trip ID text
        tripIdTextView.text = "Trip ID: ${postingId}"

        backButton.setOnClickListener {
                dismiss()
        }
    }
}