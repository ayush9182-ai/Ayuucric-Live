package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Policy
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BallEventEntity
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
import com.example.ui.viewmodel.CommentaryFilter

@Composable
fun BallCommentaryList(
    ballEvents: List<BallEventEntity>,
    selectedFilter: CommentaryFilter,
    onSelectFilter: (CommentaryFilter) -> Unit,
    onOpenScorer: () -> Unit,
    onOpenDrsReview: () -> Unit,
    modifier: Modifier = Modifier
) {
    val filteredBalls = when (selectedFilter) {
        CommentaryFilter.ALL -> ballEvents
        CommentaryFilter.BOUNDARIES -> ballEvents.filter { it.isBoundary || it.isSix }
        CommentaryFilter.WICKETS -> ballEvents.filter { it.isWicket }
        CommentaryFilter.OVERS -> ballEvents.filter { it.ballInOver == 6 || it.ballInOver == 1 }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Scorer & DRS Action Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onOpenScorer,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .testTag("open_scorer_sheet_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = CricketGreen),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Scorer Panel",
                    tint = PitchDark,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Record Ball (Scorer)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = PitchDark
                )
            }

            OutlinedButton(
                onClick = onOpenDrsReview,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .testTag("trigger_drs_review_btn"),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, HawkEyeCyan),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = HawkEyeCyan)
            ) {
                Icon(
                    imageVector = Icons.Default.Policy,
                    contentDescription = "DRS Review",
                    tint = HawkEyeCyan,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "DRS Appeal Review",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = HawkEyeCyan
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Commentary Filter Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CommentaryFilter.entries.forEach { filter ->
                val label = when (filter) {
                    CommentaryFilter.ALL -> "All Balls"
                    CommentaryFilter.BOUNDARIES -> "Boundaries (4s/6s)"
                    CommentaryFilter.WICKETS -> "Wickets"
                    CommentaryFilter.OVERS -> "Over Summaries"
                }

                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = { onSelectFilter(filter) },
                    label = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                    modifier = Modifier.height(30.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CricketGreen.copy(alpha = 0.2f),
                        selectedLabelColor = CricketGreen,
                        labelColor = TextSecondary
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Commentary Items List
        if (filteredBalls.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Campaign,
                        contentDescription = "Empty",
                        tint = TextMuted,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No commentary items for this filter",
                        color = TextMuted,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                filteredBalls.forEach { ball ->
                    CommentaryItemCard(ball = ball)
                }
            }
        }
    }
}

@Composable
fun CommentaryItemCard(ball: BallEventEntity) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("commentary_item_${ball.overNumber}_${ball.ballInOver}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                ball.isWicket -> DrsOutRed.copy(alpha = 0.12f)
                ball.isSix -> StadiumGold.copy(alpha = 0.12f)
                ball.isBoundary -> CricketGreen.copy(alpha = 0.08f)
                else -> PitchCard
            }
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                when {
                    ball.isWicket -> DrsOutRed.copy(alpha = 0.6f)
                    ball.isSix -> StadiumGold.copy(alpha = 0.6f)
                    ball.isBoundary -> CricketGreen.copy(alpha = 0.4f)
                    else -> PitchCardBorder
                }
            ),
            width = 0.8.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Over & Ball Pill
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(44.dp)
            ) {
                Text(
                    text = "${ball.overNumber}.${ball.ballInOver}",
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp,
                    color = HawkEyeCyan
                )

                Spacer(modifier = Modifier.height(4.dp))

                RecentBallPill(ball = ball)
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Commentary Text
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
                        color = TextSecondary
                    )

                    if (ball.pitchZone.isNotEmpty()) {
                        Text(
                            text = ball.pitchZone,
                            fontSize = 10.sp,
                            color = TextMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = ball.commentary,
                    fontSize = 13.sp,
                    color = TextPrimary,
                    lineHeight = 18.sp
                )
            }
        }
    }
}
