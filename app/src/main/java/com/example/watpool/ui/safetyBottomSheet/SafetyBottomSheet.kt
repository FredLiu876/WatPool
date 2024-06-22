package com.example.watpool.ui.safetyBottomSheet

import com.example.watpool.R
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton


class SafetyBottomSheetDialog() : BottomSheetDialogFragment() {
    private var mListener: BottomSheetListener? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val v: View = inflater.inflate(R.layout.bottom_sheet_safety, container, false)

        val btnCall911 = v.findViewById<MaterialButton>(R.id.btn_911)
        val btnViewProfile = v.findViewById<MaterialButton>(R.id.btn_view_profile)
        val btnShareTrip = v.findViewById<MaterialButton>(R.id.btn_share_trip)
        btnCall911.setOnClickListener {
//            mListener!!.onButtonClicked("Button 1 clicked")
            val callIntent = Intent(Intent.ACTION_DIAL)
            callIntent.data = Uri.parse("tel:123-456-7890")
            startActivity(callIntent)
            dismiss()
        }
        btnViewProfile.setOnClickListener {
//            mListener!!.onButtonClicked("Button 2 clicked")
            dismiss()
        }
        btnShareTrip.setOnClickListener {
            val sendIntent: Intent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, "I'm in a Watpool at LOCATION")
                type = "text/plain"
            }
            val shareIntent = Intent.createChooser(sendIntent, null)
            startActivity(shareIntent)
            dismiss()
        }

        return v
    }

    interface BottomSheetListener {
        fun onButtonClicked(text: String?)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        try {
//            mListener = context as BottomSheetListener
        } catch (e: ClassCastException) {
            throw ClassCastException(
                context.toString()
                        + " must implement BottomSheetListener"
            )
        }
    }
}