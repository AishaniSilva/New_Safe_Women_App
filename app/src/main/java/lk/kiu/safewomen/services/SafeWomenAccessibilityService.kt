package lk.kiu.safewomen.services

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.os.PowerManager
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import kotlinx.coroutines.*
import lk.kiu.safewomen.utils.PreferenceManager

/**
 * SafeWomenAccessibilityService:
 * Hardware Key Event Filter for physical buttons.
 *
 * Intercepts physical Volume Down presses while the app is in the background or on the Lock Screen.
 * Enforces a strict 3-second (3000ms) continuous hold requirement.
 * Releasing before 3 seconds cancels the timer, completely preventing normal volume adjustments
 * from triggering an emergency alert.
 */
class SafeWomenAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var preferenceManager: PreferenceManager
    private var powerManager: PowerManager? = null
    private var wakeLock: PowerManager.WakeLock? = null

    private var firstKeyDownTime = 0L
    private var lastKeyDownTime = 0L
    private var keyDownPulseCount = 0
    private var watchdogJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        preferenceManager = PreferenceManager(this)
        powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
        wakeLock = powerManager?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SafeWomen:AccessibilityKeyLock")
        Log.d(TAG, "SafeWomenAccessibilityService created.")
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        try {
            val info = serviceInfo ?: AccessibilityServiceInfo()
            info.flags = info.flags or AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
            info.eventTypes = AccessibilityEvent.TYPES_ALL_MASK
            info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            info.notificationTimeout = 50
            serviceInfo = info
            Log.i(TAG, "SafeWomenAccessibilityService connected with FLAG_REQUEST_FILTER_KEY_EVENTS.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to apply AccessibilityServiceInfo: ${e.message}", e)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Passive accessibility event sink
    }

    override fun onInterrupt() {
        Log.w(TAG, "SafeWomenAccessibilityService interrupted.")
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        // Strictly filter for Volume Down key only
        if (event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            val threshold = preferenceManager.triggerDurationMs // Calibrated 3000ms (3.0 seconds)

            when (event.action) {
                KeyEvent.ACTION_DOWN -> {
                    val now = System.currentTimeMillis()

                    // If pulses are more than 400ms apart, previous press was released
                    if (firstKeyDownTime == 0L || (now - lastKeyDownTime > 400L)) {
                        firstKeyDownTime = now
                        lastKeyDownTime = now
                        keyDownPulseCount = 1
                        Log.d(TAG, "Hardware Volume Down initial pulse received.")

                        try {
                            wakeLock?.acquire(threshold + 2000L)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    } else {
                        // Continuous hardware repeat pulses while physically holding button
                        lastKeyDownTime = now
                        keyDownPulseCount++
                        val elapsed = now - firstKeyDownTime
                        Log.d(TAG, "Hardware Volume Down holding pulse: count=$keyDownPulseCount, elapsed=${elapsed}ms")

                        // Subtle tactile ticks during active hold
                        if (elapsed in 1000L..1250L && keyDownPulseCount in 3..5) {
                            EmergencyTriggerCoordinator.vibrateHoldCountdownTick(this, 1)
                        } else if (elapsed in 2000L..2250L && keyDownPulseCount in 6..8) {
                            EmergencyTriggerCoordinator.vibrateHoldCountdownTick(this, 2)
                        }

                        // STRICT TRIGGER: Must exceed continuous 3000ms AND have received at least 7 repeat pulses!
                        // A single short touch will have count=1 and elapsed=0ms, so it is IMPOSSIBLE to trigger here.
                        if (elapsed >= threshold && keyDownPulseCount >= 7) {
                            Log.i(TAG, "🎯 3-SECOND PHYSICAL VOLUME DOWN HOLD VERIFIED! (Elapsed: ${elapsed}ms, Pulses: $keyDownPulseCount). Triggering emergency...")
                            firstKeyDownTime = 0L
                            lastKeyDownTime = 0L
                            keyDownPulseCount = 0
                            watchdogJob?.cancel()

                            EmergencyTriggerCoordinator.triggerEmergency(
                                context = this@SafeWomenAccessibilityService,
                                triggerSource = "HARDWARE_VOLUME_DOWN_HOLD_ACCESSIBILITY",
                                durationMs = elapsed
                            )
                            return true
                        }
                    }

                    // Watchdog: If button is released, repeat pulses stop immediately.
                    // After 400ms without a new pulse, reset the hold tracker completely.
                    watchdogJob?.cancel()
                    watchdogJob = serviceScope.launch {
                        delay(400L)
                        if (firstKeyDownTime != 0L) {
                            val held = lastKeyDownTime - firstKeyDownTime
                            Log.d(TAG, "Volume Down released after ${held}ms (Pulses=$keyDownPulseCount < 7). Normal volume adjustment, false alarm prevented.")
                            firstKeyDownTime = 0L
                            lastKeyDownTime = 0L
                            keyDownPulseCount = 0
                            try {
                                if (wakeLock?.isHeld == true) {
                                    wakeLock?.release()
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                    return false // Pass event to OS for normal volume adjustment on short presses
                }

                KeyEvent.ACTION_UP -> {
                    firstKeyDownTime = 0L
                    lastKeyDownTime = 0L
                    keyDownPulseCount = 0
                    watchdogJob?.cancel()
                    watchdogJob = null
                    try {
                        if (wakeLock?.isHeld == true) {
                            wakeLock?.release()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    return false
                }
            }
        }

        // Pass all other keys (Volume Up, Power, etc.) transparently to OS
        return super.onKeyEvent(event)
    }

    override fun onDestroy() {
        super.onDestroy()
        watchdogJob?.cancel()
        serviceScope.cancel()
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        Log.i(TAG, "SafeWomenAccessibilityService destroyed.")
    }

    companion object {
        private const val TAG = "SafeWomenAccessibility"
    }
}
