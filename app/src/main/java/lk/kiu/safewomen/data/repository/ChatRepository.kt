package lk.kiu.safewomen.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import lk.kiu.safewomen.data.local.ChatMessageDao
import lk.kiu.safewomen.data.local.ChatMessageEntity
import lk.kiu.safewomen.data.model.ChatMessage

class ChatRepository(private val chatMessageDao: ChatMessageDao) {

    val allMessages: Flow<List<ChatMessage>> = chatMessageDao.getAllMessages().map { list ->
        list.map { it.toDomain() }
    }

    suspend fun sendMessage(message: ChatMessage): Long {
        return chatMessageDao.insertMessage(ChatMessageEntity.fromDomain(message))
    }

    suspend fun postSystemEmergencyBroadcast(latitude: Double, longitude: Double): Long {
        val mapsLink = "https://maps.google.com/?q=$latitude,$longitude"
        val alert = ChatMessage(
            senderName = "SAFE WOMEN SYSTEM",
            senderRole = "SYSTEM_ALERT",
            messageText = "🚨 EMERGENCY SOS ACTIVATED! Current live coordinates captured in transit.",
            timestamp = System.currentTimeMillis(),
            isEmergencyAlert = true,
            locationPin = mapsLink
        )
        return chatMessageDao.insertMessage(ChatMessageEntity.fromDomain(alert))
    }

    suspend fun clearHistory() {
        chatMessageDao.clearAllMessages()
    }
}
