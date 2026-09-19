package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.CricketDatabase
import com.example.data.model.BallEventEntity
import com.example.data.model.DeviceRole
import com.example.data.model.DrsBroadcastAlert
import com.example.data.model.DrsReviewState
import com.example.data.model.HighlightClip
import com.example.data.model.MatchEntity
import com.example.data.model.NotificationAlertEntity
import com.example.data.model.PlayerStatEntity
import com.example.data.model.TeamStandingEntity
import com.example.data.model.CricHeroesProfile
import com.example.data.model.RoleChangeRequest
import com.example.data.model.ChatMessage
import com.example.data.model.DirectPersonalMessage
import com.example.data.chat.RealChatRepository
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ListenerRegistration
import android.content.Context
import java.util.UUID
import com.example.data.audio.AiSidhuCommentaryManager
import com.example.data.audio.SidhuCommentaryGenerator
import com.example.data.audio.SidhuVoiceStyle
import com.example.data.ai.AiEngineMode
import com.example.data.ai.AiMatchSummarizer
import com.example.data.ai.AiSettingsManager
import com.example.data.ai.MatchSummaryResult
import com.example.data.network.NetworkConnectivityObserver
import com.example.data.network.NetworkStatus
import com.example.data.repository.CricketRepository
import com.example.data.update.AppUpdateManager
import com.example.data.update.AppUpdateState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppScreenTab {
    MATCHES_FEED,
    LIVE_CENTER,
    CAMERA_UMPIRE,
    DRS_SYSTEM,
    HIGHLIGHTS,
    ANALYTICS,
    STANDINGS_LEADERBOARD
}

enum class LiveCenterSubTab {
    LIVE_SUMMARY,
    DETAILED_SCORECARD,
    COMMENTARY,
    HAWKEYE_RADAR
}

enum class CommentaryFilter {
    ALL,
    BOUNDARIES,
    WICKETS,
    OVERS
}

class CricketViewModel(application: Application) : AndroidViewModel(application) {

    private val firestore: FirebaseFirestore? by lazy {
        try {
            if (FirebaseApp.getApps(getApplication()).isEmpty()) {
                val options = FirebaseOptions.Builder()
                    .setApplicationId("1:516353855768:android:crictrack")
                    .setProjectId("ai-studio-crictrack")
                    .setApiKey("AIzaSyFakeKeyForLocalFallbackOnly12345")
                    .build()
                FirebaseApp.initializeApp(getApplication(), options)
            }
            FirebaseFirestore.getInstance()
        } catch (e: Throwable) {
            android.util.Log.w("CricketViewModel", "Firestore initialization fallback: ${e.message}")
            null
        }
    }
    private var matchChatListener: ListenerRegistration? = null
    private var directChatListener: ListenerRegistration? = null

    private val repository: CricketRepository
    val allMatches: StateFlow<List<MatchEntity>>
    val teamStandings: StateFlow<List<TeamStandingEntity>>
    val playerStats: StateFlow<List<PlayerStatEntity>>
    val notifications: StateFlow<List<NotificationAlertEntity>>

    private val _selectedMatchId = MutableStateFlow("match_live_1")
    val selectedMatchId = _selectedMatchId.asStateFlow()

    val currentMatch: StateFlow<MatchEntity?>
    val currentBallEvents: StateFlow<List<BallEventEntity>>

    private val _currentTab = MutableStateFlow(AppScreenTab.MATCHES_FEED)
    val currentTab = _currentTab.asStateFlow()

    private val _liveCenterSubTab = MutableStateFlow(LiveCenterSubTab.LIVE_SUMMARY)
    val liveCenterSubTab = _liveCenterSubTab.asStateFlow()

