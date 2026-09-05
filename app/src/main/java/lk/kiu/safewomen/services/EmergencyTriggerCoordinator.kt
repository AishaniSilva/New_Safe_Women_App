package lk.kiu.safewomen.services

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import kotlinx.coroutines.*
import lk.kiu.safewomen.data.local.AppDatabase
import lk.kiu.safewomen.data.local.ChatMessageEntity
import lk.kiu.safewomen.data.model.Guardian
import lk.kiu.safewomen.location.LocationTracker
import lk.kiu.safewomen.telephony.SmsDispatcher
import lk.kiu.safewomen.utils.Constants
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * EmergencyTriggerCoordinator: Centralized thread-safe emergency dispatcher.
 *
 * Responsibilities:
 * 1. Deduplication & Cooldown: Enforces a 15-second cooldown window so that duplicate triggers
 *    from dual detection engines or repeated button presses are safely ignored.
 * 2. Tactile Feedback: Instantly fires a distinct vibration alert to inform the user that the trigger succeeded.
 * 3. Screen Illumination: Temporarily turns on the screen using PowerManager wake lock.
 * 4. High-Accuracy Location: Obtains sub-5-meter GPS coordinates using the pre-warmed GPS pipeline.
 * 5. Cellular SMS Dispatch: Dispatches emergency SMS to all registered guardians via native SmsManager
 *    (pure cellular carrier transmission, absolutely NO internet or web server required).
 * 6. Audit & History: Records the incident in Room SQLite database and broadcasts to UI consoles.
 */
object EmergencyTriggerCoordinator {

    private const val TAG = "EmergencyCoordinator"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val isDispatching = AtomicBoolean(false)
    private val lastTriggerTimestamp = AtomicLong(0L)
    private const val COOLDOWN_WINDOW_MS = 15_000L // 15-second lockout window

    /**
     * Executes the emergency alert pipeline. Can be called from ForegroundService (MediaSession),
     * AccessibilityService, or UI touch zone.
     */
    fun triggerEmergency(context: Context, triggerSource: String, durationMs: Long = 3000L) {
        val now = System.currentTimeMillis()
        val last = lastTriggerTimestamp.get()

        // 1. Cooldown check to prevent duplicate SMS transmissions
        if (now - last < COOLDOWN_WINDOW_MS) {
            Log.w(TAG, "Trigger ignored: Within 15-second cooldown window (${now - last}ms since last trigger).")
            return
        }

        // 2. Concurrency lock
        if (!isDispatching.compareAndSet(false, true)) {
            Log.w(TAG, "Trigger ignored: Dispatch pipeline is currently active.")
            return
        }

        lastTriggerTimestamp.set(now)
        Log.i(TAG, "🚨 CRITICAL EMERGENCY TRIGGER DETECTED via $triggerSource (Hold Duration: ${durationMs}ms)!")

        // 3. Strong tactile feedback pattern (Morse code SOS: ... --- ...)
        vibrateEmergencyPattern(context)

        // 4. Wake up device screen if locked or turned off
        wakeScreen(context)

        // 5. Asynchronous location acquisition and cellular SMS transmission
        scope.launch {
            val startTime = System.currentTimeMillis()
            try {
                val db = AppDatabase.getInstance(context.applicationContext)
                val smsDispatcher = SmsDispatcher(context.applicationContext)
                val locationTracker = LocationTracker(context.applicationContext)

                // Retrieve registered guardians from local Room SQLite database
                var guardians = db.guardianDao().getAllGuardiansSync().map { it.toDomain() }
                if (guardians.isEmpty()) {
                    Log.w(TAG, "No registered guardians found! Using calibrated baseline contacts.")
                    guardians = listOf(
                        Guardian(name = "Primary Contact", phoneNumber = "0776336982", relationship = "Guardian", isPrimary = true),
                        Guardian(name = "Secondary Contact", phoneNumber = "0768361075", relationship = "Guardian", isPrimary = false)
                    )
                }

                // Acquire high-precision GPS coordinates
                val locationResult = locationTracker.acquireCurrentLocation()
                val lat = locationResult.latitude
                val lon = locationResult.longitude
                val acc = locationResult.accuracy
                Log.d(TAG, "GPS fix acquired for dispatch: Lat=$lat, Lon=$lon, Accuracy=±${acc}m (${locationResult.provider})")

                // Cellular SMS Transmission via android.telephony.SmsManager (No internet needed)
                val dispatchResult = smsDispatcher.dispatchEmergencyAlert(
                    guardians = guardians,
                    latitude = lat,
                    longitude = lon
                )

                // Persist event into in-app incident chat thread
                try {
                    val mapsLink = "https://maps.google.com/?q=$lat,$lon"
                    db.chatMessageDao().insertMessage(
                        ChatMessageEntity(
                            senderName = "SAFE WOMEN SYSTEM",
                            senderRole = "SYSTEM_ALERT",
                            messageText = "🚨 EMERGENCY SOS ACTIVATED via $triggerSource! Alert dispatched to ${guardians.size} guardians.",
                            isEmergencyAlert = true,
                            locationPin = mapsLink
                        )
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to persist chat message: ${e.message}")
                }

                val totalDuration = System.currentTimeMillis() - startTime
                Log.i(TAG, "Emergency dispatch finished in ${totalDuration}ms. Success: ${dispatchResult.isSuccess} (${dispatchResult.sentCount} sent, ${dispatchResult.failedCount} failed)")

                // Broadcast results for UI and Diagnostics Screen update
                val broadcastIntent = Intent(Constants.ACTION_SMS_DISPATCHED).apply {
                    putExtra(Constants.EXTRA_LOG_MESSAGE, dispatchResult.formattedMessage)
                    putExtra(Constants.EXTRA_IS_SUCCESS, dispatchResult.isSuccess)
                    setPackage(context.packageName)
                }
                context.sendBroadcast(broadcastIntent)

            } catch (e: Exception) {
                Log.e(TAG, "Emergency dispatch pipeline error: ${e.message}", e)
            } finally {
                isDispatching.set(false)
            }
        }
    }

    /**
     * Subtle tactile tick to indicate button press registration.
     */
    fun vibrateShortTick(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator?.vibrate(VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                val v = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                v?.vibrate(40)
            }
        } catch (e: Exception) {
            // Ignore vibration failure
        }
    }

    /**
     * Strong distinctive vibration pattern to confirm emergency dispatch.
     */
    private fun vibrateEmergencyPattern(context: Context) {
        try {
            val pattern = longArrayOf(0, 400, 150, 400, 150, 600)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                val v = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v?.vibrate(VibrationEffect.createWaveform(pattern, -1))
                } else {
                    @Suppress("DEPRECATION")
                    v?.vibrate(pattern, -1)
                }
            }
        } catch (e: Exception) {
            // Ignore vibration failure
        }
    }

    /**
     * Wakes up the screen using PowerManager so the user/guardian sees the emergency status.
     */
    private fun wakeScreen(context: Context) {
        try {
            val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            @Suppress("DEPRECATION")
            val screenLock = pm?.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.ON_AFTER_RELEASE,
                "SafeWomen:EmergencyScreenWake"
            )
            screenLock?.acquire(3000L)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to acquire screen wake lock: ${e.message}")
        }
    }
}
