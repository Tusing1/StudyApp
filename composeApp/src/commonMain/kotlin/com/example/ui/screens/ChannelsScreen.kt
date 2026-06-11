package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.AppScreen
import com.example.ui.StudygramViewModel
import com.example.ui.theme.*
import com.example.ui.components.CreateChannelDialog
import com.example.ui.components.CreateGroupDialog
import com.example.ui.components.SidebarContent
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelsScreen(
    viewModel: StudygramViewModel,
    userProfile: UserProfile?
) {
    val coroutineScope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val conversations = viewModel.conversations
    val archivedConversations = viewModel.archivedConversations
    val conversationsLoading = viewModel.conversationsLoading
    
    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(0) } // 0 = Chats, 1 = Archived

    var showCreateGroup by remember { mutableStateOf(false) }
    var showCreateChannel by remember { mutableStateOf(false) }
    var showInviteDialog by remember { mutableStateOf(false) }
    var showPermissionsDialog by remember { mutableStateOf(false) }

    // Fetch conversation updates
    LaunchedEffect(Unit) {
        viewModel.fetchChannels()
        val needsMic = !com.example.data.isPermissionGranted("microphone")
        val needsCamera = !com.example.data.isPermissionGranted("camera")
        if (needsMic || needsCamera) {
            showPermissionsDialog = true
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color(0xFF0A0A0F),
                modifier = Modifier.width(280.dp)
            ) {
                SidebarContent(
                    viewModel = viewModel,
                    onClose = {
                        coroutineScope.launch { drawerState.close() }
                    },
                    onNewGroupClick = {
                        showCreateGroup = true
                    },
                    onNewChannelClick = {
                        showCreateChannel = true
                    },
                    onInviteFriendsClick = {
                        showInviteDialog = true
                    },
                    onPermissionsClick = {
                        showPermissionsDialog = true
                    }
                )
            }
        }
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent
        ) { paddingValues ->
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                // Background ambient glow (Glassmorphism overlay)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.25f))
                ) {
                    // Accent glows
                    Box(
                        modifier = Modifier
                            .size(300.dp)
                            .align(Alignment.TopStart)
                            .blur(80.dp)
                            .background(Color(0xFF9C27B0).copy(alpha = 0.08f), CircleShape)
                    )
                    Box(
                        modifier = Modifier
                            .size(250.dp)
                            .align(Alignment.BottomEnd)
                            .blur(80.dp)
                            .background(Color(0xFF00BCD4).copy(alpha = 0.08f), CircleShape)
                    )
                }

                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Header Area
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White.copy(alpha = 0.02f))
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Menu Button
                            IconButton(
                                onClick = {
                                    coroutineScope.launch { drawerState.open() }
                                },
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = Color.White.copy(alpha = 0.06f)
                                )
                            ) {
                                Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.White)
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // Glassmorphic Search Bar
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.06f))
                                    .padding(horizontal = 12.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "Search",
                                        tint = Color.White.copy(alpha = 0.4f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    BasicTextField(
                                        value = searchQuery,
                                        onValueChange = { searchQuery = it },
                                        textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        decorationBox = { innerTextField ->
                                            if (searchQuery.isEmpty()) {
                                                Text(
                                                    text = "Search chats...",
                                                    color = Color.White.copy(alpha = 0.3f),
                                                    fontSize = 14.sp
                                                )
                                            }
                                            innerTextField()
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Tab Switcher (Chats vs Archived)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val activeCount = conversations.size
                            val archivedCount = archivedConversations.size

                            com.example.ui.components.NeonPill(
                                text = "Chats ($activeCount)",
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 }
                            )

                            com.example.ui.components.NeonPill(
                                text = "Archived ($archivedCount)",
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 }
                            )
                        }
                    }

                    // Conversation List Content
                    val listToShow = if (selectedTab == 0) conversations else archivedConversations
                    val filteredList = listToShow.filter { item ->
                        val name = when (item.conversation.type) {
                            "group", "channel" -> item.conversation.name ?: ""
                            else -> {
                                val other = item.participants.find { it.participant.userId != viewModel.remoteProfile?.userId }
                                other?.profile?.fullName ?: other?.profile?.username ?: ""
                            }
                        }
                        name.contains(searchQuery, ignoreCase = true)
                    }

                    if (conversationsLoading && filteredList.isEmpty()) {
                        Box(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    } else if (filteredList.isEmpty()) {
                        Box(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "No conversations found",
                                    color = Color.White.copy(alpha = 0.4f),
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = { showCreateGroup = true },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Text("Create a Group", color = Color.White)
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            contentPadding = PaddingValues(vertical = 12.dp)
                        ) {
                            items(filteredList) { item ->
                                ConversationListItem(
                                    item = item,
                                    myUid = viewModel.remoteProfile?.userId ?: "",
                                    onClick = {
                                        viewModel.navigateTo(AppScreen.DiscussionRoom(item.conversation))
                                    },
                                    onArchive = {
                                        if (selectedTab == 0) {
                                            viewModel.archiveConversation(item.conversation.id)
                                        } else {
                                            viewModel.unarchiveConversation(item.conversation.id)
                                        }
                                    },
                                    onDelete = {
                                        viewModel.deleteConversation(item.conversation.id)
                                    },
                                    viewModel = viewModel
                                )
                            }
                        }
                    }
                }

                // Floating Action Button (FAB) Area
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(20.dp)
                ) {
                    FloatingActionButton(
                        onClick = { showCreateGroup = true },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White,
                        modifier = Modifier.size(56.dp),
                        shape = CircleShape,
                        elevation = FloatingActionButtonDefaults.elevation(8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "New Group/Chat")
                    }
                }
            }
        }

        if (showCreateGroup) {
            CreateGroupDialog(
                onDismiss = { showCreateGroup = false },
                onCreateGroup = { name, description, memberIds ->
                    viewModel.createGroup(name, description, memberIds) { convId ->
                        showCreateGroup = false
                    }
                }
            )
        }

        if (showCreateChannel) {
            CreateChannelDialog(
                onDismiss = { showCreateChannel = false },
                onCreateChannel = { name, description, enableDiscussion ->
                    viewModel.createChannel(name, description, enableDiscussion) { convId ->
                        showCreateChannel = false
                    }
                }
            )
        }

        if (showInviteDialog) {
            val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
            AlertDialog(
                onDismissRequest = { showInviteDialog = false },
                title = { Text("Invite Friends", color = Color.White, fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text("Share this link to invite your peers to Studygram!", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = "https://studdybuddyapp.com/invite",
                            onValueChange = {},
                            readOnly = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = Color(0xFF161622),
                                unfocusedContainerColor = Color(0xFF161622),
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            clipboardManager.setText(androidx.compose.ui.text.AnnotatedString("https://studdybuddyapp.com/invite"))
                            showInviteDialog = false
                        }
                    ) {
                        Text("Copy Link")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showInviteDialog = false }) {
                        Text("Close", color = Color.White.copy(alpha = 0.6f))
                    }
                },
                containerColor = Color(0xFF161622)
            )
        }

        if (showPermissionsDialog) {
            PermissionsDialog(
                onDismiss = { showPermissionsDialog = false }
            )
        }
    }
}

