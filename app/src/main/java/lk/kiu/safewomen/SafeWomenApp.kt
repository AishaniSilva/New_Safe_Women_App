package lk.kiu.safewomen

import android.app.Application
import lk.kiu.safewomen.data.local.AppDatabase
import lk.kiu.safewomen.data.repository.GuardianRepository
import lk.kiu.safewomen.services.SafeWomenForegroundService
import lk.kiu.safewomen.utils.PreferenceManager

class SafeWomenApp : Application() {

    val database by lazy { AppDatabase.getInstance(this) }
    val repository by lazy { GuardianRepository(database.guardianDao()) }
    val preferenceManager by lazy { PreferenceManager(this) }

    override fun onCreate() {
        super.onCreate()
        instance = this

        // If protection is enabled, start foreground service
        if (preferenceManager.isProtectionActive) {
            SafeWomenForegroundService.startService(this)
        }
    }

    companion object {
        lateinit var instance: SafeWomenApp
            private set
    }
}
