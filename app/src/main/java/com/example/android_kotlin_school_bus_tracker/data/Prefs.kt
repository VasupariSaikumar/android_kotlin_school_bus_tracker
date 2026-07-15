package com.example.android_kotlin_school_bus_tracker.data

import android.content.Context
import android.content.SharedPreferences
import java.util.Calendar

class Prefs(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("bus_tracker_prefs", Context.MODE_PRIVATE)

    // ── Tracking state ──────────────────────────────────────────────────
    var trackingEnabled: Boolean
        get() = prefs.getBoolean(KEY_TRACKING_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_TRACKING_ENABLED, value).apply()

    // ── Notification schedule ───────────────────────────────────────────
    var notifyHour: Int
        get() = prefs.getInt(KEY_NOTIFY_HOUR, 7)
        set(value) = prefs.edit().putInt(KEY_NOTIFY_HOUR, value).apply()

    var notifyMinute: Int
        get() = prefs.getInt(KEY_NOTIFY_MINUTE, 0)
        set(value) = prefs.edit().putInt(KEY_NOTIFY_MINUTE, value).apply()

    /**
     * Active days stored as a set of day-of-week indices using [Calendar] constants:
     * Mon = 2, Tue = 3, Wed = 4, Thu = 5, Fri = 6, Sat = 7.
     * Default: all weekdays Monday–Saturday.
     */
    var activeDays: Set<String>
        get() = prefs.getStringSet(
            KEY_ACTIVE_DAYS,
            setOf(
                Calendar.MONDAY.toString(),
                Calendar.TUESDAY.toString(),
                Calendar.WEDNESDAY.toString(),
                Calendar.THURSDAY.toString(),
                Calendar.FRIDAY.toString(),
                Calendar.SATURDAY.toString()
            )
        ) ?: emptySet()
        set(value) = prefs.edit().putStringSet(KEY_ACTIVE_DAYS, value).apply()

    // ── Location frequency (minutes) ────────────────────────────────────
    var frequencyMinutes: Int
        get() = prefs.getInt(KEY_FREQ_MINUTES, 5)
        set(value) = prefs.edit().putInt(KEY_FREQ_MINUTES, value).apply()

    // ── Stop proximity radius (metres) ──────────────────────────────────
    var proximityRadiusMetres: Float
        get() = prefs.getFloat(KEY_PROXIMITY_RADIUS, 150f)
        set(value) = prefs.edit().putFloat(KEY_PROXIMITY_RADIUS, value).apply()

    // ── Custom Firebase configuration ───────────────────────────────────
    var firebaseApiKey: String
        get() = prefs.getString(KEY_FB_API_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_FB_API_KEY, value).apply()

    var firebaseProjectId: String
        get() = prefs.getString(KEY_FB_PROJECT_ID, "") ?: ""
        set(value) = prefs.edit().putString(KEY_FB_PROJECT_ID, value).apply()

    var firebaseAppId: String
        get() = prefs.getString(KEY_FB_APP_ID, "") ?: ""
        set(value) = prefs.edit().putString(KEY_FB_APP_ID, value).apply()

    var firebaseDatabaseUrl: String
        get() = prefs.getString(KEY_FB_DB_URL, "") ?: ""
        set(value) = prefs.edit().putString(KEY_FB_DB_URL, value).apply()

    // ── Driver ID ───────────────────────────────────────────────────────
    var driverId: String
        get() = prefs.getString(KEY_DRIVER_ID, "") ?: ""
        set(value) = prefs.edit().putString(KEY_DRIVER_ID, value).apply()

    // ── Helpers ─────────────────────────────────────────────────────────

    /** Returns a [Calendar] set to today at the configured notification time. */
    fun getNotifyCalendar(): Calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, notifyHour)
        set(Calendar.MINUTE, notifyMinute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    /**
     * Returns `true` when at least the three mandatory Firebase fields
     * (API key, Project ID, App ID) are filled in.
     */
    fun hasCustomFirebaseConfig(): Boolean =
        firebaseApiKey.isNotBlank() &&
                firebaseProjectId.isNotBlank() &&
                firebaseAppId.isNotBlank()

    // ── Stop-visit tracking for "once-per-day" proximity logic ──────────
    /**
     * Records that [stopId] was visited today so the service will not
     * re-upload its location for the remainder of the day.
     */
    fun markStopVisitedToday(stopId: String) {
        val todayKey = todayDateKey()
        val visited = getVisitedStopsForDate(todayKey).toMutableSet()
        visited.add(stopId)
        prefs.edit().putStringSet("${KEY_PREFIX_VISITED}_$todayKey", visited).apply()
    }

    /** Returns `true` when [stopId] has already been visited today. */
    fun isStopVisitedToday(stopId: String): Boolean {
        return getVisitedStopsForDate(todayDateKey()).contains(stopId)
    }

    /** Clears visited-stop records for any day other than today (housekeeping). */
    fun clearOldVisitedEntries() {
        val today = todayDateKey()
        prefs.all.keys
            .filter { it.startsWith(KEY_PREFIX_VISITED) && !it.endsWith(today) }
            .forEach { prefs.edit().remove(it).apply() }
    }

    private fun getVisitedStopsForDate(dateKey: String): Set<String> =
        prefs.getStringSet("${KEY_PREFIX_VISITED}_$dateKey", emptySet()) ?: emptySet()

    private fun todayDateKey(): String {
        val cal = Calendar.getInstance()
        return "${cal.get(Calendar.YEAR)}_${cal.get(Calendar.DAY_OF_YEAR)}"
    }

    companion object {
        private const val KEY_TRACKING_ENABLED = "tracking_enabled"
        private const val KEY_NOTIFY_HOUR = "notify_hour"
        private const val KEY_NOTIFY_MINUTE = "notify_minute"
        private const val KEY_ACTIVE_DAYS = "active_days"
        private const val KEY_FREQ_MINUTES = "frequency_minutes"
        private const val KEY_PROXIMITY_RADIUS = "proximity_radius"
        private const val KEY_FB_API_KEY = "firebase_api_key"
        private const val KEY_FB_PROJECT_ID = "firebase_project_id"
        private const val KEY_FB_APP_ID = "firebase_app_id"
        private const val KEY_FB_DB_URL = "firebase_database_url"
        private const val KEY_DRIVER_ID = "driver_id"
        private const val KEY_PREFIX_VISITED = "visited_stops"
    }
}
