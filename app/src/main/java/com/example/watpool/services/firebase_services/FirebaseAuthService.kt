package com.example.watpool.services.firebase_services

import com.example.watpool.services.interfaces.AuthService
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth

class FirebaseAuthService : AuthService {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    override fun signIn(email: String, password: String): Task<AuthResult> {
        return auth.signInWithEmailAndPassword(email, password)
    }

    override fun signUp(email: String, password: String): Task<AuthResult> {
        return auth.createUserWithEmailAndPassword(email, password)
    }

    override fun currentUser(): String {
        val user =  auth.currentUser
        return if (user != null)  {
            user.uid
        } else {
            "Not signed in"
        }
    }
}