package com.example.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class SupabaseProfile(
    val id: String,
    @SerialName("user_id") val userId: String,
    val username: String,
    @SerialName("full_name") val fullName: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    val bio: String? = null,
    @SerialName("last_seen") val lastSeen: String? = null,
    @SerialName("is_online") val isOnline: Boolean = false,
    val interests: List<String>? = null,
    @SerialName("phone_number") val phoneNumber: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class SupabaseConversation(
    val id: String,
    val type: String, // 'direct', 'group', 'channel', 'saved'
    val name: String? = null,
    val description: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("created_by") val createdBy: String? = null,
    @SerialName("is_archived") val isArchived: Boolean = false,
    @SerialName("linked_discussion_id") val linkedDiscussionId: String? = null,
    @SerialName("subscriber_count") val subscriberCount: Int? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class SupabaseMessage(
    val id: String? = null,
    @SerialName("conversation_id") val conversationId: String,
    @SerialName("sender_id") val senderId: String,
    val content: String? = null,
    @SerialName("message_type") val messageType: String = "text",
    @SerialName("file_url") val fileUrl: String? = null,
    @SerialName("file_name") val fileName: String? = null,
    @SerialName("file_size") val fileSize: Long? = null,
    @SerialName("is_read") val isRead: Boolean = false,
    @SerialName("reply_to_channel_message_id") val replyToChannelMessageId: String? = null,
    @SerialName("view_count") val viewCount: Int? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class SupabaseConversationParticipant(
    val id: String? = null,
    @SerialName("conversation_id") val conversationId: String,
    @SerialName("user_id") val userId: String,
    val role: String = "member",
    @SerialName("joined_at") val joinedAt: String? = null
)

@Serializable
data class SupabaseCall(
    val id: String? = null,
    @SerialName("conversation_id") val conversationId: String,
    @SerialName("started_by") val startedBy: String,
    @SerialName("call_type") val callType: String = "video", // voice, video, livestream
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("is_recording") val isRecording: Boolean = false,
    @SerialName("recording_url") val recordingUrl: String? = null,
    @SerialName("recording_title") val recordingTitle: String? = null,
    @SerialName("recorded_by") val recordedBy: String? = null,
    @SerialName("livestream_title") val livestreamTitle: String? = null,
    @SerialName("started_at") val startedAt: String? = null,
    @SerialName("ended_at") val endedAt: String? = null
)

@Serializable
data class SupabaseCallParticipant(
    val id: String? = null,
    @SerialName("call_id") val callId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("joined_at") val joinedAt: String? = null,
    @SerialName("left_at") val leftAt: String? = null,
    @SerialName("is_muted") val isMuted: Boolean = false,
    @SerialName("is_video_off") val isVideoOff: Boolean = false,
    @SerialName("hand_raised") val handRaised: Boolean = false,
    @SerialName("noise_suppression") val noiseSuppression: Boolean = false
)

@Serializable
data class SupabaseCallSignal(
    val id: String? = null,
    @SerialName("call_id") val callId: String,
    @SerialName("from_user") val fromUser: String,
    @SerialName("to_user") val toUser: String? = null,
    @SerialName("signal_type") val signalType: String, // offer, answer, ice-candidate, ice-restart-request
    @SerialName("signal_data") val signalData: JsonElement
)

@Serializable
data class SupabaseInterestCategory(
    val id: String,
    val name: String,
    val emoji: String
)

@Serializable
data class ParticipantWithProfile(
    val participant: SupabaseConversationParticipant,
    val profile: SupabaseProfile? = null
)

@Serializable
data class ConversationWithDetails(
    val conversation: SupabaseConversation,
    val participants: List<ParticipantWithProfile>,
    val lastMessage: SupabaseMessage? = null,
    val isSavedMessages: Boolean = false,
    val isSelfChat: Boolean = false
)

