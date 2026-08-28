package lk.kiu.safewomen.data.model

data class ChatMessage(
    val id: Long = 0,
    val senderName: String,
    val senderRole: String, // "PASSENGER", "GUARDIAN", "SYSTEM_ALERT"
    val messageText: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isEmergencyAlert: Boolean = false,
    val locationPin: String? = null
)
