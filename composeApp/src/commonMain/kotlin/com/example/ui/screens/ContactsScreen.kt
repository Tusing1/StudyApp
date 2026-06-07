package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.StudygramViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen(viewModel: StudygramViewModel) {
    val buddies = viewModel.potentialBuddies
    var showMatchDialog by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Study Buddies", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            if (buddies.isEmpty()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No more peers in your area.", color = Color.Gray)
                }
            } else {
                val currentBuddy = buddies.first()

                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .aspectRatio(0.75f)
                            .shadow(12.dp, RoundedCornerShape(24.dp)),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(120.dp)
                                    .clip(CircleShape)
                                    .background(Color(currentBuddy.avatarColor)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = currentBuddy.username.first().toString(),
                                    color = Color.White,
                                    fontSize = 48.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            Text(
                                text = currentBuddy.username,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text(
                                    text = currentBuddy.nursingField,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(48.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(0.7f),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Skip Button
                        IconButton(
                            onClick = { viewModel.matchWithBuddy(currentBuddy.id, false) },
                            modifier = Modifier
                                .size(72.dp)
                                .shadow(8.dp, CircleShape)
                                .background(MaterialTheme.colorScheme.surface, CircleShape)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Skip", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(36.dp))
                        }

                        // Match Button
                        IconButton(
                            onClick = { 
                                showMatchDialog = currentBuddy.username
                                viewModel.matchWithBuddy(currentBuddy.id, true) 
                            },
                            modifier = Modifier
                                .size(72.dp)
                                .shadow(8.dp, CircleShape)
                                .background(MaterialTheme.colorScheme.surface, CircleShape)
                        ) {
                            Icon(Icons.Default.Favorite, contentDescription = "Match", tint = Color(0xFFE91E63), modifier = Modifier.size(36.dp))
                        }
                    }
                }
            }

            if (showMatchDialog != null) {
                AlertDialog(
                    onDismissRequest = { showMatchDialog = null },
                    confirmButton = {
                        Button(onClick = { showMatchDialog = null }) {
                            Text("Keep Swiping")
                        }
                    },
                    title = { Text("It's a Match! 🎉") },
                    text = { Text("You and ${showMatchDialog} have liked each other. You can now chat in your Direct Messages.") }
                )
            }
        }
    }
}
