package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.graphics.Brush
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
import com.example.ui.components.AnimatedGradientBackground
import com.example.ui.components.AnimatedScreenEnter
import com.example.ui.components.AppScreenShowsBottomNav
import com.example.ui.components.GlassCard
import com.example.ui.components.StudygramButton
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudygramAppContent(
    viewModel: StudygramViewModel,
    modifier: Modifier = Modifier
) {
    val activeScreen = viewModel.currentScreen
    val userProfile by viewModel.userProfile.collectAsState()
    val showBottomNav = AppScreenShowsBottomNav(activeScreen)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0.dp),
        containerColor = BgDeep,
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomNav,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut()
            ) {
                StudygramBottomNavBar(
                    currentScreen = activeScreen,
                    onNavigate = { viewModel.navigateTo(it) }
                )
            }
        }
    ) { innerPadding ->
        AnimatedGradientBackground(
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = innerPadding.calculateBottomPadding())
            ) {
                AnimatedContent(
                    targetState = activeScreen,
                    transitionSpec = {
                        fadeIn(tween(350)) + slideInHorizontally { it / 6 } togetherWith
                        fadeOut(tween(250)) + slideOutHorizontally { -it / 6 }
                },
                label = "screenTransition"
            ) { screen ->
                AnimatedScreenEnter {
                    when (screen) {
                        is AppScreen.Welcome -> WelcomeScreen(
                            onNavigateToLogin = { viewModel.navigateTo(AppScreen.Login) },
                            onNavigateToSignUp = { viewModel.navigateTo(AppScreen.SignUp) }
                        )
                        is AppScreen.Login -> LoginScreen(
                            viewModel = viewModel,
                            onNavigateBack = { viewModel.navigateTo(AppScreen.Welcome) },
                            onLoginSuccess = { viewModel.navigateTo(AppScreen.Channels) }
                        )
                        is AppScreen.SignUp -> SignUpScreen(
                            viewModel = viewModel,
                            onNavigateBack = { viewModel.navigateTo(AppScreen.Welcome) },
                            onSignUpSuccess = { viewModel.navigateTo(AppScreen.Channels) }
                        )
                        is AppScreen.Channels -> ChannelsScreen(
                            viewModel = viewModel,
                            userProfile = userProfile
                        )
                        is AppScreen.Calls -> CallsScreen(viewModel = viewModel)
                        is AppScreen.Contacts -> ContactsScreen(viewModel = viewModel)
                        is AppScreen.Settings -> SettingsScreen(viewModel = viewModel)
                        is AppScreen.StudyTokens -> StudyTokensScreen(viewModel = viewModel)
                        is AppScreen.Thread -> ThreadScreen(viewModel = viewModel, messageId = screen.messageId, channelId = screen.channelId)
                        is AppScreen.AudioPlayer -> AudioPlayerScreen(viewModel = viewModel, url = screen.url)
                        is AppScreen.PdfViewer -> PdfViewerScreen(viewModel = viewModel, url = screen.url)
                        is AppScreen.AIChat -> AIChatScreen(viewModel = viewModel)
                        is AppScreen.StudyLabs -> StudyLabsScreen(viewModel = viewModel)
                        is AppScreen.StudyBuddies -> StudyBuddiesScreen(viewModel = viewModel)
                        is AppScreen.Recordings -> RecordingsScreen(viewModel = viewModel)
                        is AppScreen.DiscussionRoom -> RoomScreen(
                            viewModel = viewModel,
                            channel = screen.channel
                        )
                        is AppScreen.ChannelInfo -> ChannelInfoScreen(
                            viewModel = viewModel,
                            channel = screen.channel
                        )
                        is AppScreen.QuizPractice -> QuizScreen(viewModel = viewModel)
                        is AppScreen.Bookmarks -> BookmarksScreen(viewModel = viewModel)
                        is AppScreen.InterestsOnboarding -> InterestsOnboardingScreen(
                            viewModel = viewModel,
                            onComplete = { viewModel.navigateTo(AppScreen.Channels) }
                        )
                    }
                }
            }
        }

            val incomingCall = viewModel.activeIncomingCall
            if (incomingCall != null) {
                IncomingCallOverlay(
                    onAccept = { viewModel.acceptIncomingCall(incomingCall) },
                    onDecline = { viewModel.declineIncomingCall() }
                )
            }
        }
    }
}

