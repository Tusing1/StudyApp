package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.StudygramViewModel
import com.example.data.UserProfile
import com.example.ui.AppScreen
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyBuddiesScreen(
    viewModel: StudygramViewModel,
    modifier: Modifier = Modifier
) {
    var activeTab by remember { mutableStateOf("discover") } // discover, matches
    val potentialBuddies = viewModel.potentialBuddies
    val matchedBuddies = viewModel.matchedBuddies
    
    val currentBuddy = potentialBuddies.firstOrNull()

    var showMatchCelebration by remember { mutableStateOf<UserProfile?>(null) }
    var showUnlockDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Study Buddies", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(AppScreen.Channels) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White.copy(alpha = 0.02f)
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.25f))
                .padding(paddingValues)
        ) {
            // Ambient Atmosphere
            Box(
                modifier = Modifier
                    .size(250.dp)
                    .blur(80.dp)
                    .align(Alignment.TopStart)
                    .background(Color(0xFFEC407A).copy(alpha = 0.04f), CircleShape)
            )
            Box(
                modifier = Modifier
                    .size(250.dp)
                    .blur(80.dp)
                    .align(Alignment.BottomEnd)
                    .background(Color(0xFFAB47BC).copy(alpha = 0.04f), CircleShape)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Tab Selection Pill
                Row(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.White.copy(alpha = 0.03f))
                        .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(24.dp))
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (activeTab == "discover") MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { activeTab = "discover" }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Discover",
                            color = if (activeTab == "discover") Color.White else Color.White.copy(alpha = 0.4f),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (activeTab == "matches") MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { activeTab = "matches" }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Matches",
                                color = if (activeTab == "matches") Color.White else Color.White.copy(alpha = 0.4f),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            if (matchedBuddies.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFEC407A)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = matchedBuddies.size.toString(),
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (activeTab == "discover") {
                    if (currentBuddy != null) {
                        // Swipe Card Layout
                        var offsetX by remember { mutableStateOf(0f) }
                        var offsetY by remember { mutableStateOf(0f) }
                        val scope = rememberCoroutineScope()

                        val rotation = offsetX / 12f
                        val scale = (1f - (kotlin.math.abs(offsetX) / 1000f)).coerceIn(0.9f, 1f)

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(0.9f)
                                .graphicsLayer(
                                    translationX = offsetX,
                                    translationY = offsetY,
                                    rotationZ = rotation,
                                    scaleX = scale,
                                    scaleY = scale
                                )
                                .clip(RoundedCornerShape(32.dp))
                                .background(Color(0xFF0F0F16))
                                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(32.dp))
                                .pointerInput(Unit) {
                                    detectDragGestures(
                                        onDragEnd = {
                                            if (offsetX > 250) {
                                                // SWIPE RIGHT (LIKE/MATCH)
                                                scope.launch {
                                                    offsetX = 600f
                                                    delay(200)
                                                    viewModel.matchWithBuddy(currentBuddy.id.toString(), true)
                                                    // Trigger celebration modal for demo/like compatibility
                                                    showMatchCelebration = currentBuddy
                                                    offsetX = 0f
                                                    offsetY = 0f
                                                }
                                            } else if (offsetX < -250) {
                                                // SWIPE LEFT (NOPE)
                                                scope.launch {
                                                    offsetX = -600f
                                                    delay(200)
                                                    viewModel.matchWithBuddy(currentBuddy.id.toString(), false)
                                                    offsetX = 0f
                                                    offsetY = 0f
                                                }
                                            } else {
                                                // SNAP BACK
                                                offsetX = 0f
                                                offsetY = 0f
                                            }
                                        },
                                        onDrag = { change, dragAmount ->
                                            offsetX += dragAmount.x
                                            offsetY += dragAmount.y
                                        }
                                    )
                                }
                        ) {
                            // Profile contents
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(64.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    Brush.linearGradient(
                                                        colors = listOf(
                                                            Color(currentBuddy.avatarColor),
                                                            Color(currentBuddy.avatarColor).copy(alpha = 0.5f)
                                                        )
                                                    )
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = currentBuddy.username.take(1).uppercase(),
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 24.sp
                                            )
                                        }

                                        // Online indicator
                                        Surface(
                                            color = Color.White.copy(alpha = 0.05f),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Text(
                                                text = "ACTIVE",
                                                color = Color.Green,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                letterSpacing = 0.5.sp
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(20.dp))

                                    Text(
                                        text = currentBuddy.username,
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = currentBuddy.nursingField,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Text(
                                        text = "Revision buddy specializing in nursing board subjects, calculations, and active deck reviews.",
                                        fontSize = 13.sp,
                                        color = Color.White.copy(alpha = 0.6f),
                                        lineHeight = 18.sp
                                    )
                                }

                                // Specialty Pills
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = "FOCUS INTERESTS",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White.copy(alpha = 0.4f),
                                        letterSpacing = 1.sp
                                    )
                                    
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        listOf("Pharmacology", "Calculations", "Midwifery").forEach { tag ->
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(Color.White.copy(alpha = 0.05f))
                                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                                            ) {
                                                Text(tag, color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }
                            }

                            // Swipe Left/Right Text Stamps overlays
                            if (offsetX > 40) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(24.dp)
                                        .border(2.dp, Color.Green, RoundedCornerShape(8.dp))
                                        .background(Color.Green.copy(alpha = 0.1f))
                                        .padding(horizontal = 12.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "LIKE",
                                        color = Color.Green,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            if (offsetX < -40) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(24.dp)
                                        .border(2.dp, Color(0xFFEC407A), RoundedCornerShape(8.dp))
                                        .background(Color(0xFFEC407A).copy(alpha = 0.1f))
                                        .padding(horizontal = 12.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "NOPE",
                                        color = Color(0xFFEC407A),
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Controls Trio Row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(24.dp),
                            modifier = Modifier.padding(bottom = 16.dp)
                        ) {
                            // Nope button
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        offsetX = -400f
                                        delay(150)
                                        viewModel.matchWithBuddy(currentBuddy.id.toString(), false)
                                        offsetX = 0f
                                    }
                                },
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.05f))
                                    .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Nope", tint = Color(0xFFEC407A))
                            }

                            // Like button
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        offsetX = 400f
                                        delay(150)
                                        viewModel.matchWithBuddy(currentBuddy.id.toString(), true)
                                        showMatchCelebration = currentBuddy
                                        offsetX = 0f
                                    }
                                },
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                                    .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                            ) {
                                Icon(Icons.Default.Favorite, contentDescription = "Like", tint = Color.White)
                            }
                        }
                    } else {
                        // End of cards
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.03f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("✨", fontSize = 44.sp)
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                text = "End of the line",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 18.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "You've swiped on all potential revision buddies! Check back later.",
                                color = Color.White.copy(alpha = 0.4f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(0.7f),
                                fontSize = 13.sp
                            )
                        }
                    }
                } else {
                    // Matches Tab
                    if (matchedBuddies.isNotEmpty()) {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(matchedBuddies) { buddy ->
                                Surface(
                                    color = Color.White.copy(alpha = 0.02f),
                                    shape = RoundedCornerShape(16.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
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
                                                    .size(44.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(buddy.avatarColor)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = buddy.username.take(1).uppercase(),
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(
                                                    text = buddy.username,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                )
                                                Text(
                                                    text = buddy.nursingField,
                                                    color = Color.White.copy(alpha = 0.4f),
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }

                                        IconButton(
                                            onClick = {
                                                // Direct Message trigger
                                                viewModel.navigateTo(AppScreen.Channels)
                                            }
                                        ) {
                                            Text("💬", fontSize = 18.sp)
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("❤️", fontSize = 36.sp)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No matches yet",
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Mutual likes will create matches, enabling direct messaging revision chats.",
                                color = Color.White.copy(alpha = 0.4f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(0.7f),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // Match Celebration Modal Overlay
            if (showMatchCelebration != null) {
                val buddy = showMatchCelebration!!
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.85f))
                        .clickable { showMatchCelebration = null },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Text("🎉", fontSize = 64.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "IT'S A STUDY MATCH!",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "You and ${buddy.username} have liked each other. You can now collaborate and revise together!",
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            fontSize = 14.sp
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        Button(
                            onClick = {
                                showMatchCelebration = null
                                viewModel.navigateTo(AppScreen.Channels)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier.fillMaxWidth(0.8f).height(48.dp)
                        ) {
                            Text("Send Message", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        TextButton(onClick = { showMatchCelebration = null }) {
                            Text("Keep Swiping", color = Color.White.copy(alpha = 0.6f))
                        }
                    }
                }
            }
        }
    }
}
