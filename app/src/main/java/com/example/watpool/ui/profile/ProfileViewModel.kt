package com.example.watpool.ui.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.android.gms.tasks.Task

data class UserDetails(
    val email: String,
    val isDriver: Boolean,
    val name: String,
    val ratingAsDriver: Double,
    val ratingAsRider: Double
)

class ProfileViewModel : ViewModel() {
    private val _userLiveData = MutableLiveData<FirebaseUser?>()
    val userLiveData: LiveData<FirebaseUser?> = _userLiveData

    private val _userDetails = MutableLiveData<UserDetails>()
    val userDetails: LiveData<UserDetails> = _userDetails

    private val usersRef = FirebaseFirestore.getInstance().collection("users")

    init {
        val user = FirebaseAuth.getInstance().currentUser
        _userLiveData.value = user
        fetchUserDetails(user)
    }

    private fun fetchUserDetails(user: FirebaseUser?) {
        user?.let {
            fetchUsersByEmail(it.email ?: "").addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val document = querySnapshot.documents[0]
                    val userDetails = UserDetails(
                        email = document.getString("email") ?: "N/A",
                        isDriver = document.getBoolean("is_driver") ?: false,
                        name = document.getString("name") ?: "N/A",
                        ratingAsDriver = (document.getLong("rating_as_driver") ?: 0L).toDouble(),
                        ratingAsRider = (document.getLong("rating_as_rider") ?: 0L).toDouble()
                    )
                    _userDetails.value = userDetails
                }
            }
        }
    }

    private fun fetchUsersByEmail(email: String): Task<QuerySnapshot> {
        return usersRef.whereEqualTo("email", email).get()
    }

    fun logoutUser() {
        FirebaseAuth.getInstance().signOut()
        _userLiveData.value = null
    }
}
