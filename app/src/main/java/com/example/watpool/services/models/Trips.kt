package com.example.watpool.services.models

import java.util.Date

data class Trips(
    val driver_id: String = "",
    val ending_coordinate: String = "",
    val from: String = "",
    val id: String = "",
    val is_recurring: Boolean = false,
    val passengers: List<String> = emptyList(),
    val starting_coordinate: String = "",
    val to: String = "",
    val trip_date: Date = Date()
)