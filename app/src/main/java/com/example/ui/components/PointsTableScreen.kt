package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingUp
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
import com.example.data.model.TeamStandingEntity
import com.example.ui.theme.*

/**
 * Real-time Tournament Points Table Screen synced with Cloud Firestore.
 * Displays matches played (P), wins (W), losses (L), tied (T), net run rate (NRR), and points (PTS).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PointsTableScreen(
    standings: List<TeamStandingEntity>,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {},
    tournamentTitle: String = "Gully Premier League 2026",
    onTeamClick: ((TeamStandingEntity) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableIntStateOf(0) } // 0: All, 1: Top 4, 2: Positive NRR
    var selectedTeamForDetails by remember { mutableStateOf<TeamStandingEntity?>(null) }

    val filteredStandings = remember(standings, searchQuery, selectedFilter) {
        var list = standings
        if (searchQuery.isNotBlank()) {
            val q = searchQuery.trim().lowercase()
            list = list.filter {
                it.name.lowercase().contains(q) || it.shortName.lowercase().contains(q)
            }
        }
        when (selectedFilter) {
            1 -> list.take(4)
            2 -> list.filter { it.netRunRate >= 0.0 }
            else -> list
        }
    }

    // Top Leader & Stats
    val leaderTeam = standings.firstOrNull()
    val totalMatchesPlayed = standings.sumOf { it.matchesPlayed } / 2
    val bestNrrTeam = standings.maxByOrNull { it.netRunRate }

    // Pulsing animation for Live Firestore Sync indicator
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    val rotateTransition = rememberInfiniteTransition(label = "spin")
    val spinAngle by rotateTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spin"
    )

    // Team Details Modal Dialog
    if (selectedTeamForDetails != null) {
        val team = selectedTeamForDetails!!
        TeamDetailsModal(
            team = team,
            onDismiss = { selectedTeamForDetails = null }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PitchDark)
            .padding(horizontal = 14.dp)
            .testTag("points_table_screen")
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        // 1. Top Header with Firestore Sync Badge and Refresh Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = "Trophy",
                        tint = StadiumGold,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "POINTS TABLE",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary,
                        letterSpacing = 1.sp
                    )
                }
                Text(
                    text = tournamentTitle,
                    fontSize = 11.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium
                )
            }

            // Real-time Firestore Sync Badge & Refresh Button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF0F2E2A))
                        .border(1.dp, CricketGreen.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(CricketGreen.copy(alpha = pulseAlpha))
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "LIVE CLOUD",
                            color = CricketGreen,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E293B))
                        .testTag("refresh_standings_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh from Firestore",
                        tint = StadiumGold,
                        modifier = Modifier
                            .size(18.dp)
                            .rotate(if (isRefreshing) spinAngle else 0f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 2. High-Level Summary Stat Cards (Leader, Matches, Best NRR)
        if (standings.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Table Leader Card
                StatPillCard(
                    title = "TABLE LEADER",
                    mainValue = leaderTeam?.name ?: "–",
                    subValue = "${leaderTeam?.points ?: 0} Pts • NRR ${formatNrr(leaderTeam?.netRunRate ?: 0.0)}",
                    accentColor = StadiumGold,
                    modifier = Modifier.weight(1.3f)
                )

                // Matches Played Card
                StatPillCard(
                    title = "MATCHES",
                    mainValue = "$totalMatchesPlayed Played",
                    subValue = "${standings.size} Teams Competing",
                    accentColor = HawkEyeCyan,
                    modifier = Modifier.weight(1f)
                )

                // Best NRR Card
                StatPillCard(
                    title = "BEST NRR",
                    mainValue = formatNrr(bestNrrTeam?.netRunRate ?: 0.0),
                    subValue = bestNrrTeam?.shortName ?: "–",
                    accentColor = CricketGreen,
                    modifier = Modifier.weight(0.9f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
        }

        // 3. Search and Filter Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search team...", fontSize = 11.sp, color = TextMuted) },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp))
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(20.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextSecondary, modifier = Modifier.size(14.dp))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CricketGreen,
                    unfocusedBorderColor = Color(0xFF334155),
                    focusedContainerColor = PitchSurface,
                    unfocusedContainerColor = PitchSurface,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .testTag("search_standings_input")
            )

            // Filter Chips
            LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                item {
                    FilterTabChip(
                        label = "All",
                        isSelected = selectedFilter == 0,
                        onClick = { selectedFilter = 0 }
                    )
                }
                item {
                    FilterTabChip(
                        label = "Top 4",
                        isSelected = selectedFilter == 1,
                        onClick = { selectedFilter = 1 }
                    )
                }
                item {
                    FilterTabChip(
                        label = "NRR (+)",
                        isSelected = selectedFilter == 2,
                        onClick = { selectedFilter = 2 }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 4. Main Points Table Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = PitchCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, PitchCardBorder)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Table Header Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0F172A))
                        .padding(horizontal = 10.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "POS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted, modifier = Modifier.width(30.dp))
                    Text(text = "TEAM", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted, modifier = Modifier.weight(1f))
                    Text(text = "P", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted, textAlign = TextAlign.Center, modifier = Modifier.width(26.dp))
                    Text(text = "W", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted, textAlign = TextAlign.Center, modifier = Modifier.width(24.dp))
                    Text(text = "L", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted, textAlign = TextAlign.Center, modifier = Modifier.width(24.dp))
                    Text(text = "NRR", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted, textAlign = TextAlign.End, modifier = Modifier.width(48.dp))
                    Text(text = "PTS", fontSize = 10.5.sp, fontWeight = FontWeight.Black, color = StadiumGold, textAlign = TextAlign.End, modifier = Modifier.width(36.dp))
                }

                Divider(color = Color(0xFF1E293B), thickness = 1.dp)

                if (filteredStandings.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = if (searchQuery.isNotEmpty()) "No teams matching \"$searchQuery\"" else "No standings recorded in Firestore yet",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "Matches complete hone par real-time points table Firestore se sync hokar yahan dikhega.",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Button(
                                onClick = onRefresh,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, StadiumGold.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = StadiumGold, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Sync from Cloud 🔄", color = StadiumGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        itemsIndexed(filteredStandings, key = { _, item -> item.teamId }) { index, team ->
                            val isTop4 = index < 4
                            val nrrIsPositive = team.netRunRate >= 0.0

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onTeamClick?.invoke(team)
                                        selectedTeamForDetails = team
                                    }
                                    .background(if (index % 2 == 1) Color(0xFF131D31) else Color.Transparent)
                                    .padding(horizontal = 10.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Position with Top 4 Qualifier Indicator
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.width(30.dp)
                                ) {
                                    if (isTop4) {
                                        Box(
                                            modifier = Modifier
                                                .width(3.dp)
                                                .height(16.dp)
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(CricketGreen)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    Text(
                                        text = "${index + 1}",
                                        fontSize = 12.sp,
                                        fontWeight = if (isTop4) FontWeight.Black else FontWeight.SemiBold,
                                        color = if (isTop4) CricketGreen else TextMuted
                                    )
                                }

                                // Team Name + Form Guide
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = team.name,
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "(${team.shortName})",
                                            fontSize = 10.sp,
                                            color = TextMuted,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }

                                    // Form Guide Pills (W / L / T)
                                    if (team.formGuide.isNotBlank() && team.formGuide != "–") {
                                        Row(
                                            modifier = Modifier.padding(top = 2.dp),
                                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                                        ) {
                                            team.formGuide.split(",").takeLast(5).forEach { result ->
                                                val res = result.trim().uppercase()
                                                val badgeColor = when (res) {
                                                    "W" -> CricketGreen
                                                    "L" -> DrsOutRed
                                                    else -> HawkEyeCyan
                                                }
                                                Box(
                                                    modifier = Modifier
                                                        .size(13.dp)
                                                        .clip(CircleShape)
                                                        .background(badgeColor.copy(alpha = 0.2f))
                                                        .border(0.5.dp, badgeColor, CircleShape),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = res,
                                                        fontSize = 7.5.sp,
                                                        fontWeight = FontWeight.Black,
                                                        color = badgeColor
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // Matches Played (P)
                                Text(
                                    text = "${team.matchesPlayed}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextSecondary,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.width(26.dp)
                                )

                                // Won (W)
                                Text(
                                    text = "${team.won}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CricketGreen,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.width(24.dp)
                                )

                                // Lost (L)
                                Text(
                                    text = "${team.lost}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = DrsOutRed.copy(alpha = 0.85f),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.width(24.dp)
                                )

                                // Net Run Rate (NRR) Column
                                Text(
                                    text = formatNrr(team.netRunRate),
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (nrrIsPositive) CricketGreen else Color(0xFFF87171),
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.width(48.dp)
                                )

                                // Points (PTS)
                                Box(
                                    modifier = Modifier
                                        .width(36.dp),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(StadiumGold.copy(alpha = 0.18f))
                                            .border(1.dp, StadiumGold.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "${team.points}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Black,
                                            color = StadiumGold
                                        )
                                    }
                                }
                            }
                            Divider(color = Color(0xFF1E293B), thickness = 0.5.dp)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 5. Qualification Legend & Sync Status
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(CricketGreen)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Top 4 Qualify for Semi-Finals",
                    fontSize = 10.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium
                )
            }

            Text(
                text = "Tap any team for stats • Live Firestore",
                fontSize = 10.sp,
                color = TextMuted
            )
        }

        Spacer(modifier = Modifier.height(10.dp))
    }
}

/**
 * Formats Net Run Rate cleanly with + or - sign, e.g. +1.450 or -0.820
 */
