package lk.kiu.safewomen.services

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.VolumeProvider
import android.media.session.MediaSession
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import lk.kiu.safewomen.R
import lk.kiu.safewomen.ui.MainActivity
import lk.kiu.safewomen.utils.Constants
import lk.kiu.safewomen.utils.PreferenceManager

/**
 * SafeWomenForegroundService:
 * Persistent foreground service that ensures background survival and hardware Volume Down
 * button interception even when the phone is locked and the display is completely turned off.
 *
 * Mechanism for Screen-Off / Locked Device Interception:
 * 1. Standard Android OS suspends Accessibility key filtering when the screen is powered off to save power.
 * 2. However, Android's Audio & Media Framework continues to route hardware volume rocker events to
 *    active media sessions.
 * 3. This service initializes a lightweight, inaudible AudioTrack (0dB digital silence) and registers
 *    a MediaSession with a custom VolumeProvider (VOLUME_CONTROL_RELATIVE).
 * 4. When the user presses Volume Down while the phone is locked in their bag/pocket with screen off:
 *    - The OS dispatches volume adjustments directly to onAdjustVolume(direction = -1).
 *    - Holding the physical button produces continuous key-repeat pulses (~200-250ms interval).
 *    - The service measures the continuous unbroken duration. If it reaches 3000ms (3.0 seconds),
 *      it triggers the emergency SMS dispatch pipeline!
 *    - A single short press or release prior to 3.0 seconds automatically resets after 600ms,
 *      preventing false alarms.
 */
