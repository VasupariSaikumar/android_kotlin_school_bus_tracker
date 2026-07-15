package com.example.android_kotlin_school_bus_tracker.data

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Handles dynamic Firebase initialisation.
 *
 * If the user has entered custom Firebase credentials in settings, a secondary
 * [FirebaseApp] called "custom" is initialised and used.  Otherwise the default
 * app (from `google-services.json`) is used.
 */
object FirebaseManager {

    private const val TAG = "FirebaseManager"
    private const val CUSTOM_APP_NAME = "custom_bus_tracker"

    /**
     * Returns a [FirebaseFirestore] instance – either from the custom app
     * or from the default one.
     */
    fun getFirestore(context: Context, prefs: Prefs): FirebaseFirestore {
        return if (prefs.hasCustomFirebaseConfig()) {
            val app = getOrCreateCustomApp(context, prefs)
            FirebaseFirestore.getInstance(app)
        } else {
            FirebaseFirestore.getInstance()
        }
    }

    private fun getOrCreateCustomApp(context: Context, prefs: Prefs): FirebaseApp {
        // Return existing custom app if already initialised
        FirebaseApp.getApps(context).firstOrNull { it.name == CUSTOM_APP_NAME }?.let {
            return it
        }

        val dbUrl = prefs.firebaseDatabaseUrl.ifBlank {
            "https://${prefs.firebaseProjectId}.firebaseio.com/"
        }

        val options = FirebaseOptions.Builder()
            .setApiKey(prefs.firebaseApiKey)
            .setProjectId(prefs.firebaseProjectId)
            .setApplicationId(prefs.firebaseAppId)
            .setDatabaseUrl(dbUrl)
            .build()

        return try {
            FirebaseApp.initializeApp(context, options, CUSTOM_APP_NAME)
        } catch (e: IllegalStateException) {
            // Already exists – race-condition guard
            Log.w(TAG, "Custom FirebaseApp already exists, reusing", e)
            FirebaseApp.getInstance(CUSTOM_APP_NAME)
        }
    }

    /**
     * Deletes the custom [FirebaseApp] so it can be re-created with new
     * credentials the next time [getFirestore] is called.
     */
    fun resetCustomApp(context: Context) {
        FirebaseApp.getApps(context)
            .firstOrNull { it.name == CUSTOM_APP_NAME }
            ?.delete()
    }
}
