package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BallEventEntity
import com.example.data.model.MatchEntity
import com.example.ui.theme.CricketGreen
import com.example.ui.theme.DrsOutRed
import com.example.ui.theme.HawkEyeCyan
import com.example.ui.theme.PitchCard
import com.example.ui.theme.PitchCardBorder
import com.example.ui.theme.PitchDark
import com.example.ui.theme.StadiumGold
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun ScoreBanner(
    match: MatchEntity,
    recentBalls: List<BallEventEntity>,
    onSwitchStriker: () -> Unit,
    onOpenSquad: (() -> Unit)? = null,
    onOpenUpdateApp: (() -> Unit)? = null,
    onChangeBowler: (() -> Unit)? = null,
    onChangeBatsman: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("score_banner_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = PitchCard),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.linearGradient(
                listOf(PitchCardBorder, Color(0xFF1E3A8A), CricketGreen.copy(alpha = 0.3f))
            ),
            width = 1.2.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Tournament & Live Badge Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = match.tournamentName,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    modifier = Modifier.weight(1f, fill = false)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (onOpenUpdateApp != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF2563EB).copy(alpha = 0.2f))
                                .border(0.8.dp, Color(0xFF60A5FA).copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                                .clickable { onOpenUpdateApp() }
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SystemUpdate,
                                contentDescription = "Update",
                                tint = Color(0xFF60A5FA),
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "UPDATE",
                                color = Color(0xFF93C5FD),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(DrsOutRed.copy(alpha = 0.2f))
                            .border(0.8.dp, DrsOutRed.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(DrsOutRed.copy(alpha = pulseAlpha))
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "LIVE",
                            color = DrsOutRed,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Scoreboard Main Numbers
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1.3f, fill = false)) {
                    Text(
                        text = match.battingTeam,
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "${match.score}/${match.wickets}",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            color = CricketGreen
                        )
                        val overs = match.legalBalls / 6
                        val ballsInOver = match.legalBalls % 6
                        Text(
                            text = "($overs.$ballsInOver / ${match.totalOvers} ov)",
                            fontSize = 14.sp,
                            color = TextSecondary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    val crr = if (match.legalBalls > 0) (match.score.toFloat() / match.legalBalls) * 6f else 0f
                    val ballsLeft = (match.totalOvers * 6) - match.legalBalls
                    val runsNeeded = match.target - match.score
                    val rrr = if (ballsLeft > 0 && runsNeeded > 0) (runsNeeded.toFloat() / ballsLeft) * 6f else 0f

                    Text(
                        text = "CRR: ${"%.2f".format(crr)}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = HawkEyeCyan,
                        maxLines = 1
                    )
                    if (match.currentInnings == 2 && runsNeeded > 0) {
                        Text(
                            text = "RRR: ${"%.2f".format(rrr)}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = StadiumGold,
                            maxLines = 1
                        )
                        Text(
                            text = "Target: ${match.target}",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            maxLines = 1
                        )
                    } else {
                        Text(
                            text = match.teamAFirstInningsScore,
                            fontSize = 12.sp,
                            color = TextMuted,
                            maxLines = 1
                        )
                    }
                }
            }

            // Match Equation Status
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0F172A))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = match.statusDetail,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (match.status == "FINISHED") StadiumGold else Color(0xFF93C5FD)
                    )
                    Text(
                        text = match.venue,
                        fontSize = 10.sp,
                        color = TextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Current Batsmen & Bowler
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Striker & Non-Striker
                Column(modifier = Modifier.weight(1.2f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "BATTERS",
                            fontSize = 10.sp,
                            color = TextMuted,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(
                                onClick = onSwitchStriker,
                                modifier = Modifier
                                    .height(28.dp)
                                    .testTag("switch_striker_btn"),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, CricketGreen.copy(alpha = 0.7f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = CricketGreen)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SwapHoriz,
                                    contentDescription = "Rotate Strike",
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("Swap", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                            }
                            if (onChangeBatsman != null) {
                                Box(
                                    modifier = Modifier
                                        .height(28.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(CricketGreen.copy(alpha = 0.18f))
                                        .border(1.dp, CricketGreen.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                        .clickable { onChangeBatsman() }
                                        .padding(horizontal = 8.dp, vertical = 3.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("✏️ Edit", fontSize = 10.5.sp, color = CricketGreen, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))

                    // Striker
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = onChangeBatsman != null) { onChangeBatsman?.invoke() },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(CricketGreen)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = match.strikerName,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            maxLines = 1,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${match.strikerRuns}* (${match.strikerBalls})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = CricketGreen,
                            maxLines = 1
                        )
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    // Non-Striker
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = onChangeBatsman != null) { onChangeBatsman?.invoke() },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = match.nonStrikerName,
                            fontSize = 12.sp,
                            color = TextSecondary,
                            maxLines = 1,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${match.nonStrikerRuns} (${match.nonStrikerBalls})",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Bowler
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(enabled = onChangeBowler != null) { onChangeBowler?.invoke() }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "BOWLER",
                            fontSize = 10.sp,
                            color = TextMuted,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        OutlinedButton(
                            onClick = { onChangeBowler?.invoke() },
                            modifier = Modifier
                                .height(28.dp)
                                .testTag("change_bowler_btn"),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, StadiumGold.copy(alpha = 0.8f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = StadiumGold)
                        ) {
                            Text("🎳 Change", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = match.bowlerName,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = StadiumGold,
                        maxLines = 1
                    )
                    val bOvers = match.bowlerBalls / 6
                    val bBalls = match.bowlerBalls % 6
                    val econ = if (match.bowlerBalls > 0) (match.bowlerRuns.toFloat() / match.bowlerBalls) * 6f else 0f
                    Text(
                        text = "$bOvers.$bBalls ov • ${match.bowlerWickets}/${match.bowlerRuns} (E: ${"%.1f".format(econ)})",
                        fontSize = 10.5.sp,
                        color = HawkEyeCyan,
                        maxLines = 1
                    )
                }
            }

            // Recent Balls Strip
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent: ",
                    fontSize = 11.sp,
                    color = TextMuted,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(6.dp))

                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val lastBalls = recentBalls.take(8).reversed()
                    if (lastBalls.isEmpty()) {
                        Text("No balls bowled yet", fontSize = 11.sp, color = TextMuted)
                    } else {
                        lastBalls.forEach { ball ->
                            RecentBallPill(ball = ball)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RecentBallPill(ball: BallEventEntity) {
    val (bgColor, textColor, text) = when {
        ball.isWicket -> Triple(DrsOutRed, Color.White, "W")
        ball.isSix -> Triple(StadiumGold, PitchDark, "6")
        ball.isBoundary -> Triple(CricketGreen, PitchDark, "4")
        ball.extraType == "Wide" -> Triple(Color(0xFF6366F1), Color.White, "Wd")
        ball.extraType == "NoBall" -> Triple(Color(0xFFEC4899), Color.White, "Nb")
        ball.runs == 0 -> Triple(Color(0xFF334155), TextSecondary, "•")
        else -> Triple(Color(0xFF1E293B), TextPrimary, ball.runs.toString())
    }

    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(bgColor)
            .border(0.5.dp, Color.White.copy(alpha = 0.2f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = if (text.length > 1) 9.sp else 11.sp,
            fontWeight = FontWeight.Black,
            color = textColor
        )
    }
}
