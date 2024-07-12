package com.example.watpool.ui.create_trip

import PlacesFragment
import PlacesViewModel
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
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModelProvider
import com.example.watpool.R

class create_trip : Fragment() {

    companion object {
        fun newInstance() = create_trip()
    }

    private var _binding: FragmentCreateTripBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CreateTripViewModel by viewModels()
    private lateinit var placesViewModel: PlacesViewModel

    private lateinit var pickDateBtn: Button
    private lateinit var selectedDateTV: TextView
    private lateinit var pickTimeBtn: Button
    private lateinit var selectedTimeTV: TextView
    private lateinit var createTripBtn: Button

    private lateinit var pickupSearchView: SearchView
    private lateinit var pickupPlacesFragment: PlacesFragment
    private lateinit var destinationSearchView: SearchView
    private lateinit var destinationPlacesFragment: PlacesFragment
    private var isSelectionInProgress = false



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateTripBinding.inflate(inflater, container, false)

        if (savedInstanceState == null) {
            pickupPlacesFragment = PlacesFragment()
            destinationPlacesFragment = PlacesFragment()
            childFragmentManager.commit {
                add(R.id.places_fragment_container, pickupPlacesFragment)
                add(R.id.places_fragment_container2, destinationPlacesFragment)
            }
        } else {
            pickupPlacesFragment = childFragmentManager.findFragmentById(R.id.places_fragment_container) as PlacesFragment
            destinationPlacesFragment = childFragmentManager.findFragmentById(R.id.places_fragment_container2) as PlacesFragment
        }

        placesViewModel = ViewModelProvider(requireActivity()).get(PlacesViewModel::class.java)

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

        pickupSearchView = binding.pickupLocation
        pickupSearchView.setQueryHint("Enter pickup location");
        pickupSearchView.isIconified = false

        destinationSearchView = binding.destination
        destinationSearchView.setQueryHint("Enter destination");
        destinationSearchView.isIconified = false


        placesViewModel.getSelectedPrediction().observe(viewLifecycleOwner, Observer { prediction ->
            if (pickupSearchView.hasFocus()) {
                isSelectionInProgress = true
                pickupSearchView.setQuery(prediction, false)
                pickupPlacesFragment.clearList()
                isSelectionInProgress = false
            }
            if (destinationSearchView.hasFocus()) {
                isSelectionInProgress = true
                destinationSearchView.setQuery(prediction, false)
                destinationPlacesFragment.clearList()
                isSelectionInProgress = false
            }
        })

        pickupSearchView.setOnQueryTextListener(object: SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(query: String?): Boolean {
                pickupPlacesFragment.clearList()
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                // Saves pickup location if any change is made to the field
                newText?.let { viewModel.setPickupLocation(it) }

                if (!isSelectionInProgress) {
                    if (!newText.isNullOrEmpty()) {
                        placesViewModel.getAutocompletePredictions(newText).observe(viewLifecycleOwner) { predictions ->
                            pickupPlacesFragment.updateList(predictions)
                        }
                    } else {
                        pickupPlacesFragment.clearList()
                    }
                }
                return true
            }
        })

        destinationSearchView.setOnQueryTextListener(object: SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(query: String?): Boolean {
                destinationPlacesFragment.clearList()
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                // Saves destination location if any change is made to the field
                newText?.let { viewModel.setDestination(it) }

                if (!isSelectionInProgress) {
                    if (!newText.isNullOrEmpty()) {
                        placesViewModel.getAutocompletePredictions(newText).observe(viewLifecycleOwner) { predictions ->
                            destinationPlacesFragment.updateList(predictions)
                        }
                    } else {
                        destinationPlacesFragment.clearList()
                    }
                }
                return true
            }
        })

        // Observe ViewModel LiveData and update UI accordingly
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
            viewModel.setNumAvailableSeats(binding.numAvailableSeats.text.toString())
            viewModel.saveTrip()
            viewModel.onCreateTrip(findNavController())
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}