package net.calvuz.qreport.app.app.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import android.widget.Toast
import net.calvuz.qreport.app.app.domain.model.Address
import net.calvuz.qreport.app.app.domain.model.GeoCoordinates


/**
 * Utility class for opening map applications with addresses or coordinates
 */
object MapUtils {

    private const val TAG = "MapUtils"

    /**
     * Open maps app with facility address
     * Uses GPS coordinates if available, otherwise falls back to text address
     */
    fun openMapsWithAddress(context: Context, address: Address) {
        try {
            Log.d(TAG, "Opening maps with address: ${address.toDisplayString()}")

            val success = when {
                address.hasCoordinates() -> {
                    // Use GPS coordinates for better precision
                    val coords = address.coordinates!!
                    Log.d(TAG, "Using coordinates: ${coords.latitude}, ${coords.longitude}")
                    openWithCoordinates(context, coords.latitude, coords.longitude)
                }
                address.isComplete() -> {
                    // Use formatted text address
                    val query = address.toDisplayString()
                    Log.d(TAG, "Using address query: $query")
                    openWithQuery(context, query)
                }
                else -> {
                    Log.w(TAG, "Incomplete address data")
                    showToast(context, "Indirizzo incompleto")
                    false
                }
            }

            if (!success) {
                showToast(context, "Nessuna app mappe disponibile")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error opening maps", e)
            showToast(context, "Errore apertura mappe")
        }
    }

    /**
     * Open maps with coordinates using multiple fallback strategies
     */
    private fun openWithCoordinates(context: Context, lat: Double, lng: Double): Boolean {
        val strategies = listOf(
            // Strategy 1: Google Maps direct
            {
                val uri = Uri.parse("https://maps.google.com/?q=$lat,$lng")
                val intent = Intent(Intent.ACTION_VIEW, uri)
                intent.setPackage("com.google.android.apps.maps")
                intent
            },

            // Strategy 2: Generic geo intent
            {
                val uri = Uri.parse("geo:$lat,$lng?q=$lat,$lng")
                Intent(Intent.ACTION_VIEW, uri)
            },

            // Strategy 3: Google Maps web fallback
            {
                val uri = Uri.parse("https://maps.google.com/?q=$lat,$lng")
                Intent(Intent.ACTION_VIEW, uri)
            },

            // Strategy 4: OpenStreetMap
            {
                val uri = Uri.parse("geo:$lat,$lng")
                Intent(Intent.ACTION_VIEW, uri)
            }
        )

        return tryIntentStrategies(context, strategies)
    }

    /**
     * Open maps with address query using multiple fallback strategies
     */
    private fun openWithQuery(context: Context, query: String): Boolean {
        val encodedQuery = Uri.encode(query)

        val strategies = listOf(
            // Strategy 1: Google Maps search
            {
                val uri = Uri.parse("https://maps.google.com/?q=$encodedQuery")
                val intent = Intent(Intent.ACTION_VIEW, uri)
                intent.setPackage("com.google.android.apps.maps")
                intent
            },

            // Strategy 2: Generic geo search
            {
                val uri = Uri.parse("geo:0,0?q=$encodedQuery")
                Intent(Intent.ACTION_VIEW, uri)
            },

            // Strategy 3: Generic map search
            {
                val uri = Uri.parse("maps:q=$encodedQuery")
                Intent(Intent.ACTION_VIEW, uri)
            },

            // Strategy 4: Google Maps web
            {
                val uri = Uri.parse("https://maps.google.com/?q=$encodedQuery")
                Intent(Intent.ACTION_VIEW, uri)
            },

            // Strategy 5: Generic browser search
            {
                val uri = Uri.parse("https://www.google.com/maps/search/$encodedQuery")
                Intent(Intent.ACTION_VIEW, uri)
            }
        )

        return tryIntentStrategies(context, strategies)
    }

    /**
     * Try multiple intent strategies until one works
     */
    private fun tryIntentStrategies(context: Context, strategies: List<() -> Intent>): Boolean {
        for ((index, strategy) in strategies.withIndex()) {
            try {
                val intent = strategy()
                Log.d(TAG, "Trying strategy ${index + 1}: ${intent.data}")

                if (isIntentAvailable(context, intent)) {
                    Log.d(TAG, "Strategy ${index + 1} successful")
                    context.startActivity(intent)
                    return true
                } else {
                    Log.d(TAG, "Strategy ${index + 1} not available")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Strategy ${index + 1} failed: ${e.message}")
            }
        }

        Log.w(TAG, "All strategies failed")
        return false
    }

    /**
     * Check if an intent can be resolved
     */
    private fun isIntentAvailable(context: Context, intent: Intent): Boolean {
        return try {
            val activities = context.packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
            activities.isNotEmpty()
        } catch (e: Exception) {
            Log.w(TAG, "Error checking intent availability: ${e.message}")
            false
        }
    }

    /**
     * Show toast message to user
     */
    private fun showToast(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    /**
     * Open maps app with specific coordinates
     */
    fun openMapsWithCoordinates(context: Context, coordinates: GeoCoordinates) {
        val success = openWithCoordinates(context, coordinates.latitude, coordinates.longitude)
        if (!success) {
            showToast(context, "Nessuna app mappe disponibile")
        }
    }

    /**
     * Open maps app with text query
     */
    fun openMapsWithQuery(context: Context, query: String) {
        val success = openWithQuery(context, query)
        if (!success) {
            showToast(context, "Nessuna app mappe disponibile")
        }
    }

    /**
     * Debug: List available map applications
     */
    fun listAvailableMapApps(context: Context) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=test"))
        val activities = context.packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)

        Log.d(TAG, "Available map applications:")
        for (activity in activities) {
            Log.d(TAG, "- ${activity.activityInfo.packageName}: ${activity.loadLabel(context.packageManager)}")
        }

        if (activities.isEmpty()) {
            Log.d(TAG, "No map applications found")
        }
    }
}