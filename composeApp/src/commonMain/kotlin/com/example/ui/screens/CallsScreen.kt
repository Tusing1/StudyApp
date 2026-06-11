package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.StudygramViewModel
import com.example.data.CallState
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallsScreen(viewModel: StudygramViewModel, modifier: Modifier = Modifier) {
    val callState by viewModel.callManager.callState.collectAsState()
    val isMuted by viewModel.callManager.isMuted.collectAsState()
    val isCameraOff by viewModel.callManager.isCameraOff.collectAsState()
    val isRecording by viewModel.callManager.isRecording.collectAsState()

    var showRecordDialog by remember { mutableStateOf(false) }
    var recordingTitle by remember { mutableStateOf("") }
    
    // Call duration timer
    var callDurationSeconds by remember { mutableStateOf(0) }
    LaunchedEffect(callState) {
        if (callState == CallState.CONNECTED) {
            callDurationSeconds = 0
            while (true) {
                delay(1000)
                callDurationSeconds++
            }
        }
    }

    if (callState != CallState.IDLE) {
        // FULL SCREEN ONGOING CALL OVERLAY
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF07070C)),
            contentAlignment = Alignment.Center
        ) {
            // Ambient glow
            Box(
                modifier = Modifier
                    .size(250.dp)
                    .blur(70.dp)
                    .align(Alignment.TopCenter)
                    .background(Color(0xFF00E5FF).copy(alpha = 0.05f), CircleShape)
            )
            Box(
                modifier = Modifier
                    .size(250.dp)
                    .blur(70.dp)
                    .align(Alignment.BottomCenter)
                    .background(Color(0xFFD500F9).copy(alpha = 0.04f), CircleShape)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Header info
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 32.dp)
                ) {
                    Surface(
                        color = Color.White.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (callState == CallState.CONNECTED) Color.Green else Color.Yellow
                                    )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = when (callState) {
                                    CallState.RINGING -> "RINGING"
                                    CallState.CONNECTING -> "CONNECTING..."
                                    else -> "SECURE VOICE CALL"
                                },
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.7f),
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    if (isRecording) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            color = Color(0xFFEF5350).copy(alpha = 0.15f),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF5350).copy(alpha = 0.25f))
                        ) {
                            Text(
                                text = "REC ACTIVE",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFEF5350),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }

                // Center Caller Info & Avatar
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(160.dp)
                    ) {
                        // Pulsing background ring when speaking or active
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
                                .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), CircleShape)
                        )
                        Box(
                            modifier = Modifier
                                .size(130.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.03f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "📞",
                                fontSize = 54.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "Ongoing Session",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    if (callState == CallState.CONNECTED) {
                        val minutes = callDurationSeconds / 60
                        val seconds = callDurationSeconds % 60
                        val durStr = "$minutes:${seconds.toString().padStart(2, '0')}"
                        Text(
                            text = durStr,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    } else {
                        Text(
                            text = "Awaiting connection...",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.4f)
                        )
                    }
                }

                // Bottom Floating Controls Card
                Surface(
                    color = Color.White.copy(alpha = 0.04f),
                    shape = RoundedCornerShape(32.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                    modifier = Modifier.padding(bottom = 32.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        // Mic Mute Control
                        IconButton(
                            onClick = { viewModel.callManager.toggleMute() },
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isMuted) Color(0xFFEF5350).copy(alpha = 0.15f) else Color.White.copy(alpha = 0.08f)
                                )
                        ) {
                            Text(if (isMuted) "🔇" else "🎙️", fontSize = 18.sp)
                        }

                        // Call Recording Toggle
                        IconButton(
                            onClick = {
                                if (isRecording) {
                                    viewModel.callManager.toggleRecording("")
                                } else {
                                    showRecordDialog = true
                                }
                            },
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isRecording) Color(0xFFEF5350) else Color.White.copy(alpha = 0.08f)
                                )
                        ) {
                            Text("⏺️", fontSize = 16.sp)
                        }

                        // Hangup Button
                        IconButton(
                            onClick = { viewModel.callManager.endCall() },
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE53935))
                        ) {
                            Text("❌", fontSize = 20.sp)
                        }
                    }
                }
            }

            // Dialog for Recording title entry
            if (showRecordDialog) {
                AlertDialog(
                    onDismissRequest = { showRecordDialog = false },
                    title = { Text("Start Session Recording", color = Color.White) },
                    text = {
                        Column {
                            Text("Input a name/title for this call recording to archive.", color = Color.White.copy(alpha = 0.7f))
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = recordingTitle,
                                onValueChange = { recordingTitle = it },
                                placeholder = { Text("e.g. Clinical Review AMTSL", color = Color.White.copy(alpha = 0.4f)) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedContainerColor = Color(0xFF161622),
                                    unfocusedContainerColor = Color(0xFF161622)
                                )
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.callManager.toggleRecording(recordingTitle)
                                showRecordDialog = false
                            }
                        ) {
                            Text("Record")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showRecordDialog = false }) {
                            Text("Cancel")
                        }
                    },
                    containerColor = Color(0xFF161622)
                )
            }
        }
    } else {
        // STANDARD CALL HISTORY SCREEN
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Call Sessions", fontWeight = FontWeight.Bold, color = Color.White) },
                    actions = {
                        IconButton(onClick = { /* New call setup */ }) {
                            Icon(Icons.Default.Add, contentDescription = "New Call", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.White.copy(alpha = 0.02f)
                    )
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { /* Setup direct quick dial */ },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Call, contentDescription = "Make a call")
                }
            }
        ) { paddingValues ->
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.25f))
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                // Background Glows
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .blur(70.dp)
                        .align(Alignment.TopEnd)
                        .background(Color(0xFF00E5FF).copy(alpha = 0.03f), CircleShape)
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth(0.85f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.03f))
                            .border(1.dp, Color.White.copy(alpha = 0.06f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Call,
                            contentDescription = null,
                            modifier = Modifier.size(44.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Text(
                        text = "No Call History",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        text = "Your group revision calls, direct call sessions, and audio classrooms will be displayed here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.4f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
