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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
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
import com.example.data.model.BallEventEntity
import com.example.data.model.MatchEntity
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangeBowlerDialog(
    match: MatchEntity,
    recentDeliveries: List<BallEventEntity> = emptyList(),
    onDismiss: () -> Unit,
    onConfirmNewBowler: (newBowlerName: String) -> Unit
) {
    var bowlerInput by remember { mutableStateOf("") }

    // Bowling team squad suggestions
    val bowlingSquad = remember(match) {
        val rawSquad = if (match.currentInnings == 2) match.teamAPlayers else match.teamBPlayers
        rawSquad.split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() && !it.equals(match.bowlerName, ignoreCase = true) }
            .distinct()
    }

    // Previous bowlers who bowled in this match
    val previousBowlers = remember(recentDeliveries, match) {
        recentDeliveries
            .map { it.bowler }
            .filter { it.isNotBlank() && !it.equals(match.bowlerName, ignoreCase = true) }
            .distinct()
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 16.dp)
                .testTag("change_bowler_dialog"),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = PitchDark),
            border = androidx.compose.foundation.BorderStroke(1.dp, StadiumGold.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
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
                                .size(38.dp)
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
                                text = "Change Bowler / Bowler Badlein",
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Over end ya bowling rotation ke liye",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Current Bowler Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF161E2E)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, HawkEyeCyan.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "CURRENT BOWLER",
                                fontSize = 10.sp,
                                color = TextMuted,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = match.bowlerName,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = StadiumGold
                            )
                        }
                        val ov = "${match.bowlerBalls / 6}.${match.bowlerBalls % 6}"
                        val econ = if (match.bowlerBalls > 0) String.format("%.1f", (match.bowlerRuns.toFloat() / match.bowlerBalls) * 6f) else "0.0"
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "$ov overs",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                            Text(
                                text = "${match.bowlerWickets}/${match.bowlerRuns} (Eco $econ)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = CricketGreen
                            )
                        }
                    }
                }

                // Text Input for New Bowler
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "NEW BOWLER NAME / NAYE BOWLER KA NAAM",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = StadiumGold,
                        letterSpacing = 0.8.sp
                    )
                    OutlinedTextField(
                        value = bowlerInput,
                        onValueChange = { bowlerInput = it },
                        placeholder = { Text("e.g. Bumrah, Shami, Adarsh, Vikrant...", fontSize = 13.sp, color = TextMuted) },
                        singleLine = true,
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = null, tint = StadiumGold)
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = StadiumGold,
                            unfocusedBorderColor = Color(0xFF334155),
                            cursorColor = StadiumGold
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("new_bowler_input")
                    )
                }

                // Previous bowlers in this match (if any)
                if (previousBowlers.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "PREVIOUS BOWLERS (QUICK SELECT):",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(previousBowlers) { name ->
                                val isSelected = bowlerInput.equals(name, ignoreCase = true)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { bowlerInput = name },
                                    label = { Text(name, fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = StadiumGold,
                                        selectedLabelColor = PitchDark,
                                        containerColor = Color(0xFF1E293B),
                                        labelColor = TextPrimary
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }
                    }
                }

                // Bowling Team Squad (if available)
                if (bowlingSquad.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "${match.bowlingTeam.ifBlank { "Bowling Team" }} SQUAD:",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(bowlingSquad) { name ->
                                val isSelected = bowlerInput.equals(name, ignoreCase = true)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { bowlerInput = name },
                                    label = { Text(name, fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = CricketGreen,
                                        selectedLabelColor = PitchDark,
                                        containerColor = Color(0xFF1E293B),
                                        labelColor = TextPrimary
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }
                    }
                }

                // Common cricket bowler suggestions for gully/local play
                val defaultSuggestions = listOf("Opening Bowler", "Spinner", "Pacer", "Left Arm Fast", "Death Bowler")
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "SUGGESTIONS:",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        defaultSuggestions.take(3).forEach { suggestion ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF1E293B))
                                    .clickable { bowlerInput = suggestion }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = suggestion,
                                    fontSize = 10.sp,
                                    color = TextSecondary,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Confirm Button
                Button(
                    onClick = {
                        val name = bowlerInput.trim().ifBlank { "New Bowler" }
                        onConfirmNewBowler(name)
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("confirm_new_bowler_btn"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = StadiumGold)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = PitchDark,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (bowlerInput.isNotBlank()) "SET ${bowlerInput.uppercase()} AS BOWLER" else "CONFIRM NEW BOWLER",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        color = PitchDark
                    )
                }
            }
        }
    }
}
