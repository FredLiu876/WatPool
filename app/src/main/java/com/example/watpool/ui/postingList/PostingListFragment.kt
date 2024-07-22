package com.example.watpool.ui.postingList

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.watpool.R
import com.example.watpool.databinding.FragmentPostingListDialogItemBinding
import com.example.watpool.databinding.FragmentPostingListDialogBinding
import com.example.watpool.services.FirebaseService
import com.example.watpool.ui.maps.MapsViewModel
import com.example.watpool.ui.tripList.PostingAdapter
import net.cachapa.expandablelayout.ExpandableLayout


/**
 *
 * A fragment that shows a list of items as a modal bottom sheet.
 *
 * You can show this modal bottom sheet from your activity like this:
 * <pre>
 *    PostingListFragment.newInstance(30).show(supportFragmentManager, "dialog")
 * </pre>
 */
class PostingListFragment : BottomSheetDialogFragment() {

    private val mapsViewModel: MapsViewModel by activityViewModels()
    private val postingListViewModel: PostingListViewModel by viewModels()
    private lateinit var postingDetailFragment : PostingDetailFragment

    private var firebaseService: FirebaseService? = null
    private var firebaseBound: Boolean = false

    private lateinit var postingAdapter: PostingAdapter

    private var searchLatitude: Double = 0.0
    private var searchLongitude: Double = 0.0
    private var searchRadius: Double = 0.0
    private var searchFilter: Boolean = false

    companion object {
        private const val BUNDLE_LAT = "latitude"
        private const val BUNDLE_LNG = "longitude"
        private const val BUNDLE_RADIUS = "radius"
        private const val BUNDLE_FILTER = "filter"

        fun newInstance(latitude: Double, longitude: Double, radius: Double, filter: Boolean): PostingListFragment {
            val fragment = PostingListFragment()
            val args = Bundle()
            args.putDouble(BUNDLE_LAT, latitude)
            args.putDouble(BUNDLE_LNG, longitude)
            args.putDouble(BUNDLE_RADIUS, radius)
            args.putBoolean(BUNDLE_FILTER, filter)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            searchLatitude = it.getDouble(BUNDLE_LAT)
            searchLongitude = it.getDouble(BUNDLE_LNG)
            searchRadius = it.getDouble(BUNDLE_RADIUS)
            searchFilter= it.getBoolean(BUNDLE_FILTER)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {

        return inflater.inflate(R.layout.fragment_posting_list_dialog, container, false)

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {


        postingDetailFragment = PostingDetailFragment()

        // Bind to FirebaseService
        val serviceIntent = Intent(requireContext(), FirebaseService::class.java)
        requireContext().bindService(serviceIntent, firebaseConnection, Context.BIND_AUTO_CREATE)

        postingAdapter = PostingAdapter(emptyList()) {posting ->
            firebaseService?.let {
                postingListViewModel.getPostingDetails(it,  posting.id)
            }
            postingDetailFragment.show(childFragmentManager, posting.id)
            /*postingListViewModel.postings.observe(viewLifecycleOwner, Observer { post ->
                if(post.to.isNotEmpty()){
                    Log.e("ListLog", "To " + post.to)
                    postingDetailFragment.show(childFragmentManager, posting.id)
                }
            })*/

        }

        // Initialize recycler views and adapters
        val recyclerViewStart = view.findViewById<RecyclerView>(R.id.recycler_view_start)
        recyclerViewStart.layoutManager = LinearLayoutManager(context)
        recyclerViewStart.adapter = postingAdapter

        val recyclerViewDestination = view.findViewById<RecyclerView>(R.id.recycler_view_destination)
        recyclerViewDestination.layoutManager = LinearLayoutManager(context)
        recyclerViewDestination.adapter = postingAdapter

        val expandableLayoutStart = view.findViewById<ExpandableLayout>(R.id.expandable_layout_start)
        val headerStart = view.findViewById<TextView>(R.id.header_start_postings)
        headerStart.setOnClickListener{
            expandableLayoutStart.toggle()
        }

        val expandableLayoutDestination = view.findViewById<ExpandableLayout>(R.id.expandable_layout_destination)
        val headerDestination = view.findViewById<TextView>(R.id.header_destination_postings)
        headerDestination.setOnClickListener{
            expandableLayoutDestination.toggle()
        }
        // Default with destinations expanded
        expandableLayoutDestination.toggle()
        mapsViewModel.postingsInRadius.observe(viewLifecycleOwner, Observer { postings ->
            postingAdapter.updateTrips(postings)
            for (post in postings){
                Log.e("PostingList", "Location " + post.driverId)
            }
        })
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

        }

        override fun onServiceDisconnected(name: ComponentName?) {
            firebaseBound = false
        }
    }



}