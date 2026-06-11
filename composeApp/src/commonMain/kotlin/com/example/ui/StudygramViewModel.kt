package com.example.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.repository.StudyRepository
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.decodeRecord
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
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
    object AIChat : AppScreen
    object StudyLabs : AppScreen
    object QuizPractice : AppScreen
    data class Thread(val messageId: String, val channelId: String) : AppScreen
    data class AudioPlayer(val url: String) : AppScreen
    data class PdfViewer(val url: String) : AppScreen
    object Bookmarks : AppScreen
    object InterestsOnboarding : AppScreen
    object StudyTokens : AppScreen
    object StudyBuddies : AppScreen
    object Recordings : AppScreen
}

data class StudyTransaction(
    val description: String,
    val amount: Int,
    val isEarn: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

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

    var contactsInitialTab by mutableStateOf("friends")

    var remoteProfile by mutableStateOf<SupabaseProfile?>(null)
    val availableInterests = mutableStateListOf<SupabaseInterestCategory>()
    var savingInterests by mutableStateOf(false)
    var loadingInterests by mutableStateOf(false)

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

    // Comment thread states
    val messageCommentCounts: MutableMap<String, Int> = mutableStateMapOf<String, Int>()
    val threadOriginalMessage = mutableStateOf<SupabaseMessage?>(null)
    val threadReplies = mutableStateListOf<SupabaseMessage>()
    var threadRepliesLoading by mutableStateOf(false)
    fun createGroup(name: String, description: String, memberIds: List<String>, onSuccess: (String) -> Unit) {
        viewModelScope.launch {
            val convId = repository.createGroup(name, description, memberIds)
            if (convId != null) {
                fetchChannels()
                onSuccess(convId)
            }
        }
    }

    fun createChannel(name: String, description: String, enableDiscussion: Boolean, onSuccess: (String) -> Unit) {
        viewModelScope.launch {
            val convId = repository.createChannel(name, description, enableDiscussion)
            if (convId != null) {
                fetchChannels()
                onSuccess(convId)
            }
        }
    }

    fun archiveConversation(conversationId: String) {
        viewModelScope.launch {
            try {
                SupabaseApi.client.postgrest["conversations"]
                    .update({
                        set("is_archived", true)
                    }) {
                        filter {
                            eq("id", conversationId)
                        }
                    }
                fetchChannels()
            } catch (e: Exception) {
                println("Error archiving conversation: ${e.message}")
            }
        }
    }

    fun unarchiveConversation(conversationId: String) {
        viewModelScope.launch {
            try {
                SupabaseApi.client.postgrest["conversations"]
                    .update({
                        set("is_archived", false)
                    }) {
                        filter {
                            eq("id", conversationId)
                        }
                    }
                fetchChannels()
            } catch (e: Exception) {
                println("Error unarchiving conversation: ${e.message}")
            }
        }
    }

    fun deleteConversation(conversationId: String) {
        viewModelScope.launch {
            try {
                // Delete messages
                SupabaseApi.client.postgrest["messages"].delete {
                    filter {
                        eq("conversation_id", conversationId)
                    }
                }
                // Delete participants
                SupabaseApi.client.postgrest["conversation_participants"].delete {
                    filter {
                        eq("conversation_id", conversationId)
                    }
                }
                // Delete conversation
                SupabaseApi.client.postgrest["conversations"].delete {
                    filter {
                        eq("id", conversationId)
                    }
                }
                fetchChannels()
            } catch (e: Exception) {
                println("Error deleting conversation: ${e.message}")
            }
        }
    }

    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            try {
                SupabaseApi.client.postgrest["messages"].delete {
                    filter {
                        eq("id", messageId)
                    }
                }
                activeChannelId?.let { loadMessagesForChannel(it) }
            } catch (e: Exception) {
                println("Error deleting message: ${e.message}")
            }
        }
    }


    fun getUserRole(conversationId: String): String? {
        val session = SupabaseApi.client.auth.currentSessionOrNull() ?: return null
        val myUid = session.user?.id ?: return null
        val conv = conversations.find { it.conversation.id == conversationId } 
            ?: archivedConversations.find { it.conversation.id == conversationId }
            ?: return null
        val participant = conv.participants.find { it.participant.userId == myUid }
        return participant?.participant?.role
    }

    private var activeChannelId: String? = null
    val channels = mutableStateListOf<SupabaseConversation>()
    val conversations = mutableStateListOf<ConversationWithDetails>()
    val archivedConversations = mutableStateListOf<ConversationWithDetails>()
    var conversationsLoading by mutableStateOf(false)


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

    // Tinder Matching / Study Buddies
    val potentialBuddies = mutableStateListOf<com.example.data.UserProfile>()
    val matchedBuddies = mutableStateListOf<com.example.data.UserProfile>()

    var activeIncomingCall by mutableStateOf<SupabaseCall?>(null)

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

            SupabaseApi.client.channel("public:calls_listener")
                .postgresChangeFlow<io.github.jan.supabase.realtime.PostgresAction.Insert>(schema = "public") {
                    table = "calls"
                    filter("is_active", FilterOperator.EQ, true)
                }
                .onEach { action ->
                    val call = action.decodeRecord<com.example.data.SupabaseCall>()
                    if (call.startedBy == myUid) return@onEach

                    try {
                        val isParticipant = SupabaseApi.client.postgrest["conversation_participants"]
                            .select {
                                filter {
                                    eq("conversation_id", call.conversationId)
                                    eq("user_id", myUid)
                                }
                            }
                            .decodeList<SupabaseConversationParticipant>()
                            .isNotEmpty()

                        if (isParticipant) {
                            activeIncomingCall = call
                        }
                    } catch (e: Exception) {
                        println("Error checking call participation: ${e.message}")
                    }
                }
                .launchIn(this)
        }
    }

    fun acceptIncomingCall(call: SupabaseCall) {
        val session = SupabaseApi.client.auth.currentSessionOrNull()
        val myUid = session?.user?.id ?: return
        callManager.answerCall(call.id ?: "", myUid)
        activeIncomingCall = null
        navigateTo(AppScreen.Calls)
    }

    fun declineIncomingCall() {
        activeIncomingCall = null
    }

    fun fetchChannels() {
        viewModelScope.launch {
            fetchPotentialBuddies()
            fetchRealRecordings()
            conversationsLoading = true
            try {
                val session = SupabaseApi.client.auth.currentSessionOrNull()
                val myUid = session?.user?.id
                if (myUid == null) {
                    conversationsLoading = false
                    return@launch
                }

                val participants = SupabaseApi.client.postgrest["conversation_participants"]
                    .select {
                        filter {
                            eq("user_id", myUid)
                        }
                    }
                    .decodeList<SupabaseConversationParticipant>()

                val convIds = participants.map { it.conversationId }
                if (convIds.isEmpty()) {
                    conversations.clear()
                    archivedConversations.clear()
                    conversationsLoading = false
                    return@launch
                }

                val dbConversations = SupabaseApi.client.postgrest["conversations"]
                    .select()
                    .decodeList<SupabaseConversation>()
                    .filter { it.id in convIds }

                val allParticipants = SupabaseApi.client.postgrest["conversation_participants"]
                    .select()
                    .decodeList<SupabaseConversationParticipant>()
                    .filter { it.conversationId in convIds }

                val allUserIds = allParticipants.map { it.userId }.distinct()
                val allProfiles = SupabaseApi.client.postgrest["profiles"]
                    .select()
                    .decodeList<SupabaseProfile>()
                    .filter { it.userId in allUserIds }

                val lastMessages = try {
                    SupabaseApi.client.postgrest["messages"]
                        .select {
                            order("created_at", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                        }
                        .decodeList<SupabaseMessage>()
                        .filter { it.conversationId in convIds }
                } catch (e: Exception) {
                    emptyList()
                }

                val activeList = mutableListOf<ConversationWithDetails>()
                val archivedList = mutableListOf<ConversationWithDetails>()

                for (conv in dbConversations) {
                    val convParticipants = allParticipants.filter { it.conversationId == conv.id }
                    val participantsWithProfiles = convParticipants.map { p ->
                        ParticipantWithProfile(
                            participant = p,
                            profile = allProfiles.find { it.userId == p.userId }
                        )
                    }

                    val convMessages = lastMessages.filter { it.conversationId == conv.id }
                    val lastMsg = convMessages.firstOrNull()

                    val isSelfChat = conv.type == "direct" && convParticipants.size == 1 && convParticipants[0].userId == myUid
                    val isSavedMessages = isSelfChat || conv.type == "saved"

                    val details = ConversationWithDetails(
                        conversation = conv,
                        participants = participantsWithProfiles,
                        lastMessage = lastMsg,
                        isSavedMessages = isSavedMessages,
                        isSelfChat = isSelfChat
                    )

                    if (conv.isArchived) {
                        archivedList.add(details)
                    } else {
                        activeList.add(details)
                    }
                }

                val sortFn = { a: ConversationWithDetails, b: ConversationWithDetails ->
                    val aTime = a.lastMessage?.createdAt ?: a.conversation.updatedAt ?: a.conversation.createdAt ?: ""
                    val bTime = b.lastMessage?.createdAt ?: b.conversation.updatedAt ?: b.conversation.createdAt ?: ""
                    bTime.compareTo(aTime)
                }
                activeList.sortWith(Comparator(sortFn))
                archivedList.sortWith(Comparator(sortFn))

                conversations.clear()
                conversations.addAll(activeList)
                
                archivedConversations.clear()
                archivedConversations.addAll(archivedList)

                channels.clear()
                channels.addAll(dbConversations)
            } catch (e: Exception) {
                println("Error fetching conversations: ${e.message}")
            } finally {
                conversationsLoading = false
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

    // Gamification
    val tokenTransactions = mutableStateListOf<StudyTransaction>(
        StudyTransaction("Daily Login Bonus", 15, true, System.currentTimeMillis() - 3600000),
        StudyTransaction("Complete Profile Setup", 25, true, System.currentTimeMillis() - 86400000)
    )
    var loginStreak by mutableStateOf(3)

    fun addStudyTokens(amount: Int, description: String = "Lab game reward") {
        viewModelScope.launch {
            repository.addStudyTokens(amount)
            tokenTransactions.add(0, StudyTransaction(description, amount, true))
        }
    }

    fun spendStudyTokens(amount: Int, description: String): Boolean {
        val currentTokens = userProfile.value?.studyTokens ?: 0
        if (currentTokens >= amount) {
            viewModelScope.launch {
                repository.addStudyTokens(-amount)
                tokenTransactions.add(0, StudyTransaction(description, amount, false))
            }
            return true
        }
        return false
    }

    // Study Buddies Matching
    fun matchWithBuddy(buddyId: String, isMatch: Boolean) {
        val buddy = potentialBuddies.find { it.id == buddyId }
        if (buddy != null) {
            potentialBuddies.remove(buddy)
            if (isMatch) {
                matchedBuddies.add(buddy)
            }
        }
    }

    fun updateGeminiApiKey(key: String) {
        viewModelScope.launch {
            repository.updateGeminiApiKey(key)
        }
    }

    fun updateNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateNotificationsEnabled(enabled)
        }
    }

    fun updateProfileDetails(username: String, field: String, color: Int) {
        viewModelScope.launch {
            repository.updateProfileDetails(username, field, color)
        }
    }

    fun logout() {
        viewModelScope.launch {
            try {
                SupabaseApi.client.auth.signOut()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            repository.clearProfile()
            currentScreen = AppScreen.Welcome
        }
    }

    // Load messages dynamically and collect reactive updates
    private fun loadMessagesForChannel(channelId: String) {
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

                // Fetch comment counts if it's a channel and has a linked discussion group
                val activeConv = conversations.find { it.conversation.id == channelId }?.conversation
                    ?: archivedConversations.find { it.conversation.id == channelId }?.conversation
                val discussionId = activeConv?.linkedDiscussionId
                if (discussionId != null) {
                    fetchCommentCountsForChannel(discussionId)
                }

                // Cache locally in Room DB
                for (m in msgs) {
                    val localMsg = DiscussionMessage(
                        channelId = channelId,
                        senderName = m.senderId,
                        senderField = "Student",
                        avatarColor = 0xFF00796B.toInt(),
                        text = m.content ?: "",
                        isAiGenerated = m.messageType == "ai"
                    )
                    repository.insertMessageDirect(localMsg)
                }

                val channel = SupabaseApi.client.channel("public:messages:$channelId")
                channel.postgresChangeFlow<io.github.jan.supabase.realtime.PostgresAction.Insert>(schema = "public") {
                    table = "messages"
                    filter("conversation_id", FilterOperator.EQ, channelId)
                }.onEach {
                    val newMsg = it.decodeRecord<SupabaseMessage>()
                    // Avoid duplicating optimistic UI inserts
                    if (activeChannelMessages.none { existing -> existing.id == newMsg.id }) {
                        activeChannelMessages.add(0, newMsg)
                    }
                    // Cache realtime message too
                    val localMsg = DiscussionMessage(
                        channelId = channelId,
                        senderName = newMsg.senderId,
                        senderField = "Student",
                        avatarColor = 0xFF00796B.toInt(),
                        text = newMsg.content ?: "",
                        isAiGenerated = newMsg.messageType == "ai"
                    )
                    repository.insertMessageDirect(localMsg)
                }.launchIn(viewModelScope)
                SupabaseApi.client.realtime.connect()
                channel.subscribe()
            } catch (e: Exception) {
                // Fallback to offline cache
                repository.getMessages(channelId).collect { localMsgs ->
                    if (activeChannelId == channelId) {
                        val mapped = localMsgs.map { local ->
                            SupabaseMessage(
                                id = local.id.toString(),
                                conversationId = local.channelId,
                                senderId = local.senderName,
                                content = local.text,
                                messageType = if (local.isAiGenerated) "ai" else "text"
                            )
                        }
                        activeChannelMessages.clear()
                        activeChannelMessages.addAll(mapped)
                    }
                }
            }
        }
    }

    // Post comment/post in active discussion channels
    fun sendDiscussionMessage(channelId: String, text: String) {
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

    fun uploadFileAndSendMessage(
        channelId: String,
        text: String,
        fileBytes: ByteArray,
        fileName: String,
        messageType: String = "image"
    ) {
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
                    messageType = messageType,
                    fileUrl = publicUrl,
                    fileName = fileName,
                    fileSize = fileBytes.size.toLong()
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

        if (isCorrectAnswer) {
            addStudyTokens(10)
        }
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

    // Direct Chat with AI Tutor
    fun sendMessageToAi(userText: String) {
        if (aiPlaygroundMessages.isEmpty()) {
            aiPlaygroundMessages.add(ChatMessage(
                senderName = "AI Tutor",
                text = "Hello! I am your Studygram AI Tutor. How can I help you today?",
                isUser = false
            ))
        }
        
        aiPlaygroundMessages.add(ChatMessage(
            senderName = "Me",
            text = userText,
            isUser = true
        ))
        
        isGeneratingAI = true
        
        viewModelScope.launch {
            val systemInstructions = "You are an AI Study Tutor for Ugandan nursing students. Be helpful and encouraging."
            val response = repository.askGemini(userText, systemInstructions)
            
            aiPlaygroundMessages.add(ChatMessage(
                senderName = "AI Tutor",
                text = response,
                isUser = false
            ))
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
                    description = if ((msg.content?.length ?: 0) > 50) msg.content?.take(50) + "..." else (msg.content ?: ""),
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
                SupabaseApi.client.auth.awaitInitialization()
                val session = SupabaseApi.client.auth.currentSessionOrNull()
                if (session != null) {
                    fetchRemoteProfile()
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
                fetchRemoteProfile()
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
                    data = kotlinx.serialization.json.buildJsonObject {
                        put("username", kotlinx.serialization.json.JsonPrimitive(user))
                        put("full_name", kotlinx.serialization.json.JsonPrimitive(display))
                    }
                }
                val uid = result?.id ?: throw Exception("No user ID returned")
                
                // Insert into public.profiles
                val newProfile = SupabaseProfile(
                    id = uid,
                    userId = uid,
                    username = user,
                    fullName = display
                )
                SupabaseApi.client.postgrest["profiles"].insert(newProfile)
                
                fetchRemoteProfile()
            } catch (e: Exception) {
                authError = e.message ?: "Sign up failed"
            }
        }
    }

    fun fetchRemoteProfile() {
        viewModelScope.launch {
            try {
                val session = SupabaseApi.client.auth.currentSessionOrNull() ?: return@launch
                val uid = session.user?.id ?: return@launch
                val profile = SupabaseApi.client.postgrest["profiles"]
                    .select { filter { eq("user_id", uid) } }
                    .decodeSingle<SupabaseProfile>()
                remoteProfile = profile
                
                // Synchronize locally in Room DB
                val db = DatabaseProvider.getDatabase()
                val dao = db.userProfileDao()
                val localProf = dao.getUserProfileDirect()
                if (localProf == null) {
                    val activeSession = SupabaseApi.client.auth.currentSessionOrNull()
                    val sessionJson = activeSession?.let {
                        kotlinx.serialization.json.Json.encodeToString(io.github.jan.supabase.auth.user.UserSession.serializer(), it)
                    } ?: ""
                    dao.insertProfile(
                        UserProfile(
                            id = "local_user",
                            username = profile.username,
                            nursingField = profile.bio ?: "General",
                            avatarColor = profile.interests?.hashCode() ?: 0xFF8B5CF6.toInt(),
                            supabaseSessionJson = sessionJson
                        )
                    )
                } else {
                    dao.updateProfileDetails(
                        profile.username,
                        profile.bio ?: "General",
                        profile.interests?.hashCode() ?: 0xFF8B5CF6.toInt()
                    )
                }

                if (profile.interests == null || profile.interests.size < 3) {
                    navigateTo(AppScreen.InterestsOnboarding)
                } else {
                    navigateTo(AppScreen.Channels)
                }
            } catch (e: Exception) {
                println("Error fetching remote profile: ${e.message}")
                // Fallback to Channels if profile retrieval fails
                navigateTo(AppScreen.Channels)
            }
        }
    }

    fun fetchAvailableInterests() {
        loadingInterests = true
        viewModelScope.launch {
            try {
                val list = SupabaseApi.client.postgrest["interest_categories"]
                    .select()
                    .decodeList<SupabaseInterestCategory>()
                availableInterests.clear()
                availableInterests.addAll(list)
            } catch (e: Exception) {
                availableInterests.clear()
                availableInterests.addAll(listOf(
                    SupabaseInterestCategory("1", "Pharmacology", "💊"),
                    SupabaseInterestCategory("2", "Midwifery", "🤰"),
                    SupabaseInterestCategory("3", "Pediatrics", "👶"),
                    SupabaseInterestCategory("4", "Anatomy", "🫀"),
                    SupabaseInterestCategory("5", "Surgical Review", "🏥"),
                    SupabaseInterestCategory("6", "General Nursing", "🩺"),
                    SupabaseInterestCategory("7", "Midwifery Care", "🍼"),
                    SupabaseInterestCategory("8", "Child Health", "🧸"),
                    SupabaseInterestCategory("9", "Calculations", "✏️")
                ))
            } finally {
                loadingInterests = false
            }
        }
    }

    fun saveInterests(selectedInterests: List<String>, onComplete: () -> Unit) {
        savingInterests = true
        viewModelScope.launch {
            try {
                val session = SupabaseApi.client.auth.currentSessionOrNull() ?: return@launch
                val uid = session.user?.id ?: return@launch
                
                SupabaseApi.client.postgrest["profiles"]
                    .update({
                        set("interests", selectedInterests)
                    }) {
                        filter {
                            eq("user_id", uid)
                        }
                    }
                
                fetchRemoteProfile()
                onComplete()
            } catch (e: Exception) {
                println("Error saving interests: ${e.message}")
            } finally {
                savingInterests = false
            }
        }
    }

    fun fetchCommentCountsForChannel(discussionGroupId: String) {
        viewModelScope.launch {
            try {
                val msgs = SupabaseApi.client.postgrest["messages"]
                    .select {
                        filter {
                            eq("conversation_id", discussionGroupId)
                        }
                    }
                    .decodeList<SupabaseMessage>()
                
                val counts = msgs.filter { it.replyToChannelMessageId != null }
                    .groupBy { it.replyToChannelMessageId!! }
                    .mapValues { it.value.size }
                
                counts.forEach { (msgId, count) ->
                    messageCommentCounts.put(msgId, count)
                }
            } catch (e: Exception) {
                println("Error fetching comment counts: ${e.message}")
            }
        }
    }

    fun loadThreadComments(channelMessageId: String, discussionGroupId: String) {
        threadRepliesLoading = true
        viewModelScope.launch {
            try {
                // 1. Find or fetch original message
                val localMsg = activeChannelMessages.find { it.id == channelMessageId }
                if (localMsg != null) {
                    threadOriginalMessage.value = localMsg
                } else {
                    val originalList = SupabaseApi.client.postgrest["messages"]
                        .select { filter { eq("id", channelMessageId) } }
                        .decodeList<SupabaseMessage>()
                    threadOriginalMessage.value = originalList.firstOrNull()
                }

                // 2. Fetch comments from discussion group
                val msgs = SupabaseApi.client.postgrest["messages"]
                    .select {
                        filter {
                            eq("conversation_id", discussionGroupId)
                            eq("reply_to_channel_message_id", channelMessageId)
                        }
                        order("created_at", io.github.jan.supabase.postgrest.query.Order.ASCENDING)
                    }
                    .decodeList<SupabaseMessage>()
                
                threadReplies.clear()
                threadReplies.addAll(msgs)

                // 3. Listen to Realtime updates for comment thread
                val channel = SupabaseApi.client.channel("public:comments:$channelMessageId")
                channel.postgresChangeFlow<io.github.jan.supabase.realtime.PostgresAction.Insert>(schema = "public") {
                    table = "messages"
                    filter("conversation_id", FilterOperator.EQ, discussionGroupId)
                    filter("reply_to_channel_message_id", FilterOperator.EQ, channelMessageId)
                }.onEach {
                    val newComment = it.decodeRecord<SupabaseMessage>()
                    if (threadReplies.none { existing -> existing.id == newComment.id }) {
                        threadReplies.add(newComment)
                        val currentCount = messageCommentCounts[channelMessageId] ?: 0
                        messageCommentCounts.put(channelMessageId, currentCount + 1)
                    }
                }.launchIn(viewModelScope)

                SupabaseApi.client.realtime.connect()
                channel.subscribe()
            } catch (e: Exception) {
                println("Error loading comments thread: ${e.message}")
            } finally {
                threadRepliesLoading = false
            }
        }
    }

    fun sendThreadComment(channelMessageId: String, discussionGroupId: String, content: String) {
        if (content.isBlank()) return
        viewModelScope.launch {
            try {
                val session = SupabaseApi.client.auth.currentSessionOrNull()
                val uid = session?.user?.id ?: return@launch

                val newMsg = SupabaseMessage(
                    conversationId = discussionGroupId,
                    senderId = uid,
                    content = content,
                    replyToChannelMessageId = channelMessageId,
                    messageType = "text"
                )

                val insertedMsg = SupabaseApi.client.postgrest["messages"]
                    .insert(newMsg) { select() }
                    .decodeSingle<SupabaseMessage>()
                
                if (threadReplies.none { it.id == insertedMsg.id }) {
                    threadReplies.add(insertedMsg)
                }
                
                val currentCount = messageCommentCounts[channelMessageId] ?: 0
                messageCommentCounts.put(channelMessageId, currentCount + 1)
            } catch (e: Exception) {
                println("Error sending comment: ${e.message}")
            }
        }
    }

    val realRecordings = mutableStateListOf<SupabaseCall>()
    var recordingsLoading by mutableStateOf(false)

    fun fetchRealRecordings() {
        recordingsLoading = true
        viewModelScope.launch {
            try {
                val list = SupabaseApi.client.postgrest["calls"]
                    .select()
                    .decodeList<SupabaseCall>()
                    .filter { !it.recordingUrl.isNullOrBlank() }
                realRecordings.clear()
                realRecordings.addAll(list)
            } catch (e: Exception) {
                println("Error fetching recordings: ${e.message}")
            } finally {
                recordingsLoading = false
            }
        }
    }

    fun fetchPotentialBuddies() {
        viewModelScope.launch {
            try {
                val session = SupabaseApi.client.auth.currentSessionOrNull()
                val myUid = session?.user?.id ?: return@launch
                
                val profiles = SupabaseApi.client.postgrest["profiles"]
                    .select()
                    .decodeList<SupabaseProfile>()
                
                val realPeers = profiles.filter { it.userId != myUid }.map { sp ->
                    com.example.data.UserProfile(
                        id = sp.userId,
                        username = sp.username,
                        nursingField = sp.interests?.firstOrNull() ?: "General Nursing",
                        avatarColor = sp.interests?.hashCode() ?: 0xFF8B5CF6.toInt()
                    )
                }
                potentialBuddies.clear()
                potentialBuddies.addAll(realPeers)
            } catch (e: Exception) {
                println("Error fetching potential buddies: ${e.message}")
                potentialBuddies.clear()
                potentialBuddies.addAll(com.example.data.DefaultData.MOCK_PEERS)
            }
        }
    }
}
