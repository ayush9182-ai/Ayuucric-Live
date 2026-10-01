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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SportsCricket
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.graphicsLayer
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
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun PulsingLiveDot(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_dot")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dotAlpha"
    )
    Box(
        modifier = modifier
            .graphicsLayer { alpha = pulseAlpha }
            .clip(CircleShape)
            .background(DrsOutRed)
    )
}

@Composable
fun PulsingLiveBadge(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_badge")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "badgeAlpha"
    )
    Box(
        modifier = modifier
            .graphicsLayer { alpha = pulseAlpha }
            .clip(RoundedCornerShape(6.dp))
            .background(DrsOutRed)
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
    onOpenAiSummary: (String) -> Unit = {},
    currentRole: com.example.data.model.DeviceRole = com.example.data.model.DeviceRole.SPECTATOR_VIEWER,
    onDeleteMatch: ((String) -> Unit)? = null,
    onSyncCloud: () -> Unit = {},
    onEditVenue: ((MatchEntity) -> Unit)? = null
) {
    val context = LocalContext.current
    var selectedFilter by remember { mutableStateOf("LIVE") } // "LIVE", "ALL", "COMPLETED"
    var matchToDelete by remember { mutableStateOf<MatchEntity?>(null) }

    // Match Delete Confirmation Dialog
    matchToDelete?.let { targetMatch ->
        AlertDialog(
            onDismissRequest = { matchToDelete = null },
            containerColor = Color(0xFF0F172A),
            shape = RoundedCornerShape(16.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = null, tint = DrsOutRed)
                    Text("Delete Match?", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Text(
                    "Are you sure you want to delete '${targetMatch.teamA} vs ${targetMatch.teamB}'? All score records will be permanently removed.",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val idToDelete = targetMatch.id
                        matchToDelete = null
                        onDeleteMatch?.invoke(idToDelete)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DrsOutRed),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Delete Match", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { matchToDelete = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    val filteredMatches = when (selectedFilter) {
        "LIVE" -> matches.filter { it.status == "LIVE" }
        "COMPLETED" -> matches.filter { it.status != "LIVE" }
        else -> matches
    }

    val activeLiveMatch = matches.find { it.status == "LIVE" }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(PitchDark)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Dynamic Home Screen Widget Card (Transforms between Standby Card and Live Scorecard)
        item {
            HomeScreenLiveScoreWidgetCard(
                liveMatch = activeLiveMatch,
                sidhuDialogue = sidhuCurrentDialogue,
                onCreateNewMatch = onCreateNewMatch,
                onWatchLiveVideoAndScore = onWatchLiveVideoAndScore,
                onOpenFullScorecard = onOpenFullScorecard,
                onSyncCloud = onSyncCloud
            )
        }

        // Section Header with Filter Chips (Clean, uncluttered, no redundant buttons)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Matches (${matches.size})",
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val liveCount = matches.count { it.status == "LIVE" }
                    FilterChip(
                        selected = selectedFilter == "LIVE",
                        onClick = { selectedFilter = "LIVE" },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (liveCount > 0) {
                                    PulsingLiveDot(modifier = Modifier.size(6.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Text(
                                    text = "Live ($liveCount)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
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
                        label = {
                            Text(
                                text = "All",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        },
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
                        label = {
                            Text(
                                text = "Recent",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        },
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

        // Live Matches List
        if (filteredMatches.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = PitchCard),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(CricketGreen.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SportsCricket,
                                    contentDescription = null,
                                    tint = CricketGreen,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = if (selectedFilter == "LIVE") "No Live Matches" else "No Matches Available",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (selectedFilter == "LIVE") "All current fixtures are concluded or not started yet." else "Create a new match or sync fixtures.",
                                fontSize = 12.sp,
                                color = TextSecondary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(0.9f),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (selectedFilter == "LIVE" && matches.isNotEmpty()) {
                                    Button(
                                        onClick = { selectedFilter = "ALL" },
                                        colors = ButtonDefaults.buttonColors(containerColor = HawkEyeCyan),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f).height(42.dp)
                                    ) {
                                        Text("View All Matches", color = PitchDark, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                } else {
                                    Button(
                                        onClick = { onCreateNewMatch() },
                                        colors = ButtonDefaults.buttonColors(containerColor = StadiumGold),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f).height(42.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, tint = PitchDark, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Start Match", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = PitchDark)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

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
                            val isWeird = com.example.data.maps.GoogleMapsGroundingService.isWeirdVenueName(match.venue)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier
                                    .padding(top = 2.dp)
                                    .clickable {
                                        if (onEditVenue != null) {
                                            onEditVenue(match)
                                        } else {
                                            com.example.data.maps.GoogleMapsGroundingService.openGoogleMapsForVenue(
                                                context = context,
                                                venue = match.venue,
                                                address = match.venueAddress,
                                                coordinates = match.venueCoordinates
                                            )
                                        }
                                    }
                            ) {
                                Text(
                                    text = "📍",
                                    fontSize = 10.sp
                                )
                                Text(
                                    text = if (isWeird) "⚠️ ${match.venue.take(16)}... (Edit)" else match.venue.ifBlank { "Turf Ground" } +
                                            if (match.matchDate.isNotBlank()) " • ${match.matchDate}" else "" +
                                            if (match.matchTime.isNotBlank()) " ${match.matchTime}" else "",
                                    color = if (isWeird) StadiumGold else TextSecondary,
                                    fontSize = 10.sp,
                                    fontWeight = if (isWeird) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1
                                )
                                if (onEditVenue != null) {
                                    Text(
                                        text = "✏️",
                                        fontSize = 9.sp
                                    )
                                }
                            }
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

                                PulsingLiveBadge()
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
                    val isBattingA = match.battingTeam.equals(match.teamA, ignoreCase = true)
                    val liveScoreText = "${match.score}/${match.wickets}"
                    val liveOversText = "(${match.legalBalls / 6}.${match.legalBalls % 6}/${match.totalOvers})"

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
                                    color = if (isBattingA) Color.White else TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                if (isBattingA) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("🏏", fontSize = 10.sp)
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            if (isBattingA) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = liveScoreText,
                                        color = CricketGreen,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = liveOversText,
                                        color = TextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                            } else {
                                Text(
                                    text = if (match.currentInnings == 2) match.teamAFirstInningsScore else "Yet to bat",
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        // Team B
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.End
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (!isBattingA) {
                                    Text("🏏", fontSize = 10.sp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Text(
                                    text = match.teamB,
                                    color = if (!isBattingA) Color.White else TextPrimary,
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
                            if (!isBattingA) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = liveScoreText,
                                        color = CricketGreen,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = liveOversText,
                                        color = TextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                            } else {
                                Text(
                                    text = if (match.currentInnings == 2) match.teamAFirstInningsScore else "Yet to bat",
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

                    // Real Current Over Trail (Shows exact batsman & runs per ball)
                    Spacer(modifier = Modifier.height(8.dp))
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(text = "This Over (${(match.legalBalls / 6) + 1}): ", color = StadiumGold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            val currentOverIndex = match.legalBalls / 6
                            val thisOverBalls = currentBallEvents.filter { it.overNumber == currentOverIndex }.sortedBy { it.ballInOver }
                            if (thisOverBalls.isEmpty()) {
                                Text(
                                    text = "Over starting (0/6)",
                                    color = TextMuted,
                                    fontSize = 10.sp,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                )
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    thisOverBalls.forEach { event ->
                                        val text = when {
                                            event.isWicket -> "W"
                                            event.runs == 6 -> "6"
                                            event.runs == 4 -> "4"
                                            event.extraType != "None" -> event.extraType.take(2)
                                            else -> event.runs.toString()
                                        }
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color(0xFF1E293B))
                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                        ) {
                                            BallBadgeCircle(text = text)
                                            Spacer(modifier = Modifier.width(3.dp))
                                            val shortBatter = event.batsman.trim().split(" ").firstOrNull() ?: "Bat"
                                            Text(
                                                text = "$shortBatter:${event.runs}r",
                                                fontSize = 9.sp,
                                                color = TextSecondary,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // AI Match Wrap-up & Summary Bar
                    Spacer(modifier = Modifier.height(10.dp))

                    // ACTION BUTTONS (CricHeroes Style: Watch Live Video + Score & Full Scorecard)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onWatchLiveVideoAndScore(match.id) },
                            modifier = Modifier
                                .weight(1.3f)
                                .height(40.dp),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
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
                                text = if (hasVideoActive) "Video & Score" else "Match Center",
                                color = if (hasVideoActive) Color.White else PitchDark,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        OutlinedButton(
                            onClick = { onOpenFullScorecard(match.id) },
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
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
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
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

                        // Direct Delete Match for Official Scorer & Admin
                        if (onDeleteMatch != null) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(DrsOutRed.copy(alpha = 0.15f))
                                    .border(1.dp, DrsOutRed.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                    .clickable { matchToDelete = match },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Delete Match",
                                    tint = DrsOutRed,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HomeScreenLiveScoreWidgetCard(
    liveMatch: MatchEntity?,
    sidhuDialogue: String = "",
    onCreateNewMatch: () -> Unit,
    onWatchLiveVideoAndScore: (String) -> Unit,
    onOpenFullScorecard: (String) -> Unit,
    onSyncCloud: () -> Unit
) {
    if (liveMatch != null) {
        // STATE 1: MATCH IS LIVE -> DYNAMIC IPL SCORECARD HERO WIDGET
        val oversStr = "${liveMatch.legalBalls / 6}.${liveMatch.legalBalls % 6}"
        val crr = if (liveMatch.legalBalls > 0) {
            String.format("%.2f", (liveMatch.score.toFloat() / liveMatch.legalBalls) * 6)
        } else "0.00"

        val equation = if (liveMatch.target > 0 && liveMatch.legalBalls < (liveMatch.totalOvers * 6)) {
            val needed = (liveMatch.target - liveMatch.score).coerceAtLeast(0)
            val ballsLeft = ((liveMatch.totalOvers * 6) - liveMatch.legalBalls).coerceAtLeast(1)
            val rrr = String.format("%.2f", (needed.toFloat() / ballsLeft) * 6)
            "Target: ${liveMatch.target} • Need $needed runs in $ballsLeft balls (RRR: $rrr)"
        } else {
            liveMatch.statusDetail.ifBlank { "1st Innings • In Progress" }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, DrsOutRed.copy(alpha = 0.6f))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // Top Tag Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PulsingLiveDot(modifier = Modifier.size(8.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "LIVE SCORE",
                            color = DrsOutRed,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF1E293B))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "CRR: $crr",
                            color = StadiumGold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Teams and Big Live Score
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${liveMatch.teamA} vs ${liveMatch.teamB}",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 1
                        )
                        Text(
                            text = liveMatch.tournamentName.ifBlank { "Local Match" } + if (liveMatch.venue.isNotBlank()) " • ${liveMatch.venue}" else "",
                            color = TextMuted,
                            fontSize = 11.sp,
                            maxLines = 1
                        )
                    }

                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "${liveMatch.score}/${liveMatch.wickets}",
                            color = CricketGreen,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "($oversStr ov)",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Current Batsmen & Bowler IPL Mini-strip
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = PitchDark,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "🏏 ${liveMatch.strikerName}* ${liveMatch.strikerRuns}(${liveMatch.strikerBalls})",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${liveMatch.nonStrikerName} ${liveMatch.nonStrikerRuns}(${liveMatch.nonStrikerBalls})",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        val bowlerOvers = "${liveMatch.bowlerBalls / 6}.${liveMatch.bowlerBalls % 6}"
                        Text(
                            text = "🎯 ${liveMatch.bowlerName}: $bowlerOvers-${liveMatch.bowlerMaidens}-${liveMatch.bowlerRuns}-${liveMatch.bowlerWickets}",
                            color = HawkEyeCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Match Status Equation
                Text(
                    text = equation,
                    color = StadiumGold,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )

                // Sidhu Paaji Commentary Ticker
                if (sidhuDialogue.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF1E293B).copy(alpha = 0.8f),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, StadiumGold.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🎙️", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = sidhuDialogue,
                                color = StadiumGold,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 2
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onWatchLiveVideoAndScore(liveMatch.id) },
                        modifier = Modifier.weight(1f).height(40.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CricketGreen)
                    ) {
                        Icon(Icons.Default.Videocam, contentDescription = null, tint = PitchDark, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Watch Live", color = PitchDark, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = { onOpenFullScorecard(liveMatch.id) },
                        modifier = Modifier.weight(1f).height(40.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
                    ) {
                        Text("Scorecard", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    } else {
        // STATE 2: NO MATCH LIVE -> HOME STANDBY WIDGET CARD
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF1E293B))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(CricketGreen))
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "MATCH ARENA",
                                color = TextSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(CricketGreen.copy(alpha = 0.15f))
                            .border(0.8.dp, CricketGreen.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "STANDBY",
                            color = CricketGreen,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(CricketGreen.copy(alpha = 0.15f))
                            .border(1.dp, CricketGreen.copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🏏", fontSize = 22.sp)
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "No Live Match",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Start a match to begin live scoring & broadcast.",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onCreateNewMatch,
                        modifier = Modifier.weight(1f).height(40.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = StadiumGold)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = PitchDark, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Start Match", color = PitchDark, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = onSyncCloud,
                        modifier = Modifier.weight(1f).height(40.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = HawkEyeCyan),
                        border = androidx.compose.foundation.BorderStroke(1.dp, HawkEyeCyan.copy(alpha = 0.5f))
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = HawkEyeCyan, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Sync Cloud", fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
