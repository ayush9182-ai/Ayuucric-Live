package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ai.MatchSummaryResult
import com.example.data.audio.SidhuVoiceStyle
import com.example.data.model.BallEventEntity
import com.example.data.model.DeviceRole
import com.example.data.model.MatchEntity
import com.example.data.repository.CricketRepository
import com.example.ui.theme.*
import com.example.ui.viewmodel.CommentaryFilter
import com.example.ui.viewmodel.LiveCenterSubTab

/**
 * Commercial-grade LiveCenterTab following Content-First Hierarchy:
 *  1. Top 40% Viewport: Broadcast Video Player + Pitch Cam + Scoreboard Overlay
 *  2. Docked Sidhu Paaji AI Commentary Pill (Compact Play/Pause + Style indicator)
 *  3. Sub-tab switcher: Summary, Scorecard, Commentary, Radar
 *  4. Role-specific Contextual Floating Dock (Spectator vs Official Scorer vs Umpire)
 *  5. Elimination of redundant duplicate buttons across the UI
 */
@Composable
fun LiveCenterTab(
    match: MatchEntity?,
    ballEvents: List<BallEventEntity>,
    commentaryFilter: CommentaryFilter,
    currentRole: DeviceRole,
    spectatorCamAngle: String,
    onSelectSpectatorAngle: (String) -> Unit,
    liveCenterSubTab: LiveCenterSubTab,
    onSelectSubTab: (LiveCenterSubTab) -> Unit,
    isVideoOverlayExpanded: Boolean,
    onToggleVideoOverlay: () -> Unit,
    onSelectFilter: (CommentaryFilter) -> Unit,
    onOpenScorer: () -> Unit,
    onOpenDrsReview: () -> Unit,
    onOpenUmpireCamera: () -> Unit,
    onOpenRoleDialog: () -> Unit,
    onSwitchStriker: () -> Unit,
    onTriggerDelivery: () -> Unit,
    isSidhuCommentaryEnabled: Boolean,
    isSidhuSpeaking: Boolean,
    sidhuCurrentDialogue: String,
    sidhuVoiceStyle: SidhuVoiceStyle,
    onToggleSidhuCommentary: (Boolean) -> Unit,
    onSelectSidhuStyle: (SidhuVoiceStyle) -> Unit,
    onTestSidhuVoice: () -> Unit,
    matchSummary: MatchSummaryResult?,
    onOpenSummaryDialog: () -> Unit,
    onQuickListenSummary: () -> Unit,
    onRecordRun: (Int) -> Unit = {},
    onRecordWicket: () -> Unit = {},
    onRecordExtra: (String) -> Unit = {},
    onUndoDelivery: () -> Unit = {},
    onOpenMessagesHub: () -> Unit = {},
    onTogglePitchCam: () -> Unit = {},
    onToggleSideCam: () -> Unit = {},
    onTriggerAppeal: (String) -> Unit = {},
    onChangeBowler: () -> Unit = {},
    onChangeBatsman: () -> Unit = {},
    onOpenNewBatsmanDialog: () -> Unit = {},
    onSwitchBattingTeam: () -> Unit = {},
    onStartSecondInnings: () -> Unit = {},
    onEditVenue: (() -> Unit)? = null
) {
    if (match == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .testTag("live_center_empty_state"),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = PitchCard),
                border = BorderStroke(1.2.dp, Color(0xFF334155))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E293B)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🏏", fontSize = 32.sp)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "No Match in Progress",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Select a fixture or create a new match to track live scores and ball-by-ball commentary.",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = onOpenRoleDialog,
                        colors = ButtonDefaults.buttonColors(containerColor = StadiumGold, contentColor = PitchDark),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = PitchDark)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Start Match", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = onOpenMessagesHub,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFF475569)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Chat, contentDescription = null, tint = CricketGreen)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Match Chat & DMs", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        return
    }

    val activeMatch = match
    val activeBallEvents = ballEvents

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("live_center_tab")
    ) {
        // Scrollable Match Center Content
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // 1. Primary Viewport: Broadcast Video Player (Top ~40%)
            item {
                LiveMatchBroadcastPlayer(
                    match = activeMatch,
                    recentBalls = activeBallEvents,
                    currentRole = currentRole,
                    spectatorCamAngle = spectatorCamAngle,
                    onSelectSpectatorAngle = onSelectSpectatorAngle,
                    isExpanded = isVideoOverlayExpanded,
                    onToggleExpand = onToggleVideoOverlay
                )
            }

            // 2. CricHeroes Sub-Tab Switcher (Summary, Scorecard, Commentary, Radar)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(PitchSurface)
                        .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(10.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(
                        Pair(LiveCenterSubTab.LIVE_SUMMARY, "⚡ Summary"),
                        Pair(LiveCenterSubTab.DETAILED_SCORECARD, "📊 Scorecard"),
                        Pair(LiveCenterSubTab.COMMENTARY, "🎙️ Commentary"),
                        Pair(LiveCenterSubTab.MATCH_DETAIL, "📋 Details")
                    ).forEach { (subTab, title) ->
                        val isSelected = liveCenterSubTab == subTab
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) CricketGreen else Color.Transparent)
                                .clickable { onSelectSubTab(subTab) }
                                .padding(vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = title,
                                color = if (isSelected) PitchDark else TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // 4. Dynamic Content based on CricHeroes Sub-Tab
            when (liveCenterSubTab) {
                LiveCenterSubTab.LIVE_SUMMARY -> {
                    val isOfficialScorer = currentRole == DeviceRole.OFFICIAL_SCORER || currentRole.isOfficial
                    // Match Score Equation & Batsmen/Bowler Banner
                    item {
                        ScoreBanner(
                            match = activeMatch,
                            recentBalls = activeBallEvents,
                            onSwitchStriker = onSwitchStriker,
                            isOfficialScorer = isOfficialScorer,
                            onChangeBowler = onChangeBowler,
                            onChangeBatsman = onChangeBatsman,
                            onSwitchBattingTeam = onSwitchBattingTeam,
                            onStartSecondInnings = onStartSecondInnings,
                            onEditVenue = onEditVenue
                        )
                    }

                    // AI Match Wrap-up & Summary Card
                    item {
                        AiMatchSummaryCard(
                            summary = matchSummary,
                            onOpenSummaryDialog = onOpenSummaryDialog,
                            onQuickListen = onQuickListenSummary
                        )
                    }
                }

                LiveCenterSubTab.DETAILED_SCORECARD -> {
                    item {
                        DetailedScorecardView(match = activeMatch)
                    }
                }

                LiveCenterSubTab.COMMENTARY -> {
                    val isOfficialScorer = currentRole == DeviceRole.OFFICIAL_SCORER
                    item {
                        BallCommentaryList(
                            ballEvents = activeBallEvents,
                            selectedFilter = commentaryFilter,
                            onSelectFilter = onSelectFilter,
                            onOpenScorer = onOpenScorer,
                            onOpenDrsReview = onOpenDrsReview,
                            isOfficialScorer = isOfficialScorer,
                            isMatchFinished = activeMatch.status == "FINISHED"
                        )
                    }
                }

                LiveCenterSubTab.MATCH_DETAIL -> {
                    item {
                        MatchDetailScreen(
                            match = activeMatch,
                            ballEvents = activeBallEvents,
                            onBack = { onSelectSubTab(LiveCenterSubTab.LIVE_SUMMARY) },
                            commentaryFilter = commentaryFilter,
                            onSelectFilter = onSelectFilter,
                            currentRole = currentRole,
                            onOpenScorer = onOpenScorer,
                            onOpenDrsReview = onOpenDrsReview,
                            onEditVenue = onEditVenue
                        )
                    }
                }

                else -> {}
            }

            // Bottom spacer to ensure scrolling content clears the persistent dock
            item {
                Spacer(modifier = Modifier.height(78.dp))
            }
        }

        // 5. Contextual Action Floating Bar (FAB / Dock) at Bottom
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            when (currentRole) {
                DeviceRole.SPECTATOR_VIEWER -> {
                    SpectatorContextualDock(
                        currentAngle = spectatorCamAngle,
                        onSelectAngle = onSelectSpectatorAngle,
                        onOpenChat = onOpenMessagesHub
                    )
                }

                DeviceRole.OFFICIAL_SCORER -> {
                    if (activeMatch.status == "FINISHED") {
                        MatchFinishedScorerDock(
                            statusDetail = activeMatch.statusDetail,
                            onOpenSummary = onOpenSummaryDialog
                        )
                    } else if (activeMatch.status == "INNINGS_BREAK" || (activeMatch.currentInnings == 1 && activeMatch.legalBalls >= activeMatch.totalOvers * 6)) {
                        InningsBreakScorerDock(
                            match = activeMatch,
                            onStartSecondInnings = onStartSecondInnings
                        )
                    } else {
                        ScorerPersistentDock(
                            onRecordRun = onRecordRun,
                            onRecordWicket = onOpenNewBatsmanDialog,
                            onRecordExtra = onRecordExtra,
                            onUndoDelivery = onUndoDelivery,
                            onOpenFullKeypad = onOpenScorer,
                            onChangeBowler = onChangeBowler,
                            onChangeBatsman = onChangeBatsman,
                            onStartSecondInnings = onStartSecondInnings,
                            isFirstInnings = activeMatch.currentInnings == 1
                        )
                    }
                }

                DeviceRole.BOWLER_END_UMPIRE,
                DeviceRole.SQUARE_LEG_UMPIRE,
                DeviceRole.THIRD_UMPIRE_DRS -> {
                    UmpireContextualDock(
                        currentRole = currentRole,
                        onOpenUmpireCamera = onOpenUmpireCamera,
                        onTriggerAppeal = onTriggerAppeal,
                        onOpenDrsReview = onOpenDrsReview
                    )
                }
            }
        }
    }
}

