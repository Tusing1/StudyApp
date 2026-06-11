package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.AppScreen
import com.example.ui.StudygramViewModel
import com.example.ui.StudyTransaction
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyTokensScreen(viewModel: StudygramViewModel) {
    var activeTab by remember { mutableStateOf("earn") } // earn, buy, spend, tokenomics, history
    val userProfile by viewModel.userProfile.collectAsState()
    val balance = userProfile?.studyTokens ?: 0
    val streak = viewModel.loginStreak
    val transactions = viewModel.tokenTransactions

    val scope = rememberCoroutineScope()
    
    // Dialog states for purchase simulation
    var showMMPanel by remember { mutableStateOf<Boolean>(false) }
    var selectedPackageName by remember { mutableStateOf("") }
    var selectedPackagePrice by remember { mutableStateOf(0) }
    var selectedPackageTokens by remember { mutableStateOf(0) }
    var mmProvider by remember { mutableStateOf("") } // MTN or Airtel
    var paymentPhone by remember { mutableStateOf("") }
    var mockActivationCode by remember { mutableStateOf<String?>(null) }
    var isProcessingPayment by remember { mutableStateOf(false) }

    var codeVerificationInput by remember { mutableStateOf("") }
    var verificationStatusMsg by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Study Tokens Wallet", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(AppScreen.Channels) }) {
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
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Immersive Balance Card
                Surface(
                    color = Color(0xFF0F0F1A),
                    shape = RoundedCornerShape(32.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.padding(24.dp)) {
                        Column {
                            Text(
                                "CURRENT BALANCE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.4f),
                                letterSpacing = 1.5.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    "$balance",
                                    fontSize = 48.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFFFFD700),
                                    lineHeight = 48.sp
                                )
                                Text(
                                    "TK",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFFD700).copy(alpha = 0.6f),
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                            }
                        }

                        // Streak and Total Earned indicators on right
                        Column(
                            horizontalAlignment = Alignment.End,
                            modifier = Modifier.align(Alignment.CenterEnd)
                        ) {
                            Surface(
                                color = Color(0xFFFF9800).copy(alpha = 0.15f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text("🔥", fontSize = 12.sp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("$streak Day Streak", color = Color(0xFFFF9800), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Custom navigation scrollable menu pills
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.03f))
                        .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val tabs = listOf("earn" to "Earn", "buy" to "Buy", "spend" to "Spend", "tokenomics" to "Value", "history" to "Ledger")
                    tabs.forEach { (tabId, label) ->
                        val isSelected = activeTab == tabId
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .clickable { activeTab = tabId }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) Color.Black else Color.White.copy(alpha = 0.6f),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                maxLines = 1
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Tab Content Selector
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    when (activeTab) {
                        "earn" -> EarnTab(viewModel)
                        "buy" -> BuyTab(
                            onSelectPackage = { name, tokens, price ->
                                selectedPackageName = name
                                selectedPackageTokens = tokens
                                selectedPackagePrice = price
                                showMMPanel = true
                                mockActivationCode = null
                                mmProvider = ""
                                paymentPhone = ""
                            },
                            codeVerificationInput = codeVerificationInput,
                            onCodeInputChange = { codeVerificationInput = it },
                            verificationStatusMsg = verificationStatusMsg,
                            onVerifyCode = {
                                if (mockActivationCode != null && codeVerificationInput.trim().uppercase() == mockActivationCode) {
                                    viewModel.addStudyTokens(selectedPackageTokens, "Purchased $selectedPackageTokens Tokens")
                                    verificationStatusMsg = "Success! $selectedPackageTokens TK added to balance."
                                    mockActivationCode = null
                                    codeVerificationInput = ""
                                } else {
                                    verificationStatusMsg = "Invalid activation code. Try again."
                                }
                            }
                        )
                        "spend" -> SpendTab(viewModel, balance)
                        "tokenomics" -> TokenomicsTab(transactions)
                        "history" -> LedgerTab(transactions)
                    }
                }
            }

            // Mobile Money Payment Dialog Simulation
            if (showMMPanel) {
                AlertDialog(
                    onDismissRequest = { showMMPanel = false },
                    title = { Text("Mobile Money Payment", color = Color.White, fontWeight = FontWeight.Bold) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                "Package: $selectedPackageName\nPrice: UGX ${selectedPackagePrice.toString().replace(Regex("(\\d)(?=(\\d{3})+\$)"), "$1,")}",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 13.sp
                            )
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { mmProvider = "MTN" },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (mmProvider == "MTN") Color(0xFFFFEB3B) else Color.White.copy(alpha = 0.05f)
                                    ),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("MTN MoMo", color = if (mmProvider == "MTN") Color.Black else Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = { mmProvider = "Airtel" },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (mmProvider == "Airtel") Color(0xFFEF5350) else Color.White.copy(alpha = 0.05f)
                                    ),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Airtel Money", color = if (mmProvider == "Airtel") Color.White else Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            OutlinedTextField(
                                value = paymentPhone,
                                onValueChange = { paymentPhone = it },
                                label = { Text("Phone Number") },
                                placeholder = { Text("07XXXXXXXX") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )

                            if (isProcessingPayment) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Waiting for MM PIN entry...", color = Color.White, fontSize = 12.sp)
                                }
                            }

                            if (mockActivationCode != null) {
                                Surface(
                                    color = Color(0xFF4CAF50).copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(12.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF4CAF50))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text("Payment Approved! 🎉", color = Color(0xFF81C784), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("Activation Code (Enter below):", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                                        Text(
                                            mockActivationCode!!,
                                            color = Color.White,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 24.sp,
                                            letterSpacing = 2.sp,
                                            fontFamily = FontFamily.Monospace,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        if (mockActivationCode == null) {
                            Button(
                                onClick = {
                                    scope.launch {
                                        isProcessingPayment = true
                                        delay(2000)
                                        isProcessingPayment = false
                                        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
                                        mockActivationCode = "TK" + (1..4).map { chars.random() }.joinToString("")
                                    }
                                },
                                enabled = mmProvider.isNotEmpty() && paymentPhone.isNotEmpty() && !isProcessingPayment
                            ) {
                                Text("Pay Now", fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Button(
                                onClick = { showMMPanel = false }
                            ) {
                                Text("Done", fontWeight = FontWeight.Bold)
                            }
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showMMPanel = false }) {
                            Text("Cancel", color = Color.White.copy(alpha = 0.5f))
                        }
                    }
                )
            }
        }
    }
}

