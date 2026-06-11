package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.AppScreen
import com.example.ui.StudygramViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ----------------------------------------------------
// GAME DATA MODELS & DATASETS
// ----------------------------------------------------

data class LabScenario(val condition: String, val lab: String, val correct: String, val explanation: String)
data class AnatomyQuestion(val text: String, val options: List<String>, val correct: String)
data class MatchPair(val drug: String, val drugClass: String)

val LAB_SCENARIOS = listOf(
    LabScenario("Dehydration", "Urine Specific Gravity", "higher", "Kidneys conserve water, concentrating urine."),
    LabScenario("Iron Deficiency Anemia", "MCV (Mean Corpuscular Volume)", "lower", "Microcytic anemia causes smaller red blood cells."),
    LabScenario("SIADH", "Serum Sodium", "lower", "Dilutional hyponatremia due to excess water retention."),
    LabScenario("Acute Bacterial Infection", "Neutrophils", "higher", "Neutrophilia is the classic response to bacterial infection."),
    LabScenario("Hyperthyroidism", "TSH", "lower", "Excess T3/T4 suppresses pituitary TSH release.")
)

val ANATOMY_QUESTIONS = listOf(
    AnatomyQuestion("Which bone is commonly known as the collarbone?", listOf("Scapula", "Clavicle", "Sternum", "Humerus"), "Clavicle"),
    AnatomyQuestion("What is the largest artery in the human body?", listOf("Vena Cava", "Aorta", "Femoral Artery", "Carotid"), "Aorta"),
    AnatomyQuestion("Which organ produces bile?", listOf("Gallbladder", "Liver", "Pancreas", "Spleen"), "Liver"),
    AnatomyQuestion("Which cranial nerve is responsible for the sense of smell?", listOf("Optic", "Olfactory", "Trigeminal", "Vagus"), "Olfactory")
)

val MATCH_PAIRS = listOf(
    MatchPair("Amlodipine", "Calcium Channel Blocker"),
    MatchPair("Metformin", "Biguanide (Antidiabetic)"),
    MatchPair("Atorvastatin", "HMG-CoA Reductase Inhibitor"),
    MatchPair("Lisinopril", "ACE Inhibitor"),
    MatchPair("Furosemide", "Loop Diuretic")
)

data class LeaderboardEntry(val rank: Int, val name: String, val score: Int, val emoji: String)
val MOCK_LEADERBOARD = listOf(
    LeaderboardEntry(1, "Nurse Sarah", 2450, "🥇"),
    LeaderboardEntry(2, "Dr. Alex", 2200, "🥈"),
    LeaderboardEntry(3, "Stethoscope_Sam", 1950, "🥉"),
    LeaderboardEntry(4, "Pediatric_Paul", 1700, "👩‍⚕️"),
    LeaderboardEntry(5, "Pharm_Phil", 1550, "👨‍⚕️")
)

