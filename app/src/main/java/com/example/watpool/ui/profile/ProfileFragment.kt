package com.example.watpool.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.watpool.R
import com.example.watpool.databinding.FragmentProfileBinding
import com.google.firebase.auth.FirebaseUser

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null

    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val profileViewModel: ProfileViewModel by viewModels()

        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        val root: View = binding.root

        profileViewModel.userDetails.observe(viewLifecycleOwner) { userDetails ->
            binding.textName.text = userDetails.name
            binding.textEmail.text = userDetails.email
            binding.textRatingAsRider.text = userDetails.ratingAsRider.toString()

            if (userDetails.isDriver) {
                binding.textRatingAsDriver.text = userDetails.ratingAsDriver.toString()
                binding.textRatingAsDriver.visibility = View.VISIBLE
                binding.labelRatingAsDriver.visibility = View.VISIBLE
            } else {
                binding.textRatingAsDriver.visibility = View.GONE
                binding.labelRatingAsDriver.visibility = View.GONE
            }
        }

        binding.buttonLogout.setOnClickListener {
            profileViewModel.logoutUser()
        }

        profileViewModel.userLiveData.observe(viewLifecycleOwner) { user ->
            handleSignOutResult(user)
        }

        return root
    }

    private fun handleSignOutResult(user: FirebaseUser?) {
        if (user == null) {
            // Handle successful logout
            findNavController().navigate(R.id.navigation_welcome)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
