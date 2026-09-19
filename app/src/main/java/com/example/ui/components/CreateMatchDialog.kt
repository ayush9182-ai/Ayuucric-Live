package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.SportsCricket
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CricketGreen
import com.example.ui.theme.PitchDark
import com.example.ui.theme.StadiumGold
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun CreateMatchDialog(
    onDismiss: () -> Unit,
    onCreateMatch: (
        name: String,
        teamA: String,
        teamB: String,
        overs: Int,
        striker: String,
        nonStriker: String,
        bowler: String,
        venue: String,
        pin: String
    ) -> Unit
) {
    var matchName by remember { mutableStateOf("Local Cricket Match") }
    var teamA by remember { mutableStateOf("Team A") }
    var teamB by remember { mutableStateOf("Team B") }
    var overs by remember { mutableIntStateOf(10) }
    var striker by remember { mutableStateOf("Batsman 1") }
    var nonStriker by remember { mutableStateOf("Batsman 2") }
    var bowler by remember { mutableStateOf("Bowler 1") }
    var venue by remember { mutableStateOf("Local Ground") }
    var pin by remember { mutableStateOf("1234") }

    val overOptions = listOf(5, 8, 10, 12, 16, 20)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = PitchDark,
        shape = RoundedCornerShape(20.dp),
        title = {
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
                        text = "New Local Cricket Match",
                        color = TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Setup game & assign 4 official phones",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = matchName,
                    onValueChange = { matchName = it },
                    label = { Text("Match / Tournament Name", fontSize = 12.sp) },
                    singleLine = true,
                    colors = dialogTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = teamA,
                        onValueChange = { teamA = it },
                        label = { Text("Team 1 (Batting)", fontSize = 11.sp) },
                        singleLine = true,
                        colors = dialogTextFieldColors(),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = teamB,
                        onValueChange = { teamB = it },
                        label = { Text("Team 2 (Bowling)", fontSize = 11.sp) },
                        singleLine = true,
                        colors = dialogTextFieldColors(),
                        modifier = Modifier.weight(1f)
                    )
                }

                Text(
                    text = "TOTAL OVERS PER INNINGS:",
                    color = StadiumGold,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    overOptions.forEach { opt ->
                        FilterChip(
                            selected = overs == opt,
                            onClick = { overs = opt },
                            label = { Text("${opt} ov", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CricketGreen,
                                selectedLabelColor = PitchDark,
                                containerColor = SurfaceDark,
                                labelColor = TextSecondary
                            )
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = striker,
                        onValueChange = { striker = it },
                        label = { Text("Opening Striker", fontSize = 11.sp) },
                        singleLine = true,
                        colors = dialogTextFieldColors(),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = nonStriker,
                        onValueChange = { nonStriker = it },
                        label = { Text("Non-Striker", fontSize = 11.sp) },
                        singleLine = true,
                        colors = dialogTextFieldColors(),
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = bowler,
                    onValueChange = { bowler = it },
                    label = { Text("Opening Bowler", fontSize = 12.sp) },
                    singleLine = true,
                    colors = dialogTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = venue,
                        onValueChange = { venue = it },
                        label = { Text("Ground / Venue", fontSize = 11.sp) },
                        singleLine = true,
                        colors = dialogTextFieldColors(),
                        modifier = Modifier.weight(1.3f)
                    )
                    OutlinedTextField(
                        value = pin,
                        onValueChange = { if (it.length <= 4) pin = it },
                        label = { Text("Role PIN", fontSize = 11.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = dialogTextFieldColors(),
                        modifier = Modifier.weight(0.7f)
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1E293B))
                        .padding(8.dp)
                ) {
                    Text(
                        text = "Share this 4-digit PIN ($pin) with Umpire 1, Umpire 2, Scorer & Third Umpire. Spectators can watch directly without PIN.",
                        color = TextMuted,
                        fontSize = 10.sp,
                        lineHeight = 13.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onCreateMatch(
                        matchName,
                        teamA,
                        teamB,
                        overs,
                        striker,
                        nonStriker,
                        bowler,
                        venue,
                        pin
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = StadiumGold),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Start Live Match", color = PitchDark, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
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
