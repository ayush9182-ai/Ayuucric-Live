package com.example.ui.components

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
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.DonutLarge
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BallEventEntity
import com.example.data.model.PlayerStatEntity
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
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun TournamentAnalyticsScreen(
    ballEvents: List<BallEventEntity>,
    playerStats: List<PlayerStatEntity>,
    modifier: Modifier = Modifier
) {
    var selectedChartTab by remember { mutableIntStateOf(0) } // 0: Wagon Wheel, 1: Manhattan & Worm, 2: Leaders

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
            .testTag("tournament_analytics_screen")
    ) {
        // Analytics Header
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = PitchCard),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = Brush.linearGradient(listOf(Color(0xFF8B5CF6), HawkEyeCyan)),
                width = 1.2.dp
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF8B5CF6).copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Analytics,
                        contentDescription = "Analytics",
                        tint = Color(0xFFA78BFA),
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "TOURNAMENT ANALYTICS",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary
                    )
                    Text(
                        text = "Wagon Wheel • Manhattan • Worm Curves • Cap Leaders",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Chart Navigation Tabs
        TabRow(
            selectedTabIndex = selectedChartTab,
            containerColor = PitchSurface,
            contentColor = Color(0xFFA78BFA),
            indicator = { tabPositions ->
                SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedChartTab]),
                    color = Color(0xFFA78BFA),
                    height = 3.dp
                )
            }
        ) {
            Tab(
                selected = selectedChartTab == 0,
                onClick = { selectedChartTab = 0 },
                text = { Text("Wagon Wheel", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
            )
            Tab(
                selected = selectedChartTab == 1,
                onClick = { selectedChartTab = 1 },
                text = { Text("Manhattan / Worm", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
            )
            Tab(
                selected = selectedChartTab == 2,
                onClick = { selectedChartTab = 2 },
                text = { Text("Cap Leaders", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (selectedChartTab) {
            0 -> WagonWheelSection(ballEvents = ballEvents)
            1 -> ManhattanAndWormSection()
            2 -> TournamentLeadersSection(playerStats = playerStats)
        }
    }
}

@Composable
fun WagonWheelSection(ballEvents: List<BallEventEntity>) {
    val totalRuns = ballEvents.sumOf { it.runs }
    val offRuns = ballEvents.filter { it.shotAngle in 0f..180f }.sumOf { it.runs }
    val legRuns = ballEvents.filter { it.shotAngle > 180f }.sumOf { it.runs }
    val offPct = if (totalRuns > 0) (offRuns * 100) / totalRuns else 0
    val legPct = if (totalRuns > 0) (legRuns * 100) / totalRuns else 0

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
                Text(
                    text = "360° Ground Wagon Wheel",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Text(
                    text = if (ballEvents.isNotEmpty()) "$totalRuns runs (${ballEvents.size} balls)" else "No records yet",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (ballEvents.isNotEmpty()) CricketGreen else TextMuted
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Wagon Wheel Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF071B12))
                    .border(1.dp, Color(0xFF0F3825), RoundedCornerShape(12.dp))
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawWagonWheel(ballEvents)
                }

                if (ballEvents.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "No records yet",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Abhi tak koi shot nahi khela gaya hai",
                                color = Color.White.copy(alpha = 0.65f),
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Run Color Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                LegendItem(color = Color.White, label = "1s & 2s")
                LegendItem(color = CricketGreen, label = "4s (Boundaries)")
                LegendItem(color = StadiumGold, label = "6s (Maximums)")
                LegendItem(color = DrsOutRed, label = "Wickets")
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Off Side vs Leg Side Distribution
            if (ballEvents.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1E293B))
                            .padding(10.dp)
                    ) {
                        Column {
                            Text("OFF SIDE", fontSize = 10.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                            Text("$offRuns Runs ($offPct%)", fontSize = 13.sp, fontWeight = FontWeight.Black, color = HawkEyeCyan)
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1E293B))
                            .padding(10.dp)
                    ) {
                        Column {
                            Text("LEG SIDE", fontSize = 10.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                            Text("$legRuns Runs ($legPct%)", fontSize = 13.sp, fontWeight = FontWeight.Black, color = CricketGreen)
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1E293B))
                        .padding(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No records yet (0 runs)", fontSize = 11.sp, color = TextMuted)
                }
            }
        }
    }
}

@Composable
fun ManhattanAndWormSection() {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Manhattan Chart (Runs per over)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = PitchCard)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Manhattan Chart (Runs Per Over)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text("Innings 2", fontSize = 11.sp, color = HawkEyeCyan)
                }

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .background(Color(0xFF0F172A), RoundedCornerShape(10.dp))
                        .padding(8.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawManhattanBars()
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "• Red dots indicate wickets fallen in that over",
                    fontSize = 10.sp,
                    color = TextMuted
                )
            }
        }

        // Worm Chart (Comparison)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = PitchCard)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Worm Chart (Score Comparison)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).background(Color(0xFFEF4444), CircleShape))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("MSK 177", fontSize = 10.sp, color = TextSecondary)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).background(CricketGreen, CircleShape))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("RS 154*", fontSize = 10.sp, color = CricketGreen)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .background(Color(0xFF0F172A), RoundedCornerShape(10.dp))
                        .padding(8.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawWormCurves()
                    }
                }
            }
        }
    }
}

