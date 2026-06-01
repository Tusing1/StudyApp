package com.example.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.repository.StudyRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface AppScreen {
    object Welcome : AppScreen
    object Channels : AppScreen
    data class DiscussionRoom(val channel: DiscussionChannel) : AppScreen
    object QuizPractice : AppScreen
    object AIChat : AppScreen
    object Bookmarks : AppScreen
}

data class ChatMessage(
    val senderName: String,
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

class StudygramViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = StudyRepository(database)

    // Current app screen routing state
    var currentScreen by mutableStateOf<AppScreen>(AppScreen.Welcome)
        private set

    // Observe active profile and bookmarks
    val userProfile: StateFlow<UserProfile?> = repository.userProfile
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val bookmarks: StateFlow<List<Bookmark>> = repository.allBookmarks
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Chat and channel messages variables
    val activeChannelMessages = mutableStateListOf<DiscussionMessage>()
    private var activeChannelId: String? = null

    // Practice Quiz states
    var activeQuizSubject by mutableStateOf("All")
    val quizQuestions = mutableStateListOf<QuizQuestion>()
    var currentQuestionIndex by mutableIntStateOf(0)
    var selectedOption by mutableStateOf<String?>(null)
    var hasAnswered by mutableStateOf(false)
    var isCorrectAnswer by mutableStateOf(false)
    var quizExplanation by mutableStateOf("")

    // AI Tutoring & Companion States
    var aiExplanationText by mutableStateOf("")
    var isGeneratingAI by mutableStateOf(false)

    // Dedicated Play Chat Messages list
    val aiPlaygroundMessages = mutableStateListOf<ChatMessage>()

    init {
        viewModelScope.launch {
            // Seed database configurations on start
            repository.initDatabaseIfNeeded()
            
            // Check if profile exists, route to channels if registered
            userProfile.collect { profile ->
                if (profile != null && currentScreen == AppScreen.Welcome) {
                    currentScreen = AppScreen.Channels
                }
            }
        }
    }

    // Navigation helper
    fun navigateTo(screen: AppScreen) {
        currentScreen = screen
        // If navigating to discussion room, load messages
        if (screen is AppScreen.DiscussionRoom) {
            loadMessagesForChannel(screen.channel.id)
        } else if (screen is AppScreen.QuizPractice) {
            loadQuizQuestions(activeQuizSubject)
        }
    }

    // Set user profile
    fun registerUser(handle: String, specialty: String) {
        viewModelScope.launch {
            val colors = listOf(0xFF00796B, 0xFFC2185B, 0xFF7B1FA2, 0xFF1976D2, 0xFF388E3C)
            val randomColor = colors.random().toInt()
            repository.saveProfile(handle, specialty, randomColor)
            currentScreen = AppScreen.Channels
        }
    }

    // Log-out or change profile details
    fun logout() {
        viewModelScope.launch {
            repository.clearProfile()
            currentScreen = AppScreen.Welcome
        }
    }

    // Load messages dynamically and collect reactive updates
    private fun loadMessagesForChannel(channelId: String) {
        activeChannelId = channelId
        viewModelScope.launch {
            repository.getMessages(channelId).collect { msgs ->
                // Guard against race conditions if user left the channel already
                if (activeChannelId == channelId) {
                    activeChannelMessages.clear()
                    activeChannelMessages.addAll(msgs)
                }
            }
        }
    }

    // Post comment/post in active discussion channels
    fun sendDiscussionMessage(text: String) {
        val channelId = activeChannelId ?: return
        val profile = userProfile.value ?: return

        if (text.isBlank()) return

        viewModelScope.launch {
            repository.sendMessage(
                channelId = channelId,
                text = text,
                senderName = profile.username,
                senderField = profile.nursingField,
                avatarColor = profile.avatarColor
            )

            // Dynamic Peer/Model simulation for rich demonstration!
            simulateInteractiveDiscussion(channelId, text)
        }
    }

    // Simulates natural chat and study group replies
    private var simulationJob: kotlinx.coroutines.Job? = null
    private fun simulateInteractiveDiscussion(channelId: String, userText: String) {
        simulationJob?.cancel()
        simulationJob = viewModelScope.launch {
            kotlinx.coroutines.delay(1800) // Delay to simulate typical typing response lag
            
            val responseText: String
            val senderName: String
            val senderRole: String
            val senderColor: Int

            if (userText.contains("AMTSL", ignoreCase = true) || userText.contains("labor", ignoreCase = true)) {
                senderName = "Namuli Catherine"
                senderRole = "Qualified Registered Midwife"
                senderColor = 0xFFC2185B.toInt()
                responseText = "Excellent point on Oxytocin! Let's remember that if oxytocin is unavailable, misoprostol 600 mcg orally is the recommended alternative according to Ministry of Health guidelines."
            } else if (userText.contains("drip", ignoreCase = true) || userText.contains("calculation", ignoreCase = true) || userText.contains("drops", ignoreCase = true)) {
                senderName = "Mugisha Paul"
                senderRole = "Senior Surgical Nurse"
                senderColor = 0xFF1976D2.toInt()
                responseText = "Yes, and always read the infusion label carefully! A standard pediatric microdrip set is 60 drops/mL, while standard adult blood transfusion sets are typically 15 drops/mL."
            } else {
                senderName = "Kiiza Simon"
                senderRole = "UNMC Revision Mentor"
                senderColor = 0xFFE64A19.toInt()
                responseText = "Spot on discussion! Let me add that writing down nursing action notes and memorizing rationale for clinical indices is vital for the upcoming council papers."
            }

            val simMessage = DiscussionMessage(
                channelId = channelId,
                senderName = senderName,
                senderField = senderRole,
                avatarColor = senderColor,
                text = responseText,
                isAiGenerated = false
            )
            repository.insertMessageDirect(simMessage)
        }
    }

    // Request AI interpretation on any specific post
    fun askAiAboutPost(msg: DiscussionMessage) {
        isGeneratingAI = true
        aiExplanationText = "Evaluating discussion context..."
        viewModelScope.launch {
            val systemInstructions = "You are a friendly and precise Nursing Mentor for the Ugandan study group 'Nurses Revision Uganda'. Clarify or validate the nursing discussion post. Give clinical accuracy points in brief."
            val userPrompt = "Provide an expert clinical explanation, verify the correctness, and summarize in high-yield points for this student discussion post:\n\"${msg.senderName} (${msg.senderField}): ${msg.text}\""
            
            val explanation = repository.askGemini(userPrompt, systemInstructions)
            aiExplanationText = explanation
            isGeneratingAI = false
        }
    }

    // Load Quiz questions
    fun loadQuizQuestions(subject: String) {
        activeQuizSubject = subject
        viewModelScope.launch {
            repository.getQuizQuestions(subject).collect { questions ->
                quizQuestions.clear()
                quizQuestions.addAll(questions)
                // Reset index and score tracking keys
                currentQuestionIndex = 0
                resetQuestionState()
            }
        }
    }

    private fun resetQuestionState() {
        selectedOption = null
        hasAnswered = false
        isCorrectAnswer = false
        quizExplanation = ""
    }

    // Answer selection inside MCQs
    fun selectQuizOption(option: String) {
        if (hasAnswered) return
        selectedOption = option
    }

    fun submitQuizAnswer() {
        val currentQuestion = quizQuestions.getOrNull(currentQuestionIndex) ?: return
        if (selectedOption == null || hasAnswered) return

        hasAnswered = true
        isCorrectAnswer = selectedOption == currentQuestion.correctOption
        quizExplanation = currentQuestion.explanation
    }

    fun nextQuizQuestion() {
        if (currentQuestionIndex + 1 < quizQuestions.size) {
            currentQuestionIndex++
            resetQuestionState()
        } else {
            // Circle back or stay on summary
            resetQuestionState()
            currentQuestionIndex = 0
        }
    }

    // Request direct Gemini explanations for the active Quiz question
    fun askAiToExplainQuiz() {
        val currentQuestion = quizQuestions.getOrNull(currentQuestionIndex) ?: return
        isGeneratingAI = true
        aiExplanationText = "Consulting AI clinical database..."
        viewModelScope.launch {
            val systemInstructions = "You are an expert Clinical MCQ Tutor for Ugandan licensing exams conducted by the UNMC (Uganda Nurses and Midwives Council). Give step-by-step reasoning."
            val userPrompt = """
                Question: ${currentQuestion.questionText}
                Options:
                A) ${currentQuestion.optionA}
                B) ${currentQuestion.optionB}
                C) ${currentQuestion.optionC}
                D) ${currentQuestion.optionD}
                
                The correct option is: ${currentQuestion.correctOption}.
                
                Explain in detail:
                1. Why option ${currentQuestion.correctOption} is correct.
                2. Briefly why other choices are incorrect.
                3. What high-yield clinical takeaway should a Ugandan student write down for their UNMC board exam?
            """.trimIndent()
            
            val explanation = repository.askGemini(userPrompt, systemInstructions)
            aiExplanationText = explanation
            isGeneratingAI = false
        }
    }

    // Bookmark management
    fun toggleMessageBookmark(msg: DiscussionMessage) {
        viewModelScope.launch {
            val isCurrentlyBookmarked = bookmarks.value.any { it.referenceId == msg.id.toString() && it.type == "DISCUSSION" }
            if (isCurrentlyBookmarked) {
                repository.removeBookmark(msg.id.toString(), "DISCUSSION")
                repository.updateMessageBookmarkStatus(msg.id, false)
            } else {
                repository.addBookmark(
                    type = "DISCUSSION",
                    title = "Group Thread: ${msg.senderName}",
                    description = if (msg.text.length > 50) msg.text.take(50) + "..." else msg.text,
                    refId = msg.id.toString()
                )
                repository.updateMessageBookmarkStatus(msg.id, true)
            }
        }
    }

    fun toggleQuizBookmark(question: QuizQuestion) {
        viewModelScope.launch {
            val isCurrentlyBookmarked = bookmarks.value.any { it.referenceId == question.id.toString() && it.type == "QUIZ" }
            if (isCurrentlyBookmarked) {
                repository.removeBookmark(question.id.toString(), "QUIZ")
            } else {
                repository.addBookmark(
                    type = "QUIZ",
                    title = "Quiz Question: ${question.subject}",
                    description = if (question.questionText.length > 50) question.questionText.take(50) + "..." else question.questionText,
                    refId = question.id.toString()
                )
            }
        }
    }

    fun isQuizBookmarked(questionId: Int): Boolean {
        return bookmarks.value.any { it.referenceId == questionId.toString() && it.type == "QUIZ" }
    }

    fun deleteBookmarkDirect(bookmark: Bookmark) {
        viewModelScope.launch {
            repository.removeBookmark(bookmark.referenceId, bookmark.type)
            if (bookmark.type == "DISCUSSION") {
                try {
                    repository.updateMessageBookmarkStatus(bookmark.referenceId.toInt(), false)
                } catch (e: Exception) {
                    // Overlook if message ID is non-numeric or missing
                }
            }
        }
    }

    // AI Chat Buddy actions
    fun sendAiChatMessage(userMessageText: String) {
        if (userMessageText.isBlank()) return
        
        val userMsg = ChatMessage(senderName = "Me", text = userMessageText, isUser = true)
        aiPlaygroundMessages.add(userMsg)
        
        isGeneratingAI = true
        
        viewModelScope.launch {
            val systemInstructions = "You are 'NR Care Buddy', a friendly, professional AI Clinical Tutor for Nurses Revision Uganda. You explain pediatric plans, drug calculations, and midwifery priorities based on WHO and Uganda Min of Health guidelines."
            
            // Build conversation context from previous logs
            val contextPrompt = aiPlaygroundMessages.takeLast(6).joinToString("\n") { 
                "${if (it.isUser) "Student" else "Tutor"}: ${it.text}"
            } + "\nTutor:"

            val aiResponse = repository.askGemini(contextPrompt, systemInstructions)
            
            aiPlaygroundMessages.add(
                ChatMessage(senderName = "AI Companion", text = aiResponse, isUser = false)
            )
            isGeneratingAI = false
        }
    }

    fun clearAiChat() {
        aiPlaygroundMessages.clear()
        aiPlaygroundMessages.add(
            ChatMessage(
                senderName = "AI Tutor",
                text = "Hello! I am your AI clinical study guide. Ask me any pharmacology calculations, pediatric immunization schedules, or midwifery standards!",
                isUser = false
            )
        )
    }
}
