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

    private var triggerJob: Job? = null
    private var isKeyDown: Boolean = false
    private var keyDownStartTime: Long = 0L

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
                    if (!isKeyDown) {
                        isKeyDown = true
                        keyDownStartTime = System.currentTimeMillis()
                        Log.d(TAG, "Hardware Volume Down pressed on lockscreen/background. Starting ${threshold}ms hold timer...")

                        try {
                            wakeLock?.acquire(threshold + 4000L)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }

                        // Immediate tactile tick acknowledging button press
                        EmergencyTriggerCoordinator.vibrateShortTick(this)

                        // Launch active hold countdown with progressive haptic pulses
                        triggerJob?.cancel()
                        triggerJob = serviceScope.launch {
                            delay(1000L)
                            if (isKeyDown) {
                                EmergencyTriggerCoordinator.vibrateHoldCountdownTick(this@SafeWomenAccessibilityService, 1)
                            }
                            delay(1000L)
                            if (isKeyDown) {
                                EmergencyTriggerCoordinator.vibrateHoldCountdownTick(this@SafeWomenAccessibilityService, 2)
                            }
                            delay(1000L)
                            if (isKeyDown) {
                                Log.i(TAG, "🎯 3-SECOND VOLUME DOWN HOLD COMPLETED! Initiating emergency dispatch...")
                                EmergencyTriggerCoordinator.triggerEmergency(
                                    context = this@SafeWomenAccessibilityService,
                                    triggerSource = "HARDWARE_VOLUME_DOWN_HOLD_ACCESSIBILITY",
                                    durationMs = threshold
                                )
                            }
                        }
                    } else {
                        // Key repeat while holding
                        val elapsed = System.currentTimeMillis() - keyDownStartTime
                        Log.d(TAG, "Hardware Volume Down holding: elapsed=${elapsed}ms, repeatCount=${event.repeatCount}")
                        if (elapsed >= threshold && triggerJob?.isActive == true) {
                            triggerJob?.cancel()
                            triggerJob = null
                            Log.i(TAG, "🎯 3-SECOND VOLUME DOWN KEY REPEAT TRIGGER! Initiating emergency dispatch...")
                            EmergencyTriggerCoordinator.triggerEmergency(
                                context = this@SafeWomenAccessibilityService,
                                triggerSource = "HARDWARE_VOLUME_DOWN_REPEAT_ACCESSIBILITY",
                                durationMs = elapsed
                            )
                        }
                    }
                    return true // Consume key event to prevent volume slider popup on lockscreen
                }

                KeyEvent.ACTION_UP -> {
                    val duration = System.currentTimeMillis() - keyDownStartTime
                    Log.d(TAG, "Hardware Volume Down released after ${duration}ms. Resetting hold timer.")
                    isKeyDown = false
                    triggerJob?.cancel()
                    triggerJob = null
                    try {
                        if (wakeLock?.isHeld == true) {
                            wakeLock?.release()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    return duration >= threshold
                }
            }
        }

        // Pass all other keys (Volume Up, Power, etc.) transparently to OS
        return super.onKeyEvent(event)
    }

    override fun onDestroy() {
        super.onDestroy()
        triggerJob?.cancel()
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
