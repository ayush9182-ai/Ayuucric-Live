package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.example.ui.theme.SnickoSpike
import com.example.ui.theme.SnickoWaveform
import com.example.ui.theme.StadiumGold
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.sin

@Composable
fun DrsReviewScreen(
    drsState: DrsReviewState,
    currentRole: DeviceRole,
    onStartReview: (appealType: String) -> Unit,
    onManualParametersChanged: (pitching: String, impact: String, wickets: String, deviation: Float, impactDist: Float) -> Unit,
    onResetDrs: () -> Unit,
    onThirdUmpireDecision: (decision: String, appealType: String, reason: String) -> Unit,
    onOpenRoleDialog: () -> Unit,
    onSelectCameraAngle: (String) -> Unit,
    onSetFrameIndex: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: TV Umpire Live Sequence, 1: DRS Predictor Sandbox

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
            .testTag("drs_review_screen")
    ) {
        // DRS Banner Header
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = PitchCard),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = Brush.linearGradient(listOf(HawkEyeCyan, Color(0xFF6366F1))),
                width = 1.2.dp
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(HawkEyeCyan.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Policy,
                            contentDescription = "DRS",
                            tint = HawkEyeCyan,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "DECISION REVIEW SYSTEM",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = TextPrimary,
                            letterSpacing = 0.8.sp
                        )
                        Text(
                            text = "Phone 4 Third Umpire Desk • Hawk-Eye • UltraEdge",
                            fontSize = 11.sp,
                            color = HawkEyeCyan
                        )
                    }
                }

                OutlinedButton(
                    onClick = onResetDrs,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    modifier = Modifier.height(30.dp),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(0.8.dp, TextMuted)
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = "Reset", tint = TextSecondary, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reset", fontSize = 10.sp, color = TextSecondary)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Tabs: Live Umpire Broadcast vs Sandbox Predictor
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = PitchSurface,
            contentColor = HawkEyeCyan,
            indicator = { tabPositions ->
                SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = HawkEyeCyan,
                    height = 3.dp
                )
            }
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("TV Umpire Review", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Predictor Sandbox", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (selectedTab == 0) {
            TvUmpireSequenceView(
                drsState = drsState,
                currentRole = currentRole,
                onStartReview = onStartReview,
                onThirdUmpireDecision = onThirdUmpireDecision,
                onOpenRoleDialog = onOpenRoleDialog,
                onSelectCameraAngle = onSelectCameraAngle,
                onSetFrameIndex = onSetFrameIndex
            )
        } else {
            DrsSandboxPredictorView(
                drsState = drsState,
                onParametersChanged = onManualParametersChanged
            )
        }
    }
}

