package com.example.watpool.ui.signup

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.watpool.R
import com.example.watpool.databinding.FragmentSignupBinding
import com.example.watpool.services.FirebaseService
import com.example.watpool.services.LocationService
import com.google.firebase.auth.FirebaseUser

class SignUpFragment : Fragment() {
    private var _binding: FragmentSignupBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SignUpViewModel by viewModels()
    private var firebaseService: FirebaseService? = null
    private var firebaseBound: Boolean = false

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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (activity as AppCompatActivity).supportActionBar?.hide()

        binding.signupButton.setOnClickListener {
            val email = binding.email.text.toString()
            val password = binding.password.text.toString()
            val name = binding.name.text.toString()
            viewModel.registerUser(email, password, name)
            firebaseService?.createUser(email, name)
        }

        viewModel.userLiveData.observe(viewLifecycleOwner) { user ->
            handleSignUpResult(user)
        }

        viewModel.errorLiveData.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val serviceIntent = Intent(requireContext(), FirebaseService::class.java)
        requireContext().bindService(serviceIntent, firebaseConnection, Context.BIND_AUTO_CREATE)
        firebaseBound = true
    }

    private fun handleSignUpResult(user: FirebaseUser?) {
        if (user != null) {
            // Handle successful registration
            findNavController().navigate(R.id.navigation_profile)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if(firebaseBound){
            requireContext().unbindService(firebaseConnection)
            firebaseBound = false
        }
        (activity as AppCompatActivity).supportActionBar?.show()
        _binding = null
    }
}
