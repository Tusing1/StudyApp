package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.AppScreen
import com.example.ui.StudygramViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashcardsScreen(viewModel: StudygramViewModel) {
    // Mock Data for Flashcards
    val cards = remember {
        mutableStateListOf(
            "What is the normal resting heart rate for an adult?",
            "What is the first-line medication for anaphylaxis?",
            "What is the most common cause of acute kidney injury?",
            "What is the normal range for blood pH?"
        )
    }

    var currentIndex by remember { mutableStateOf(0) }
    var isFlipped by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gamified Revision", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(AppScreen.Channels) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Coins Display
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.padding(end = 16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("🪙 450", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            if (currentIndex < cards.size) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Progress
                    Text(
                        text = "Card ${currentIndex + 1} of ${cards.size}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // Flashcard
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .aspectRatio(0.75f)
                            .shadow(12.dp, RoundedCornerShape(24.dp))
                            .clip(RoundedCornerShape(24.dp))
                            .background(
                                if (isFlipped) MaterialTheme.colorScheme.primaryContainer 
                                else MaterialTheme.colorScheme.surface
                            )
                            .clickable { isFlipped = !isFlipped },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isFlipped) "Answer goes here (Flip back to see question)" else cards[currentIndex],
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            color = if (isFlipped) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Tinder-like Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(0.8f),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Swipe Left (Don't Know)
                        IconButton(
                            onClick = { 
                                isFlipped = false
                                currentIndex++ 
                            },
                            modifier = Modifier
                                .size(64.dp)
                                .shadow(4.dp, RoundedCornerShape(32.dp))
                                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(32.dp))
                        ) {
                            Icon(Icons.Default.Clear, contentDescription = "Don't Know", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(32.dp))
                        }

                        // Swipe Right (Know)
                        IconButton(
                            onClick = { 
                                isFlipped = false
                                currentIndex++ 
                            },
                            modifier = Modifier
                                .size(64.dp)
                                .shadow(4.dp, RoundedCornerShape(32.dp))
                                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(32.dp))
                        ) {
                            Icon(Icons.Default.Check, contentDescription = "Know It", tint = Color(0xFF4CAF50), modifier = Modifier.size(32.dp))
                        }
                    }
                }
            } else {
                // Done state
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🎉", fontSize = 64.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Deck Completed!", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("+50 Coins Earned", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = { currentIndex = 0 }) {
                        Text("Study Again")
                    }
                }
            }
        }
    }
}
