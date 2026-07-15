package com.example.android_kotlin_school_bus_tracker.data

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class LocationRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    /**
     * Saves a GPS fix under `buses/{busId}/locations`.
     * If [nearStopId] is provided the record is tagged so the backend/dashboard
     * knows this was a stop-proximity report.
     */
    suspend fun saveLocation(
        busId: String,
        latitude: Double,
        longitude: Double,
        accuracy: Float,
        timestamp: Long,
        nearStopId: String? = null
    ) {
        val data = mutableMapOf<String, Any>(
            "latitude" to latitude,
            "longitude" to longitude,
            "accuracy" to accuracy,
            "timestamp" to timestamp,
            "busId" to busId
        )
        nearStopId?.let { data["nearStopId"] = it }

        firestore
            .collection("buses")
            .document(busId)
            .collection("locations")
            .add(data)
            .await()
    }
}