@Composable
fun PermissionsDialog(onDismiss: () -> Unit) {
    var micGranted by remember { mutableStateOf(com.example.data.isPermissionGranted("microphone")) }
    var cameraGranted by remember { mutableStateOf(com.example.data.isPermissionGranted("camera")) }
    var storageGranted by remember { mutableStateOf(com.example.data.isPermissionGranted("storage")) }

    // Check status periodically when active
    LaunchedEffect(Unit) {
        while (true) {
            micGranted = com.example.data.isPermissionGranted("microphone")
            cameraGranted = com.example.data.isPermissionGranted("camera")
            storageGranted = com.example.data.isPermissionGranted("storage")
            kotlinx.coroutines.delay(1000)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🔑", fontSize = 24.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("App Permissions", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "Grant these permissions to unlock the full potential of Studygram (voice recording, document reading, and session calls).",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 13.sp
                )
                
                PermissionRow(
                    title = "Microphone",
                    description = "Required for recording voice notes and group calls.",
                    granted = micGranted,
                    onRequest = {
                        com.example.data.requestPermission("microphone") { granted ->
                            micGranted = granted
                        }
                    }
                )
                
                PermissionRow(
                    title = "Camera",
                    description = "Required for video meetings and avatar configuration.",
                    granted = cameraGranted,
                    onRequest = {
                        com.example.data.requestPermission("camera") { granted ->
                            cameraGranted = granted
                        }
                    }
                )
                
                PermissionRow(
                    title = "Storage",
                    description = "Required for viewing PDF study guides and uploading files.",
                    granted = storageGranted,
                    onRequest = {
                        com.example.data.requestPermission("storage") { granted ->
                            storageGranted = granted
                        }
                    }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Done", color = Color.White)
            }
        },
        containerColor = Color(0xFF161622)
    )
}