fun formatNrr(nrr: Double): String {
    val sign = if (nrr > 0.0) "+" else if (nrr < 0.0) "" else ""
    return "$sign${"%.3f".format(nrr)}"
}

@Composable
private fun StatPillCard(
    title: String,
    mainValue: String,
    subValue: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = PitchSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Bold,
                color = TextMuted,
                letterSpacing = 0.5.sp
            )
            Text(
                text = mainValue,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                color = accentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subValue,
                fontSize = 9.sp,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun FilterTabChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) CricketGreen else Color(0xFF1E293B))
            .border(1.dp, if (isSelected) CricketGreen else Color(0xFF334155), RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
            color = if (isSelected) PitchDark else TextSecondary
        )
    }
}

/**
 * Team Tournament Breakdown Modal
 */
@Composable
private fun TeamDetailsModal(
    team: TeamStandingEntity,
    onDismiss: () -> Unit
) {
    val winPct = if (team.matchesPlayed > 0) ((team.won.toDouble() / team.matchesPlayed) * 100).toInt() else 0

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = PitchSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, StadiumGold.copy(alpha = 0.6f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
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
                            Text(
                                text = team.shortName.take(2).uppercase(),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                color = StadiumGold
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = team.name,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Code: ${team.shortName} • ${team.points} Points",
                                fontSize = 11.sp,
                                color = StadiumGold
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Divider(color = Color(0xFF334155))

                // Stats Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    DetailStatItem(label = "Matches", value = "${team.matchesPlayed}")
                    DetailStatItem(label = "Wins", value = "${team.won}", valueColor = CricketGreen)
                    DetailStatItem(label = "Losses", value = "${team.lost}", valueColor = DrsOutRed)
                    DetailStatItem(label = "Tied", value = "${team.tied}")
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    DetailStatItem(label = "Points", value = "${team.points}", valueColor = StadiumGold)
                    DetailStatItem(label = "Net Run Rate", value = formatNrr(team.netRunRate), valueColor = if (team.netRunRate >= 0) CricketGreen else Color(0xFFF87171))
                    DetailStatItem(label = "Win Rate", value = "$winPct%")
                    DetailStatItem(label = "Standing ID", value = team.teamId.takeLast(4).uppercase())
                }

                // Form Guide
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "RECENT FORM (LAST 5 MATCHES):",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val forms = if (team.formGuide.isNotBlank() && team.formGuide != "–") {
                            team.formGuide.split(",").takeLast(5)
                        } else {
                            listOf("–")
                        }
                        forms.forEach { f ->
                            val res = f.trim().uppercase()
                            val color = when (res) {
                                "W" -> CricketGreen
                                "L" -> DrsOutRed
                                else -> HawkEyeCyan
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(color.copy(alpha = 0.2f))
                                    .border(1.dp, color, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (res == "W") "WIN" else if (res == "L") "LOSS" else res,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = color
                                )
                            }
                        }
                    }
                }

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close", color = TextPrimary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun DetailStatItem(
    label: String,
    value: String,
    valueColor: Color = TextPrimary
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, fontSize = 10.sp, color = TextMuted)
        Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Black, color = valueColor)
    }
}
