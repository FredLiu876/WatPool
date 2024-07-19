package com.example.watpool.services.interfaces
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.QuerySnapshot

interface UserService {
    fun fetchUsersById(id: String): Task<QuerySnapshot>
    fun fetchUsersByUsername(username: String): Task<QuerySnapshot>
    fun createUser(email: String, name: String): Task<DocumentReference>
    fun createDriver(username: String, licenseNumber: String, carModel: String, carColor: String ): Task<Void>
}