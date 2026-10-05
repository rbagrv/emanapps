package com.example.business.data.model

enum class ChannelType {
    GENERAL,
    CASHBOX,
    WAREHOUSE,
    MANAGEMENT,
    DIRECT
}

enum class MessageType {
    TEXT,
    ALERT,
    SYSTEM_REPORT
}

data class ChatMessage(
    val id: String,
    val channelId: String,
    val senderId: String,
    val senderName: String,
    val senderRole: String, // Sahibkar, Baş Kassir, Anbardar, Mühasib
    val text: String,
    val timestamp: Long,
    val isMine: Boolean,
    val messageType: MessageType = MessageType.TEXT,
    val isDelivered: Boolean = true
)

data class ChatChannel(
    val id: String,
    val name: String,
    val description: String,
    val type: ChannelType,
    val unreadCount: Int = 0,
    val lastMessage: String,
    val lastMessageTime: String,
    val onlineParticipants: Int = 3
)
