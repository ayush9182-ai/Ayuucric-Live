package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.SportsCricket
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DeviceRole
import com.example.ui.theme.CricketGreen
import com.example.ui.theme.DrsOutRed
import com.example.ui.theme.HawkEyeCyan
import com.example.ui.theme.PitchCard
import com.example.ui.theme.PitchDark
import com.example.ui.theme.PitchSurface
import com.example.ui.theme.StadiumGold
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScorerControllerSheet(
    onDismiss: () -> Unit,
    onRecordBall: (runs: Int, isWicket: Boolean, wicketType: String, extraType: String, shotAngle: Float, pitchZone: String) -> Unit,
    onUndoLastDelivery: () -> Unit,
    onSwitchStriker: () -> Unit,
    onRequestDrs: () -> Unit,
    currentRole: DeviceRole,
    onOpenRoleDialog: () -> Unit,
    strikerName: String,
    bowlerName: String,
    onChangeBowler: () -> Unit = {},
    onChangeBatsman: () -> Unit = {},
    onOpenNewBatsmanDialog: (String) -> Unit = {}
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var selectedRuns by remember { mutableIntStateOf(0) }
    var isWicketSelected by remember { mutableStateOf(false) }
    var selectedWicketType by remember { mutableStateOf("Bowled") }
    var selectedExtra by remember { mutableStateOf("None") }
    var selectedPitchZone by remember { mutableStateOf("Good Length") }
    var selectedShotDirection by remember { mutableStateOf("Covers") }
    var shotAngle by remember { mutableFloatStateOf(45f) }

    val isAuthorized = currentRole == DeviceRole.OFFICIAL_SCORER

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = PitchSurface,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .testTag("scorer_controller_sheet")
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.SportsCricket,
                        contentDescription = "Scorer",
                        tint = if (isAuthorized) CricketGreen else StadiumGold,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = if (isAuthorized) "Phone 3: Official Scorer Keypad" else "Scorer Console (View Only)",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = if (isAuthorized) "Authorized to count runs & wickets" else "Locked: Official Scorer access required",
                            fontSize = 11.sp,
                            color = if (isAuthorized) CricketGreen else StadiumGold
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Non-authorized warning banner if spectator
            if (!isAuthorized) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1E293B))
                        .border(1.dp, StadiumGold.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = StadiumGold,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Permission Locked for Spectators",
                            color = StadiumGold,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "As per match rules, only Phone 3 (Official Scorer) can count runs, wickets, and extras. Spectators can watch live scores.",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                    Button(
                        onClick = onOpenRoleDialog,
                        colors = ButtonDefaults.buttonColors(containerColor = StadiumGold),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Authorize Phone 3 with Official PIN", color = PitchDark, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Striker & Bowler Ticker
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(PitchCard)
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(enabled = isAuthorized) {
                            onChangeBatsman()
                            onDismiss()
                        }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Striker", fontSize = 10.sp, color = TextMuted)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("✏️ Edit", fontSize = 9.sp, color = CricketGreen)
                    }
                    Text(
                        text = strikerName,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = StadiumGold,
                        maxLines = 1
                    )
                }

                IconButton(
                    onClick = onSwitchStriker,
                    enabled = isAuthorized,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color(0xFF1E293B))
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = "Rotate Striker",
                        tint = if (isAuthorized) CricketGreen else Color.Gray,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(enabled = isAuthorized) {
                            onChangeBowler()
                            onDismiss()
                        },
                    horizontalAlignment = Alignment.End
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🎳 Change", fontSize = 9.sp, color = StadiumGold)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Bowler", fontSize = 10.sp, color = TextMuted)
                    }
                    Text(
                        text = bowlerName,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Runs Selector Grid (0, 1, 2, 3, 4, 6)
            Text(
                text = "COUNT RUNS SCORED",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 0.8.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val runsList = listOf(0, 1, 2, 3, 4, 6)
                runsList.forEach { runs ->
                    val isSelected = selectedRuns == runs && !isWicketSelected
                    val btnColor = when (runs) {
                        4 -> CricketGreen
                        6 -> StadiumGold
                        else -> Color(0xFF334155)
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isSelected) btnColor else PitchCard
                            )
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) btnColor else Color(0xFF1E293B),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable(enabled = isAuthorized) {
                                selectedRuns = runs
                                isWicketSelected = false
                            }
                            .testTag("scorer_run_btn_$runs"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (runs == 0) "•" else "$runs",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isSelected) PitchDark else TextPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Extras & Wickets Action Row
            Text(
                text = "EXTRAS & DISMISSAL",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 0.8.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val extras = listOf("None", "Wide", "NoBall", "LegBye", "Bye")
                extras.forEach { ext ->
                    FilterChip(
                        selected = selectedExtra == ext,
                        onClick = { if (isAuthorized) selectedExtra = ext },
                        label = { Text(if (ext == "None") "Fair" else ext, fontSize = 11.sp, maxLines = 1) },
                        enabled = isAuthorized,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = StadiumGold.copy(alpha = 0.2f),
                            selectedLabelColor = StadiumGold
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Wicket Toggle Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(enabled = isAuthorized) {
                            isWicketSelected = !isWicketSelected
                        },
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isWicketSelected) DrsOutRed else Color(0xFF201317)
                    ),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(
                            if (isWicketSelected) DrsOutRed else Color(0xFF3F1923)
                        )
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isWicketSelected) "✓ WICKET FALLEN" else "WICKET",
                            color = if (isWicketSelected) Color.White else DrsOutRed,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (isWicketSelected) {
                    val wTypes = listOf("Bowled", "Caught", "LBW", "Run Out", "Stumped")
                    wTypes.take(3).forEach { wt ->
                        FilterChip(
                            selected = selectedWicketType == wt,
                            onClick = { if (isAuthorized) selectedWicketType = wt },
                            label = { Text(wt, fontSize = 11.sp) },
                            enabled = isAuthorized,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = DrsOutRed.copy(alpha = 0.3f),
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Pitch zone & Shot Direction
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val zones = listOf("Yorker", "Good Length", "Short", "Full")
                zones.forEach { zone ->
                    FilterChip(
                        selected = selectedPitchZone == zone,
                        onClick = { if (isAuthorized) selectedPitchZone = zone },
                        label = { Text(zone, fontSize = 10.sp) },
                        enabled = isAuthorized,
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CricketGreen.copy(alpha = 0.2f),
                            selectedLabelColor = CricketGreen
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Auxiliary Actions (Undo, Refer to DRS)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        onUndoLastDelivery()
                        onDismiss()
                    },
                    enabled = isAuthorized,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.Undo, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Undo Ball", fontSize = 11.sp)
                }

                OutlinedButton(
                    onClick = {
                        onRequestDrs()
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.Policy, contentDescription = null, tint = HawkEyeCyan, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Call DRS", color = HawkEyeCyan, fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Submit Ball Button
            Button(
                onClick = {
                    if (isAuthorized) {
                        if (isWicketSelected) {
                            onOpenNewBatsmanDialog(selectedWicketType)
                        } else {
                            onRecordBall(
                                selectedRuns,
                                false,
                                selectedWicketType,
                                selectedExtra,
                                shotAngle,
                                selectedPitchZone
                            )
                        }
                        onDismiss()
                    }
                },
                enabled = isAuthorized,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("submit_ball_record_btn"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isWicketSelected) DrsOutRed else CricketGreen
                )
            ) {
                Text(
                    text = if (!isAuthorized) "LOCK: SCORER PHONE ONLY"
                    else if (isWicketSelected) "⚡ RECORD WICKET & NEW BATTER ➔"
                    else "RECORD $selectedRuns RUN(S)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isWicketSelected) Color.White else PitchDark
                )
            }
        }
    }
}
