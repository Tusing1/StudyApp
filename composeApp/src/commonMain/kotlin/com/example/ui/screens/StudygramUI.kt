package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.*
import com.example.ui.AppScreen
import com.example.ui.ChatMessage
import com.example.ui.StudygramViewModel
// Removed java.text and java.util imports

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudygramAppContent(
    viewModel: StudygramViewModel,
    modifier: Modifier = Modifier
) {
    val activeScreen = viewModel.currentScreen
    val userProfile by viewModel.userProfile.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            if (activeScreen !is AppScreen.Welcome && activeScreen !is AppScreen.Login && activeScreen !is AppScreen.SignUp) {
                StudygramBottomNavBar(
                    currentScreen = activeScreen,
                    onNavigate = { viewModel.navigateTo(it) }
                )
            }
        },
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (activeScreen) {
                is AppScreen.Welcome -> WelcomeScreen(
                    onNavigateToLogin = { viewModel.navigateTo(AppScreen.Login) },
                    onNavigateToSignUp = { viewModel.navigateTo(AppScreen.SignUp) }
                )
                is AppScreen.Login -> LoginScreen(
                    onNavigateBack = { viewModel.navigateTo(AppScreen.Welcome) },
                    onLoginSuccess = { viewModel.navigateTo(AppScreen.Channels) }
                )
                is AppScreen.SignUp -> SignUpScreen(
                    onNavigateBack = { viewModel.navigateTo(AppScreen.Welcome) },
                    onSignUpSuccess = { viewModel.navigateTo(AppScreen.Channels) }
                )
                is AppScreen.Channels -> ChannelsScreen(
                    viewModel = viewModel,
                    userProfile = userProfile
                )
                is AppScreen.Calls -> CallsScreen()
                is AppScreen.Contacts -> ContactsScreen(viewModel = viewModel)
                is AppScreen.Settings -> SettingsScreen(viewModel = viewModel)
                is AppScreen.Flashcards -> FlashcardsScreen(viewModel = viewModel)
                is AppScreen.Thread -> ThreadScreen(viewModel = viewModel, messageId = activeScreen.messageId)
                is AppScreen.AudioPlayer -> AudioPlayerScreen(viewModel = viewModel, url = activeScreen.url)
                is AppScreen.PdfViewer -> PdfViewerScreen(viewModel = viewModel, url = activeScreen.url)
                is AppScreen.AIChat -> AIChatScreen(viewModel = viewModel)
                is AppScreen.StudyLabs -> StudyLabsScreen(viewModel = viewModel)
                is AppScreen.DiscussionRoom -> RoomScreen(
                    viewModel = viewModel,
                    channel = activeScreen.channel
                )
                is AppScreen.ChannelInfo -> ChannelInfoScreen(
                    viewModel = viewModel,
                    channel = activeScreen.channel
                )
                is AppScreen.QuizPractice -> QuizScreen(
                    viewModel = viewModel
                )
                is AppScreen.AIChat -> AIChatPlaygroundScreen(
                    viewModel = viewModel
                )
                is AppScreen.Bookmarks -> BookmarksScreen(
                    viewModel = viewModel
                )
            }
        }
    }
}

