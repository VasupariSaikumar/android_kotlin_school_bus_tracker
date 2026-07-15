package com.example.android_kotlin_school_bus_tracker.ui

import android.app.Application
import android.content.Intent
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.android_kotlin_school_bus_tracker.data.AuthRepository
import com.example.android_kotlin_school_bus_tracker.data.FirebaseManager
import com.example.android_kotlin_school_bus_tracker.data.Prefs
import com.example.android_kotlin_school_bus_tracker.data.StopRepository
import com.example.android_kotlin_school_bus_tracker.domain.Stop
import com.example.android_kotlin_school_bus_tracker.receiver.AlarmScheduler
import com.example.android_kotlin_school_bus_tracker.service.GPSTrackingService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.UUID
import javax.inject.Inject

data class MainUiState(
    val isTracking: Boolean = false,
    val busId: String = "",
    val stops: List<Stop> = emptyList(),
    val isLoadingStops: Boolean = false,
    val notifyHour: Int = 7,
    val notifyMinute: Int = 0,
    val activeDays: Set<Int> = setOf(
        Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
        Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY
    ),
    val frequencyMinutes: Int = 5,
    val firebaseApiKey: String = "",
    val firebaseProjectId: String = "",
    val firebaseAppId: String = "",
    val firebaseDatabaseUrl: String = "",
    val snackbarMessage: String? = null
)

@HiltViewModel
class MainViewModel @Inject constructor(
    application: Application,
    private val authRepository: AuthRepository,
    private val stopRepository: StopRepository
) : AndroidViewModel(application) {

    private val prefs = Prefs(application)
    private val _state = MutableStateFlow(MainUiState())
    val state: StateFlow<MainUiState> = _state.asStateFlow()

    init {
        loadInitialState()
    }

    private fun loadInitialState() {
        _state.value = _state.value.copy(
            isTracking = prefs.trackingEnabled,
            notifyHour = prefs.notifyHour,
            notifyMinute = prefs.notifyMinute,
            activeDays = prefs.activeDays.mapNotNull { it.toIntOrNull() }.toSet(),
            frequencyMinutes = prefs.frequencyMinutes,
            firebaseApiKey = prefs.firebaseApiKey,
            firebaseProjectId = prefs.firebaseProjectId,
            firebaseAppId = prefs.firebaseAppId,
            firebaseDatabaseUrl = prefs.firebaseDatabaseUrl
        )

        viewModelScope.launch {
            try {
                val busId = authRepository.getOrCreateUid()
                _state.value = _state.value.copy(busId = busId)
                loadStops(busId)
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    snackbarMessage = "Auth error: ${e.message}"
                )
            }
        }
    }

    private suspend fun loadStops(busId: String) {
        _state.value = _state.value.copy(isLoadingStops = true)
        try {
            val stops = stopRepository.getStops(busId)
            _state.value = _state.value.copy(stops = stops, isLoadingStops = false)
        } catch (e: Exception) {
            _state.value = _state.value.copy(
                isLoadingStops = false,
                snackbarMessage = "Failed to load stops: ${e.message}"
            )
        }
    }

    // ── Tracking controls ────────────────────────────────────────────────

    fun startTracking() {
        if (_state.value.isTracking) return   // already tracking – ignore double-tap

        val ctx = getApplication<Application>()
        val intent = Intent(ctx, GPSTrackingService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ctx.startForegroundService(intent)
        } else {
            ctx.startService(intent)
        }
        prefs.trackingEnabled = true
        _state.value = _state.value.copy(isTracking = true)
    }

    fun stopTracking() {
        val ctx = getApplication<Application>()
        val intent = Intent(ctx, GPSTrackingService::class.java).apply {
            action = GPSTrackingService.ACTION_STOP
        }
        ctx.startService(intent)
        prefs.trackingEnabled = false
        _state.value = _state.value.copy(isTracking = false)
    }

    // ── Stop management ──────────────────────────────────────────────────

    fun addCurrentLocationAsStop(lat: Double, lng: Double, name: String) {
        val busId = _state.value.busId
        if (busId.isBlank()) return

        val stop = Stop(
            id = UUID.randomUUID().toString(),
            name = name.ifBlank { "Stop ${_state.value.stops.size + 1}" },
            latitude = lat,
            longitude = lng
        )

        viewModelScope.launch {
            try {
                stopRepository.addStop(busId, stop)
                _state.value = _state.value.copy(
                    stops = _state.value.stops + stop,
                    snackbarMessage = "Stop \"${stop.name}\" saved"
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    snackbarMessage = "Failed to save stop: ${e.message}"
                )
            }
        }
    }

    fun deleteStop(stop: Stop) {
        val busId = _state.value.busId
        if (busId.isBlank()) return

        viewModelScope.launch {
            try {
                stopRepository.deleteStop(busId, stop.id)
                _state.value = _state.value.copy(
                    stops = _state.value.stops.filter { it.id != stop.id },
                    snackbarMessage = "Stop \"${stop.name}\" deleted"
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    snackbarMessage = "Failed to delete stop: ${e.message}"
                )
            }
        }
    }

    // ── Settings ─────────────────────────────────────────────────────────

    fun updateNotifyTime(hour: Int, minute: Int) {
        prefs.notifyHour = hour
        prefs.notifyMinute = minute
        _state.value = _state.value.copy(notifyHour = hour, notifyMinute = minute)
        AlarmScheduler.schedule(getApplication())
    }

    fun toggleDay(day: Int) {
        val current = _state.value.activeDays.toMutableSet()
        if (current.contains(day)) current.remove(day) else current.add(day)
        prefs.activeDays = current.map { it.toString() }.toSet()
        _state.value = _state.value.copy(activeDays = current)
        AlarmScheduler.schedule(getApplication())
    }

    fun updateFrequency(minutes: Int) {
        val clamped = minutes.coerceIn(1, 120)
        prefs.frequencyMinutes = clamped
        _state.value = _state.value.copy(frequencyMinutes = clamped)
    }

    fun updateFirebaseConfig(
        apiKey: String,
        projectId: String,
        appId: String,
        databaseUrl: String
    ) {
        prefs.firebaseApiKey = apiKey
        prefs.firebaseProjectId = projectId
        prefs.firebaseAppId = appId
        prefs.firebaseDatabaseUrl = databaseUrl
        _state.value = _state.value.copy(
            firebaseApiKey = apiKey,
            firebaseProjectId = projectId,
            firebaseAppId = appId,
            firebaseDatabaseUrl = databaseUrl,
            snackbarMessage = "Firebase config saved. Restart tracking to apply."
        )
        // Delete old custom app so it reinitialises with new credentials
        FirebaseManager.resetCustomApp(getApplication())
    }

    fun clearSnackbar() {
        _state.value = _state.value.copy(snackbarMessage = null)
    }

    fun refreshStops() {
        val busId = _state.value.busId
        if (busId.isBlank()) return
        viewModelScope.launch { loadStops(busId) }
    }
}
