package com.example.watpool.services.interfaces

import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult

interface AuthService {
    fun signIn(email: String, password: String): Task<AuthResult>
    fun signUp(email: String, password: String): Task<AuthResult>
    fun updateUserProfile(name: String): Any
    fun currentUser(): String
}