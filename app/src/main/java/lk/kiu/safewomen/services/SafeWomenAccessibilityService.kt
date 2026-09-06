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
 * Global Hardware Volume Down Button Interceptor.
 *
 * Responsibilities:
 * 1. Global Interception: Intercepts physical Volume Down key presses even when the
 *    phone screen is locked and the Safe Women app is not open.
 * 2. 3-Second Continuous Hold Detection: Requires holding Volume Down continuously for 3 seconds.
 * 3. Existing Emergency Trigger: Calls the existing emergency function upon 3-second completion.
 * 4. Early-Release Cancellation: If released before 3 seconds, cancels the trigger immediately.
 * 5. Single-Press Anti-Duplicate Protection: Prevents duplicate triggers from a single long press.
 * 6. Normal Volume Operation: Passes short taps through to Android OS for normal volume reduction.
 */
class SafeWomenAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var preferenceManager: PreferenceManager
    private var powerManager: PowerManager? = null
    private var wakeLock: PowerManager.WakeLock? = null

    // Hold state tracking
    private var firstKeyDownTime = 0L
    private var lastKeyDownTime = 0L
    private var keyDownPulseCount = 0
    private var hasTriggeredForCurrentHold = false
    private var watchdogJob: Job? = null
    private var triggerTimerJob: Job? = null

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
            info.flags = info.flags or
                    AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS or
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                    AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
            info.eventTypes = AccessibilityEvent.TYPES_ALL_MASK
            info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            info.notificationTimeout = 50
            info.packageNames = null // Intercept globally across all apps and the lockscreen
            serviceInfo = info
            Log.i(TAG, "SafeWomenAccessibilityService connected with global key filter on lockscreen.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to apply AccessibilityServiceInfo: ${e.message}", e)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Passive sink
    }

    override fun onInterrupt() {
        Log.w(TAG, "SafeWomenAccessibilityService interrupted.")
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        // Filter strictly for Volume Down button
        if (event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            val threshold = preferenceManager.triggerDurationMs // 3000ms

            when (event.action) {
                KeyEvent.ACTION_DOWN -> {
                    val now = System.currentTimeMillis()

                    // If more than 1000ms elapsed since last pulse, treat as a fresh press sequence
                    if (firstKeyDownTime == 0L || (now - lastKeyDownTime > 1000L)) {
                        firstKeyDownTime = now
                        lastKeyDownTime = now
                        keyDownPulseCount = 1
                        hasTriggeredForCurrentHold = false
                        Log.d(TAG, "Volume Down initial press detected (Screen locked/background).")

                        try {
                            wakeLock?.acquire(threshold + 3000L)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }

                        // Start 3-second (3000ms) countdown coroutine
                        triggerTimerJob?.cancel()
                        triggerTimerJob = serviceScope.launch {
                            delay(threshold)
                            val heldDuration = System.currentTimeMillis() - firstKeyDownTime
                            // Verify that button is still held (pulses active within last 1000ms) and not yet triggered
                            if (firstKeyDownTime != 0L && !hasTriggeredForCurrentHold && (System.currentTimeMillis() - lastKeyDownTime <= 1000L)) {
                                hasTriggeredForCurrentHold = true
                                Log.i(TAG, "🚨 3-SECOND PHYSICAL VOLUME DOWN HOLD VERIFIED ON LOCKSCREEN! Triggering emergency...")

                                // Trigger existing emergency function
                                EmergencyTriggerCoordinator.triggerEmergency(
                                    context = this@SafeWomenAccessibilityService,
                                    triggerSource = "HARDWARE_VOLUME_DOWN_HOLD_ACCESSIBILITY",
                                    durationMs = heldDuration
                                )
                            }
                        }
                    } else {
                        // Continuing repeat pulse while physically holding button down
                        lastKeyDownTime = now
                        keyDownPulseCount++
                        val elapsed = now - firstKeyDownTime
                        Log.d(TAG, "Volume Down pulse #$keyDownPulseCount (elapsed=${elapsed}ms).")

                        // Subtle progressive tactile ticks at 1.0s and 2.0s
                        if (elapsed in 1000L..1300L && keyDownPulseCount in 2..6) {
                            EmergencyTriggerCoordinator.vibrateHoldCountdownTick(this, 1)
                        } else if (elapsed in 2000L..2300L && keyDownPulseCount in 5..12) {
                            EmergencyTriggerCoordinator.vibrateHoldCountdownTick(this, 2)
                        }

                        // If elapsed time reaches threshold via pulses, trigger immediately
                        if (elapsed >= threshold && !hasTriggeredForCurrentHold) {
                            hasTriggeredForCurrentHold = true
                            triggerTimerJob?.cancel()
                            watchdogJob?.cancel()
                            Log.i(TAG, "🚨 3-SECOND VOLUME DOWN HOLD REACHED VIA PULSES! Triggering emergency...")

                            // Trigger existing emergency function
                            EmergencyTriggerCoordinator.triggerEmergency(
                                context = this@SafeWomenAccessibilityService,
                                triggerSource = "HARDWARE_VOLUME_DOWN_HOLD_ACCESSIBILITY",
                                durationMs = elapsed
                            )
                            return true // Consume event
                        }
                    }

                    // Watchdog: If user releases button, pulses stop.
                    // After 1000ms of silence, reset hold state.
                    // A single short tap has pulseCount=1 and will be cleanly reset without triggering.
                    watchdogJob?.cancel()
                    watchdogJob = serviceScope.launch {
                        delay(1000L)
                        if (firstKeyDownTime != 0L) {
                            val totalHeld = lastKeyDownTime - firstKeyDownTime
                            if (!hasTriggeredForCurrentHold) {
                                Log.d(TAG, "Volume Down released after ${totalHeld}ms (< 3000ms). Resetting tracker.")
                            }
                            resetHoldState()
                        }
                    }

                    return false // Pass event to OS for normal volume reduction on short presses
                }

                KeyEvent.ACTION_UP -> {
                    val totalHeld = if (firstKeyDownTime != 0L) System.currentTimeMillis() - firstKeyDownTime else 0L
                    if (totalHeld < threshold) {
                        Log.d(TAG, "Volume Down released early at ${totalHeld}ms (< 3000ms). Emergency trigger cancelled.")
                    }
                    triggerTimerJob?.cancel()
                    triggerTimerJob = null
                    resetHoldState()
                    return false
                }
            }
        }

        // Pass all other keys transparently to OS
        return super.onKeyEvent(event)
    }

    private fun resetHoldState() {
        firstKeyDownTime = 0L
        lastKeyDownTime = 0L
        keyDownPulseCount = 0
        hasTriggeredForCurrentHold = false
        watchdogJob?.cancel()
        watchdogJob = null
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        triggerTimerJob?.cancel()
        watchdogJob?.cancel()
        serviceScope.cancel()
        resetHoldState()
        Log.i(TAG, "SafeWomenAccessibilityService destroyed.")
    }

    companion object {
        private const val TAG = "SafeWomenAccessibility"
    }
}
