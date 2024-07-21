package com.example.watpool.services.firebase_services

import com.example.watpool.services.interfaces.AuthService
import com.example.watpool.ui.profile.UserDetails
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore

class FirebaseAuthService : AuthService {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    override fun signIn(email: String, password: String): Task<AuthResult> {
        return auth.signInWithEmailAndPassword(email, password)
    }

    override fun signUp(email: String, password: String): Task<AuthResult> {
        return auth.createUserWithEmailAndPassword(email, password)
    }

    override fun updateUserProfile(name: String): Task<Void> {
        val user = auth.currentUser
        val profileUpdates = UserProfileChangeRequest.Builder()
            .setDisplayName(name)
            .build()
        return user!!.updateProfile(profileUpdates)
    }

    override fun getCurrentUser(): FirebaseUser? {
        return auth.currentUser
    }
}