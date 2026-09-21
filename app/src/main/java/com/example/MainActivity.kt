package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SportsCricket
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.BallEventEntity
import com.example.data.model.DeviceRole
import com.example.data.model.MatchEntity
import com.example.data.ai.MatchSummaryResult
import com.example.data.audio.SidhuVoiceStyle
import com.example.data.network.NetworkStatus
import com.example.ui.components.AiMatchSummaryCard
import com.example.ui.components.AiMatchSummaryDialog
import com.example.ui.components.AiSettingsDialog
import com.example.ui.components.BallCommentaryList
import com.example.ui.components.CoinFlipperDialog
import com.example.ui.components.CreateMatchDialog
import com.example.ui.components.DetailedScorecardView
import com.example.ui.components.DrsBroadcastOverlay
import com.example.ui.components.DrsReviewScreen
import com.example.ui.components.GuideListDialog
import com.example.data.model.CricHeroesProfile
import com.example.ui.components.CricHeroesLoginScreen
import com.example.ui.components.CricHeroesProfileDialog
import com.example.ui.components.DirectPersonalMessagingDialog
import com.example.ui.components.HighlightsReelScreen
import com.example.ui.components.InAppUpdateDialog
import com.example.ui.components.InstagramMatchChatDialog
import com.example.ui.components.LiveCenterTab
import com.example.ui.components.LiveMatchBroadcastPlayer
import com.example.ui.components.LiveMatchesFeedScreen
import com.example.ui.components.MatchSwitcherBottomSheet
import com.example.ui.components.MessagesCenterDialog
import com.example.ui.components.NetworkStatusDialog
import com.example.ui.components.PlayerProfileCardDialog
import com.example.ui.components.PlayingSquadDialog
import com.example.ui.components.RoleAuthorizationDialog
import com.example.ui.components.RoleRequestsDialog
import com.example.ui.components.ScoreBanner
import com.example.ui.components.ScorerControllerSheet
import com.example.ui.components.ChangeBowlerDialog
import com.example.ui.components.NewBatsmanDialog
import com.example.ui.components.ChangeBatsmanDialog
import com.example.ui.components.SidhuAiCommentaryBar
import com.example.ui.components.StandingsAndLeaderboardScreen
import com.example.ui.components.TopBroadcastHeader
import com.example.ui.components.TournamentAnalyticsScreen
import com.example.ui.components.UmpireCameraScreen
import com.example.ui.components.ZeroDelayLiveRadar
import com.example.ui.components.WhatsAppMatchShareDialog
import com.example.ui.components.WagonWheelAndPitchMapDialog
import com.example.ui.components.BroadcastGraphicOverlay
import com.example.ui.theme.CricketGreen
import com.example.ui.theme.CricTrackTheme
import com.example.ui.theme.DrsOutRed
import com.example.ui.theme.HawkEyeCyan
import com.example.ui.theme.PitchCard
import com.example.ui.theme.PitchDark
import com.example.ui.theme.PitchSurface
import com.example.ui.theme.StadiumGold
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.AppScreenTab
import com.example.ui.viewmodel.CricketViewModel
import com.example.ui.viewmodel.LiveCenterSubTab

class MainActivity : ComponentActivity() {

