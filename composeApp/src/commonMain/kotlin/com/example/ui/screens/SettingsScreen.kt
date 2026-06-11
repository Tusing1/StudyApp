package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.StudygramViewModel

val AVATAR_COLORS = listOf(
    0xFF00796B, // Teal
    0xFFC2185B, // Pink
    0xFF7B1FA2, // Purple
    0xFF1976D2, // Blue
    0xFF388E3C, // Green
    0xFFE64A19  // Orange
)

val SPECIALTIES = listOf(
    "General Nursing",
    "Midwifery",
    "Pediatrics",
    "Education",
    "Cardiology",
    "Pharmacology"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: StudygramViewModel,
    modifier: Modifier = Modifier
) {
    val userProfile by viewModel.userProfile.collectAsState()
    
    // Edit states
    var editUsername by remember { mutableStateOf("") }
    var editSpecialty by remember { mutableStateOf("") }
    var editColor by remember { mutableStateOf(0xFF00796B.toInt()) }
    var geminiKeyInput by remember { mutableStateOf("") }
    var notificationsOn by remember { mutableStateOf(true) }
    
    var showSpecialtyDropdown by remember { mutableStateOf(false) }
    var isInitialized by remember { mutableStateOf(false) }

    LaunchedEffect(userProfile) {
        userProfile?.let {
            if (!isInitialized) {
                editUsername = it.username
                editSpecialty = it.nursingField
                editColor = it.avatarColor
                geminiKeyInput = it.geminiApiKey
                notificationsOn = it.notificationsEnabled
                isInitialized = true
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold, color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
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
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Profile Avatar Details Card
                item {
                    Surface(
                        color = Color.White.copy(alpha = 0.02f),
                        shape = RoundedCornerShape(24.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape)
                                    .background(Color(editColor)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = editUsername.take(1).uppercase(),
                                    color = Color.White,
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Username Input
                            OutlinedTextField(
                                value = editUsername,
                                onValueChange = { editUsername = it },
                                label = { Text("Username") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // Specialty Dropdown field
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = editSpecialty,
                                    onValueChange = {},
                                    label = { Text("Specialty Field") },
                                    readOnly = true,
                                    modifier = Modifier.fillMaxWidth().clickable { showSpecialtyDropdown = true },
                                    trailingIcon = {
                                        IconButton(onClick = { showSpecialtyDropdown = true }) {
                                            Icon(Icons.Default.ArrowDropDown, contentDescription = "Select", tint = Color.White)
                                        }
                                    },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    )
                                )
                                
                                DropdownMenu(
                                    expanded = showSpecialtyDropdown,
                                    onDismissRequest = { showSpecialtyDropdown = false }
                                ) {
                                    SPECIALTIES.forEach { specialty ->
                                        DropdownMenuItem(
                                            text = { Text(specialty) },
                                            onClick = {
                                                editSpecialty = specialty
                                                showSpecialtyDropdown = false
                                            }
                                        )
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Avatar Color Picker
                            Text(
                                "Avatar Color Theme",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.align(Alignment.Start)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(AVATAR_COLORS) { colorHex ->
                                    val isSelected = editColor == colorHex.toInt()
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(Color(colorHex))
                                            .border(
                                                width = if (isSelected) 3.dp else 0.dp,
                                                color = if (isSelected) Color.White else Color.Transparent,
                                                shape = CircleShape
                                            )
                                            .clickable { editColor = colorHex.toInt() }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))
                            
                            Button(
                                onClick = {
                                    viewModel.updateProfileDetails(editUsername, editSpecialty, editColor)
                                },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text("Save Profile Changes", fontWeight = FontWeight.Bold, color = Color.Black)
                            }
                        }
                    }
                }

                // Notifications Toggle Card
                item {
                    Surface(
                        color = Color.White.copy(alpha = 0.02f),
                        shape = RoundedCornerShape(20.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Sound Notifications", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                                    Text("Play sound on new messages", color = Color.White.copy(alpha = 0.4f), fontSize = 10.sp)
                                }
                            }
                            Switch(
                                checked = notificationsOn,
                                onCheckedChange = {
                                    notificationsOn = it
                                    viewModel.updateNotificationsEnabled(it)
                                }
                            )
                        }
                    }
                }

                // AI Secrets / Custom Key Card
                item {
                    Surface(
                        color = Color.White.copy(alpha = 0.02f),
                        shape = RoundedCornerShape(20.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = Color.Cyan)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Gemini API Configuration", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                                    Text("Configure custom clinical assistant key", color = Color.White.copy(alpha = 0.4f), fontSize = 10.sp)
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = geminiKeyInput,
                                    onValueChange = { geminiKeyInput = it },
                                    placeholder = { Text("Paste API Key here...", color = Color.White.copy(alpha = 0.4f)) },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    )
                                )
                                Button(
                                    onClick = {
                                        viewModel.updateGeminiApiKey(geminiKeyInput)
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.height(56.dp)
                                ) {
                                    Text("Apply", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Logout Button
                item {
                    Button(
                        onClick = { viewModel.logout() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f), contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.2f))
                    ) {
                        Text("Log Out Session", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