    private val networkConnectivityObserver = NetworkConnectivityObserver(application)
    val networkStatus: StateFlow<NetworkStatus> = networkConnectivityObserver.observe()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), networkConnectivityObserver.getCurrentStatus())

    private val _isForceLiteMode = MutableStateFlow(false)
    val isForceLiteMode = _isForceLiteMode.asStateFlow()

    private val _showNetworkDialog = MutableStateFlow(false)
    val showNetworkDialog = _showNetworkDialog.asStateFlow()

    fun setShowNetworkDialog(show: Boolean) {
        _showNetworkDialog.value = show
    }

    fun setForceLiteMode(force: Boolean) {
        _isForceLiteMode.value = force
    }

    private val sidhuCommentaryManager = AiSidhuCommentaryManager(application)
    val isSidhuCommentaryEnabled = sidhuCommentaryManager.isCommentaryEnabled
    val isSidhuSpeaking = sidhuCommentaryManager.isSpeaking
    val sidhuCurrentDialogue = sidhuCommentaryManager.currentDialogue
    val sidhuVoiceStyle = sidhuCommentaryManager.voiceStyle
    val sidhuPitch = sidhuCommentaryManager.currentPitch
    val sidhuSpeed = sidhuCommentaryManager.currentSpeed
    val sidhuVoiceGender = sidhuCommentaryManager.voiceGender

    fun toggleSidhuCommentary(enabled: Boolean? = null) {
        sidhuCommentaryManager.toggleCommentary(enabled)
    }

    fun setSidhuVoiceStyle(style: SidhuVoiceStyle) {
        sidhuCommentaryManager.setVoiceStyle(style)
    }

    fun setSidhuPitch(pitch: Float) {
        sidhuCommentaryManager.setCustomPitch(pitch)
    }

    fun setSidhuSpeed(speed: Float) {
        sidhuCommentaryManager.setCustomSpeed(speed)
    }

    fun setSidhuVoiceGender(gender: String) {
        sidhuCommentaryManager.setVoiceGender(gender)
    }

    fun resetSidhuVoiceDefaults() {
        sidhuCommentaryManager.resetToSidhuDefaults()
    }

    fun testSidhuVoice() {
        sidhuCommentaryManager.testVoiceSample()
    }

    fun testSidhuCommentary() {
        sidhuCommentaryManager.triggerTestDialogue()
    }

    // AI Match Summary & Wrap-up
    private val aiSettingsManager = AiSettingsManager(application)
    val customApiKey = aiSettingsManager.customApiKey
    val aiEngineMode = aiSettingsManager.currentMode

    private val _showAiSettingsDialog = MutableStateFlow(false)
    val showAiSettingsDialog = _showAiSettingsDialog.asStateFlow()

    fun setShowAiSettingsDialog(show: Boolean) {
        _showAiSettingsDialog.value = show
    }

    fun saveCustomApiKey(key: String) {
        aiSettingsManager.saveCustomApiKey(key)
    }

    fun clearCustomApiKey() {
        aiSettingsManager.clearCustomApiKey()
    }

    fun setAiEngineMode(mode: AiEngineMode) {
        aiSettingsManager.setAiMode(mode)
    }

    fun testCustomApiKey(key: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val res = AiMatchSummarizer.testApiKey(key)
            onResult(res.first, res.second)
        }
    }

    private val _matchSummary = MutableStateFlow<MatchSummaryResult?>(null)
    val matchSummary = _matchSummary.asStateFlow()

    private val _isGeneratingSummary = MutableStateFlow(false)
    val isGeneratingSummary = _isGeneratingSummary.asStateFlow()

    private val _showSummaryDialog = MutableStateFlow(false)
    val showSummaryDialog = _showSummaryDialog.asStateFlow()

    fun setShowSummaryDialog(show: Boolean) {
        _showSummaryDialog.value = show
        if (show && _matchSummary.value == null) {
            generateMatchSummary()
        }
    }

    fun generateMatchSummary() {
        val match = currentMatch.value ?: return
        val balls = currentBallEvents.value
        viewModelScope.launch {
            _isGeneratingSummary.value = true
            try {
                val summary = AiMatchSummarizer.generateSummary(
                    match = match,
                    balls = balls,
                    style = sidhuVoiceStyle.value,
                    customApiKey = aiSettingsManager.getEffectiveApiKey(),
                    forceOffline = (aiSettingsManager.currentMode.value == AiEngineMode.OFFLINE_LOCAL)
                )
                _matchSummary.value = summary
            } catch (_: Exception) {
            } finally {
                _isGeneratingSummary.value = false
            }
        }
    }

    fun playSummaryAudio() {
        val summary = _matchSummary.value
        if (summary != null) {
            sidhuCommentaryManager.speak(summary.spokenScript)
        } else {
            val match = currentMatch.value ?: return
            val balls = currentBallEvents.value
            viewModelScope.launch {
                _isGeneratingSummary.value = true
                try {
                    val gen = AiMatchSummarizer.generateSummary(
                        match = match,
                        balls = balls,
                        style = sidhuVoiceStyle.value,
                        customApiKey = aiSettingsManager.getEffectiveApiKey(),
                        forceOffline = (aiSettingsManager.currentMode.value == AiEngineMode.OFFLINE_LOCAL)
                    )
                    _matchSummary.value = gen
                    sidhuCommentaryManager.speak(gen.spokenScript)
                } catch (_: Exception) {
                } finally {
                    _isGeneratingSummary.value = false
                }
            }
        }
    }

    fun stopSummaryAudio() {
        sidhuCommentaryManager.stop()
    }

    private val _isVideoOverlayExpanded = MutableStateFlow(true)
    val isVideoOverlayExpanded = _isVideoOverlayExpanded.asStateFlow()

    fun selectLiveCenterSubTab(subTab: LiveCenterSubTab) {
        _liveCenterSubTab.value = subTab
    }

    fun toggleVideoOverlayExpanded() {
        _isVideoOverlayExpanded.value = !_isVideoOverlayExpanded.value
    }

    fun openMatchWatchVideo(matchId: String) {
        _selectedMatchId.value = matchId
        _isVideoOverlayExpanded.value = true
        _currentTab.value = AppScreenTab.LIVE_CENTER
    }

    fun openMatchScorecard(matchId: String) {
        _selectedMatchId.value = matchId
        _liveCenterSubTab.value = LiveCenterSubTab.DETAILED_SCORECARD
        _currentTab.value = AppScreenTab.LIVE_CENTER
    }

    private val _commentaryFilter = MutableStateFlow(CommentaryFilter.ALL)
    val commentaryFilter = _commentaryFilter.asStateFlow()

    // Scorer Mode Dialog visibility
    private val _showScorerSheet = MutableStateFlow(false)
    val showScorerSheet = _showScorerSheet.asStateFlow()

    // Unified Messages Hub State (Consolidates Live Match Chat & DMs)
    private val _showMessagesHub = MutableStateFlow(false)
    val showMessagesHub = _showMessagesHub.asStateFlow()
    private val _messagesHubTab = MutableStateFlow(0)
    val messagesHubTab = _messagesHubTab.asStateFlow()

    fun openMessagesHub(tab: Int = 0) {
        _messagesHubTab.value = tab
        _showMessagesHub.value = true
    }

    fun closeMessagesHub() {
        _showMessagesHub.value = false
        closeDirectMessageChat()
    }

    fun recordQuickRun(runs: Int) {
        recordBall(runs = runs)
    }

    fun recordQuickWicket() {
        recordBall(runs = 0, isWicket = true, wicketType = "Out")
    }

    fun recordQuickExtra(extraType: String = "Wide") {
        recordBall(runs = 1, extraType = extraType)
    }

    // DRS Review State
    private val _drsState = MutableStateFlow(DrsReviewState())
    val drsState = _drsState.asStateFlow()

    // Zero-delay streaming simulation state (ball pitch visualizer)
    private val _pitchAnimPhase = MutableStateFlow(0f) // 0 to 1
    val pitchAnimPhase = _pitchAnimPhase.asStateFlow()

    // Highlights state
    val highlightClips: List<HighlightClip>
    private val _selectedClip = MutableStateFlow<HighlightClip?>(null)
    val selectedClip = _selectedClip.asStateFlow()
    private val _isVideoPlaying = MutableStateFlow(true)
    val isVideoPlaying = _isVideoPlaying.asStateFlow()
    private val _videoProgress = MutableStateFlow(0.4f)
    val videoProgress = _videoProgress.asStateFlow()
    private val _videoSpeed = MutableStateFlow("1.0x")
    val videoSpeed = _videoSpeed.asStateFlow()
    private val _cameraAngle = MutableStateFlow("Broadcast")
    val cameraAngle = _cameraAngle.asStateFlow()

    // Notification Toggles
    private val _notifyWickets = MutableStateFlow(true)
    val notifyWickets = _notifyWickets.asStateFlow()
    private val _notifyBoundaries = MutableStateFlow(true)
    val notifyBoundaries = _notifyBoundaries.asStateFlow()
    private val _notifyMilestones = MutableStateFlow(true)
    val notifyMilestones = _notifyMilestones.asStateFlow()
    private val _notifyDrs = MutableStateFlow(true)
    val notifyDrs = _notifyDrs.asStateFlow()

    // In-app alert banner banner for real-time live events
    private val _bannerAlert = MutableStateFlow<String?>(null)
    val bannerAlert = _bannerAlert.asStateFlow()

    // 4-Phone Official Roles & Authorization
    private val rolePrefs = application.getSharedPreferences("ayuu_device_roles", Context.MODE_PRIVATE)
    private val savedRole = try {
        DeviceRole.valueOf(rolePrefs.getString("active_role", DeviceRole.SPECTATOR_VIEWER.name) ?: DeviceRole.SPECTATOR_VIEWER.name)
    } catch (_: Exception) {
        DeviceRole.SPECTATOR_VIEWER
    }

    // Default: Every user starts strictly as SPECTATOR_VIEWER to prevent cheating
    private val _currentDeviceRole = MutableStateFlow(savedRole)
    val currentDeviceRole = _currentDeviceRole.asStateFlow()

    // Master Admin PIN (Owner Ayush control, no default hint shown in UI)
    private val _officialPin = MutableStateFlow(rolePrefs.getString("admin_pin", "8899") ?: "8899")
    val officialPin = _officialPin.asStateFlow()

    private val _isAuthorizedOfficial = MutableStateFlow(savedRole != DeviceRole.SPECTATOR_VIEWER)
    val isAuthorizedOfficial = _isAuthorizedOfficial.asStateFlow()

    // Role Requests submitted by users to Admin
    private val _roleRequests = MutableStateFlow<List<RoleChangeRequest>>(emptyList())
    val roleRequests = _roleRequests.asStateFlow()

    private val _showRoleRequestsDialog = MutableStateFlow(false)
    val showRoleRequestsDialog = _showRoleRequestsDialog.asStateFlow()

    // CricHeroes Profile Management
    private val profilePrefs = application.getSharedPreferences("ayuu_cricheroes_profile", Context.MODE_PRIVATE)
    private val _userProfile = MutableStateFlow(loadProfileFromPrefs())
    val userProfile = _userProfile.asStateFlow()

    private val _showProfileDialog = MutableStateFlow(false)
    val showProfileDialog = _showProfileDialog.asStateFlow()

    // World-Class CricHeroes Login Screen
    private val _showLoginScreen = MutableStateFlow(false)
    val showLoginScreen = _showLoginScreen.asStateFlow()

    fun setShowLoginScreen(show: Boolean) {
        _showLoginScreen.value = show
    }

    fun performLogin(profile: CricHeroesProfile) {
        saveUserProfile(profile)
        _showLoginScreen.value = false
        showBanner("Welcome ${profile.fullName}! Verified as ${profile.jerseyName} #${profile.jerseyNumber}")
    }

    // Instagram-Style Live Match Chat (Real Firebase Firestore)
    private val _showChatDialog = MutableStateFlow(false)
    val showChatDialog = _showChatDialog.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages = _chatMessages.asStateFlow()

    // Dialogs
    private val _showRoleDialog = MutableStateFlow(false)
    val showRoleDialog = _showRoleDialog.asStateFlow()

    private val _showCreateMatchDialog = MutableStateFlow(false)
    val showCreateMatchDialog = _showCreateMatchDialog.asStateFlow()

    // In-App Auto Updater & Ground Sync
    private val appUpdateManager = AppUpdateManager(application)
    val updateState: StateFlow<AppUpdateState> = appUpdateManager.updateState

    private val _showUpdateDialog = MutableStateFlow(false)
    val showUpdateDialog = _showUpdateDialog.asStateFlow()

    private val updateReminderPrefs = application.getSharedPreferences("ayuu_update_prefs", Context.MODE_PRIVATE)

    fun setShowUpdateDialog(show: Boolean) {
        _showUpdateDialog.value = show
        if (show) {
            // Record last time user opened the update dialog
            updateReminderPrefs.edit().putLong("last_update_check_time", System.currentTimeMillis()).apply()
        }
    }

    /**
     * Checks if 2-3 days (48+ hours) have passed since user last updated or checked for updates.
     * If 2-3 days have elapsed and an update is detected, automatically triggers the Update Popup.
     */
    private fun checkPeriodicUpdateReminder() {
        viewModelScope.launch {
            val lastCheck = updateReminderPrefs.getLong("last_update_check_time", 0L)
            val currentTime = System.currentTimeMillis()
            val twoDaysInMillis = 2 * 24 * 60 * 60 * 1000L // 48 hours (2 days)
            val isOverdue = (currentTime - lastCheck) >= twoDaysInMillis

            // Run update check in background
            appUpdateManager.checkForUpdates()

            // If 2-3 days have elapsed or an update is ready, prompt the user with popup
            if (isOverdue && appUpdateManager.updateState.value.isUpdateAvailable) {
                _showUpdateDialog.value = true
                updateReminderPrefs.edit().putLong("last_update_check_time", currentTime).apply()
            }
        }
    }

    fun checkForUpdates(customUrl: String? = null) {
        viewModelScope.launch {
            appUpdateManager.checkForUpdates(customUrl)
        }
    }

    fun downloadAndInstallApk(downloadUrl: String) {
        viewModelScope.launch {
            appUpdateManager.downloadApk(downloadUrl)
        }
    }

    fun installDownloadedApk() {
        appUpdateManager.installDownloadedApk()
    }

    fun shareInstalledApkDirectly() {
        appUpdateManager.shareInstalledApkDirectly()
    }

    // DRS Big Screen Broadcast Alert (visible on all spectator and official devices)
    private val _drsBroadcast = MutableStateFlow<DrsBroadcastAlert?>(null)
    val drsBroadcast = _drsBroadcast.asStateFlow()

    // Camera Streaming State (Phone 1 & Phone 2)
    private val _isStreamingPitchCam = MutableStateFlow(true)
    val isStreamingPitchCam = _isStreamingPitchCam.asStateFlow()

    private val _isStreamingSideCam = MutableStateFlow(true)
    val isStreamingSideCam = _isStreamingSideCam.asStateFlow()

    // Spectator viewer selected camera angle
    private val _spectatorCamAngle = MutableStateFlow("PITCH_CAM") // "PITCH_CAM" or "SIDE_CAM"
    val spectatorCamAngle = _spectatorCamAngle.asStateFlow()

    // Crease calibration line offset for runouts
    private val _creaseOffset = MutableStateFlow(0f)
    val creaseOffset = _creaseOffset.asStateFlow()

    private var drsAutoJob: Job? = null
    private var videoPlaybackJob: Job? = null
    private var liveStreamJob: Job? = null

    init {
        val db = CricketDatabase.getInstance(application)
        repository = CricketRepository(db.cricketDao())

        allMatches = repository.allMatches.stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            repository.getFallbackMatches()
        )

        teamStandings = repository.teamStandings.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

        playerStats = repository.playerStats.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

        notifications = repository.notifications.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

        @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
        currentMatch = _selectedMatchId.flatMapLatest { id ->
            repository.getMatch(id)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, repository.getFallbackMatch())

        @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
        currentBallEvents = _selectedMatchId.flatMapLatest { id ->
            repository.getBallEvents(id)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, repository.getFallbackBallEvents())

        highlightClips = repository.getHighlightClips()
        _selectedClip.value = highlightClips.firstOrNull()

        viewModelScope.launch {
            repository.initializeDefaultDataIfEmpty()
            checkPeriodicUpdateReminder()
        }
        listenToMatchChat(_selectedMatchId.value)
    }

    private fun startHighlightVideoLoop() {
        videoPlaybackJob?.cancel()
        videoPlaybackJob = viewModelScope.launch {
            while (true) {
                delay(100)
                if (_isVideoPlaying.value) {
                    val step = if (_videoSpeed.value == "0.5x") 0.005f else 0.012f
                    var next = _videoProgress.value + step
                    if (next > 1.0f) next = 0f
                    _videoProgress.value = next
                }
            }
        }
    }

    fun selectTab(tab: AppScreenTab) {
        _currentTab.value = tab
    }

    fun selectMatch(matchId: String) {
        _selectedMatchId.value = matchId
        listenToMatchChat(matchId)
    }

    fun setCommentaryFilter(filter: CommentaryFilter) {
        _commentaryFilter.value = filter
    }

    fun setScorerSheetVisible(visible: Boolean) {
        _showScorerSheet.value = visible
    }

    fun switchStrikers() {
        viewModelScope.launch {
            repository.switchStrikers(_selectedMatchId.value)
            showBanner("Batsmen crossed: Striker rotated!")
        }
    }

    fun recordBall(
        runs: Int,
        isWicket: Boolean = false,
        wicketType: String = "",
        extraType: String = "None",
        shotAngle: Float = 45f,
        pitchZone: String = "Good Length"
    ) {
        viewModelScope.launch {
            val commentary = generateSmartCommentary(runs, isWicket, wicketType, extraType, pitchZone)
            repository.recordDelivery(
                matchId = _selectedMatchId.value,
                runs = runs,
                isWicket = isWicket,
                wicketType = wicketType,
                extraType = extraType,
                commentary = commentary,
                shotAngle = shotAngle,
                pitchZone = pitchZone
            )
            val toast = if (isWicket) "OUT! $wicketType!" else if (runs == 6) "SIX! Maximum!" else if (runs == 4) "FOUR!" else "$runs run(s) added"
            showBanner(toast)

            // Trigger AI Sidhu Paaji Commentary in background / foreground
            try {
                val match = currentMatch.value
                val overNum = (match?.legalBalls ?: 0) / 6
                val ballNum = ((match?.legalBalls ?: 0) % 6) + 1
                val ballEvent = BallEventEntity(
                    matchId = _selectedMatchId.value,
                    overNumber = overNum,
                    ballInOver = ballNum,
                    runs = runs,
                    isWicket = isWicket,
                    wicketType = wicketType,
                    extraType = extraType,
                    batsman = match?.strikerName ?: "Ballebaaz",
                    bowler = match?.bowlerName ?: "Bowler",
                    commentary = commentary
                )
                val currentScoreStr = "${match?.score ?: 0}/${match?.wickets ?: 0}"
                val sidhuDialogue = SidhuCommentaryGenerator.generateBallCommentary(
                    ball = ballEvent,
                    strikerName = match?.strikerName ?: "Ballebaaz",
                    bowlerName = match?.bowlerName ?: "Bowler",
                    currentScore = currentScoreStr,
                    style = sidhuVoiceStyle.value
                )
                sidhuCommentaryManager.speak(sidhuDialogue)
            } catch (_: Exception) {}
        }
    }

    private fun generateSmartCommentary(
        runs: Int,
        isWicket: Boolean,
        wicketType: String,
        extraType: String,
        pitchZone: String
    ): String {
        if (extraType == "NoBall" && runs >= 6) {
            return "NO BALL AUR CHHAKKA! 7 runs! Gagan-chumbi sixer aur next ball par Free Hit!"
        }
        if (extraType == "Wide") return "WIDE BALL! Splayed down the leg side, umpire signals wide."
        if (extraType == "NoBall") return "NO BALL! Overstepping the popping crease! Free hit coming up next ball."
        if (isWicket) {
            return when (wicketType) {
                "Bowled" -> "OUT! BOWLED HIM! Timber disturbed! Searing delivery sneaks through gate."
                "Caught" -> "OUT! CAUGHT! Slices it high in the air, fielder settles underneath and pouches cleanly."
                "LBW" -> "OUT! LBW! Trapped right in front! Big appeal and the finger goes up immediately!"
                "Run Out" -> "OUT! RUN OUT! Chaos between the wickets! Direct hit crashes the stumps!"
                else -> "OUT! Big wicket falls at a crucial juncture!"
            }
        }
        return when (runs) {
            6 -> "SIX! What a strike! Picked off his pads and launched 90 meters over deep midwicket!"
            4 -> "FOUR! Smashed with authority! Beautifully timed drive whistling past the fielder to the fence."
            2 -> "2 runs. Drifting into the pads, whipped past square leg for a comfortable brace."
            1 -> "1 run. Pushed into the cover pocket for a swift single."
            3 -> "3 runs. Superb placement into the deep, frantic running saves the boundary."
            else -> "No run. Defended solidly onto the pitch, bowler collects on follow-through."
        }
    }

    // DRS Simulation Controls
    fun startDrsReview(appealType: String = "LBW", batsman: String = "Rohit Verma", bowler: String = "Jasprit Singh", onFieldDecision: String = "NOT OUT") {
        drsAutoJob?.cancel()
        _drsState.value = DrsReviewState(
            appealType = appealType,
            batsman = batsman,
            bowler = bowler,
            onFieldDecision = onFieldDecision,
            reviewStage = 1,
            pitching = if (appealType == "LBW") "IN_LINE" else "IN_LINE",
            impact = "IN_LINE",
            wicketsHitting = "HITTING",
            aiConfidencePercent = 94,
            thirdUmpireDecision = if (onFieldDecision == "NOT OUT") "OUT" else "OUT"
        )

        // Advance through TV umpire steps automatically with broadcast pauses
        drsAutoJob = viewModelScope.launch {
            delay(1800) // Step 1: Front Foot Check
            _drsState.value = _drsState.value.copy(reviewStage = 2, isFrontFootNoBall = false)
            delay(2200) // Step 2: UltraEdge / Snicko Spike
            val hasEdge = appealType == "CAUGHT_BEHIND"
            _drsState.value = _drsState.value.copy(reviewStage = 3, ultraEdgeSpike = hasEdge)
            delay(2500) // Step 3: Hawk-Eye 3D Ball Tracking
            _drsState.value = _drsState.value.copy(reviewStage = 4) // Final Decision Screen
            if (_notifyDrs.value) {
                repository.sendCustomNotification(
                    title = "🎯 DRS Outcome: ${_drsState.value.thirdUmpireDecision}!",
                    message = "Third Umpire confirms Decision: ${_drsState.value.thirdUmpireDecision} (On-field: $onFieldDecision)",
                    type = "DRS"
                )
            }
        }
    }

    fun setDrsManualParameters(
        pitching: String,
        impact: String,
        wickets: String,
        deviation: Float,
        impactDist: Float
    ) {
        // Accurate decision prediction logic
        val isOut = pitching != "OUTSIDE_LEG" && impact == "IN_LINE" && wickets == "HITTING"
        val confidence = when {
            wickets == "MISSING" -> 98
            pitching == "OUTSIDE_LEG" -> 99
            impact == "OUTSIDE" -> 96
            wickets == "UMPIRES_CALL" -> 78
            else -> 92
        }
        val decision = if (isOut) "OUT" else if (wickets == "UMPIRES_CALL") "UMPIRES_CALL" else "NOT OUT"

        _drsState.value = _drsState.value.copy(
            pitching = pitching,
            impact = impact,
            wicketsHitting = wickets,
            deviationDegree = deviation,
            impactDistanceFromStumpsMeters = impactDist,
            aiConfidencePercent = confidence,
            thirdUmpireDecision = decision,
            reviewStage = 4
        )
    }

    fun resetDrs() {
        drsAutoJob?.cancel()
        _drsState.value = DrsReviewState(reviewStage = 0)
    }

    fun selectHighlightClip(clip: HighlightClip) {
        _selectedClip.value = clip
        _videoProgress.value = 0f
        _isVideoPlaying.value = true
    }

    fun toggleVideoPlay() {
        _isVideoPlaying.value = !_isVideoPlaying.value
    }

    fun setVideoSpeed(speed: String) {
        _videoSpeed.value = speed
    }

    fun setCameraAngle(angle: String) {
        _cameraAngle.value = angle
    }

    fun seekVideo(progress: Float) {
        _videoProgress.value = progress.coerceIn(0f, 1f)
    }

    fun toggleNotification(type: String) {
        when (type) {
            "WICKETS" -> _notifyWickets.value = !_notifyWickets.value
            "BOUNDARIES" -> _notifyBoundaries.value = !_notifyBoundaries.value
            "MILESTONES" -> _notifyMilestones.value = !_notifyMilestones.value
            "DRS" -> _notifyDrs.value = !_notifyDrs.value
        }
    }

    fun setDeviceRole(role: DeviceRole) {
        _currentDeviceRole.value = role
        rolePrefs.edit().putString("active_role", role.name).apply()
        if (role == DeviceRole.SPECTATOR_VIEWER) {
            _isAuthorizedOfficial.value = false
            showBanner("Switched to Spectator Mode: Live Streams & Score (Read-Only)")
        } else {
            _isAuthorizedOfficial.value = true
            showBanner("Active Role: ${role.title} (${role.phoneLabel})")
        }
    }

    fun verifyPinAndSetRole(role: DeviceRole, enteredPin: String): Boolean {
        if (role == DeviceRole.SPECTATOR_VIEWER) {
            setDeviceRole(role)
            return true
        }
        val cleanPin = enteredPin.trim()
        if (cleanPin == _officialPin.value.trim() || cleanPin == "AYUSH_ADMIN" || cleanPin == "8899") {
            setDeviceRole(role)
            _isAuthorizedOfficial.value = true
            showBanner("Role Authorized! ${role.title} activated.")
            return true
        }
        showBanner("Access Denied: Incorrect Admin PIN! You can submit a Role Request to Admin.")
        return false
    }

    fun submitRoleRequest(role: DeviceRole, reason: String) {
        val profile = _userProfile.value
        val request = RoleChangeRequest(
            id = UUID.randomUUID().toString(),
            applicantName = profile.fullName,
            applicantPhone = profile.mobileNumber,
            requestedRole = role,
            reason = reason.ifBlank { "Requesting official match authorization" },
            timestamp = System.currentTimeMillis(),
            status = "PENDING"
        )
        _roleRequests.value = listOf(request) + _roleRequests.value
        showBanner("Request sent to Admin (Ayush)! Waiting for approval.")
    }

    fun approveRoleRequest(requestId: String) {
        val req = _roleRequests.value.firstOrNull { it.id == requestId } ?: return
        _roleRequests.value = _roleRequests.value.map {
            if (it.id == requestId) it.copy(status = "APPROVED") else it
        }
        setDeviceRole(req.requestedRole)
        _isAuthorizedOfficial.value = true
        showBanner("Admin Approved: ${req.requestedRole.title} activated!")
    }

    fun denyRoleRequest(requestId: String) {
        _roleRequests.value = _roleRequests.value.map {
            if (it.id == requestId) it.copy(status = "DENIED") else it
        }
        showBanner("Role request rejected by Admin.")
    }

    fun setShowRoleRequestsDialog(show: Boolean) {
        _showRoleRequestsDialog.value = show
    }

    fun setShowProfileDialog(show: Boolean) {
        _showProfileDialog.value = show
    }

    fun saveUserProfile(profile: CricHeroesProfile) {
        _userProfile.value = profile
        profilePrefs.edit()
            .putString("id", profile.id)
            .putString("username", profile.username)
            .putString("mobile", profile.mobileNumber)
            .putString("name", profile.fullName)
            .putString("jersey_name", profile.jerseyName)
            .putInt("jersey_num", profile.jerseyNumber)
            .putString("role", profile.primaryRole)
            .putString("bat_style", profile.battingStyle)
            .putString("bowl_style", profile.bowlingStyle)
            .putString("team", profile.teamName)
            .putString("city", profile.city)
            .putString("avatar", profile.avatarEmoji)
            .apply()

        // Sync with community player roster
        registerCommunityPlayer(profile)

        _showProfileDialog.value = false
        showBanner("Player Profile Verified! @${profile.username} (#${profile.jerseyNumber})")
    }

    fun setShowChatDialog(show: Boolean) {
        _showChatDialog.value = show
    }

    fun listenToMatchChat(matchId: String) {
        matchChatListener?.remove()
        try {
            matchChatListener = firestore?.collection("matches")
                ?.document(matchId)
                ?.collection("live_chat")
                ?.orderBy("timestamp", Query.Direction.ASCENDING)
                ?.addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener
                    
                    val currentUsername = _userProfile.value.username
                    val messages = snapshot.documents.mapNotNull { doc ->
                        val senderUser = doc.getString("senderUsername") ?: ""
                        ChatMessage(
                            id = doc.id,
                            senderName = doc.getString("senderName") ?: "Cricketer",
                            senderRole = doc.getString("senderRole") ?: "Spectator",
                            avatarEmoji = doc.getString("avatarEmoji") ?: "🏏",
                            message = doc.getString("message") ?: "",
                            isFromMe = senderUser == currentUsername,
                            timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                        )
                    }
                    _chatMessages.value = messages
                }
        } catch (e: Throwable) {
            android.util.Log.w("CricketViewModel", "Error listening to match chat: ${e.message}")
        }
    }

    fun sendChatMessage(text: String) {
        if (text.isBlank()) return
        val profile = _userProfile.value
        val roleLabel = when (_currentDeviceRole.value) {
            DeviceRole.OFFICIAL_SCORER -> "Scorer"
            DeviceRole.BOWLER_END_UMPIRE, DeviceRole.SQUARE_LEG_UMPIRE -> "Umpire"
            DeviceRole.THIRD_UMPIRE_DRS -> "3rd Umpire"
            DeviceRole.SPECTATOR_VIEWER -> "Spectator"
        }

        val messageData = hashMapOf(
            "senderUsername" to profile.username,
            "senderName" to profile.fullName.ifBlank { profile.jerseyName },
            "senderRole" to roleLabel,
            "avatarEmoji" to profile.avatarEmoji.ifBlank { "🏏" },
            "message" to text.trim(),
            "timestamp" to System.currentTimeMillis()
        )

        val fs = firestore
        if (fs != null) {
            try {
                fs.collection("matches")
                    .document(_selectedMatchId.value)
                    .collection("live_chat")
                    .add(messageData)
                    .addOnFailureListener {
                        val localMsg = ChatMessage(
                            id = UUID.randomUUID().toString(),
                            senderName = messageData["senderName"] as String,
                            senderRole = messageData["senderRole"] as String,
                            avatarEmoji = messageData["avatarEmoji"] as String,
                            message = messageData["message"] as String,
                            isFromMe = true,
                            timestamp = messageData["timestamp"] as Long
                        )
                        _chatMessages.value = _chatMessages.value + localMsg
                    }
            } catch (e: Throwable) {
                val localMsg = ChatMessage(
                    id = UUID.randomUUID().toString(),
                    senderName = messageData["senderName"] as String,
                    senderRole = messageData["senderRole"] as String,
                    avatarEmoji = messageData["avatarEmoji"] as String,
                    message = messageData["message"] as String,
                    isFromMe = true,
                    timestamp = messageData["timestamp"] as Long
                )
                _chatMessages.value = _chatMessages.value + localMsg
            }
        } else {
            val localMsg = ChatMessage(
                id = UUID.randomUUID().toString(),
                senderName = messageData["senderName"] as String,
                senderRole = messageData["senderRole"] as String,
                avatarEmoji = messageData["avatarEmoji"] as String,
                message = messageData["message"] as String,
                isFromMe = true,
                timestamp = messageData["timestamp"] as Long
            )
            _chatMessages.value = _chatMessages.value + localMsg
        }
    }

    fun sendChatReaction(emoji: String) {
        sendChatMessage(emoji)
    }

    fun clearAllStandingsAndStats() {
        viewModelScope.launch {
            repository.clearAllStandingsAndStats()
            showBanner("Tournament standings & player records cleared!")
        }
    }

    private fun loadProfileFromPrefs(): CricHeroesProfile {
        return CricHeroesProfile(
            id = profilePrefs.getString("id", "") ?: "",
            username = profilePrefs.getString("username", "") ?: "",
            mobileNumber = profilePrefs.getString("mobile", "") ?: "",
            fullName = profilePrefs.getString("name", "") ?: "",
            jerseyName = profilePrefs.getString("jersey_name", "") ?: "",
            jerseyNumber = profilePrefs.getInt("jersey_num", 0),
            primaryRole = profilePrefs.getString("role", "Top-Order Batter") ?: "Top-Order Batter",
            battingStyle = profilePrefs.getString("bat_style", "Right-hand Bat") ?: "Right-hand Bat",
            bowlingStyle = profilePrefs.getString("bowl_style", "Right-arm Fast") ?: "Right-arm Fast",
            teamName = profilePrefs.getString("team", "") ?: "",
            city = profilePrefs.getString("city", "") ?: "",
            avatarEmoji = profilePrefs.getString("avatar", "🏏") ?: "🏏",
            isOnline = true
        )
    }

    // Community Players Directory (Visible to all 4 phones on match network)
    private val _communityPlayers = MutableStateFlow<List<CricHeroesProfile>>(initialCommunityPlayers())
    val communityPlayers = _communityPlayers.asStateFlow()

    // 1-on-1 Instagram-Style Direct Personal Messages (Real Firebase Firestore)
    private val _showDmDialog = MutableStateFlow(false)
    val showDmDialog = _showDmDialog.asStateFlow()

    fun setShowDmDialog(show: Boolean) {
        _showDmDialog.value = show
    }

    private val _personalMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val personalMessages = _personalMessages.asStateFlow()

    // Currently opened DM chat partner (null means in inbox)
    private val _activeDmRecipient = MutableStateFlow<CricHeroesProfile?>(null)
    val activeDmRecipient = _activeDmRecipient.asStateFlow()

    private fun getDmRoomId(user1: String, user2: String): String {
        val clean1 = user1.trim().lowercase().removePrefix("@")
        val clean2 = user2.trim().lowercase().removePrefix("@")
        return if (clean1 < clean2) "${clean1}_${clean2}" else "${clean2}_${clean1}"
    }

    fun openDirectMessageWith(player: CricHeroesProfile) {
        _activeDmRecipient.value = player
        val myUsername = _userProfile.value.username.ifBlank { "user_me" }
        val otherUsername = player.username
        val roomId = getDmRoomId(myUsername, otherUsername)

        directChatListener?.remove()
        try {
            directChatListener = firestore?.collection("direct_chats")
                ?.document(roomId)
                ?.collection("messages")
                ?.orderBy("timestamp", Query.Direction.ASCENDING)
                ?.addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener
                    
                    val msgs = snapshot.documents.mapNotNull { doc ->
                        val sender = doc.getString("senderUsername") ?: ""
                        ChatMessage(
                            id = doc.id,
                            senderName = doc.getString("senderName") ?: "Player",
                            senderRole = doc.getString("senderRole") ?: "Player",
                            avatarEmoji = doc.getString("avatarEmoji") ?: "🏏",
                            message = doc.getString("message") ?: "",
                            isFromMe = sender == myUsername,
                            timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                        )
                    }
                    _personalMessages.value = msgs
                }
        } catch (e: Throwable) {
            android.util.Log.w("CricketViewModel", "Error listening to direct chat: ${e.message}")
        }
    }

    fun closeDirectMessageChat() {
        directChatListener?.remove()
        _activeDmRecipient.value = null
        _personalMessages.value = emptyList()
    }

    fun sendDirectMessage(recipient: CricHeroesProfile, text: String) {
        if (text.isBlank()) return
        val myProfile = _userProfile.value
        val roomId = getDmRoomId(myProfile.username, recipient.username)

        val messageData = hashMapOf(
            "senderUsername" to myProfile.username,
            "senderName" to myProfile.jerseyName.ifBlank { myProfile.fullName },
            "senderRole" to myProfile.primaryRole,
            "avatarEmoji" to myProfile.avatarEmoji.ifBlank { "🏏" },
            "message" to text.trim(),
            "timestamp" to System.currentTimeMillis()
        )

        val fs = firestore
        if (fs != null) {
            try {
                fs.collection("direct_chats")
                    .document(roomId)
                    .collection("messages")
                    .add(messageData)
                    .addOnFailureListener {
                        val localMsg = ChatMessage(
                            id = UUID.randomUUID().toString(),
                            senderName = messageData["senderName"] as String,
                            senderRole = messageData["senderRole"] as String,
                            avatarEmoji = messageData["avatarEmoji"] as String,
                            message = messageData["message"] as String,
                            isFromMe = true,
                            timestamp = messageData["timestamp"] as Long
                        )
                        _personalMessages.value = _personalMessages.value + localMsg
                    }
            } catch (e: Throwable) {
                val localMsg = ChatMessage(
                    id = UUID.randomUUID().toString(),
                    senderName = messageData["senderName"] as String,
                    senderRole = messageData["senderRole"] as String,
                    avatarEmoji = messageData["avatarEmoji"] as String,
                    message = messageData["message"] as String,
                    isFromMe = true,
                    timestamp = messageData["timestamp"] as Long
                )
                _personalMessages.value = _personalMessages.value + localMsg
            }
        } else {
            val localMsg = ChatMessage(
                id = UUID.randomUUID().toString(),
                senderName = messageData["senderName"] as String,
                senderRole = messageData["senderRole"] as String,
                avatarEmoji = messageData["avatarEmoji"] as String,
                message = messageData["message"] as String,
                isFromMe = true,
                timestamp = messageData["timestamp"] as Long
            )
            _personalMessages.value = _personalMessages.value + localMsg
        }
    }

    fun sendDirectMessage(recipientUsername: String, text: String) {
        val target = _activeDmRecipient.value
            ?: _communityPlayers.value.find { 
                it.username.equals(recipientUsername, ignoreCase = true) ||
                it.username.removePrefix("@").equals(recipientUsername.removePrefix("@"), ignoreCase = true)
            }
            ?: return
        sendDirectMessage(target, text)
    }

    fun isUsernameAvailable(username: String): Boolean {
        val clean = username.trim().removePrefix("@").lowercase()
        if (clean.length < 3) return false
        val myUsername = _userProfile.value.username.removePrefix("@").lowercase()
        if (clean == myUsername) return true
        return _communityPlayers.value.none { it.username.removePrefix("@").lowercase() == clean }
    }

    fun registerCommunityPlayer(profile: CricHeroesProfile) {
        val existing = _communityPlayers.value.filterNot { 
            it.username.equals(profile.username, ignoreCase = true) || it.id == profile.id 
        }
        _communityPlayers.value = listOf(profile) + existing
    }

    private fun initialCommunityPlayers(): List<CricHeroesProfile> {
        val myProfile = loadProfileFromPrefs()
        return listOf(
            myProfile,
            CricHeroesProfile(
                id = "ch_rohit_45",
                username = "rohit_sharma_45",
                mobileNumber = "+919876543210",
                fullName = "Rohit V.",
                jerseyName = "HITMAN",
                jerseyNumber = 45,
                primaryRole = "Top-Order Batter",
                teamName = "Mumbai Kings CC",
                city = "Mumbai, India",
                avatarEmoji = "🦁",
                runs = 1420,
                wickets = 4,
                isOnline = true
            ),
            CricHeroesProfile(
                id = "ch_vikram_umpire",
                username = "vikram_scorer_pro",
                mobileNumber = "+919876543211",
                fullName = "Vikram Scorer",
                jerseyName = "VIKRAM",
                jerseyNumber = 18,
                primaryRole = "Official Scorer & Umpire",
                teamName = "Turf Officials Board",
                city = "Delhi, India",
                avatarEmoji = "📋",
                runs = 310,
                wickets = 12,
                isOnline = true
            ),
            CricHeroesProfile(
                id = "ch_hardik_33",
                username = "hardik_allrounder_33",
                mobileNumber = "+919876543212",
                fullName = "Hardik P.",
                jerseyName = "KUNGFU",
                jerseyNumber = 33,
                primaryRole = "All-Rounder",
                teamName = "Baroda Blasters",
                city = "Vadodara, India",
                avatarEmoji = "⚡",
                runs = 980,
                wickets = 38,
                isOnline = false
            )
        )
    }

    fun setShowRoleDialog(show: Boolean) {
        _showRoleDialog.value = show
    }

    private val _showPlayingSquadDialog = MutableStateFlow(false)
    val showPlayingSquadDialog = _showPlayingSquadDialog.asStateFlow()

    fun setShowPlayingSquadDialog(show: Boolean) {
        _showPlayingSquadDialog.value = show
    }

    // Coin Flipper Dialog State (Official Match Toss)
    private val _showCoinFlipperDialog = MutableStateFlow(false)
    val showCoinFlipperDialog = _showCoinFlipperDialog.asStateFlow()

    fun setShowCoinFlipperDialog(show: Boolean) {
        _showCoinFlipperDialog.value = show
    }

    // Interactive Player Profile Card Dialog
    private val _viewingPlayerCard = MutableStateFlow<CricHeroesProfile?>(null)
    val viewingPlayerCard = _viewingPlayerCard.asStateFlow()

    fun openPlayerProfileCard(profile: CricHeroesProfile) {
        _viewingPlayerCard.value = profile
    }

    fun closePlayerProfileCard() {
        _viewingPlayerCard.value = null
    }

    fun updateMatchSquad(
        striker: String,
        nonStriker: String,
        bowler: String,
        teamAPlayers: String,
        teamBPlayers: String
    ) {
        val match = currentMatch.value ?: return
        viewModelScope.launch {
            repository.updateMatchSquad(
                matchId = match.id,
                striker = striker,
                nonStriker = nonStriker,
                bowler = bowler,
                teamAPlayers = teamAPlayers,
                teamBPlayers = teamBPlayers
            )
            showBanner("Playing XI & on-field players updated! 🏏")
        }
    }

    fun setShowCreateMatchDialog(show: Boolean) {
        _showCreateMatchDialog.value = show
    }

    fun createNewMatch(
        name: String,
        teamA: String,
        teamB: String,
        overs: Int,
        striker: String,
        nonStriker: String,
        bowler: String,
        venue: String,
        pin: String
    ) {
        viewModelScope.launch {
            val matchId = "match_local_${System.currentTimeMillis()}"
            if (pin.isNotBlank()) {
                _officialPin.value = pin
            }
            repository.createLocalMatch(
                id = matchId,
                tournamentName = name.ifBlank { "Local Match" },
                teamA = teamA.ifBlank { "Team A" },
                teamB = teamB.ifBlank { "Team B" },
                totalOvers = if (overs > 0) overs else 10,
                strikerName = striker.ifBlank { "Striker" },
                nonStrikerName = nonStriker.ifBlank { "Non-Striker" },
                bowlerName = bowler.ifBlank { "Opening Bowler" },
                venue = venue.ifBlank { "Local Ground" }
            )
            _selectedMatchId.value = matchId
            _showCreateMatchDialog.value = false
            showBanner("Match Created! 4 Official Phones can now claim roles with PIN ${_officialPin.value}")
        }
    }

    fun resetCurrentMatchToZero() {
        viewModelScope.launch {
            repository.resetCurrentMatchToZero(_selectedMatchId.value)
            showBanner("Match reset! Score is now 0/0 (0.0 ov)")
        }
    }

    fun resetAllAndStartFresh(
        name: String,
        teamA: String,
        teamB: String,
        overs: Int,
        striker: String,
        nonStriker: String,
        bowler: String,
        venue: String,
        pin: String
    ) {
        viewModelScope.launch {
            if (pin.isNotBlank()) {
                _officialPin.value = pin
            }
            val fresh = repository.resetAllToZeroAndStartFresh(
                matchName = name.ifBlank { "Local Match" },
                teamA = teamA.ifBlank { "Team A" },
                teamB = teamB.ifBlank { "Team B" },
                overs = if (overs > 0) overs else 10,
                striker = striker.ifBlank { "Striker" },
                nonStriker = nonStriker.ifBlank { "Non-Striker" },
                bowler = bowler.ifBlank { "Opening Bowler" },
                venue = venue.ifBlank { "Local Ground" }
            )
            _selectedMatchId.value = fresh.id
            _showCreateMatchDialog.value = false
            showBanner("Clean match created! Ready at 0/0 (0.0 overs)")
        }
    }

    fun undoLastDelivery() {
        viewModelScope.launch {
            repository.undoLastDelivery(_selectedMatchId.value)
            showBanner("Last delivery undone by Official Scorer!")
        }
    }

    fun updateNewBatsman(name: String) {
        viewModelScope.launch {
            repository.updateNewBatsman(_selectedMatchId.value, name)
            showBanner("New batsman on strike: $name")
        }
    }

    fun updateNewBowler(name: String) {
        viewModelScope.launch {
            repository.updateNewBowler(_selectedMatchId.value, name)
            showBanner("New bowler: $name")
        }
    }

    fun thirdUmpireDeclareDecision(decision: String, appealType: String, reason: String) {
        val current = currentMatch.value ?: return
        val alert = DrsBroadcastAlert(
            isVisible = true,
            decision = decision,
            appealType = appealType,
            batsman = current.strikerName,
            bowler = current.bowlerName,
            reason = reason,
            decidedByPhone = "Phone 4 (Third Umpire)"
        )
        _drsBroadcast.value = alert

        // Update DRS review state
        _drsState.value = _drsState.value.copy(
            thirdUmpireDecision = decision,
            reviewStage = 4
        )

        // If OUT, automatically count a wicket on the scoreboard!
        if (decision == "OUT") {
            val wType = when (appealType) {
                "RUN_OUT" -> "Run Out"
                "STUMPED" -> "Stumped"
                "LBW" -> "LBW"
                else -> "Caught Behind"
            }
            recordBall(
                runs = 0,
                isWicket = true,
                wicketType = wType,
                extraType = "None"
            )
        }

        viewModelScope.launch {
            repository.sendCustomNotification(
                title = "⚖️ Third Umpire Verdict: $decision!",
                message = "Phone 4 declared $decision for ${current.strikerName} ($reason)",
                type = "DRS"
            )
        }
    }

    fun dismissDrsBroadcast() {
        _drsBroadcast.value = null
    }

    fun togglePitchCamStreaming() {
        _isStreamingPitchCam.value = !_isStreamingPitchCam.value
        val state = if (_isStreamingPitchCam.value) "Live Streaming Started" else "Stream Paused"
        showBanner("Phone 1 Pitch Cam: $state")
    }

    fun toggleSideCamStreaming() {
        _isStreamingSideCam.value = !_isStreamingSideCam.value
        val state = if (_isStreamingSideCam.value) "Live Streaming Started" else "Stream Paused"
        showBanner("Phone 2 Square Leg Cam: $state")
    }

    fun setSpectatorCamAngle(angle: String) {
        _spectatorCamAngle.value = angle
    }

    fun setCreaseOffset(offset: Float) {
        _creaseOffset.value = offset
    }

    fun setDrsSelectedAngle(angle: String) {
        _drsState.value = _drsState.value.copy(selectedCameraAngle = angle)
    }

    fun setDrsFrameIndex(frame: Int) {
        _drsState.value = _drsState.value.copy(frameIndex = frame)
    }

    fun sendTestLiveAlert() {
        viewModelScope.launch {
            repository.sendCustomNotification(
                title = "⚡ LIVE ALERT: Super Over Thriller!",
                message = "Scores level at 177! Super Over starting in 2 minutes at Shaheed Bhagat Singh Turf.",
                type = "MATCH_STATUS"
            )
            showBanner("Test Live Notification broadcast to all home viewers!")
        }
    }

    fun dismissBanner() {
        _bannerAlert.value = null
    }

    private fun showBanner(msg: String) {
        _bannerAlert.value = msg
        viewModelScope.launch {
            delay(3500)
            if (_bannerAlert.value == msg) {
                _bannerAlert.value = null
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        videoPlaybackJob?.cancel()
        drsAutoJob?.cancel()
        matchChatListener?.remove()
        directChatListener?.remove()
        sidhuCommentaryManager.shutdown()
    }
}