@Composable
fun TournamentLeadersSection(playerStats: List<PlayerStatEntity>) {
    if (playerStats.isEmpty()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = PitchCard)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("No records yet", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(
                    "Matches khele jaane par Orange Cap, Purple Cap aur Most Sixes yahan automatic update honge.",
                    color = TextMuted,
                    fontSize = 11.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
        return
    }

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Orange Cap (Top Run Getter)
        val orangeCap = playerStats.maxByOrNull { it.runs }
        if (orangeCap != null && orangeCap.runs > 0) {
            LeaderCapCard(
                title = "ORANGE CAP (MOST RUNS)",
                player = orangeCap,
                capColor = StadiumGold,
                statLabel = "${orangeCap.runs} Runs",
                subLabel = "SR ${orangeCap.strikeRate} • HS ${orangeCap.highestScore}"
            )
        }

        // Purple Cap (Top Wicket Taker)
        val purpleCap = playerStats.maxByOrNull { it.wickets }
        if (purpleCap != null && purpleCap.wickets > 0) {
            LeaderCapCard(
                title = "PURPLE CAP (MOST WICKETS)",
                player = purpleCap,
                capColor = Color(0xFFA855F7),
                statLabel = "${purpleCap.wickets} Wickets",
                subLabel = "Econ ${purpleCap.economy}"
            )
        }

        // Highest Individual Scores
        val topScorers = playerStats.filter { it.highestScore > 0 }.sortedByDescending { it.highestScore }.take(5)
        if (topScorers.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = PitchCard)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "HIGHEST INDIVIDUAL SCORES",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    topScorers.forEachIndexed { index, player ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "${index + 1}. ${player.name} (${player.team})", fontSize = 12.sp, color = TextPrimary)
                            Text(text = "${player.highestScore} Runs", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = StadiumGold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LeaderCapCard(
    title: String,
    player: PlayerStatEntity,
    capColor: Color,
    statLabel: String,
    subLabel: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PitchCard),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(capColor.copy(alpha = 0.6f)),
            width = 1.2.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(capColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = title,
                    tint = capColor,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = capColor,
                    letterSpacing = 1.sp
                )
                Text(
                    text = player.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = TextPrimary
                )
                Text(
                    text = "${player.team} • ${player.role}",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
                Text(
                    text = subLabel,
                    fontSize = 10.sp,
                    color = TextMuted
                )
            }

            Text(
                text = statLabel,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                color = capColor
            )
        }
    }
}

@Composable
fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, fontSize = 10.sp, color = TextSecondary)
    }
}

