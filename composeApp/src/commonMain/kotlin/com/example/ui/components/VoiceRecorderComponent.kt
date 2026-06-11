package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AudioRecorder
import com.example.data.checkAndRequestRecordPermission
import com.example.data.getTempDirectoryPath
import com.example.data.readFileBytes
import com.example.data.deleteFile
import kotlinx.coroutines.delay

@Composable
fun VoiceRecorderComponent(
    onRecordingComplete: (fileBytes: ByteArray, durationSeconds: Int) -> Unit,
    onRecordingStateChanged: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val recorder = remember { AudioRecorder() }
    var isRecording by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }
    var durationSeconds by remember { mutableStateOf(0) }
    var maxAmplitude by remember { mutableStateOf(0f) }
    
    val currentFilePath = remember { mutableStateOf("") }

    LaunchedEffect(recorder) {
        while (true) {
            val oldRecording = isRecording
            isRecording = recorder.isRecording
            isPaused = recorder.isPaused
            if (isRecording != oldRecording) {
                onRecordingStateChanged(isRecording)
            }
            if (recorder.isRecording) {
                durationSeconds = recorder.durationSeconds
                maxAmplitude = recorder.maxAmplitude
            }
            delay(100)
        }
    }

    DisposableEffect(recorder) {
        onDispose {
            recorder.release()
        }
    }

    if (!isRecording) {
        IconButton(
            onClick = {
                checkAndRequestRecordPermission {
                    val tempPath = getTempDirectoryPath()
                    val path = if (tempPath.isNotEmpty()) {
                        "$tempPath/voice_message_${System.currentTimeMillis()}.mp4"
                    } else {
                        "voice_message_${System.currentTimeMillis()}.mp4"
                    }
                    currentFilePath.value = path
                    recorder.startRecording(path)
                }
            },
            modifier = modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.05f))
        ) {
            Text("🎤", fontSize = 18.sp)
        }
    } else {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFFE53935).copy(alpha = 0.15f))
                .border(1.dp, Color(0xFFE53935).copy(alpha = 0.25f), RoundedCornerShape(24.dp))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Cancel Button
            IconButton(
                onClick = {
                    recorder.release()
                    if (currentFilePath.value.isNotEmpty()) {
                        deleteFile(currentFilePath.value)
                    }
                    isRecording = false
                }
            ) {
                Text("🗑️", fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Pulsing Indicator
            val animatedScale by animateFloatAsState(
                targetValue = if (isPaused) 1f else 1f + (maxAmplitude * 0.4f),
                label = "pulse"
            )
            
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .scale(animatedScale)
                    .clip(CircleShape)
                    .background(Color(0xFFE53935))
            )

            Spacer(modifier = Modifier.width(8.dp))

            val mins = durationSeconds / 60
            val secs = durationSeconds % 60
            val durationText = "$mins:${secs.toString().padStart(2, '0')}"
            
            Text(
                text = durationText,
                color = Color(0xFFE53935),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Waveform visualizer
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(24.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(8) { index ->
                    val randomVal = (index * 13 % 17) / 17f
                    val heightRatio = if (isPaused) 0.15f else (maxAmplitude * 0.7f + randomVal * 0.3f).coerceIn(0.15f, 1f)
                    val barHeight = (heightRatio * 20).dp
                    
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(barHeight)
                            .clip(CircleShape)
                            .background(Color(0xFFE53935).copy(alpha = 0.6f))
                    )
                }
            }

            // Pause / Resume Toggle
            IconButton(
                onClick = {
                    if (isPaused) {
                        recorder.resumeRecording()
                    } else {
                        recorder.pauseRecording()
                    }
                }
            ) {
                Text(if (isPaused) "▶️" else "⏸️", fontSize = 14.sp)
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Send Button
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable {
                        recorder.stopRecording()
                        val filePath = currentFilePath.value
                        if (filePath.isNotEmpty()) {
                            val bytes = readFileBytes(filePath)
                            if (bytes.isNotEmpty()) {
                                onRecordingComplete(bytes, durationSeconds)
                            }
                            deleteFile(filePath)
                        }
                        isRecording = false
                    },
                contentAlignment = Alignment.Center
            ) {
                Text("🚀", fontSize = 14.sp)
            }
        }
    }
}
