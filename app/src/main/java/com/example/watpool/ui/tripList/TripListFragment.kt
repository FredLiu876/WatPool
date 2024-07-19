package com.example.watpool.ui.tripList

import android.content.ComponentName
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.watpool.R
import com.example.watpool.services.FirebaseService
import com.example.watpool.services.models.TripConfirmationDetails
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import net.cachapa.expandablelayout.ExpandableLayout
import java.text.SimpleDateFormat
import java.util.Date
import java.time.LocalDate
import java.util.Calendar
import java.util.Locale

class TripListFragment :  BottomSheetDialogFragment() {

    private lateinit var todayTripAdapter: TripAdapter
    private lateinit var upcomingTripAdapter: TripAdapter
    private lateinit var previousTripAdapter: TripAdapter
    private val tripListViewModel: TripListViewModel by viewModels()

    private var firebaseService: FirebaseService? = null
    private var firebaseBound: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_trip_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize RecyclerView and Adapter
        /*tripAdapter = TripAdapter(emptyList()) { trip ->
            if(dialog == null){
                val action = TripListFragmentDirections.actionTripListFragmentToIsCurrentTripFragment(trip.id)
                findNavController().navigate(action)
            } else {
                showTripDetailsSheet(trip.id)
            }*/
        // Initialize RecyclerViews and Adapters
        todayTripAdapter = TripAdapter(emptyList()) { trip ->
            val action = TripListFragmentDirections.actionTripListFragmentToIsCurrentTripFragment(trip.id)
            findNavController().navigate(action)

        }
        upcomingTripAdapter = TripAdapter(emptyList()) { trip ->
            val action = TripListFragmentDirections.actionTripListFragmentToIsCurrentTripFragment(trip.id)
            findNavController().navigate(action)
        }
        previousTripAdapter = TripAdapter(emptyList()) { trip ->
            val action = TripListFragmentDirections.actionTripListFragmentToTripDetailFragment(trip.id)
            findNavController().navigate(action)
        }

        val recyclerViewToday = view.findViewById<RecyclerView>(R.id.recycler_view_today)
        recyclerViewToday.layoutManager = LinearLayoutManager(context)
        recyclerViewToday.adapter = todayTripAdapter

        val recyclerViewUpcoming = view.findViewById<RecyclerView>(R.id.recycler_view_upcoming)
        recyclerViewUpcoming.layoutManager = LinearLayoutManager(context)
        recyclerViewUpcoming.adapter = upcomingTripAdapter

        val recyclerViewPrevious = view.findViewById<RecyclerView>(R.id.recycler_view_previous)
        recyclerViewPrevious.layoutManager = LinearLayoutManager(context)
        recyclerViewPrevious.adapter = previousTripAdapter

        // Set up expandable layouts
        val expandableLayoutToday = view.findViewById<ExpandableLayout>(R.id.expandable_layout_today)
        val headerToday = view.findViewById<TextView>(R.id.header_today_trips)
        headerToday.setOnClickListener {
            expandableLayoutToday.toggle()
        }
        // Today's trips begins expanded
        expandableLayoutToday.toggle()

        val expandableLayoutUpcoming = view.findViewById<ExpandableLayout>(R.id.expandable_layout_upcoming)
        val headerUpcoming = view.findViewById<TextView>(R.id.header_upcoming_trips)
        headerUpcoming.setOnClickListener {
            expandableLayoutUpcoming.toggle()
        }

        val expandableLayoutPrevious = view.findViewById<ExpandableLayout>(R.id.expandable_layout_previous)
        val headerPrevious = view.findViewById<TextView>(R.id.header_previous_trips)
        headerPrevious.setOnClickListener {
            expandableLayoutPrevious.toggle()
        }

        // Observe the LiveData from the ViewModel
        tripListViewModel.trips.observe(viewLifecycleOwner, Observer { trips ->
            todayTripAdapter.updateTrips(trips.filter { isToday(it.tripDate) })
            upcomingTripAdapter.updateTrips(trips.filter { isUpcomingTrip(it.tripDate) })
            previousTripAdapter.updateTrips(trips.filter { isPreviousTrip(it.tripDate) })
        })

        // Bind to FirebaseService
        val serviceIntent = Intent(requireContext(), FirebaseService::class.java)
        requireContext().bindService(serviceIntent, firebaseConnection, Context.BIND_AUTO_CREATE)
    }

    private fun showTripDetailsSheet(tripId: String) {
        val tripDetailFragment = TripDetailFragment()
        tripDetailFragment.show(childFragmentManager, tripId)
    }

    private fun convertStringToDate(date: String): Date? {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return format.parse(date)
    }

    private fun isToday(date: String): Boolean {
        val dateAsDateObject = convertStringToDate(date)
        val today = Calendar.getInstance()
        val tripDate = Calendar.getInstance()
        if (dateAsDateObject != null) {
            tripDate.time = dateAsDateObject
        }

        return today.get(Calendar.YEAR) == tripDate.get(Calendar.YEAR) &&
                today.get(Calendar.DAY_OF_YEAR) == tripDate.get(Calendar.DAY_OF_YEAR)
    }

    private fun isUpcomingTrip(date: String): Boolean {
        val today = Calendar.getInstance()
        val dateAsDateObject = convertStringToDate(date)
        today.set(Calendar.HOUR_OF_DAY, 0)
        today.set(Calendar.MINUTE, 0)
        today.set(Calendar.SECOND, 0)
        today.set(Calendar.MILLISECOND, 0)
        return dateAsDateObject?.after(today.time) ?: false
    }

    private fun isPreviousTrip(date: String): Boolean {
        val today = Calendar.getInstance()
        val dateAsDateObject = convertStringToDate(date)
        today.set(Calendar.HOUR_OF_DAY, 0)
        today.set(Calendar.MINUTE, 0)
        today.set(Calendar.SECOND, 0)
        today.set(Calendar.MILLISECOND, 0)

        return dateAsDateObject?.before(today.time) ?: false
    }


    override fun onStart() {
        super.onStart()
        val serviceIntent = Intent(requireContext(), FirebaseService::class.java)
        requireContext().bindService(serviceIntent, firebaseConnection, Context.BIND_AUTO_CREATE)
        firebaseBound = true
    }

    // Stop location service if still bound
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
    }

    private val firebaseConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as FirebaseService.FirebaseBinder
            firebaseService = binder.getService()
            firebaseBound = true

            // Fetch trips once the service is connected
            firebaseService?.let {
                //val riderId = it.currentUser()
                val riderId = "user_id_1"
                tripListViewModel.fetchConfirmedTrips(it, riderId)
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            firebaseBound = false
        }
    }
}