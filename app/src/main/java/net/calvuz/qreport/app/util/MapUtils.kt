package net.calvuz.qreport.app.app.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import net.calvuz.qreport.app.app.domain.model.Address
import net.calvuz.qreport.app.app.domain.model.GeoCoordinates
import timber.log.Timber

/**
 * Utility class for opening map applications - MINIMAL approach
 *
 * Strategy:
 * 1. Google Maps app (if installed)
 * 2. Generic geo intent (automatically handled by ANY map app)
 * 3. Browser fallback
 */
object MapUtils {

    /**
     * Open maps app with facility address
     */
    fun openMapsWithAddress(context: Context, address: Address) {
        try {
            Timber.d("Opening maps with address: ${address.toDisplayString()}")

            when {
                address.hasCoordinates() -> {
                    val coords = address.coordinates!!
                    openMapsWithCoordinates(context, coords)
                }
                address.isComplete() -> {
                    openMapsWithQuery(context, address.toDisplayString())
                }
                else -> {
                    showToast(context, "Indirizzo incompleto")
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error opening maps")
            showToast(context, "Errore apertura mappe")
        }
    }

    /**
     * Open maps with coordinates - 3 strategies only
     */
    fun openMapsWithCoordinates(context: Context, coordinates: GeoCoordinates) {
        val lat = coordinates.latitude
        val lng = coordinates.longitude

        Timber.d("Opening maps with coordinates: $lat, $lng")

        val attempts = listOf(
            // 1. Google Maps app (if installed)
            { openGoogleMapsApp(context, lat, lng) },

            // 2. Generic geo intent (ANY map app: Waze, HERE, OSM, etc.)
            { openGeoIntent(context, "geo:$lat,$lng?q=$lat,$lng") },

            // 3. Browser fallback
            { openInBrowser(context, "https://maps.google.com/?q=$lat,$lng") }
        )

        executeAttempts(context, attempts, "coordinate")
    }

    /**
     * Open maps with address query - 3 strategies only
     */
    fun openMapsWithQuery(context: Context, query: String) {
        Timber.d("Opening maps with query: $query")

        val encodedQuery = Uri.encode(query)

        val attempts = listOf(
            // 1. Google Maps app (if installed)
            { openGoogleMapsApp(context, query) },

            // 2. Generic geo intent (ANY map app: Waze, HERE, OSM, etc.)
            { openGeoIntent(context, "geo:0,0?q=$encodedQuery") },

            // 3. Browser fallback
            { openInBrowser(context, "https://maps.google.com/?q=$encodedQuery") }
        )

        executeAttempts(context, attempts, "query")
    }

    /**
     * Execute attempts until one succeeds
     */
    private fun executeAttempts(context: Context, attempts: List<() -> Boolean>, type: String) {
        for ((index, attempt) in attempts.withIndex()) {
            try {
                Timber.d("Trying $type attempt ${index + 1}")
                if (attempt()) {
                    Timber.d("$type attempt ${index + 1} succeeded")
                    return
                }
            } catch (e: Exception) {
                Timber.w(e, "$type attempt ${index + 1} failed")
            }
        }

        Timber.w("All $type attempts failed")
        showToast(context, "Impossibile aprire le mappe")
    }

    /**
     * Strategy 1: Google Maps app (if installed)
     */
    private fun openGoogleMapsApp(context: Context, lat: Double, lng: Double): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:$lat,$lng"))
            intent.setPackage("com.google.android.apps.maps")
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Timber.d(e, "Google Maps app (coordinates) not available")
            false
        }
    }

    /**
     * Strategy 1b: Google Maps app with query
     */
    private fun openGoogleMapsApp(context: Context, query: String): Boolean {
        return try {
            val encodedQuery = Uri.encode(query)
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=$encodedQuery"))
            intent.setPackage("com.google.android.apps.maps")
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Timber.d(e, "Google Maps app (query) not available")
            false
        }
    }

    /**
     * Strategy 2: Generic geo intent (ANY map app automatically)
     */
    private fun openGeoIntent(context: Context, geoUri: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(geoUri))
            context.startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            Timber.d(e, "No app available for geo intent")
            false
        }
    }

    /**
     * Strategy 3: Browser fallback (always works)
     */
    private fun openInBrowser(context: Context, url: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            Timber.w(e, "Even browser fallback failed")
            false
        }
    }

    /**
     * Show toast message
     */
    private fun showToast(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    /**
     * Check if Google Maps is installed
     */
    fun isGoogleMapsInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo("com.google.android.apps.maps", 0)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Debug function to test and show available options
     */
    fun debugMapCapabilities(context: Context) {
        Timber.d("=== MAP CAPABILITIES DEBUG ===")
        Timber.d("Google Maps installed: ${isGoogleMapsInstalled(context)}")

        // Test basic geo intent availability
        val geoIntent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=test"))
        val geoApps = context.packageManager.queryIntentActivities(geoIntent, 0)
        Timber.d("Apps handling geo intents: ${geoApps.size}")

        // Test browser availability
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://maps.google.com"))
        val browserAvailable = browserIntent.resolveActivity(context.packageManager) != null
        Timber.d("Browser available: $browserAvailable")
    }
}