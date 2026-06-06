package com.example.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SupabaseUser(
    val id: String,
    val username: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("phone_number") val phoneNumber: String? = null,
    val email: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    val bio: String? = null,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("last_seen") val lastSeen: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class SupabaseConversation(
    val id: Long,
    val type: String, // 'direct', 'group', 'channel', 'secret'
    val title: String? = null,
    val description: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("created_by") val createdBy: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class SupabaseMessage(
    val id: Long? = null,
    @SerialName("conversation_id") val conversationId: Long,
    @SerialName("sender_id") val senderId: String,
    val content: String,
    @SerialName("message_type") val messageType: String = "text",
    @SerialName("file_url") val fileUrl: String? = null,
    @SerialName("reply_to_message_id") val replyToMessageId: Long? = null,
    @SerialName("is_edited") val isEdited: Boolean = false,
    @SerialName("is_deleted") val isDeleted: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class SupabaseConversationMember(
    val id: Long,
    @SerialName("conversation_id") val conversationId: Long,
    @SerialName("user_id") val userId: String,
    val role: String = "member",
    @SerialName("joined_at") val joinedAt: String? = null,
    @SerialName("is_muted") val isMuted: Boolean = false,
    @SerialName("last_read_message_id") val lastReadMessageId: Long? = null
)

@Serializable
data class SupabaseCall(
    val id: String,
    @SerialName("caller_id") val callerId: String,
    @SerialName("receiver_id") val receiverId: String,
    val status: String = "ringing", // ringing, ongoing, ended, rejected
    @SerialName("started_at") val startedAt: String? = null,
    @SerialName("ended_at") val endedAt: String? = null
)

@Serializable
data class SupabaseCallSignal(
    val id: Long? = null,
    @SerialName("call_id") val callId: String,
    @SerialName("sender_id") val senderId: String,
    @SerialName("receiver_id") val receiverId: String,
    val type: String, // offer, answer, ice-candidate
    val payload: String // Stringified JSON of SDP or ICE
)
