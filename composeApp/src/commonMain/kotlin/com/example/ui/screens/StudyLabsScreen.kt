package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.StudygramViewModel

val NURSING_TERMS = listOf(
    "OXYTOCIN", "PLACENTA", "PEDIATRICS", "MIDWIFERY", "ANATOMY",
    "TACHYCARDIA", "HYPERTENSION", "SYSTOLIC", "DIASTOLIC", "PHARMACOLOGY",
    "HEMORRHAGE", "NEONATAL", "GESTATION", "INTRAVENOUS", "PATHOLOGY"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyLabsScreen(viewModel: StudygramViewModel) {
    var currentWord by remember { mutableStateOf(NURSING_TERMS.random()) }
    var scrambledWord by remember { mutableStateOf(currentWord.toList().shuffled().joinToString("")) }
    var userInput by remember { mutableStateOf("") }
    var showSuccess by remember { mutableStateOf(false) }

    fun nextWord() {
        val newWord = NURSING_TERMS.filter { it != currentWord }.random()
        currentWord = newWord
        scrambledWord = currentWord.toList().shuffled().joinToString("")
        userInput = ""
        showSuccess = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Study Labs: Medical Scramble", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Unscramble the Nursing Term!",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(32.dp))

            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            ) {
                Text(
                    text = scrambledWord,
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 8.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = userInput,
                onValueChange = { userInput = it.uppercase() },
                label = { Text("Your Guess") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (userInput == currentWord) {
                        showSuccess = true
                        viewModel.addStudyTokens(25)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(28.dp),
                enabled = userInput.isNotBlank() && !showSuccess
            ) {
                Text("Check Answer", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            if (showSuccess) {
                Spacer(modifier = Modifier.height(24.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF4CAF50)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Correct! 🎉", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text("+25 Study Tokens Earned", color = Color.White, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { nextWord() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF4CAF50))
                        ) {
                            Text("Next Word")
                        }
                    }
                }
            }
        }
    }
}