@Composable
private fun IncomingCallOverlay(
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    val pulse = rememberInfiniteTransition(label = "callPulse").animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
        label = "callScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center
    ) {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .padding(16.dp),
            cornerRadius = 28.dp
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("📞 Incoming Call!", fontSize = 22.sp, fontWeight = FontWeight.Black, color = Color.White)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Someone wants to study with you", color = TextMuted, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(24.dp))
                Box(
                    modifier = Modifier
                        .size((90 * pulse.value).dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(listOf(NeonGreen.copy(0.3f), Color.Transparent))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🎙️", fontSize = 40.sp)
                }
                Spacer(modifier = Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StudygramButton(
                        text = "Accept",
                        emoji = "✅",
                        onClick = onAccept,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = onDecline) {
                    Text("Decline", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun StudygramBottomNavBar(
    currentScreen: AppScreen,
    onNavigate: (AppScreen) -> Unit
) {
    val tabs = listOf(
        Triple(AppScreen.Channels, "💬", "Chats"),
        Triple(AppScreen.Calls, "📞", "Calls"),
        Triple(AppScreen.Contacts, "👋", "Buddies"),
        Triple(AppScreen.StudyLabs, "🎮", "Labs"),
        Triple(AppScreen.Settings, "⚡", "Me")
    )

    Surface(
        modifier = Modifier.testTag("bottom_nav_bar"),
        color = BgSurface.copy(alpha = 0.95f),
        shadowElevation = 16.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            tabs.forEach { (screen, emoji, label) ->
                val selected = when (screen) {
                    AppScreen.Channels -> currentScreen is AppScreen.Channels
                    AppScreen.Calls -> currentScreen is AppScreen.Calls
                    AppScreen.Contacts -> currentScreen is AppScreen.Contacts
                    AppScreen.StudyLabs -> currentScreen is AppScreen.StudyLabs
                    AppScreen.Settings -> currentScreen is AppScreen.Settings
                    else -> false
                }
                val scale by animateFloatAsState(
                    targetValue = if (selected) 1.15f else 1f,
                    animationSpec = spring(dampingRatio = 0.5f, stiffness = 400f),
                    label = "navScale"
                )

                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onNavigate(screen) }
                        .background(
                            if (selected) Brush.verticalGradient(
                                listOf(NeonPurple.copy(0.25f), NeonPink.copy(0.15f))
                            ) else Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Transparent)
                            )
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(emoji, fontSize = (20 * scale).sp)
                    Text(
                        label,
                        fontSize = 10.sp,
                        fontWeight = if (selected) FontWeight.Black else FontWeight.Medium,
                        color = if (selected) NeonCyan else TextMuted
                    )
                }
            }
        }
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
        com.example.ui.components.StudygramTopBar(
            title = "Quiz Blitz",
            emoji = "🧠",
            onBack = { viewModel.navigateTo(AppScreen.StudyLabs) }
        )
        Text(
            text = "Crush licensing exams — one question at a time!",
            color = TextMuted,
            fontSize = 13.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            subjects.forEach { subject ->
                com.example.ui.components.NeonPill(
                    text = subject,
                    selected = viewModel.activeQuizSubject == subject,
                    onClick = { viewModel.loadQuizQuestions(subject) },
                    modifier = Modifier.testTag("quiz_subject_$subject")
                )
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

// BOOKMARKS / STUDY NOTEBOOK MODULE
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
        com.example.ui.components.StudygramTopBar(
            title = "Saved Notes",
            emoji = "🔖",
            onBack = { viewModel.navigateTo(AppScreen.Channels) }
        )
        Text(
            text = "Your bookmarked quizzes & chats — study offline anytime!",
            color = TextMuted,
            fontSize = 13.sp,
            modifier = Modifier.padding(bottom = 16.dp)
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
