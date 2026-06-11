package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SupabaseMessage
import com.example.ui.AppScreen
import com.example.ui.StudygramViewModel
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.TextMuted

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThreadScreen(
    viewModel: StudygramViewModel,
    messageId: String,
    channelId: String
) {
    val originalChannel = viewModel.channels.find { it.id == channelId }
    val discussionGroupId = originalChannel?.linkedDiscussionId ?: ""

    // Trigger comment loading on start
    LaunchedEffect(messageId, discussionGroupId) {
        if (discussionGroupId.isNotEmpty()) {
            viewModel.loadThreadComments(messageId, discussionGroupId)
        }
    }

    val originalMessage = viewModel.threadOriginalMessage.value
    val replies = viewModel.threadReplies
    val loading = viewModel.threadRepliesLoading
    var inputText by remember { mutableStateOf("") }

    val discussionDetails = viewModel.conversations.find { it.conversation.id == discussionGroupId }
        ?: viewModel.archivedConversations.find { it.conversation.id == discussionGroupId }

    val myUid = viewModel.remoteProfile?.userId ?: ""

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Comments", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = {
                        val ch = viewModel.channels.find { it.id == channelId }
                        if (ch != null) {
                            viewModel.navigateTo(AppScreen.DiscussionRoom(ch))
                        } else {
                            viewModel.navigateTo(AppScreen.Channels)
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White.copy(alpha = 0.02f))
            )
        },
        bottomBar = {
            Surface(
                color = Color.White.copy(alpha = 0.02f),
                tonalElevation = 4.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp)
                        .navigationBarsPadding(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text("Reply to comment...", color = Color.White.copy(alpha = 0.4f)) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        maxLines = 4,
                        textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.White.copy(alpha = 0.15f),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.08f),
                            focusedContainerColor = Color.White.copy(alpha = 0.04f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.04f)
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(
                            onClick = {
                                if (inputText.isNotBlank() && discussionGroupId.isNotEmpty()) {
                                    viewModel.sendThreadComment(messageId, discussionGroupId, inputText)
                                    inputText = ""
                                }
                            },
                            enabled = inputText.isNotBlank() && discussionGroupId.isNotEmpty()
                        ) {
                            Icon(Icons.Default.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.25f))
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Original Post Card
                item {
                    if (originalMessage != null) {
                        val senderProfile = discussionDetails?.participants?.find { it.participant.userId == originalMessage.senderId }?.profile
                        val senderName = senderProfile?.fullName ?: senderProfile?.username ?: "Channel Admin"
                        
                        com.example.ui.components.GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = 16.dp
                        ) {
                            Column {
                                Text(
                                    text = senderName,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp,
                                    color = NeonCyan
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = originalMessage.content ?: "",
                                    fontSize = 15.sp,
                                    color = Color.White,
                                    lineHeight = 20.sp
                                )
                            }
                        }
                    } else {
                        com.example.ui.components.GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = 16.dp
                        ) {
                            Text("Loading original post...", color = TextMuted, fontSize = 14.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Replies",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }

                if (loading && replies.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(100.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    }
                } else if (replies.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(100.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No comments yet. Be the first to reply!",
                                color = Color.White.copy(alpha = 0.4f),
                                fontSize = 13.sp
                            )
                        }
                    }
                } else {
                    items(replies) { reply ->
                        val isMe = reply.senderId == myUid
                        val senderProfile = discussionDetails?.participants?.find { it.participant.userId == reply.senderId }?.profile
                        val displayName = if (isMe) "You" else (senderProfile?.fullName ?: senderProfile?.username ?: "User")
                        val avatarColor = senderProfile?.interests?.hashCode() ?: 0xFF1976D2.toInt()

                        val bubbleGradient = if (isMe) {
                            androidx.compose.ui.graphics.Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                                )
                            )
                        } else {
                            androidx.compose.ui.graphics.Brush.linearGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.08f),
                                    Color.White.copy(alpha = 0.05f)
                                )
                            )
                        }

                        val alignment = if (isMe) Alignment.End else Alignment.Start
                        val bubbleShape = if (isMe) {
                            RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp)
                        } else {
                            RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp)
                        }

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = alignment
                        ) {
                            if (!isMe) {
                                Text(
                                    text = displayName,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.5f),
                                    modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                                )
                            }
                            Surface(
                                modifier = Modifier
                                    .widthIn(max = 280.dp)
                                    .clip(bubbleShape)
                                    .border(1.dp, Color.White.copy(alpha = 0.05f), bubbleShape),
                                color = Color.Transparent,
                                shape = bubbleShape
                            ) {
                                Box(
                                    modifier = Modifier.background(bubbleGradient)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = reply.content ?: "",
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            lineHeight = 19.sp
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        val timestampText = try {
                                            val parts = reply.createdAt?.split("T")
                                            if (parts != null && parts.size > 1) {
                                                val timeParts = parts[1].split(":")
                                                if (timeParts.size > 1) {
                                                    "${timeParts[0]}:${timeParts[1]}"
                                                } else {
                                                    parts[0]
                                                }
                                            } else {
                                                reply.createdAt?.take(10) ?: ""
                                            }
                                        } catch (e: Exception) {
                                            ""
                                        }
                                        Text(
                                            text = timestampText,
                                            color = Color.White.copy(alpha = 0.5f),
                                            fontSize = 9.sp,
                                            modifier = Modifier.align(Alignment.End)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
