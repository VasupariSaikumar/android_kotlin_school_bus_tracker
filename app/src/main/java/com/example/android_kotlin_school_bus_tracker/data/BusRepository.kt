package com.example.android_kotlin_school_bus_tracker.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class BusRepository @Inject constructor(
    private val firestore :  FirebaseFirestore
) {
    suspend fun ensureBusExists(busId: String){
        val defaults = mapOf(
            "id" to busId ,
            "createdAt" to System.currentTimeMillis()
        )
        val document = firestore.collection("buses").document(busId)
            .set(defaults , SetOptions.merge())
            .await()

    }
}