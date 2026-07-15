package com.example.android_kotlin_school_bus_tracker.receiver

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.android_kotlin_school_bus_tracker.MainActivity
import com.example.android_kotlin_school_bus_tracker.R
import com.example.android_kotlin_school_bus_tracker.data.Prefs
import com.example.android_kotlin_school_bus_tracker.service.GPSTrackingService
import java.util.Calendar

/**
 * Fired by [AlarmScheduler] at the configured time.
 * Shows a notification prompting the driver to start tracking,
 * then reschedules the next alarm.
 */
class AlarmReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "AlarmReceiver"
        private const val CHANNEL_ID = "reminder_channel"
        private const val NOTIFICATION_ID = 3001
    }

    override fun onReceive(context: Context, intent: Intent) {
        val prefs = Prefs(context)

        // Verify today is an active day
        val today = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        val activeDays = prefs.activeDays.mapNotNull { it.toIntOrNull() }.toSet()
        if (activeDays.isNotEmpty() && today !in activeDays) {
            Log.d(TAG, "Today ($today) is not an active day – skipping")
            AlarmScheduler.schedule(context)   // reschedule for next active day
            return
        }

        // Housekeeping: clear old visited-stop records
        prefs.clearOldVisitedEntries()

        // Show the reminder notification
        createNotificationChannel(context)
        showReminderNotification(context)

        // Auto-start the tracking service
        val serviceIntent = Intent(context, GPSTrackingService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
        prefs.trackingEnabled = true

        // Reschedule for the next active day
        AlarmScheduler.schedule(context)
    }

    // ── Notification ─────────────────────────────────────────────────────

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "GPS Reminder",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Daily reminder to start GPS tracking"
            }
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    private fun showReminderNotification(context: Context) {
        val openApp = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pi = PendingIntent.getActivity(
            context, 0, openApp,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_bus_notification)
            .setContentTitle("School Bus Tracker")
            .setContentText("GPS tracking has been started. Tap to open the app.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                Log.w(TAG, "POST_NOTIFICATIONS not granted – cannot show reminder")
                return
            }
        }

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }
}