@Composable
fun PermissionRow(
    title: String,
    description: String,
    granted: Boolean,
    onRequest: () -> Unit
) {
    Surface(
        color = Color.White.copy(alpha = 0.03f),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (granted) Color(0xFF4CAF50) else Color(0xFFEF5350))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(description, color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
            }
            
            Button(
                onClick = onRequest,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (granted) Color.White.copy(alpha = 0.1f) else MaterialTheme.colorScheme.primary
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text(
                    text = if (granted) "Granted" else "Grant",
                    color = if (granted) Color.White.copy(alpha = 0.6f) else Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun TabItem(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(40))
            .background(
                if (selected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.05f)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = title,
            color = if (selected) Color.White else Color.White.copy(alpha = 0.6f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun ConversationListItem(
    item: ConversationWithDetails,
    myUid: String,
    onClick: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
    viewModel: StudygramViewModel
) {
    val conv = item.conversation
    val lastMsg = item.lastMessage

    val otherParticipant = item.participants.find { it.participant.userId != myUid }
    
    val displayName = when (conv.type) {
        "group", "channel" -> conv.name ?: "Discussion"
        "saved" -> "Saved Messages"
        else -> otherParticipant?.profile?.fullName ?: otherParticipant?.profile?.username ?: "Direct Message"
    }

    val subtitle = when {
        lastMsg != null -> {
            val senderPrefix = if (lastMsg.senderId == myUid) "You: " else ""
            val bodyText = when (lastMsg.messageType) {
                "image" -> "📷 Photo"
                "file" -> "📎 File"
                else -> lastMsg.content ?: "Sent an attachment"
            }
            "$senderPrefix$bodyText"
        }
        conv.type == "group" -> "${item.participants.size} members"
        conv.type == "channel" -> "${conv.subscriberCount ?: 0} subscribers"
        else -> conv.description ?: "A direct study conversation"
    }

    val avatarColor = when (conv.type) {
        "group" -> 0xFF388E3C.toInt()
        "channel" -> 0xFF7B1FA2.toInt()
        "saved" -> 0xFF00796B.toInt()
        else -> otherParticipant?.profile?.interests?.hashCode() ?: 0xFF1976D2.toInt()
    }

    val timestampText = lastMsg?.createdAt?.let { dateStr ->
        // Format date string (e.g. 2026-06-07T11:00:53) to simple time or date
        try {
            val parts = dateStr.split("T")
            if (parts.size > 1) {
                val timeParts = parts[1].split(":")
                if (timeParts.size > 1) {
                    "${timeParts[0]}:${timeParts[1]}"
                } else {
                    parts[0]
                }
            } else {
                dateStr.take(10)
            }
        } catch (e: Exception) {
            ""
        }
    } ?: ""

    var expandedMenu by remember { mutableStateOf(false) }

    com.example.ui.components.GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("conversation_card_${conv.id}"),
        cornerRadius = 16.dp,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val avatarBorderModifier = if (conv.type == "direct" && (otherParticipant?.profile?.isOnline == true)) {
                Modifier.border(
                    BorderStroke(1.5.dp, Brush.horizontalGradient(listOf(NeonCyan, NeonPurple))),
                    CircleShape
                )
            } else {
                Modifier
            }

            // Avatar
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(avatarColor))
                    .then(avatarBorderModifier),
                contentAlignment = Alignment.Center
            ) {
                when (conv.type) {
                    "group" -> Icon(Icons.Default.Person, contentDescription = "Group", tint = Color.White)
                    "channel" -> Icon(Icons.Default.PlayArrow, contentDescription = "Channel", tint = Color.White)
                    "saved" -> Icon(Icons.Default.Favorite, contentDescription = "Saved Messages", tint = Color.White)
                    else -> {
                        Text(
                            text = displayName.take(1).uppercase(),
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                // Online indicator
                if (conv.type == "direct" && (otherParticipant?.profile?.isOnline == true)) {
                    com.example.ui.components.PulsingDot(
                        color = NeonGreen,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .border(2.dp, BgDeep, CircleShape)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Text Info
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = displayName,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = timestampText,
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 11.sp
                        )
                        
                        Box {
                            IconButton(
                                onClick = { expandedMenu = true },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Options",
                                    tint = Color.White.copy(alpha = 0.4f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            
                            DropdownMenu(
                                expanded = expandedMenu,
                                onDismissRequest = { expandedMenu = false },
                                modifier = Modifier.background(Color(0xFF161622))
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = if (conv.isArchived) "Unarchive" else "Archive",
                                            color = Color.White
                                        )
                                    },
                                    onClick = {
                                        expandedMenu = false
                                        onArchive()
                                    }
                                )
                                
                                val role = viewModel.getUserRole(conv.id)
                                val isAdminOrOwner = role == "admin" || role == "owner"
                                if (conv.type == "direct" || conv.type == "saved" || isAdminOrOwner) {
                                    DropdownMenuItem(
                                        text = { Text("Delete Chat", color = Color(0xFFEF5350)) },
                                        onClick = {
                                            expandedMenu = false
                                            onDelete()
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = subtitle,
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
