package com.example.watpool.ui.create_trip

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.fragment.app.viewModels
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.example.watpool.databinding.FragmentCreateTripBinding
import java.util.Calendar

class create_trip : Fragment() {

    companion object {
        fun newInstance() = create_trip()
    }

    private var _binding: FragmentCreateTripBinding? = null
    private val binding get() = _binding!!
    private val viewModel: CreateTripViewModel by viewModels()

    private lateinit var pickDateBtn: Button
    private lateinit var selectedDateTV: TextView
    private lateinit var pickTimeBtn: Button
    private lateinit var selectedTimeTV: TextView
    private lateinit var createTripBtn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // TODO: Use the ViewModel
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateTripBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (activity as AppCompatActivity).supportActionBar?.hide()

        // Initialize the button and text view from the binding
        pickDateBtn = binding.idBtnPickDate
        selectedDateTV = binding.idTVSelectedDate
        pickTimeBtn = binding.idBtnPickTime
        selectedTimeTV = binding.idTVSelectedTime
        createTripBtn = binding.createTripBtn

        // Observe ViewModel LiveData and update UI accordingly
        viewModel.pickupLocation.observe(viewLifecycleOwner, Observer {
            binding.pickupLocation.setText(it)
        })
        viewModel.destination.observe(viewLifecycleOwner, Observer {
            binding.destination.setText(it)
        })
        viewModel.selectedDate.observe(viewLifecycleOwner, Observer {
            binding.idTVSelectedDate.text = it
        })
        viewModel.selectedTime.observe(viewLifecycleOwner, Observer {
            binding.idTVSelectedTime.text = it
        })
        viewModel.numAvailableSeats.observe(viewLifecycleOwner, Observer {
            binding.numAvailableSeats.setText(it)
        })

        // Set onClickListeners
        pickDateBtn.setOnClickListener {
            val c = Calendar.getInstance()
            val year = c.get(Calendar.YEAR)
            val month = c.get(Calendar.MONTH)
            val day = c.get(Calendar.DAY_OF_MONTH)
            val datePickerDialog = DatePickerDialog(
                requireContext(),
                { _, year, monthOfYear, dayOfMonth ->
                    val date = "$dayOfMonth-${monthOfYear + 1}-$year"
                    viewModel.setSelectedDate(date)
                },
                year, month, day
            )
            datePickerDialog.show()
        }

        pickTimeBtn.setOnClickListener {
            val c = Calendar.getInstance()
            val hour = c.get(Calendar.HOUR_OF_DAY)
            val minute = c.get(Calendar.MINUTE)
            val timePickerDialog = TimePickerDialog(
                requireContext(),
                { _, selectedHour, selectedMinute ->
                    val time = String.format("%02d:%02d", selectedHour, selectedMinute)
                    viewModel.setSelectedTime(time)
                },
                hour, minute, true
            )
            timePickerDialog.show()
        }

        createTripBtn.setOnClickListener {
            // Store the EditText values in the ViewModel
            viewModel.setPickupLocation(binding.pickupLocation.text.toString())
            viewModel.setDestination(binding.destination.text.toString())
            viewModel.setNumAvailableSeats(binding.numAvailableSeats.text.toString())

            // Perform the actions to create the trip

            viewModel.saveTrip()
            viewModel.onCreateTrip(findNavController())
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
