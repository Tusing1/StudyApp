package com.example.repository

import android.util.Log
import com.example.BuildConfig
import com.example.api.Content
import com.example.api.GenerateContentRequest
import com.example.api.Part
import com.example.api.RetrofitClient
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

class StudyRepository(
    private val db: AppDatabase
) {
    private val userProfileDao = db.userProfileDao()
    private val messageDao = db.discussionMessageDao()
    private val quizDao = db.quizQuestionDao()
    private val bookmarkDao = db.bookmarkDao()

    val userProfile: Flow<UserProfile?> = userProfileDao.getUserProfile()
    val allBookmarks: Flow<List<Bookmark>> = bookmarkDao.getAllBookmarks()

    suspend fun saveProfile(username: String, field: String, color: Int) = withContext(Dispatchers.IO) {
        val user = UserProfile(username = username, nursingField = field, avatarColor = color)
        userProfileDao.insertProfile(user)
    }

    suspend fun clearProfile() = withContext(Dispatchers.IO) {
        userProfileDao.deleteProfile()
    }

    suspend fun initDatabaseIfNeeded() = withContext(Dispatchers.IO) {
        // Initializing quiz questions if database is empty
        val existingQuestions = quizDao.getAllQuestions().firstOrNull() ?: emptyList()
        if (existingQuestions.isEmpty()) {
            quizDao.insertQuestions(DefaultData.PRELOAD_QUIZ_QUESTIONS)
            
            // Pre-seed some discussion messages for key channels if empty
            for (channel in DefaultData.CHANNELS) {
                val existingMsgList = messageDao.getMessagesForChannel(channel.id).firstOrNull() ?: emptyList()
                if (existingMsgList.isEmpty()) {
                    val defaultMsgs = DefaultData.getPreloadMessages(channel.id)
                    for (msg in defaultMsgs) {
                        messageDao.insertMessage(msg)
                    }
                }
            }
        }
    }

    fun getMessages(channelId: String): Flow<List<DiscussionMessage>> {
        return messageDao.getMessagesForChannel(channelId)
    }

    suspend fun sendMessage(channelId: String, text: String, senderName: String, senderField: String, avatarColor: Int) = withContext(Dispatchers.IO) {
        val message = DiscussionMessage(
            channelId = channelId,
            senderName = senderName,
            senderField = senderField,
            avatarColor = avatarColor,
            text = text,
            isAiGenerated = false
        )
        messageDao.insertMessage(message)
    }

    suspend fun insertMessageDirect(msg: DiscussionMessage) = withContext(Dispatchers.IO) {
        messageDao.insertMessage(msg)
    }

    suspend fun updateMessageBookmarkStatus(id: Int, isBookmarked: Boolean) = withContext(Dispatchers.IO) {
        messageDao.updateMessageBookmarkStatus(id, isBookmarked)
    }

    fun getQuizQuestions(subject: String): Flow<List<QuizQuestion>> {
        return if (subject.equals("All", ignoreCase = true)) {
            quizDao.getAllQuestions()
        } else {
            quizDao.getQuestionsForSubject(subject)
        }
    }

    val allQuizQuestions: Flow<List<QuizQuestion>> = quizDao.getAllQuestions()

    suspend fun addBookmark(type: String, title: String, description: String, refId: String) = withContext(Dispatchers.IO) {
        val bookmark = Bookmark(type = type, title = title, description = description, referenceId = refId)
        bookmarkDao.insertBookmark(bookmark)
    }

    suspend fun removeBookmark(refId: String, type: String) = withContext(Dispatchers.IO) {
        bookmarkDao.deleteBookmarkByRef(refId, type)
    }

    fun isBookmarked(refId: String, type: String): Flow<Boolean> {
        return bookmarkDao.isBookmarked(refId, type)
    }

    suspend fun askGemini(prompt: String, systemPrompt: String? = null): String = withContext(Dispatchers.IO) {
        // Safe check for the Gemini API key
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY" || apiKey.contains("placeholder", ignoreCase = true)) {
            return@withContext "API Configuration Notice: Gemini API Key is missing. To activate real-time clinical grading and AI tutor explanations, please add your Google Gemini API Key into the Secrets panel in the AI Studio sidebar."
        }

        try {
            val systemContent = systemPrompt?.let {
                Content(parts = listOf(Part(text = it)))
            }
            val request = GenerateContentRequest(
                contents = listOf(Content(parts = listOf(Part(text = prompt)))),
                systemInstruction = systemContent
            )
            val model = "gemini-3.5-flash"
            val response = RetrofitClient.service.generateContent(model, apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text 
                ?: "No explanation generated. Try asking again."
        } catch (e: Exception) {
            Log.e("StudyRepository", "Gemini API error", e)
            "AI Assistant is currently offline. Error details: ${e.localizedMessage}. Verify your internet connection and API key configuration."
        }
    }
}
