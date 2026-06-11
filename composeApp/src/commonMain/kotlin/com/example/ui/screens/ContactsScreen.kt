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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.AppScreen
import com.example.ui.StudygramViewModel
import com.example.data.UserProfile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen(viewModel: StudygramViewModel) {
    var activeTab by remember { mutableStateOf(viewModel.contactsInitialTab) } // friends, requests, referrals
    DisposableEffect(Unit) {
        onDispose {
            viewModel.contactsInitialTab = "friends"
        }
    }
    var searchQuery by remember { mutableStateOf("") }
    
    val matchedBuddies = viewModel.matchedBuddies
    val potentialBuddies = viewModel.potentialBuddies
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    // Mock pending requests list
    val pendingRequests = remember {
        mutableStateListOf<UserProfile>(
            UserProfile(id = "req_1", username = "Kiiza Simon", nursingField = "Education Mentor", avatarColor = 0xFF388E3C.toInt()),
            UserProfile(id = "req_2", username = "Nakitende Grace", nursingField = "Surgical Nurse", avatarColor = 0xFF00796B.toInt())
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Contacts & Search", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(AppScreen.Channels) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White.copy(alpha = 0.02f))
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.25f))
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Global Search Input
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search @username or name...", color = Color.White.copy(alpha = 0.4f)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.White.copy(alpha = 0.4f)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.1f)
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Custom navigation tabs pill row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.03f))
                        .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                        .padding(4.dp)
                ) {
                    val tabs = listOf("friends" to "Friends", "requests" to "Requests (${pendingRequests.size})", "referrals" to "Referrals")
                    tabs.forEach { (tabId, label) ->
                        val isSelected = activeTab == tabId
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .clickable { activeTab = tabId }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) Color.Black else Color.White.copy(alpha = 0.6f),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Tab Content List
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    if (searchQuery.isNotEmpty()) {
                        // Global search query results view
                        val searchResults = (matchedBuddies + potentialBuddies + pendingRequests).filter {
                            it.username.contains(searchQuery, ignoreCase = true) ||
                            it.nursingField.contains(searchQuery, ignoreCase = true)
                        }.distinctBy { it.id }

                        if (searchResults.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No peer matching '$searchQuery' found.", color = Color.White.copy(alpha = 0.4f))
                            }
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(searchResults) { peer ->
                                    val isFriend = matchedBuddies.any { it.id == peer.id }
                                    val isPending = pendingRequests.any { it.id == peer.id }
                                    
                                    Surface(
                                        color = Color.White.copy(alpha = 0.02f),
                                        shape = RoundedCornerShape(16.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(40.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(peer.avatarColor)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(peer.username.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
                                                }
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Column {
                                                    Text(peer.username, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                                                    Text(peer.nursingField, color = Color.White.copy(alpha = 0.4f), fontSize = 10.sp)
                                                }
                                            }

                                            when {
                                                isFriend -> {
                                                    Button(
                                                        onClick = { viewModel.navigateTo(AppScreen.Channels) },
                                                        shape = RoundedCornerShape(12.dp)
                                                    ) {
                                                        Text("Message", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                                isPending -> {
                                                    Text(
                                                        "In Requests",
                                                        color = MaterialTheme.colorScheme.primary,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(end = 8.dp)
                                                    )
                                                }
                                                else -> {
                                                    Button(
                                                        onClick = {
                                                            viewModel.matchWithBuddy(peer.id, true)
                                                        },
                                                        shape = RoundedCornerShape(12.dp)
                                                    ) {
                                                        Text("Add", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Standard tab content
                        when (activeTab) {
                            "friends" -> {
                                if (matchedBuddies.isEmpty()) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text("No matched study buddies yet. Start swiping in Discover!", color = Color.White.copy(alpha = 0.4f), textAlign = TextAlign.Center)
                                    }
                                } else {
                                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        items(matchedBuddies) { friend ->
                                            Surface(
                                                color = Color.White.copy(alpha = 0.02f),
                                                shape = RoundedCornerShape(16.dp),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(40.dp)
                                                                .clip(CircleShape)
                                                                .background(Color(friend.avatarColor)),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Text(friend.username.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
                                                        }
                                                        Spacer(modifier = Modifier.width(12.dp))
                                                        Column {
                                                            Text(friend.username, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                                                            Text(friend.nursingField, color = Color.White.copy(alpha = 0.4f), fontSize = 10.sp)
                                                        }
                                                    }

                                                    Button(
                                                        onClick = { viewModel.navigateTo(AppScreen.Channels) },
                                                        shape = RoundedCornerShape(12.dp)
                                                    ) {
                                                        Text("Message", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            "requests" -> {
                                if (pendingRequests.isEmpty()) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text("No pending friend requests.", color = Color.White.copy(alpha = 0.4f))
                                    }
                                } else {
                                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        items(pendingRequests) { req ->
                                            Surface(
                                                color = Color.White.copy(alpha = 0.02f),
                                                shape = RoundedCornerShape(16.dp),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(40.dp)
                                                                .clip(CircleShape)
                                                                .background(Color(req.avatarColor)),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Text(req.username.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
                                                        }
                                                        Spacer(modifier = Modifier.width(12.dp))
                                                        Column {
                                                            Text(req.username, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                                                            Text(req.nursingField, color = Color.White.copy(alpha = 0.4f), fontSize = 10.sp)
                                                        }
                                                    }

                                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                        IconButton(
                                                            onClick = {
                                                                pendingRequests.remove(req)
                                                            },
                                                            modifier = Modifier
                                                                .size(36.dp)
                                                                .clip(CircleShape)
                                                                .background(Color.White.copy(alpha = 0.05f))
                                                        ) {
                                                            Icon(Icons.Default.Clear, contentDescription = "Decline", tint = Color(0xFFE57373), modifier = Modifier.size(16.dp))
                                                        }

                                                        IconButton(
                                                            onClick = {
                                                                pendingRequests.remove(req)
                                                                matchedBuddies.add(req)
                                                                viewModel.addStudyTokens(25, "Accepted peer request")
                                                            },
                                                            modifier = Modifier
                                                                .size(36.dp)
                                                                .clip(CircleShape)
                                                                .background(MaterialTheme.colorScheme.primary)
                                                        ) {
                                                            Icon(Icons.Default.Check, contentDescription = "Accept", tint = Color.Black, modifier = Modifier.size(16.dp))
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            "referrals" -> {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text("🎁", fontSize = 48.sp)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text("Invite Peer Friends & Earn", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 18.sp)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "Earn +75 Study Tokens immediately when your classmate joins using your referral code.",
                                        color = Color.White.copy(alpha = 0.5f),
                                        fontSize = 12.sp,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 18.sp
                                    )

                                    Spacer(modifier = Modifier.height(32.dp))

                                    Surface(
                                        color = Color.White.copy(alpha = 0.02f),
                                        shape = RoundedCornerShape(16.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                                        modifier = Modifier.fillMaxWidth(0.9f)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(20.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text("YOUR REFERRAL CODE", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                "NURSE-SHARE-9817",
                                                fontWeight = FontWeight.Black,
                                                fontSize = 20.sp,
                                                color = Color.White,
                                                letterSpacing = 1.sp
                                            )
                                            Spacer(modifier = Modifier.height(20.dp))
                                            Button(
                                                onClick = {
                                                    clipboardManager.setText(AnnotatedString("NURSE-SHARE-9817"))
                                                },
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(16.dp)
                                            ) {
                                                Icon(Icons.Default.Share, contentDescription = "Copy", modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("Copy Referral Code", fontWeight = FontWeight.Bold, color = Color.Black)
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
    }
}
