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
import android.os.Build
import androidx.fragment.app.viewModels
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.text.InputType
import android.view.ContextThemeWrapper
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import android.widget.ToggleButton
import androidx.annotation.RequiresApi
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
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

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
    private lateinit var backBtn: Button

    private lateinit var recurringSwitch: Switch
    private lateinit var recurringOptionsLayout: LinearLayout
    private lateinit var recurringEndDateSpinner: Spinner
    private lateinit var recurringEndDateTV: TextView
    private val dayToggles = mutableListOf<ToggleButton>()

    private lateinit var pickupSearchView: SearchView
    private lateinit var pickupPlacesFragment: PlacesFragment
    private lateinit var destinationSearchView: SearchView
    private lateinit var destinationPlacesFragment: PlacesFragment
    private var isSelectionInProgress = false

    private var firebaseService: FirebaseService? = null
    private var firebaseBound: Boolean = false


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

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize the button and text view from the binding
        pickDateBtn = binding.idBtnPickDate
        selectedDateTV = binding.idTVSelectedDate
        pickTimeBtn = binding.idBtnPickTime
        selectedTimeTV = binding.idTVSelectedTime
        createTripBtn = binding.createTripBtn
        backBtn = binding.createTripBackBtn

        backBtn.visibility = View.INVISIBLE

        recurringSwitch = binding.recurringSwitch
        updateRecurringSwitchUI(false)

        recurringOptionsLayout = binding.recurringOptionsLayout
        recurringEndDateTV = binding.recurringEndDateTV
        recurringEndDateSpinner = binding.recurringEndDateSpinner

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
        pickupSearchView.setQueryHint("Enter pickup location")
        pickupSearchView.isIconified = false
        pickupSearchView.clearFocus()

        destinationSearchView = binding.destination
        destinationSearchView.setQueryHint("Enter destination")
        destinationSearchView.isIconified = false
        destinationSearchView.clearFocus()



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

        arguments?.let {
            val start = create_tripArgs.fromBundle(it).startDestination
            val end = create_tripArgs.fromBundle(it).endDestination
            if (!start.isEmpty() && !end.isEmpty()){
                backBtn.visibility= View.VISIBLE
            }
            pickupSearchView.setQuery(start, false)
            destinationSearchView.setQuery(end, false)

            viewModel.setPickupLocation(start)
            viewModel.setDestination(end)
        }

        backBtn.setOnClickListener {
            findNavController().popBackStack()
        }

        pickupSearchView.setOnQueryTextListener(object: SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(query: String?): Boolean {
                pickupSearchView.clearFocus()
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
                destinationSearchView.clearFocus()
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
        viewModel.recurringEndDates.observe(viewLifecycleOwner) { dates ->
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, dates)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            recurringEndDateSpinner.adapter = adapter
        }

        recurringEndDateSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedDate = parent?.getItemAtPosition(position) as String
                viewModel.setRecurringEndDate(selectedDate)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing
            }
        }

        viewModel.selectedTime.observe(viewLifecycleOwner, Observer {
            binding.idTVSelectedTime.text = it
        })

        binding.numAvailableSeats.inputType = InputType.TYPE_CLASS_NUMBER
        viewModel.numAvailableSeats.observe(viewLifecycleOwner, Observer {
            binding.numAvailableSeats.setText(it)
        })

        dayToggles.forEach { it.isClickable = false }

        viewModel.selectedCalendar.observe(viewLifecycleOwner) { calendar ->
            updateDateTimeDisplay(calendar)
        }

        pickDateBtn.setOnClickListener {
            showDatePicker()
        }

        pickTimeBtn.setOnClickListener {
            showTimePicker()
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
                resetSearchViews()
                viewModel.onCreateTrip(findNavController())
                viewModel.resetTripCreationStatus()
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

        viewModel.recurringEndDate.observe(viewLifecycleOwner, Observer {
            val date = formatDateString(it)
            recurringEndDateTV.text = "End Date: $date"
        })

        viewModel.recurringDays.observe(viewLifecycleOwner) { days ->
            days.forEachIndexed { index, isSelected ->
                dayToggles[index].isChecked = isSelected
            }
        }

    }

    override fun onStart() {
        super.onStart()
        val serviceIntent = Intent(requireContext(), FirebaseService::class.java)
        requireContext().bindService(serviceIntent, firebaseConnection, Context.BIND_AUTO_CREATE)
        firebaseBound = true
    }

    override fun onResume() {
        super.onResume()
        resetSearchViews()
    }

    private fun resetSearchViews() {
        viewModel.clearSearchViewData()
        pickupSearchView.setQuery("", false)
        destinationSearchView.setQuery("", false)
        pickupPlacesFragment.clearList()
        destinationPlacesFragment.clearList()
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
    @RequiresApi(Build.VERSION_CODES.O)
    private fun formatDateString(inputDate: String): String {
        try {
            val inputFormatter = DateTimeFormatter.ofPattern("d-M-yyyy")
            val outputFormatter = DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH)

            val date = LocalDate.parse(inputDate, inputFormatter)

            return date.format(outputFormatter)
        } catch (e: Exception) {
            Log.e("formatDateString", "$e")
        }
        return inputDate
    }

    private fun showDatePicker() {
        val calendar = viewModel.selectedCalendar.value ?: Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            ContextThemeWrapper(requireContext(), androidx.appcompat.R.style.Theme_AppCompat_Dialog).apply {
                theme.applyStyle(R.style.DatePickerDialogTheme, true)
            },
            { _, year, monthOfYear, dayOfMonth ->
                val updatedCalendar = viewModel.selectedCalendar.value ?: Calendar.getInstance()
                updatedCalendar.set(Calendar.YEAR, year)
                updatedCalendar.set(Calendar.MONTH, monthOfYear)
                updatedCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                viewModel.updateSelectedDateTime(
                    year,
                    monthOfYear,
                    dayOfMonth,
                    updatedCalendar.get(Calendar.HOUR_OF_DAY),
                    updatedCalendar.get(Calendar.MINUTE)
                )
            },
            year, month, day
        )
        datePickerDialog.show()
    }

    private fun showTimePicker() {
        val calendar = viewModel.selectedCalendar.value ?: Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        val timePickerDialog = TimePickerDialog(
            ContextThemeWrapper(requireContext(), androidx.appcompat.R.style.Theme_AppCompat_Dialog).apply {
                theme.applyStyle(R.style.TimePickerDialogTheme, true)
            },
            { _, selectedHour, selectedMinute ->
                val updatedCalendar = viewModel.selectedCalendar.value ?: Calendar.getInstance()
                viewModel.updateSelectedDateTime(
                    updatedCalendar.get(Calendar.YEAR),
                    updatedCalendar.get(Calendar.MONTH),
                    updatedCalendar.get(Calendar.DAY_OF_MONTH),
                    selectedHour,
                    selectedMinute
                )
            },
            hour, minute, false
        )
        timePickerDialog.show()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun updateDateTimeDisplay(calendar: Calendar) {
        val dateFormat = SimpleDateFormat("d-M-yyyy", Locale.getDefault()).format(calendar.time)
        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault()).format(calendar.time)

        viewModel.setSelectedDate(dateFormat)
        viewModel.setSelectedTime(timeFormat)

        selectedDateTV.text = "Date: ${formatDateString(dateFormat)}"
        selectedTimeTV.text = "Time: ${timeFormat}"
    }
}