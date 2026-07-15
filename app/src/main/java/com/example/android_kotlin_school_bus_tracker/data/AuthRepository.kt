package com.example.android_kotlin_school_bus_tracker.data

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth
){
    suspend fun getOrCreateUid(): String{
        firebaseAuth.currentUser?.let { return it.uid}
        val result = firebaseAuth.signInAnonymously().await()
        return result.user?.uid
            ?: error("Anonymous sign-in succeeded but returned no user")
    }
}