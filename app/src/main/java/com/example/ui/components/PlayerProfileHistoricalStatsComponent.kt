package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.firebase.PlayerStatsSyncService
import com.example.data.model.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch

/**
 * High-fidelity Player Profile UI component that displays comprehensive
 * historical batting and bowling statistics, fetchable directly from Firebase Firestore.
 */
@Composable
fun PlayerHistoricalStatsDialog(
    playerIdOrUsername: String,
    initialProfile: CricHeroesProfile? = null,
    onDismiss: () -> Unit,
    onOpenDmWithPlayer: ((CricHeroesProfile) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .padding(vertical = 12.dp)
                .testTag("player_historical_stats_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = PitchDark),
            border = BorderStroke(
                1.5.dp,
                Brush.linearGradient(listOf(StadiumGold, CricketGreen, HawkEyeCyan, StadiumGold))
            )
        ) {
            PlayerProfileHistoricalStatsComponent(
                playerIdOrUsername = playerIdOrUsername,
                initialProfile = initialProfile,
                onDismiss = onDismiss,
                onOpenDmWithPlayer = onOpenDmWithPlayer,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun PlayerProfileHistoricalStatsComponent(
    playerIdOrUsername: String,
    initialProfile: CricHeroesProfile? = null,
    onDismiss: (() -> Unit)? = null,
    onOpenDmWithPlayer: ((CricHeroesProfile) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var selectedTab by remember { mutableStateOf(0) } // 0: Overview, 1: Batting, 2: Bowling, 3: Match Logs
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var statsData by remember { mutableStateOf<PlayerHistoricalStats?>(null) }
    var isRefreshing by remember { mutableStateOf(false) }

    // Rotate animation for refresh icon
    val infiniteTransition = rememberInfiniteTransition(label = "rotation")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "refresh_spin"
    )

    fun loadStats() {
        coroutineScope.launch {
            isLoading = statsData == null
            isRefreshing = true
            errorMessage = null
            val result = PlayerStatsSyncService.fetchHistoricalStats(playerIdOrUsername)
            result.onSuccess { stats ->
                statsData = stats
                isLoading = false
                isRefreshing = false
            }.onFailure { err ->
                errorMessage = err.localizedMessage ?: "Failed to fetch from Firestore"
                isLoading = false
                isRefreshing = false
            }
        }
    }

    LaunchedEffect(playerIdOrUsername) {
        loadStats()
    }

    Column(
        modifier = modifier
            .background(PitchDark)
            .padding(16.dp)
            .testTag("player_stats_component")
    ) {
        // Top Bar: Title + Cloud Sync status + Refresh + Close
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
                        .background(Brush.linearGradient(listOf(Color(0xFFFF5252), Color(0xFFFF7A00)))),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🏏", fontSize = 16.sp)
                }

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "AYUUCRIC",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = StadiumGold
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = "Verified",
                            tint = HawkEyeCyan,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                    Text(
                        text = "Official Player Profile & Career Stats",
                        fontSize = 9.5.sp,
                        color = TextSecondary
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Cloud Sync Indicator
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(CricketGreen.copy(alpha = 0.15f))
                        .border(0.8.dp, CricketGreen.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(CricketGreen)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Firestore Cloud",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = CricketGreen
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Refresh button
                IconButton(
                    onClick = { loadStats() },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh from Firestore",
                        tint = StadiumGold,
                        modifier = Modifier
                            .size(18.dp)
                            .rotate(if (isRefreshing) rotationAngle else 0f)
                    )
                }

                if (onDismiss != null) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Segmented Tabs
        val tabs = listOf("Overview", "Batting", "Bowling", "Match Logs")
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color(0xFF0F172A),
            contentColor = StadiumGold,
            edgePadding = 0.dp,
            divider = {},
            indicator = {},
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
        ) {
            tabs.forEachIndexed { index, tabTitle ->
                val isSelected = selectedTab == index
                Box(
                    modifier = Modifier
                        .clickable { selectedTab = index }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) CricketGreen else Color.Transparent)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tabTitle,
                        fontSize = 11.5.sp,
                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                        color = if (isSelected) PitchDark else TextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Content Area
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = StadiumGold, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Fetching statistics from Firestore...",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }
            errorMessage != null && statsData == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.CloudOff,
                            contentDescription = null,
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = errorMessage ?: "Failed to load player stats",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { loadStats() },
                            colors = ButtonDefaults.buttonColors(containerColor = StadiumGold)
                        ) {
                            Text("Retry Fetch", color = PitchDark, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            statsData != null -> {
                val stats = statsData!!
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    when (selectedTab) {
                        0 -> {
                            // OVERVIEW: Exact Pro Pass Card spec + Key Highlights + Milestones
                            item {
                                AyuuCricProPassCard(stats = stats)
                            }
                            item {
                                KeyCareerHighlightsRow(stats = stats)
                            }
                            item {
                                MilestonesSection(milestones = stats.milestones)
                            }
                        }
                        1 -> {
                            // BATTING MASTERCLASS
                            item {
                                BattingStatsGrid(batting = stats.batting)
                            }
                            item {
                                BoundaryDistributionCard(batting = stats.batting)
                            }
                            item {
                                Text(
                                    text = "RECENT INNINGS BREAKDOWN",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = StadiumGold,
                                    letterSpacing = 1.sp,
                                    modifier = Modifier.padding(top = 6.dp)
                                )
                            }
                            items(stats.batting.recentInnings) { inning ->
                                BattingInningCard(inning = inning)
                            }
                        }
                        2 -> {
                            // BOWLING ARSENAL
                            item {
                                BowlingStatsGrid(bowling = stats.bowling)
                            }
                            item {
                                BowlingControlMetricCard(bowling = stats.bowling)
                            }
                            item {
                                Text(
                                    text = "RECENT BOWLING SPELLS",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = HawkEyeCyan,
                                    letterSpacing = 1.sp,
                                    modifier = Modifier.padding(top = 6.dp)
                                )
                            }
                            items(stats.bowling.recentSpells) { spell ->
                                BowlingSpellCard(spell = spell)
                            }
                        }
                        3 -> {
                            // MATCH LOGS (COMBINED)
                            item {
                                Text(
                                    text = "CAREER MATCH HISTORY & LOGS",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = CricketGreen,
                                    letterSpacing = 1.sp
                                )
                            }
                            val maxCount = maxOf(stats.batting.recentInnings.size, stats.bowling.recentSpells.size)
                            for (i in 0 until maxCount) {
                                val inn = stats.batting.recentInnings.getOrNull(i)
                                val sp = stats.bowling.recentSpells.getOrNull(i)
                                item {
                                    CombinedMatchLogCard(inning = inn, spell = sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Action Buttons at bottom (DM, Dismiss)
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (onOpenDmWithPlayer != null && statsData != null) {
                Button(
                    onClick = {
                        val prof = CricHeroesProfile(
                            id = statsData!!.playerId,
                            uid = statsData!!.uid,
                            username = statsData!!.username,
                            fullName = statsData!!.fullName,
                            jerseyName = statsData!!.jerseyName,
                            jerseyNumber = statsData!!.jerseyNumber,
                            primaryRole = statsData!!.primaryRole,
                            avatarEmoji = statsData!!.avatarEmoji,
                            teamName = statsData!!.teamName,
                            city = statsData!!.city
                        )
                        onOpenDmWithPlayer(prof)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CricketGreen),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                ) {
                    Icon(imageVector = Icons.Default.Chat, contentDescription = null, tint = PitchDark, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Direct Message", color = PitchDark, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            if (onDismiss != null) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF334155)),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                ) {
                    Text("Close", color = TextSecondary, fontSize = 12.sp)
                }
            }
        }
    }
}

/**
 * EXACT Visual Reproduction of the AYUUCRIC PRO PASS Card shown in user's screenshot.
 */
@Composable
fun AyuuCricProPassCard(stats: PlayerHistoricalStats) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF03251E)),
        border = BorderStroke(1.5.dp, StadiumGold)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Card Header: AYUUCRIC PRO PASS + ID
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "AYUUCRIC PRO PASS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = StadiumGold,
                    letterSpacing = 0.5.sp
                )

                val displayId = "ID: " + if (stats.playerId.length >= 6) stats.playerId.takeLast(6).uppercase() else "NTOOF2"
                Text(
                    text = displayId,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = HawkEyeCyan
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Middle: Avatar + Name + Jersey + Tags
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar circle with gold border
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF071F1A))
                        .border(2.dp, StadiumGold, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = stats.avatarEmoji.ifBlank { "🦁" }, fontSize = 26.sp)
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stats.fullName.ifBlank { "Ayush Kumar" },
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "#${if (stats.jerseyNumber > 0) stats.jerseyNumber else 7}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = CricketGreen
                        )
                    }

                    Text(
                        text = "@${stats.username.removePrefix("@").ifBlank { "ayush_7" }} • ${stats.jerseyName.ifBlank { "Ayush" }}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = StadiumGold
                    )

                    Text(
                        text = "${stats.teamName.ifBlank { "Local XI" }} • ${stats.city.ifBlank { "India" }}",
                        fontSize = 10.5.sp,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Bottom 3 rounded pill tags
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TagPill(text = stats.battingStyle.ifBlank { "Right-hand Bat" }, textColor = HawkEyeCyan)
                TagPill(text = stats.bowlingStyle.ifBlank { "Right-arm Fast" }, textColor = CricketGreen)
                TagPill(text = if (stats.isVerified) "100% Verified" else "Pro Player", textColor = StadiumGold)
            }
        }
    }
}

@Composable
private fun TagPill(text: String, textColor: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF0C2B26))
            .border(0.8.dp, Color(0xFF1A463E), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

@Composable
fun KeyCareerHighlightsRow(stats: PlayerHistoricalStats) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CareerMetricBox(
            title = "Runs",
            value = "${stats.batting.runs}",
            sub = "SR: ${stats.batting.strikeRate}",
            color = StadiumGold,
            modifier = Modifier.weight(1f)
        )
        CareerMetricBox(
            title = "Wickets",
            value = "${stats.bowling.wickets}",
            sub = "Econ: ${stats.bowling.economyRate}",
            color = CricketGreen,
            modifier = Modifier.weight(1f)
        )
        CareerMetricBox(
            title = "Matches",
            value = "${stats.batting.matches}",
            sub = "HS: ${stats.batting.highestScore}",
            color = HawkEyeCyan,
            modifier = Modifier.weight(1f)
        )
        CareerMetricBox(
            title = "Avg",
            value = "${stats.batting.average}",
            sub = "BBI: ${stats.bowling.bestBowling}",
            color = Color(0xFFA78BFA),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun CareerMetricBox(
    title: String,
    value: String,
    sub: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0F172A))
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            .padding(vertical = 10.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = title, fontSize = 9.5.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
            Text(text = value, fontSize = 16.sp, fontWeight = FontWeight.Black, color = color)
            Text(text = sub, fontSize = 9.sp, color = Color.White.copy(alpha = 0.7f), fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun MilestonesSection(milestones: List<PlayerMilestone>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF0F172A))
            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("🏅", fontSize = 14.sp)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "CAREER MILESTONES & HONORS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = StadiumGold,
                letterSpacing = 0.8.sp
            )
        }

        milestones.forEach { milestone ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1E293B).copy(alpha = 0.5f))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = milestone.icon, fontSize = 18.sp)
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = milestone.title,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    if (milestone.subtitle.isNotBlank()) {
                        Text(
                            text = milestone.subtitle,
                            fontSize = 10.sp,
                            color = TextSecondary
                        )
                    }
                }
                if (milestone.date.isNotBlank()) {
                    Text(
                        text = milestone.date,
                        fontSize = 9.5.sp,
                        color = StadiumGold,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun BattingStatsGrid(batting: BattingHistoricalStats) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatMetricTile("Innings", "${batting.innings}", "Matches: ${batting.matches}", StadiumGold, Modifier.weight(1f))
            StatMetricTile("Runs", "${batting.runs}", "Balls: ${batting.ballsFaced}", CricketGreen, Modifier.weight(1f))
            StatMetricTile("Highest Score", batting.highestScore, "Not Outs: ${batting.notOuts}", HawkEyeCyan, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatMetricTile("Batting Avg", "${batting.average}", "Dismissals: ${batting.innings - batting.notOuts}", Color(0xFFFACC15), Modifier.weight(1f))
            StatMetricTile("Strike Rate", "${batting.strikeRate}", "High-Impact", Color(0xFF38BDF8), Modifier.weight(1f))
            StatMetricTile("Fifties / 100s", "${batting.fifties} / ${batting.hundreds}", "Milestones", Color(0xFFA78BFA), Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatMetricTile("Fours (4s)", "${batting.fours}", "${batting.fours * 4} runs", Color(0xFF60A5FA), Modifier.weight(1f))
            StatMetricTile("Sixes (6s)", "${batting.sixes}", "${batting.sixes * 6} runs", Color(0xFFF43F5E), Modifier.weight(1f))
            StatMetricTile("Boundary %", String.format("%.1f%%", batting.boundaryPercentage), "${batting.runsInBoundaries} runs", StadiumGold, Modifier.weight(1f))
        }
    }
}

@Composable
fun BoundaryDistributionCard(batting: BattingHistoricalStats) {
    val totalRuns = batting.runs.coerceAtLeast(1)
    val runsFromFours = batting.fours * 4
    val runsFromSixes = batting.sixes * 6
    val runsFromRunning = (totalRuns - runsFromFours - runsFromSixes).coerceAtLeast(0)

    val foursRatio = (runsFromFours.toFloat() / totalRuns).coerceIn(0f, 1f)
    val sixesRatio = (runsFromSixes.toFloat() / totalRuns).coerceIn(0f, 1f)
    val runningRatio = (runsFromRunning.toFloat() / totalRuns).coerceIn(0f, 1f)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        border = BorderStroke(1.dp, Color(0xFF1E293B))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("RUN CONTRIBUTION SPECTRUM", fontSize = 10.sp, fontWeight = FontWeight.Black, color = TextSecondary)
            Spacer(modifier = Modifier.height(8.dp))

            // Multi-color progress bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(Color(0xFF1E293B))
            ) {
                if (sixesRatio > 0) {
                    Box(modifier = Modifier.weight(sixesRatio.coerceAtLeast(0.01f)).fillMaxHeight().background(Color(0xFFF43F5E)))
                }
                if (foursRatio > 0) {
                    Box(modifier = Modifier.weight(foursRatio.coerceAtLeast(0.01f)).fillMaxHeight().background(Color(0xFF38BDF8)))
                }
                if (runningRatio > 0) {
                    Box(modifier = Modifier.weight(runningRatio.coerceAtLeast(0.01f)).fillMaxHeight().background(CricketGreen))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                LegendItem("6s: $runsFromSixes runs", Color(0xFFF43F5E))
                LegendItem("4s: $runsFromFours runs", Color(0xFF38BDF8))
                LegendItem("1s/2s: $runsFromRunning runs", CricketGreen)
            }
        }
    }
}

