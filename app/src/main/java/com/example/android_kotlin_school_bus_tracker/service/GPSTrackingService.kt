package com.example.android_kotlin_school_bus_tracker.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.android_kotlin_school_bus_tracker.MainActivity
import com.example.android_kotlin_school_bus_tracker.R
import com.example.android_kotlin_school_bus_tracker.data.FirebaseManager
import com.example.android_kotlin_school_bus_tracker.data.Prefs
import com.example.android_kotlin_school_bus_tracker.domain.Stop
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

/**
 * Foreground service that periodically obtains the device's GPS location
 * and saves it to Firebase Firestore.
 *
 * **Two modes**:
 * 1. *Periodic* – when no stops are saved, location is uploaded every
 *    [Prefs.frequencyMinutes] minutes.
 * 2. *Stop-proximity* – when stops exist, location is polled every 30 s
 *    and uploaded **once per stop per day** when the bus comes within
 *    [Prefs.proximityRadiusMetres] of a stop.
 */
class GPSTrackingService : Service() {

    companion object {
        private const val TAG = "GPSTrackingService"
        const val CHANNEL_ID = "gps_tracking_channel"
        const val NOTIFICATION_ID = 2001
        const val ACTION_STOP = "ACTION_STOP_TRACKING"
    }

    private lateinit var fusedClient: FusedLocationProviderClient
    private lateinit var prefs: Prefs
    private lateinit var firestore: FirebaseFirestore

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var locationCallback: LocationCallback? = null
    private var stops: List<Stop> = emptyList()
    private var busId: String = ""
    private var isRunning = false

    // ── Lifecycle ────────────────────────────────────────────────────────

    override fun onCreate() {
        super.onCreate()
        prefs = Prefs(this)
        fusedClient = LocationServices.getFusedLocationProviderClient(this)
        firestore = FirebaseManager.getFirestore(this, prefs)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopTracking()
            return START_NOT_STICKY
        }

        // Guard: if already running, just refresh the notification
        if (isRunning) {
            Log.d(TAG, "Service already running – ignoring duplicate start")
            return START_STICKY
        }
        isRunning = true

        startForeground(NOTIFICATION_ID, buildNotification())
        prefs.trackingEnabled = true

        // Load busId then stops, then start location updates
        serviceScope.launch {
            try {
                busId = loadBusId()
                stops = loadStops()
                Log.d(TAG, "Loaded ${stops.size} stops for bus $busId")
                startLocationUpdates()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialise tracking", e)
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        stopLocationUpdates()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ── Location updates ─────────────────────────────────────────────────

    private fun startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e(TAG, "Location permission not granted – stopping service")
            stopTracking()
            return
        }

        val intervalMs: Long = if (stops.isNotEmpty()) {
            // Proximity mode: poll every 30 seconds so we can detect stops
            TimeUnit.SECONDS.toMillis(30)
        } else {
            // Periodic mode: use configured frequency
            TimeUnit.MINUTES.toMillis(prefs.frequencyMinutes.toLong())
        }

        val request = LocationRequest.Builder(intervalMs)
            .setMinUpdateIntervalMillis(intervalMs)
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { handleLocation(it) }
            }
        }

        fusedClient.requestLocationUpdates(
            request,
            locationCallback!!,
            Looper.getMainLooper()
        )
    }

    private fun stopLocationUpdates() {
        locationCallback?.let { fusedClient.removeLocationUpdates(it) }
        locationCallback = null
    }

    // ── Location handling ────────────────────────────────────────────────

    private fun handleLocation(location: Location) {
        serviceScope.launch {
            if (stops.isNotEmpty()) {
                handleProximityMode(location)
            } else {
                saveLocation(location, nearStopId = null)
            }
        }
    }

    /**
     * In proximity mode we check the distance to every saved stop.
     * If the bus is within [Prefs.proximityRadiusMetres] **and**
     * hasn't been uploaded today, we upload the location and mark
     * the stop as visited.
     */
    private suspend fun handleProximityMode(location: Location) {
        val radius = prefs.proximityRadiusMetres
        for (stop in stops) {
            if (prefs.isStopVisitedToday(stop.id)) continue

            val results = FloatArray(1)
            Location.distanceBetween(
                location.latitude, location.longitude,
                stop.latitude, stop.longitude,
                results
            )
            val distanceMetres = results[0]
            if (distanceMetres <= radius) {
                Log.d(TAG, "Near stop '${stop.name}' (${distanceMetres.toInt()} m) – uploading")
                saveLocation(location, nearStopId = stop.id)
                prefs.markStopVisitedToday(stop.id)
            }
        }
    }

    private suspend fun saveLocation(location: Location, nearStopId: String?) {
        val data = mutableMapOf<String, Any>(
            "latitude" to location.latitude,
            "longitude" to location.longitude,
            "accuracy" to location.accuracy,
            "timestamp" to System.currentTimeMillis(),
            "busId" to busId
        )
        nearStopId?.let { data["nearStopId"] = it }

        try {
            firestore
                .collection("buses")
                .document(busId)
                .collection("locations")
                .add(data)
                .await()
            Log.d(TAG, "Location saved to Firebase")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save location", e)
        }
    }

    // ── Firebase helpers ─────────────────────────────────────────────────

    private suspend fun loadBusId(): String {
        // Re-use the anonymous auth uid stored after initial login
        val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
        return auth.currentUser?.uid ?: run {
            val result = auth.signInAnonymously().await()
            result.user?.uid ?: "unknown"
        }
    }

    private suspend fun loadStops(): List<Stop> {
        return try {
            val snapshot = firestore
                .collection("buses")
                .document(busId)
                .collection("stops")
                .get()
                .await()
            snapshot.documents.mapNotNull { doc ->
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
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load stops", e)
            emptyList()
        }
    }

    // ── Service stop ─────────────────────────────────────────────────────

    private fun stopTracking() {
        stopLocationUpdates()
        prefs.trackingEnabled = false
        isRunning = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    // ── Notification ─────────────────────────────────────────────────────

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "GPS Tracking",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows while GPS tracking is active"
                setShowBadge(false)
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val openApp = Intent(this, MainActivity::class.java)
        val openPi = PendingIntent.getActivity(
            this, 0, openApp,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, GPSTrackingService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPi = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val modeText = if (stops.isNotEmpty()) {
            "Stop-proximity mode (${stops.size} stops)"
        } else {
            "Sharing location every ${prefs.frequencyMinutes} min"
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_bus_notification)
            .setContentTitle("Bus GPS Active")
            .setContentText(modeText)
            .setContentIntent(openPi)
            .addAction(
                R.drawable.ic_stop,
                "Stop Tracking",
                stopPi
            )
            .setOngoing(true)
            .build()
    }
}
