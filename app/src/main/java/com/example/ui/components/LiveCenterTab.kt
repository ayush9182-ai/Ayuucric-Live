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
    onOpenNewBatsmanDialog: () -> Unit = {}
) {
    val activeMatch = match ?: remember { CricketRepository.sampleMatch }
    val activeBallEvents = if (ballEvents.isNotEmpty()) ballEvents else remember { CricketRepository.sampleBallEvents }

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

            // 2. Docked Sidhu Paaji AI Audio Pill (Sleek Compact Floating Bar)
            item {
                DockedAiCommentaryPill(
                    isEnabled = isSidhuCommentaryEnabled,
                    isSpeaking = isSidhuSpeaking,
                    currentDialogue = sidhuCurrentDialogue,
                    selectedStyle = sidhuVoiceStyle,
                    onToggleEnabled = onToggleSidhuCommentary,
                    onSelectStyle = onSelectSidhuStyle,
                    onTestVoice = onTestSidhuVoice
                )
            }

            // 3. CricHeroes Sub-Tab Switcher (Summary, Scorecard, Commentary, Radar)
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
                        Pair(LiveCenterSubTab.COMMENTARY, "🎙️ Comm"),
                        Pair(LiveCenterSubTab.HAWKEYE_RADAR, "🎯 Radar")
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
                    // Match Score Equation & Batsmen/Bowler Banner
                    item {
                        ScoreBanner(
                            match = activeMatch,
                            recentBalls = activeBallEvents,
                            onSwitchStriker = onSwitchStriker,
                            onChangeBowler = onChangeBowler,
                            onChangeBatsman = onChangeBatsman
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

                    // Zero-Delay Pitch Radar
                    item {
                        ZeroDelayLiveRadar(
                            onTriggerDelivery = onTriggerDelivery
                        )
                    }
                }

                LiveCenterSubTab.DETAILED_SCORECARD -> {
                    item {
                        DetailedScorecardView(match = activeMatch)
                    }
                }

                LiveCenterSubTab.COMMENTARY -> {
                    item {
                        BallCommentaryList(
                            ballEvents = activeBallEvents,
                            selectedFilter = commentaryFilter,
                            onSelectFilter = onSelectFilter,
                            onOpenScorer = onOpenScorer,
                            onOpenDrsReview = onOpenDrsReview
                        )
                    }
                }

                LiveCenterSubTab.HAWKEYE_RADAR -> {
                    item {
                        ZeroDelayLiveRadar(
                            onTriggerDelivery = onTriggerDelivery
                        )
                    }
                }
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
                        onOpenDrs = onOpenDrsReview,
                        onOpenChat = onOpenMessagesHub
                    )
                }

                DeviceRole.OFFICIAL_SCORER -> {
                    ScorerPersistentDock(
                        onRecordRun = onRecordRun,
                        onRecordWicket = onOpenNewBatsmanDialog,
                        onRecordExtra = onRecordExtra,
                        onUndoDelivery = onUndoDelivery,
                        onOpenFullKeypad = onOpenScorer,
                        onChangeBowler = onChangeBowler,
                        onChangeBatsman = onChangeBatsman
                    )
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
 * Shows ONLY Watch Angles, DRS Big Screen, and Fan Chat.
 * Absolutely NO scoring keys, reset buttons, or wicket controls.
 */
@Composable
private fun SpectatorContextualDock(
    currentAngle: String,
    onSelectAngle: (String) -> Unit,
    onOpenDrs: () -> Unit,
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
                            .padding(horizontal = 8.dp, vertical = 5.dp)
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

            // DRS Big Screen Button
            Button(
                onClick = onOpenDrs,
                colors = ButtonDefaults.buttonColors(containerColor = HawkEyeCyan.copy(alpha = 0.15f)),
                border = BorderStroke(1.dp, HawkEyeCyan.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Policy,
                    contentDescription = null,
                    tint = HawkEyeCyan,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "DRS View",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = HawkEyeCyan
                )
            }

            // Fan Chat Button
            Button(
                onClick = onOpenChat,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE1306C).copy(alpha = 0.15f)),
                border = BorderStroke(1.dp, Color(0xFFE1306C).copy(alpha = 0.6f)),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text(text = "🔥", fontSize = 11.sp)
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
    onChangeBatsman: () -> Unit = {}
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
            .height(34.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(color)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            color = textColor
        )
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
            // Camera Stream Button
            Button(
                onClick = onOpenUmpireCamera,
                colors = ButtonDefaults.buttonColors(containerColor = CricketGreen),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                modifier = Modifier
                    .weight(1.2f)
                    .height(34.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    tint = PitchDark,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Cam View",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = PitchDark
                )
            }

            // LBW Appeal Trigger
            OutlinedButton(
                onClick = { onTriggerAppeal("LBW") },
                border = BorderStroke(1.dp, StadiumGold),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(34.dp)
            ) {
                Text(
                    text = "LBW Appeal",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = StadiumGold
                )
            }

            // Edge / UltraEdge Appeal Trigger
            OutlinedButton(
                onClick = { onTriggerAppeal("EDGE") },
                border = BorderStroke(1.dp, HawkEyeCyan),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(34.dp)
            ) {
                Text(
                    text = "Edge Check",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = HawkEyeCyan
                )
            }

            // Full DRS Screen
            IconButton(
                onClick = onOpenDrsReview,
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E293B))
            ) {
                Icon(
                    imageVector = Icons.Default.Policy,
                    contentDescription = "DRS",
                    tint = Color(0xFFA78BFA),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
