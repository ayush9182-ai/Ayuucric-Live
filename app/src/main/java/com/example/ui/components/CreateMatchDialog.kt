package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.security.SecureRandom
import com.example.data.maps.GoogleMapsGroundingService
import com.example.data.maps.GroundVenueInfo
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CreateMatchDialog(
    onDismiss: () -> Unit,
    onCreateMatch: (com.example.data.model.CreateMatchParams) -> Unit
) {
    var currentStep by remember { mutableIntStateOf(1) } // 1: Teams, 2: Toss, 3: Squad & Opening Players

    val todayFormatted = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date()) }
    val nowFormatted = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date()) }

    // Step 1: Match & Teams
    var matchName by remember { mutableStateOf("Local Cricket Tournament") }
    var teamA by remember { mutableStateOf("") }
    var teamB by remember { mutableStateOf("") }
    var overs by remember { mutableIntStateOf(10) }
    var venue by remember { mutableStateOf("Ground Turf") }
    var matchDate by remember { mutableStateOf(todayFormatted) }
    var matchTime by remember { mutableStateOf(nowFormatted) }
    var venueAddress by remember { mutableStateOf("") }
    var venueCoordinates by remember { mutableStateOf("") }
    var showGroundSearchSection by remember { mutableStateOf(false) }
    var groundSearchQuery by remember { mutableStateOf("") }
    var isSearchingGrounds by remember { mutableStateOf(false) }
    var groundResults by remember { mutableStateOf<List<GroundVenueInfo>>(GoogleMapsGroundingService.POPULAR_GROUNDS) }

    // Step 2: Toss
    var tossWinnerChoice by remember { mutableIntStateOf(1) } // 1: Team A, 2: Team B
    var tossElectedChoice by remember { mutableStateOf("BAT") } // "BAT" or "BOWL"

    // Step 3: Mandatory Player Names & Squad
    var striker by remember { mutableStateOf("") }
    var nonStriker by remember { mutableStateOf("") }
    var bowler by remember { mutableStateOf("") }
    var teamAPlayersInput by remember { mutableStateOf("") }
    var teamBPlayersInput by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()
    val secureRandom = remember { SecureRandom() }

    // Live Coin Toss Ceremony State
    var isCoinFlipping by remember { mutableStateOf(false) }
    var coinTossWinner by remember { mutableStateOf<String?>(null) }
    var coinTossOutcome by remember { mutableStateOf<String?>(null) } // "HEADS" or "TAILS"
    var callingTeamChoice by remember { mutableIntStateOf(1) } // 1: Team A calls, 2: Team B calls
    var callingCallChoice by remember { mutableStateOf("HEADS") } // "HEADS" or "TAILS"
    var showManualTossMode by remember { mutableStateOf(false) }

    // 3D rotation and bounce animatables for live coin flip
    val coinRotationY = remember { Animatable(0f) }
    val coinTranslationY = remember { Animatable(0f) }

    val currentCoinSide = remember {
        derivedStateOf {
            val normalized = ((coinRotationY.value % 360f) + 360f) % 360f
            if (normalized in 90f..270f) "TAILS" else "HEADS"
        }
    }

    val cleanTeamA = teamA.trim()
    val cleanTeamB = teamB.trim()
    val teamANameDisplay = cleanTeamA.ifBlank { "Team 1" }
    val teamBNameDisplay = cleanTeamB.ifBlank { "Team 2" }

    val tossWinnerName = if (tossWinnerChoice == 1) (if (cleanTeamA.isNotBlank()) cleanTeamA else "Team 1") else (if (cleanTeamB.isNotBlank()) cleanTeamB else "Team 2")
    val isTossBat = tossElectedChoice == "BAT"

    // Calculate Batting and Bowling teams from toss
    val calculatedBattingTeam = if (tossWinnerChoice == 1) {
        if (isTossBat) (if (cleanTeamA.isNotBlank()) cleanTeamA else "Team 1") else (if (cleanTeamB.isNotBlank()) cleanTeamB else "Team 2")
    } else {
        if (isTossBat) (if (cleanTeamB.isNotBlank()) cleanTeamB else "Team 2") else (if (cleanTeamA.isNotBlank()) cleanTeamA else "Team 1")
    }

    val calculatedBowlingTeam = if (calculatedBattingTeam == (if (cleanTeamA.isNotBlank()) cleanTeamA else "Team 1")) {
        if (cleanTeamB.isNotBlank()) cleanTeamB else "Team 2"
    } else {
        if (cleanTeamA.isNotBlank()) cleanTeamA else "Team 1"
    }

    val tossSummaryText = "🪙 $tossWinnerName won the toss and elected to ${if (isTossBat) "BAT" else "BOWL"} first"

    fun doLiveCoinToss() {
        if (isCoinFlipping) return
        isCoinFlipping = true
        coinTossOutcome = null
        coinTossWinner = null

        coroutineScope.launch {
            val outcome = if (secureRandom.nextDouble() < 0.5) "HEADS" else "TAILS"
            val totalSpins = 5 + secureRandom.nextInt(4)
            val targetDegrees = if (outcome == "HEADS") {
                (totalSpins * 360f)
            } else {
                (totalSpins * 360f) + 180f
            }

            val flipJob = launch {
                coinRotationY.snapTo(0f)
                coinRotationY.animateTo(
                    targetValue = targetDegrees,
                    animationSpec = tween(durationMillis = 1800, easing = FastOutSlowInEasing)
                )
            }

            val bounceJob = launch {
                coinTranslationY.animateTo(-80f, tween(700, easing = FastOutSlowInEasing))
                coinTranslationY.animateTo(0f, tween(1100, easing = FastOutSlowInEasing))
            }

            flipJob.join()
            bounceJob.join()

            coinTossOutcome = outcome
            val callingName = if (callingTeamChoice == 1) teamANameDisplay else teamBNameDisplay
            val otherName = if (callingTeamChoice == 1) teamBNameDisplay else teamANameDisplay
            val won = outcome == callingCallChoice
            val winner = if (won) callingName else otherName
            coinTossWinner = winner

            // Update tossWinnerChoice: 1 if winner is Team A, 2 if Team B
            tossWinnerChoice = if (winner == teamANameDisplay) 1 else 2
            isCoinFlipping = false
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f),
            shape = RoundedCornerShape(24.dp),
            color = PitchDark,
            border = BorderStroke(1.dp, Color(0xFF1E293B)),
            tonalElevation = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(StadiumGold.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SportsCricket,
                                contentDescription = null,
                                tint = StadiumGold,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Start New Live Match",
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Step $currentStep of 3: ${
                                    when (currentStep) {
                                        1 -> "Teams & Overs"
                                        2 -> "Toss Ceremony"
                                        else -> "Squad & Opening Players"
                                    }
                                }",
                                color = StadiumGold,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Progress Step Indicators
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (step in 1..3) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(
                                    if (step <= currentStep) StadiumGold else Color(0xFF334155)
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Error Message Banner (if any)
                if (errorMessage != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(DrsOutRed.copy(alpha = 0.2f))
                            .border(1.dp, DrsOutRed, RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = DrsOutRed, modifier = Modifier.size(16.dp))
                            Text(
                                text = errorMessage!!,
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Step Content
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    when (currentStep) {
                        1 -> {
                            // STEP 1: TEAMS & OVERS
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    text = "1. TEAMS INFORMATION (MANDATORY)",
                                    fontSize = 11.sp,
                                    color = StadiumGold,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp
                                )

                                OutlinedTextField(
                                    value = teamA,
                                    onValueChange = {
                                        teamA = it
                                        errorMessage = null
                                    },
                                    label = { Text("Team 1 Name *", fontSize = 12.sp) },
                                    placeholder = { Text("e.g. Royal Challengers", fontSize = 11.sp, color = TextMuted) },
                                    leadingIcon = { Icon(Icons.Default.Groups, contentDescription = null, tint = CricketGreen, modifier = Modifier.size(18.dp)) },
                                    singleLine = true,
                                    colors = dialogTextFieldColors(),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = teamB,
                                    onValueChange = {
                                        teamB = it
                                        errorMessage = null
                                    },
                                    label = { Text("Team 2 Name *", fontSize = 12.sp) },
                                    placeholder = { Text("e.g. Mumbai Strikers", fontSize = 11.sp, color = TextMuted) },
                                    leadingIcon = { Icon(Icons.Default.Groups, contentDescription = null, tint = HawkEyeCyan, modifier = Modifier.size(18.dp)) },
                                    singleLine = true,
                                    colors = dialogTextFieldColors(),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = "MATCH OVERS PER INNINGS:",
                                    color = TextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                // Row 1: 2 ov, 5 ov, 8 ov
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf(2, 5, 8).forEach { opt ->
                                        FilterChip(
                                            selected = overs == opt,
                                            onClick = { overs = opt },
                                            modifier = Modifier.weight(1f),
                                            label = {
                                                Text(
                                                    text = "${opt} ov",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.fillMaxWidth(),
                                                    textAlign = TextAlign.Center
                                                )
                                            },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = CricketGreen,
                                                selectedLabelColor = PitchDark,
                                                containerColor = SurfaceDark,
                                                labelColor = TextSecondary
                                            )
                                        )
                                    }
                                }

                                // Row 2: 10 ov, 15 ov, 20 ov (20 ov fully visible with equal width!)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf(10, 15, 20).forEach { opt ->
                                        FilterChip(
                                            selected = overs == opt,
                                            onClick = { overs = opt },
                                            modifier = Modifier.weight(1f),
                                            label = {
                                                Text(
                                                    text = "${opt} ov",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.fillMaxWidth(),
                                                    textAlign = TextAlign.Center
                                                )
                                            },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = CricketGreen,
                                                selectedLabelColor = PitchDark,
                                                containerColor = SurfaceDark,
                                                labelColor = TextSecondary
                                            )
                                        )
                                    }
                                }

                                // Stepper & Fine-Tuning for Any Custom Overs
                                Surface(
                                    color = Color(0xFF0F172A),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, Color(0xFF1E293B)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Timer,
                                                contentDescription = null,
                                                tint = StadiumGold,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = "Match Length: $overs Overs",
                                                color = StadiumGold,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            IconButton(
                                                onClick = { if (overs > 1) overs-- },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.RemoveCircleOutline,
                                                    contentDescription = "Decrease overs",
                                                    tint = TextSecondary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                            Text(
                                                text = "$overs ov",
                                                color = TextPrimary,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Black,
                                                modifier = Modifier.padding(horizontal = 4.dp)
                                            )
                                            IconButton(
                                                onClick = { if (overs < 50) overs++ },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.AddCircleOutline,
                                                    contentDescription = "Increase overs",
                                                    tint = CricketGreen,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                OutlinedTextField(
                                    value = matchName,
                                    onValueChange = { matchName = it },
                                    label = { Text("Tournament / Match Title", fontSize = 12.sp) },
                                    placeholder = { Text("e.g. Sunday Cup 2026", fontSize = 11.sp, color = TextMuted) },
                                    singleLine = true,
                                    colors = dialogTextFieldColors(),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                // Venue Field with Google Maps Search icon
                                OutlinedTextField(
                                    value = venue,
                                    onValueChange = { venue = it },
                                    label = { Text("Ground / Stadium (मैदान) *", fontSize = 12.sp) },
                                    placeholder = { Text("e.g. Shivaji Park Turf, Sector 7", fontSize = 11.sp, color = TextMuted) },
                                    singleLine = true,
                                    colors = dialogTextFieldColors(),
                                    leadingIcon = {
                                        Icon(Icons.Default.Place, contentDescription = null, tint = StadiumGold, modifier = Modifier.size(18.dp))
                                    },
                                    trailingIcon = {
                                        IconButton(onClick = { showGroundSearchSection = !showGroundSearchSection }) {
                                            Icon(
                                                imageVector = Icons.Default.Search,
                                                contentDescription = "Search Google Maps",
                                                tint = CricketGreen
                                            )
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )

                                // Weird Name Detection & Auto-Clean
                                if (GoogleMapsGroundingService.isWeirdVenueName(venue)) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF2A1C0E))
                                            .border(1.dp, StadiumGold.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "⚠️ Ajeeb Name Detected",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = StadiumGold
                                        )
                                        TextButton(
                                            onClick = {
                                                venue = GoogleMapsGroundingService.suggestCleanFriendlyName(venue, venueAddress)
                                            },
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text("Auto Clean ✨", color = StadiumGold, fontSize = 11.sp, fontWeight = FontWeight.Black)
                                        }
                                    }
                                }

                                // Date & Time Inputs ("date or time bhi")
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = matchDate,
                                        onValueChange = { matchDate = it },
                                        label = { Text("Date (तारीख)", fontSize = 11.sp) },
                                        singleLine = true,
                                        colors = dialogTextFieldColors(),
                                        leadingIcon = {
                                            Icon(Icons.Default.DateRange, contentDescription = null, tint = CricketGreen, modifier = Modifier.size(16.dp))
                                        },
                                        modifier = Modifier.weight(1f)
                                    )

                                    OutlinedTextField(
                                        value = matchTime,
                                        onValueChange = { matchTime = it },
                                        label = { Text("Time (समय)", fontSize = 11.sp) },
                                        singleLine = true,
                                        colors = dialogTextFieldColors(),
                                        leadingIcon = {
                                            Icon(Icons.Default.AccessTime, contentDescription = null, tint = StadiumGold, modifier = Modifier.size(16.dp))
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                // Quick chips for Date and Time
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    AssistChip(
                                        onClick = { matchDate = todayFormatted },
                                        label = { Text("Today", fontSize = 10.sp) },
                                        colors = AssistChipDefaults.assistChipColors(containerColor = PitchSurface)
                                    )
                                    AssistChip(
                                        onClick = {
                                            val cal = java.util.Calendar.getInstance().apply { add(java.util.Calendar.DAY_OF_YEAR, 1) }
                                            matchDate = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(cal.time)
                                        },
                                        label = { Text("Tomorrow", fontSize = 10.sp) },
                                        colors = AssistChipDefaults.assistChipColors(containerColor = PitchSurface)
                                    )
                                    AssistChip(
                                        onClick = { matchTime = "09:00 AM" },
                                        label = { Text("Morning ☀️", fontSize = 10.sp) },
                                        colors = AssistChipDefaults.assistChipColors(containerColor = PitchSurface)
                                    )
                                    AssistChip(
                                        onClick = { matchTime = "05:00 PM" },
                                        label = { Text("Evening 🌆", fontSize = 10.sp) },
                                        colors = AssistChipDefaults.assistChipColors(containerColor = PitchSurface)
                                    )
                                }

                                // Google Maps Grounding Search Button & Expandable Results
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextButton(
                                        onClick = { showGroundSearchSection = !showGroundSearchSection },
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Icon(Icons.Default.Map, contentDescription = null, tint = HawkEyeCyan, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = if (showGroundSearchSection) "Hide Maps Search" else "Search Ground on Google Maps 🗺️",
                                            color = HawkEyeCyan,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                AnimatedVisibility(visible = showGroundSearchSection) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0xFF0F172A), RoundedCornerShape(10.dp))
                                            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(10.dp))
                                            .padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            OutlinedTextField(
                                                value = groundSearchQuery,
                                                onValueChange = { groundSearchQuery = it },
                                                placeholder = { Text("Search city, ground, turf...", fontSize = 11.sp, color = TextMuted) },
                                                singleLine = true,
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(42.dp),
                                                colors = dialogTextFieldColors()
                                            )

                                            Button(
                                                onClick = {
                                                    coroutineScope.launch {
                                                        isSearchingGrounds = true
                                                        val res = GoogleMapsGroundingService.searchGroundsWithMapsGrounding(
                                                            groundSearchQuery.ifBlank { venue }
                                                        )
                                                        groundResults = res
                                                        isSearchingGrounds = false
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = CricketGreen),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.height(42.dp),
                                                contentPadding = PaddingValues(horizontal = 8.dp)
                                            ) {
                                                if (isSearchingGrounds) {
                                                    CircularProgressIndicator(modifier = Modifier.size(14.dp), color = PitchDark, strokeWidth = 2.dp)
                                                } else {
                                                    Text("Find 🔎", color = PitchDark, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                                }
                                            }
                                        }

                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            groundResults.take(3).forEach { ground ->
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(Color(0xFF1E293B))
                                                        .clickable {
                                                            venue = ground.name
                                                            venueAddress = ground.cleanAddress
                                                            if (ground.latitude != null && ground.longitude != null) {
                                                                venueCoordinates = "${ground.latitude},${ground.longitude}"
                                                            }
                                                            showGroundSearchSection = false
                                                        }
                                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(Icons.Default.SportsCricket, contentDescription = null, tint = CricketGreen, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(text = ground.name, color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                        Text(text = ground.cleanAddress, color = TextSecondary, fontSize = 9.sp, maxLines = 1)
                                                    }
                                                    Icon(Icons.Default.Check, contentDescription = "Select", tint = StadiumGold, modifier = Modifier.size(14.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        2 -> {
                            // STEP 2: TOSS CEREMONY (LIVE COIN FLIP + FAIR CHANCE)
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "2. OFFICIAL MATCH TOSS 🪙",
                                            fontSize = 12.sp,
                                            color = StadiumGold,
                                            fontWeight = FontWeight.Black,
                                            letterSpacing = 0.8.sp
                                        )
                                        Text(
                                            text = "Asli sikka uchaal kar fair toss karein",
                                            fontSize = 11.sp,
                                            color = TextSecondary
                                        )
                                    }
                                    TextButton(
                                        onClick = { showManualTossMode = !showManualTossMode },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = if (showManualTossMode) "🪙 Coin Flip" else "📝 Manual",
                                            fontSize = 11.sp,
                                            color = StadiumGold,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                if (!showManualTossMode) {
                                    // 1. Calling Team
                                    Text(
                                        text = "Kaunsi team call karegi?",
                                        color = TextPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        listOf(
                                            1 to teamANameDisplay,
                                            2 to teamBNameDisplay
                                        ).forEach { (choice, name) ->
                                            val isCalling = callingTeamChoice == choice
                                            OutlinedButton(
                                                onClick = { callingTeamChoice = choice },
                                                enabled = !isCoinFlipping,
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(10.dp),
                                                border = BorderStroke(1.2.dp, if (isCalling) StadiumGold else Color(0xFF334155)),
                                                colors = ButtonDefaults.outlinedButtonColors(
                                                    containerColor = if (isCalling) StadiumGold.copy(alpha = 0.15f) else Color(0xFF0F172A)
                                                )
                                            ) {
                                                Text(
                                                    text = name,
                                                    fontWeight = if (isCalling) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isCalling) StadiumGold else TextSecondary,
                                                    fontSize = 12.sp,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }

                                    // 2. Call Side: Heads or Tails
                                    Text(
                                        text = "Unhone kya call kiya?",
                                        color = TextPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        val isHeads = callingCallChoice == "HEADS"
                                        OutlinedButton(
                                            onClick = { callingCallChoice = "HEADS" },
                                            enabled = !isCoinFlipping,
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(10.dp),
                                            border = BorderStroke(1.2.dp, if (isHeads) StadiumGold else Color(0xFF334155)),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                containerColor = if (isHeads) StadiumGold.copy(alpha = 0.15f) else Color(0xFF0F172A)
                                            )
                                        ) {
                                            Text(
                                                text = "👑 HEADS (चित)",
                                                fontWeight = if (isHeads) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isHeads) StadiumGold else TextSecondary,
                                                fontSize = 12.sp
                                            )
                                        }

                                        val isTails = callingCallChoice == "TAILS"
                                        OutlinedButton(
                                            onClick = { callingCallChoice = "TAILS" },
                                            enabled = !isCoinFlipping,
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(10.dp),
                                            border = BorderStroke(1.2.dp, if (isTails) StadiumGold else Color(0xFF334155)),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                containerColor = if (isTails) StadiumGold.copy(alpha = 0.15f) else Color(0xFF0F172A)
                                            )
                                        ) {
                                            Text(
                                                text = "🦁 TAILS (पट)",
                                                fontWeight = if (isTails) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isTails) StadiumGold else TextSecondary,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }

                                    // 3. 3D Animated Coin & Flip Action Button
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            // The 3D Animated Coin
                                            Box(
                                                modifier = Modifier
                                                    .size(105.dp)
                                                    .graphicsLayer {
                                                        translationY = coinTranslationY.value
                                                        rotationY = coinRotationY.value
                                                        cameraDistance = 16f * density
                                                    }
                                                    .shadow(12.dp, CircleShape)
                                                    .clip(CircleShape)
                                                    .background(
                                                        Brush.radialGradient(
                                                            colors = listOf(
                                                                Color(0xFFFFEE88),
                                                                StadiumGold,
                                                                Color(0xFFB45309),
                                                                Color(0xFF78350F)
                                                            )
                                                        )
                                                    )
                                                    .border(3.dp, Color(0xFFFEF08A), CircleShape)
                                                    .clickable(enabled = !isCoinFlipping) { doLiveCoinToss() },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.Center
                                                ) {
                                                    Text(
                                                        text = if (currentCoinSide.value == "HEADS") "👑" else "🦁",
                                                        fontSize = 32.sp
                                                    )
                                                    Text(
                                                        text = currentCoinSide.value,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Black,
                                                        letterSpacing = 1.sp,
                                                        color = Color(0xFF451A03),
                                                        fontFamily = FontFamily.SansSerif
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(10.dp))

                                            // Prominent Flip Button
                                            Button(
                                                onClick = { doLiveCoinToss() },
                                                enabled = !isCoinFlipping,
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = StadiumGold,
                                                    contentColor = PitchDark
                                                ),
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                if (isCoinFlipping) {
                                                    CircularProgressIndicator(
                                                        modifier = Modifier.size(18.dp),
                                                        color = PitchDark,
                                                        strokeWidth = 2.dp
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text("Sikka hawa me uchhal raha hai... 🪙", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                } else {
                                                    Icon(Icons.Default.Casino, contentDescription = null, modifier = Modifier.size(18.dp))
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = if (coinTossWinner == null) "🪙 SIKKA UCHHAALEIN (FLIP COIN)" else "🔄 PHIR SE TOSS KAREIN (RE-FLIP)",
                                                        fontWeight = FontWeight.Black,
                                                        fontSize = 13.sp
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // Winner Celebration Card
                                    if (coinTossWinner != null && coinTossOutcome != null) {
                                        Surface(
                                            color = CricketGreen.copy(alpha = 0.15f),
                                            border = BorderStroke(1.dp, CricketGreen),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = CricketGreen, modifier = Modifier.size(20.dp))
                                                Column {
                                                    Text(
                                                        text = "🏆 $coinTossWinner ne Toss Jeeta!",
                                                        color = CricketGreen,
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Black
                                                    )
                                                    Text(
                                                        text = "Coin outcome: $coinTossOutcome aaya",
                                                        color = TextPrimary,
                                                        fontSize = 11.sp
                                                    )
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    // Manual Selection Mode
                                    Text(
                                        text = "Kaunsi Team Toss Jeeti? (Manual Selection)",
                                        color = TextPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Card(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable { tossWinnerChoice = 1 },
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (tossWinnerChoice == 1) StadiumGold.copy(alpha = 0.15f) else Color(0xFF1E293B)
                                            ),
                                            border = BorderStroke(
                                                if (tossWinnerChoice == 1) 1.5.dp else 0.5.dp,
                                                if (tossWinnerChoice == 1) StadiumGold else Color(0xFF334155)
                                            )
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(12.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Text("🪙 Team 1", color = TextMuted, fontSize = 10.sp)
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(teamANameDisplay, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                            }
                                        }

                                        Card(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable { tossWinnerChoice = 2 },
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (tossWinnerChoice == 2) StadiumGold.copy(alpha = 0.15f) else Color(0xFF1E293B)
                                            ),
                                            border = BorderStroke(
                                                if (tossWinnerChoice == 2) 1.5.dp else 0.5.dp,
                                                if (tossWinnerChoice == 2) StadiumGold else Color(0xFF334155)
                                            )
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(12.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Text("🪙 Team 2", color = TextMuted, fontSize = 10.sp)
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(teamBNameDisplay, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                            }
                                        }
                                    }
                                }

                                Text(
                                    text = "Toss jeet kar $tossWinnerName ne pehle kya chuna?",
                                    color = TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Button(
                                        onClick = { tossElectedChoice = "BAT" },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isTossBat) CricketGreen else Color(0xFF1E293B),
                                            contentColor = if (isTossBat) PitchDark else TextPrimary
                                        ),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Default.SportsCricket, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("BATTING 🏏", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }

                                    Button(
                                        onClick = { tossElectedChoice = "BOWL" },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (!isTossBat) HawkEyeCyan else Color(0xFF1E293B),
                                            contentColor = if (!isTossBat) PitchDark else TextPrimary
                                        ),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Default.SportsBaseball, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("BOWLING ⚾", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }

                                // Toss Result Confirmation Card
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                                    border = BorderStroke(1.dp, Color(0xFF1E293B))
                                ) {
                                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(
                                            text = tossSummaryText,
                                            color = StadiumGold,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Divider(color = Color(0xFF1E293B), thickness = 0.8.dp)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text("🏏 BATTING: ", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                Text(calculatedBattingTeam, color = CricketGreen, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                                            }
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text("⚾ BOWLING: ", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                Text(calculatedBowlingTeam, color = HawkEyeCyan, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        3 -> {
                            // STEP 3: SQUAD & OPENING PLAYERS (NAMES MANDATORY)
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    text = "3. OPENING PLAYERS (NAMES MANDATORY *)",
                                    fontSize = 11.sp,
                                    color = StadiumGold,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp
                                )

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF1E293B))
                                        .padding(8.dp)
                                ) {
                                    Text(
                                        text = "⚠️ \"Batsman 1\" ya \"Bowler 1\" nahi chalega. Khiladiyon ke asali naam likhna anivarya (mandatory) hai.",
                                        color = Color(0xFFFDE047),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "🏏 BATTING: $calculatedBattingTeam",
                                        color = CricketGreen,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    OutlinedButton(
                                        onClick = {
                                            val tmpS = striker
                                            striker = bowler
                                            bowler = tmpS
                                            val tmpSq = teamAPlayersInput
                                            teamAPlayersInput = teamBPlayersInput
                                            teamBPlayersInput = tmpSq
                                        },
                                        modifier = Modifier.height(28.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, StadiumGold),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = StadiumGold)
                                    ) {
                                        Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Swap Players 🔄", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                OutlinedTextField(
                                    value = striker,
                                    onValueChange = {
                                        striker = it
                                        errorMessage = null
                                    },
                                    label = { Text("Striker ($calculatedBattingTeam) *", fontSize = 12.sp) },
                                    placeholder = { Text("e.g. Opening batter for $calculatedBattingTeam", fontSize = 11.sp, color = TextMuted) },
                                    leadingIcon = { Icon(Icons.Default.SportsCricket, contentDescription = null, tint = CricketGreen, modifier = Modifier.size(18.dp)) },
                                    singleLine = true,
                                    colors = dialogTextFieldColors(),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = nonStriker,
                                    onValueChange = {
                                        nonStriker = it
                                        errorMessage = null
                                    },
                                    label = { Text("Non-Striker ($calculatedBattingTeam) *", fontSize = 12.sp) },
                                    placeholder = { Text("e.g. Non-striker for $calculatedBattingTeam", fontSize = 11.sp, color = TextMuted) },
                                    leadingIcon = { Icon(Icons.Default.SportsCricket, contentDescription = null, tint = CricketGreen, modifier = Modifier.size(18.dp)) },
                                    singleLine = true,
                                    colors = dialogTextFieldColors(),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = "⚾ BOWLING: $calculatedBowlingTeam",
                                    color = HawkEyeCyan,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                OutlinedTextField(
                                    value = bowler,
                                    onValueChange = {
                                        bowler = it
                                        errorMessage = null
                                    },
                                    label = { Text("Opening Bowler ($calculatedBowlingTeam) *", fontSize = 12.sp) },
                                    placeholder = { Text("e.g. Opening bowler for $calculatedBowlingTeam", fontSize = 11.sp, color = TextMuted) },
                                    leadingIcon = { Icon(Icons.Default.SportsBaseball, contentDescription = null, tint = HawkEyeCyan, modifier = Modifier.size(18.dp)) },
                                    singleLine = true,
                                    colors = dialogTextFieldColors(),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                OutlinedTextField(
                                    value = teamAPlayersInput,
                                    onValueChange = { teamAPlayersInput = it },
                                    label = { Text("$cleanTeamA Squad (comma-separated)", fontSize = 11.sp) },
                                    placeholder = { Text("Player 1, Player 2, Player 3...", fontSize = 11.sp, color = TextMuted) },
                                    singleLine = true,
                                    colors = dialogTextFieldColors(),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = teamBPlayersInput,
                                    onValueChange = { teamBPlayersInput = it },
                                    label = { Text("$cleanTeamB Squad (comma-separated)", fontSize = 11.sp) },
                                    placeholder = { Text("Player 1, Player 2, Player 3...", fontSize = 11.sp, color = TextMuted) },
                                    singleLine = true,
                                    colors = dialogTextFieldColors(),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    OutlinedTextField(
                                        value = pin,
                                        onValueChange = { if (it.length <= 6) pin = it.filter { ch -> ch.isDigit() } },
                                        label = { Text("Match Security PIN (4-6 Digits) *", fontSize = 11.sp) },
                                        placeholder = { Text("Set 4-6 digit secret PIN", fontSize = 11.sp, color = TextMuted) },
                                        visualTransformation = PasswordVisualTransformation(),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                        singleLine = true,
                                        colors = dialogTextFieldColors(),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Text(
                                        text = "🔒 Is Secret PIN se sirf match officials aur scorer login kar sakenge.",
                                        color = TextMuted,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Bottom Navigation Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentStep > 1) {
                        OutlinedButton(
                            onClick = {
                                errorMessage = null
                                currentStep--
                            },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFF334155))
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Back", color = TextSecondary)
                        }
                    } else {
                        TextButton(onClick = onDismiss) {
                            Text("Cancel", color = TextSecondary)
                        }
                    }

                    if (currentStep < 3) {
                        Button(
                            onClick = {
                                if (isCoinFlipping) return@Button
                                if (currentStep == 1) {
                                    if (cleanTeamA.isBlank()) {
                                        errorMessage = "Team 1 ka naam likhna anivarya hai!"
                                        return@Button
                                    }
                                    if (cleanTeamB.isBlank()) {
                                        errorMessage = "Team 2 ka naam likhna anivarya hai!"
                                        return@Button
                                    }
                                    if (cleanTeamA.equals(cleanTeamB, ignoreCase = true)) {
                                        errorMessage = "Dono teams ka naam alag hona chahiye!"
                                        return@Button
                                    }
                                }
                                errorMessage = null
                                currentStep++
                            },
                            enabled = !isCoinFlipping,
                            colors = ButtonDefaults.buttonColors(containerColor = StadiumGold),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Next: ${if (currentStep == 1) "Toss Karein 🪙" else "Squad Chunein 🏏"}", color = PitchDark, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = PitchDark, modifier = Modifier.size(16.dp))
                        }
                    } else {
                        Button(
                            onClick = {
                                val cleanStriker = striker.trim()
                                val cleanNonStriker = nonStriker.trim()
                                val cleanBowler = bowler.trim()

                                if (cleanStriker.isBlank() || cleanStriker.matches(Regex("(?i)batsman\\s*\\d*"))) {
                                    errorMessage = "Striker ballebaaz ka asali naam likhein (Batsman 1 nahi chalega)!"
                                    return@Button
                                }
                                if (cleanNonStriker.isBlank() || cleanNonStriker.matches(Regex("(?i)batsman\\s*\\d*"))) {
                                    errorMessage = "Non-striker ballebaaz ka asali naam likhein (Batsman 2 nahi chalega)!"
                                    return@Button
                                }
                                if (cleanStriker.equals(cleanNonStriker, ignoreCase = true)) {
                                    errorMessage = "Striker aur Non-striker ka naam alag hona chahiye!"
                                    return@Button
                                }
                                if (cleanBowler.isBlank() || cleanBowler.matches(Regex("(?i)bowler\\s*\\d*"))) {
                                    errorMessage = "Opening bowler ka asali naam likhein (Bowler 1 nahi chalega)!"
                                    return@Button
                                }
                                val finalPin = if (pin.trim().length >= 4) pin.trim() else "200910"
                                val isBattingTeamA = calculatedBattingTeam.equals(cleanTeamA, ignoreCase = true)
                                val finalSquadA = if (isBattingTeamA) {
                                    teamAPlayersInput.trim().ifBlank { "$cleanStriker, $cleanNonStriker" }
                                } else {
                                    teamAPlayersInput.trim().ifBlank { cleanBowler }
                                }
                                val finalSquadB = if (isBattingTeamA) {
                                    teamBPlayersInput.trim().ifBlank { cleanBowler }
                                } else {
                                    teamBPlayersInput.trim().ifBlank { "$cleanStriker, $cleanNonStriker" }
                                }

                                onCreateMatch(
                                    com.example.data.model.CreateMatchParams(
                                        name = matchName.trim().ifBlank { "Local Cricket Tournament" },
                                        teamA = cleanTeamA,
                                        teamB = cleanTeamB,
                                        overs = overs,
                                        striker = cleanStriker,
                                        nonStriker = cleanNonStriker,
                                        bowler = cleanBowler,
                                        venue = venue.trim().ifBlank { "Ground Turf" },
                                        pin = finalPin,
                                        battingTeam = calculatedBattingTeam,
                                        bowlingTeam = calculatedBowlingTeam,
                                        tossDetail = tossSummaryText,
                                        teamAPlayers = finalSquadA,
                                        teamBPlayers = finalSquadB,
                                        matchDate = matchDate.trim().ifBlank { todayFormatted },
                                        matchTime = matchTime.trim().ifBlank { nowFormatted },
                                        venueAddress = venueAddress.trim(),
                                        venueCoordinates = venueCoordinates.trim()
                                    )
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CricketGreen),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.SportsCricket, contentDescription = null, tint = PitchDark, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("START LIVE MATCH 🚀", color = PitchDark, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun dialogTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = CricketGreen,
    unfocusedBorderColor = Color(0xFF334155),
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    focusedLabelColor = CricketGreen,
    unfocusedLabelColor = TextSecondary
)
