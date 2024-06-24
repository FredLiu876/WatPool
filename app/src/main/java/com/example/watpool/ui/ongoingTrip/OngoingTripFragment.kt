package com.example.watpool.ui.ongoingTrip

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
import com.example.watpool.databinding.FragmentOngoingTripBinding
import com.example.watpool.ui.safetyBottomSheet.SafetyBottomSheetDialog
import com.google.android.material.button.MaterialButton

class OngoingTripFragment : Fragment() {

    private lateinit var tripId: String
    private var _binding: FragmentOngoingTripBinding? = null
    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            tripId = OngoingTripFragmentArgs.fromBundle(it).tripId
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentOngoingTripBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val buttonOpenBottomSheet: MaterialButton = binding.btnInfo
        buttonOpenBottomSheet.setOnClickListener {
            val bottomSheet = SafetyBottomSheetDialog()
            bottomSheet.show(requireActivity().supportFragmentManager, "safetyBottomSheet")
        }
        return root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Find views
        val tripIdTextView = view.findViewById<TextView>(R.id.trip_id_text_view)

        // Set the trip ID text
        tripIdTextView.text = "Trip ID: ${tripId}"
    }
}
