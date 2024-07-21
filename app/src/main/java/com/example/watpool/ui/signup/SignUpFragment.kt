package com.example.watpool.ui.signup

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.text.TextUtils
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
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
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
            if (validateInput(email, password, name)) {
                viewModel.registerUser(email, password, name)
                firebaseService?.createUser(email, name)
            }
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

    private fun validateInput(email: String, password: String, name: String): Boolean {
        var isValid = true
        if (TextUtils.isEmpty(email)) {
            Toast.makeText(context, "Email is required", Toast.LENGTH_SHORT).show()
            binding.email.text.clear()
            isValid = false
        }
        if (TextUtils.isEmpty(password)) {
            Toast.makeText(context, "Password is required", Toast.LENGTH_SHORT).show()
            binding.password.text.clear()
            isValid = false
        } else if (password.length < 6) {
            Toast.makeText(context, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
            binding.password.text.clear()
            isValid = false
        }
        if (TextUtils.isEmpty(name)) {
            Toast.makeText(context, "Name is required", Toast.LENGTH_SHORT).show()
            binding.name.text.clear()
            isValid = false
        }
        return isValid
    }

    private fun handleSignUpResult(user: FirebaseUser?) {
        if (user != null) {
            // Handle successful registration
            findNavController().navigate(R.id.navigation_dashboard)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (firebaseBound) {
            requireContext().unbindService(firebaseConnection)
            firebaseBound = false
        }
        (activity as AppCompatActivity).supportActionBar?.show()
        _binding = null
    }
}