@Composable
fun StudygramBottomNavBar(
    currentScreen: AppScreen,
    onNavigate: (AppScreen) -> Unit
) {
    NavigationBar(
        modifier = Modifier.testTag("bottom_nav_bar"),
        tonalElevation = 8.dp
    ) {
        NavigationBarItem(
            selected = currentScreen is AppScreen.Channels || currentScreen is AppScreen.DiscussionRoom,
            onClick = { onNavigate(AppScreen.Channels) },
            icon = { Icon(Icons.Default.Home, contentDescription = "Chats") },
            label = { Text("Chats", fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
            )
        )

        NavigationBarItem(
            selected = currentScreen is AppScreen.Calls,
            onClick = { onNavigate(AppScreen.Calls) },
            icon = { Icon(Icons.Default.Call, contentDescription = "Calls") },
            label = { Text("Calls", fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
            )
        )

        NavigationBarItem(
            selected = currentScreen is AppScreen.Contacts,
            onClick = { onNavigate(AppScreen.Contacts) },
            icon = { Icon(Icons.Default.Person, contentDescription = "Buddies") },
            label = { Text("Buddies", fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
            )
        )

        NavigationBarItem(
            selected = currentScreen is AppScreen.StudyLabs,
            onClick = { onNavigate(AppScreen.StudyLabs) },
            icon = { Icon(Icons.Default.Star, contentDescription = "Labs") },
            label = { Text("Labs", fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
            )
        )

        NavigationBarItem(
            selected = currentScreen is AppScreen.Settings,
            onClick = { onNavigate(AppScreen.Settings) },
            icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
            label = { Text("Settings", fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
            )
        )
    }
}

// 1. WELCOME / ONBOARDING SCREEN
@Composable
// 3. SELECTION ROOM FOR DISCUSSIONS (CHAT ROOM)
@Composable
fun RoomScreen(
    viewModel: StudygramViewModel,
    channel: SupabaseConversation
) {
    val messages = viewModel.activeChannelMessages
    var inputMessage by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    
    // Dialog state for AI Revision Helper response
    var showAiResponseDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // App bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            tonalElevation = 4.dp,
            color = MaterialTheme.colorScheme.surface
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.navigateTo(AppScreen.Channels) },
                    modifier = Modifier.testTag("back_button_room")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to Lobby"
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = channel.title ?: "Room ${channel.id}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Study group · ${channel.subscriberCount ?: 0} peers",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }

                    )
                }
            }
        }

        // Messages board
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(messages) { msg ->
                val isMe = msg.senderName == (viewModel.userProfile.value?.username ?: "")
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
                ) {
                    // Bubble frame
                    Surface(
                        color = if (isMe) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        },
                        shape = RoundedCornerShape(
                            topStart = 12.dp,
                            topEnd = 12.dp,
                            bottomStart = if (isMe) 12.dp else 0.dp,
                            bottomEnd = if (isMe) 0.dp else 12.dp
                        ),
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                        modifier = Modifier
                            .widthIn(max = 290.dp)
                            .testTag("message_bubble_${msg.id}")
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clip(CircleShape)
                                            .background(Color(msg.avatarColor))
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = msg.senderName,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = if (isMe) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = msg.senderField,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 8.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            Text(
                                text = msg.text,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 13.5.sp
                            )
                            
                            Spacer(modifier = Modifier.height(6.dp))

                            // Interactive shortcuts
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Bookmark trigger
                                IconButton(
                                    onClick = { viewModel.toggleMessageBookmark(msg) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = if (msg.isBookmarked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                        contentDescription = "Bookmark notes",
                                        tint = if (msg.isBookmarked) Color.Red else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                
                                Spacer(modifier = Modifier.width(8.dp))

                                // AI Mentor review action
                                Button(
                                    onClick = { 
                                        viewModel.askAiAboutPost(msg)
                                        showAiResponseDialog = true
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                        contentColor = MaterialTheme.colorScheme.primary
                                    ),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.height(22.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Face,
                                            contentDescription = "Ask AI Buddy",
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Ask AI Tutor", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                    val dateFormatted = remember(msg.timestamp) {
                        // TODO: Implement KMP-friendly datetime formatting in Phase 4
                        "12:00"
                    }
                    Text(
                        text = dateFormatted,
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                        modifier = Modifier.padding(top = 2.dp, start = 4.dp, end = 4.dp)
                    )
                }
            }
        }

        // Chat input bar
        Surface(
            tonalElevation = 8.dp,
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputMessage,
                    onValueChange = { inputMessage = it },
                    placeholder = { Text("Ask group a revision query...", fontSize = 13.5.sp) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_input_text_field"),
                    shape = RoundedCornerShape(20.dp),
                    maxLines = 3,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (inputMessage.isNotBlank()) {
                                viewModel.sendDiscussionMessage(inputMessage)
                                inputMessage = ""
                                focusManager.clearFocus()
                            }
                        }
                    )
                )

                Spacer(modifier = Modifier.width(6.dp))

                IconButton(
                    onClick = {
                        if (inputMessage.isNotBlank()) {
                            viewModel.sendDiscussionMessage(inputMessage)
                            inputMessage = ""
                            focusManager.clearFocus()
                        }
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                        .testTag("send_message_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }

    // AI Mentor tutor bottom dialog response
    if (showAiResponseDialog) {
        AiResponseDialog(
            explanation = viewModel.aiExplanationText,
            isLoading = viewModel.isGeneratingAI,
            onDismiss = { showAiResponseDialog = false }
        )
    }
}

// 4. LICENSURE QUIZZES REVISION DECK
@Composable
fun QuizScreen(
    viewModel: StudygramViewModel
) {
    val quizQuestions = viewModel.quizQuestions
    val currentIndex = viewModel.currentQuestionIndex
    val currentQuestion = quizQuestions.getOrNull(currentIndex)
    
    val subjects = listOf("All", "Pharmacology", "Midwifery", "Pediatrics", "Anatomy")
    var showAiResponseDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Exam header banner
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "UNMC Revision Decks",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Mock National Licensing Exams Prep",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
            
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "UGANDA SYLLABUS",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Subjects tags switcher row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            subjects.forEach { subject ->
                val isSelected = viewModel.activeQuizSubject == subject
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(40))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(
                                alpha = 0.6f
                            )
                        )
                        .clickable { viewModel.loadQuizQuestions(subject) }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("quiz_subject_$subject")
                ) {
                    Text(
                        text = subject,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (currentQuestion == null) {
            // Empty state helper
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(strokeWidth = 3.dp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Preparing revision decks...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        } else {
            // Current Revision MC Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("quiz_mcq_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Question index counter and bookmark toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${currentQuestion.subject.uppercase()} · QUESTION ${currentIndex + 1}/${quizQuestions.size}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )

                        IconButton(
                            onClick = { viewModel.toggleQuizBookmark(currentQuestion) }
                        ) {
                            val isBookmarked = viewModel.isQuizBookmarked(currentQuestion.id)
                            Icon(
                                imageVector = if (isBookmarked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Bookmark standard clinical question",
                                tint = if (isBookmarked) Color.Red else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = currentQuestion.questionText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 22.sp,
                        modifier = Modifier.weight(0.3f)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // MCQ Options buttons
                    Column(
                        modifier = Modifier.weight(0.7f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val options = listOf(
                            "A" to currentQuestion.optionA,
                            "B" to currentQuestion.optionB,
                            "C" to currentQuestion.optionC,
                            "D" to currentQuestion.optionD
                        )

                        options.forEach { (key, optionText) ->
                            val isSelected = viewModel.selectedOption == key
                            val hasAnswered = viewModel.hasAnswered
                            val isCorrectKey = currentQuestion.correctOption == key

                            val bColor = when {
                                hasAnswered && isCorrectKey -> Color(0xFFD4EDDA) // soft success green
                                hasAnswered && isSelected -> Color(0xFFF8D7DA) // soft err red
                                isSelected -> MaterialTheme.colorScheme.primaryContainer
                                else -> MaterialTheme.colorScheme.surface
                            }
                            
                            val tColor = when {
                                hasAnswered && isCorrectKey -> Color(0xFF155724)
                                hasAnswered && isSelected -> Color(0xFF721C24)
                                isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
                                else -> MaterialTheme.colorScheme.onSurface
                            }

                            val borderStroke = if (isSelected) {
                                BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                            } else {
                                BorderStroke(0.6.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                            }

                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = !hasAnswered) { viewModel.selectQuizOption(key) }
                                    .testTag("quiz_option_button_$key"),
                                color = bColor,
                                border = borderStroke,
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = key,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Text(
                                        text = optionText,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 13.sp,
                                        color = tColor
                                    )
                                }
                            }
                        }
                    }

                    // Bottom controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!viewModel.hasAnswered) {
                            Button(
                                onClick = { viewModel.submitQuizAnswer() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("submit_quiz_button"),
                                enabled = viewModel.selectedOption != null,
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Check Answer", fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Explain using Gemini AI
                                Button(
                                    onClick = {
                                        viewModel.askAiToExplainQuiz()
                                        showAiResponseDialog = true
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("explain_quiz_ai_button"),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Face,
                                            contentDescription = "AI explain details",
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("AI explanation", fontSize = 12.sp)
                                    }
                                }

                                // Load Next MCQ Question
                                Button(
                                    onClick = { viewModel.nextQuizQuestion() },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("next_quiz_button"),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Next Question", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // AI explanation dialog
    if (showAiResponseDialog) {
        AiResponseDialog(
            explanation = viewModel.aiExplanationText,
            isLoading = viewModel.isGeneratingAI,
            onDismiss = { showAiResponseDialog = false }
        )
    }
}

// 5. DEDICATED PLAYGROUND / AI CHAT BUDDY SCREEN
@Composable
fun AIChatPlaygroundScreen(
    viewModel: StudygramViewModel
) {
    val playMessages = viewModel.aiPlaygroundMessages
    var textPromptInput by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    // Preset revision templates that nursing student can click to immediately ask
    val clinicalPresetPrompts = listOf(
        "Calculate IV drip rate: 500mL over 6hrs, adult tubing (15 drops/min).",
        "Explain vaccine intervals of Pentavalent vaccination under UNEPI.",
        "List 5 critical nursing actions for a mother with postpartum hemorrhage (PPH)."
    )

    // Ensure we seed starting tutor greetings if empty
    if (playMessages.isEmpty()) {
        viewModel.clearAiChat()
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // AI Hub Header
        Surface(
            tonalElevation = 4.dp,
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Face,
                            contentDescription = "AI Buddy avatar",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Clinical AI Study Buddy",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Role-playing UNMC Licensure Mentor",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }

                IconButton(
                    onClick = { viewModel.clearAiChat() }
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset study canvas chats"
                    )
                }
            }
        }

        // Main chats view with preset shortcuts
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            contentPadding = PaddingValues(top = 10.dp, bottom = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text(
                    text = "Tap a high-yield study preset, or input your special query:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    clinicalPresetPrompts.forEach { promptItem ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.sendAiChatMessage(promptItem)
                                    focusManager.clearFocus()
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 1f))
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Ask preset query",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = promptItem,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            items(playMessages) { chatMessage ->
                val isCompanion = !chatMessage.isUser
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isCompanion) Arrangement.Start else Arrangement.End
                ) {
                    Surface(
                        color = if (isCompanion) {
                            MaterialTheme.colorScheme.surfaceVariant
                        } else {
                            MaterialTheme.colorScheme.primaryContainer
                        },
                        shape = RoundedCornerShape(
                            topStart = 12.dp,
                            topEnd = 12.dp,
                            bottomStart = if (isCompanion) 0.dp else 12.dp,
                            bottomEnd = if (isCompanion) 12.dp else 0.dp
                        ),
                        modifier = Modifier.widthIn(max = 280.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = chatMessage.senderName,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isCompanion) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onPrimaryContainer,
                                fontSize = 10.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = chatMessage.text,
                                style = MaterialTheme.typography.bodyMedium,
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }

            if (viewModel.isGeneratingAI) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "AI Study Companion is formulating reasoning...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }

        // Floating message keyboard input bar
        Surface(
            tonalElevation = 8.dp,
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface
        ) {
            Row(
                modifier = Modifier
                    .fillPaddingSafe()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = textPromptInput,
                    onValueChange = { textPromptInput = it },
                    placeholder = { Text("Ask any clinical drug calculations or nursing topics...") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("ai_companion_text_field"),
                    shape = RoundedCornerShape(20.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (textPromptInput.isNotBlank()) {
                                viewModel.sendAiChatMessage(textPromptInput)
                                textPromptInput = ""
                                focusManager.clearFocus()
                            }
                        }
                    )
                )

                Spacer(modifier = Modifier.width(6.dp))

                IconButton(
                    onClick = {
                        if (textPromptInput.isNotBlank()) {
                            viewModel.sendAiChatMessage(textPromptInput)
                            textPromptInput = ""
                            focusManager.clearFocus()
                        }
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                        .testTag("ai_companion_send_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Search",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// Helper safely handles bottom paddings
fun Modifier.fillPaddingSafe() = this.fillMaxWidth()

// 6. BOOKMARKS / STUDY NOTEBOOK MODULE
@Composable
fun BookmarksScreen(
    viewModel: StudygramViewModel
) {
    val bookmarks by viewModel.bookmarks.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Offline study Notebook",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Saved exam MCQs and discussion cards for revision anytime, even without internet access.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
        )

        if (bookmarks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Empty notebooks file icon",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Notebook is empty.",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                    Text(
                        text = "Tap the heart icon on discuss screens or standard revision quizzes to bookmark materials offline.",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        modifier = Modifier.padding(horizontal = 24.dp).padding(top = 4.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(bookmarks) { bookmark ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("bookmark_card_${bookmark.id}"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (bookmark.type == "QUIZ") Icons.Default.Star else Icons.Default.Home,
                                        contentDescription = "Bookmark Type",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = bookmark.title,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }

                                IconButton(
                                    onClick = { viewModel.deleteBookmarkDirect(bookmark) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete bookmark item",
                                        tint = Color.Red.copy(alpha = 0.6f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = bookmark.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

// ================= DIALOG COMPONENTS =================

@Composable
fun AiResponseDialog(
    explanation: String,
    isLoading: Boolean,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.7f)
                .testTag("ai_explanation_dialog"),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Toolbar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Face,
                            contentDescription = "AI face icon",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "AI Revision Explanation",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("ai_dialog_close")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss AI output"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                Spacer(modifier = Modifier.height(12.dp))

                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(strokeWidth = 3.dp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Formulating clinical steps...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        item {
                            Text(
                                text = explanation,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 21.sp,
                                fontFamily = FontFamily.SansSerif,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Understood, Back to Study", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
