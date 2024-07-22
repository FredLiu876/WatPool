package com.example.watpool.ui.tripList

import com.example.watpool.R
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.watpool.services.models.Postings
import com.example.watpool.services.models.Trips

class PostingAdapter(private var postings: List<Postings>, private val clickListener: (Postings) -> Unit) :
    RecyclerView.Adapter<PostingAdapter.TripViewHolder>() {

    // ViewHolder class that binds the trip data to the UI components
    class TripViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tripTo: TextView = itemView.findViewById(R.id.trip_to)
        private val tripFrom: TextView = itemView.findViewById(R.id.trip_from)
        private val tripDriver: TextView = itemView.findViewById(R.id.trip_driver)
        private val tripDate: TextView = itemView.findViewById(R.id.trip_date)

        fun bind(posting: Postings, clickListener: (Postings) -> Unit) {
            val context = itemView.context
            tripTo.text = context.getString(R.string.to_destination, posting.endCoords.location)
            tripFrom.text = context.getString(R.string.from_destination, posting.startCoords.location)
            tripDriver.text = posting.driverId
            tripDate.text = posting.tripDate

            itemView.setOnClickListener { clickListener(posting) }
        }
    }

    // Inflates the item layout and creates the ViewHolder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TripViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.trip_item, parent, false)
        return TripViewHolder(itemView)
    }

    // Binds the data to the ViewHolder
    override fun onBindViewHolder(holder: TripViewHolder, position: Int) {
        holder.bind(postings[position], clickListener)
    }

    // Returns the total number of items in the data set
    override fun getItemCount(): Int {
        return postings.size
    }

    // Method to update the trip list
    fun updateTrips(newPostings: MutableList<Postings>) {
        postings = newPostings
        notifyDataSetChanged()
    }
}