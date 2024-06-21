package com.example.watpool.ui.trip_list

import com.example.watpool.R
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class Trip(val id: String, val to: String, val from: String, val date: String, val driver: String)

class TripAdapter(private var trips: List<Trip>, private val clickListener: (Trip) -> Unit) :
    RecyclerView.Adapter<TripAdapter.TripViewHolder>() {

    // ViewHolder class that binds the trip data to the UI components
    class TripViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tripTo: TextView = itemView.findViewById(R.id.trip_to)
        private val tripFrom: TextView = itemView.findViewById(R.id.trip_from)
        private val tripDriver: TextView = itemView.findViewById(R.id.trip_driver)
        private val tripDate: TextView = itemView.findViewById(R.id.trip_date)

        fun bind(trip: Trip, clickListener: (Trip) -> Unit) {
            val context = itemView.context
            tripTo.text = context.getString(R.string.to_destination, trip.to)
            tripFrom.text = context.getString(R.string.from_destination, trip.from)
            tripDriver.text = trip.driver
            tripDate.text = trip.date

            itemView.setOnClickListener { clickListener(trip) }
        }
    }

    // Inflates the item layout and creates the ViewHolder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TripViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.trip_item, parent, false)
        return TripViewHolder(itemView)
    }

    // Binds the data to the ViewHolder
    override fun onBindViewHolder(holder: TripViewHolder, position: Int) {
        holder.bind(trips[position], clickListener)
    }

    // Returns the total number of items in the data set
    override fun getItemCount(): Int {
        return trips.size
    }
}