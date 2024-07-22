package com.example.watpool.ui.postingList

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.watpool.R
import com.example.watpool.services.FirebaseService
import com.example.watpool.ui.tripList.TripDetailFragmentDirections

import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class PostingDetailFragment : BottomSheetDialogFragment() {

    private lateinit var postingId: String

    private val viewModel : PostingListViewModel by viewModels()

    private var firebaseService: FirebaseService? = null
    private var firebaseBound: Boolean = false
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

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Find views
        val tripIdTextView = view.findViewById<TextView>(R.id.trip_id_text_view)
        val backButton = view.findViewById<Button>(R.id.back_button)
        val joinButton =  view.findViewById<Button>(R.id.join_button)

        // Set the trip ID text
        tripIdTextView.text = "Trip ID: " + postingId

        backButton.setOnClickListener {
                dismiss()
        }

        val serviceIntent = Intent(requireContext(), FirebaseService::class.java)
        requireContext().bindService(serviceIntent, firebaseConnection, Context.BIND_AUTO_CREATE)


        joinButton.setOnClickListener{btn ->
           firebaseService?.let {
               viewModel.confirmTrip(it, postingId)
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
        if (firebaseBound){
            requireContext().unbindService(firebaseConnection)
            firebaseBound = false
        }
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
}