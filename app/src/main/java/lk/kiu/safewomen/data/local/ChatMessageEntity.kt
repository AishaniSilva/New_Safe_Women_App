package lk.kiu.safewomen.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import lk.kiu.safewomen.data.model.ChatMessage

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val senderName: String,
    val senderRole: String,
    val messageText: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isEmergencyAlert: Boolean = false,
    val locationPin: String? = null
) {
    fun toDomain(): ChatMessage = ChatMessage(
        id = id,
        senderName = senderName,
        senderRole = senderRole,
        messageText = messageText,
        timestamp = timestamp,
        isEmergencyAlert = isEmergencyAlert,
        locationPin = locationPin
    )

    companion object {
        fun fromDomain(domain: ChatMessage): ChatMessageEntity = ChatMessageEntity(
            id = domain.id,
            senderName = domain.senderName,
            senderRole = domain.senderRole,
            messageText = domain.messageText,
            timestamp = domain.timestamp,
            isEmergencyAlert = domain.isEmergencyAlert,
            locationPin = domain.locationPin
        )
    }
}
