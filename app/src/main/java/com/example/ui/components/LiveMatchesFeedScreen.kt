package com.example.ui.components

import android.content.Intent
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SportsCricket
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.audio.SidhuVoiceStyle
import com.example.data.model.BallEventEntity
import com.example.data.model.MatchEntity
import com.example.ui.theme.CricketGreen
import com.example.ui.theme.DrsOutRed
import com.example.ui.theme.HawkEyeCyan
import com.example.ui.theme.PitchCard
import com.example.ui.theme.PitchDark
import com.example.ui.theme.PitchSurface
import com.example.ui.theme.StadiumGold
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun LiveMatchesFeedScreen(
    matches: List<MatchEntity>,
    selectedMatchId: String,
    currentBallEvents: List<BallEventEntity>,
    isStreamingPitchCam: Boolean,
    isStreamingSideCam: Boolean,
    isSidhuCommentaryEnabled: Boolean = true,
    isSidhuSpeaking: Boolean = false,
    sidhuCurrentDialogue: String = "",
    sidhuVoiceStyle: SidhuVoiceStyle = SidhuVoiceStyle.ENERGETIC_JOSH,
    onToggleSidhuCommentary: (Boolean) -> Unit = {},
    onSelectSidhuStyle: (SidhuVoiceStyle) -> Unit = {},
    onTestSidhuVoice: () -> Unit = {},
    onSelectMatch: (String) -> Unit,
    onWatchLiveVideoAndScore: (String) -> Unit,
    onOpenFullScorecard: (String) -> Unit,
    onCreateNewMatch: () -> Unit,
    onOpenAiSummary: (String) -> Unit = {}
) {
    val context = LocalContext.current
    var selectedFilter by remember { mutableStateOf("LIVE") } // "LIVE", "ALL", "COMPLETED"

    val filteredMatches = when (selectedFilter) {
        "LIVE" -> matches.filter { it.status == "LIVE" }.ifEmpty { matches }
        "COMPLETED" -> matches.filter { it.status != "LIVE" }
        else -> matches
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(PitchDark)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // CricHeroes Style Top Header
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = PitchSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(CricketGreen, Color(0xFF0284C7))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SportsCricket,
                                    contentDescription = null,
                                    tint = PitchDark,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column {
                                Text(
                                    text = "Matches & Live Streams",
                                    color = TextPrimary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = "Online Cloud Sync • Slow Net • Offline Ground",
                                    color = CricketGreen,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        // Create / Add Match
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(StadiumGold.copy(alpha = 0.15f))
                                .border(1.dp, StadiumGold.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .clickable { onCreateNewMatch() }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = StadiumGold,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "New Match",
                                    color = StadiumGold,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Filter Chips (Live Now, All Matches, Completed)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedFilter == "LIVE",
                            onClick = { selectedFilter = "LIVE" },
                            label = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(DrsOutRed.copy(alpha = pulseAlpha))
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text("🔴 Live Now (${matches.count { it.status == "LIVE" }})", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = DrsOutRed.copy(alpha = 0.2f),
                                selectedLabelColor = DrsOutRed,
                                containerColor = Color(0xFF1E293B),
                                labelColor = TextSecondary
                            )
                        )

                        FilterChip(
                            selected = selectedFilter == "ALL",
                            onClick = { selectedFilter = "ALL" },
                            label = { Text("All Matches (${matches.size})", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CricketGreen.copy(alpha = 0.2f),
                                selectedLabelColor = CricketGreen,
                                containerColor = Color(0xFF1E293B),
                                labelColor = TextSecondary
                            )
                        )

                        FilterChip(
                            selected = selectedFilter == "COMPLETED",
                            onClick = { selectedFilter = "COMPLETED" },
                            label = { Text("Completed", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = HawkEyeCyan.copy(alpha = 0.2f),
                                selectedLabelColor = HawkEyeCyan,
                                containerColor = Color(0xFF1E293B),
                                labelColor = TextSecondary
                            )
                        )
                    }
                }
            }
        }

        // Sidhu Paaji AI Live Commentary Bar (Audio stream in background / foreground)
        item {
            SidhuAiCommentaryBar(
                isEnabled = isSidhuCommentaryEnabled,
                isSpeaking = isSidhuSpeaking,
                currentDialogue = sidhuCurrentDialogue,
                selectedStyle = sidhuVoiceStyle,
                onToggleEnabled = onToggleSidhuCommentary,
                onSelectStyle = onSelectSidhuStyle,
                onTestVoice = onTestSidhuVoice
            )
        }

        // Live Matches List
        items(filteredMatches, key = { it.id }) { match ->
            val isSelected = match.id == selectedMatchId
            val isLive = match.status == "LIVE"
            val hasVideoActive = isLive && (isStreamingPitchCam || isStreamingSideCam)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .border(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = if (isSelected) CricketGreen else Color(0xFF334155),
                        shape = RoundedCornerShape(18.dp)
                    )
                    .clickable { onSelectMatch(match.id) },
                colors = CardDefaults.cardColors(containerColor = PitchCard),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Match Header: Tournament, Venue, and Live Badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = match.tournamentName,
                                color = StadiumGold,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = match.venue,
                                color = TextSecondary,
                                fontSize = 10.sp
                            )
                        }

                        if (isLive) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (hasVideoActive) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFF831843))
                                            .border(0.5.dp, Color(0xFFF43F5E), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Videocam,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(11.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = "VIDEO LIVE",
                                                color = Color.White,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Black
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(DrsOutRed.copy(alpha = pulseAlpha))
                                        .padding(horizontal = 7.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = "🔴 LIVE",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF334155))
                                    .padding(horizontal = 7.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = match.status,
                                    color = TextSecondary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Teams and Score Board
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Team A
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Color(match.teamAColorHex))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = match.teamA,
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = match.teamAFirstInningsScore,
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }

                        // Team B (Batting)
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.End
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = match.teamB,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Color(match.teamBColorHex))
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${match.score}/${match.wickets}",
                                    color = CricketGreen,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "(${match.legalBalls / 6}.${match.legalBalls % 6}/${match.totalOvers})",
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    // Equation / Status detail
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF0F172A))
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = match.statusDetail,
                            color = StadiumGold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Realtime Batsman & Bowler on Strike
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🏏 ${match.strikerName}* ${match.strikerRuns}(${match.strikerBalls})",
                            color = Color.White,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "🎯 ${match.bowlerName} ${match.bowlerWickets}/${match.bowlerRuns}",
                            color = HawkEyeCyan,
                            fontSize = 11.sp
                        )
                    }

                    // Recent Over Trail
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(text = "This Over: ", color = TextSecondary, fontSize = 10.sp)
                        val recentBalls = currentBallEvents.takeLast(6)
                        if (recentBalls.isEmpty()) {
                            listOf("0", "1", "4", "0", "6", "1").forEach { b ->
                                BallBadgeCircle(text = b)
                            }
                        } else {
                            recentBalls.forEach { event ->
                                val text = when {
                                    event.isWicket -> "W"
                                    event.runs == 6 -> "6"
                                    event.runs == 4 -> "4"
                                    else -> event.runs.toString()
                                }
                                BallBadgeCircle(text = text)
                            }
                        }
                    }

                    // AI Match Wrap-up & Summary Bar
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF161E2E))
                            .border(1.dp, StadiumGold.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                            .clickable {
                                onSelectMatch(match.id)
                                onOpenAiSummary(match.id)
                            }
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = StadiumGold,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "AI Sidhu Paaji Match Wrap-up",
                                color = StadiumGold,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "Summary Suno ➔",
                            color = CricketGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // ACTION BUTTONS (CricHeroes Style: Watch Live Video + Score & Full Scorecard)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onWatchLiveVideoAndScore(match.id) },
                            modifier = Modifier.weight(1.3f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (hasVideoActive) DrsOutRed else CricketGreen
                            )
                        ) {
                            Icon(
                                imageVector = if (hasVideoActive) Icons.Default.Videocam else Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = if (hasVideoActive) Color.White else PitchDark,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (hasVideoActive) "Watch Video + Score" else "Live Match Center",
                                color = if (hasVideoActive) Color.White else PitchDark,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        OutlinedButton(
                            onClick = { onOpenFullScorecard(match.id) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF475569))
                        ) {
                            Icon(
                                imageVector = Icons.Default.SportsCricket,
                                contentDescription = null,
                                tint = TextPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Scorecard",
                                color = TextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // WhatsApp Share for Parents at home
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF25D366).copy(alpha = 0.15f))
                                .border(1.dp, Color(0xFF25D366).copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                .clickable {
                                    val shareText = "🏏 AyuuCric Live Match Update:\n" +
                                        "🏆 ${match.tournamentName}\n" +
                                        "${match.teamA} vs ${match.teamB}\n" +
                                        "Score: ${match.score}/${match.wickets} (${match.legalBalls / 6}.${match.legalBalls % 6} ov)\n" +
                                        "Batting: ${match.strikerName} ${match.strikerRuns}* (${match.strikerBalls})\n" +
                                        "${match.statusDetail}\n" +
                                        "Watch live on AyuuCric Live App!"
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, shareText)
                                        type = "text/plain"
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "Share Live Score with Family"))
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share to WhatsApp",
                                tint = Color(0xFF25D366),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BallBadgeCircle(text: String) {
    val (bg, textColor) = when (text) {
        "W" -> Pair(DrsOutRed, Color.White)
        "6" -> Pair(StadiumGold, PitchDark)
        "4" -> Pair(CricketGreen, PitchDark)
        else -> Pair(Color(0xFF1E293B), TextSecondary)
    }

    Box(
        modifier = Modifier
            .size(18.dp)
            .clip(CircleShape)
            .background(bg),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}
