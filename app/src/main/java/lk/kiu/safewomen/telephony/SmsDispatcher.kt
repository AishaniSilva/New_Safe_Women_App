package lk.kiu.safewomen.telephony

import android.content.Context
import android.os.Build
import android.telephony.SmsManager
import android.util.Log
import lk.kiu.safewomen.data.model.Guardian
import lk.kiu.safewomen.utils.Constants
import lk.kiu.safewomen.utils.PreferenceManager

data class SmsDispatchResult(
    val isSuccess: Boolean,
    val sentCount: Int,
    val failedCount: Int,
    val formattedMessage: String,
    val mapsUrl: String,
    val errorMessage: String? = null
)

class SmsDispatcher(private val context: Context) {

    private val preferenceManager = PreferenceManager(context)

    fun dispatchEmergencyAlert(
        guardians: List<Guardian>,
        latitude: Double,
        longitude: Double
    ): SmsDispatchResult {
        if (guardians.isEmpty()) {
            Log.w("SmsDispatcher", "No guardians registered. SMS dispatch skipped.")
            return SmsDispatchResult(
                isSuccess = false,
                sentCount = 0,
                failedCount = 0,
                formattedMessage = "",
                mapsUrl = "",
                errorMessage = "No emergency guardians registered in directory."
            )
        }

        val mapsUrl = "https://maps.google.com/?q=$latitude,$longitude"
        val template = preferenceManager.customSmsTemplate
        
        // Assemble final payload string
        val messagePayload = if (template.contains("%s")) {
            String.format(template, mapsUrl)
        } else {
            "$template Location: $mapsUrl"
        }

        val smsManager: SmsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(SmsManager::class.java) ?: SmsManager.getDefault()
        } else {
            @Suppress("DEPRECATION")
            SmsManager.getDefault()
        }

        var successCount = 0
        var failCount = 0

        for (guardian in guardians) {
            try {
                val cleanNumber = guardian.phoneNumber.trim().replace(" ", "").replace("-", "")
                val messageParts = smsManager.divideMessage(messagePayload)
                
                if (messageParts.size > 1) {
                    smsManager.sendMultipartTextMessage(cleanNumber, null, messageParts, null, null)
                } else {
                    smsManager.sendTextMessage(cleanNumber, null, messagePayload, null, null)
                }
                successCount++
                Log.d("SmsDispatcher", "SMS successfully dispatched to guardian: ${guardian.name} ($cleanNumber)")
            } catch (e: Exception) {
                e.printStackTrace()
                failCount++
                Log.e("SmsDispatcher", "Failed to send SMS to guardian: ${guardian.name}, Error: ${e.message}")
            }
        }

        // Update last dispatched location and timestamp in preferences
        preferenceManager.lastDispatchedLocation = "$latitude, $longitude"
        preferenceManager.lastDispatchTimestamp = System.currentTimeMillis()

        return SmsDispatchResult(
            isSuccess = successCount > 0,
            sentCount = successCount,
            failedCount = failCount,
            formattedMessage = messagePayload,
            mapsUrl = mapsUrl,
            errorMessage = if (failCount > 0) "$failCount messages failed to dispatch." else null
        )
    }
}
