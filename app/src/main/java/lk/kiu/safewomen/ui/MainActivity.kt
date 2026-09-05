package lk.kiu.safewomen.ui

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.rememberNavController
import android.view.KeyEvent
import kotlinx.coroutines.*
import lk.kiu.safewomen.SafeWomenApp
import lk.kiu.safewomen.services.EmergencyTriggerCoordinator
import lk.kiu.safewomen.services.SafeWomenForegroundService
import lk.kiu.safewomen.ui.navigation.MainNavGraph
import lk.kiu.safewomen.ui.theme.SAFEWomenTheme
import lk.kiu.safewomen.ui.viewmodel.MainViewModel
import lk.kiu.safewomen.ui.viewmodel.MainViewModelFactory
import lk.kiu.safewomen.utils.Constants

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: MainViewModel
    private var volumeDownStartTime = 0L
    private var isHoldingVolumeDown = false
    private var inAppTriggerJob: Job? = null
    private val mainScope = CoroutineScope(Dispatchers.Main)

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val smsGranted = permissions[Manifest.permission.SEND_SMS] ?: false
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        if (fineLocationGranted || coarseLocationGranted) {
            viewModel.refreshLocation()
        }

        val app = application as SafeWomenApp
        if (app.preferenceManager.isProtectionActive) {
            SafeWomenForegroundService.startService(this)
        }

        if (smsGranted && (fineLocationGranted || coarseLocationGranted)) {
            Toast.makeText(this, "All critical safety permissions granted.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "SMS & Precise Location permissions are required for accurate emergency dispatch.", Toast.LENGTH_LONG).show()
        }
    }

    private val emergencyBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Constants.ACTION_SMS_DISPATCHED) {
                val message = intent.getStringExtra(Constants.EXTRA_LOG_MESSAGE) ?: "Alert Dispatched"
                val isSuccess = intent.getBooleanExtra(Constants.EXTRA_IS_SUCCESS, true)
                Toast.makeText(
                    this@MainActivity,
                    if (isSuccess) "EMERGENCY ALERT SENT: $message" else "FAILED: $message",
                    Toast.LENGTH_LONG
                ).show()
                viewModel.refreshLocation()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as SafeWomenApp
        val factory = MainViewModelFactory(app, app.repository, app.preferenceManager)
        viewModel = ViewModelProvider(this, factory)[MainViewModel::class.java]

        requestCriticalPermissions()

        val filter = IntentFilter(Constants.ACTION_SMS_DISPATCHED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(emergencyBroadcastReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(emergencyBroadcastReceiver, filter)
        }

        setContent {
            SAFEWomenTheme {
                val navController = rememberNavController()
                MainNavGraph(
                    navController = navController,
                    viewModel = viewModel
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkLocationHardware()
        if (::viewModel.isInitialized) {
            viewModel.refreshLocation()
            viewModel.refreshAccessibilityStatus()
        }
        val app = application as SafeWomenApp
        if (app.preferenceManager.isProtectionActive) {
            SafeWomenForegroundService.startService(this)
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            val app = application as SafeWomenApp
            val threshold = app.preferenceManager.triggerDurationMs

            when (event.action) {
                KeyEvent.ACTION_DOWN -> {
                    if (!isHoldingVolumeDown) {
                        isHoldingVolumeDown = true
                        volumeDownStartTime = System.currentTimeMillis()
                        EmergencyTriggerCoordinator.vibrateShortTick(this)
                        Toast.makeText(this, "⚠️ Holding Volume Down for ${(threshold / 1000)}s to trigger SOS...", Toast.LENGTH_SHORT).show()

                        inAppTriggerJob?.cancel()
                        inAppTriggerJob = mainScope.launch {
                            delay(1000L)
                            if (isHoldingVolumeDown) {
                                EmergencyTriggerCoordinator.vibrateHoldCountdownTick(this@MainActivity, 1)
                            }
                            delay(1000L)
                            if (isHoldingVolumeDown) {
                                EmergencyTriggerCoordinator.vibrateHoldCountdownTick(this@MainActivity, 2)
                            }
                            delay(1000L)
                            if (isHoldingVolumeDown) {
                                isHoldingVolumeDown = false
                                Toast.makeText(this@MainActivity, "🚨 Emergency Triggered! Sending SMS...", Toast.LENGTH_LONG).show()
                                EmergencyTriggerCoordinator.triggerEmergency(
                                    context = this@MainActivity,
                                    triggerSource = "PHYSICAL_VOLUME_DOWN_IN_APP_FOREGROUND",
                                    durationMs = threshold
                                )
                            }
                        }
                    }
                    return true
                }
                KeyEvent.ACTION_UP -> {
                    val held = System.currentTimeMillis() - volumeDownStartTime
                    isHoldingVolumeDown = false
                    inAppTriggerJob?.cancel()
                    inAppTriggerJob = null
                    return held >= threshold
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    private fun checkLocationHardware() {
        val lm = getSystemService(Context.LOCATION_SERVICE) as? android.location.LocationManager ?: return
        val isEnabled = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            lm.isLocationEnabled
        } else {
            lm.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) ||
            lm.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)
        }
        if (!isEnabled) {
            Toast.makeText(
                this,
                "⚠️ Device GPS is OFF! Please enable Location in Settings for 100% accurate coordinates.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(emergencyBroadcastReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun requestCriticalPermissions() {
        val permissionsToRequest = mutableListOf(
            Manifest.permission.SEND_SMS,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.CALL_PHONE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val ungranted = permissionsToRequest.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (ungranted.isNotEmpty()) {
            permissionLauncher.launch(ungranted.toTypedArray())
        }
    }
}