// ----------------------------------------------------
// SCREEN IMPLEMENTATION
// ----------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyLabsScreen(viewModel: StudygramViewModel) {
    var activeTab by remember { mutableStateOf("games") } // games, rankings
    var activeGame by remember { mutableStateOf<String?>(null) } // null, scramble, labflip, anatomy, pharma
    
    val userProfile by viewModel.userProfile.collectAsState()
    val studyTokens = userProfile?.studyTokens ?: 0

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Study Labs", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { 
                        if (activeGame != null) {
                            activeGame = null 
                        } else {
                            viewModel.navigateTo(AppScreen.Channels)
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.padding(end = 16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("🪙 $studyTokens TK", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.25f))
                .padding(paddingValues)
        ) {
            if (activeGame != null) {
                // Game Overlays
                when (activeGame) {
                    "scramble" -> MedicalScrambleGame(viewModel) { activeGame = null }
                    "labflip" -> LabFlipGame(viewModel) { activeGame = null }
                    "anatomy" -> AnatomyBlitzGame(viewModel) { activeGame = null }
                    "pharma" -> PharmMatchGame(viewModel) { activeGame = null }
                }
            } else {
                // Main Study Labs Dashboard
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Custom Navigation Pill Tabs
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
                                .background(if (activeTab == "games") MaterialTheme.colorScheme.primary else Color.Transparent)
                                .clickable { activeTab = "games" }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Experiments",
                                color = if (activeTab == "games") Color.Black else Color.White.copy(alpha = 0.4f),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (activeTab == "rankings") MaterialTheme.colorScheme.primary else Color.Transparent)
                                .clickable { activeTab = "rankings" }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Rankings",
                                color = if (activeTab == "rankings") Color.Black else Color.White.copy(alpha = 0.4f),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    if (activeTab == "games") {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            item {
                                GameCard(
                                    title = "Term Scramble",
                                    desc = "Unscramble complex medical terminology",
                                    icon = "🧬",
                                    glowColor = Color(0xFF818CF8)
                                ) { activeGame = "scramble" }
                            }
                            item {
                                GameCard(
                                    title = "Lab Flip",
                                    desc = "Guess Lab Values: Higher or Lower?",
                                    icon = "📈",
                                    glowColor = Color(0xFF22D3EE)
                                ) { activeGame = "labflip" }
                            }
                            item {
                                GameCard(
                                    title = "Anatomy Blitz",
                                    desc = "Race against time to identify structures",
                                    icon = "⚡",
                                    glowColor = Color(0xFFFBBF24)
                                ) { activeGame = "anatomy" }
                            }
                            item {
                                GameCard(
                                    title = "PharmMatch",
                                    desc = "Match drugs to their action categories",
                                    icon = "🧪",
                                    glowColor = Color(0xFF34D399)
                                ) { activeGame = "pharma" }
                            }
                            item {
                                GameCard(
                                    title = "Quiz Blitz",
                                    desc = "UNMC licensing exam practice MCQs",
                                    icon = "🧠",
                                    glowColor = Color(0xFFEC4899)
                                ) { viewModel.navigateTo(AppScreen.QuizPractice) }
                            }
                        }
                    } else {
                        // Rankings View
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            item {
                                Text(
                                    text = "Lab Leaderboard",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }

                            items(MOCK_LEADERBOARD) { entry ->
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
                                            Text(entry.emoji, fontSize = 24.sp)
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(entry.name, fontWeight = FontWeight.Bold, color = Color.White)
                                                Text("Rank #${entry.rank}", color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp)
                                            }
                                        }
                                        Text("${entry.score} pts", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary, fontSize = 15.sp)
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

@Composable
fun GameCard(
    title: String,
    desc: String,
    icon: String,
    glowColor: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = Color(0xFF0F0F16),
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
    ) {
        Box(modifier = Modifier.padding(16.dp)) {
            // Glow shadow behind icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .align(Alignment.TopStart)
                    .background(glowColor.copy(alpha = 0.2f), CircleShape)
            )

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(icon, fontSize = 28.sp)
                Column {
                    Text(title, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = desc,
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// ====================================================
// MINI-GAME 1: MEDICAL SCRAMBLE
// ====================================================

@Composable
fun MedicalScrambleGame(viewModel: StudygramViewModel, onExit: () -> Unit) {
    val terms = listOf("OXYTOCIN", "PLACENTA", "PEDIATRICS", "MIDWIFERY", "ANATOMY", "TACHYCARDIA", "HYPERTENSION")
    var currentWord by remember { mutableStateOf(terms.random()) }
    var scrambledWord by remember { mutableStateOf(currentWord.toList().shuffled().joinToString("")) }
    var userInput by remember { mutableStateOf("") }
    var showSuccess by remember { mutableStateOf(false) }

    fun nextWord() {
        currentWord = terms.filter { it != currentWord }.random()
        scrambledWord = currentWord.toList().shuffled().joinToString("")
        userInput = ""
        showSuccess = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Unscramble the Nursing Term!", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(modifier = Modifier.height(32.dp))

        Surface(
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        ) {
            Text(
                text = scrambledWord,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Black,
                letterSpacing = 6.sp,
                textAlign = TextAlign.Center,
                color = Color.White,
                modifier = Modifier.padding(24.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = userInput,
            onValueChange = { userInput = it.uppercase() },
            label = { Text("Your Answer") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (userInput == currentWord) {
                    showSuccess = true
                    viewModel.addStudyTokens(25)
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(25.dp),
            enabled = userInput.isNotBlank() && !showSuccess
        ) {
            Text("Verify Diagnosis", fontWeight = FontWeight.Bold)
        }

        if (showSuccess) {
            Spacer(modifier = Modifier.height(24.dp))
            Surface(
                color = Color(0xFF2E7D32).copy(alpha = 0.2f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF4CAF50)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Diagnosis Correct! 🎉", color = Color(0xFF81C784), fontWeight = FontWeight.Bold)
                    Text("+25 Study Tokens Earned", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { nextWord() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                    ) {
                        Text("Next Term", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        TextButton(onClick = onExit) {
            Text("Exit to Labs", color = Color.White.copy(alpha = 0.5f))
        }
    }
}

// ====================================================
// MINI-GAME 2: LAB FLIP (HIGHER OR LOWER)
// ====================================================

@Composable
fun LabFlipGame(viewModel: StudygramViewModel, onExit: () -> Unit) {
    var gameState by remember { mutableStateOf("start") } // start, playing, result
    var idx by remember { mutableStateOf(0) }
    var score by remember { mutableStateOf(0) }
    var streak by remember { mutableStateOf(0) }
    var timeLeft by remember { mutableStateOf(30) }
    
    val current = LAB_SCENARIOS[idx % LAB_SCENARIOS.size]

    LaunchedEffect(gameState) {
        if (gameState == "playing") {
            timeLeft = 30
            while (timeLeft > 0) {
                delay(1000)
                timeLeft--
            }
            gameState = "result"
            viewModel.addStudyTokens(score / 100)
        }
    }

    when (gameState) {
        "start" -> {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("📈", fontSize = 64.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Lab Flip", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Determine if the pathology causes the target lab value to be HIGHER or LOWER.", color = Color.White.copy(alpha = 0.6f), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(0.8f))
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = { gameState = "playing"; idx = 0; score = 0; streak = 0 },
                    modifier = Modifier.fillMaxWidth(0.6f).height(48.dp)
                ) {
                    Text("Begin Run", fontWeight = FontWeight.Bold)
                }
            }
        }
        "playing" -> {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Score: $score", color = Color.White, fontWeight = FontWeight.Bold)
                    Text("⏱️ ${timeLeft}s", color = if (timeLeft < 10) Color.Red else Color.White, fontWeight = FontWeight.Bold)
                }

                Surface(
                    color = Color.White.copy(alpha = 0.03f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("CONDITION", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(current.condition, style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Text("TARGET LAB", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(current.lab, style = MaterialTheme.typography.bodyLarge, color = Color.White, textAlign = TextAlign.Center)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = {
                            val correct = current.correct == "lower"
                            if (correct) {
                                score += 100 + (streak * 10)
                                streak++
                            } else {
                                streak = 0
                            }
                            idx++
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE57373)),
                        modifier = Modifier.weight(1f).height(60.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("LOWER", fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    Button(
                        onClick = {
                            val correct = current.correct == "higher"
                            if (correct) {
                                score += 100 + (streak * 10)
                                streak++
                            } else {
                                streak = 0
                            }
                            idx++
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF81C784)),
                        modifier = Modifier.weight(1f).height(60.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("HIGHER", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
        "result" -> {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("📈", fontSize = 64.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Run Finished!", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Your Score: $score pts", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))
                val earned = score / 100
                Text("+$earned Tokens Earned", color = Color.White.copy(alpha = 0.7f))
                Spacer(modifier = Modifier.height(32.dp))
                Button(onClick = onExit) {
                    Text("Return to Lab", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ====================================================
// MINI-GAME 3: ANATOMY BLITZ
// ====================================================

@Composable
fun AnatomyBlitzGame(viewModel: StudygramViewModel, onExit: () -> Unit) {
    var gameState by remember { mutableStateOf("start") } // start, playing, result
    var idx by remember { mutableStateOf(0) }
    var score by remember { mutableStateOf(0) }
    var timeLeft by remember { mutableStateOf(30) }

    val current = ANATOMY_QUESTIONS[idx % ANATOMY_QUESTIONS.size]

    LaunchedEffect(gameState) {
        if (gameState == "playing") {
            timeLeft = 30
            while (timeLeft > 0) {
                delay(1000)
                timeLeft--
            }
            gameState = "result"
            viewModel.addStudyTokens(score / 100)
        }
    }

    when (gameState) {
        "start" -> {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("⚡", fontSize = 64.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Anatomy Blitz", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Quickly answer structural identification questions under a timer.", color = Color.White.copy(alpha = 0.6f), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(0.8f))
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = { gameState = "playing"; idx = 0; score = 0 },
                    modifier = Modifier.fillMaxWidth(0.6f).height(48.dp)
                ) {
                    Text("Start Blitz", fontWeight = FontWeight.Bold)
                }
            }
        }
        "playing" -> {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Score: $score", color = Color.White, fontWeight = FontWeight.Bold)
                    Text("⏱️ ${timeLeft}s", color = if (timeLeft < 10) Color.Red else Color.White, fontWeight = FontWeight.Bold)
                }

                Surface(
                    color = Color.White.copy(alpha = 0.03f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
                ) {
                    Text(
                        text = current.text,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(24.dp)
                    )
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    current.options.forEach { opt ->
                        Button(
                            onClick = {
                                if (opt == current.correct) {
                                    score += 150
                                }
                                idx++
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.05f)),
                            modifier = Modifier.fillMaxWidth().height(54.dp),
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                        ) {
                            Text(opt, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        "result" -> {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("⚡", fontSize = 64.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Blitz Completed!", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Your Score: $score pts", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))
                val earned = score / 100
                Text("+$earned Tokens Earned", color = Color.White.copy(alpha = 0.7f))
                Spacer(modifier = Modifier.height(32.dp))
                Button(onClick = onExit) {
                    Text("Return to Lab", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ====================================================
// MINI-GAME 4: PHARMMATCH
// ====================================================

@Composable
fun PharmMatchGame(viewModel: StudygramViewModel, onExit: () -> Unit) {
    var selectedDrug by remember { mutableStateOf<String?>(null) }
    var selectedClass by remember { mutableStateOf<String?>(null) }
    var matchedPairs by remember { mutableStateOf(emptySet<String>()) }
    var isDone by remember { mutableStateOf(false) }

    val drugs = remember { MATCH_PAIRS.map { it.drug }.shuffled() }
    val classes = remember { MATCH_PAIRS.map { it.drugClass }.shuffled() }

    LaunchedEffect(matchedPairs) {
        if (matchedPairs.size == MATCH_PAIRS.size) {
            isDone = true
            viewModel.addStudyTokens(30)
        }
    }

    if (isDone) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("🧪", fontSize = 64.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Text("All Matched! 🎉", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.height(8.dp))
            Text("You matched all pharmacologic agents with their therapeutic classes.", color = Color.White.copy(alpha = 0.6f), textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(16.dp))
            Text("+30 Study Tokens Earned", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(32.dp))
            Button(onClick = onExit) {
                Text("Return to Lab", fontWeight = FontWeight.Bold)
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("PharmMatch Labs", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(modifier = Modifier.height(6.dp))
                Text("Match each agent to its pharmaceutical class category.", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
            }

            Row(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Drugs list
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("DRUG", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    drugs.forEach { d ->
                        val isMatched = MATCH_PAIRS.any { it.drug == d && it.drugClass in matchedPairs }
                        val isSelected = selectedDrug == d
                        
                        Surface(
                            onClick = { if (!isMatched) selectedDrug = d },
                            color = when {
                                isMatched -> Color(0xFF2E7D32).copy(alpha = 0.2f)
                                isSelected -> MaterialTheme.colorScheme.primary
                                else -> Color.White.copy(alpha = 0.05f)
                            },
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp, 
                                when {
                                    isMatched -> Color(0xFF4CAF50)
                                    isSelected -> MaterialTheme.colorScheme.primary
                                    else -> Color.White.copy(alpha = 0.1f)
                                }
                            ),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 8.dp)) {
                                Text(
                                    text = d,
                                    color = if (isSelected) Color.Black else Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                // Classes list
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("CLASS", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    classes.forEach { c ->
                        val isMatched = matchedPairs.contains(c)
                        val isSelected = selectedClass == c

                        Surface(
                            onClick = {
                                if (!isMatched && selectedDrug != null) {
                                    selectedClass = c
                                    val pair = MATCH_PAIRS.find { it.drug == selectedDrug }
                                    if (pair != null && pair.drugClass == c) {
                                        matchedPairs = matchedPairs + c
                                    }
                                    selectedDrug = null
                                    selectedClass = null
                                }
                            },
                            color = when {
                                isMatched -> Color(0xFF2E7D32).copy(alpha = 0.2f)
                                isSelected -> MaterialTheme.colorScheme.primary
                                else -> Color.White.copy(alpha = 0.05f)
                            },
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                when {
                                    isMatched -> Color(0xFF4CAF50)
                                    isSelected -> MaterialTheme.colorScheme.primary
                                    else -> Color.White.copy(alpha = 0.1f)
                                }
                            ),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 8.dp)) {
                                Text(
                                    text = c,
                                    color = if (isSelected) Color.Black else Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            TextButton(onClick = onExit) {
                Text("Exit to Labs", color = Color.White.copy(alpha = 0.5f))
            }
        }
    }
}
