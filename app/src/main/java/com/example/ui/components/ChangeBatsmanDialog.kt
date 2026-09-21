package com.example.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
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
fun ChangeBatsmanDialog(
    match: MatchEntity,
    onDismiss: () -> Unit,
    onConfirmChange: (isStriker: Boolean, newName: String) -> Unit
) {
    var targetStriker by remember { mutableStateOf(true) }
    var nameInput by remember { mutableStateOf("") }

    val battingSquad = remember(match) {
        val rawSquad = if (match.currentInnings == 1) match.teamAPlayers else match.teamBPlayers
        rawSquad.split(",")
            .map { it.trim() }
            .filter {
                it.isNotBlank() &&
                !it.equals(match.strikerName, ignoreCase = true) &&
                !it.equals(match.nonStrikerName, ignoreCase = true)
            }
            .distinct()
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 14.dp)
                .testTag("change_batsman_dialog"),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = PitchDark),
            border = androidx.compose.foundation.BorderStroke(1.dp, CricketGreen.copy(alpha = 0.5f))
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
                                .background(CricketGreen.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                tint = CricketGreen,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Change Batter / Ballebaaz Badlein",
                                color = TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Retired Hurt, Substitution, ya Naam Theek karein",
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

                // Choose which batter to edit
                Text(
                    text = "SELECT BATTER TO CHANGE:",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = StadiumGold,
                    letterSpacing = 0.5.sp
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { targetStriker = true },
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (targetStriker) CricketGreen.copy(alpha = 0.2f) else Color(0xFF1E293B)
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (targetStriker) CricketGreen else Color(0xFF334155)
                        )
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Striker", fontSize = 10.sp, color = TextMuted)
                            Text(
                                text = match.strikerName,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (targetStriker) CricketGreen else TextPrimary
                            )
                            Text(
                                text = "${match.strikerRuns} (${match.strikerBalls}b)",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { targetStriker = false },
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (!targetStriker) CricketGreen.copy(alpha = 0.2f) else Color(0xFF1E293B)
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (!targetStriker) CricketGreen else Color(0xFF334155)
                        )
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Non-Striker", fontSize = 10.sp, color = TextMuted)
                            Text(
                                text = match.nonStrikerName,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (!targetStriker) CricketGreen else TextPrimary
                            )
                            Text(
                                text = "${match.nonStrikerRuns} (${match.nonStrikerBalls}b)",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }

                // New Name Input
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "NEW BATTER NAME / NAYA NAAM:",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = StadiumGold,
                        letterSpacing = 0.5.sp
                    )
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        placeholder = { Text("e.g. Virat, Hardik, Shubman...", fontSize = 12.sp, color = TextMuted) },
                        singleLine = true,
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = null, tint = CricketGreen)
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = CricketGreen,
                            unfocusedBorderColor = Color(0xFF334155),
                            cursorColor = CricketGreen
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("change_batsman_input")
                    )
                }

                // Squad suggestions
                if (battingSquad.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "SQUAD BENCH PLAYERS:",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(battingSquad) { player ->
                                val isSel = nameInput.equals(player, ignoreCase = true)
                                FilterChip(
                                    selected = isSel,
                                    onClick = { nameInput = player },
                                    label = { Text(player, fontSize = 11.sp) },
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

                Spacer(modifier = Modifier.height(4.dp))

                // Confirm Button
                Button(
                    onClick = {
                        val finalName = nameInput.trim().ifBlank {
                            if (targetStriker) match.strikerName else match.nonStrikerName
                        }
                        onConfirmChange(targetStriker, finalName)
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("confirm_change_batsman_btn"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CricketGreen)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = PitchDark,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "SAVE & UPDATE BATSMAN",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = PitchDark
                    )
                }
            }
        }
    }
}
