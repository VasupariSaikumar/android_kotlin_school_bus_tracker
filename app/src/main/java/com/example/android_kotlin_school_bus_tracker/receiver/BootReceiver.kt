package com.example.android_kotlin_school_bus_tracker.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.android_kotlin_school_bus_tracker.data.Prefs

/**
 * Reschedules the daily reminder alarm after a device reboot.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Always reschedule the reminder alarm
            AlarmScheduler.schedule(context)

            // If tracking was active before reboot, restart the service
            val prefs = Prefs(context)
            if (prefs.trackingEnabled) {
                val serviceIntent =
                    Intent(context, com.example.android_kotlin_school_bus_tracker.service.GPSTrackingService::class.java)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            }
        }
    }
}
