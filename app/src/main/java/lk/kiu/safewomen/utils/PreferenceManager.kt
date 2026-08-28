package lk.kiu.safewomen.utils

import android.content.Context
import android.content.SharedPreferences
import lk.kiu.safewomen.data.model.TriggerMode

class PreferenceManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    var isProtectionActive: Boolean
        get() = prefs.getBoolean(KEY_PROTECTION_ACTIVE, true)
        set(value) = prefs.edit().putBoolean(KEY_PROTECTION_ACTIVE, value).apply()

    var triggerDurationMs: Long
        get() = prefs.getLong(KEY_TRIGGER_DURATION_MS, Constants.DEFAULT_TRIGGER_DURATION_MS)
        set(value) = prefs.edit().putLong(KEY_TRIGGER_DURATION_MS, value).apply()

    var triggerMode: TriggerMode
        get() {
            val modeName = prefs.getString(KEY_TRIGGER_MODE, TriggerMode.VOLUME_BUTTON_ONLY.name)
            return try {
                TriggerMode.valueOf(modeName ?: TriggerMode.VOLUME_BUTTON_ONLY.name)
            } catch (e: Exception) {
                TriggerMode.VOLUME_BUTTON_ONLY
            }
        }
        set(value) = prefs.edit().putString(KEY_TRIGGER_MODE, value.name).apply()

    var customSmsTemplate: String
        get() = prefs.getString(KEY_SMS_TEMPLATE, Constants.DEFAULT_SMS_TEMPLATE) ?: Constants.DEFAULT_SMS_TEMPLATE
        set(value) = prefs.edit().putString(KEY_SMS_TEMPLATE, value).apply()

    var isHapticFeedbackEnabled: Boolean
        get() = prefs.getBoolean(KEY_HAPTIC_FEEDBACK, true)
        set(value) = prefs.edit().putBoolean(KEY_HAPTIC_FEEDBACK, value).apply()

    var lastDispatchedLocation: String
        get() = prefs.getString(KEY_LAST_LOCATION, "6.9271, 79.8612") ?: "6.9271, 79.8612"
        set(value) = prefs.edit().putString(KEY_LAST_LOCATION, value).apply()

    var lastDispatchTimestamp: Long
        get() = prefs.getLong(KEY_LAST_DISPATCH_TIME, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_DISPATCH_TIME, value).apply()

    companion object {
        private const val PREF_NAME = "safe_women_settings"
        private const val KEY_PROTECTION_ACTIVE = "key_protection_active"
        private const val KEY_TRIGGER_DURATION_MS = "key_trigger_duration_ms"
        private const val KEY_TRIGGER_MODE = "key_trigger_mode"
        private const val KEY_SMS_TEMPLATE = "key_sms_template"
        private const val KEY_HAPTIC_FEEDBACK = "key_haptic_feedback"
        private const val KEY_LAST_LOCATION = "key_last_location"
        private const val KEY_LAST_DISPATCH_TIME = "key_last_dispatch_time"
    }
}