    private val viewModel: CricketViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            CricTrackTheme(darkTheme = true) {
                CricketAppContent(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun CricketAppContent(viewModel: CricketViewModel) {
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val allMatches by viewModel.allMatches.collectAsStateWithLifecycle()
    val selectedMatchId by viewModel.selectedMatchId.collectAsStateWithLifecycle()
    val currentMatch by viewModel.currentMatch.collectAsStateWithLifecycle()
    val currentBallEvents by viewModel.currentBallEvents.collectAsStateWithLifecycle()
    val commentaryFilter by viewModel.commentaryFilter.collectAsStateWithLifecycle()
    val showScorerSheet by viewModel.showScorerSheet.collectAsStateWithLifecycle()
    val drsState by viewModel.drsState.collectAsStateWithLifecycle()
    val teamStandings by viewModel.teamStandings.collectAsStateWithLifecycle()
    val playerStats by viewModel.playerStats.collectAsStateWithLifecycle()
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val selectedClip by viewModel.selectedClip.collectAsStateWithLifecycle()
    val isVideoPlaying by viewModel.isVideoPlaying.collectAsStateWithLifecycle()
    val videoProgress by viewModel.videoProgress.collectAsStateWithLifecycle()
    val videoSpeed by viewModel.videoSpeed.collectAsStateWithLifecycle()
    val cameraAngle by viewModel.cameraAngle.collectAsStateWithLifecycle()
    val notifyWickets by viewModel.notifyWickets.collectAsStateWithLifecycle()
    val notifyBoundaries by viewModel.notifyBoundaries.collectAsStateWithLifecycle()
    val notifyMilestones by viewModel.notifyMilestones.collectAsStateWithLifecycle()
    val notifyDrs by viewModel.notifyDrs.collectAsStateWithLifecycle()
    val bannerAlert by viewModel.bannerAlert.collectAsStateWithLifecycle()

    // Multi-Phone Local Match Roles & Camera State
    val currentRole by viewModel.currentDeviceRole.collectAsStateWithLifecycle()
    val officialPin by viewModel.officialPin.collectAsStateWithLifecycle()
    val showRoleDialog by viewModel.showRoleDialog.collectAsStateWithLifecycle()
    val showCreateMatchDialog by viewModel.showCreateMatchDialog.collectAsStateWithLifecycle()
    var showResetConfirmDialog by remember { mutableStateOf(false) }
    val drsBroadcast by viewModel.drsBroadcast.collectAsStateWithLifecycle()
    val isStreamingPitchCam by viewModel.isStreamingPitchCam.collectAsStateWithLifecycle()
    val isStreamingSideCam by viewModel.isStreamingSideCam.collectAsStateWithLifecycle()
    val spectatorCamAngle by viewModel.spectatorCamAngle.collectAsStateWithLifecycle()
    val updateState by viewModel.updateState.collectAsStateWithLifecycle()
    val showUpdateDialog by viewModel.showUpdateDialog.collectAsStateWithLifecycle()
    val liveCenterSubTab by viewModel.liveCenterSubTab.collectAsStateWithLifecycle()
    val isVideoOverlayExpanded by viewModel.isVideoOverlayExpanded.collectAsStateWithLifecycle()
    val networkStatus by viewModel.networkStatus.collectAsStateWithLifecycle()
    val isForceLiteMode by viewModel.isForceLiteMode.collectAsStateWithLifecycle()
    val showNetworkDialog by viewModel.showNetworkDialog.collectAsStateWithLifecycle()
    val isSidhuCommentaryEnabled by viewModel.isSidhuCommentaryEnabled.collectAsStateWithLifecycle()
    val isSidhuSpeaking by viewModel.isSidhuSpeaking.collectAsStateWithLifecycle()
    val sidhuCurrentDialogue by viewModel.sidhuCurrentDialogue.collectAsStateWithLifecycle()
    val sidhuVoiceStyle by viewModel.sidhuVoiceStyle.collectAsStateWithLifecycle()
    val sidhuPitch by viewModel.sidhuPitch.collectAsStateWithLifecycle()
    val sidhuSpeed by viewModel.sidhuSpeed.collectAsStateWithLifecycle()
    val sidhuVoiceGender by viewModel.sidhuVoiceGender.collectAsStateWithLifecycle()
    val matchSummary by viewModel.matchSummary.collectAsStateWithLifecycle()
    val isGeneratingSummary by viewModel.isGeneratingSummary.collectAsStateWithLifecycle()
    val showSummaryDialog by viewModel.showSummaryDialog.collectAsStateWithLifecycle()
    val customApiKey by viewModel.customApiKey.collectAsStateWithLifecycle()
    val aiEngineMode by viewModel.aiEngineMode.collectAsStateWithLifecycle()
    val showAiSettingsDialog by viewModel.showAiSettingsDialog.collectAsStateWithLifecycle()

    // CricHeroes Profile, Chat, DMs, and Role Approval States
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val showProfileDialog by viewModel.showProfileDialog.collectAsStateWithLifecycle()
    val showLoginScreen by viewModel.showLoginScreen.collectAsStateWithLifecycle()
    val showChatDialog by viewModel.showChatDialog.collectAsStateWithLifecycle()
    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val showDmDialog by viewModel.showDmDialog.collectAsStateWithLifecycle()
    val personalMessages by viewModel.personalMessages.collectAsStateWithLifecycle()
    val communityPlayers by viewModel.communityPlayers.collectAsStateWithLifecycle()
    val activeDmRecipient by viewModel.activeDmRecipient.collectAsStateWithLifecycle()
    val roleRequests by viewModel.roleRequests.collectAsStateWithLifecycle()
    val showRoleRequestsDialog by viewModel.showRoleRequestsDialog.collectAsStateWithLifecycle()
    val showPlayingSquadDialog by viewModel.showPlayingSquadDialog.collectAsStateWithLifecycle()
    val showCoinFlipperDialog by viewModel.showCoinFlipperDialog.collectAsStateWithLifecycle()
    val viewingPlayerCard by viewModel.viewingPlayerCard.collectAsStateWithLifecycle()
    val showMessagesHub by viewModel.showMessagesHub.collectAsStateWithLifecycle()
    val messagesHubTab by viewModel.messagesHubTab.collectAsStateWithLifecycle()
    val showWhatsAppShareDialog by viewModel.showWhatsAppShareDialog.collectAsStateWithLifecycle()
    val showWagonWheelDialog by viewModel.showWagonWheelDialog.collectAsStateWithLifecycle()
    val activeBroadcastOverlay by viewModel.activeBroadcastOverlay.collectAsStateWithLifecycle()
    val showChangeBowlerDialog by viewModel.showChangeBowlerDialog.collectAsStateWithLifecycle()
    val showNewBatsmanDialog by viewModel.showNewBatsmanDialog.collectAsStateWithLifecycle()
    val pendingWicketType by viewModel.pendingWicketType.collectAsStateWithLifecycle()
    val showChangeBatsmanDialog by viewModel.showChangeBatsmanDialog.collectAsStateWithLifecycle()
    var showMatchSwitcherModal by remember { mutableStateOf(false) }
    var showGuideDialog by remember { mutableStateOf(false) }

    if (showLoginScreen) {
        CricHeroesLoginScreen(
            currentProfile = userProfile,
            onLoginSuccess = { viewModel.performLogin(it) },
            onContinueAsSpectator = { viewModel.setShowLoginScreen(false) },
            onCheckUsernameAvailable = { viewModel.isUsernameAvailable(it) }
        )
    } else {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
        containerColor = PitchDark,
        topBar = {
            TopBroadcastHeader(
                currentMatch = currentMatch,
                currentRole = currentRole,
                networkStatus = networkStatus,
                userProfile = userProfile,
                unreadMessagesCount = chatMessages.size.coerceAtMost(9),
                onOpenNetworkDialog = { viewModel.setShowNetworkDialog(true) },
                onOpenMessagesHub = { viewModel.openMessagesHub(0) },
                onOpenRoleDialog = { viewModel.setShowRoleDialog(true) },
                onOpenProfileDialog = { viewModel.setShowProfileDialog(true) },
                onOpenMatchSwitcher = { showMatchSwitcherModal = true },
                onShareWhatsApp = { viewModel.setShowWhatsAppShareDialog(true) },
                onOpenWagonWheel = { viewModel.setShowWagonWheelDialog(true) }
            )
        },
        bottomBar = {
            BottomBroadcastNavigation(
                currentTab = currentTab,
                currentRole = currentRole,
                onSelectTab = { viewModel.selectTab(it) }
            )
        },
        floatingActionButton = {
            if (currentTab == AppScreenTab.MATCHES_FEED) {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.setShowCreateMatchDialog(true) },
                    containerColor = StadiumGold,
                    contentColor = PitchDark,
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Create Match",
                            tint = PitchDark
                        )
                    },
                    text = {
                        Text(
                            text = "New Match",
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp,
                            color = PitchDark
                        )
                    }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Screen Switcher
            when (currentTab) {
                AppScreenTab.MATCHES_FEED -> {
                    LiveMatchesFeedScreen(
                        matches = allMatches,
                        selectedMatchId = selectedMatchId,
                        currentBallEvents = currentBallEvents,
                        isStreamingPitchCam = isStreamingPitchCam,
                        isStreamingSideCam = isStreamingSideCam,
                        isSidhuCommentaryEnabled = isSidhuCommentaryEnabled,
                        isSidhuSpeaking = isSidhuSpeaking,
                        sidhuCurrentDialogue = sidhuCurrentDialogue,
                        sidhuVoiceStyle = sidhuVoiceStyle,
                        onToggleSidhuCommentary = { viewModel.toggleSidhuCommentary(it) },
                        onSelectSidhuStyle = { viewModel.setSidhuVoiceStyle(it) },
                        onTestSidhuVoice = { viewModel.testSidhuCommentary() },
                        onSelectMatch = { viewModel.selectMatch(it) },
                        onWatchLiveVideoAndScore = { viewModel.openMatchWatchVideo(it) },
                        onOpenFullScorecard = { viewModel.openMatchScorecard(it) },
                        onCreateNewMatch = { viewModel.setShowCreateMatchDialog(true) },
                        onOpenAiSummary = {
                            viewModel.selectMatch(it)
                            viewModel.setShowSummaryDialog(true)
                        }
                    )
                }

                AppScreenTab.LIVE_CENTER -> {
                    LiveCenterTab(
                        match = currentMatch,
                        ballEvents = currentBallEvents,
                        commentaryFilter = commentaryFilter,
                        currentRole = currentRole,
                        spectatorCamAngle = spectatorCamAngle,
                        onSelectSpectatorAngle = { viewModel.setSpectatorCamAngle(it) },
                        liveCenterSubTab = liveCenterSubTab,
                        onSelectSubTab = { viewModel.selectLiveCenterSubTab(it) },
                        isVideoOverlayExpanded = isVideoOverlayExpanded,
                        onToggleVideoOverlay = { viewModel.toggleVideoOverlayExpanded() },
                        onSelectFilter = { viewModel.setCommentaryFilter(it) },
                        onOpenScorer = { viewModel.setScorerSheetVisible(true) },
                        onOpenDrsReview = {
                            viewModel.selectTab(AppScreenTab.DRS_SYSTEM)
                            viewModel.startDrsReview("LBW", currentMatch?.strikerName ?: "Batter", currentMatch?.bowlerName ?: "Bowler")
                        },
                        onOpenUmpireCamera = { viewModel.selectTab(AppScreenTab.CAMERA_UMPIRE) },
                        onOpenRoleDialog = { viewModel.setShowRoleDialog(true) },
                        onSwitchStriker = { viewModel.switchStrikers() },
                        onTriggerDelivery = {
                            viewModel.recordBall(
                                runs = 4,
                                isWicket = false,
                                shotAngle = 45f,
                                pitchZone = "Good Length"
                            )
                        },
                        isSidhuCommentaryEnabled = isSidhuCommentaryEnabled,
                        isSidhuSpeaking = isSidhuSpeaking,
                        sidhuCurrentDialogue = sidhuCurrentDialogue,
                        sidhuVoiceStyle = sidhuVoiceStyle,
                        onToggleSidhuCommentary = { viewModel.toggleSidhuCommentary(it) },
                        onSelectSidhuStyle = { viewModel.setSidhuVoiceStyle(it) },
                        onTestSidhuVoice = { viewModel.testSidhuCommentary() },
                        matchSummary = matchSummary,
                        onOpenSummaryDialog = { viewModel.setShowSummaryDialog(true) },
                        onQuickListenSummary = { viewModel.playSummaryAudio() },
                        onRecordRun = { runs -> viewModel.recordQuickRun(runs) },
                        onRecordWicket = { viewModel.recordQuickWicket() },
                        onRecordExtra = { extra -> viewModel.recordQuickExtra(extra) },
                        onUndoDelivery = { viewModel.undoLastDelivery() },
                        onOpenMessagesHub = { viewModel.openMessagesHub(0) },
                        onTogglePitchCam = { viewModel.togglePitchCamStreaming() },
                        onToggleSideCam = { viewModel.toggleSideCamStreaming() },
                        onTriggerAppeal = { appealType ->
                            viewModel.selectTab(AppScreenTab.DRS_SYSTEM)
                            viewModel.startDrsReview(appealType, currentMatch?.strikerName ?: "Batter", currentMatch?.bowlerName ?: "Bowler")
                        },
                        onChangeBowler = { viewModel.openChangeBowlerDialog() },
                        onChangeBatsman = { viewModel.openChangeBatsmanDialog() },
                        onOpenNewBatsmanDialog = { viewModel.openNewBatsmanDialog("Bowled") }
                    )
                }

                AppScreenTab.CAMERA_UMPIRE -> {
                    UmpireCameraScreen(
                        currentRole = currentRole,
                        match = currentMatch,
                        isStreamingPitchCam = isStreamingPitchCam,
                        isStreamingSideCam = isStreamingSideCam,
                        spectatorCamAngle = spectatorCamAngle,
                        onTogglePitchCam = { viewModel.togglePitchCamStreaming() },
                        onToggleSideCam = { viewModel.toggleSideCamStreaming() },
                        onSelectSpectatorAngle = { viewModel.setSpectatorCamAngle(it) },
                        onRequestDrsAppeal = { appealType ->
                            viewModel.selectTab(AppScreenTab.DRS_SYSTEM)
                            viewModel.startDrsReview(appealType, currentMatch?.strikerName ?: "Batter", currentMatch?.bowlerName ?: "Bowler")
                        },
                        onChangeRoleClick = { viewModel.setShowRoleDialog(true) }
                    )
                }

                AppScreenTab.DRS_SYSTEM -> {
                    DrsReviewScreen(
                        drsState = drsState,
                        currentRole = currentRole,
                        onStartReview = { appealType ->
                            viewModel.startDrsReview(
                                appealType = appealType,
                                batsman = currentMatch?.strikerName?.ifBlank { "Batter" } ?: "Batter",
                                bowler = currentMatch?.bowlerName?.ifBlank { "Bowler" } ?: "Bowler",
                                onFieldDecision = if (appealType == "LBW") "NOT OUT" else "OUT"
                            )
                        },
                        onManualParametersChanged = { p, i, w, dev, dist ->
                            viewModel.setDrsManualParameters(p, i, w, dev, dist)
                        },
                        onResetDrs = { viewModel.resetDrs() },
                        onThirdUmpireDecision = { decision, appealType, reason ->
                            viewModel.thirdUmpireDeclareDecision(decision, appealType, reason)
                        },
                        onOpenRoleDialog = { viewModel.setShowRoleDialog(true) },
                        onSelectCameraAngle = { angle -> viewModel.setDrsSelectedAngle(angle) },
                        onSetFrameIndex = { frame -> viewModel.setDrsFrameIndex(frame) }
                    )
                }

                AppScreenTab.HIGHLIGHTS -> {
                    HighlightsReelScreen(
                        clips = viewModel.highlightClips,
                        selectedClip = selectedClip,
                        isPlaying = isVideoPlaying,
                        progress = videoProgress,
                        videoSpeed = videoSpeed,
                        cameraAngle = cameraAngle,
                        onSelectClip = { viewModel.selectHighlightClip(it) },
                        onTogglePlay = { viewModel.toggleVideoPlay() },
                        onSetSpeed = { viewModel.setVideoSpeed(it) },
                        onSetCameraAngle = { viewModel.setCameraAngle(it) },
                        onSeek = { viewModel.seekVideo(it) }
                    )
                }

                AppScreenTab.ANALYTICS -> {
                    TournamentAnalyticsScreen(
                        ballEvents = currentBallEvents,
                        playerStats = playerStats
                    )
                }

                AppScreenTab.STANDINGS_LEADERBOARD -> {
                    StandingsAndLeaderboardScreen(
                        standings = teamStandings,
                        playerStats = playerStats,
                        notifications = notifications,
                        notifyWickets = notifyWickets,
                        notifyBoundaries = notifyBoundaries,
                        notifyMilestones = notifyMilestones,
                        notifyDrs = notifyDrs,
                        onToggleNotification = { viewModel.toggleNotification(it) },
                        onSendTestAlert = { viewModel.sendTestLiveAlert() },
                        onClearAllRecords = { viewModel.clearAllStandingsAndStats() }
                    )
                }
            }

            // In-App Notification Alert Banner
            AnimatedVisibility(
                visible = bannerAlert != null,
                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                bannerAlert?.let { msg ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("in_app_banner_alert"),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = Brush.horizontalGradient(listOf(CricketGreen, HawkEyeCyan)),
                            width = 1.2.dp
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(CricketGreen.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FlashOn,
                                        contentDescription = "Alert",
                                        tint = CricketGreen,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = msg,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }
                            IconButton(
                                onClick = { viewModel.dismissBanner() },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Scorer Bottom Sheet (Authorized for Phone 3: Official Scorer)
            if (showScorerSheet) {
                ScorerControllerSheet(
                    onDismiss = { viewModel.setScorerSheetVisible(false) },
                    onRecordBall = { runs, isWkt, wType, extra, angle, zone ->
                        viewModel.recordBall(
                            runs = runs,
                            isWicket = isWkt,
                            wicketType = wType,
                            extraType = extra,
                            shotAngle = angle,
                            pitchZone = zone
                        )
                    },
                    onUndoLastDelivery = { viewModel.undoLastDelivery() },
                    onSwitchStriker = { viewModel.switchStrikers() },
                    onRequestDrs = {
                        viewModel.selectTab(AppScreenTab.DRS_SYSTEM)
                        viewModel.startDrsReview("LBW", currentMatch?.strikerName ?: "Batter", currentMatch?.bowlerName ?: "Bowler")
                    },
                    currentRole = currentRole,
                    onOpenRoleDialog = { viewModel.setShowRoleDialog(true) },
                    strikerName = currentMatch?.strikerName ?: "Striker",
                    bowlerName = currentMatch?.bowlerName ?: "Bowler",
                    onChangeBowler = { viewModel.openChangeBowlerDialog() },
                    onChangeBatsman = { viewModel.openChangeBatsmanDialog() },
                    onOpenNewBatsmanDialog = { wType -> viewModel.openNewBatsmanDialog(wType) }
                )
            }

            // Change Bowler Dialog
            if (showChangeBowlerDialog && currentMatch != null) {
                ChangeBowlerDialog(
                    match = currentMatch!!,
                    recentDeliveries = currentBallEvents,
                    onDismiss = { viewModel.closeChangeBowlerDialog() },
                    onConfirmNewBowler = { newBowler ->
                        viewModel.changeBowler(newBowler)
                    }
                )
            }

            // New Batsman / Wicket Fall Dialog
            if (showNewBatsmanDialog && currentMatch != null) {
                NewBatsmanDialog(
                    match = currentMatch!!,
                    initialWicketType = pendingWicketType,
                    onDismiss = { viewModel.closeNewBatsmanDialog() },
                    onConfirmDismissalAndNewBatsman = { dismissed, newBatsman, wType, onStrike ->
                        viewModel.recordWicketWithNewBatsman(
                            dismissedBatsman = dismissed,
                            newBatsmanName = newBatsman,
                            wicketType = wType,
                            newBatsmanOnStrike = onStrike
                        )
                    }
                )
            }

            // Change Batsman Dialog
            if (showChangeBatsmanDialog && currentMatch != null) {
                ChangeBatsmanDialog(
                    match = currentMatch!!,
                    onDismiss = { viewModel.closeChangeBatsmanDialog() },
                    onConfirmChange = { isStriker, newName ->
                        viewModel.changeBatsman(isStriker, newName)
                    }
                )
            }

            // Role Authorization Dialog
            if (showRoleDialog) {
                RoleAuthorizationDialog(
                    currentRole = currentRole,
                    officialPin = officialPin,
                    pendingRequestsCount = roleRequests.count { it.status == "PENDING" },
                    onDismiss = { viewModel.setShowRoleDialog(false) },
                    onSelectRole = { role, pin ->
                        viewModel.verifyPinAndSetRole(role, pin)
                    },
                    onRequestRole = { role, reason ->
                        viewModel.submitRoleRequest(role, reason)
                    },
                    onOpenAdminRequests = {
                        viewModel.setShowRoleDialog(false)
                        viewModel.setShowRoleRequestsDialog(true)
                    }
                )
            }

            // Admin Role Requests Approval Dialog
            if (showRoleRequestsDialog) {
                RoleRequestsDialog(
                    requests = roleRequests,
                    onDismiss = { viewModel.setShowRoleRequestsDialog(false) },
                    onApprove = { viewModel.approveRoleRequest(it) },
                    onDeny = { viewModel.denyRoleRequest(it) }
                )
            }

            // CricHeroes Pro Player Profile Pass Dialog
            if (showProfileDialog) {
                CricHeroesProfileDialog(
                    initialProfile = userProfile,
                    onDismiss = { viewModel.setShowProfileDialog(false) },
                    onSaveProfile = { viewModel.saveUserProfile(it) },
                    onCheckUsernameAvailable = { viewModel.isUsernameAvailable(it) },
                    onLogout = { viewModel.logout() }
                )
            }

            // Unified Commercial-Grade Messages Center (Match Live Room + Personal DMs)
            if (showMessagesHub || showChatDialog || showDmDialog) {
                val initialTab = if (showDmDialog) 1 else messagesHubTab
                MessagesCenterDialog(
                    initialTab = initialTab,
                    matchTitle = "${currentMatch?.teamA ?: "Team A"} vs ${currentMatch?.teamB ?: "Team B"}",
                    matchMessages = chatMessages,
                    onSendMatchMessage = { viewModel.sendChatMessage(it) },
                    onSendMatchReaction = { viewModel.sendChatReaction(it) },
                    currentUser = userProfile,
                    communityPlayers = communityPlayers,
                    personalMessages = personalMessages,
                    activeRecipient = activeDmRecipient,
                    onSelectRecipient = { viewModel.openDirectMessageWith(it) },
                    onCloseDirectChat = { viewModel.closeDirectMessageChat() },
                    onSendDirectMessage = { recipient, text -> viewModel.sendDirectMessage(recipient, text) },
                    onRefreshUsers = { viewModel.syncCloudUsers() },
                    onDismiss = {
                        viewModel.closeMessagesHub()
                        viewModel.setShowChatDialog(false)
                        viewModel.setShowDmDialog(false)
                    }
                )
            }

            // Compact Match Switcher & Unified Management Sheet
            if (showMatchSwitcherModal) {
                MatchSwitcherBottomSheet(
                    matches = allMatches,
                    selectedMatchId = selectedMatchId,
                    onSelectMatch = { viewModel.selectMatch(it) },
                    onOpenToss = { viewModel.setShowCoinFlipperDialog(true) },
                    onOpenSquad = { viewModel.setShowPlayingSquadDialog(true) },
                    onOpenResetDialog = { showResetConfirmDialog = true },
                    onOpenNetworkHub = { viewModel.setShowNetworkDialog(true) },
                    onOpenGuide = { showGuideDialog = true },
                    onOpenCreateMatch = { viewModel.setShowCreateMatchDialog(true) },
                    onOpenLogin = { viewModel.setShowLoginScreen(true) },
                    onOpenAiSettings = { viewModel.setShowAiSettingsDialog(true) },
                    onDismiss = { showMatchSwitcherModal = false }
                )
            }

            // Create Local Cricket Match Dialog
            if (showCreateMatchDialog) {
                CreateMatchDialog(
                    onDismiss = { viewModel.setShowCreateMatchDialog(false) },
                    onCreateMatch = { name, teamA, teamB, overs, striker, nonStriker, bowler, venue, pin ->
                        viewModel.resetAllAndStartFresh(
                            name = name,
                            teamA = teamA,
                            teamB = teamB,
                            overs = overs,
                            striker = striker,
                            nonStriker = nonStriker,
                            bowler = bowler,
                            venue = venue,
                            pin = pin
                        )
                    }
                )
            }

            // Playing Squad & Active Players Dialog
            if (showPlayingSquadDialog && currentMatch != null) {
                PlayingSquadDialog(
                    match = currentMatch!!,
                    registeredPlayers = communityPlayers,
                    onDismiss = { viewModel.setShowPlayingSquadDialog(false) },
                    onSaveSquad = { striker, nonStriker, bowler, teamA, teamB ->
                        viewModel.updateMatchSquad(striker, nonStriker, bowler, teamA, teamB)
                    },
                    onViewPlayerProfile = { playerProfile ->
                        viewModel.openPlayerProfileCard(playerProfile)
                    }
                )
            }

            // 100% Cheat-Proof Cryptographic Coin Flipper (Toss) Dialog
            if (showCoinFlipperDialog && currentMatch != null) {
                CoinFlipperDialog(
                    teamAName = currentMatch!!.teamA,
                    teamBName = currentMatch!!.teamB,
                    onDismiss = { viewModel.setShowCoinFlipperDialog(false) },
                    onTossDecided = { winnerTeam, electedChoice ->
                        // Toss recorded
                    }
                )
            }

            // Player Profile Identity Card Dialog
            if (viewingPlayerCard != null) {
                PlayerProfileCardDialog(
                    profile = viewingPlayerCard!!,
                    onDismiss = { viewModel.closePlayerProfileCard() },
                    onOpenDmWithPlayer = { player ->
                        viewModel.closePlayerProfileCard()
                        viewModel.openDirectMessageWith(player)
                        viewModel.setShowDmDialog(true)
                    }
                )
            }

            // Quick App Feature Guide Dialog
            if (showGuideDialog) {
                GuideListDialog(
                    onDismiss = { showGuideDialog = false }
                )
            }

            // Reset Confirmation Dialog
            if (showResetConfirmDialog) {
                AlertDialog(
                    onDismissRequest = { showResetConfirmDialog = false },
                    containerColor = PitchDark,
                    shape = RoundedCornerShape(16.dp),
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = CricketGreen)
                            Text("Reset Match to 0/0?", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                    },
                    text = {
                        Text(
                            "Kya aap saare purane demo records saaf karke 0 runs, 0 wickets aur 0.0 overs se bilkul naya live match shuru karna chahte hain?",
                            color = TextSecondary,
                            fontSize = 14.sp
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.resetCurrentMatchToZero()
                                showResetConfirmDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CricketGreen),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Haan, Reset 0-0 Karein", color = PitchDark, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showResetConfirmDialog = false }) {
                            Text("Cancel", color = TextSecondary)
                        }
                    }
                )
            }

            // In-App Auto Update & Ground Share Dialog
            if (showUpdateDialog) {
                InAppUpdateDialog(
                    updateState = updateState,
                    onDismiss = { viewModel.setShowUpdateDialog(false) },
                    onCheckUpdate = { viewModel.checkForUpdates(it) },
                    onDownloadAndInstall = { viewModel.downloadAndInstallApk(it) },
                    onInstallDownloaded = { viewModel.installDownloadedApk() },
                    onShareApkDirectly = { viewModel.shareInstalledApkDirectly() }
                )
            }

            // Hybrid Connectivity (Online / Slow Net / Offline Ground) Status Dialog
            if (showNetworkDialog) {
                NetworkStatusDialog(
                    currentStatus = networkStatus,
                    isForceLiteMode = isForceLiteMode,
                    onToggleForceLiteMode = { viewModel.setForceLiteMode(it) },
                    onDismiss = { viewModel.setShowNetworkDialog(false) }
                )
            }

            // AI Match Summary & Wrap-up Dialog (Sidhu Paaji Voice & Text)
            if (showSummaryDialog) {
                AiMatchSummaryDialog(
                    summary = matchSummary,
                    isLoading = isGeneratingSummary,
                    isSpeaking = isSidhuSpeaking,
                    sidhuPitch = sidhuPitch,
                    sidhuSpeed = sidhuSpeed,
                    sidhuVoiceGender = sidhuVoiceGender,
                    onPlayAudioSummary = { viewModel.playSummaryAudio() },
                    onStopAudioSummary = { viewModel.stopSummaryAudio() },
                    onRegenerate = { viewModel.generateMatchSummary() },
                    onPitchChange = { viewModel.setSidhuPitch(it) },
                    onSpeedChange = { viewModel.setSidhuSpeed(it) },
                    onGenderChange = { viewModel.setSidhuVoiceGender(it) },
                    onResetVoice = { viewModel.resetSidhuVoiceDefaults() },
                    onTestVoice = { viewModel.testSidhuVoice() },
                    onDismiss = { viewModel.setShowSummaryDialog(false) },
                    onOpenSettings = { viewModel.setShowAiSettingsDialog(true) }
                )
            }

            // AI & Gemini Mode / Key Settings Dialog (Auto for Viewers / Custom Key for Organizer)
            if (showAiSettingsDialog) {
                AiSettingsDialog(
                    currentMode = aiEngineMode,
                    savedApiKey = customApiKey,
                    sidhuPitch = sidhuPitch,
                    sidhuSpeed = sidhuSpeed,
                    sidhuVoiceGender = sidhuVoiceGender,
                    onSelectMode = { viewModel.setAiEngineMode(it) },
                    onSaveApiKey = { viewModel.saveCustomApiKey(it) },
                    onClearApiKey = { viewModel.clearCustomApiKey() },
                    onTestApiKey = { key, callback -> viewModel.testCustomApiKey(key, callback) },
                    onPitchChange = { viewModel.setSidhuPitch(it) },
                    onSpeedChange = { viewModel.setSidhuSpeed(it) },
                    onGenderChange = { viewModel.setSidhuVoiceGender(it) },
                    onResetVoice = { viewModel.resetSidhuVoiceDefaults() },
                    onTestVoice = { viewModel.testSidhuVoice() },
                    onDismiss = { viewModel.setShowAiSettingsDialog(false) }
                )
            }

            // DRS Live Big Screen Decision Broadcast Overlay
            DrsBroadcastOverlay(
                alert = drsBroadcast,
                onDismiss = { viewModel.dismissDrsBroadcast() }
            )

            // WhatsApp Match Summary Card & Poster Dialog
            if (showWhatsAppShareDialog && currentMatch != null) {
                WhatsAppMatchShareDialog(
                    match = currentMatch!!,
                    onDismiss = { viewModel.setShowWhatsAppShareDialog(false) }
                )
            }

            // Wagon Wheel & Ball Pitch Map Visualizer Dialog
            if (showWagonWheelDialog && currentMatch != null) {
                WagonWheelAndPitchMapDialog(
                    match = currentMatch!!,
                    deliveries = currentBallEvents,
                    onDismiss = { viewModel.setShowWagonWheelDialog(false) }
                )
            }

            // Hotstar-Style Live Broadcast Graphic Overlay (TV Lower-Thirds)
            if (activeBroadcastOverlay != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 72.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    BroadcastGraphicOverlay(
                        event = activeBroadcastOverlay!!,
                        onDismiss = { viewModel.dismissBroadcastOverlay() }
                    )
                }
            }
        }
    }
}
}


