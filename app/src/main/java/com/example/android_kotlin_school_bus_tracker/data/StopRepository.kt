package com.example.android_kotlin_school_bus_tracker.data

import com.example.android_kotlin_school_bus_tracker.domain.Stop
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class StopRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    private fun stopsCollection(busId: String) =
        firestore.collection("buses").document(busId).collection("stops")

    suspend fun addStop(busId: String, stop: Stop) {
        val data = mapOf(
            "id" to stop.id,
            "name" to stop.name,
            "latitude" to stop.latitude,
            "longitude" to stop.longitude,
            "createdAt" to stop.createdAt
        )
        stopsCollection(busId).document(stop.id).set(data).await()
    }

    suspend fun deleteStop(busId: String, stopId: String) {
        stopsCollection(busId).document(stopId).delete().await()
    }

    suspend fun getStops(busId: String): List<Stop> {
        val snapshot = stopsCollection(busId).get().await()
        return snapshot.documents.mapNotNull { doc ->
            try {
                Stop(
                    id = doc.getString("id") ?: doc.id,
                    name = doc.getString("name") ?: "",
                    latitude = doc.getDouble("latitude") ?: 0.0,
                    longitude = doc.getDouble("longitude") ?: 0.0,
                    createdAt = doc.getLong("createdAt") ?: 0L
                )
            } catch (_: Exception) {
                null
            }
        }
    }
}
