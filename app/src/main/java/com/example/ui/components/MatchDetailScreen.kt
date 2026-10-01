package com.example.ui.components

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SportsCricket
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BallEventEntity
import com.example.data.model.DeviceRole
import com.example.data.model.MatchEntity
import com.example.ui.theme.CricketGreen
import com.example.ui.theme.DrsOutRed
import com.example.ui.theme.HawkEyeCyan
import com.example.ui.theme.PitchCard
import com.example.ui.theme.PitchCardBorder
import com.example.ui.theme.PitchDark
import com.example.ui.theme.PitchSurface
import com.example.ui.theme.StadiumGold
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.CommentaryFilter
import java.util.Locale

/**
 * MatchDetailScreen Component
 * Displays a live cricket scoreboard, current run rate (CRR & RRR metrics),
 * and ball-by-ball commentary list for a selected cricket match.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchDetailScreen(
    match: MatchEntity?,
    ballEvents: List<BallEventEntity>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    commentaryFilter: CommentaryFilter = CommentaryFilter.ALL,
    onSelectFilter: (CommentaryFilter) -> Unit = {},
    currentRole: DeviceRole = DeviceRole.SPECTATOR_VIEWER,
    onOpenScorer: () -> Unit = {},
    onOpenDrsReview: () -> Unit = {},
    onRefresh: () -> Unit = {},
    onEditVenue: (() -> Unit)? = null
) {
    BackHandler { onBack() }
    val context = LocalContext.current

    var internalFilter by remember { mutableStateOf(commentaryFilter) }
    val effectiveFilter = if (commentaryFilter != CommentaryFilter.ALL) commentaryFilter else internalFilter

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("match_detail_screen"),
        containerColor = PitchDark,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (match != null) "${match.teamA} vs ${match.teamB}" else "Match Details",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (match != null) {
                                "${match.tournamentName.ifBlank { "Cricket Match" }} • ${match.venue.ifBlank { "Turf Ground" }}"
                            } else {
                                "Live Scorecard & Commentary"
                            },
                            color = StadiumGold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("match_detail_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = CricketGreen
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (match != null) {
                                val shareSummary = buildString {
                                    append("🏏 *${match.teamA} vs ${match.teamB}*\n")
                                    append("${match.tournamentName}\n\n")
                                    append("📊 Score: ${match.battingTeam} ${match.score}/${match.wickets} (${match.legalBalls / 6}.${match.legalBalls % 6} ov)\n")
                                    val crr = if (match.legalBalls > 0) String.format(Locale.US, "%.2f", (match.score.toFloat() / match.legalBalls) * 6f) else "0.00"
                                    append("⚡ CRR: $crr\n")
                                    if (match.currentInnings == 2 && match.target > 0) {
                                        append("🎯 Target: ${match.target}\n")
                                    }
                                    append("📢 Status: ${match.statusDetail}\n")
                                    append("\nTrack live ball-by-ball on CricLive App!")
                                }
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, shareSummary)
                                }
                                context.startActivity(Intent.createChooser(intent, "Share Match Score"))
                            }
                        },
                        modifier = Modifier.testTag("match_detail_share_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share Match",
                            tint = TextPrimary
                        )
                    }

                    IconButton(
                        onClick = onRefresh,
                        modifier = Modifier.testTag("match_detail_refresh_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = HawkEyeCyan
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PitchSurface,
                    navigationIconContentColor = CricketGreen,
                    titleContentColor = TextPrimary,
                    actionIconContentColor = TextPrimary
                )
            )
        }
    ) { innerPadding ->
        if (match == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .testTag("match_detail_empty_state"),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .widthIn(max = 480.dp),
                    colors = CardDefaults.cardColors(containerColor = PitchCard),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, PitchCardBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("🏏", fontSize = 40.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No Match Selected",
                            color = TextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Please select an ongoing or completed cricket fixture from the matches list.",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onBack,
                            colors = ButtonDefaults.buttonColors(containerColor = CricketGreen, contentColor = PitchDark),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Back to Matches", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            return@Scaffold
        }

        val filteredBalls = remember(ballEvents, effectiveFilter) {
            when (effectiveFilter) {
                CommentaryFilter.ALL -> ballEvents
                CommentaryFilter.BOUNDARIES -> ballEvents.filter { it.isBoundary || it.isSix || it.runs >= 4 }
                CommentaryFilter.WICKETS -> ballEvents.filter { it.isWicket }
                CommentaryFilter.OVERS -> ballEvents.filter { it.ballInOver == 6 || it.ballInOver == 1 }
            }
        }

        val isOfficialScorer = currentRole == DeviceRole.OFFICIAL_SCORER
        val isMatchLive = match.status == "LIVE"
        val isMatchFinished = match.status == "FINISHED"

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 14.dp),
            contentPadding = PaddingValues(top = 10.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Container centered on large screens
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 640.dp)
                ) {
                    // 1. LIVE SCOREBOARD CARD
                    LiveScoreboardCard(
                        match = match,
                        onEditVenue = onEditVenue
                    )
                }
            }

            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 640.dp)
                ) {
                    // 2. CURRENT RUN RATE & REQUIRED RUN RATE METRICS CARD
                    RunRateAnalysisCard(
                        match = match,
                        ballEvents = ballEvents
                    )
                }
            }

            // Quick Scorer / DRS review trigger bar for authorized officials
            if (isOfficialScorer && !isMatchFinished) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 640.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onOpenScorer,
                            colors = ButtonDefaults.buttonColors(containerColor = CricketGreen, contentColor = PitchDark),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("match_detail_open_scorer_button")
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, tint = PitchDark, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Open Scoring Pad", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = onOpenDrsReview,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = StadiumGold),
                            border = BorderStroke(1.dp, StadiumGold),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("match_detail_open_drs_button")
                        ) {
                            Icon(Icons.Default.Policy, contentDescription = null, tint = StadiumGold, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("DRS Review", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }

            // 3. BALL-BY-BALL COMMENTARY SECTION HEADER & FILTER CHIPS
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 640.dp)
                        .testTag("match_detail_commentary_header")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Campaign,
                                contentDescription = null,
                                tint = CricketGreen,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Ball-by-Ball Commentary",
                                color = TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = "${ballEvents.size} deliveries",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Commentary Filter Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val boundaryCount = ballEvents.count { it.isBoundary || it.isSix || it.runs >= 4 }
                        val wicketCount = ballEvents.count { it.isWicket }

                        FilterChip(
                            selected = effectiveFilter == CommentaryFilter.ALL,
                            onClick = {
                                internalFilter = CommentaryFilter.ALL
                                onSelectFilter(CommentaryFilter.ALL)
                            },
                            label = { Text("All Balls (${ballEvents.size})", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CricketGreen,
                                selectedLabelColor = PitchDark,
                                containerColor = PitchCard,
                                labelColor = TextSecondary
                            )
                        )

                        FilterChip(
                            selected = effectiveFilter == CommentaryFilter.WICKETS,
                            onClick = {
                                internalFilter = CommentaryFilter.WICKETS
                                onSelectFilter(CommentaryFilter.WICKETS)
                            },
                            label = { Text("Wickets ($wicketCount)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = DrsOutRed,
                                selectedLabelColor = Color.White,
                                containerColor = PitchCard,
                                labelColor = TextSecondary
                            )
                        )

                        FilterChip(
                            selected = effectiveFilter == CommentaryFilter.BOUNDARIES,
                            onClick = {
                                internalFilter = CommentaryFilter.BOUNDARIES
                                onSelectFilter(CommentaryFilter.BOUNDARIES)
                            },
                            label = { Text("Boundaries ($boundaryCount)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = StadiumGold,
                                selectedLabelColor = PitchDark,
                                containerColor = PitchCard,
                                labelColor = TextSecondary
                            )
                        )

                        FilterChip(
                            selected = effectiveFilter == CommentaryFilter.OVERS,
                            onClick = {
                                internalFilter = CommentaryFilter.OVERS
                                onSelectFilter(CommentaryFilter.OVERS)
                            },
                            label = { Text("Overs Breakdown", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = HawkEyeCyan,
                                selectedLabelColor = PitchDark,
                                containerColor = PitchCard,
                                labelColor = TextSecondary
                            )
                        )
                    }
                }
            }

            // 4. COMMENTARY LIST ITEMS
            if (filteredBalls.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 640.dp)
                            .padding(vertical = 12.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = PitchCard),
                        border = BorderStroke(1.dp, PitchCardBorder)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Campaign,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (ballEvents.isEmpty()) "No commentary yet" else "No balls match selected filter",
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (ballEvents.isEmpty()) {
                                    "Commentary will update live ball-by-ball once play begins."
                                } else {
                                    "Try selecting 'All Balls' filter to see every recorded delivery."
                                },
                                color = TextSecondary,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(filteredBalls, key = { it.id }) { ball ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 640.dp)
                    ) {
                        DetailCommentaryBallItem(ball = ball)
                    }
                }
            }
        }
    }
}

/**
 * Live Scoreboard Card displaying match scores, teams, batsmen on crease, and active bowler
 */