@Composable
fun BottomBroadcastNavigation(
    currentTab: AppScreenTab,
    currentRole: DeviceRole,
    onSelectTab: (AppScreenTab) -> Unit
) {
    NavigationBar(
        containerColor = PitchSurface,
        contentColor = CricketGreen,
        tonalElevation = 8.dp,
        modifier = Modifier
            .windowInsetsPadding(WindowInsets.navigationBars)
            .border(0.5.dp, Color(0xFF1E293B))
            .testTag("bottom_broadcast_nav")
    ) {
        NavigationBarItem(
            selected = currentTab == AppScreenTab.MATCHES_FEED,
            onClick = { onSelectTab(AppScreenTab.MATCHES_FEED) },
            icon = {
                Icon(
                    imageVector = Icons.Default.SportsCricket,
                    contentDescription = "Matches Dashboard"
                )
            },
            label = { Text("Matches", fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = PitchDark,
                selectedTextColor = CricketGreen,
                indicatorColor = CricketGreen,
                unselectedIconColor = TextSecondary,
                unselectedTextColor = TextSecondary
            )
        )

        NavigationBarItem(
            selected = currentTab == AppScreenTab.LIVE_CENTER,
            onClick = { onSelectTab(AppScreenTab.LIVE_CENTER) },
            icon = {
                Icon(
                    imageVector = Icons.Default.Videocam,
                    contentDescription = "Watch Live Match"
                )
            },
            label = { Text("Live", fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = PitchDark,
                selectedTextColor = DrsOutRed,
                indicatorColor = DrsOutRed,
                unselectedIconColor = TextSecondary,
                unselectedTextColor = TextSecondary
            )
        )

        NavigationBarItem(
            selected = currentTab == AppScreenTab.CAMERA_UMPIRE,
            onClick = { onSelectTab(AppScreenTab.CAMERA_UMPIRE) },
            icon = {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Umpire Camera"
                )
            },
            label = { Text("Camera", fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = PitchDark,
                selectedTextColor = CricketGreen,
                indicatorColor = CricketGreen,
                unselectedIconColor = TextSecondary,
                unselectedTextColor = TextSecondary
            )
        )

        NavigationBarItem(
            selected = currentTab == AppScreenTab.DRS_SYSTEM,
            onClick = { onSelectTab(AppScreenTab.DRS_SYSTEM) },
            icon = {
                Icon(
                    imageVector = Icons.Default.Policy,
                    contentDescription = "DRS Review"
                )
            },
            label = { Text("DRS", fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = PitchDark,
                selectedTextColor = HawkEyeCyan,
                indicatorColor = HawkEyeCyan,
                unselectedIconColor = TextSecondary,
                unselectedTextColor = TextSecondary
            )
        )

        NavigationBarItem(
            selected = currentTab == AppScreenTab.STANDINGS_LEADERBOARD,
            onClick = { onSelectTab(AppScreenTab.STANDINGS_LEADERBOARD) },
            icon = {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = "Points Table & Leaderboard"
                )
            },
            label = { Text("Table", fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = PitchDark,
                selectedTextColor = StadiumGold,
                indicatorColor = StadiumGold,
                unselectedIconColor = TextSecondary,
                unselectedTextColor = TextSecondary
            )
        )

        NavigationBarItem(
            selected = currentTab == AppScreenTab.ANALYTICS,
            onClick = { onSelectTab(AppScreenTab.ANALYTICS) },
            icon = {
                Icon(
                    imageVector = Icons.Default.Analytics,
                    contentDescription = "Analytics"
                )
            },
            label = { Text("Stats", fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = PitchDark,
                selectedTextColor = Color(0xFFA78BFA),
                indicatorColor = Color(0xFFA78BFA),
                unselectedIconColor = TextSecondary,
                unselectedTextColor = TextSecondary
            )
        )
    }
}