@Composable
fun TvUmpireSequenceView(
    drsState: DrsReviewState,
    currentRole: DeviceRole,
    onStartReview: (appealType: String) -> Unit,
    onThirdUmpireDecision: (decision: String, appealType: String, reason: String) -> Unit,
    onOpenRoleDialog: () -> Unit,
    onSelectCameraAngle: (String) -> Unit,
    onSetFrameIndex: (Int) -> Unit
) {
    val isThirdUmpire = currentRole == DeviceRole.THIRD_UMPIRE_DRS

    Column(modifier = Modifier.fillMaxWidth()) {
        // Third Umpire Authority Badge
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isThirdUmpire) Color(0xFF1B1B36) else Color(0xFF131B2A)
            ),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = androidx.compose.ui.graphics.SolidColor(
                    if (isThirdUmpire) Color(0xFFA78BFA) else Color(0xFF334155)
                )
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Policy,
                        contentDescription = null,
                        tint = if (isThirdUmpire) Color(0xFFA78BFA) else StadiumGold,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = if (isThirdUmpire) "PHONE 4: THIRD UMPIRE AUTHORIZED" else "DRS AUDIENCE MODE",
                            color = if (isThirdUmpire) Color(0xFFA78BFA) else StadiumGold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isThirdUmpire) "You hold sole authority to declare OUT or NOT OUT" else "Only Phone 4 can declare the final verdict",
                            color = TextSecondary,
                            fontSize = 10.sp
                        )
                    }
                }

                if (!isThirdUmpire) {
                    Button(
                        onClick = onOpenRoleDialog,
                        colors = ButtonDefaults.buttonColors(containerColor = StadiumGold),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("Claim Role", color = PitchDark, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Multi-Camera Angle Inspection Switcher
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = PitchCard),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF1E293B))
            )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "SELECT CAMERA FEED TO INSPECT",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val angles = listOf(
                        Pair("PITCH_CAM", "🎥 Phone 1: Pitch Cam"),
                        Pair("CREASE_CAM", "📐 Phone 2: Side Cam")
                    )
                    angles.forEach { (key, label) ->
                        FilterChip(
                            selected = drsState.selectedCameraAngle == key,
                            onClick = { onSelectCameraAngle(key) },
                            label = { Text(label, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = HawkEyeCyan,
                                selectedLabelColor = PitchDark,
                                containerColor = SurfaceDark,
                                labelColor = TextSecondary
                            )
                        )
                    }
                }

                // Slow-mo Frame Scrubber
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "FRAME-BY-FRAME INSPECTION:",
                        fontSize = 10.sp,
                        color = TextMuted,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Frame ${drsState.frameIndex}/30",
                        fontSize = 11.sp,
                        color = HawkEyeCyan,
                        fontWeight = FontWeight.Bold
                    )
                }
                androidx.compose.material3.Slider(
                    value = drsState.frameIndex.toFloat(),
                    onValueChange = { onSetFrameIndex(it.toInt()) },
                    valueRange = 1f..30f,
                    steps = 28,
                    colors = androidx.compose.material3.SliderDefaults.colors(
                        thumbColor = HawkEyeCyan,
                        activeTrackColor = HawkEyeCyan,
                        inactiveTrackColor = Color(0xFF334155)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Appeal Action Controls (Trigger LBW / Caught Behind / Run Out Review)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = PitchCard),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = androidx.compose.ui.graphics.SolidColor(PitchCardBorder),
                width = 0.8.dp
            )
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "TRIGGER THIRD UMPIRE APPEAL",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { onStartReview("LBW") },
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("drs_lbw_appeal_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = HawkEyeCyan),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Review LBW Appeal", fontSize = 12.sp, fontWeight = FontWeight.Black, color = PitchDark)
                    }

                    OutlinedButton(
                        onClick = { onStartReview("CAUGHT_BEHIND") },
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("drs_caught_appeal_btn"),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, StadiumGold),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = StadiumGold)
                    ) {
                        Text("Review Snicko / Edge", fontSize = 12.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Step Progress Tracker
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            DrsStepPill(step = 1, title = "Front Foot", isCurrent = drsState.reviewStage == 1, isPassed = drsState.reviewStage > 1)
            DrsStepPill(step = 2, title = "UltraEdge", isCurrent = drsState.reviewStage == 2, isPassed = drsState.reviewStage > 2)
            DrsStepPill(step = 3, title = "Hawk-Eye", isCurrent = drsState.reviewStage == 3, isPassed = drsState.reviewStage > 3)
            DrsStepPill(step = 4, title = "Decision", isCurrent = drsState.reviewStage == 4, isPassed = drsState.reviewStage >= 4)
        }

        Spacer(modifier = Modifier.height(14.dp))

        // UltraEdge / Snicko Oscilloscope Canvas
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = PitchCard),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF1E293B)),
                width = 1.dp
            )
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = "Snicko",
                            tint = SnickoWaveform,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "UltraEdge Real-Time Soundwave",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    Text(
                        text = if (drsState.ultraEdgeSpike) "SPIKE DETECTED (EDGE)" else "FLAT LINE (NO BAT)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = if (drsState.ultraEdgeSpike) SnickoSpike else CricketGreen
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF030712))
                        .border(1.dp, Color(0xFF1F2937), RoundedCornerShape(10.dp))
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawUltraEdgeWaveform(hasSpike = drsState.ultraEdgeSpike)
                    }

                    // Frame sync vertical marker
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .width(2.dp)
                            .height(90.dp)
                            .background(Color.White.copy(alpha = 0.5f))
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Hawk-Eye 3D Ball Tracking Graphic
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = PitchCard),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = androidx.compose.ui.graphics.SolidColor(HawkEyeCyan.copy(alpha = 0.4f)),
                width = 1.dp
            )
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = "HawkEye",
                            tint = HawkEyeCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Hawk-Eye 3D Ball Tracking",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    Text(
                        text = "${drsState.aiConfidencePercent}% AI Accuracy",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = HawkEyeCyan
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // HawkEye Trajectory Canvas
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF0B1726))
                        .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(10.dp))
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawHawkEyeTrajectory(
                            pitching = drsState.pitching,
                            impact = drsState.impact,
                            wickets = drsState.wicketsHitting
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 3 Criteria Status Cards (Pitching, Impact, Wickets)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DrsCriteriaBox(
                        title = "PITCHING",
                        value = drsState.pitching.replace("_", " "),
                        isSuccess = drsState.pitching != "OUTSIDE_LEG",
                        modifier = Modifier.weight(1f)
                    )
                    DrsCriteriaBox(
                        title = "IMPACT",
                        value = drsState.impact.replace("_", " "),
                        isSuccess = drsState.impact == "IN_LINE",
                        modifier = Modifier.weight(1f)
                    )
                    DrsCriteriaBox(
                        title = "WICKETS",
                        value = drsState.wicketsHitting.replace("_", " "),
                        isSuccess = drsState.wicketsHitting == "HITTING",
                        isUmpireCall = drsState.wicketsHitting == "UMPIRES_CALL",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Big Screen Decision Outcome (Flashing Broadcast style)
        val infiniteTransition = rememberInfiniteTransition(label = "flash")
        val flashAlpha by infiniteTransition.animateFloat(
            initialValue = 0.8f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(500),
                repeatMode = RepeatMode.Reverse
            ),
            label = "flash"
        )

        val decisionColor = when (drsState.thirdUmpireDecision) {
            "OUT" -> DrsOutRed
            "UMPIRES_CALL" -> DrsUmpireCall
            else -> DrsNotOutGreen
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(decisionColor.copy(alpha = 0.18f))
                .border(2.dp, decisionColor.copy(alpha = flashAlpha), RoundedCornerShape(16.dp))
                .padding(vertical = 18.dp, horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "THIRD UMPIRE DECISION",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = drsState.thirdUmpireDecision.replace("_", "'S "),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    color = decisionColor,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (drsState.thirdUmpireDecision == drsState.onFieldDecision) "On-field Decision Confirmed" else "Original Decision Overturned to ${drsState.thirdUmpireDecision}!",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
            }
        }

        // Third Umpire Official Action Buttons (Phone 4 only)
        if (isThirdUmpire) {
            Spacer(modifier = Modifier.height(14.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1B36)),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFA78BFA)),
                    width = 1.2.dp
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "PHONE 4 OFFICIAL ACTION: BROADCAST VERDICT",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFA78BFA),
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Tap your decision below to broadcast it to all spectator phones and update the match scorecard.",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                onThirdUmpireDecision("OUT", drsState.appealType, "Third Umpire confirmed out after review")
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = DrsOutRed),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("DECLARE OUT", fontWeight = FontWeight.Black, fontSize = 12.sp, color = Color.White)
                        }

                        Button(
                            onClick = {
                                onThirdUmpireDecision("NOT OUT", drsState.appealType, "Third Umpire ruled Not Out")
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = CricketGreen),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("NOT OUT", fontWeight = FontWeight.Black, fontSize = 12.sp, color = PitchDark)
                        }

                        OutlinedButton(
                            onClick = {
                                onThirdUmpireDecision("UMPIRES_CALL", drsState.appealType, "On-field call upheld")
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, StadiumGold)
                        ) {
                            Text("Umpire Call", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = StadiumGold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DrsStepPill(step: Int, title: String, isCurrent: Boolean, isPassed: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(
                    when {
                        isPassed -> CricketGreen
                        isCurrent -> HawkEyeCyan
                        else -> Color(0xFF1E293B)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = step.toString(),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (isPassed || isCurrent) PitchDark else TextSecondary
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = title,
            fontSize = 10.sp,
            color = if (isCurrent) HawkEyeCyan else if (isPassed) CricketGreen else TextMuted,
            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun DrsCriteriaBox(
    title: String,
    value: String,
    isSuccess: Boolean,
    isUmpireCall: Boolean = false,
    modifier: Modifier = Modifier
) {
    val boxColor = when {
        isUmpireCall -> DrsUmpireCall
        isSuccess -> CricketGreen
        else -> DrsOutRed
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(boxColor.copy(alpha = 0.12f))
            .border(1.dp, boxColor.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
            .padding(vertical = 8.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = title, fontSize = 9.sp, color = TextMuted, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = boxColor
            )
        }
    }
}

@Composable
fun DrsSandboxPredictorView(
    drsState: DrsReviewState,
    onParametersChanged: (pitching: String, impact: String, wickets: String, deviation: Float, impactDist: Float) -> Unit
) {
    var pitching by remember { mutableStateOf(drsState.pitching) }
    var impact by remember { mutableStateOf(drsState.impact) }
    var wickets by remember { mutableStateOf(drsState.wicketsHitting) }
    var deviation by remember { mutableFloatStateOf(1.8f) }
    var impactDistance by remember { mutableFloatStateOf(1.9f) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = PitchCard)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = "Predictor",
                        tint = HawkEyeCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Interactive Decision Predictor Engine",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Simulate how Hawk-Eye and ICC DRS protocols evaluate trajectory parameters in real-time.",
                    fontSize = 11.sp,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Pitching Selector
                Text(text = "1. PITCHING POINT", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("IN_LINE", "OUTSIDE_OFF", "OUTSIDE_LEG").forEach { opt ->
                        FilterChip(
                            selected = pitching == opt,
                            onClick = {
                                pitching = opt
                                onParametersChanged(pitching, impact, wickets, deviation, impactDistance)
                            },
                            label = { Text(opt.replace("_", " "), fontSize = 10.sp) },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = if (opt == "OUTSIDE_LEG") DrsOutRed.copy(alpha = 0.3f) else CricketGreen.copy(alpha = 0.3f),
                                selectedLabelColor = if (opt == "OUTSIDE_LEG") DrsOutRed else CricketGreen
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Impact Selector
                Text(text = "2. IMPACT ON PAD", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("IN_LINE", "OUTSIDE").forEach { opt ->
                        FilterChip(
                            selected = impact == opt,
                            onClick = {
                                impact = opt
                                onParametersChanged(pitching, impact, wickets, deviation, impactDistance)
                            },
                            label = { Text(opt.replace("_", " "), fontSize = 11.sp) },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = if (opt == "OUTSIDE") DrsOutRed.copy(alpha = 0.3f) else CricketGreen.copy(alpha = 0.3f),
                                selectedLabelColor = if (opt == "OUTSIDE") DrsOutRed else CricketGreen
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Wickets Selector
                Text(text = "3. WICKETS PATH PREDICTION", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("HITTING", "UMPIRES_CALL", "MISSING").forEach { opt ->
                        FilterChip(
                            selected = wickets == opt,
                            onClick = {
                                wickets = opt
                                onParametersChanged(pitching, impact, wickets, deviation, impactDistance)
                            },
                            label = { Text(opt.replace("_", " "), fontSize = 10.sp) },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = when (opt) {
                                    "HITTING" -> CricketGreen.copy(alpha = 0.3f)
                                    "UMPIRES_CALL" -> DrsUmpireCall.copy(alpha = 0.3f)
                                    else -> DrsOutRed.copy(alpha = 0.3f)
                                },
                                selectedLabelColor = when (opt) {
                                    "HITTING" -> CricketGreen
                                    "UMPIRES_CALL" -> DrsUmpireCall
                                    else -> DrsOutRed
                                }
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Computed Output Prediction Card
                val isOut = pitching != "OUTSIDE_LEG" && impact == "IN_LINE" && wickets == "HITTING"
                val outcomeText = if (isOut) "OUT" else if (wickets == "UMPIRES_CALL") "UMPIRE'S CALL" else "NOT OUT"
                val outcomeColor = if (isOut) DrsOutRed else if (wickets == "UMPIRES_CALL") DrsUmpireCall else DrsNotOutGreen

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(outcomeColor.copy(alpha = 0.15f))
                        .border(1.2.dp, outcomeColor, RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "PREDICTED DECISION: $outcomeText",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = outcomeColor
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = when {
                                pitching == "OUTSIDE_LEG" -> "ICC Rule 36: Cannot be given LBW if ball pitches outside leg stump."
                                impact == "OUTSIDE" -> "Impact outside off stump while playing a genuine shot."
                                wickets == "MISSING" -> "Ball tracking confirms delivery is passing over or missing the stumps."
                                wickets == "UMPIRES_CALL" -> "Less than 50% of the ball hitting stump. On-field decision stands."
                                else -> "All 3 conditions met (Pitched in line, hit in line, crashing into stumps)."
                            },
                            fontSize = 11.sp,
                            color = TextPrimary
                        )
                    }
                }
            }
        }
    }
}

// Canvas Helper: UltraEdge Waveform Drawing
private fun DrawScope.drawUltraEdgeWaveform(hasSpike: Boolean) {
    val w = size.width
    val h = size.height
    val centerY = h / 2f

    val path = Path()
    path.moveTo(0f, centerY)

    val pointsCount = 100
    for (i in 0..pointsCount) {
        val x = (i.toFloat() / pointsCount) * w
        // Spike centered at x = 0.5
        val distFromCenter = kotlin.math.abs((x / w) - 0.5f)
        val amplitude = if (hasSpike && distFromCenter < 0.08f) {
            // High sharp spike
            val spikeFactor = (1f - distFromCenter / 0.08f)
            sin(i * 0.9f) * (h * 0.42f) * spikeFactor
        } else {
            // Low background ambient buzz
            sin(i * 0.4f) * (h * 0.08f)
        }

        path.lineTo(x, centerY + amplitude)
    }

    drawPath(
        path = path,
        color = if (hasSpike) SnickoSpike else SnickoWaveform,
        style = Stroke(width = 2.dp.toPx())
    )
}

// Canvas Helper: Hawk-Eye Trajectory
private fun DrawScope.drawHawkEyeTrajectory(
    pitching: String,
    impact: String,
    wickets: String
) {
    val w = size.width
    val h = size.height

    // Pitch Rect
    drawRect(
        color = Color(0xFF0F172A),
        topLeft = Offset(0f, 0f),
        size = Size(w, h)
    )

    // Stumps (Right side)
    val stumpX = w * 0.88f
    val stumpBaseY = h * 0.72f
    val stumpHeight = 35.dp.toPx()

    // 3 Stumps
    for (i in -1..1) {
        val yOff = i * 4.dp.toPx()
        drawLine(
            color = Color(0xFFFBBF24),
            start = Offset(stumpX, stumpBaseY + yOff),
            end = Offset(stumpX, (stumpBaseY - stumpHeight) + yOff),
            strokeWidth = 3.dp.toPx()
        )
    }

    // Trajectory path
    val start = Offset(w * 0.05f, h * 0.45f)
    val pitchPoint = Offset(
        w * 0.45f,
        when (pitching) {
            "OUTSIDE_LEG" -> h * 0.78f
            "OUTSIDE_OFF" -> h * 0.42f
            else -> h * 0.60f
        }
    )
    val impactPoint = Offset(
        w * 0.72f,
        when (impact) {
            "OUTSIDE" -> h * 0.44f
            else -> h * 0.60f
        }
    )
    val stumpTarget = Offset(
        stumpX,
        when (wickets) {
            "MISSING" -> stumpBaseY - stumpHeight - 12.dp.toPx() // passes over
            "UMPIRES_CALL" -> stumpBaseY - stumpHeight // clips bail
            else -> stumpBaseY - (stumpHeight * 0.5f) // middle of stumps
        }
    )

    // 1. Path to bounce
    val path1 = Path().apply {
        moveTo(start.x, start.y)
        quadraticTo((start.x + pitchPoint.x) / 2, start.y - 15.dp.toPx(), pitchPoint.x, pitchPoint.y)
    }
    drawPath(path = path1, color = HawkEyeCyan, style = Stroke(width = 2.5.dp.toPx()))

    // 2. Path to pad impact
    val path2 = Path().apply {
        moveTo(pitchPoint.x, pitchPoint.y)
        lineTo(impactPoint.x, impactPoint.y)
    }
    drawPath(path = path2, color = HawkEyeCyan, style = Stroke(width = 2.5.dp.toPx()))

    // 3. Projected path from pad through stumps (dotted line)
    val path3 = Path().apply {
        moveTo(impactPoint.x, impactPoint.y)
        lineTo(stumpTarget.x, stumpTarget.y)
    }
    drawPath(
        path = path3,
        color = when (wickets) {
            "HITTING" -> CricketGreen
            "UMPIRES_CALL" -> DrsUmpireCall
            else -> DrsOutRed
        },
        style = Stroke(
            width = 3.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
        )
    )

    // Bounce & Impact Circles
    drawCircle(color = HawkEyeCyan, radius = 5.dp.toPx(), center = pitchPoint)
    drawCircle(color = StadiumGold, radius = 5.dp.toPx(), center = impactPoint)
    drawCircle(
        color = if (wickets == "HITTING") CricketGreen else if (wickets == "UMPIRES_CALL") DrsUmpireCall else DrsOutRed,
        radius = 6.dp.toPx(),
        center = stumpTarget
    )
}
