package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: String = "local_user",
    val username: String,
    val nursingField: String, // e.g., "General Nursing", "Midwifery", "Pediatrics", "Education"
    val avatarColor: Int, // Hex value of user avatar color
    val registeredAt: Long = System.currentTimeMillis(),
    val studyTokens: Int = 0
)

@Entity(tableName = "discussion_messages")
data class DiscussionMessage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val channelId: String,
    val senderName: String,
    val senderField: String,
    val avatarColor: Int,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isAiGenerated: Boolean = false,
    val isBookmarked: Boolean = false
)

@Entity(tableName = "quiz_questions")
data class QuizQuestion(
    @PrimaryKey val id: Int,
    val subject: String, // e.g., "Pharmacology", "Midwifery", "Anatomy"
    val questionText: String,
    val optionA: String,
    val optionB: String,
    val optionC: String,
    val optionD: String,
    val correctOption: String, // "A", "B", "C", "D"
    val explanation: String
)

@Entity(tableName = "bookmarks")
data class Bookmark(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: String, // "DISCUSSION" or "QUIZ"
    val title: String,
    val description: String,
    val referenceId: String, // messageId or questionId
    val timestamp: Long = System.currentTimeMillis()
)

data class DiscussionChannel(
    val id: Long,
    val title: String,
    val category: String,
    val description: String,
    val iconName: String,
    val onlineCount: Int
)
