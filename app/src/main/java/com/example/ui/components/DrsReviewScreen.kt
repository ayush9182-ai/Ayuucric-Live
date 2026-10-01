package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DeviceRole
import com.example.data.model.DrsReviewState
import com.example.ui.theme.CricketGreen
import com.example.ui.theme.DrsNotOutGreen
import com.example.ui.theme.DrsOutRed
import com.example.ui.theme.DrsUmpireCall
import com.example.ui.theme.HawkEyeCyan
import com.example.ui.theme.PitchCard
import com.example.ui.theme.PitchCardBorder
import com.example.ui.theme.PitchDark
import com.example.ui.theme.PitchSurface
import com.example.ui.theme.StadiumGold
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DrsReviewScreen(
    drsState: DrsReviewState,
    currentRole: DeviceRole,
    onStartReview: (appealType: String) -> Unit,
    onManualParametersChanged: (pitching: String, impact: String, wickets: String, deviation: Float, impactDist: Float) -> Unit,
    onResetDrs: () -> Unit,
    onThirdUmpireDecision: (decision: String, appealType: String, reason: String) -> Unit,
    onOpenRoleDialog: () -> Unit,
    onSelectCameraAngle: (String) -> Unit = {},
    onSetFrameIndex: (Int) -> Unit = {},
    onBackToMatch: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    BackHandler {
        onResetDrs()
        onBackToMatch()
    }

    val scrollState = rememberScrollState()
    val isOfficial = currentRole != DeviceRole.SPECTATOR_VIEWER

    // Manual review parameters state
    var selectedAppealType by remember(drsState.appealType) { mutableStateOf(drsState.appealType.ifBlank { "LBW" }) }
    var manualBatInvolved by remember { mutableStateOf<Boolean?>(null) }
    var manualPitching by remember(drsState.pitching) { mutableStateOf(drsState.pitching) }
    var manualImpact by remember(drsState.impact) { mutableStateOf(drsState.impact) }
    var manualWickets by remember(drsState.wicketsHitting) { mutableStateOf(drsState.wicketsHitting) }
    var manualNoBall by remember(drsState.isFrontFootNoBall) { mutableStateOf(drsState.isFrontFootNoBall) }
    var umpireNotes by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(14.dp)
            .testTag("drs_review_screen")
    ) {
        // 1. Header Card: Authentic Manual DRS & Sidhu Paaji Live Voice Status
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = PitchCard),
            border = BorderStroke(1.2.dp, StadiumGold.copy(alpha = 0.6f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(StadiumGold.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Gavel,
                                contentDescription = "DRS System",
                                tint = StadiumGold,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Manual DRS Review Desk",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = TextPrimary
                            )
                            Text(
                                text = "Third Umpire Official Desk",
                                fontSize = 11.sp,
                                color = StadiumGold,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            onResetDrs()
                            onBackToMatch()
                        },
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFF475569)),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = HawkEyeCyan,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Live Match", color = HawkEyeCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "100% Genuine Manual Adjudication • Third Umpire Console",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = StadiumGold
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Sidhu Paaji Voice Guarantee Banner
                Surface(
                    color = Color(0xFF1E1B4B),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFF6366F1).copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = null,
                            tint = Color(0xFFA5B4FC),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Sidhu Paaji Live Audio: Jab koi DRS leta hai, sabhi phones par aawaz aati hai: 'DRS le liya hai guru! Chak de phatte!'",
                            color = Color(0xFFE0E7FF),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 2. Appeal Triggers (Quick action buttons to take DRS)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = PitchCard),
            border = BorderStroke(1.dp, PitchCardBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "1. TAKE DRS APPEAL (डीआरएस अपील करें)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = StadiumGold
                )
                Text(
                    text = "Select appeal type to initiate review with Sidhu Paaji voice announcement",
                    fontSize = 11.sp,
                    color = TextMuted
                )

                Spacer(modifier = Modifier.height(10.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "LBW" to "LBW Appeal",
                        "CAUGHT_BEHIND" to "Caught Behind",
                        "RUN_OUT" to "Run-Out / Crease",
                        "STUMPED" to "Stumping",
                        "CLEAN_CATCH" to "Fair Catch"
                    ).forEach { (type, label) ->
                        val isSelected = selectedAppealType == type
                        OutlinedButton(
                            onClick = {
                                selectedAppealType = type
                                if (isOfficial) {
                                    onStartReview(type)
                                }
                            },
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (isSelected) CricketGreen.copy(alpha = 0.15f) else Color.Transparent
                            ),
                            border = BorderStroke(1.dp, if (isSelected) CricketGreen else Color(0xFF334155)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) CricketGreen else TextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Primary Take DRS Action Button
                Button(
                    onClick = {
                        onStartReview(selectedAppealType)
                    },
                    enabled = isOfficial,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CricketGreen,
                        disabledContainerColor = Color(0xFF334155)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("take_drs_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = null,
                        tint = PitchDark,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isOfficial) "TAKE DRS NOW (सिद्धू पाजी की लाइव आवाज)" else "DRS Locked (Officials & Captains Only)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isOfficial) PitchDark else TextMuted
                    )
                }

                if (!isOfficial) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Aap Spectator role me hain. DRS sirf Official Umpire / Captain / Scorer le sakte hain.",
                        fontSize = 10.5.sp,
                        color = TextMuted
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 3. Manual Third Umpire Adjudication Panel (or Spectator Live Tracking Card)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = PitchCard),
            border = BorderStroke(1.2.dp, if (isOfficial) StadiumGold.copy(alpha = 0.5f) else PitchCardBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "2. THIRD UMPIRE MANUAL ADJUDICATION",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = StadiumGold
                        )
                        Text(
                            text = "Batter: ${drsState.batsman.ifBlank { "Striker" }} • Bowler: ${drsState.bowler.ifBlank { "Bowler" }} • On-Field: ${drsState.onFieldDecision}",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                    if (isOfficial) {
                        OutlinedButton(
                            onClick = onResetDrs,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFF64748B)),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = TextMuted, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reset", fontSize = 10.5.sp, color = TextMuted)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (isOfficial) {
                    // Manual Step A: No-Ball Check
                    Text(
                        text = "A. Front Foot Delivery Check:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { manualNoBall = false },
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (!manualNoBall) CricketGreen.copy(alpha = 0.15f) else Color.Transparent
                            ),
                            border = BorderStroke(1.dp, if (!manualNoBall) CricketGreen else Color(0xFF334155)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Fair Delivery (Legal)", fontSize = 11.sp, color = if (!manualNoBall) CricketGreen else TextSecondary)
                        }
                        OutlinedButton(
                            onClick = { manualNoBall = true },
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (manualNoBall) DrsOutRed.copy(alpha = 0.15f) else Color.Transparent
                            ),
                            border = BorderStroke(1.dp, if (manualNoBall) DrsOutRed else Color(0xFF334155)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("No Ball (Overstepping)", fontSize = 11.sp, color = if (manualNoBall) DrsOutRed else TextSecondary)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Manual Step B: Bat / Edge Contact
                    Text(
                        text = "B. Bat Contact / Edge:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { manualBatInvolved = true },
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (manualBatInvolved == true) HawkEyeCyan.copy(alpha = 0.15f) else Color.Transparent
                            ),
                            border = BorderStroke(1.dp, if (manualBatInvolved == true) HawkEyeCyan else Color(0xFF334155)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Bat Involved (Edge/Spike)", fontSize = 11.sp, color = if (manualBatInvolved == true) HawkEyeCyan else TextSecondary)
                        }
                        OutlinedButton(
                            onClick = { manualBatInvolved = false },
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (manualBatInvolved == false) StadiumGold.copy(alpha = 0.15f) else Color.Transparent
                            ),
                            border = BorderStroke(1.dp, if (manualBatInvolved == false) StadiumGold else Color(0xFF334155)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("No Bat (Pad First/Clean)", fontSize = 11.sp, color = if (manualBatInvolved == false) StadiumGold else TextSecondary)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Manual Step C: Pitching
                    Text(
                        text = "C. Ball Pitching Line:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "IN_LINE" to "In Line",
                            "OUTSIDE_OFF" to "Outside Off",
                            "OUTSIDE_LEG" to "Outside Leg"
                        ).forEach { (code, label) ->
                            val isSelected = manualPitching == code
                            OutlinedButton(
                                onClick = {
                                    manualPitching = code
                                    onManualParametersChanged(manualPitching, manualImpact, manualWickets, 1.8f, 1.9f)
                                },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (isSelected) CricketGreen.copy(alpha = 0.15f) else Color.Transparent
                                ),
                                border = BorderStroke(1.dp, if (isSelected) CricketGreen else Color(0xFF334155)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 10.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) CricketGreen else TextSecondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Manual Step D: Impact Point
                    Text(
                        text = "D. Point of Impact:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "IN_LINE" to "In Line",
                            "OUTSIDE" to "Outside Off",
                            "NO_SHOT" to "No Shot Offered"
                        ).forEach { (code, label) ->
                            val isSelected = manualImpact == code
                            OutlinedButton(
                                onClick = {
                                    manualImpact = code
                                    onManualParametersChanged(manualPitching, manualImpact, manualWickets, 1.8f, 1.9f)
                                },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (isSelected) StadiumGold.copy(alpha = 0.15f) else Color.Transparent
                                ),
                                border = BorderStroke(1.dp, if (isSelected) StadiumGold else Color(0xFF334155)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 10.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) StadiumGold else TextSecondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Manual Step E: Wickets Trajectory
                    Text(
                        text = "E. Wickets Hitting:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "HITTING" to "Hitting Wickets",
                            "MISSING" to "Missing Stumps",
                            "UMPIRES_CALL" to "Umpire's Call"
                        ).forEach { (code, label) ->
                            val isSelected = manualWickets == code
                            val col = when (code) {
                                "HITTING" -> DrsOutRed
                                "MISSING" -> DrsNotOutGreen
                                else -> DrsUmpireCall
                            }
                            OutlinedButton(
                                onClick = {
                                    manualWickets = code
                                    onManualParametersChanged(manualPitching, manualImpact, manualWickets, 1.8f, 1.9f)
                                },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (isSelected) col.copy(alpha = 0.15f) else Color.Transparent
                                ),
                                border = BorderStroke(1.dp, if (isSelected) col else Color(0xFF334155)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) col else TextSecondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Manual Note / Remarks
                    OutlinedTextField(
                        value = umpireNotes,
                        onValueChange = { umpireNotes = it },
                        label = { Text("Third Umpire Reasoning / Comments", fontSize = 11.sp) },
                        placeholder = { Text("e.g. Pad first in line with middle, hitting leg stump", fontSize = 11.sp, color = TextMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = StadiumGold,
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 4. Final Verdict Buttons (Declare Official Decision)
                    Text(
                        text = "3. DECLARE OFFICIAL VERDICT (फैसला सुनाएं)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = StadiumGold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // OUT Button
                        Button(
                            onClick = {
                                onThirdUmpireDecision(
                                    "OUT",
                                    selectedAppealType,
                                    umpireNotes.ifBlank { "Manual Third Umpire review confirms OUT" }
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DrsOutRed),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("drs_verdict_out_button")
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("🔴 OUT", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color.White)
                        }

                        // NOT OUT Button
                        Button(
                            onClick = {
                                onThirdUmpireDecision(
                                    "NOT OUT",
                                    selectedAppealType,
                                    umpireNotes.ifBlank { "Manual Third Umpire review confirms NOT OUT" }
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DrsNotOutGreen),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("drs_verdict_not_out_button")
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = PitchDark, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("🟢 NOT OUT", fontSize = 13.sp, fontWeight = FontWeight.Black, color = PitchDark)
                        }

                        // Umpire's Call Button
                        Button(
                            onClick = {
                                onThirdUmpireDecision(
                                    "UMPIRES_CALL",
                                    selectedAppealType,
                                    umpireNotes.ifBlank { "Umpire's Call on impact/wickets - stay with on-field decision" }
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DrsUmpireCall),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("drs_verdict_umpires_call_button")
                        ) {
                            Text("🟡 UMPIRE'S CALL", fontSize = 10.sp, fontWeight = FontWeight.Black, color = PitchDark)
                        }
                    }

                } else {
                    // Spectator Live View
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(PitchSurface)
                            .padding(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Policy, contentDescription = null, tint = StadiumGold, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("DRS Review in Progress (लाइव समीक्षा)", color = StadiumGold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Third Umpire phone par manually check kar rahe hain:\n• Front foot fair delivery\n• Bat / pad contact\n• Pitching & impact line\n• Wickets trajectory",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = onOpenRoleDialog,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, StadiumGold),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = StadiumGold, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Unlock Third Umpire Desk with PIN", color = StadiumGold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4. Live Broadcast Result Banner
        if (drsState.thirdUmpireDecision.isNotBlank()) {
            val isOut = drsState.thirdUmpireDecision == "OUT"
            val isNotOut = drsState.thirdUmpireDecision == "NOT OUT"
            val bannerColor = if (isOut) DrsOutRed else if (isNotOut) DrsNotOutGreen else DrsUmpireCall

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = bannerColor.copy(alpha = 0.15f)),
                border = BorderStroke(1.5.dp, bannerColor)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "OFFICIAL THIRD UMPIRE VERDICT",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = drsState.thirdUmpireDecision,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        color = bannerColor
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Pitching: ${drsState.pitching} • Impact: ${drsState.impact} • Wickets: ${drsState.wicketsHitting}",
                        fontSize = 11.5.sp,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = {
                            onResetDrs()
                            onBackToMatch()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = bannerColor),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                    ) {
                        Text(
                            text = "मैच पर वापस जाएं (Back to Live Match) ➔",
                            color = if (isOut) Color.White else PitchDark,
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}
