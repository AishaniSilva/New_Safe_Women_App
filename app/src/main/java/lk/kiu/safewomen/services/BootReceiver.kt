package lk.kiu.safewomen.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import lk.kiu.safewomen.utils.PreferenceManager

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            Log.d("BootReceiver", "Device boot completed. Checking protection status...")
            val prefs = PreferenceManager(context)
            if (prefs.isProtectionActive) {
                Log.i("BootReceiver", "Protection is enabled. Starting SafeWomenForegroundService...")
                SafeWomenForegroundService.startService(context)
            }
        }
    }
}
