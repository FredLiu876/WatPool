package com.example.watpool.ui.create_trip

import PlacesFragment
import PlacesViewModel
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.res.ColorStateList
import android.graphics.Color
import androidx.fragment.app.viewModels
import android.os.Bundle
import android.os.IBinder
import android.view.ContextThemeWrapper
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import android.widget.ToggleButton
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.example.watpool.databinding.FragmentCreateTripBinding
import java.util.Calendar
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModelProvider
import com.example.watpool.R
import com.example.watpool.services.FirebaseService

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

    private lateinit var recurringSwitch: Switch
    private lateinit var recurringOptionsLayout: LinearLayout
    private lateinit var recurringEndDateBtn: Button
    private lateinit var recurringEndDateTV: TextView
    private val dayToggles = mutableListOf<ToggleButton>()

    private lateinit var pickupSearchView: SearchView
    private lateinit var pickupPlacesFragment: PlacesFragment
    private lateinit var destinationSearchView: SearchView
    private lateinit var destinationPlacesFragment: PlacesFragment
    private var isSelectionInProgress = false

    private var firebaseService: FirebaseService? = null
    private var firebaseBound: Boolean = false



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
                add(R.id.pickup_places_fragment_container, pickupPlacesFragment)
                add(R.id.destination_places_fragment_container, destinationPlacesFragment)
            }
        } else {
            pickupPlacesFragment = childFragmentManager.findFragmentById(R.id.pickup_places_fragment_container) as PlacesFragment
            destinationPlacesFragment = childFragmentManager.findFragmentById(R.id.destination_places_fragment_container) as PlacesFragment
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

        recurringSwitch = binding.recurringSwitch
        updateRecurringSwitchUI(false)

        recurringOptionsLayout = binding.recurringOptionsLayout
        recurringEndDateBtn = binding.recurringEndDateBtn
        recurringEndDateTV = binding.recurringEndDateTV
        dayToggles.addAll(listOf(
            binding.sundayToggle,
            binding.mondayToggle,
            binding.tuesdayToggle,
            binding.wednesdayToggle,
            binding.thursdayToggle,
            binding.fridayToggle,
            binding.saturdayToggle
        ))

        pickupSearchView = binding.pickupLocation
        pickupSearchView.setQueryHint("Enter pickup location");
        pickupSearchView.isIconified = false

        destinationSearchView = binding.destination
        destinationSearchView.setQueryHint("Enter destination");
        destinationSearchView.isIconified = false

        // Ensures each search view has separate queries
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
                ContextThemeWrapper(requireContext(), androidx.appcompat.R.style.Theme_AppCompat_Dialog).apply {
                    theme.applyStyle(R.style.DatePickerDialogTheme, true)
                },
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
                ContextThemeWrapper(requireContext(), androidx.appcompat.R.style.Theme_AppCompat_Dialog).apply {
                    theme.applyStyle(R.style.TimePickerDialogTheme, true)
                },
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
            firebaseService?.let { service ->
                viewModel.saveTrip(service)
            } ?: run {
                Toast.makeText(context, "Firebase service not available", Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.tripCreationStatus.observe(viewLifecycleOwner, Observer { status ->
            Toast.makeText(context, status, Toast.LENGTH_LONG).show()
            if (status.startsWith("Trip created successfully")) {
                viewModel.onCreateTrip(findNavController())
            }

            // for debugging saveTrip
            firebaseService?.let { service ->
                viewModel.fetchTripsForCurrentUser(service)
                viewModel.fetchAllCoordinatesForCurrentUser(service)
            }
        })

        recurringSwitch.setOnCheckedChangeListener { _, isChecked ->
            updateRecurringSwitchUI(isChecked)

            recurringOptionsLayout.visibility = if (isChecked) View.VISIBLE else View.GONE
            viewModel.setIsRecurring(isChecked)
        }


        recurringEndDateBtn.setOnClickListener {
            val c = Calendar.getInstance()
            val year = c.get(Calendar.YEAR)
            val month = c.get(Calendar.MONTH)
            val day = c.get(Calendar.DAY_OF_MONTH)
            val datePickerDialog = DatePickerDialog(
                ContextThemeWrapper(requireContext(), androidx.appcompat.R.style.Theme_AppCompat_Dialog).apply {
                    theme.applyStyle(R.style.DatePickerDialogTheme, true)
                },
                { _, year, monthOfYear, dayOfMonth ->
                    val date = "$dayOfMonth-${monthOfYear + 1}-$year"
                    viewModel.setRecurringEndDate(date)
                    recurringEndDateTV.text = "End Date: $date"
                },
                year, month, day
            )
            datePickerDialog.show()
        }

        //TODO: double check logic for setRecurringDay
        dayToggles.forEachIndexed { index, toggle ->
            toggle.setOnCheckedChangeListener { _, isChecked ->
                viewModel.setRecurringDay(index, isChecked)
            }
        }

    }

    override fun onStart() {
        super.onStart()
        val serviceIntent = Intent(requireContext(), FirebaseService::class.java)
        requireContext().bindService(serviceIntent, firebaseConnection, Context.BIND_AUTO_CREATE)
        firebaseBound = true
    }

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
        _binding = null
    }

    private val firebaseConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as FirebaseService.FirebaseBinder
            firebaseService = binder.getService()
            firebaseBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            firebaseBound = false
        }
    }

    private fun updateRecurringSwitchUI(isChecked: Boolean) {
        val context = requireContext()
        if (isChecked) {
            recurringSwitch.thumbTintList = ContextCompat.getColorStateList(context, R.color.primary)
            recurringSwitch.trackTintList = ContextCompat.getColorStateList(context, R.color.primary_variant)
        } else {
            // Both are shades of grey
            recurringSwitch.thumbTintList = ColorStateList.valueOf(Color.parseColor("#AEAEAE"))
            recurringSwitch.trackTintList = ColorStateList.valueOf(Color.parseColor("#808080"))
        }
    }
}