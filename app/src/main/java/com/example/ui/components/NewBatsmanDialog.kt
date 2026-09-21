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
import androidx.compose.material.icons.filled.Warning
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
fun NewBatsmanDialog(
    match: MatchEntity,
    initialWicketType: String = "Bowled",
    onDismiss: () -> Unit,
    onConfirmDismissalAndNewBatsman: (
        dismissedBatsman: String,
        newBatsmanName: String,
        wicketType: String,
        newBatsmanOnStrike: Boolean
    ) -> Unit
) {
    var selectedDismissed by remember { mutableStateOf(match.strikerName) }
    var selectedWicketType by remember { mutableStateOf(initialWicketType) }
    var newBatsmanInput by remember { mutableStateOf("") }
    var newBatsmanOnStrike by remember { mutableStateOf(true) }

    // Batting squad available players
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

    val dismissalModes = listOf("Bowled", "Caught", "LBW", "Run Out", "Stumped", "Hit Wicket")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 12.dp)
                .testTag("new_batsman_dialog"),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = PitchDark),
            border = androidx.compose.foundation.BorderStroke(1.2.dp, DrsOutRed.copy(alpha = 0.8f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
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
                                .background(DrsOutRed.copy(alpha = 0.25f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = DrsOutRed,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "WICKET! Naya Ballebaaz Chunein",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Dismissal & Next Batter In",
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

                // 1. Who got out?
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "1. KAUN OUT HUA? (WHO WAS DISMISSED?):",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = StadiumGold,
                        letterSpacing = 0.5.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Striker
                        val isStrikerSelected = selectedDismissed == match.strikerName
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedDismissed = match.strikerName },
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isStrikerSelected) DrsOutRed.copy(alpha = 0.25f) else Color(0xFF1E293B)
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isStrikerSelected) DrsOutRed else Color(0xFF334155)
                            )
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("🏏 Striker (Out)", fontSize = 10.sp, color = TextMuted)
                                Text(
                                    text = match.strikerName,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isStrikerSelected) Color.White else TextPrimary
                                )
                                Text(
                                    text = "${match.strikerRuns} (${match.strikerBalls}b)",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                        }

                        // Non-Striker
                        val isNonStrikerSelected = selectedDismissed == match.nonStrikerName
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedDismissed = match.nonStrikerName },
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isNonStrikerSelected) DrsOutRed.copy(alpha = 0.25f) else Color(0xFF1E293B)
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isNonStrikerSelected) DrsOutRed else Color(0xFF334155)
                            )
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Non-Striker (e.g. Run Out)", fontSize = 10.sp, color = TextMuted)
                                Text(
                                    text = match.nonStrikerName,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isNonStrikerSelected) Color.White else TextPrimary
                                )
                                Text(
                                    text = "${match.nonStrikerRuns} (${match.nonStrikerBalls}b)",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }

                // 2. Dismissal Mode
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "2. HOW OUT? (DISMISSAL TYPE):",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = StadiumGold,
                        letterSpacing = 0.5.sp
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(dismissalModes) { mode ->
                            val isSel = selectedWicketType.equals(mode, ignoreCase = true)
                            FilterChip(
                                selected = isSel,
                                onClick = { selectedWicketType = mode },
                                label = { Text(mode, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = DrsOutRed,
                                    selectedLabelColor = Color.White,
                                    containerColor = Color(0xFF1E293B),
                                    labelColor = TextPrimary
                                ),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }

                // 3. New Batsman Input
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "3. NAYA BALLEBAAZ (NEW BATSMAN NAME):",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = StadiumGold,
                        letterSpacing = 0.5.sp
                    )
                    OutlinedTextField(
                        value = newBatsmanInput,
                        onValueChange = { newBatsmanInput = it },
                        placeholder = { Text("Enter batsman name (e.g. Rohit, Adarsh, Kohli...)", fontSize = 12.sp, color = TextMuted) },
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
                            .testTag("new_batsman_input")
                    )
                }

                // Squad quick pick
                if (battingSquad.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "BATTING TEAM SQUAD (QUICK PICK):",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(battingSquad) { player ->
                                val isSel = newBatsmanInput.equals(player, ignoreCase = true)
                                FilterChip(
                                    selected = isSel,
                                    onClick = { newBatsmanInput = player },
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

                // 4. Who takes strike?
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "4. STRIKE KISKI HOGI? (WHO TAKES STRIKE?):",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = StadiumGold,
                        letterSpacing = 0.5.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = newBatsmanOnStrike,
                            onClick = { newBatsmanOnStrike = true },
                            label = { Text("New Batsman Takes Strike", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = HawkEyeCyan,
                                selectedLabelColor = PitchDark,
                                containerColor = Color(0xFF1E293B),
                                labelColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                        FilterChip(
                            selected = !newBatsmanOnStrike,
                            onClick = { newBatsmanOnStrike = false },
                            label = { Text("Other Batsman on Strike", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = HawkEyeCyan,
                                selectedLabelColor = PitchDark,
                                containerColor = Color(0xFF1E293B),
                                labelColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Confirm Action Button
                Button(
                    onClick = {
                        val nextName = newBatsmanInput.trim().ifBlank {
                            "Batsman ${match.wickets + 3}"
                        }
                        onConfirmDismissalAndNewBatsman(
                            selectedDismissed,
                            nextName,
                            selectedWicketType,
                            newBatsmanOnStrike
                        )
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("confirm_wicket_and_new_batsman_btn"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DrsOutRed)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "CONFIRM WICKET & BRING BATTER IN",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }
            }
        }
    }
}
