package com.example.android_kotlin_school_bus_tracker.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.android_kotlin_school_bus_tracker.data.Prefs
import java.util.Calendar

/**
 * Schedules / cancels the daily reminder alarm that tells the driver
 * to start GPS tracking.
 */
object AlarmScheduler {

    private const val TAG = "AlarmScheduler"
    private const val REQUEST_CODE = 10_001

    fun schedule(context: Context) {
        val prefs = Prefs(context)
        cancel(context)                       // always cancel old alarm first

        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = makePendingIntent(context)
        val target = nextAlarmTime(prefs) ?: return

        Log.d(TAG, "Scheduling alarm for ${target.time}")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
            // On API 31+ without SCHEDULE_EXACT_ALARM permission, fall back to inexact
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, target.timeInMillis, pi)
        } else {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, target.timeInMillis, pi)
        }
    }

    fun cancel(context: Context) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = makePendingIntent(context)
        am.cancel(pi)
        pi.cancel()
    }

    // ── internals ────────────────────────────────────────────────────────

    private fun makePendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * Returns the next [Calendar] that matches the user's configured
     * notification time **and** is on one of the active days.
     * Returns `null` if no active days are configured.
     */
    private fun nextAlarmTime(prefs: Prefs): Calendar? {
        val activeDays = prefs.activeDays.mapNotNull { it.toIntOrNull() }.toSet()
        if (activeDays.isEmpty()) return null

        val now = Calendar.getInstance()
        val target = prefs.getNotifyCalendar()           // today at HH:mm

        // If it's already past the target time today, start searching from tomorrow
        if (!target.after(now)) {
            target.add(Calendar.DAY_OF_YEAR, 1)
            target.set(Calendar.HOUR_OF_DAY, prefs.notifyHour)
            target.set(Calendar.MINUTE, prefs.notifyMinute)
            target.set(Calendar.SECOND, 0)
            target.set(Calendar.MILLISECOND, 0)
        }

        // Walk forward (up to 7 days) until we land on an active day
        for (i in 0..6) {
            if (activeDays.contains(target.get(Calendar.DAY_OF_WEEK))) return target
            target.add(Calendar.DAY_OF_YEAR, 1)
            target.set(Calendar.HOUR_OF_DAY, prefs.notifyHour)
            target.set(Calendar.MINUTE, prefs.notifyMinute)
            target.set(Calendar.SECOND, 0)
            target.set(Calendar.MILLISECOND, 0)
        }
        return null
    }
}
