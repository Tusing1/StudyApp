package com.example.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SupabaseConversation
import com.example.data.SupabaseMessage
import com.example.ui.AppScreen
import com.example.ui.StudygramViewModel
import com.example.ui.theme.*
import coil3.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomScreen(
    viewModel: StudygramViewModel,
    channel: SupabaseConversation
) {
    val messages = viewModel.activeChannelMessages
    var inputMessage by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val myUid = viewModel.remoteProfile?.userId ?: ""
    val myRole = viewModel.getUserRole(channel.id)
    val isAdminOrOwner = myRole == "admin" || myRole == "owner"
    val isChannel = channel.type == "channel"
    val canSendMessages = !isChannel || isAdminOrOwner

    var showAiSheet by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel.aiExplanationText) {
        if (viewModel.aiExplanationText.isNotEmpty()) {
            showAiSheet = true
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

    // Resolve channel details
    val convDetails = viewModel.conversations.find { it.conversation.id == channel.id }
        ?: viewModel.archivedConversations.find { it.conversation.id == channel.id }
    
    val displayName = when (channel.type) {
        "group", "channel" -> channel.name ?: "Discussion"
        "saved" -> "Saved Messages"
        else -> {
            val otherParticipant = convDetails?.participants?.find { it.participant.userId != myUid }
            otherParticipant?.profile?.fullName ?: otherParticipant?.profile?.username ?: "Direct Message"
        }
    }

    val subtitleText = when (channel.type) {
        "group" -> "${convDetails?.participants?.size ?: 0} members"
        "channel" -> "${channel.subscriberCount ?: 0} subscribers"
        else -> {
            val otherParticipant = convDetails?.participants?.find { it.participant.userId != myUid }
            if (otherParticipant?.profile?.isOnline == true) "online" else "offline"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column(modifier = Modifier.clickable { viewModel.navigateTo(AppScreen.ChannelInfo(channel)) }) {
                        Text(
                            text = displayName,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = subtitleText,
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(AppScreen.Channels) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        com.example.data.checkAndRequestRecordPermission {
                            viewModel.callManager.startCall(
                                conversationId = channel.id,
                                currentUserId = myUid,
                                callType = "voice"
                            )
                            viewModel.navigateTo(AppScreen.Calls)
                        }
                    }) {
                        Text("📞", fontSize = 16.sp)
                    }
                    IconButton(onClick = { viewModel.navigateTo(AppScreen.ChannelInfo(channel)) }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More options", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White.copy(alpha = 0.02f)
                )
            )
        },
        bottomBar = {
            Surface(
                color = Color.White.copy(alpha = 0.02f),
                tonalElevation = 4.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
            ) {
                if (canSendMessages) {
                    var isRecording by remember { mutableStateOf(false) }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp)
                            .navigationBarsPadding(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!isRecording) {
                            IconButton(onClick = { 
                                // Simulate an image attachment for demonstration purposes
                                val dummyImageBytes = "mock_image_data".encodeToByteArray()
                                viewModel.uploadFileAndSendMessage(
                                    channelId = channel.id,
                                    text = inputMessage,
                                    fileBytes = dummyImageBytes,
                                    fileName = "test_image.png"
                                )
                                inputMessage = ""
                                focusManager.clearFocus()
                            }) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = "Attach",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }

                            OutlinedTextField(
                                value = inputMessage,
                                onValueChange = { inputMessage = it },
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 8.dp),
                                placeholder = { Text("Message", color = Color.White.copy(alpha = 0.4f)) },
                                shape = RoundedCornerShape(24.dp),
                                maxLines = 4,
                                textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color.White.copy(alpha = 0.15f),
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.08f),
                                    focusedContainerColor = Color.White.copy(alpha = 0.04f),
                                    unfocusedContainerColor = Color.White.copy(alpha = 0.04f)
                                ),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                keyboardActions = KeyboardActions(
                                    onSend = {
                                        if (inputMessage.isNotBlank()) {
                                            viewModel.sendDiscussionMessage(channel.id, inputMessage)
                                            inputMessage = ""
                                            focusManager.clearFocus()
                                        }
                                    }
                                )
                            )
                        }

                        if (inputMessage.isNotBlank() && !isRecording) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                IconButton(
                                    onClick = {
                                        viewModel.sendDiscussionMessage(channel.id, inputMessage)
                                        inputMessage = ""
                                        focusManager.clearFocus()
                                    }
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.Send,
                                        contentDescription = "Send",
                                        tint = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }
                        } else {
                            val recorderModifier = if (isRecording) Modifier.weight(1f) else Modifier
                            com.example.ui.components.VoiceRecorderComponent(
                                onRecordingComplete = { bytes, duration ->
                                    viewModel.uploadFileAndSendMessage(
                                        channelId = channel.id,
                                        text = "",
                                        fileBytes = bytes,
                                        fileName = "voice_msg_${System.currentTimeMillis()}.mp4",
                                        messageType = "voice"
                                    )
                                },
                                onRecordingStateChanged = { recording ->
                                    isRecording = recording
                                },
                                modifier = recorderModifier
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .navigationBarsPadding(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (channel.linkedDiscussionId != null) {
                            Button(
                                onClick = {
                                    val disc = viewModel.channels.find { it.id == channel.linkedDiscussionId }
                                    disc?.let { viewModel.navigateTo(AppScreen.DiscussionRoom(it)) }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary
                                )
                            ) {
                                Text("Open Discussion to Comment", color = Color.White)
                            }
                        } else {
                            Text(
                                text = "Only admins can post to this channel",
                                color = Color.White.copy(alpha = 0.4f),
                                fontSize = 13.sp
                            )
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
            // Ambient glows
            Box(
                modifier = Modifier
                    .size(250.dp)
                    .blur(70.dp)
                    .align(Alignment.TopEnd)
                    .background(Color(0xFF00E5FF).copy(alpha = 0.04f), CircleShape)
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                reverseLayout = true,
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                items(messages) { message ->
                    val isMe = message.senderId == myUid
                    MessageBubbleItem(
                        message = message,
                        isMe = isMe,
                        viewModel = viewModel,
                        convDetails = convDetails
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }
    }

    if (showAiSheet) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                .clickable {
                    showAiSheet = false
                    viewModel.aiExplanationText = ""
                }
        ) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .fillMaxHeight(0.65f)
                    .clickable(enabled = false) {},
                color = Color(0xFF0F0F1A),
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f))
                            .align(Alignment.CenterHorizontally)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🤖", fontSize = 24.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                "AI Clinical Mentor",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }

                        IconButton(
                            onClick = {
                                showAiSheet = false
                                viewModel.aiExplanationText = ""
                            }
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                    Spacer(modifier = Modifier.height(16.dp))

                    if (viewModel.isGeneratingAI) {
                        Column(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "AI Clinical Mentor is analyzing context...",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 13.sp
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f)
                        ) {
                            item {
                                Text(
                                    text = viewModel.aiExplanationText,
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 14.sp,
                                    lineHeight = 22.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                showAiSheet = false
                                viewModel.aiExplanationText = ""
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Text("Understood", fontWeight = FontWeight.Bold, color = Color.Black)
                        }
                    }
                }
            }
        }
    }
}
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubbleItem(
    message: SupabaseMessage,
    isMe: Boolean,
    viewModel: StudygramViewModel,
    convDetails: com.example.data.ConversationWithDetails?
) {
    val alignment = if (isMe) Alignment.End else Alignment.Start
    val bubbleShape = if (isMe) {
        RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp)
    } else {
        RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp)
    }

    val bubbleGradient = if (isMe) {
        Brush.linearGradient(
            colors = listOf(
                MaterialTheme.colorScheme.primary,
                MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.08f),
                Color.White.copy(alpha = 0.05f)
            )
        )
    }

    val textColor = Color.White
    
    // Resolve Sender Name
    val senderName = if (isMe) {
        "You"
    } else {
        val senderProfile = convDetails?.participants?.find { it.participant.userId == message.senderId }?.profile
        senderProfile?.fullName ?: senderProfile?.username ?: "User"
    }

    val timestampText = try {
        val parts = message.createdAt?.split("T")
        if (parts != null && parts.size > 1) {
            val timeParts = parts[1].split(":")
            if (timeParts.size > 1) {
                "${timeParts[0]}:${timeParts[1]}"
            } else {
                parts[0]
            }
        } else {
            message.createdAt?.take(10) ?: ""
        }
    } catch (e: Exception) {
        ""
    }

    var showMenu by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalAlignment = alignment
    ) {
        if (!isMe) {
            Text(
                text = senderName,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
            )
        }
        
        Box {
            Surface(
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .clip(bubbleShape)
                    .combinedClickable(
                        onClick = {
                            val isChan = convDetails?.conversation?.type == "channel"
                            val discId = convDetails?.conversation?.linkedDiscussionId
                            if (isChan && discId != null) {
                                viewModel.navigateTo(AppScreen.Thread(messageId = message.id ?: "", channelId = message.conversationId))
                            }
                        },
                        onLongClick = {
                            showMenu = true
                        }
                    ),
                color = Color.Transparent,
                shape = bubbleShape,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
            ) {
                Box(
                    modifier = Modifier.background(bubbleGradient)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        // File attachment rendering
                        if (!message.fileUrl.isNullOrBlank()) {
                            when (message.messageType) {
                                "image" -> {
                                    AsyncImage(
                                        model = message.fileUrl,
                                        contentDescription = "Attached Image",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(160.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .padding(bottom = 6.dp)
                                            .background(Color.DarkGray)
                                    )
                                }
                                "audio", "voice" -> {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 6.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.White.copy(alpha = 0.06f))
                                            .clickable { viewModel.navigateTo(AppScreen.AudioPlayer(message.fileUrl)) }
                                            .padding(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primary),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = Color.White, modifier = Modifier.size(16.dp))
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text("Voice Message", fontSize = 12.sp, color = textColor, fontWeight = FontWeight.Bold)
                                            Text("0:00", fontSize = 10.sp, color = textColor.copy(alpha = 0.6f))
                                        }
                                    }
                                }
                                else -> {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 6.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.White.copy(alpha = 0.06f))
                                            .clickable { viewModel.navigateTo(AppScreen.PdfViewer(message.fileUrl)) }
                                            .padding(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color(0xFFE53935)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.Info, contentDescription = "File", tint = Color.White, modifier = Modifier.size(16.dp))
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(message.fileName ?: "Document.pdf", fontSize = 12.sp, color = textColor, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text(if (message.fileSize != null) "${message.fileSize / 1024} KB" else "PDF", fontSize = 10.sp, color = textColor.copy(alpha = 0.6f))
                                        }
                                    }
                                }
                            }
                        }

                        if (!message.content.isNullOrBlank()) {
                            Text(
                                text = message.content ?: "",
                                color = textColor,
                                fontSize = 14.sp,
                                lineHeight = 19.sp
                            )
                        }

                        val isChannelMessage = convDetails?.conversation?.type == "channel"
                        val linkedDiscussionId = convDetails?.conversation?.linkedDiscussionId
                        if (isChannelMessage && linkedDiscussionId != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                            
                            val commentCount = viewModel.messageCommentCounts[message.id ?: ""] ?: 0
                            val commentLabel = if (commentCount > 0) {
                                if (commentCount == 1) "1 comment" else "$commentCount comments"
                            } else {
                                "Leave a comment"
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        message.id?.let { msgId ->
                                            viewModel.navigateTo(AppScreen.Thread(messageId = msgId, channelId = message.conversationId))
                                        }
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("💬", fontSize = 14.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = commentLabel,
                                        color = NeonCyan,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = "→",
                                    color = NeonCyan.copy(alpha = 0.7f),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        
                        // Time & Delivery ticks
                        Row(
                            modifier = Modifier.align(Alignment.End).padding(top = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = timestampText,
                                color = textColor.copy(alpha = 0.5f),
                                fontSize = 9.sp
                            )
                            if (isMe) {
                                Spacer(modifier = Modifier.width(4.dp))
                                if (message.isRead) {
                                    Text("✓✓", color = Color.White.copy(alpha = 0.8f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                } else {
                                    Text("✓", color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                modifier = Modifier.background(Color(0xFF161622))
            ) {
                DropdownMenuItem(
                    text = { Text("Explain with AI", color = Color.Cyan) },
                    onClick = {
                        showMenu = false
                        viewModel.askAiAboutPost(message)
                    }
                )
                DropdownMenuItem(
                    text = { Text("Copy Text", color = Color.White) },
                    onClick = {
                        showMenu = false
                        message.content?.let {
                            clipboardManager.setText(AnnotatedString(it))
                        }
                    }
                )
                if (isMe) {
                    DropdownMenuItem(
                        text = { Text("Delete Message", color = Color(0xFFEF5350)) },
                        onClick = {
                            showMenu = false
                            message.id?.let { viewModel.deleteMessage(it) }
                        }
                    )
                }
            }
        }
    }
}
