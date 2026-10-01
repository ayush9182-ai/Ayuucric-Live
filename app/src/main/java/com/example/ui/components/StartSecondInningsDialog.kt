package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.SportsBaseball
import androidx.compose.material.icons.filled.SportsCricket
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.MatchEntity
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartSecondInningsDialog(
    match: MatchEntity,
    onDismiss: () -> Unit,
    onConfirmStartSecondInnings: (striker: String, nonStriker: String, bowler: String) -> Unit
) {
    val firstInningsBatTeam = match.battingTeam
    val newBattingTeam = match.bowlingTeam
    val target = match.score + 1
    val rrr = if (match.totalOvers > 0) (target * 6f) / (match.totalOvers * 6) else 0f

    val isNewBattingTeamA = newBattingTeam.equals(match.teamA, ignoreCase = true)
    val chasingSquad = remember(match) {
        val rawSquad = if (isNewBattingTeamA) match.teamAPlayers else match.teamBPlayers
        rawSquad.split(",").map { it.trim() }.filter { it.isNotBlank() }.distinct()
    }
    val defendingSquad = remember(match) {
        val rawSquad = if (isNewBattingTeamA) match.teamBPlayers else match.teamAPlayers
        rawSquad.split(",").map { it.trim() }.filter { it.isNotBlank() }.distinct()
    }

    var strikerInput by remember {
        mutableStateOf(chasingSquad.getOrNull(0) ?: "Batter 1")
    }
    var nonStrikerInput by remember {
        mutableStateOf(chasingSquad.getOrNull(1) ?: "Batter 2")
    }
    var bowlerInput by remember {
        mutableStateOf(defendingSquad.getOrNull(0) ?: match.strikerName.ifBlank { "Opening Bowler" })
    }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = PitchSurface),
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, StadiumGold.copy(alpha = 0.6f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .testTag("start_second_innings_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(StadiumGold.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🏏", fontSize = 18.sp)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Start 2nd Innings",
                                color = TextPrimary,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "दूसरी पारी शुरू करें • Official Scorer",
                                color = StadiumGold,
                                fontSize = 11.sp
                            )
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Target Summary Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF0F172A))
                        .border(1.dp, Color(0xFF334155), RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "1st Innings:",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "$firstInningsBatTeam ${match.score}/${match.wickets} (${match.legalBalls / 6}.${match.legalBalls % 6}/${match.totalOvers} ov)",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Divider(color = Color(0xFF1E293B), thickness = 1.dp)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "TARGET FOR $newBattingTeam:",
                                color = StadiumGold,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "$target RUNS",
                                color = CricketGreen,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black
                            )
                        }

                        Text(
                            text = "Equation: $newBattingTeam need $target runs in ${match.totalOvers * 6} balls (RRR: ${"%.2f".format(rrr)})",
                            color = HawkEyeCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Striker Batsman
                Text(
                    text = "1. Opening Striker ($newBattingTeam) *",
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = strikerInput,
                    onValueChange = {
                        strikerInput = it
                        errorMessage = null
                    },
                    placeholder = { Text("Striker Batsman Name", fontSize = 12.sp, color = TextMuted) },
                    leadingIcon = { Icon(Icons.Default.SportsCricket, contentDescription = null, tint = CricketGreen, modifier = Modifier.size(18.dp)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CricketGreen,
                        unfocusedBorderColor = Color(0xFF475569),
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("second_inn_striker_input")
                )

                if (chasingSquad.isNotEmpty()) {
                    LazyRow(
                        modifier = Modifier.padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(chasingSquad) { player ->
                            FilterChip(
                                selected = strikerInput.equals(player, ignoreCase = true),
                                onClick = { strikerInput = player },
                                label = { Text(player, fontSize = 10.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CricketGreen.copy(alpha = 0.25f),
                                    selectedLabelColor = CricketGreen
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Non-Striker Batsman
                Text(
                    text = "2. Opening Non-Striker ($newBattingTeam) *",
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = nonStrikerInput,
                    onValueChange = {
                        nonStrikerInput = it
                        errorMessage = null
                    },
                    placeholder = { Text("Non-Striker Batsman Name", fontSize = 12.sp, color = TextMuted) },
                    leadingIcon = { Icon(Icons.Default.SportsCricket, contentDescription = null, tint = StadiumGold, modifier = Modifier.size(18.dp)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = StadiumGold,
                        unfocusedBorderColor = Color(0xFF475569),
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("second_inn_non_striker_input")
                )

                if (chasingSquad.isNotEmpty()) {
                    LazyRow(
                        modifier = Modifier.padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(chasingSquad) { player ->
                            FilterChip(
                                selected = nonStrikerInput.equals(player, ignoreCase = true),
                                onClick = { nonStrikerInput = player },
                                label = { Text(player, fontSize = 10.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = StadiumGold.copy(alpha = 0.25f),
                                    selectedLabelColor = StadiumGold
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Opening Bowler
                Text(
                    text = "3. Opening Bowler ($firstInningsBatTeam) *",
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = bowlerInput,
                    onValueChange = {
                        bowlerInput = it
                        errorMessage = null
                    },
                    placeholder = { Text("Opening Bowler Name", fontSize = 12.sp, color = TextMuted) },
                    leadingIcon = { Icon(Icons.Default.SportsBaseball, contentDescription = null, tint = HawkEyeCyan, modifier = Modifier.size(18.dp)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = HawkEyeCyan,
                        unfocusedBorderColor = Color(0xFF475569),
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("second_inn_bowler_input")
                )

                if (defendingSquad.isNotEmpty()) {
                    LazyRow(
                        modifier = Modifier.padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(defendingSquad) { player ->
                            FilterChip(
                                selected = bowlerInput.equals(player, ignoreCase = true),
                                onClick = { bowlerInput = player },
                                label = { Text(player, fontSize = 10.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = HawkEyeCyan.copy(alpha = 0.25f),
                                    selectedLabelColor = HawkEyeCyan
                                )
                            )
                        }
                    }
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage ?: "",
                        color = DrsOutRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Confirm Button
                Button(
                    onClick = {
                        val s = strikerInput.trim()
                        val ns = nonStrikerInput.trim()
                        val b = bowlerInput.trim()
                        if (s.isBlank()) {
                            errorMessage = "Striker batsman ka naam likhein!"
                            return@Button
                        }
                        if (ns.isBlank()) {
                            errorMessage = "Non-striker batsman ka naam likhein!"
                            return@Button
                        }
                        if (s.equals(ns, ignoreCase = true)) {
                            errorMessage = "Striker aur non-striker alag-alag khiladi hone chahiye!"
                            return@Button
                        }
                        if (b.isBlank()) {
                            errorMessage = "Opening bowler ka naam likhein!"
                            return@Button
                        }
                        onConfirmStartSecondInnings(s, ns, b)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CricketGreen),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("confirm_start_second_innings_btn")
                ) {
                    Text(
                        text = "🚀 Start 2nd Innings (Target $target)",
                        color = PitchDark,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}
