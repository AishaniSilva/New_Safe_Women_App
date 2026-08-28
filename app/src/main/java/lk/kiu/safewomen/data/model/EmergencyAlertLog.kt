package lk.kiu.safewomen.data.model

data class EmergencyAlertLog(
    val timestamp: Long = System.currentTimeMillis(),
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
    val recipientsCount: Int,
    val messagePayload: String,
    val isSuccess: Boolean,
    val triggerType: String,
    val triggerLatencyMs: Long = 0L,
    val totalLatencyMs: Long = 0L
)