/**
 * Docked Sidhu Paaji AI Commentary floating pill right below video player.
 */
@Composable
private fun DockedAiCommentaryPill(
    isEnabled: Boolean,
    isSpeaking: Boolean,
    currentDialogue: String,
    selectedStyle: SidhuVoiceStyle,
    onToggleEnabled: (Boolean) -> Unit,
    onSelectStyle: (SidhuVoiceStyle) -> Unit,
    onTestVoice: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("docked_sidhu_pill"),
        shape = RoundedCornerShape(16.dp),
        color = if (isEnabled) Color(0xFF1B182B) else Color(0xFF111722),
        border = BorderStroke(
            1.dp,
            if (isEnabled) Brush.horizontalGradient(listOf(StadiumGold, Color(0xFFA78BFA), CricketGreen))
            else androidx.compose.ui.graphics.SolidColor(Color(0xFF1E293B))
        ),
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Icon + Text Snippet
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(
                            if (isEnabled) Brush.radialGradient(listOf(StadiumGold, Color(0xFFD97706)))
                            else Brush.linearGradient(listOf(Color(0xFF334155), Color(0xFF1E293B)))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isEnabled) "👳" else "🎙️",
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Sidhu Paaji AI",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isEnabled) StadiumGold else TextSecondary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        // Style pill
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF334155).copy(alpha = 0.5f))
                                .clickable {
                                    val styles = SidhuVoiceStyle.values()
                                    val nextIndex = (selectedStyle.ordinal + 1) % styles.size
                                    onSelectStyle(styles[nextIndex])
                                }
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = selectedStyle.label.split(" ").firstOrNull() ?: "Style",
                                fontSize = 8.sp,
                                color = HawkEyeCyan,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    val dialogueText = if (isEnabled) {
                        if (currentDialogue.isNotBlank()) "\"$currentDialogue\"" else "Active • Thoko Taali!"
                    } else {
                        "Voice Commentary Muted"
                    }

                    Text(
                        text = dialogueText,
                        fontSize = 9.sp,
                        color = if (isEnabled) Color(0xFFE2E8F0) else TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Right: Play/Mute Audio Actions
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isEnabled) {
                    IconButton(
                        onClick = onTestVoice,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = "Test Voice",
                            tint = StadiumGold,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                IconButton(
                    onClick = { onToggleEnabled(!isEnabled) },
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(if (isEnabled) StadiumGold else Color(0xFF1E293B))
                ) {
                    Icon(
                        imageVector = if (isEnabled) Icons.Default.Mic else Icons.Default.MicOff,
                        contentDescription = "Toggle Audio",
                        tint = if (isEnabled) PitchDark else TextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

/**
 * Spectator Contextual Dock:
 * Shows ONLY Watch Angles and Fan Chat.
 * Spectators cannot call DRS reviews or score runs.
 */
@Composable
private fun SpectatorContextualDock(
    currentAngle: String,
    onSelectAngle: (String) -> Unit,
    onOpenChat: () -> Unit
) {
    Surface(
        color = Color(0xEE0F172A),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, Color(0xFF1E293B)),
        tonalElevation = 10.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Watch Angles Pills
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1E293B))
                    .padding(2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                listOf(
                    Pair("PITCH", "Pitch"),
                    Pair("SQUARE_LEG", "Crease"),
                    Pair("BATSMAN", "Batter")
                ).forEach { (angleKey, label) ->
                    val isSel = currentAngle == angleKey
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSel) CricketGreen else Color.Transparent)
                            .clickable { onSelectAngle(angleKey) }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = label,
                            fontSize = 10.sp,
                            fontWeight = if (isSel) FontWeight.Black else FontWeight.SemiBold,
                            color = if (isSel) PitchDark else TextSecondary
                        )
                    }
                }
            }

            // Fan Chat Button
            Button(
                onClick = onOpenChat,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE1306C).copy(alpha = 0.15f)),
                border = BorderStroke(1.dp, Color(0xFFE1306C).copy(alpha = 0.6f)),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text(text = "💬", fontSize = 11.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Fan Chat",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF43F5E)
                )
            }
        }
    }
}

