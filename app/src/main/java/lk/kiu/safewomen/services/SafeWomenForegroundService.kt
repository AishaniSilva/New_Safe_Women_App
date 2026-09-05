package lk.kiu.safewomen.services

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.VolumeProvider
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
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
 * 4. Audio Focus (AUDIOFOCUS_GAIN) and PlaybackState.STATE_PLAYING are maintained to ensure AudioService
 *    routes hardware volume rocker adjustments to this session.
 * 5. A BroadcastReceiver for android.media.VOLUME_CHANGED_ACTION provides a dual-tier volume change listener.
 * 6. Continuous unbroken Volume Down duration is measured. If it reaches 3000ms (3.0s), the emergency pipeline fires.
 */
class SafeWomenForegroundService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var preferenceManager: PreferenceManager
    private var powerManager: PowerManager? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null

    // MediaSession & VolumeProvider for Screen-Off Hardware Button Interception
    private var mediaSession: MediaSession? = null
    private var silentAudioTrack: AudioTrack? = null
    @Volatile
    private var isSilentAudioRunning = false
    private var isVolumeReceiverRegistered = false

    // State tracking for continuous 3-second hold
    private var volumeDownFirstPressTime = 0L
    private var volumeDownLastPressTime = 0L
    private var volumeDownRepeatCount = 0
    private var volumeResetJob: Job? = null

    private val volumeChangeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "android.media.VOLUME_CHANGED_ACTION") {
                val newVol = intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_VALUE", -1)
                val oldVol = intent.getIntExtra("android.media.EXTRA_PREV_VOLUME_STREAM_VALUE", -1)
                Log.d(TAG, "VOLUME_CHANGED_ACTION broadcast: old=$oldVol, new=$newVol")
                if (newVol <= oldVol) {
                    handleVolumeDownAdjustment()
                } else {
                    resetVolumeDownAccumulator("Volume increased")
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        preferenceManager = PreferenceManager(this)
        powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
        audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        
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

        try {
            registerReceiver(volumeChangeReceiver, IntentFilter("android.media.VOLUME_CHANGED_ACTION"))
            isVolumeReceiverRegistered = true
            Log.i(TAG, "VOLUME_CHANGED_ACTION receiver registered.")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to register VOLUME_CHANGED_ACTION receiver: ${e.message}")
        }

        Log.i(TAG, "SafeWomenForegroundService created and volume interceptor initialized.")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = buildForegroundNotification()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                var serviceType = ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                val fineLocGranted = ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                if (fineLocGranted) {
                    serviceType = serviceType or ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                }
                startForeground(Constants.FOREGROUND_NOTIFICATION_ID, notification, serviceType)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(Constants.FOREGROUND_NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
            } else {
                startForeground(Constants.FOREGROUND_NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting foreground with specific type: ${e.message}", e)
            try {
                startForeground(Constants.FOREGROUND_NOTIFICATION_ID, notification)
            } catch (e2: Exception) {
                Log.e(TAG, "Fallback startForeground failed: ${e2.message}", e2)
            }
        }

        return START_STICKY
    }

    /**
     * Requests audio focus so Android OS routes hardware volume keys to this service's MediaSession.
     */
    private fun requestAudioFocus() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val playbackAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
                audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(playbackAttributes)
                    .setAcceptsDelayedFocusGain(true)
                    .setOnAudioFocusChangeListener { focusChange ->
                        Log.d(TAG, "Audio focus changed: $focusChange")
                    }
                    .build()
                audioFocusRequest?.let { audioManager?.requestAudioFocus(it) }
            } else {
                @Suppress("DEPRECATION")
                audioManager?.requestAudioFocus(
                    { focusChange -> Log.d(TAG, "Audio focus changed: $focusChange") },
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN
                )
            }
            Log.i(TAG, "Audio focus requested successfully.")
        } catch (e: Exception) {
            Log.w(TAG, "Audio focus request warning: ${e.message}")
        }
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

                val playbackState = PlaybackState.Builder()
                    .setState(PlaybackState.STATE_PLAYING, PlaybackState.PLAYBACK_POSITION_UNKNOWN, 1.0f)
                    .setActions(
                        PlaybackState.ACTION_PLAY or
                        PlaybackState.ACTION_PAUSE or
                        PlaybackState.ACTION_PLAY_PAUSE
                    )
                    .build()
                setPlaybackState(playbackState)

                isActive = true
            }

            requestAudioFocus()
            Log.i(TAG, "MediaSession VolumeProvider & PlaybackState successfully activated.")
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

        if (volumeDownFirstPressTime == 0L || (now - volumeDownLastPressTime > 400L)) {
            // New hold sequence initiated
            volumeDownFirstPressTime = now
            volumeDownLastPressTime = now
            volumeDownRepeatCount = 1
            Log.d(TAG, "Physical Volume Down hold sequence started (Target: ${threshold}ms).")
        } else {
            // Ongoing hold sequence
            volumeDownLastPressTime = now
            volumeDownRepeatCount++
            val elapsed = now - volumeDownFirstPressTime
            Log.d(TAG, "Physical Volume Down holding: elapsed=${elapsed}ms, repeats=$volumeDownRepeatCount")

            // Progressive haptic ticks while actively holding
            if (elapsed in 1000L..1250L && volumeDownRepeatCount in 3..5) {
                EmergencyTriggerCoordinator.vibrateHoldCountdownTick(this, 1)
            } else if (elapsed in 2000L..2250L && volumeDownRepeatCount in 6..8) {
                EmergencyTriggerCoordinator.vibrateHoldCountdownTick(this, 2)
            }

            // STRICT: Must sustain continuous hold >= 3000ms AND at least 6 repeat pulses
            if (elapsed >= threshold && volumeDownRepeatCount >= 6) {
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
        // After 400ms of silence, reset the accumulator so short presses do NOT trigger an alert.
        volumeResetJob?.cancel()
        volumeResetJob = serviceScope.launch {
            delay(400L)
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
            if (isVolumeReceiverRegistered) {
                unregisterReceiver(volumeChangeReceiver)
                isVolumeReceiverRegistered = false
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest?.let { audioManager?.abandonAudioFocusRequest(it) }
            } else {
                @Suppress("DEPRECATION")
                audioManager?.abandonAudioFocus(null)
            }
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