// Canvas Wagon Wheel
private fun DrawScope.drawWagonWheel(ballEvents: List<BallEventEntity>) {
    val w = size.width
    val h = size.height
    val cx = w / 2f
    val cy = h / 2f
    val radius = (h * 0.45f).coerceAtMost(w * 0.45f)

    // Ground Circle
    drawCircle(
        color = Color(0xFF064E3B),
        radius = radius,
        center = Offset(cx, cy)
    )
    drawCircle(
        color = Color.White.copy(alpha = 0.7f),
        radius = radius,
        center = Offset(cx, cy),
        style = Stroke(width = 2.dp.toPx())
    )

    // Center pitch
    drawRect(
        color = Color(0xFFD4A373),
        topLeft = Offset(cx - 4.dp.toPx(), cy - 14.dp.toPx()),
        size = Size(8.dp.toPx(), 28.dp.toPx())
    )

    // Plot real shots from ballEvents
    val validShots = ballEvents.filter { it.runs > 0 }
    validShots.forEach { ball ->
        val deg = ball.shotAngle
        val rad = Math.toRadians((deg - 90).toDouble())
        val distRatio = when (ball.runs) {
            6 -> 1.0f
            4 -> 0.92f
            2, 3 -> 0.65f
            else -> 0.45f
        }
        val endX = cx + (radius * distRatio * cos(rad)).toFloat()
        val endY = cy + (radius * distRatio * sin(rad)).toFloat()

        val shotColor = when {
            ball.runs >= 6 -> StadiumGold
            ball.runs >= 4 -> CricketGreen
            ball.isWicket -> DrsOutRed
            else -> Color.White.copy(alpha = 0.8f)
        }

        drawLine(
            color = shotColor,
            start = Offset(cx, cy),
            end = Offset(endX, endY),
            strokeWidth = if (ball.runs >= 4) 2.5.dp.toPx() else 1.5.dp.toPx()
        )
        drawCircle(
            color = shotColor,
            radius = if (ball.runs >= 4) 3.5.dp.toPx() else 2.dp.toPx(),
            center = Offset(endX, endY)
        )
    }
}

// Canvas Manhattan Bars
private fun DrawScope.drawManhattanBars() {
    val w = size.width
    val h = size.height

    val overRuns = listOf(7, 4, 12, 6, 14, 9, 8, 5, 11, 7, 15, 6, 13, 8, 10, 14, 12)
    val wicketsAt = listOf(4, 9, 14, 16) // over indices where wickets fell
    val barWidth = (w / (overRuns.size + 2))

    overRuns.forEachIndexed { i, runs ->
        val x = (i + 1) * barWidth
        val barHeight = (runs / 18f) * (h * 0.8f)
        val y = h - barHeight

        drawRect(
            color = if (runs >= 12) StadiumGold else CricketGreen,
            topLeft = Offset(x, y),
            size = Size(barWidth * 0.7f, barHeight)
        )

        if (i in wicketsAt) {
            drawCircle(
                color = DrsOutRed,
                radius = 4.dp.toPx(),
                center = Offset(x + (barWidth * 0.35f), y - 6.dp.toPx())
            )
        }
    }
}

// Canvas Worm Curves
private fun DrawScope.drawWormCurves() {
    val w = size.width
    val h = size.height

    val team1Runs = listOf(0, 8, 17, 24, 38, 49, 61, 70, 78, 89, 98, 112, 122, 134, 145, 153, 162, 177)
    val team2Runs = listOf(0, 10, 16, 28, 36, 52, 64, 76, 84, 95, 110, 118, 130, 142, 154)

    val maxRuns = 185f

    // Team 1 path (Red)
    val path1 = Path()
    team1Runs.forEachIndexed { i, score ->
        val x = (i.toFloat() / 20f) * w
        val y = h - ((score / maxRuns) * (h * 0.9f))
        if (i == 0) path1.moveTo(x, y) else path1.lineTo(x, y)
    }
    drawPath(path = path1, color = Color(0xFFEF4444), style = Stroke(width = 2.dp.toPx()))

    // Team 2 path (Green)
    val path2 = Path()
    team2Runs.forEachIndexed { i, score ->
        val x = (i.toFloat() / 20f) * w
        val y = h - ((score / maxRuns) * (h * 0.9f))
        if (i == 0) path2.moveTo(x, y) else path2.lineTo(x, y)
    }
    drawPath(path = path2, color = CricketGreen, style = Stroke(width = 2.5.dp.toPx()))
}
