package com.example.watpool.ui.welcome

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.watpool.R
import com.example.watpool.databinding.FragmentWelcomeBinding
import com.google.firebase.auth.FirebaseUser

class WelcomeFragment : Fragment() {
    private var _binding: FragmentWelcomeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: WelcomeViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWelcomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (activity as AppCompatActivity).supportActionBar?.hide()

        viewModel.checkUserLoggedIn()

        viewModel.userLiveData.observe(viewLifecycleOwner, { user ->
            handleUserLoggedIn(user)
        })

        binding.buttonLogin.setOnClickListener {
            viewModel.onGetStartedClicked(findNavController())
        }

        binding.buttonRegister.setOnClickListener {
            viewModel.onRegisterClicked(findNavController())
        }
    }

    private fun handleUserLoggedIn(user: FirebaseUser?) {
        if (user != null) {
            // User is already logged in, navigate to the home screen
            findNavController().navigate(R.id.navigation_dashboard)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        (activity as AppCompatActivity).supportActionBar?.show()
        _binding = null
    }
}