package lk.kiu.safewomen.utils

import android.content.Context
import android.content.SharedPreferences
import java.security.MessageDigest

class AuthManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    var isSessionUnlocked: Boolean = false

    val isOnboardingCompleted: Boolean
        get() = prefs.getBoolean(KEY_IS_ONBOARDED, false)

    val registeredPhoneNumber: String
        get() = prefs.getString(KEY_PHONE_NUMBER, "") ?: ""

    fun registerUser(phoneNumber: String, password: String, pin: String): Boolean {
        if (phoneNumber.isBlank() || password.length < 4 || pin.length != 4) {
            return false
        }
        val passHash = hashSha256(password)
        val pinHash = hashSha256(pin)

        prefs.edit()
            .putString(KEY_PHONE_NUMBER, phoneNumber)
            .putString(KEY_PASSWORD_HASH, passHash)
            .putString(KEY_PIN_HASH, pinHash)
            .putBoolean(KEY_IS_ONBOARDED, true)
            .apply()

        isSessionUnlocked = true
        return true
    }

    fun validatePin(pin: String): Boolean {
        val storedHash = prefs.getString(KEY_PIN_HASH, "") ?: ""
        val inputHash = hashSha256(pin)
        val isValid = storedHash.isNotEmpty() && storedHash == inputHash
        if (isValid) {
            isSessionUnlocked = true
        }
        return isValid
    }

    fun validatePassword(password: String): Boolean {
        val storedHash = prefs.getString(KEY_PASSWORD_HASH, "") ?: ""
        val inputHash = hashSha256(password)
        val isValid = storedHash.isNotEmpty() && storedHash == inputHash
        if (isValid) {
            isSessionUnlocked = true
        }
        return isValid
    }

    fun resetPin(newPin: String): Boolean {
        if (newPin.length != 4) return false
        val pinHash = hashSha256(newPin)
        prefs.edit().putString(KEY_PIN_HASH, pinHash).apply()
        return true
    }

    fun lockSession() {
        isSessionUnlocked = false
    }

    private fun hashSha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val PREF_NAME = "safe_women_auth_prefs"
        private const val KEY_IS_ONBOARDED = "is_onboarded"
        private const val KEY_PHONE_NUMBER = "phone_number"
        private const val KEY_PASSWORD_HASH = "password_hash"
        private const val KEY_PIN_HASH = "pin_hash"
    }
}
