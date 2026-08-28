package lk.kiu.safewomen.services

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import kotlinx.coroutines.*
import lk.kiu.safewomen.data.local.AppDatabase
import lk.kiu.safewomen.data.model.Guardian
import lk.kiu.safewomen.data.model.TriggerMode
import lk.kiu.safewomen.location.LocationTracker
import lk.kiu.safewomen.telephony.SmsDispatcher
import lk.kiu.safewomen.utils.Constants
import lk.kiu.safewomen.utils.PreferenceManager

class SafeWomenAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var preferenceManager: PreferenceManager
    private lateinit var locationTracker: LocationTracker
    private lateinit var smsDispatcher: SmsDispatcher
    private var powerManager: PowerManager? = null
    private var wakeLock: PowerManager.WakeLock? = null

    private var triggerJob: Job? = null
    private var isKeyDown: Boolean = false

    override fun onCreate() {
        super.onCreate()
        preferenceManager = PreferenceManager(this)
        locationTracker = LocationTracker(this)
        smsDispatcher = SmsDispatcher(this)
        powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
        wakeLock = powerManager?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SafeWomen:KeyEventListenerLock")
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
            e.printStackTrace()
            Log.e(TAG, "Failed to apply AccessibilityServiceInfo: ${e.message}")
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Passive listener
    }

    override fun onInterrupt() {
        Log.w(TAG, "SafeWomenAccessibilityService interrupted.")
    }

    private val clickTimestamps = mutableListOf<Long>()

    override fun onKeyEvent(event: KeyEvent): Boolean {
        // Emergency trigger operates regardless of lock screen or app state
        val keyCode = event.keyCode
        val isVolumeKey = (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP)

        if (isVolumeKey) {
            val threshold = preferenceManager.triggerDurationMs

            when (event.action) {
                KeyEvent.ACTION_DOWN -> {
                    val now = System.currentTimeMillis()

                    // --- 1. RAPID TRIPLE CLICK DETECTION (3 clicks within 1500ms) ---
                    synchronized(clickTimestamps) {
                        clickTimestamps.removeAll { now - it > 1500L }
                        clickTimestamps.add(now)
                        if (clickTimestamps.size >= 3) {
                            Log.i(TAG, "Rapid Triple Click detected (${clickTimestamps.size} clicks within 1.5s)! Triggering emergency dispatch...")
                            clickTimestamps.clear()
                            triggerJob?.cancel()
                            isKeyDown = false
                            executeSilentEmergencyPipeline(
                                triggerType = "RAPID_TRIPLE_CLICK",
                                latencyMs = now - (clickTimestamps.firstOrNull() ?: now)
                            )
                            return true
                        }
                    }

                    // --- 2. CONTINUOUS 3.0-SECOND HOLD DETECTION ---
                    if (!isKeyDown) {
                        isKeyDown = true
                        Log.d(TAG, "Hardware Volume button pressed. Starting $threshold ms hold timer...")

                        // Acquire CPU WakeLock so process stays awake
                        try {
                            wakeLock?.acquire(threshold + 5000L)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }

                        // Immediate subtle tactile tick
                        triggerShortTick()

                        // Launch active hold timer
                        triggerJob?.cancel()
                        triggerJob = serviceScope.launch {
                            delay(threshold)
                            if (isKeyDown) {
                                Log.i(TAG, "Volume button hold threshold reached ($threshold ms)! Initiating emergency dispatch...")
                                executeSilentEmergencyPipeline(
                                    triggerType = "VOLUME_BUTTON_HOLD",
                                    latencyMs = threshold
                                )
                            }
                        }
                    }
                }

                KeyEvent.ACTION_UP -> {
                    Log.d(TAG, "Volume button released. Resetting continuous hold timer.")
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
                }
            }
        }

        return super.onKeyEvent(event)
    }

    private fun executeSilentEmergencyPipeline(triggerType: String, latencyMs: Long) {
        // 1. Strong tactile emergency vibration
        triggerEmergencyVibration()

        // 2. Wake up screen if locked
        try {
            val screenWakeLock = powerManager?.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.ON_AFTER_RELEASE,
                "SafeWomen:EmergencyScreenWakeup"
            )
            screenWakeLock?.acquire(3000L)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 3. Background location acquisition and cellular SMS transmission
        serviceScope.launch {
            val startTime = System.currentTimeMillis()
            try {
                val db = AppDatabase.getInstance(applicationContext)
                var guardians = db.guardianDao().getAllGuardiansSync().map { it.toDomain() }

                // Fallback default numbers if none configured
                if (guardians.isEmpty()) {
                    guardians = listOf(
                        Guardian(name = "Primary Contact", phoneNumber = "0776336982", relationship = "Guardian", isPrimary = true),
                        Guardian(name = "Secondary Contact", phoneNumber = "0768361075", relationship = "Guardian", isPrimary = false)
                    )
                }

                val location = locationTracker.acquireCurrentLocation()
                val dispatchResult = smsDispatcher.dispatchEmergencyAlert(
                    guardians = guardians,
                    latitude = location.latitude,
                    longitude = location.longitude
                )

                // Record into in-app guardian incident thread
                try {
                    val mapsLink = "https://maps.google.com/?q=${location.latitude},${location.longitude}"
                    db.chatMessageDao().insertMessage(
                        lk.kiu.safewomen.data.local.ChatMessageEntity(
                            senderName = "SAFE WOMEN SYSTEM",
                            senderRole = "SYSTEM_ALERT",
                            messageText = "🚨 EMERGENCY SOS ACTIVATED via Hardware Volume Hold! Alert dispatched to ${guardians.size} guardians.",
                            isEmergencyAlert = true,
                            locationPin = mapsLink
                        )
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                val totalDuration = System.currentTimeMillis() - startTime
                Log.i(TAG, "Emergency dispatch complete in $totalDuration ms. Success: ${dispatchResult.isSuccess}")

                // Broadcast results for UI & Diagnostics Console
                val broadcastIntent = Intent(Constants.ACTION_SMS_DISPATCHED).apply {
                    putExtra(Constants.EXTRA_LOG_MESSAGE, dispatchResult.formattedMessage)
                    putExtra(Constants.EXTRA_IS_SUCCESS, dispatchResult.isSuccess)
                    setPackage(packageName)
                }
                sendBroadcast(broadcastIntent)

            } catch (e: Exception) {
                e.printStackTrace()
                Log.e(TAG, "Emergency pipeline failed: ${e.message}")
            }
        }
    }

    private fun triggerShortTick() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator?.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                val v = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                v?.vibrate(50)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun triggerEmergencyVibration() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 350, 150, 350), -1))
            } else {
                @Suppress("DEPRECATION")
                val v = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 350, 150, 350), -1))
                } else {
                    @Suppress("DEPRECATION")
                    v?.vibrate(longArrayOf(0, 350, 150, 350), -1)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
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
    }

    companion object {
        private const val TAG = "SafeWomenAccessibility"
    }
}
