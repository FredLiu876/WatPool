package com.example.watpool.ui.becomeDriver

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.example.watpool.R
import com.example.watpool.databinding.FragmentBecomeDriverBinding
import com.example.watpool.services.FirebaseService
import com.example.watpool.ui.ongoingTrip.OngoingTripFragmentDirections
import com.google.firebase.auth.FirebaseUser

class BecomeDriverFragment : Fragment() {

    private var _binding: FragmentBecomeDriverBinding? = null

    private var firebaseService: FirebaseService? = null
    private var firebaseBound: Boolean = false

    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    private lateinit var user: FirebaseUser

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val becomeDriverViewModel: BecomeDriverViewModel by viewModels()

        becomeDriverViewModel.userLiveData.observe(viewLifecycleOwner, Observer { u ->
                u?.let {
                    user = it
                }
            })

        _binding = FragmentBecomeDriverBinding.inflate(inflater, container, false)
        val root: View = binding.root

        binding.submitButton.setOnClickListener {
            val email = user.email ?: ""
            val licenseNumber = binding.licenseNumber.text.toString()
            val carModel = binding.carModel.text.toString()
            val carColor = binding.carColor.text.toString()
            firebaseService?.createDriver(email, licenseNumber, carModel, carColor)?.addOnSuccessListener {
                Toast.makeText(requireContext(), "You are now a driver!", Toast.LENGTH_SHORT).show()
                // Operation was successful
                findNavController().navigate(R.id.navigation_profile)
            }
                ?.addOnFailureListener {
                    // Handle the error if needed
                    Toast.makeText(requireContext(), "Failed to create driver", Toast.LENGTH_SHORT).show()
                }
        }

        // back button
        val buttonBack: Button = binding.back
        buttonBack.setOnClickListener {
            findNavController().navigate(R.id.navigation_profile)
        }

        return root
    }
    override fun onStart() {
        super.onStart()
        val serviceIntent = Intent(requireContext(), FirebaseService::class.java)
        requireContext().bindService(serviceIntent, firebaseConnection, Context.BIND_AUTO_CREATE)
        firebaseBound = true
    }
    override fun onDestroyView() {
        super.onDestroyView()
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
}