@Composable
private fun LegendItem(text: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(color))
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = text, fontSize = 9.5.sp, color = TextSecondary, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun BattingInningCard(inning: BattingInningsRecord) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        border = BorderStroke(0.8.dp, Color(0xFF1E293B))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = inning.matchTitle,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "vs ${inning.opponent} • ${inning.venue} • ${inning.date}",
                    fontSize = 9.5.sp,
                    color = TextSecondary
                )
                Text(
                    text = inning.dismissal,
                    fontSize = 9.sp,
                    color = if (inning.isNotOut) CricketGreen else Color(0xFFEF4444),
                    fontWeight = FontWeight.SemiBold
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${inning.runs}${if (inning.isNotOut) "*" else ""}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = StadiumGold
                )
                Text(
                    text = "${inning.balls}b (${inning.fours}x4, ${inning.sixes}x6)",
                    fontSize = 9.5.sp,
                    color = Color.White
                )
                Text(
                    text = "SR: ${String.format("%.1f", inning.strikeRate)}",
                    fontSize = 9.sp,
                    color = HawkEyeCyan,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun BowlingStatsGrid(bowling: BowlingHistoricalStats) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatMetricTile("Wickets", "${bowling.wickets}", "Matches: ${bowling.matches}", CricketGreen, Modifier.weight(1f))
            StatMetricTile("Best Bowling", bowling.bestBowling, "Record Spell", StadiumGold, Modifier.weight(1f))
            StatMetricTile("Economy", "${bowling.economyRate}", "Runs/Over", HawkEyeCyan, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatMetricTile("Overs Bowled", "${bowling.overs}", "Maidens: ${bowling.maidens}", Color(0xFF60A5FA), Modifier.weight(1f))
            StatMetricTile("Bowling Avg", "${bowling.average}", "Runs/Wkt", Color(0xFFFACC15), Modifier.weight(1f))
            StatMetricTile("Strike Rate", "${bowling.strikeRate}", "Balls/Wkt", Color(0xFFA78BFA), Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatMetricTile("Runs Conceded", "${bowling.runsConceded}", "Total runs", Color(0xFFF43F5E), Modifier.weight(1f))
            StatMetricTile("3w / 5w Hauls", "${bowling.threeWickets} / ${bowling.fiveWickets}", "Clutch spells", StadiumGold, Modifier.weight(1f))
            StatMetricTile("Dot Balls", "${bowling.dotBalls}", String.format("%.1f%% Dots", bowling.dotBallPercentage), CricketGreen, Modifier.weight(1f))
        }
    }
}

