package lk.kiu.safewomen.utils

object Constants {
    // Empirical survey calibrated parameters (N=101, 59.6% designated 3s)
    const val DEFAULT_TRIGGER_DURATION_MS: Long = 3000L
    const val MIN_TRIGGER_DURATION_MS: Long = 1000L
    const val MAX_TRIGGER_DURATION_MS: Long = 5000L

    // Notification Channel parameters
    const val NOTIFICATION_CHANNEL_ID = "safe_women_protection_channel"
    const val FOREGROUND_NOTIFICATION_ID = 8421 // Mapped to Student Index Number

    // Fallback Coordinates (Colombo Transit Hub)
    const val DEFAULT_FALLBACK_LATITUDE: Double = 6.9271
    const val DEFAULT_FALLBACK_LONGITUDE: Double = 79.8612

    // SMS Default Template (As specified in Proposal Section 9 and Interim Section 5.3)
    const val DEFAULT_SMS_TEMPLATE = "Emergency alert. I may be in danger. My current location is: %s. Please call 119 immediately and inform the police."

    // Emergency Hotlines
    const val POLICE_EMERGENCY_HOTLINE = "119"
    const val NTC_BUS_SAFETY_HOTLINE = "1933"

    // Broadcast Intent Actions
    const val ACTION_EMERGENCY_TRIGGERED = "lk.kiu.safewomen.ACTION_EMERGENCY_TRIGGERED"
    const val ACTION_SMS_DISPATCHED = "lk.kiu.safewomen.ACTION_SMS_DISPATCHED"
    const val EXTRA_LOG_MESSAGE = "extra_log_message"
    const val EXTRA_IS_SUCCESS = "extra_is_success"
}
