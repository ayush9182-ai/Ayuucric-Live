package com.example.ui.components

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
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.data.model.NotificationAlertEntity
import com.example.data.model.PlayerStatEntity
import com.example.data.model.TeamStandingEntity
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

@Composable
fun StandingsAndLeaderboardScreen(
    standings: List<TeamStandingEntity>,
    playerStats: List<PlayerStatEntity>,
    notifications: List<NotificationAlertEntity>,
    notifyWickets: Boolean,
    notifyBoundaries: Boolean,
    notifyMilestones: Boolean,
    notifyDrs: Boolean,
    onToggleNotification: (String) -> Unit,
    onSendTestAlert: () -> Unit,
    onClearAllRecords: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Points Table, 1: Fan Leaderboard, 2: Alerts
    var showClearDialog by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    if (showClearDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showClearDialog = false },
            containerColor = PitchDark,
            shape = RoundedCornerShape(16.dp),
            title = {
                Text("Clear All Records & Tables?", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                Text(
                    "Kya aap table aur stats ke saare pre-existing records hatana chahte hain taaki pure fresh match records hi dikhein?",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onClearAllRecords()
                        showClearDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DrsOutRed),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Haan, Saaf Karein", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
            .testTag("standings_and_leaderboard_screen")
    ) {
        // Quick Action Row for Table & Stats
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "TOURNAMENT ARENA",
                color = StadiumGold,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp
            )

            Button(
                onClick = { showClearDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                shape = RoundedCornerShape(8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Clear Records 🗑️",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Navigation Tabs
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = PitchSurface,
            contentColor = CricketGreen,
            indicator = { tabPositions ->
                SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = CricketGreen,
                    height = 3.dp
                )
            }
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Points Table", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Leaderboard", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("Notifications", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (selectedTab) {
            0 -> PointsTableTab(standings = standings)
            1 -> FanLeaderboardTab(playerStats = playerStats)
            2 -> NotificationsManagerTab(
                notifications = notifications,
                notifyWickets = notifyWickets,
                notifyBoundaries = notifyBoundaries,
                notifyMilestones = notifyMilestones,
                notifyDrs = notifyDrs,
                onToggleNotification = onToggleNotification,
                onSendTestAlert = onSendTestAlert
            )
        }
    }
}

@Composable
fun PointsTableTab(standings: List<TeamStandingEntity>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PitchCard),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(PitchCardBorder),
            width = 1.dp
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "GULLY PREMIER LEAGUE 2026",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = StadiumGold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Top 4 Qualify",
                    fontSize = 10.sp,
                    color = CricketGreen,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (standings.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Points Table Cleared",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Purane records saaf hain. Naye matches khelein aur point table yahan live banta jayega!",
                            color = TextMuted,
                            fontSize = 11.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                // Table Header Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0F172A))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "#", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted, modifier = Modifier.width(20.dp))
                    Text(text = "TEAM", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted, modifier = Modifier.weight(1f))
                    Text(text = "P", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted, modifier = Modifier.width(24.dp))
                    Text(text = "W", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted, modifier = Modifier.width(24.dp))
                    Text(text = "L", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted, modifier = Modifier.width(24.dp))
                    Text(text = "NRR", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted, modifier = Modifier.width(44.dp))
                    Text(text = "PTS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CricketGreen, modifier = Modifier.width(30.dp))
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Table Rows
                standings.forEachIndexed { index, team ->
                val isTop4 = index < 4
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${index + 1}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isTop4) CricketGreen else TextMuted,
                        modifier = Modifier.width(20.dp)
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = team.name,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            team.formGuide.split(",").forEach { f ->
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .background(if (f == "W") CricketGreen else DrsOutRed),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = f, fontSize = 8.sp, fontWeight = FontWeight.Black, color = PitchDark)
                                }
                            }
                        }
                    }

                    Text(text = "${team.matchesPlayed}", fontSize = 12.sp, color = TextSecondary, modifier = Modifier.width(24.dp))
                    Text(text = "${team.won}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.width(24.dp))
                    Text(text = "${team.lost}", fontSize = 12.sp, color = TextSecondary, modifier = Modifier.width(24.dp))

                    val nrrSign = if (team.netRunRate >= 0) "+" else ""
                    Text(
                        text = "$nrrSign${"%.2f".format(team.netRunRate)}",
                        fontSize = 11.sp,
                        color = if (team.netRunRate >= 0) CricketGreen else DrsOutRed,
                        modifier = Modifier.width(44.dp)
                    )

                    Text(
                        text = "${team.points}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = StadiumGold,
                        modifier = Modifier.width(30.dp)
                    )
                }

                if (index == 3) {
                    // Qualification cut-off line
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            thickness = 1.dp,
                            color = CricketGreen.copy(alpha = 0.5f)
                        )
                        Text(
                            text = "  PLAYOFF LINE  ",
                            fontSize = 9.sp,
                            color = CricketGreen,
                            fontWeight = FontWeight.Bold
                        )
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            thickness = 1.dp,
                            color = CricketGreen.copy(alpha = 0.5f)
                        )
                    }
                } else if (index < standings.size - 1) {
                    HorizontalDivider(color = Color(0xFF1E293B), thickness = 0.5.dp)
                }
            }
        }
    }
}
}

