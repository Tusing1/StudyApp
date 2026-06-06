package com.example.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.SupabaseApi
import com.example.data.SupabaseConversation
import com.example.data.SupabaseMessage
import com.example.data.SupabaseUser
import com.example.data.CallManager
import com.example.repository.StudyRepository
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface AppScreen {
    object Welcome : AppScreen
    object Login : AppScreen
    object SignUp : AppScreen
    object Channels : AppScreen // "Chats"
    data class DiscussionRoom(val channel: SupabaseConversation) : AppScreen
    data class ChannelInfo(val channel: SupabaseConversation) : AppScreen
    object Calls : AppScreen
    object Contacts : AppScreen
    object Settings : AppScreen
    object Flashcards : AppScreen // Gamified Revision
    object QuizPractice : AppScreen
    data class Thread(val messageId: String) : AppScreen
    data class AudioPlayer(val url: String) : AppScreen
    data class PdfViewer(val url: String) : AppScreen
    object AIChat : AppScreen
    object Bookmarks : AppScreen
}

data class ChatMessage(
    val senderName: String,
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

class StudygramViewModel : ViewModel() {
    private val database = DatabaseProvider.getDatabase()
    private val repository = StudyRepository(database)
    
    val callManager = CallManager(viewModelScope)

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
    val activeChannelMessages = mutableStateListOf<SupabaseMessage>()
    private var activeChannelId: Long? = null
    val channels = mutableStateListOf<SupabaseConversation>()

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
            repository.initDatabaseIfNeeded()
            checkAuthStatus()
            fetchChannels()
            listenForIncomingCalls()
        }
    }

    private fun listenForIncomingCalls() {
        viewModelScope.launch {
            val session = SupabaseApi.client.auth.currentSessionOrNull()
            val myUid = session?.user?.id ?: return@launch

            SupabaseApi.client.realtime.channel("public:calls_listener")
                .postgresChangeFlow<io.github.jan.supabase.realtime.PostgresAction.Insert>(schema = "public") {
                    table = "calls"
                    filter = "receiver_id=eq.$myUid"
                }
                .onEach { action ->
                    val call = action.decodeRecord<com.example.data.SupabaseCall>()
                    if (call.status == "ringing") {
                        // User has an incoming call!
                        // For demonstration, immediately navigate to the Calls UI.
                        navigateTo(AppScreen.Calls)
                    }
                }
                .launchIn(this)
        }
    }

    private fun fetchChannels() {
        viewModelScope.launch {
            try {
                val dbChannels = SupabaseApi.client.postgrest["conversations"]
                    .select()
                    .decodeList<SupabaseConversation>()
                channels.clear()
                channels.addAll(dbChannels)
            } catch (e: Exception) {
                // Keep UI going even if DB empty
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
    private fun loadMessagesForChannel(channelId: Long) {
        activeChannelId = channelId
        viewModelScope.launch {
            try {
                // 1. Initial Fetch
                val msgs = SupabaseApi.client.postgrest["messages"]
                    .select { filter { eq("conversation_id", channelId) } }
                    .decodeList<SupabaseMessage>()
                
                if (activeChannelId == channelId) {
                    activeChannelMessages.clear()
                    activeChannelMessages.addAll(msgs)
                }

                // 2. Realtime Subscriptions
                val channel = SupabaseApi.client.realtime.channel("public:messages:$channelId")
                channel.postgresChangeFlow<io.github.jan.supabase.realtime.PostgresAction.Insert>(schema = "public") {
                    table = "messages"
                    filter = "conversation_id=eq.${channelId}"
                }.onEach {
                    val newMsg = it.decodeRecord<SupabaseMessage>()
                    // Avoid duplicating optimistic UI inserts
                    if (activeChannelMessages.none { existing -> existing.id == newMsg.id }) {
                        activeChannelMessages.add(0, newMsg)
                    }
                }.launchIn(viewModelScope)
                SupabaseApi.client.realtime.connect()
                channel.subscribe()
            } catch (e: Exception) {
                // Error fetching messages
            }
        }
    }

    // Post comment/post in active discussion channels
    fun sendDiscussionMessage(channelId: Long, text: String) {
        if (text.isBlank()) return

        viewModelScope.launch {
            try {
                val session = SupabaseApi.client.auth.currentSessionOrNull()
                val uid = session?.user?.id ?: return@launch

                val newMsg = SupabaseMessage(
                    conversationId = channelId,
                    senderId = uid,
                    content = text,
                    messageType = "text"
                )
                
                val insertedMsg = SupabaseApi.client.postgrest["messages"]
                    .insert(newMsg) { select() }
                    .decodeSingle<SupabaseMessage>()
                    
                // Optimistic UI update
                activeChannelMessages.add(0, insertedMsg)
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    // Upload file and send message
    fun uploadFileAndSendMessage(channelId: Long, text: String, fileBytes: ByteArray, fileName: String) {
        viewModelScope.launch {
            try {
                val session = SupabaseApi.client.auth.currentSessionOrNull()
                val uid = session?.user?.id ?: return@launch

                // 1. Upload to Supabase Storage bucket 'chat_media'
                val bucket = SupabaseApi.client.storage["chat_media"]
                val filePath = "$channelId/${System.currentTimeMillis()}_$fileName"
                bucket.upload(filePath, fileBytes)
                
                // 2. Get public URL
                val publicUrl = bucket.publicUrl(filePath)
                
                // 3. Insert message
                val newMsg = SupabaseMessage(
                    conversationId = channelId,
                    senderId = uid,
                    content = text.ifBlank { "Sent an attachment" },
                    messageType = "image", // or "pdf"
                    fileUrl = publicUrl
                )
                
                val insertedMsg = SupabaseApi.client.postgrest["messages"]
                    .insert(newMsg) { select() }
                    .decodeSingle<SupabaseMessage>()
                    
                activeChannelMessages.add(0, insertedMsg)
            } catch (e: Exception) {
                // Error handling
            }
        }
    }
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
    fun askAiAboutPost(msg: SupabaseMessage) {
        isGeneratingAI = true
        aiExplanationText = "Evaluating discussion context..."
        viewModelScope.launch {
            val systemInstructions = "You are a friendly and precise Nursing Mentor for the Ugandan study group 'Nurses Revision Uganda'. Clarify or validate the nursing discussion post. Give clinical accuracy points in brief."
            val userPrompt = "Provide an expert clinical explanation, verify the correctness, and summarize in high-yield points for this student discussion post:\n\"${msg.senderId}: ${msg.content}\""
            
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
    fun toggleMessageBookmark(msg: SupabaseMessage) {
        viewModelScope.launch {
            val msgIdStr = msg.id?.toString() ?: return@launch
            val isCurrentlyBookmarked = bookmarks.value.any { it.referenceId == msgIdStr && it.type == "DISCUSSION" }
            if (isCurrentlyBookmarked) {
                repository.removeBookmark(msgIdStr, "DISCUSSION")
                // repository.updateMessageBookmarkStatus(msg.id, false) // Not applicable to Supabase directly yet
            } else {
                repository.addBookmark(
                    type = "DISCUSSION",
                    title = "Group Thread: ${msg.senderId}",
                    description = if (msg.content.length > 50) msg.content.take(50) + "..." else msg.content,
                    refId = msgIdStr
                )
                // repository.updateMessageBookmarkStatus(msg.id, true)
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

    // --- Authentication ---
    var authError by mutableStateOf<String?>(null)
        private set

    fun checkAuthStatus() {
        viewModelScope.launch {
            try {
                // If there's an active session, navigate to Channels
                val session = SupabaseApi.client.auth.currentSessionOrNull()
                if (session != null) {
                    navigateTo(AppScreen.Channels)
                } else {
                    navigateTo(AppScreen.Welcome)
                }
            } catch (e: Exception) {
                authError = e.message
            }
        }
    }

    fun login(email: String, pass: String) {
        viewModelScope.launch {
            authError = null
            try {
                SupabaseApi.client.auth.signInWith(Email) {
                    this.email = email
                    password = pass
                }
                navigateTo(AppScreen.Channels)
            } catch (e: Exception) {
                authError = e.message ?: "Login failed"
            }
        }
    }

    fun signUp(email: String, pass: String, user: String, display: String) {
        viewModelScope.launch {
            authError = null
            try {
                val result = SupabaseApi.client.auth.signUpWith(Email) {
                    this.email = email
                    password = pass
                }
                val uid = result?.id ?: throw Exception("No user ID returned")
                
                // Insert into public.users
                val newUser = SupabaseUser(
                    id = uid,
                    username = user,
                    displayName = display,
                    email = email
                )
                SupabaseApi.client.postgrest["users"].insert(newUser)
                
                navigateTo(AppScreen.Channels)
            } catch (e: Exception) {
                authError = e.message ?: "Sign up failed"
            }
        }
    }
}
