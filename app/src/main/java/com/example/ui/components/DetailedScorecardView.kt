package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MatchEntity
import com.example.data.model.parseDismissedBatsmen
import com.example.ui.theme.CricketGreen
import com.example.ui.theme.HawkEyeCyan
import com.example.ui.theme.PitchCard
import com.example.ui.theme.StadiumGold
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun DetailedScorecardView(
    match: MatchEntity,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Batting Scorecard Table
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = PitchCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🏏 ${match.teamB} Batting",
                        color = StadiumGold,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${match.score}/${match.wickets} (${match.legalBalls / 6}.${match.legalBalls % 6} ov)",
                        color = CricketGreen,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Table Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF0F172A))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("BATTER", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(2.2f))
                    Text("R", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.7f))
                    Text("B", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.7f))
                    Text("4s", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.6f))
                    Text("6s", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.6f))
                    Text("SR", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                }

                // Striker
                val strikerSr = if (match.strikerBalls > 0) String.format("%.1f", (match.strikerRuns.toFloat() / match.strikerBalls) * 100) else "0.0"
                BatterScorecardRow(
                    name = "${match.strikerName}*",
                    dismissal = "batting (on strike)",
                    runs = match.strikerRuns,
                    balls = match.strikerBalls,
                    fours = match.strikerFours,
                    sixes = match.strikerSixes,
                    sr = strikerSr,
                    isNotOut = true
                )

                // Non-Striker
                val nonStrikerSr = if (match.nonStrikerBalls > 0) String.format("%.1f", (match.nonStrikerRuns.toFloat() / match.nonStrikerBalls) * 100) else "0.0"
                BatterScorecardRow(
                    name = match.nonStrikerName,
                    dismissal = "batting (non-strike)",
                    runs = match.nonStrikerRuns,
                    balls = match.nonStrikerBalls,
                    fours = match.nonStrikerFours,
                    sixes = match.nonStrikerSixes,
                    sr = nonStrikerSr,
                    isNotOut = true
                )

                // Dismissed Batsmen (Record of all batsmen who got out)
                val dismissedList = remember(match.dismissedBatsmenJson) {
                    parseDismissedBatsmen(match.dismissedBatsmenJson)
                }

                if (dismissedList.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0F172A).copy(alpha = 0.6f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "DISMISSED BATSMEN (${dismissedList.size})",
                            color = Color(0xFFEF4444),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    dismissedList.forEach { batsman ->
                        val sr = if (batsman.balls > 0) {
                            String.format("%.1f", (batsman.runs.toFloat() / batsman.balls) * 100)
                        } else "0.0"
                        BatterScorecardRow(
                            name = batsman.name,
                            dismissal = batsman.dismissal,
                            runs = batsman.runs,
                            balls = batsman.balls,
                            fours = batsman.fours,
                            sixes = batsman.sixes,
                            sr = batsman.strikeRate.ifBlank { sr },
                            isNotOut = false
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Extras & Total
                val dismissedRunsTotal = dismissedList.sumOf { it.runs }
                val batRunsTotal = match.strikerRuns + match.nonStrikerRuns + dismissedRunsTotal
                val extrasTotal = (match.score - batRunsTotal).coerceAtLeast(0)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Extras (wides, no-balls, byes)", color = TextSecondary, fontSize = 11.sp)
                    Text("$extrasTotal", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF1E293B))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("TOTAL (${match.wickets} wkts, ${match.legalBalls / 6}.${match.legalBalls % 6} Ov)", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Black)
                    Text("${match.score}", color = CricketGreen, fontSize = 13.sp, fontWeight = FontWeight.Black)
                }

                if (dismissedList.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Fall of Wickets: " + dismissedList.mapIndexed { idx, b -> "${idx + 1}-${b.runs} (${b.name})" }.joinToString(", "),
                        color = TextSecondary,
                        fontSize = 10.sp,
                        lineHeight = 14.sp,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }
        }

        // 2. Bowling Scorecard Table
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = PitchCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "🎯 ${match.teamA} Bowling",
                    color = HawkEyeCyan,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Table Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF0F172A))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("BOWLER", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(2.2f))
                    Text("O", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.7f))
                    Text("M", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.6f))
                    Text("R", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.7f))
                    Text("W", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.6f))
                    Text("ECON", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                }

                // Bowler 1 (Current Bowler)
                val bowlerOvers = "${match.bowlerBalls / 6}.${match.bowlerBalls % 6}"
                val econ = if (match.bowlerBalls > 0) String.format("%.2f", (match.bowlerRuns.toFloat() / match.bowlerBalls) * 6) else "0.00"
                BowlerScorecardRow(
                    name = "${match.bowlerName}*",
                    overs = bowlerOvers,
                    maidens = match.bowlerMaidens,
                    runs = match.bowlerRuns,
                    wickets = match.bowlerWickets,
                    economy = econ,
                    isCurrent = true
                )
            }
        }
    }
}

@Composable
private fun BatterScorecardRow(
    name: String,
    dismissal: String,
    runs: Int,
    balls: Int,
    fours: Int,
    sixes: Int,
    sr: String,
    isNotOut: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp, horizontal = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(2.2f)) {
                Text(
                    text = name,
                    color = if (isNotOut) StadiumGold else TextPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = dismissal,
                    color = TextSecondary,
                    fontSize = 9.sp
                )
            }
            Text(text = "$runs", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(0.7f))
            Text(text = "$balls", color = TextSecondary, fontSize = 11.sp, modifier = Modifier.weight(0.7f))
            Text(text = "$fours", color = TextSecondary, fontSize = 11.sp, modifier = Modifier.weight(0.6f))
            Text(text = "$sixes", color = TextSecondary, fontSize = 11.sp, modifier = Modifier.weight(0.6f))
            Text(text = sr, color = CricketGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun BowlerScorecardRow(
    name: String,
    overs: String,
    maidens: Int,
    runs: Int,
    wickets: Int,
    economy: String,
    isCurrent: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp, horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = name,
            color = if (isCurrent) HawkEyeCyan else TextPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(2.2f)
        )
        Text(text = overs, color = TextSecondary, fontSize = 11.sp, modifier = Modifier.weight(0.7f))
        Text(text = "$maidens", color = TextSecondary, fontSize = 11.sp, modifier = Modifier.weight(0.6f))
        Text(text = "$runs", color = Color.White, fontSize = 11.sp, modifier = Modifier.weight(0.7f))
        Text(text = "$wickets", color = CricketGreen, fontSize = 11.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(0.6f))
        Text(text = economy, color = TextSecondary, fontSize = 11.sp, modifier = Modifier.weight(1f))
    }
}