@Composable
fun FanLeaderboardTab(playerStats: List<PlayerStatEntity>) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (playerStats.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = PitchCard)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Leaderboard Records Cleared",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Saare demo player records hata diye gaye hain. Real live match khelte waqt players ke runs, wickets aur fantasy points yahan auto-calculate honge!",
                            color = TextMuted,
                            fontSize = 11.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }
        } else {
            val p1 = playerStats.getOrNull(0)
            val p2 = playerStats.getOrNull(1)
            val p3 = playerStats.getOrNull(2)

            // Top 3 Podium
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = PitchCard),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.linearGradient(listOf(StadiumGold, HawkEyeCyan)),
                    width = 1.dp
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "COMMUNITY MVP & FANTASY LEADERBOARD",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = StadiumGold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        // #2
                        if (p2 != null) {
                            PodiumColumn(rank = 2, name = p2.name, points = p2.fantasyPoints, height = 75.dp, badgeColor = Color(0xFF94A3B8))
                        }
                        // #1
                        if (p1 != null) {
                            PodiumColumn(rank = 1, name = p1.name, points = p1.fantasyPoints, height = 95.dp, badgeColor = StadiumGold)
                        }
                        // #3
                        if (p3 != null) {
                            PodiumColumn(rank = 3, name = p3.name, points = p3.fantasyPoints, height = 65.dp, badgeColor = Color(0xFFB45309))
                        }
                    }
                }
            }

            // Leaderboard List
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = PitchCard)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    playerStats.forEachIndexed { i, player ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "#${i + 1}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (i < 3) StadiumGold else TextMuted,
                            modifier = Modifier.width(28.dp)
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = player.name,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "${player.team} • ${player.role}",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${player.fantasyPoints} pts",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = HawkEyeCyan
                            )
                            Text(
                                text = "${player.runs}R • ${player.wickets}W",
                                fontSize = 10.sp,
                                color = TextMuted
                            )
                        }
                    }
                    if (i < playerStats.size - 1) {
                        HorizontalDivider(color = Color(0xFF1E293B), thickness = 0.5.dp)
                    }
                }
            }
        }
    }
}
}

@Composable
fun PodiumColumn(rank: Int, name: String, points: Int, height: androidx.compose.ui.unit.Dp, badgeColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(badgeColor.copy(alpha = 0.2f))
                .border(1.2.dp, badgeColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.MilitaryTech,
                contentDescription = "Rank $rank",
                tint = badgeColor,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = name, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Text(text = "$points pts", fontSize = 10.sp, fontWeight = FontWeight.Black, color = HawkEyeCyan)

        Spacer(modifier = Modifier.height(6.dp))

        Box(
            modifier = Modifier
                .width(68.dp)
                .height(height)
                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                .background(badgeColor.copy(alpha = 0.3f))
                .border(1.dp, badgeColor.copy(alpha = 0.6f), RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "#$rank",
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = badgeColor
            )
        }
    }
}

