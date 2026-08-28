package lk.kiu.safewomen.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Build
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import lk.kiu.safewomen.utils.Constants

data class SafeLocationResult(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val isFallback: Boolean = false,
    val provider: String = "GPS_FUSED"
)

class LocationTracker(private val context: Context) {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    suspend fun acquireCurrentLocation(): SafeLocationResult {
        return try {
            val cancellationTokenSource = CancellationTokenSource()
            
            // Try fetching fresh high-accuracy location with a 4-second timeout
            val freshLocation: Location? = withTimeoutOrNull(4000L) {
                fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    cancellationTokenSource.token
                ).await()
            }

            if (freshLocation != null) {
                SafeLocationResult(
                    latitude = freshLocation.latitude,
                    longitude = freshLocation.longitude,
                    accuracy = freshLocation.accuracy,
                    isFallback = false,
                    provider = freshLocation.provider ?: "FusedGPS"
                )
            } else {
                // Fallback 1: Try cached last location
                val lastLoc = fusedLocationClient.lastLocation.await()
                if (lastLoc != null) {
                    SafeLocationResult(
                        latitude = lastLoc.latitude,
                        longitude = lastLoc.longitude,
                        accuracy = lastLoc.accuracy,
                        isFallback = false,
                        provider = "CachedLastLocation"
                    )
                } else {
                    // Fallback 2: Default transit hub coordinates (Colombo)
                    SafeLocationResult(
                        latitude = Constants.DEFAULT_FALLBACK_LATITUDE,
                        longitude = Constants.DEFAULT_FALLBACK_LONGITUDE,
                        accuracy = 100.0f,
                        isFallback = true,
                        provider = "ColomboFallbackHub"
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            SafeLocationResult(
                latitude = Constants.DEFAULT_FALLBACK_LATITUDE,
                longitude = Constants.DEFAULT_FALLBACK_LONGITUDE,
                accuracy = 100.0f,
                isFallback = true,
                provider = "ErrorFallbackHub"
            )
        }
    }
}