@Composable
fun BowlingControlMetricCard(bowling: BowlingHistoricalStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        border = BorderStroke(1.dp, Color(0xFF1E293B))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("ECONOMY STATUS", fontSize = 9.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                Text(
                    text = if (bowling.economyRate <= 7.0) "ELITE CONTROL" else "ATTACKING",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = if (bowling.economyRate <= 7.0) CricketGreen else StadiumGold
                )
            }
            Box(modifier = Modifier.width(1.dp).height(28.dp).background(Color(0xFF334155)))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("DOT BALL PRESSURE", fontSize = 9.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                Text(
                    text = String.format("%.1f%%", bowling.dotBallPercentage),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = HawkEyeCyan
                )
            }
            Box(modifier = Modifier.width(1.dp).height(28.dp).background(Color(0xFF334155)))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("BALLS / WICKET", fontSize = 9.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                Text(
                    text = "${bowling.strikeRate} balls",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFA78BFA)
                )
            }
        }
    }
}

@Composable
fun BowlingSpellCard(spell: BowlingSpellRecord) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        border = BorderStroke(0.8.dp, Color(0xFF1E293B))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = spell.matchTitle,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "vs ${spell.opponent} • ${spell.venue} • ${spell.date}",
                    fontSize = 9.5.sp,
                    color = TextSecondary
                )
                Text(
                    text = "${spell.dotBalls} dot balls bowled",
                    fontSize = 9.sp,
                    color = HawkEyeCyan,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${spell.wickets}/${spell.runs}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = CricketGreen
                )
                Text(
                    text = "${spell.overs} ov (${spell.maidens} M)",
                    fontSize = 9.5.sp,
                    color = Color.White
                )
                Text(
                    text = "Econ: ${String.format("%.2f", spell.economy)}",
                    fontSize = 9.sp,
                    color = StadiumGold,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun CombinedMatchLogCard(inning: BattingInningsRecord?, spell: BowlingSpellRecord?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        border = BorderStroke(1.dp, Color(0xFF1E293B))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = inning?.matchTitle ?: spell?.matchTitle ?: "Match",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                Text(
                    text = inning?.date ?: spell?.date ?: "",
                    fontSize = 9.5.sp,
                    color = StadiumGold
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "vs ${inning?.opponent ?: spell?.opponent ?: "Opponent"} • ${inning?.venue ?: spell?.venue ?: "Stadium"}",
                fontSize = 10.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1E293B).copy(alpha = 0.5f))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🏏 BATTING", fontSize = 8.5.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                    Text(
                        text = if (inning != null) "${inning.runs}${if (inning.isNotOut) "*" else ""} (${inning.balls}b)" else "DNB",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = StadiumGold
                    )
                }

                Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color(0xFF334155)))

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🎯 BOWLING", fontSize = 8.5.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                    Text(
                        text = if (spell != null) "${spell.wickets}/${spell.runs} (${spell.overs} ov)" else "DNB",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = CricketGreen
                    )
                }
            }
        }
    }
}

@Composable
private fun StatMetricTile(
    title: String,
    value: String,
    sub: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF0F172A))
            .border(0.8.dp, color.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
            .padding(vertical = 8.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = title, fontSize = 9.sp, color = TextSecondary, fontWeight = FontWeight.SemiBold)
            Text(text = value, fontSize = 14.5.sp, fontWeight = FontWeight.Black, color = color)
            Text(text = sub, fontSize = 8.sp, color = Color.White.copy(alpha = 0.6f), maxLines = 1)
        }
    }
}
