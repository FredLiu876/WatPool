package com.example.watpool.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.watpool.databinding.FragmentDashboardBinding
import com.example.watpool.ui.safetyBottomSheet.SafetyBottomSheetDialog
import com.google.android.material.button.MaterialButton


class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private val viewModel: DashboardViewModel by viewModels()

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        val dashboardViewModel =
                ViewModelProvider(this).get(DashboardViewModel::class.java)

        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val textView: TextView = binding.textDashboard
        dashboardViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }

        val buttonOpenBottomSheet: MaterialButton = binding.btnInfo
        buttonOpenBottomSheet.setOnClickListener {
            val bottomSheet = SafetyBottomSheetDialog()
            bottomSheet.show(requireActivity().supportFragmentManager, "safetyBottomSheet")
        }
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (activity as AppCompatActivity).supportActionBar?.hide()

        binding.button.setOnClickListener {
            viewModel.onCreateTrip(findNavController())
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}