@Composable
fun NotificationsManagerTab(
    notifications: List<NotificationAlertEntity>,
    notifyWickets: Boolean,
    notifyBoundaries: Boolean,
    notifyMilestones: Boolean,
    notifyDrs: Boolean,
    onToggleNotification: (String) -> Unit,
    onSendTestAlert: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Notification Preferences Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = PitchCard),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = androidx.compose.ui.graphics.SolidColor(PitchCardBorder),
                width = 1.dp
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = "Alerts",
                            tint = CricketGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "LIVE MATCH ALERTS",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            letterSpacing = 1.sp
                        )
                    }

                    Text(text = "Home Live Mode", fontSize = 10.sp, color = HawkEyeCyan)
                }

                Spacer(modifier = Modifier.height(12.dp))

                NotificationToggleRow(
                    title = "Wicket Alerts",
                    description = "Instant alert when a wicket falls or bails fly",
                    checked = notifyWickets,
                    onCheckedChange = { onToggleNotification("WICKETS") }
                )

                HorizontalDivider(color = Color(0xFF1E293B), thickness = 0.5.dp, modifier = Modifier.padding(vertical = 8.dp))

                NotificationToggleRow(
                    title = "Boundaries (4s & 6s)",
                    description = "Notify on big sixes and crucial boundaries",
                    checked = notifyBoundaries,
                    onCheckedChange = { onToggleNotification("BOUNDARIES") }
                )

                HorizontalDivider(color = Color(0xFF1E293B), thickness = 0.5.dp, modifier = Modifier.padding(vertical = 8.dp))

                NotificationToggleRow(
                    title = "Milestones (50s / 100s)",
                    description = "Alert when batsmen achieve half-century or century",
                    checked = notifyMilestones,
                    onCheckedChange = { onToggleNotification("MILESTONES") }
                )

                HorizontalDivider(color = Color(0xFF1E293B), thickness = 0.5.dp, modifier = Modifier.padding(vertical = 8.dp))

                NotificationToggleRow(
                    title = "DRS Third Umpire Reviews",
                    description = "Live updates on Hawk-Eye verdicts and appeals",
                    checked = notifyDrs,
                    onCheckedChange = { onToggleNotification("DRS") }
                )

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onSendTestAlert,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("send_test_alert_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = HawkEyeCyan),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Campaign, contentDescription = "Broadcast", tint = PitchDark, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Broadcast Test Live Match Alert", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PitchDark)
                }
            }
        }

        // Notification History
        Text(
            text = "RECENT MATCH ALERTS",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = TextMuted,
            letterSpacing = 1.sp
        )

        notifications.forEach { alert ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = PitchCard)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                when (alert.type) {
                                    "WICKET" -> DrsOutRed.copy(alpha = 0.2f)
                                    "SIX" -> StadiumGold.copy(alpha = 0.2f)
                                    "DRS" -> HawkEyeCyan.copy(alpha = 0.2f)
                                    else -> CricketGreen.copy(alpha = 0.2f)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = alert.title,
                            tint = when (alert.type) {
                                "WICKET" -> DrsOutRed
                                "SIX" -> StadiumGold
                                "DRS" -> HawkEyeCyan
                                else -> CricketGreen
                            },
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = alert.title,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = alert.message,
                            fontSize = 11.sp,
                            color = TextSecondary,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text(text = description, fontSize = 10.sp, color = TextSecondary)
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = CricketGreen,
                checkedTrackColor = CricketGreen.copy(alpha = 0.3f),
                uncheckedThumbColor = TextMuted,
                uncheckedTrackColor = Color(0xFF1E293B)
            )
        )
    }
}
