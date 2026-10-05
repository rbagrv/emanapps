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
    SYSTEM_REPORT,
    TASK
}

enum class TaskPriority(val label: String) {
    NORMAL("Normal"),
    URGENT("Təcili")
}

enum class TaskStatus(val label: String) {
    PENDING("Gözləyir"),
    IN_PROGRESS("İcrada"),
    COMPLETED("Tamamlandı")
}

data class ChatTask(
    val id: String,
    val title: String,
    val assignedTo: String, // e.g. "@Elmir Qasımov (Kassir)"
    val dueDate: String,
    val priority: TaskPriority = TaskPriority.NORMAL,
    val status: TaskStatus = TaskStatus.PENDING
)

data class TeamEmployee(
    val id: String,
    val name: String,
    val role: String,
    val phone: String
)

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
    val isDelivered: Boolean = true,
    val task: ChatTask? = null,
    val taggedEmployee: String? = null
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