/**
 * Official Scorer Persistent Dock:
 * Rapid one-tap scoring keys [+0] [+1] [+2] [+4] [+6] [OUT] [WD] [Undo]
 * Plus quick tap to expand full scorer sheet.
 */
@Composable
private fun ScorerPersistentDock(
    onRecordRun: (Int) -> Unit,
    onRecordWicket: () -> Unit,
    onRecordExtra: (String) -> Unit,
    onUndoDelivery: () -> Unit,
    onOpenFullKeypad: () -> Unit,
    onChangeBowler: () -> Unit = {},
    onChangeBatsman: () -> Unit = {},
    onStartSecondInnings: () -> Unit = {},
    isFirstInnings: Boolean = false
) {
    Surface(
        color = Color(0xFA1E1B0F),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, StadiumGold.copy(alpha = 0.6f)),
        tonalElevation = 12.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 6.dp)
        ) {
            // Scorer Header indicator
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "⚡ SCORER DOCK",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    color = StadiumGold,
                    letterSpacing = 1.sp
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isFirstInnings) {
                        Text(
                            text = "2nd Inn 🚀",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFEF08A),
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(StadiumGold.copy(alpha = 0.25f))
                                .clickable { onStartSecondInnings() }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = "🎳 Bowler",
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = StadiumGold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(StadiumGold.copy(alpha = 0.15f))
                            .clickable { onChangeBowler() }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                    Text(
                        text = "🏏 Batter",
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = CricketGreen,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(CricketGreen.copy(alpha = 0.15f))
                            .clickable { onChangeBatsman() }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                    Text(
                        text = "Keypad ›",
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = HawkEyeCyan,
                        modifier = Modifier.clickable { onOpenFullKeypad() }
                    )
                }
            }

            // Quick Scoring Button Strip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ScoringPillButton(text = "0", color = Color(0xFF334155), textColor = Color.White, modifier = Modifier.weight(0.9f)) {
                    onRecordRun(0)
                }
                ScoringPillButton(text = "+1", color = CricketGreen, textColor = PitchDark, modifier = Modifier.weight(1f)) {
                    onRecordRun(1)
                }
                ScoringPillButton(text = "+2", color = CricketGreen, textColor = PitchDark, modifier = Modifier.weight(1f)) {
                    onRecordRun(2)
                }
                ScoringPillButton(text = "+4", color = StadiumGold, textColor = PitchDark, modifier = Modifier.weight(1f)) {
                    onRecordRun(4)
                }
                ScoringPillButton(text = "+6", color = Color(0xFFA78BFA), textColor = PitchDark, modifier = Modifier.weight(1f)) {
                    onRecordRun(6)
                }
                ScoringPillButton(text = "OUT", color = DrsOutRed, textColor = Color.White, modifier = Modifier.weight(1.1f)) {
                    onRecordWicket()
                }
                ScoringPillButton(text = "WD", color = Color(0xFFF97316), textColor = Color.White, modifier = Modifier.weight(0.9f)) {
                    onRecordExtra("Wide")
                }
                ScoringPillButton(text = "↶", color = Color(0xFF1E293B), textColor = TextSecondary, modifier = Modifier.weight(0.8f)) {
                    onUndoDelivery()
                }
            }
        }
    }
}

