package com.example.watpool.services.firebase_services
import com.example.watpool.services.interfaces.UserService

import com.google.android.gms.tasks.Task

import com.google.firebase.Firebase
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot

import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.firestore

import java.util.UUID


class FirebaseUserService: UserService {
    private val database = Firebase.firestore
    private val usersRef: CollectionReference = database.collection("users")

    override fun fetchUsersById(id: String): Task<QuerySnapshot> {
        return usersRef.whereEqualTo("id", id).get()
    }

    override fun fetchUserByDocumentId(id: String): Task<DocumentSnapshot> {
        return usersRef.document(id).get()
    }
    override fun fetchUsersByEmail(email: String): Task<QuerySnapshot> {
        return usersRef.whereEqualTo("email", email).get()
    }

    override fun createUser(email: String, name: String): Task<DocumentReference> {
        val id: String = UUID.randomUUID().toString()
        val user = hashMapOf(
            "id" to id,
            "email" to email,
            "name" to name,
            "phone" to 1234567890,
            "rating_as_rider" to 5,
            "rating_as_driver" to 5,
            "is_driver" to false,
        )
        return usersRef.add(user)
    }

    override fun createDriver(username: String, licenseNumber: String, carModel: String, carColor: String ): Task<Void> {
        val userUpdate = hashMapOf(
            "license_number" to licenseNumber,
            "car_model" to carModel,
            "carColor" to carColor,
            "license_number" to licenseNumber 
        )

        return usersRef.whereEqualTo("username", username)
        .get()
        .continueWithTask { task ->
            if (task.isSuccessful) {
                val documents = task.result?.documents
                if (!documents.isNullOrEmpty()) {
                    documents[0].reference.update(userUpdate as Map<String, Any>)
                } else {
                    throw Exception("User not found")
                }
            } else {
                throw task.exception ?: Exception("Failed to fetch user")
            }
        }
    }



    
}
