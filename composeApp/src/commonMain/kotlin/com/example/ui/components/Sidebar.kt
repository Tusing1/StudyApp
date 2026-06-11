package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.AppScreen
import com.example.ui.StudygramViewModel

@Composable
fun SidebarContent(
    viewModel: StudygramViewModel,
    onClose: () -> Unit,
    onNewGroupClick: () -> Unit = {},
    onNewChannelClick: () -> Unit = {},
    onInviteFriendsClick: () -> Unit = {},
    onPermissionsClick: () -> Unit = {}
) {
    val remoteProfile = viewModel.remoteProfile
    val localProfile by viewModel.userProfile.collectAsState()
    
    val displayName = remoteProfile?.fullName ?: remoteProfile?.username ?: localProfile?.username ?: "Nurses Revision"
    val username = remoteProfile?.username ?: localProfile?.username ?: "nursesrevision"
    val avatarColor = localProfile?.avatarColor ?: 0xFF00796B.toInt()
    val tokens = localProfile?.studyTokens ?: 95

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(280.dp)
            .background(Color(0xFF0F172A)) // Match premium dark theme background
            .verticalScroll(rememberScrollState())
    ) {
        // Header (Vibrant Blue Background)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1E88E5),
                            Color(0xFF1565C0)
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Avatar Circle with white background border
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .padding(2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(Color(avatarColor)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = displayName.take(1).uppercase(),
                                color = Color.White,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = displayName,
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "@$username",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        // Streak Info
                        Text(
                            text = "🔥 ${viewModel.loginStreak} day streak",
                            color = Color(0xFFFFB300),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Close Button
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.align(Alignment.Top)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Cards Grid Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Tokens Card (Yellow Highlighted border/text)
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                onClose()
                                viewModel.navigateTo(AppScreen.StudyTokens)
                            },
                        color = Color.White.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFD700).copy(alpha = 0.3f))
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(8.dp)
                        ) {
                            Text("🪙", fontSize = 18.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Tokens", color = Color.White.copy(alpha = 0.9f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text("$tokens", color = Color(0xFFFFD700), fontSize = 13.sp, fontWeight = FontWeight.Black)
                        }
                    }
                    
                    // Ask AI Card
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                onClose()
                                viewModel.navigateTo(AppScreen.AIChat)
                            },
                        color = Color.White.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(8.dp)
                        ) {
                            Text("🤖", fontSize = 18.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Ask AI", color = Color.White.copy(alpha = 0.9f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text("Tutor", color = Color(0xFF00E5FF), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    
                    // Search Card
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                onClose()
                                viewModel.navigateTo(AppScreen.Channels)
                            },
                        color = Color.White.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Search", color = Color.White.copy(alpha = 0.9f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text("Chats", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // StudyLabs promo banner
        Surface(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .fillMaxWidth()
                .clickable {
                    onClose()
                    viewModel.navigateTo(AppScreen.StudyLabs)
                },
            color = Color(0xFF2E1A47), // Premium Dark Purple
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE040FB).copy(alpha = 0.2f))
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF9C27B0), Color(0xFFE91E63))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🧪", fontSize = 18.sp)
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "StudyLabs",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "GAMES & REWARDS",
                        color = Color(0xFFE040FB),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                // NEW tag
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(40))
                        .background(Color(0xFFE91E63).copy(alpha = 0.2f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "NEW",
                        color = Color(0xFFFF4081),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        
        Divider(
            color = Color.White.copy(alpha = 0.08f),
            modifier = Modifier.padding(vertical = 8.dp)
        )
        
        // Menu Option List (Aligned to screenshot)
        SidebarMenuItem(icon = "📇", label = "Contacts") {
            onClose()
            viewModel.navigateTo(AppScreen.Contacts)
        }
        
        SidebarMenuItem(icon = "👥", label = "New Group") {
            onClose()
            onNewGroupClick()
        }
        
        SidebarMenuItem(icon = "📣", label = "New Channel") {
            onClose()
            onNewChannelClick()
        }
        
        SidebarMenuItem(icon = "✉️", label = "Invite Friends") {
            onClose()
            onInviteFriendsClick()
        }
        
        SidebarMenuItem(icon = "💖", label = "StudyBuddies") {
            onClose()
            viewModel.navigateTo(AppScreen.StudyBuddies)
        }
        
        SidebarMenuItem(icon = "💞", label = "Friend Requests") {
            onClose()
            viewModel.contactsInitialTab = "requests"
            viewModel.navigateTo(AppScreen.Contacts)
        }
        
        SidebarMenuItem(icon = "📞", label = "Calls") {
            onClose()
            viewModel.navigateTo(AppScreen.Calls)
        }
        
        SidebarMenuItem(icon = "🎤", label = "Recordings") {
            onClose()
            viewModel.navigateTo(AppScreen.Recordings)
        }

        SidebarMenuItem(icon = "🧠", label = "Quiz Blitz") {
            onClose()
            viewModel.navigateTo(AppScreen.QuizPractice)
        }

        SidebarMenuItem(icon = "🔖", label = "Saved Notes") {
            onClose()
            viewModel.navigateTo(AppScreen.Bookmarks)
        }
        
        Divider(
            color = Color.White.copy(alpha = 0.08f),
            modifier = Modifier.padding(vertical = 8.dp)
        )
        
        // Settings / Mode Option List
        SidebarMenuItem(icon = "👤", label = "My Profile") {
            onClose()
            viewModel.navigateTo(AppScreen.Settings)
        }
        
        SidebarMenuItem(icon = "🔑", label = "App Permissions") {
            onClose()
            onPermissionsClick()
        }
        
        SidebarMenuItem(icon = "☀️", label = "Light Mode") {
            // Can toggle light/dark in standard configuration
        }
        
        SidebarMenuItem(icon = "⚙️", label = "Settings") {
            onClose()
            viewModel.navigateTo(AppScreen.Settings)
        }
        
        Divider(
            color = Color.White.copy(alpha = 0.08f),
            modifier = Modifier.padding(vertical = 8.dp)
        )
        
        // Sign Out (Vibrant Red)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    onClose()
                    viewModel.logout()
                }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.ExitToApp,
                contentDescription = "Sign Out",
                tint = Color(0xFFEF5350)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "Sign Out",
                color = Color(0xFFEF5350),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun SidebarMenuItem(
    icon: String,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = icon, fontSize = 20.sp)
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
