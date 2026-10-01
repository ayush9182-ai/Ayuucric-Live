package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.DrsDecisionCardData
import com.example.ui.theme.*
import kotlinx.coroutines.delay

/**
 * TV Broadcast DRS Decision Card (Hotstar/IPL Big Screen Style).
 * Pops up simultaneously on all spectators' and officials' phones.
 */
@Composable
fun DrsDecisionBroadcastCard(
    decision: DrsDecisionCardData,
    onDismiss: () -> Unit
) {
    // Auto dismiss after 7 seconds
    LaunchedEffect(decision.id) {
        delay(7000)
        onDismiss()
    }

    val isOut = decision.thirdUmpireDecision == "OUT"
    val isUmpiresCall = decision.thirdUmpireDecision == "UMPIRES_CALL"

    val decisionColor = when {
        isOut -> DrsOutRed
        isUmpiresCall -> StadiumGold
        else -> CricketGreen
    }

    val decisionText = when {
        isOut -> "OUT"
        isUmpiresCall -> "UMPIRE'S CALL"
        else -> "NOT OUT"
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse_card")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .padding(16.dp)
                .testTag("drs_decision_broadcast_card"),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(24.dp, RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = PitchDark),
                border = BorderStroke(2.dp, decisionColor.copy(alpha = glowAlpha))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    decisionColor.copy(alpha = 0.25f),
                                    Color(0xFF0F172A),
                                    PitchDark
                                )
                            )
                        )
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(HawkEyeCyan.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Policy,
                                    contentDescription = "DRS",
                                    tint = HawkEyeCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "3RD UMPIRE DECISION",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                    color = HawkEyeCyan,
                                    letterSpacing = 1.5.sp
                                )
                                Text(
                                    text = "LIVE TV BROADCAST REVIEW",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextMuted
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Player & Bowler Appeal info
                    Surface(
                        color = Color(0xFF1E293B).copy(alpha = 0.7f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(text = "BATSMAN", fontSize = 9.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                                Text(text = decision.batsman, fontSize = 13.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                            }
                            Text(text = "vs", fontSize = 11.sp, color = TextMuted)
                            Column(horizontalAlignment = Alignment.End) {
                                Text(text = "BOWLER", fontSize = 9.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                                Text(text = decision.bowler, fontSize = 13.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Big Glowing Outcome Banner
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        decisionColor.copy(alpha = 0.2f),
                                        decisionColor.copy(alpha = 0.45f),
                                        decisionColor.copy(alpha = 0.2f)
                                    )
                                )
                            )
                            .border(2.dp, decisionColor, RoundedCornerShape(16.dp))
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = decisionText,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            color = decisionColor,
                            letterSpacing = 3.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // HawkEye 3-Point Ball Tracking Telemetry
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TelemetryBadge(
                            label = "Pitching",
                            value = decision.pitching.replace("_", " "),
                            isPositive = decision.pitching == "IN_LINE",
                            modifier = Modifier.weight(1f)
                        )
                        TelemetryBadge(
                            label = "Impact",
                            value = decision.impact.replace("_", " "),
                            isPositive = decision.impact == "IN_LINE",
                            modifier = Modifier.weight(1f)
                        )
                        TelemetryBadge(
                            label = "Wickets",
                            value = decision.wickets.replace("_", " "),
                            isPositive = decision.wickets == "HITTING",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Original on-field decision: ${decision.onFieldDecision}",
                        fontSize = 11.sp,
                        color = TextMuted
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "Got it (Dismiss)", fontSize = 12.sp, color = TextPrimary)
                    }
                }
            }
        }
    }
}

@Composable
private fun TelemetryBadge(
    label: String,
    value: String,
    isPositive: Boolean,
    modifier: Modifier = Modifier
) {
    val tint = if (isPositive) CricketGreen else Color(0xFFEF4444)
    Surface(
        color = Color(0xFF0F172A),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(0.8.dp, tint.copy(alpha = 0.6f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = label.uppercase(), fontSize = 8.sp, color = TextMuted, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = value, fontSize = 10.sp, fontWeight = FontWeight.Black, color = tint)
        }
    }
}