@Composable
private fun ScoringPillButton(
    text: String,
    color: Color,
    textColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(38.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(color)
            .clickable { onClick() }
            .padding(horizontal = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            color = textColor,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
private fun MatchFinishedScorerDock(
    statusDetail: String,
    onOpenSummary: () -> Unit
) {
    Surface(
        color = Color(0xFA1E293B),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.2.dp, StadiumGold),
        tonalElevation = 12.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "🏆 MATCH FINISHED",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = StadiumGold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = statusDetail.ifBlank { "Match successfully completed" },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onOpenSummary,
                colors = ButtonDefaults.buttonColors(containerColor = StadiumGold, contentColor = PitchDark),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "Summary 📊",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

@Composable
private fun InningsBreakScorerDock(
    match: MatchEntity,
    onStartSecondInnings: () -> Unit
) {
    Surface(
        color = Color(0xFA0F2E2A),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.2.dp, CricketGreen),
        tonalElevation = 12.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "🏏 1ST INNINGS COMPLETED (${match.totalOvers} ov)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = StadiumGold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Target: ${match.target} runs for ${match.bowlingTeam}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = CricketGreen,
                    maxLines = 1
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onStartSecondInnings,
                colors = ButtonDefaults.buttonColors(containerColor = CricketGreen, contentColor = PitchDark),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "Start 2nd Inn 🚀",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

/**
 * Umpire Contextual Dock:
 * Camera streaming toggle, DRS appeal triggers, and crease review.
 */
@Composable
private fun UmpireContextualDock(
    currentRole: DeviceRole,
    onOpenUmpireCamera: () -> Unit,
    onTriggerAppeal: (String) -> Unit,
    onOpenDrsReview: () -> Unit
) {
    Surface(
        color = Color(0xEE0B1F19),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, CricketGreen.copy(alpha = 0.6f)),
        tonalElevation = 10.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // DRS Review Button (Sidhu Paaji Announcement)
            Button(
                onClick = onOpenDrsReview,
                colors = ButtonDefaults.buttonColors(containerColor = StadiumGold),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                modifier = Modifier
                    .weight(1.3f)
                    .height(38.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Policy,
                    contentDescription = null,
                    tint = PitchDark,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "🎙️ Take DRS",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Black,
                    color = PitchDark,
                    maxLines = 1,
                    softWrap = false
                )
            }

            if (currentRole.isOfficial) {
                // LBW Appeal Trigger (Officials Only)
                OutlinedButton(
                    onClick = { onTriggerAppeal("LBW") },
                    border = BorderStroke(1.dp, StadiumGold),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                ) {
                    Text(
                        text = "LBW Check",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = StadiumGold,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                // Caught Behind Appeal Trigger (Officials Only)
                OutlinedButton(
                    onClick = { onTriggerAppeal("CAUGHT_BEHIND") },
                    border = BorderStroke(1.dp, HawkEyeCyan),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                ) {
                    Text(
                        text = "Edge Check",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = HawkEyeCyan,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            } else {
                // Spectator View Only Badge - Spectators can watch and chat only
                Box(
                    modifier = Modifier
                        .weight(2f)
                        .height(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF0F172A))
                        .border(1.dp, Color(0xFF334155), RoundedCornerShape(10.dp))
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.RemoveRedEye,
                            contentDescription = null,
                            tint = CricketGreen,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "Spectator: Match Dekhein & Chat Karein",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextSecondary,
                            maxLines = 1
                        )
                    }
                }
            }

            // Full DRS Screen
            IconButton(
                onClick = onOpenDrsReview,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E293B))
            ) {
                Icon(
                    imageVector = Icons.Default.Policy,
                    contentDescription = "DRS",
                    tint = Color(0xFFA78BFA),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
