package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.StudygramViewModel
import com.example.data.AudioPlayer
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioPlayerScreen(viewModel: StudygramViewModel, url: String) {
    val player = remember { AudioPlayer() }
    
    var isPlaying by remember { mutableStateOf(false) }
    var currentPositionMs by remember { mutableStateOf(0L) }
    var durationMs by remember { mutableStateOf(0L) }
    var speed by remember { mutableStateOf(1f) }
    var volume by remember { mutableStateOf(1f) }
    var isMuted by remember { mutableStateOf(false) }

    // Periodically update state from native player
    LaunchedEffect(player) {
        while (true) {
            isPlaying = player.isPlaying
            currentPositionMs = player.currentPositionMs
            durationMs = player.durationMs
            delay(150)
        }
    }

    DisposableEffect(player) {
        onDispose {
            player.release()
        }
    }

    val fileName = url.substringAfterLast("/").substringBefore("?")
    val progress = if (durationMs > 0) currentPositionMs.toFloat() / durationMs.toFloat() else 0f

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(fileName, fontWeight = FontWeight.Bold, maxLines = 1, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(com.example.ui.AppScreen.Channels) }) {
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
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            // Ambient glows
            Box(
                modifier = Modifier
                    .size(300.dp)
                    .blur(80.dp)
                    .align(Alignment.TopStart)
                    .background(Color(0xFF8E24AA).copy(alpha = 0.05f), CircleShape)
            )
            Box(
                modifier = Modifier
                    .size(300.dp)
                    .blur(80.dp)
                    .align(Alignment.BottomEnd)
                    .background(Color(0xFF00E5FF).copy(alpha = 0.05f), CircleShape)
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .padding(16.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White.copy(alpha = 0.03f))
                    .padding(24.dp)
            ) {
                Text(
                    text = "NOW PLAYING",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.5.sp
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = fileName,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1
                )
                
                Spacer(modifier = Modifier.height(24.dp))

                // Interactive Audio Waveform
                val barCount = 28
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(70.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(barCount) { index ->
                        // Deterministic heights for mock waveform visual
                        val barHeight = when (index % 4) {
                            0 -> 48.dp
                            1 -> 28.dp
                            2 -> 36.dp
                            else -> 18.dp
                        }
                        
                        val isPlayed = (index.toFloat() / barCount.toFloat()) < progress
                        val barColor = if (isPlayed) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.15f)

                        Box(
                            modifier = Modifier
                                .width(5.dp)
                                .height(barHeight)
                                .clip(CircleShape)
                                .background(barColor)
                                .clickable {
                                    if (durationMs > 0) {
                                        val seekRatio = index.toFloat() / barCount.toFloat()
                                        player.seekTo((seekRatio * durationMs).toLong())
                                    }
                                }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Time Labels
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatTime(currentPositionMs),
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                    Text(
                        text = formatTime(durationMs),
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Playback Speed Pill
                Button(
                    onClick = {
                        val speeds = listOf(0.5f, 1f, 1.5f, 2f)
                        val idx = speeds.indexOf(speed)
                        val nextIdx = (idx + 1) % speeds.size
                        speed = speeds[nextIdx]
                        player.setPlaybackSpeed(speed)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.05f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "${speed}x Speed",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Playback Controls Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Skip Backward 10s
                    IconButton(
                        onClick = {
                            val target = (currentPositionMs - 10000).coerceAtLeast(0L)
                            player.seekTo(target)
                        }
                    ) {
                        // Unicode 10s back arrow symbol replacement or text representation
                        Text("⟲ 10s", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    // Play / Pause Circle
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary,
                                        Color(0xFF00E5FF)
                                    )
                                )
                            )
                            .clickable {
                                if (isPlaying) {
                                    player.pause()
                                } else {
                                    player.play(url)
                                    player.setPlaybackSpeed(speed)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isPlaying) {
                            // Draw Custom Pause Lines (since standard Pause icon might not load)
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.width(5.dp).height(18.dp).background(Color.White, RoundedCornerShape(2.dp)))
                                Box(modifier = Modifier.width(5.dp).height(18.dp).background(Color.White, RoundedCornerShape(2.dp)))
                            }
                        } else {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = "Play",
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    // Skip Forward 10s
                    IconButton(
                        onClick = {
                            val target = (currentPositionMs + 10000).coerceAtMost(durationMs)
                            player.seekTo(target)
                        }
                    ) {
                        Text("10s ⟳", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Volume Controls
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isMuted) "🔇" else "🔊",
                        modifier = Modifier
                            .clickable {
                                isMuted = !isMuted
                                player.setVolume(if (isMuted) 0f else volume)
                            }
                            .padding(8.dp),
                        fontSize = 16.sp
                    )
                    
                    Slider(
                        value = volume,
                        onValueChange = {
                            volume = it
                            isMuted = false
                            player.setVolume(it)
                        },
                        valueRange = 0f..1f,
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = Color.White.copy(alpha = 0.1f)
                        )
                    )
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    if (ms <= 0) return "0:00"
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}
