package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.security.SecureRandom
import kotlin.math.abs

@Composable
fun CoinFlipperDialog(
    teamAName: String,
    teamBName: String,
    onDismiss: () -> Unit,
    onTossDecided: ((winner: String, decision: String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val secureRandom = remember { SecureRandom() }

    var isFlipping by remember { mutableStateOf(false) }
    var resultText by remember { mutableStateOf<String?>(null) } // "HEADS" or "TAILS"
    var callingTeam by remember { mutableStateOf(teamAName) }
    var selectedCall by remember { mutableStateOf("HEADS") }
    var winnerTeam by remember { mutableStateOf<String?>(null) }
    var tossDecision by remember { mutableStateOf("Bat First") }
    var flipCount by remember { mutableIntStateOf(0) }

    // 3D rotation and bounce animatable
    val rotationY = remember { Animatable(0f) }
    val translationY = remember { Animatable(0f) }

    // Current displayed side based on rotation degrees
    val currentSide = remember {
        derivedStateOf {
            val normalized = ((rotationY.value % 360f) + 360f) % 360f
            if (normalized in 90f..270f) "TAILS" else "HEADS"
        }
    }

    fun doFairFlip() {
        if (isFlipping) return
        isFlipping = true
        resultText = null
        winnerTeam = null

        coroutineScope.launch {
            // Cryptographically secure outcome: 0 = HEADS, 1 = TAILS
            val outcome = if (secureRandom.nextBoolean()) "HEADS" else "TAILS"
            val totalSpins = 5 + secureRandom.nextInt(4) // 5 to 8 full 360 spins
            val targetDegrees = if (outcome == "HEADS") {
                (totalSpins * 360f)
            } else {
                (totalSpins * 360f) + 180f
            }

            // Animate height bounce + 3D spin
            val flipJob = launch {
                rotationY.snapTo(0f)
                rotationY.animateTo(
                    targetValue = targetDegrees,
                    animationSpec = tween(durationMillis = 1800, easing = FastOutSlowInEasing)
                )
            }

            val bounceJob = launch {
                translationY.animateTo(-120f, tween(700, easing = FastOutSlowInEasing))
                translationY.animateTo(0f, tween(1100, easing = FastOutSlowInEasing))
            }

            flipJob.join()
            bounceJob.join()

            resultText = outcome
            flipCount += 1
            isFlipping = false

            // Determine winner
            val won = outcome == selectedCall
            winnerTeam = if (won) callingTeam else (if (callingTeam == teamAName) teamBName else teamAName)
        }
    }

    Dialog(
        onDismissRequest = { if (!isFlipping) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 20.dp)
                .testTag("coin_flipper_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = PitchDark),
            border = BorderStroke(
                1.5.dp,
                Brush.linearGradient(listOf(StadiumGold, CricketGreen, HawkEyeCyan))
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
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
                            Icon(
                                imageVector = Icons.Default.Casino,
                                contentDescription = "Toss",
                                tint = StadiumGold,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Official Match Toss",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = CricketGreen,
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "100% Cheat-Proof Secure Random",
                                    fontSize = 11.sp,
                                    color = CricketGreen,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        enabled = !isFlipping,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Calling Team Selection
                Text(
                    text = "Who calls the Toss? (टॉस कॉल करने वाली टीम):",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1E293B))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val isTeamA = callingTeam == teamAName
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isTeamA) CricketGreen else Color.Transparent)
                            .clickable(enabled = !isFlipping) { callingTeam = teamAName }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = teamAName.ifBlank { "Team A" },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isTeamA) PitchDark else TextSecondary,
                            maxLines = 1
                        )
                    }

                    val isTeamB = callingTeam == teamBName
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isTeamB) HawkEyeCyan else Color.Transparent)
                            .clickable(enabled = !isFlipping) { callingTeam = teamBName }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = teamBName.ifBlank { "Team B" },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isTeamB) PitchDark else TextSecondary,
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Heads or Tails selection
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val isHeads = selectedCall == "HEADS"
                    OutlinedButton(
                        onClick = { selectedCall = "HEADS" },
                        enabled = !isFlipping,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(
                            1.5.dp,
                            if (isHeads) StadiumGold else Color(0xFF334155)
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (isHeads) StadiumGold.copy(alpha = 0.15f) else Color.Transparent
                        )
                    ) {
                        Text(
                            text = "🪙 Heads (चित)",
                            fontWeight = if (isHeads) FontWeight.Black else FontWeight.Normal,
                            color = if (isHeads) StadiumGold else TextSecondary,
                            fontSize = 12.sp
                        )
                    }

                    val isTails = selectedCall == "TAILS"
                    OutlinedButton(
                        onClick = { selectedCall = "TAILS" },
                        enabled = !isFlipping,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(
                            1.5.dp,
                            if (isTails) StadiumGold else Color(0xFF334155)
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (isTails) StadiumGold.copy(alpha = 0.15f) else Color.Transparent
                        )
                    ) {
                        Text(
                            text = "🪙 Tails (पट)",
                            fontWeight = if (isTails) FontWeight.Black else FontWeight.Normal,
                            color = if (isTails) StadiumGold else TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // The 3D Animated Coin Box
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .graphicsLayer {
                            this.translationY = translationY.value
                            this.rotationY = rotationY.value
                            cameraDistance = 16f * density
                        }
                        .shadow(16.dp, CircleShape)
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
                        .border(4.dp, Color(0xFFFEF08A), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = if (currentSide.value == "HEADS") "👑" else "🦁",
                            fontSize = 38.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = currentSide.value,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            color = Color(0xFF451A03),
                            fontFamily = FontFamily.SansSerif
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Result announcement or status banner
                if (resultText != null && winnerTeam != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                        border = BorderStroke(1.dp, StadiumGold.copy(alpha = 0.6f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "TOSS RESULT: $resultText!",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = StadiumGold,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "🏆 $winnerTeam won the toss!",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = CricketGreen,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Decision: Bat First or Bowl First
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val isBat = tossDecision == "Bat First"
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isBat) StadiumGold else Color(0xFF1E293B))
                                        .clickable { tossDecision = "Bat First" }
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "🏏 Bat First",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isBat) PitchDark else TextSecondary
                                    )
                                }

                                val isBowl = tossDecision == "Bowl First"
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isBowl) HawkEyeCyan else Color(0xFF1E293B))
                                        .clickable { tossDecision = "Bowl First" }
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "🎯 Bowl First",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isBowl) PitchDark else TextSecondary
                                    )
                                }
                            }
                        }
                    }
                } else if (isFlipping) {
                    Text(
                        text = "🪙 Flipping coin in the air... Suspense! 🏏",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = HawkEyeCyan
                    )
                } else {
                    Text(
                        text = "$callingTeam calls $selectedCall. Tap Flip Coin below to toss!",
                        fontSize = 11.5.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Flip Button
                Button(
                    onClick = { doFairFlip() },
                    enabled = !isFlipping,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StadiumGold,
                        disabledContainerColor = StadiumGold.copy(alpha = 0.4f)
                    )
                ) {
                    Text(
                        text = if (isFlipping) "FLIPPING... 🌀" else if (flipCount > 0) "FLIP AGAIN (टॉस दोबारा करें)" else "SPIN COIN (टॉस करें) 🪙",
                        color = PitchDark,
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp
                    )
                }

                // If result available, option to apply decision
                if (resultText != null && winnerTeam != null && onTossDecided != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = {
                            onTossDecided(winnerTeam!!, tossDecision)
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = CricketGreen, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Confirm: $winnerTeam chose to $tossDecision",
                            color = CricketGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