class SafeWomenForegroundService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var preferenceManager: PreferenceManager
    private var powerManager: PowerManager? = null
    private var wakeLock: PowerManager.WakeLock? = null

    // MediaSession & VolumeProvider for Screen-Off Hardware Button Interception
    private var mediaSession: MediaSession? = null
    private var silentAudioTrack: AudioTrack? = null
    @Volatile
    private var isSilentAudioRunning = false

    // State tracking for continuous 3-second hold
    private var volumeDownFirstPressTime = 0L
    private var volumeDownLastPressTime = 0L
    private var volumeDownRepeatCount = 0
    private var volumeResetJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        preferenceManager = PreferenceManager(this)
        powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
        
        // Acquire PARTIAL_WAKE_LOCK to prevent CPU deep sleep while protection is active
        wakeLock = powerManager?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SafeWomen:ForegroundServiceWakeLock")
        try {
            wakeLock?.acquire()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        createNotificationChannel()
        setupMediaSessionVolumeInterceptor()
        startSilentAudioPlayback()
        Log.i(TAG, "SafeWomenForegroundService created and volume interceptor initialized.")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = buildForegroundNotification()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            var serviceType = ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                serviceType = serviceType or ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            }
            startForeground(Constants.FOREGROUND_NOTIFICATION_ID, notification, serviceType)
        } else {
            startForeground(Constants.FOREGROUND_NOTIFICATION_ID, notification)
        }

        return START_STICKY
    }

    /**
     * Sets up an active MediaSession with a Relative VolumeProvider.
     * Android routes physical volume rocker presses to this callback when the screen is locked/black.
     */
    private fun setupMediaSessionVolumeInterceptor() {
        try {
            mediaSession = MediaSession(this, "SafeWomenMediaSession").apply {
                setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS or MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS)

                val volumeProvider = object : VolumeProvider(VOLUME_CONTROL_RELATIVE, 100, 50) {
                    override fun onAdjustVolume(direction: Int) {
                        if (direction < 0) {
                            // Volume Down button pressed / held
                            handleVolumeDownAdjustment()
                        } else if (direction > 0) {
                            // Volume Up button - reset volume down accumulator
                            resetVolumeDownAccumulator("Volume Up pressed")
                        }
                    }
                }

                setPlaybackToRemote(volumeProvider)
                isActive = true
            }
            Log.i(TAG, "MediaSession VolumeProvider successfully activated.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize MediaSession: ${e.message}", e)
        }
    }

    /**
     * Evaluates continuous Volume Down hold duration.
     * When held, Android hardware drivers fire repeat events every ~200-250ms.
     */
    private fun handleVolumeDownAdjustment() {
        val now = System.currentTimeMillis()
        val threshold = preferenceManager.triggerDurationMs // Calibrated 3000ms (3.0s)

        if (volumeDownFirstPressTime == 0L || (now - volumeDownLastPressTime > 650L)) {
            // New hold sequence initiated
            volumeDownFirstPressTime = now
            volumeDownLastPressTime = now
            volumeDownRepeatCount = 1
            EmergencyTriggerCoordinator.vibrateShortTick(this)
            Log.d(TAG, "Physical Volume Down hold sequence started (Target: ${threshold}ms).")
        } else {
            // Ongoing hold sequence
            volumeDownLastPressTime = now
            volumeDownRepeatCount++
            val elapsed = now - volumeDownFirstPressTime
            Log.d(TAG, "Physical Volume Down holding: elapsed=${elapsed}ms, repeats=$volumeDownRepeatCount")

            if (elapsed >= threshold) {
                Log.i(TAG, "🎯 3-SECOND VOLUME DOWN THRESHOLD REACHED WHILE LOCKED! Dispatching SOS...")
                volumeDownFirstPressTime = 0L
                volumeDownLastPressTime = 0L
                volumeDownRepeatCount = 0
                volumeResetJob?.cancel()

                EmergencyTriggerCoordinator.triggerEmergency(
                    context = this@SafeWomenForegroundService,
                    triggerSource = "HARDWARE_VOLUME_DOWN_LOCKED_SCREEN_OFF",
                    durationMs = elapsed
                )
                return
            }
        }

        // Debounce reset: If user releases the button before 3s, no repeats arrive.
        // After 650ms of silence, reset the accumulator so short presses do NOT trigger an alert.
        volumeResetJob?.cancel()
        volumeResetJob = serviceScope.launch {
            delay(650L)
            if (volumeDownFirstPressTime != 0L) {
                val heldDuration = volumeDownLastPressTime - volumeDownFirstPressTime
                Log.d(TAG, "Volume Down released after ${heldDuration}ms (< ${threshold}ms). Normal volume adjustment, false alarm prevented.")
                volumeDownFirstPressTime = 0L
                volumeDownLastPressTime = 0L
                volumeDownRepeatCount = 0
            }
        }
    }

    private fun resetVolumeDownAccumulator(reason: String) {
        if (volumeDownFirstPressTime != 0L) {
            Log.d(TAG, "Volume Down accumulator reset ($reason).")
            volumeDownFirstPressTime = 0L
            volumeDownLastPressTime = 0L
            volumeDownRepeatCount = 0
            volumeResetJob?.cancel()
        }
    }

    /**
     * Plays digital silence (0dB PCM buffer) through a background AudioTrack.
     * This ensures the Android OS audio subsystem keeps active audio focus and routes hardware
     * volume keys to this service even when the device is locked and the display is completely black.
     */
    private fun startSilentAudioPlayback() {
        serviceScope.launch(Dispatchers.Default) {
            try {
                val sampleRate = 8000
                val minBufferSize = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
                val silentBuffer = ShortArray(minBufferSize) // All zeros = pure silence

                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()

                val audioFormat = AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .build()

                silentAudioTrack = AudioTrack.Builder()
                    .setAudioAttributes(audioAttributes)
                    .setAudioFormat(audioFormat)
                    .setBufferSizeInBytes(minBufferSize * 2)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

                silentAudioTrack?.play()
                isSilentAudioRunning = true

                while (isSilentAudioRunning && silentAudioTrack != null) {
                    silentAudioTrack?.write(silentBuffer, 0, silentBuffer.size)
                    delay(800L) // Low-overhead loop
                }
            } catch (e: Exception) {
                Log.w(TAG, "Silent audio loop initialization note: ${e.message}")
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                Constants.NOTIFICATION_CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_description)
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildForegroundNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, Constants.NOTIFICATION_CHANNEL_ID)
            .setContentTitle("SAFE Women Active Protection")
            .setContentText("Emergency listener active. Hold Volume Down for 3s to alert guardians.")
            .setSmallIcon(android.R.drawable.ic_lock_idle_charging)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "SafeWomenForegroundService stopping. Releasing resources...")
        
        isSilentAudioRunning = false
        try {
            silentAudioTrack?.stop()
            silentAudioTrack?.release()
            silentAudioTrack = null
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            mediaSession?.isActive = false
            mediaSession?.release()
            mediaSession = null
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        volumeResetJob?.cancel()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "SafeWomenForeground"

        fun startService(context: Context) {
            val intent = Intent(context, SafeWomenForegroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, SafeWomenForegroundService::class.java)
            context.stopService(intent)
        }
    }
}