@Composable
private fun LiveScoreboardCard(
    match: MatchEntity,
    onEditVenue: (() -> Unit)? = null
) {
    val isBattingA = match.battingTeam.equals(match.teamA, ignoreCase = true)
    val isLive = match.status == "LIVE"
    val isBreak = match.status == "INNINGS_BREAK"
    val isFinished = match.status == "FINISHED"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("match_detail_scoreboard"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = PitchSurface),
        border = BorderStroke(
            1.2.dp,
            if (isLive) CricketGreen.copy(alpha = 0.6f) else PitchCardBorder
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Top Row: Tournament, Venue, and Live / Finished Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = match.tournamentName.ifBlank { "Cricket Fixture" },
                        color = StadiumGold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text("•", color = TextMuted, fontSize = 10.sp)
                    Text(
                        text = match.venue.ifBlank { "Turf Ground" },
                        color = TextSecondary,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (onEditVenue != null) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Venue",
                            tint = StadiumGold,
                            modifier = Modifier
                                .size(12.dp)
                                .clickable { onEditVenue() }
                        )
                    }
                }

                // Match Status Pill
                val statusText = when {
                    isLive -> "LIVE 🔴"
                    isBreak -> "INNINGS BREAK ⏸️"
                    isFinished -> "FINISHED 🏆"
                    else -> match.status
                }
                val statusBg = when {
                    isLive -> CricketGreen.copy(alpha = 0.15f)
                    isBreak -> StadiumGold.copy(alpha = 0.15f)
                    isFinished -> Color(0xFF1E293B)
                    else -> PitchCard
                }
                val statusColor = when {
                    isLive -> CricketGreen
                    isBreak -> StadiumGold
                    isFinished -> Color.White
                    else -> TextSecondary
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(statusBg)
                        .border(0.8.dp, statusColor.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = statusText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = statusColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Teams & Scoreboard Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Team A Column
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
                            color = if (isBattingA) Color.White else TextSecondary,
                            fontSize = 13.sp,
                            fontWeight = if (isBattingA) FontWeight.Black else FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
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
                                text = "${match.score}/${match.wickets}",
                                color = CricketGreen,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "(${match.legalBalls / 6}.${match.legalBalls % 6} ov)",
                                color = TextSecondary,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    } else {
                        Text(
                            text = if (match.currentInnings == 2) match.teamAFirstInningsScore.ifBlank { "Innings closed" } else "Yet to bat",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Center VS Badge
                Box(
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0F172A))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "VS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = StadiumGold
                    )
                }

                // Team B Column
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
                            color = if (!isBattingA) Color.White else TextSecondary,
                            fontSize = 13.sp,
                            fontWeight = if (!isBattingA) FontWeight.Black else FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
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
                                text = "${match.score}/${match.wickets}",
                                color = CricketGreen,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "(${match.legalBalls / 6}.${match.legalBalls % 6} ov)",
                                color = TextSecondary,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    } else {
                        Text(
                            text = if (match.currentInnings == 2) match.teamAFirstInningsScore.ifBlank { "Innings closed" } else "Yet to bat",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Match Equation Banner
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0F172A))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = match.statusDetail.ifBlank {
                        "${match.battingTeam} batting • ${match.score}/${match.wickets} (${match.legalBalls / 6}.${match.legalBalls % 6} / ${match.totalOvers} ov)"
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isFinished) StadiumGold else Color(0xFF93C5FD)
                )
            }

            // Active Players Mini-Table (Batsmen on strike and Bowler)
            if (!isFinished && match.strikerName.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = Color(0xFF1E293B), thickness = 0.8.dp)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Batters on crease
                    Column(modifier = Modifier.weight(1.2f)) {
                        Text(
                            text = "ON CREASE",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        val strikerSr = if (match.strikerBalls > 0) String.format(Locale.US, "%.1f", (match.strikerRuns.toFloat() / match.strikerBalls) * 100f) else "0.0"
                        Text(
                            text = "🏏 ${match.strikerName}*  ${match.strikerRuns} (${match.strikerBalls}b, 4s:${match.strikerFours}, 6s:${match.strikerSixes}) SR:$strikerSr",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (match.nonStrikerName.isNotBlank()) {
                            val nonStrikerSr = if (match.nonStrikerBalls > 0) String.format(Locale.US, "%.1f", (match.nonStrikerRuns.toFloat() / match.nonStrikerBalls) * 100f) else "0.0"
                            Text(
                                text = "    ${match.nonStrikerName}  ${match.nonStrikerRuns} (${match.nonStrikerBalls}b, 4s:${match.nonStrikerFours}, 6s:${match.nonStrikerSixes}) SR:$nonStrikerSr",
                                color = TextSecondary,
                                fontSize = 10.5.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Bowler
                    Column(
                        modifier = Modifier.weight(0.8f),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "BOWLER",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "🎯 ${match.bowlerName}",
                            color = HawkEyeCyan,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val bowlerOversStr = "${match.bowlerBalls / 6}.${match.bowlerBalls % 6}"
                        val econStr = if (match.bowlerBalls > 0) String.format(Locale.US, "%.1f", (match.bowlerRuns.toFloat() / match.bowlerBalls) * 6f) else "0.0"
                        Text(
                            text = "$bowlerOversStr ov • ${match.bowlerWickets}/${match.bowlerRuns} (Econ: $econStr)",
                            color = TextSecondary,
                            fontSize = 10.sp,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

/**
 * Current Run Rate (CRR) & Required Run Rate (RRR) Analysis Card
 * Includes recent over ball badges trail and projected scores
 */
@Composable
private fun RunRateAnalysisCard(
    match: MatchEntity,
    ballEvents: List<BallEventEntity>
) {
    val crr = if (match.legalBalls > 0) {
        (match.score.toFloat() / match.legalBalls) * 6f
    } else {
        0.0f
    }
    val crrFormatted = String.format(Locale.US, "%.2f", crr)

    val maxTotalBalls = match.totalOvers * 6
    val ballsRemaining = (maxTotalBalls - match.legalBalls).coerceAtLeast(0)
    val runsNeeded = (match.target - match.score).coerceAtLeast(0)

    val rrr = if (match.currentInnings == 2 && ballsRemaining > 0 && runsNeeded > 0) {
        (runsNeeded.toFloat() / ballsRemaining) * 6f
    } else {
        0.0f
    }
    val rrrFormatted = String.format(Locale.US, "%.2f", rrr)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("match_detail_crr_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PitchCard),
        border = BorderStroke(1.dp, PitchCardBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Icon + Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = HawkEyeCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Run Rate & Match Pace",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "${match.legalBalls} / $maxTotalBalls balls bowled",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Key Metrics Pill Row: CRR, RRR / Target, Overs Left
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // CRR Box
                Surface(
                    modifier = Modifier.weight(1f),
                    color = Color(0xFF0F172A),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, HawkEyeCyan.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("CURRENT RUN RATE", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = crrFormatted,
                            color = HawkEyeCyan,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text("runs / over", color = TextSecondary, fontSize = 9.5.sp)
                    }
                }

                // RRR Box or Projected Score
                if (match.currentInnings == 2 && match.target > 0) {
                    Surface(
                        modifier = Modifier.weight(1f),
                        color = Color(0xFF0F172A),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(
                            1.dp,
                            if (rrr > 12f) DrsOutRed.copy(alpha = 0.5f) else StadiumGold.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("REQUIRED RUN RATE", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (match.score >= match.target) "TARGET MET" else rrrFormatted,
                                color = if (rrr > 12f) DrsOutRed else StadiumGold,
                                fontSize = if (match.score >= match.target) 13.sp else 20.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = if (match.score >= match.target) "🏆 Won" else "Need $runsNeeded in $ballsRemaining b",
                                color = TextSecondary,
                                fontSize = 9.5.sp
                            )
                        }
                    }
                } else {
                    // Projected Score Box for 1st Innings
                    val projectedCurrent = (crr * match.totalOvers).toInt()
                    Surface(
                        modifier = Modifier.weight(1f),
                        color = Color(0xFF0F172A),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, StadiumGold.copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("PROJECTED TOTAL", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "$projectedCurrent",
                                color = StadiumGold,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text("@ Current CRR (${match.totalOvers} ov)", color = TextSecondary, fontSize = 9.5.sp)
                        }
                    }
                }
            }

            // Recent Deliveries Trail (Current / Last Over visual circles)
            Spacer(modifier = Modifier.height(12.dp))
            val recentBalls = ballEvents.take(12).reversed()
            if (recentBalls.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Recent Deliveries:",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        recentBalls.takeLast(8).forEach { ball ->
                            RecentDeliveryCircleBadge(ball = ball)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Circle badge representing outcome of recent ball (Dot, 1, 2, 4, 6, W, Wd, Nb)
 */
@Composable
private fun RecentDeliveryCircleBadge(ball: BallEventEntity) {
    val text = when {
        ball.isWicket -> "W"
        ball.extraType.equals("Wide", ignoreCase = true) || ball.extraType.equals("WD", ignoreCase = true) -> "Wd"
        ball.extraType.equals("NoBall", ignoreCase = true) || ball.extraType.equals("NB", ignoreCase = true) -> "Nb"
        ball.isSix || ball.runs == 6 -> "6"
        ball.isBoundary || ball.runs == 4 -> "4"
        ball.runs == 0 -> "•"
        else -> ball.runs.toString()
    }

    val (bg, fg) = when {
        ball.isWicket -> Pair(DrsOutRed, Color.White)
        ball.isSix || ball.runs == 6 -> Pair(StadiumGold, PitchDark)
        ball.isBoundary || ball.runs == 4 -> Pair(CricketGreen, PitchDark)
        ball.extraType != "None" -> Pair(Color(0xFF38BDF8), PitchDark)
        ball.runs == 0 -> Pair(Color(0xFF1E293B), TextMuted)
        else -> Pair(Color(0xFF334155), Color.White)
    }

    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(bg),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = if (text.length > 1) 8.5.sp else 10.sp,
            fontWeight = FontWeight.Black,
            color = fg
        )
    }
}

/**
 * Individual Card in the Ball-by-Ball Commentary List
 */
@Composable
private fun DetailCommentaryBallItem(ball: BallEventEntity) {
    val cardBg = when {
        ball.isWicket -> DrsOutRed.copy(alpha = 0.12f)
        ball.isSix || ball.runs == 6 -> StadiumGold.copy(alpha = 0.12f)
        ball.isBoundary || ball.runs == 4 -> CricketGreen.copy(alpha = 0.08f)
        else -> PitchCard
    }

    val cardBorder = when {
        ball.isWicket -> DrsOutRed.copy(alpha = 0.5f)
        ball.isSix || ball.runs == 6 -> StadiumGold.copy(alpha = 0.5f)
        ball.isBoundary || ball.runs == 4 -> CricketGreen.copy(alpha = 0.35f)
        else -> PitchCardBorder
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("commentary_ball_${ball.overNumber}_${ball.ballInOver}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(0.9.dp, cardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Left: Over.ball number and Result Badge
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(46.dp)
            ) {
                Text(
                    text = "${ball.overNumber}.${ball.ballInOver}",
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp,
                    color = HawkEyeCyan
                )

                Spacer(modifier = Modifier.height(4.dp))

                RecentDeliveryCircleBadge(ball = ball)
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Right: Bowler to Batsman matchup, delivery tags, and commentary text
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${ball.bowler} to ${ball.batsman}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    if (ball.pitchZone.isNotBlank() && ball.pitchZone != "None") {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF0F172A))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = ball.pitchZone,
                                fontSize = 9.5.sp,
                                color = TextMuted,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = ball.commentary.ifBlank {
                        if (ball.isWicket) "OUT! A crucial wicket falls!" else "${ball.runs} run(s) scored."
                    },
                    fontSize = 12.5.sp,
                    color = if (ball.isWicket) Color.White else TextSecondary,
                    lineHeight = 17.sp
                )
            }
        }
    }
}