// ----------------------------------------------------
// TAB SUB-COMPONENTS
// ----------------------------------------------------

@Composable
fun EarnTab(viewModel: StudygramViewModel) {
    val activities = listOf(
        EarnActivityItem("Daily Login", 15, "Claimed automatically on opening the app.", "Claimed", Color(0xFFFFC107)),
        EarnActivityItem("Complete Revision Deck", 50, "Answer questions in a subject deck.", "Play Decks", Color(0xFF4CAF50)),
        EarnActivityItem("Play Lab Games", 25, "Verify scramble terms, flip lab results, or play matching.", "Visit Labs", Color(0xFF03A9F4)),
        EarnActivityItem("Invite Peer Friend", 75, "Share referral code with other nursing students.", "Share", Color(0xFFE91E63))
    )

    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(activities) { act ->
            Surface(
                color = Color.White.copy(alpha = 0.02f),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(act.color.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("+", color = act.color, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(act.name, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                            Text(act.desc, color = Color.White.copy(alpha = 0.4f), fontSize = 10.sp, lineHeight = 14.sp)
                        }
                    }

                    Surface(
                        color = act.color.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            "+${act.tokens} TK",
                            color = act.color,
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

data class EarnActivityItem(val name: String, val tokens: Int, val desc: String, val actionText: String, val color: Color)

@Composable
fun BuyTab(
    onSelectPackage: (String, Int, Int) -> Unit,
    codeVerificationInput: String,
    onCodeInputChange: (String) -> Unit,
    verificationStatusMsg: String,
    onVerifyCode: () -> Unit
) {
    val packages = listOf(
        TokenPackageItem("100 Tokens", 100, 5000),
        TokenPackageItem("250 Tokens", 250, 10000),
        TokenPackageItem("600 Tokens", 600, 20000),
        TokenPackageItem("1500 Tokens", 1500, 45000)
    )

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Select Store Package", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            packages.take(2).forEach { pkg ->
                PackageCard(pkg, modifier = Modifier.weight(1f), onSelect = onSelectPackage)
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            packages.drop(2).forEach { pkg ->
                PackageCard(pkg, modifier = Modifier.weight(1f), onSelect = onSelectPackage)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Claim Activation Code", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = codeVerificationInput,
                onValueChange = onCodeInputChange,
                placeholder = { Text("E.G. TK8X2Y") },
                singleLine = true,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            Button(
                onClick = onVerifyCode,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(56.dp)
            ) {
                Text("Redeem", fontWeight = FontWeight.Bold)
            }
        }

        if (verificationStatusMsg.isNotEmpty()) {
            Text(
                verificationStatusMsg,
                color = if (verificationStatusMsg.startsWith("Success")) Color(0xFF81C784) else Color(0xFFE57373),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

data class TokenPackageItem(val name: String, val tokens: Int, val price: Int)

@Composable
fun PackageCard(pkg: TokenPackageItem, modifier: Modifier = Modifier, onSelect: (String, Int, Int) -> Unit) {
    Surface(
        onClick = { onSelect(pkg.name, pkg.tokens, pkg.price) },
        color = Color.White.copy(alpha = 0.02f),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
        modifier = modifier.height(100.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(pkg.name, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text("UGX " + pkg.price.toString().replace(Regex("(\\d)(?=(\\d{3})+\$)"), "$1,"), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black, fontSize = 16.sp)
        }
    }
}

@Composable
fun SpendTab(viewModel: StudygramViewModel, balance: Int) {
    val items = listOf(
        SpendItem("Reveal Tinder Likes", 100, "View the names and profiles of matches that swiped on you.", "likes_reveal"),
        SpendItem("Standard AI Tutor Mode", 50, "Ask Gemini detailed explanations on peer conversations.", "ai_tutor"),
        SpendItem("Unlimited AI Mentor playground", 350, "Full access playground asking medical queries without bounds.", "ai_playground"),
        SpendItem("Record Video call sessions", 150, "Record WebRTC audio and video stream captures locally.", "call_recordings")
    )

    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(items) { item ->
            val hasEnough = balance >= item.cost
            Surface(
                color = Color.White.copy(alpha = 0.02f),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.name, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                        Text(item.desc, color = Color.White.copy(alpha = 0.4f), fontSize = 10.sp, lineHeight = 14.sp)
                    }

                    Button(
                        onClick = {
                            viewModel.spendStudyTokens(item.cost, "Unlocked ${item.name}")
                        },
                        enabled = hasEnough,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("${item.cost} TK", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

data class SpendItem(val name: String, val cost: Int, val desc: String, val key: String)

@Composable
fun TokenomicsTab(transactions: List<StudyTransaction>) {
    val totalEarned = transactions.filter { it.isEarn }.sumOf { it.amount }
    val totalSpent = transactions.filter { !it.isEarn }.sumOf { it.amount }
    val currentSupply = totalEarned - totalSpent

    // Ratio indicators
    val burnRatio = if (totalEarned > 0) totalSpent.toFloat() / totalEarned.toFloat() else 0f
    val valueMultiplier = 1f + (burnRatio * 2f)

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Tokenomics ledger & Value check", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)

        Surface(
            color = Color.White.copy(alpha = 0.02f),
            shape = RoundedCornerShape(24.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Total Supply Circulating", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                    Text("$currentSupply TK", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Total Supply Burned", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                    Text("$totalSpent TK", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                Divider(color = Color.White.copy(alpha = 0.08f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Current Value multiplier", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            "${String.format("%.2f", valueMultiplier)}x",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // Circular scarcity bar illustration
        Surface(
            color = Color.White.copy(alpha = 0.01f),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.03f))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("ℹ️", fontSize = 18.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "Token value increases automatically as more tokens are burned through premium feature unlocking.",
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 10.sp,
                    lineHeight = 14.sp
                )
            }
        }
    }
}

@Composable
fun LedgerTab(transactions: List<StudyTransaction>) {
    if (transactions.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("No transactions logged.", color = Color.White.copy(alpha = 0.3f), fontSize = 13.sp)
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(transactions) { tx ->
                Surface(
                    color = Color.White.copy(alpha = 0.02f),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(if (tx.isEarn) "📥" else "📤", fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(tx.description, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                                Text("Transaction Ledger Log", color = Color.White.copy(alpha = 0.3f), fontSize = 9.sp)
                            }
                        }

                        Text(
                            text = (if (tx.isEarn) "+" else "-") + "${tx.amount} TK",
                            color = if (tx.isEarn) Color(0xFF81C784) else Color(0xFFE57373),
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}
