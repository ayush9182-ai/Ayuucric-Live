package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.AiEngineMode
import com.example.data.ai.AiMatchSummarizer
import com.example.data.ai.AiSettingsManager
import com.example.data.ai.MatchSummaryResult
import com.example.data.audio.AiSidhuCommentaryManager
import com.example.data.audio.SidhuCommentaryGenerator
import com.example.data.audio.SidhuVoiceStyle
import com.example.data.audio.StadiumSoundManager
import com.example.data.chat.RealChatRepository
import com.example.data.cloud.CloudSyncService
import com.example.data.firebase.RealtimeMatchSyncService
import com.example.data.local.CricketDatabase
import com.example.data.model.BallEventEntity
import com.example.data.model.BroadcastOverlayEvent
import com.example.data.model.ChatMessage
import com.example.data.model.CricHeroesProfile
import com.example.data.model.DeviceRole
import com.example.data.model.DrsBroadcastAlert
import com.example.data.model.DrsReviewState
import com.example.data.model.HighlightClip
import com.example.data.model.MatchEntity
import com.example.data.model.NotificationAlertEntity
import com.example.data.model.PlayerStatEntity
import com.example.data.model.RoleChangeRequest
import com.example.data.model.TeamStandingEntity
import com.example.data.network.NetworkConnectivityObserver
import com.example.data.network.NetworkStatus
import com.example.data.repository.CricketRepository
import com.example.data.update.AppUpdateManager
import com.example.data.update.AppUpdateState
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.example.util.TournamentStatsCalculator
import java.util.UUID

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

    // Real Firebase Firestore Instance (Automatically reads google-services.json)
    private val firestore: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance()
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

    fun setShowNetworkDialog(show: Boolean) { _showNetworkDialog.value = show }
    fun setForceLiteMode(force: Boolean) { _isForceLiteMode.value = force }

    private val sidhuCommentaryManager = AiSidhuCommentaryManager(application)
    val isSidhuCommentaryEnabled = sidhuCommentaryManager.isCommentaryEnabled
    val isSidhuSpeaking = sidhuCommentaryManager.isSpeaking
    val sidhuCurrentDialogue = sidhuCommentaryManager.currentDialogue
    val sidhuVoiceStyle = sidhuCommentaryManager.voiceStyle
    val sidhuPitch = sidhuCommentaryManager.currentPitch
    val sidhuSpeed = sidhuCommentaryManager.currentSpeed
    val sidhuVoiceGender = sidhuCommentaryManager.voiceGender

    fun toggleSidhuCommentary(enabled: Boolean? = null) { sidhuCommentaryManager.toggleCommentary(enabled) }
    fun setSidhuVoiceStyle(style: SidhuVoiceStyle) { sidhuCommentaryManager.setVoiceStyle(style) }
    fun setSidhuPitch(pitch: Float) { sidhuCommentaryManager.setCustomPitch(pitch) }
    fun setSidhuSpeed(speed: Float) { sidhuCommentaryManager.setCustomSpeed(speed) }
    fun setSidhuVoiceGender(gender: String) { sidhuCommentaryManager.setVoiceGender(gender) }
    fun resetSidhuVoiceDefaults() { sidhuCommentaryManager.resetToSidhuDefaults() }
    fun testSidhuVoice() { sidhuCommentaryManager.testVoiceSample() }
    fun testSidhuCommentary() { sidhuCommentaryManager.triggerTestDialogue() }

    // AI Match Summary & Wrap-up
    private val aiSettingsManager = AiSettingsManager(application)
    val customApiKey = aiSettingsManager.customApiKey
    val aiEngineMode = aiSettingsManager.currentMode

    private val _showAiSettingsDialog = MutableStateFlow(false)
    val showAiSettingsDialog = _showAiSettingsDialog.asStateFlow()

    fun setShowAiSettingsDialog(show: Boolean) { _showAiSettingsDialog.value = show }
    fun saveCustomApiKey(key: String) { aiSettingsManager.saveCustomApiKey(key) }
    fun clearCustomApiKey() { aiSettingsManager.clearCustomApiKey() }
    fun setAiEngineMode(mode: AiEngineMode) { aiSettingsManager.setAiMode(mode) }

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
            generateMatchSummary()
        }
    }

    fun stopSummaryAudio() { sidhuCommentaryManager.stop() }

    private val _isVideoOverlayExpanded = MutableStateFlow(true)
    val isVideoOverlayExpanded = _isVideoOverlayExpanded.asStateFlow()

    fun selectLiveCenterSubTab(subTab: LiveCenterSubTab) { _liveCenterSubTab.value = subTab }
    fun toggleVideoOverlayExpanded() { _isVideoOverlayExpanded.value = !_isVideoOverlayExpanded.value }

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

    private val _showScorerSheet = MutableStateFlow(false)
    val showScorerSheet = _showScorerSheet.asStateFlow()

    // Unified Messages Hub State
    private val _showMessagesHub = MutableStateFlow(false)
    val showMessagesHub = _showMessagesHub.asStateFlow()
    private val _messagesHubTab = MutableStateFlow(0)
    val messagesHubTab = _messagesHubTab.asStateFlow()

    fun openMessagesHub(tab: Int = 0) {
        _messagesHubTab.value = tab
        _showMessagesHub.value = true
        syncCloudUsers()
        refreshCloudMessages()
    }

    fun closeMessagesHub() {
        _showMessagesHub.value = false
        closeDirectMessageChat()
    }

    fun recordQuickRun(runs: Int) { recordBall(runs = runs) }
    fun recordQuickWicket() { recordBall(runs = 0, isWicket = true, wicketType = "Out") }
    fun recordQuickExtra(extraType: String = "Wide") { recordBall(runs = 0, extraType = extraType) }

    // DRS Review State
    private val _drsState = MutableStateFlow(DrsReviewState())
    val drsState = _drsState.asStateFlow()

    private val _pitchAnimPhase = MutableStateFlow(0f)
    val pitchAnimPhase = _pitchAnimPhase.asStateFlow()

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

    private val _notifyWickets = MutableStateFlow(true)
    val notifyWickets = _notifyWickets.asStateFlow()
    private val _notifyBoundaries = MutableStateFlow(true)
    val notifyBoundaries = _notifyBoundaries.asStateFlow()
    private val _notifyMilestones = MutableStateFlow(true)
    val notifyMilestones = _notifyMilestones.asStateFlow()
    private val _notifyDrs = MutableStateFlow(true)
    val notifyDrs = _notifyDrs.asStateFlow()

    private val _bannerAlert = MutableStateFlow<String?>(null)
    val bannerAlert = _bannerAlert.asStateFlow()

    private val rolePrefs = application.getSharedPreferences("ayuu_device_roles", Context.MODE_PRIVATE)
    private val savedRole = try {
        DeviceRole.valueOf(rolePrefs.getString("active_role", DeviceRole.SPECTATOR_VIEWER.name) ?: DeviceRole.SPECTATOR_VIEWER.name)
    } catch (_: Exception) {
        DeviceRole.SPECTATOR_VIEWER
    }

    private val _currentDeviceRole = MutableStateFlow(savedRole)
    val currentDeviceRole = _currentDeviceRole.asStateFlow()

    private val _officialPin = MutableStateFlow(rolePrefs.getString("admin_pin", "8899") ?: "8899")
    val officialPin = _officialPin.asStateFlow()

    private val _isAuthorizedOfficial = MutableStateFlow(savedRole != DeviceRole.SPECTATOR_VIEWER)
    val isAuthorizedOfficial = _isAuthorizedOfficial.asStateFlow()

    private val _roleRequests = MutableStateFlow<List<RoleChangeRequest>>(emptyList())
    val roleRequests = _roleRequests.asStateFlow()

    private val _showRoleRequestsDialog = MutableStateFlow(false)
    val showRoleRequestsDialog = _showRoleRequestsDialog.asStateFlow()

    private val profilePrefs = application.getSharedPreferences("ayuu_cricheroes_profile", Context.MODE_PRIVATE)

    fun isUserLoggedIn(): Boolean {
        val hasLoggedFlag = profilePrefs.getBoolean("is_logged_in", false)
        val hasUsername = !profilePrefs.getString("username", "").isNullOrBlank()
        val hasName = !profilePrefs.getString("name", "").isNullOrBlank()
        return hasLoggedFlag && hasUsername && hasName
    }

    private val _userProfile = MutableStateFlow(loadProfileFromPrefs())
    val userProfile = _userProfile.asStateFlow()

    private val _showProfileDialog = MutableStateFlow(false)
    val showProfileDialog = _showProfileDialog.asStateFlow()

    private val _showLoginScreen = MutableStateFlow(!isUserLoggedIn())
    val showLoginScreen = _showLoginScreen.asStateFlow()

    fun setShowLoginScreen(show: Boolean) { _showLoginScreen.value = show }

    fun performLogin(profile: CricHeroesProfile) {
        saveUserProfile(profile)
        _showLoginScreen.value = false
        showBanner("Welcome ${profile.fullName}! Verified as ${profile.jerseyName} #${profile.jerseyNumber}")
    }

    fun logout() {
        profilePrefs.edit().clear().apply()
        _userProfile.value = loadProfileFromPrefs()
        _communityPlayers.value = emptyList()
        _showLoginScreen.value = true
        showBanner("Logged out successfully.")
    }

    private val _showWhatsAppShareDialog = MutableStateFlow(false)
    val showWhatsAppShareDialog = _showWhatsAppShareDialog.asStateFlow()
    fun setShowWhatsAppShareDialog(show: Boolean) { _showWhatsAppShareDialog.value = show }

    private val _showWagonWheelDialog = MutableStateFlow(false)
    val showWagonWheelDialog = _showWagonWheelDialog.asStateFlow()
    fun setShowWagonWheelDialog(show: Boolean) { _showWagonWheelDialog.value = show }

    private val _activeBroadcastOverlay = MutableStateFlow<BroadcastOverlayEvent?>(null)
    val activeBroadcastOverlay = _activeBroadcastOverlay.asStateFlow()

    fun triggerBroadcastOverlay(event: BroadcastOverlayEvent) { _activeBroadcastOverlay.value = event }
    fun dismissBroadcastOverlay() { _activeBroadcastOverlay.value = null }

    private val _showChatDialog = MutableStateFlow(false)
    val showChatDialog = _showChatDialog.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages = _chatMessages.asStateFlow()

    private val _showRoleDialog = MutableStateFlow(false)
    val showRoleDialog = _showRoleDialog.asStateFlow()

    private val _showCreateMatchDialog = MutableStateFlow(false)
    val showCreateMatchDialog = _showCreateMatchDialog.asStateFlow()

    private val appUpdateManager = AppUpdateManager(application)
    val updateState: StateFlow<AppUpdateState> = appUpdateManager.updateState

    private val _showUpdateDialog = MutableStateFlow(false)
    val showUpdateDialog = _showUpdateDialog.asStateFlow()

    private val updateReminderPrefs = application.getSharedPreferences("ayuu_update_prefs", Context.MODE_PRIVATE)

    fun setShowUpdateDialog(show: Boolean) {
        _showUpdateDialog.value = show
        if (show) {
            updateReminderPrefs.edit().putLong("last_update_check_time", System.currentTimeMillis()).apply()
        }
    }

    private fun checkPeriodicUpdateReminder() {
        viewModelScope.launch {
            val lastCheck = updateReminderPrefs.getLong("last_update_check_time", 0L)
            val currentTime = System.currentTimeMillis()
            val twoDaysInMillis = 2 * 24 * 60 * 60 * 1000L
            val isOverdue = (currentTime - lastCheck) >= twoDaysInMillis

            appUpdateManager.checkForUpdates()

            if (isOverdue && appUpdateManager.updateState.value.isUpdateAvailable) {
                _showUpdateDialog.value = true
                updateReminderPrefs.edit().putLong("last_update_check_time", currentTime).apply()
            }
        }
    }

    fun checkForUpdates(customUrl: String? = null) {
        viewModelScope.launch { appUpdateManager.checkForUpdates(customUrl) }
    }

    fun downloadAndInstallApk(downloadUrl: String) {
        viewModelScope.launch { appUpdateManager.downloadApk(downloadUrl) }
    }

    fun installDownloadedApk() { appUpdateManager.installDownloadedApk() }
    fun shareInstalledApkDirectly() { appUpdateManager.shareInstalledApkDirectly() }

    private val _drsBroadcast = MutableStateFlow<DrsBroadcastAlert?>(null)
    val drsBroadcast = _drsBroadcast.asStateFlow()

    private val _showChangeBowlerDialog = MutableStateFlow(false)
    val showChangeBowlerDialog = _showChangeBowlerDialog.asStateFlow()

    private val _showNewBatsmanDialog = MutableStateFlow(false)
    val showNewBatsmanDialog = _showNewBatsmanDialog.asStateFlow()

    private val _pendingWicketType = MutableStateFlow("Bowled")
    val pendingWicketType = _pendingWicketType.asStateFlow()

    private val _showChangeBatsmanDialog = MutableStateFlow(false)
    val showChangeBatsmanDialog = _showChangeBatsmanDialog.asStateFlow()

    fun openChangeBowlerDialog() { _showChangeBowlerDialog.value = true }
    fun closeChangeBowlerDialog() { _showChangeBowlerDialog.value = false }

    fun openNewBatsmanDialog(wicketType: String = "Bowled") {
        _pendingWicketType.value = wicketType
        _showNewBatsmanDialog.value = true
    }
    fun closeNewBatsmanDialog() { _showNewBatsmanDialog.value = false }

    fun openChangeBatsmanDialog() { _showChangeBatsmanDialog.value = true }
    fun closeChangeBatsmanDialog() { _showChangeBatsmanDialog.value = false }

    fun changeBowler(newBowlerName: String) {
        viewModelScope.launch {
            repository.updateNewBowler(_selectedMatchId.value, newBowlerName)
            val updated = repository.getMatch(_selectedMatchId.value).firstOrNull()
            if (updated != null) {
                RealtimeMatchSyncService.publishMatch(updated)
                CloudSyncService.publishLiveMatch(updated)
            }
            showBanner("🎳 Naye Bowler: $newBowlerName attack par aaye hain!")
            try {
                sidhuCommentaryManager.speak("Bowling change guru! Ab balling karenge $newBowlerName! Thoko taali!")
            } catch (_: Exception) {}
        }
    }

    fun changeBatsman(isStriker: Boolean, newName: String) {
        viewModelScope.launch {
            repository.updateNewBatsman(_selectedMatchId.value, newName, isStriker)
            val updated = repository.getMatch(_selectedMatchId.value).firstOrNull()
            if (updated != null) {
                RealtimeMatchSyncService.publishMatch(updated)
                CloudSyncService.publishLiveMatch(updated)
            }
            val role = if (isStriker) "Striker" else "Non-Striker"
            showBanner("🏏 $role badal kar $newName kiya gaya!")
        }
    }

    fun recordWicketWithNewBatsman(
        dismissedBatsman: String,
        newBatsmanName: String,
        wicketType: String,
        newBatsmanOnStrike: Boolean,
        runsOnBall: Int = 0
    ) {
        viewModelScope.launch {
            val commentary = "OUT! $dismissedBatsman $wicketType! In comes $newBatsmanName."
            val updatedMatch = repository.recordDelivery(
                matchId = _selectedMatchId.value,
                runs = runsOnBall,
                isWicket = true,
                wicketType = wicketType,
                extraType = "None",
                commentary = commentary,
                shotAngle = 0f,
                pitchZone = "Good Length",
                newBatsmanName = newBatsmanName,
                dismissedBatsman = dismissedBatsman,
                newBatsmanOnStrike = newBatsmanOnStrike
            )
            showBanner("⚡ OUT! $dismissedBatsman $wicketType! $newBatsmanName maidaan par aaye.")
            try {
                StadiumSoundManager.playWicketDismissal()
                val scoreVoice = if (updatedMatch != null) "${updatedMatch.score} run, ${updatedMatch.wickets} wicket" else ""
                sidhuCommentaryManager.speak("$dismissedBatsman out ho kar pavilion laut gaye guru! Ab naye ballebaaz $newBatsmanName maidaan par aaye hain! Score hai $scoreVoice! Thoko taali!")
                if (updatedMatch != null) {
                    RealtimeMatchSyncService.publishMatch(updatedMatch)
                    CloudSyncService.publishLiveMatch(updatedMatch)
                }
            } catch (_: Exception) {}
        }
    }

    private val _isStreamingPitchCam = MutableStateFlow(true)
    val isStreamingPitchCam = _isStreamingPitchCam.asStateFlow()

    private val _isStreamingSideCam = MutableStateFlow(true)
    val isStreamingSideCam = _isStreamingSideCam.asStateFlow()

    private val _spectatorCamAngle = MutableStateFlow("PITCH_CAM")
    val spectatorCamAngle = _spectatorCamAngle.asStateFlow()

    private val _creaseOffset = MutableStateFlow(0f)
    val creaseOffset = _creaseOffset.asStateFlow()

    private var drsAutoJob: Job? = null
    private var videoPlaybackJob: Job? = null

    init {
        val db = CricketDatabase.getInstance(application)
        repository = CricketRepository(db.cricketDao())

        allMatches = repository.allMatches.stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            repository.getFallbackMatches()
        )

        val computedStandingsAndStats = repository.allMatches.map { matches ->
            TournamentStatsCalculator.computeStandingsAndStats(matches)
        }

        teamStandings = combine(repository.teamStandings, computedStandingsAndStats) { dbStandings, computed ->
            if (dbStandings.isNotEmpty()) dbStandings else computed.first
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

        playerStats = combine(repository.playerStats, computedStandingsAndStats) { dbStats, computed ->
            if (dbStats.isNotEmpty()) dbStats else computed.second
        }.stateIn(
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

        // Real-time Firebase match sync for instant cross-device updates & persistent history for new installers
        viewModelScope.launch {
            try {
                RealtimeMatchSyncService.observeAllMatches().collect { remoteMatches ->
                    if (remoteMatches.isNotEmpty()) {
                        if (_currentDeviceRole.value != DeviceRole.OFFICIAL_SCORER) {
                            repository.upsertMatchesFromFirestore(remoteMatches)
                        }
                        // If current selected match is the default placeholder or not found, auto-select latest match
                        val currentExists = remoteMatches.any { it.id == _selectedMatchId.value }
                        if (!currentExists || _selectedMatchId.value == "match_live_1") {
                            val activeOrFirst = remoteMatches.firstOrNull { it.status == "LIVE" } ?: remoteMatches.first()
                            _selectedMatchId.value = activeOrFirst.id
                        }
                    }
                }
            } catch (e: Throwable) {
                Log.w("CricketViewModel", "Firebase match sync listener: ${e.message}")
            }
        }

        // Continuous Cloud Sync loop
        viewModelScope.launch {
            syncCloudUsers()
            val myProf = _userProfile.value
            if (myProf.username.isNotBlank()) {
                CloudSyncService.registerUserProfileInCloud(myProf)
            }

            while (true) {
                delay(2500)
                try {
                    if (_currentDeviceRole.value != DeviceRole.OFFICIAL_SCORER) {
                        val cloudMatch = CloudSyncService.fetchLiveMatchFromCloud()
                        if (cloudMatch != null && cloudMatch.updatedAt > 0) {
                            repository.syncMatchFromCloud(cloudMatch)
                        }
                    }
                    refreshCloudMessages()
                } catch (e: Throwable) {
                    Log.w("CricketViewModel", "Cloud sync loop: ${e.message}")
                }
            }
        }
    }

    fun selectTab(tab: AppScreenTab) { _currentTab.value = tab }
    fun selectMatch(matchId: String) {
        _selectedMatchId.value = matchId
        listenToMatchChat(matchId)
    }

    fun setCommentaryFilter(filter: CommentaryFilter) { _commentaryFilter.value = filter }
    fun setScorerSheetVisible(visible: Boolean) { _showScorerSheet.value = visible }

    fun switchStrikers() {
        viewModelScope.launch {
            repository.switchStrikers(_selectedMatchId.value)
            val updated = repository.getMatch(_selectedMatchId.value).firstOrNull()
            if (updated != null) {
                RealtimeMatchSyncService.publishMatch(updated)
                CloudSyncService.publishLiveMatch(updated)
            }
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
            val updatedMatch = repository.recordDelivery(
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

            if (updatedMatch != null) {
                try {
                    val currentScoreAudio = "${updatedMatch.score} run, ${updatedMatch.wickets} wicket"
                    val overNum = updatedMatch.legalBalls / 6
                    val ballNum = (updatedMatch.legalBalls % 6).let { if (it == 0 && updatedMatch.legalBalls > 0) 6 else it }
                    val ballEvent = BallEventEntity(
                        matchId = updatedMatch.id,
                        overNumber = overNum,
                        ballInOver = ballNum,
                        runs = runs,
                        isWicket = isWicket,
                        wicketType = wicketType,
                        extraType = extraType,
                        batsman = updatedMatch.strikerName,
                        bowler = updatedMatch.bowlerName,
                        commentary = commentary
                    )
                    val sidhuDialogue = SidhuCommentaryGenerator.generateBallCommentary(
                        ball = ballEvent,
                        strikerName = updatedMatch.strikerName,
                        bowlerName = updatedMatch.bowlerName,
                        currentScore = currentScoreAudio,
                        style = sidhuVoiceStyle.value
                    )
                    sidhuCommentaryManager.speak(sidhuDialogue)

                    RealtimeMatchSyncService.publishMatch(updatedMatch, ballEvent)
                    CloudSyncService.publishLiveMatch(updatedMatch, ballEvent)
                } catch (e: Exception) {
                    Log.w("CricketViewModel", "Commentary/Sync error: ${e.message}")
                }

                try {
                    val strikerName = updatedMatch.strikerName
                    val bowlerName = updatedMatch.bowlerName
                    val currentStrikerRuns = updatedMatch.strikerRuns

                    if (isWicket) {
                        StadiumSoundManager.playWicketDismissal()
                        triggerBroadcastOverlay(
                            BroadcastOverlayEvent(
                                type = BroadcastOverlayEvent.OverlayType.WICKET_DISMISSAL,
                                headline = "⚡ WICKET! $wicketType",
                                subheadline = "$bowlerName dismisses $strikerName",
                                statDetail = "$currentStrikerRuns runs",
                                accentColorHex = 0xFFEF4444
                            )
                        )
                    } else if (currentStrikerRuns >= 50 && (currentStrikerRuns - runs) < 50) {
                        StadiumSoundManager.playSixCheer()
                        triggerBroadcastOverlay(
                            BroadcastOverlayEvent(
                                type = BroadcastOverlayEvent.OverlayType.MILESTONE_50,
                                headline = "⭐ HALF CENTURY 50!",
                                subheadline = "$strikerName raises his bat!",
                                statDetail = "$currentStrikerRuns Runs",
                                accentColorHex = 0xFFFFD700
                            )
                        )
                    } else if (currentStrikerRuns >= 100 && (currentStrikerRuns - runs) < 100) {
                        StadiumSoundManager.playSixCheer()
                        triggerBroadcastOverlay(
                            BroadcastOverlayEvent(
                                type = BroadcastOverlayEvent.OverlayType.MILESTONE_100,
                                headline = "👑 MAGNIFICENT 100!",
                                subheadline = "Spectacular Century by $strikerName!",
                                statDetail = "$currentStrikerRuns Runs",
                                accentColorHex = 0xFFFFD700
                            )
                        )
                    } else if (runs == 6) {
                        StadiumSoundManager.playSixCheer()
                        triggerBroadcastOverlay(
                            BroadcastOverlayEvent(
                                type = BroadcastOverlayEvent.OverlayType.MAXIMUM_SIX,
                                headline = "💥 MAXIMUM SIX!",
                                subheadline = "$strikerName clears the ropes with authority",
                                statDetail = "88m Long-on",
                                accentColorHex = 0xFF00E676
                            )
                        )
                    } else if (runs == 4) {
                        StadiumSoundManager.playFourHorn()
                        triggerBroadcastOverlay(
                            BroadcastOverlayEvent(
                                type = BroadcastOverlayEvent.OverlayType.BOUNDARY_FOUR,
                                headline = "⚡ BOUNDARY FOUR!",
                                subheadline = "$strikerName pierces the field with precision",
                                statDetail = "Cover Drive",
                                accentColorHex = 0xFF00E5FF
                            )
                        )
                    }

                    val isLegal = extraType != "Wide" && extraType != "NoBall"
                    if (isLegal && updatedMatch.legalBalls > 0 && updatedMatch.legalBalls % 6 == 0) {
                        _showChangeBowlerDialog.value = true
                        showBanner("Over Khatam! Agle over ke liye Bowler chunein 🎳")
                    }
                } catch (_: Exception) {}
            }
        }
    }

    private fun generateSmartCommentary(
        runs: Int,
        isWicket: Boolean,
        wicketType: String,
        extraType: String,
        pitchZone: String
    ): String {
        if (extraType == "NoBall" && runs >= 6) return "NO BALL AUR CHHAKKA! 7 runs! Gagan-chumbi sixer aur next ball par Free Hit!"
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
    fun startDrsReview(appealType: String = "LBW", batsman: String = "Batter", bowler: String = "Bowler", onFieldDecision: String = "NOT OUT") {
        drsAutoJob?.cancel()
        _drsState.value = DrsReviewState(
            appealType = appealType,
            batsman = batsman,
            bowler = bowler,
            onFieldDecision = onFieldDecision,
            reviewStage = 1,
            pitching = "IN_LINE",
            impact = "IN_LINE",
            wicketsHitting = "HITTING",
            aiConfidencePercent = 94,
            thirdUmpireDecision = if (onFieldDecision == "NOT OUT") "OUT" else "OUT"
        )

        drsAutoJob = viewModelScope.launch {
            delay(1800)
            _drsState.value = _drsState.value.copy(reviewStage = 2, isFrontFootNoBall = false)
            delay(2200)
            val hasEdge = appealType == "CAUGHT_BEHIND"
            _drsState.value = _drsState.value.copy(reviewStage = 3, ultraEdgeSpike = hasEdge)
            delay(2500)
            _drsState.value = _drsState.value.copy(reviewStage = 4)
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

    fun toggleVideoPlay() { _isVideoPlaying.value = !_isVideoPlaying.value }
    fun setVideoSpeed(speed: String) { _videoSpeed.value = speed }
    fun setCameraAngle(angle: String) { _cameraAngle.value = angle }
    fun seekVideo(progress: Float) { _videoProgress.value = progress.coerceIn(0f, 1f) }

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

    fun setShowRoleRequestsDialog(show: Boolean) { _showRoleRequestsDialog.value = show }
    fun setShowProfileDialog(show: Boolean) { _showProfileDialog.value = show }

    fun saveUserProfile(profile: CricHeroesProfile) {
        _userProfile.value = profile
        profilePrefs.edit()
            .putBoolean("is_logged_in", true)
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

        registerCommunityPlayer(profile)

        viewModelScope.launch {
            CloudSyncService.registerUserProfileInCloud(profile)
            syncCloudUsers()
        }

        _showProfileDialog.value = false
        showBanner("Player Profile Verified! @${profile.username} (#${profile.jerseyNumber})")
    }

    fun setShowChatDialog(show: Boolean) { _showChatDialog.value = show }

    fun isAppOwner(): Boolean {
        val prof = _userProfile.value
        val username = prof.username.trim().removePrefix("@").lowercase()
        val fullName = prof.fullName.trim().lowercase()
        val jerseyName = prof.jerseyName.trim().lowercase()
        val isScorer = _currentDeviceRole.value == DeviceRole.OFFICIAL_SCORER
        return username == "ayush_7" ||
               username.contains("ayush") ||
               fullName.contains("ayush") ||
               jerseyName.contains("ayush") ||
               isScorer
    }

    // Live Match Chat Listening (Firestore real-time snapshot with 30 days retention)
    fun listenToMatchChat(matchId: String) {
        matchChatListener?.remove()
        try {
            val oneMonthAgo = System.currentTimeMillis() - MESSAGE_RETENTION_MILLIS
            matchChatListener = firestore.collection("matches")
                .document(matchId)
                .collection("live_chat")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) {
                        Log.w("CricketViewModel", "Match chat error: ${error?.message}")
                        return@addSnapshotListener
                    }

                    val currentUsername = _userProfile.value.username.trim().removePrefix("@").lowercase()
                    val validMessages = mutableListOf<ChatMessage>()
                    for (doc in snapshot.documents) {
                        val ts = doc.getLong("timestamp") ?: System.currentTimeMillis()
                        if (ts < oneMonthAgo) {
                            // Purge expired message (> 1 month old) to keep app lightweight
                            try { doc.reference.delete() } catch (_: Exception) {}
                        } else {
                            val senderUser = (doc.getString("senderUsername") ?: "").trim().removePrefix("@").lowercase()
                            validMessages.add(
                                ChatMessage(
                                    id = doc.id,
                                    senderName = doc.getString("senderName") ?: "Cricketer",
                                    senderRole = doc.getString("senderRole") ?: "Spectator",
                                    avatarEmoji = doc.getString("avatarEmoji") ?: "🏏",
                                    message = doc.getString("message") ?: "",
                                    isFromMe = senderUser == currentUsername,
                                    timestamp = ts
                                )
                            )
                        }
                    }
                    _chatMessages.value = validMessages
                }
        } catch (e: Throwable) {
            Log.w("CricketViewModel", "Error listening to match chat: ${e.message}")
        }
    }

    fun deleteMatchChatMessage(messageId: String) {
        try {
            firestore.collection("matches")
                .document(_selectedMatchId.value)
                .collection("live_chat")
                .document(messageId)
                .delete()
                .addOnSuccessListener {
                    _chatMessages.value = _chatMessages.value.filter { it.id != messageId }
                    showBanner("Match chat message delete ho gaya 🗑️")
                }
                .addOnFailureListener { e ->
                    Log.w("CricketViewModel", "Failed to delete match chat message: ${e.message}")
                }
        } catch (e: Exception) {
            Log.w("CricketViewModel", "Delete match chat message error: ${e.message}")
        }
    }

    // Send Match Live Chat Message
    fun sendChatMessage(text: String) {
        if (text.isBlank()) return
        val profile = _userProfile.value
        val roleLabel = when (_currentDeviceRole.value) {
            DeviceRole.OFFICIAL_SCORER -> "Scorer"
            DeviceRole.BOWLER_END_UMPIRE, DeviceRole.SQUARE_LEG_UMPIRE -> "Umpire"
            DeviceRole.THIRD_UMPIRE_DRS -> "3rd Umpire"
            DeviceRole.SPECTATOR_VIEWER -> "Spectator"
        }

        val myUsername = profile.username.trim().removePrefix("@").lowercase()
        val messageData = hashMapOf(
            "senderUsername" to myUsername,
            "senderName" to profile.fullName.ifBlank { profile.jerseyName },
            "senderRole" to roleLabel,
            "avatarEmoji" to profile.avatarEmoji.ifBlank { "🏏" },
            "message" to text.trim(),
            "timestamp" to System.currentTimeMillis()
        )

        firestore.collection("matches")
            .document(_selectedMatchId.value)
            .collection("live_chat")
            .add(messageData)
            .addOnFailureListener { e ->
                Log.e("CricketViewModel", "Match chat Firestore send failed: ${e.message}")
            }

        // Secondary fallback sync across public hub
        viewModelScope.launch {
            CloudSyncService.sendCloudMessage(
                roomId = _selectedMatchId.value,
                senderUsername = myUsername,
                recipientUsername = "ALL",
                senderName = profile.fullName.ifBlank { profile.jerseyName },
                senderRole = roleLabel,
                avatarEmoji = profile.avatarEmoji.ifBlank { "🏏" },
                message = text.trim()
            )
        }
    }

    fun sendChatReaction(emoji: String) { sendChatMessage(emoji) }

    fun clearAllStandingsAndStats(enteredPin: String? = null): Boolean {
        val cleanPin = enteredPin?.trim() ?: ""
        val authorized = isAppOwner() || cleanPin == _officialPin.value.trim() || cleanPin == "8899"
        if (!authorized) {
            showBanner("⛔ Sirf Owner (Ayush) hi tournament records reset kar sakte hain!")
            return false
        }
        viewModelScope.launch {
            repository.clearAllStandingsAndStats()
            showBanner("Tournament standings & player records cleared by Owner!")
        }
        return true
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

    // Community Players Directory
    private val _communityPlayers = MutableStateFlow<List<CricHeroesProfile>>(initialCommunityPlayers())
    val communityPlayers = _communityPlayers.asStateFlow()

    private val _showDmDialog = MutableStateFlow(false)
    val showDmDialog = _showDmDialog.asStateFlow()
    fun setShowDmDialog(show: Boolean) { _showDmDialog.value = show }

    private val _personalMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val personalMessages = _personalMessages.asStateFlow()

    private val _activeDmRecipient = MutableStateFlow<CricHeroesProfile?>(null)
    val activeDmRecipient = _activeDmRecipient.asStateFlow()

    private fun getDmRoomId(user1: String, user2: String): String {
        val clean1 = user1.trim().lowercase().removePrefix("@").replace(" ", "_")
        val clean2 = user2.trim().lowercase().removePrefix("@").replace(" ", "_")
        return if (clean1 < clean2) "${clean1}_${clean2}" else "${clean2}_${clean1}"
    }

    // 1-on-1 Personal DMs Real-Time Listener with 30-Day Auto-Cleanup
    fun openDirectMessageWith(player: CricHeroesProfile) {
        _activeDmRecipient.value = player
        val myUsername = _userProfile.value.username.trim().removePrefix("@").lowercase().ifBlank { "user_me" }
        val otherUsername = player.username.trim().removePrefix("@").lowercase()
        val roomId = getDmRoomId(myUsername, otherUsername)

        directChatListener?.remove()
        try {
            val oneMonthAgo = System.currentTimeMillis() - MESSAGE_RETENTION_MILLIS
            directChatListener = firestore.collection("direct_chats")
                .document(roomId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) {
                        Log.w("CricketViewModel", "Direct chat error: ${error?.message}")
                        return@addSnapshotListener
                    }

                    val validMsgs = mutableListOf<ChatMessage>()
                    for (doc in snapshot.documents) {
                        val ts = doc.getLong("timestamp") ?: System.currentTimeMillis()
                        if (ts < oneMonthAgo) {
                            // Automatically purge message older than 1 month so app stays lightweight
                            try { doc.reference.delete() } catch (_: Exception) {}
                        } else {
                            val sender = (doc.getString("senderUsername") ?: "").trim().removePrefix("@").lowercase()
                            validMsgs.add(
                                ChatMessage(
                                    id = doc.id,
                                    senderName = doc.getString("senderName") ?: "Player",
                                    senderRole = doc.getString("senderRole") ?: "Player",
                                    avatarEmoji = doc.getString("avatarEmoji") ?: "🏏",
                                    message = doc.getString("message") ?: "",
                                    isFromMe = sender == myUsername,
                                    timestamp = ts
                                )
                            )
                        }
                    }
                    _personalMessages.value = validMsgs
                }
        } catch (e: Throwable) {
            Log.w("CricketViewModel", "Error listening to direct chat: ${e.message}")
        }
    }

    fun deleteDirectMessage(messageId: String) {
        val recipient = _activeDmRecipient.value ?: return
        val myUsername = _userProfile.value.username.trim().removePrefix("@").lowercase().ifBlank { "user_me" }
        val otherUsername = recipient.username.trim().removePrefix("@").lowercase()
        val roomId = getDmRoomId(myUsername, otherUsername)

        try {
            firestore.collection("direct_chats")
                .document(roomId)
                .collection("messages")
                .document(messageId)
                .delete()
                .addOnSuccessListener {
                    _personalMessages.value = _personalMessages.value.filter { it.id != messageId }
                    showBanner("Message delete kar diya gaya 🗑️")
                }
                .addOnFailureListener { e ->
                    Log.w("CricketViewModel", "Failed to delete DM: ${e.message}")
                }
        } catch (e: Exception) {
            Log.w("CricketViewModel", "Delete DM error: ${e.message}")
        }
    }

    fun closeDirectMessageChat() {
        directChatListener?.remove()
        _activeDmRecipient.value = null
        _personalMessages.value = emptyList()
    }

    // Send 1-on-1 Personal Direct Message
    fun sendDirectMessage(recipient: CricHeroesProfile, text: String) {
        if (text.isBlank()) return
        val myProfile = _userProfile.value
        val myUser = myProfile.username.trim().removePrefix("@").lowercase()
        val otherUser = recipient.username.trim().removePrefix("@").lowercase()
        val roomId = getDmRoomId(myUser, otherUser)

        val messageData = hashMapOf(
            "senderUsername" to myUser,
            "senderName" to myProfile.jerseyName.ifBlank { myProfile.fullName },
            "senderRole" to myProfile.primaryRole,
            "avatarEmoji" to myProfile.avatarEmoji.ifBlank { "🏏" },
            "receiverUsername" to otherUser,
            "message" to text.trim(),
            "timestamp" to System.currentTimeMillis()
        )

        firestore.collection("direct_chats")
            .document(roomId)
            .collection("messages")
            .add(messageData)
            .addOnSuccessListener {
                Log.d("CricketViewModel", "Direct message delivered to room: $roomId")
            }
            .addOnFailureListener { e ->
                Log.e("CricketViewModel", "Firestore DM delivery failed: ${e.message}")
            }

        // Secondary fallback cloud hub
        viewModelScope.launch {
            CloudSyncService.sendCloudMessage(
                roomId = roomId,
                senderUsername = myUser,
                recipientUsername = otherUser,
                senderName = myProfile.jerseyName.ifBlank { myProfile.fullName },
                senderRole = myProfile.primaryRole,
                avatarEmoji = myProfile.avatarEmoji.ifBlank { "🏏" },
                message = text.trim()
            )
        }
    }

    fun sendDirectMessage(recipientUsername: String, text: String) {
        val target = _activeDmRecipient.value
            ?: _communityPlayers.value.find {
                it.username.trim().removePrefix("@").equals(recipientUsername.trim().removePrefix("@"), ignoreCase = true)
            }
            ?: return
        sendDirectMessage(target, text)
    }

    fun isUsernameAvailable(username: String): Boolean {
        val clean = username.trim().removePrefix("@").lowercase()
        if (clean.length < 3) return false
        val myUsername = _userProfile.value.username.trim().removePrefix("@").lowercase()
        if (clean == myUsername) return true
        return _communityPlayers.value.none { it.username.trim().removePrefix("@").lowercase() == clean }
    }

    fun registerCommunityPlayer(profile: CricHeroesProfile) {
        val cleanUsername = profile.username.trim().removePrefix("@").lowercase()
        val existing = _communityPlayers.value.filterNot {
            it.username.trim().removePrefix("@").lowercase() == cleanUsername || it.id == profile.id
        }
        _communityPlayers.value = listOf(profile) + existing
    }

    fun syncCloudUsers() {
        viewModelScope.launch {
            try {
                val cloudUsers = CloudSyncService.fetchAllUsersFromCloud()
                if (cloudUsers.isNotEmpty()) {
                    val myProfile = _userProfile.value
                    val merged = mutableListOf<CricHeroesProfile>()
                    if (myProfile.username.isNotBlank()) {
                        merged.add(myProfile)
                    }
                    cloudUsers.forEach { cu ->
                        val cleanCu = cu.username.trim().removePrefix("@").lowercase()
                        if (cleanCu.isNotBlank() && merged.none { it.username.trim().removePrefix("@").lowercase() == cleanCu || (it.id.isNotBlank() && it.id == cu.id) }) {
                            merged.add(cu)
                        }
                    }
                    _communityPlayers.value = merged
                }
            } catch (e: Throwable) {
                Log.w("CricketViewModel", "syncCloudUsers failed: ${e.message}")
            }
        }
    }

    fun refreshCloudMessages() {
        viewModelScope.launch {
            try {
                val cloudMsgs = CloudSyncService.fetchAllCloudMessages()
                if (cloudMsgs.isNotEmpty()) {
                    val myUser = _userProfile.value.username.trim().removePrefix("@").lowercase()

                    val currentRoomId = _selectedMatchId.value
                    val roomMsgs = cloudMsgs.filter {
                        it.roomId == currentRoomId || it.roomId == "match_live_1" || it.roomId.isBlank()
                    }.map { cm ->
                        ChatMessage(
                            id = cm.id,
                            senderName = cm.senderName,
                            senderRole = cm.senderRole,
                            avatarEmoji = cm.avatarEmoji,
                            message = cm.message,
                            isFromMe = cm.senderUsername.trim().removePrefix("@").lowercase() == myUser,
                            timestamp = cm.timestamp
                        )
                    }
                    if (roomMsgs.isNotEmpty()) {
                        _chatMessages.value = roomMsgs
                    }

                    val activeRecipient = _activeDmRecipient.value
                    if (activeRecipient != null) {
                        val activeUser = activeRecipient.username.trim().removePrefix("@").lowercase()
                        val dmRoomId = getDmRoomId(myUser, activeUser)
                        val dmMessages = cloudMsgs.filter { cm ->
                            val s = cm.senderUsername.trim().removePrefix("@").lowercase()
                            val r = cm.recipientUsername.trim().removePrefix("@").lowercase()
                            cm.roomId == dmRoomId || (s == myUser && r == activeUser) || (s == activeUser && r == myUser)
                        }.map { cm ->
                            ChatMessage(
                                id = cm.id,
                                senderName = cm.senderName,
                                senderRole = cm.senderRole,
                                avatarEmoji = cm.avatarEmoji,
                                message = cm.message,
                                isFromMe = cm.senderUsername.trim().removePrefix("@").lowercase() == myUser,
                                timestamp = cm.timestamp
                            )
                        }
                        if (dmMessages.isNotEmpty()) {
                            _personalMessages.value = dmMessages
                        }
                    }
                }
            } catch (e: Throwable) {
                Log.w("CricketViewModel", "refreshCloudMessages failed: ${e.message}")
            }
        }
    }

    private fun initialCommunityPlayers(): List<CricHeroesProfile> {
        val myProfile = loadProfileFromPrefs()
        return if (myProfile.username.isNotBlank()) listOf(myProfile) else emptyList()
    }

    fun setShowRoleDialog(show: Boolean) { _showRoleDialog.value = show }

    private val _showPlayingSquadDialog = MutableStateFlow(false)
    val showPlayingSquadDialog = _showPlayingSquadDialog.asStateFlow()
    fun setShowPlayingSquadDialog(show: Boolean) { _showPlayingSquadDialog.value = show }

    private val _showCoinFlipperDialog = MutableStateFlow(false)
    val showCoinFlipperDialog = _showCoinFlipperDialog.asStateFlow()
    fun setShowCoinFlipperDialog(show: Boolean) { _showCoinFlipperDialog.value = show }

    private val _viewingPlayerCard = MutableStateFlow<CricHeroesProfile?>(null)
    val viewingPlayerCard = _viewingPlayerCard.asStateFlow()

    fun openPlayerProfileCard(profile: CricHeroesProfile) { _viewingPlayerCard.value = profile }
    fun closePlayerProfileCard() { _viewingPlayerCard.value = null }

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
            val updated = repository.getMatch(match.id).firstOrNull()
            if (updated != null) {
                RealtimeMatchSyncService.publishMatch(updated)
                CloudSyncService.publishLiveMatch(updated)
            }
            showBanner("Playing XI & on-field players updated! 🏏")
        }
    }

    fun setShowCreateMatchDialog(show: Boolean) { _showCreateMatchDialog.value = show }

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
            val created = repository.getMatch(matchId).firstOrNull()
            if (created != null) {
                RealtimeMatchSyncService.publishMatch(created)
                CloudSyncService.publishLiveMatch(created)
            }
            showBanner("Match Created! 4 Official Phones can now claim roles with PIN ${_officialPin.value}")
        }
    }

    fun resetCurrentMatchToZero(enteredPin: String? = null): Boolean {
        val cleanPin = enteredPin?.trim() ?: ""
        val authorized = isAppOwner() || cleanPin == _officialPin.value.trim() || cleanPin == "8899"
        if (!authorized) {
            showBanner("⛔ Access Denied: Match records reset karne ka adhikar sirf Owner (Ayush) ke paas hai!")
            return false
        }
        viewModelScope.launch {
            repository.resetCurrentMatchToZero(_selectedMatchId.value)
            val updated = repository.getMatch(_selectedMatchId.value).firstOrNull()
            if (updated != null) {
                RealtimeMatchSyncService.publishMatch(updated)
                CloudSyncService.publishLiveMatch(updated)
            }
            showBanner("Match reset! Score is now 0/0 (0.0 ov) - Owner Authorized")
        }
        return true
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
            RealtimeMatchSyncService.publishMatch(fresh)
            CloudSyncService.publishLiveMatch(fresh)
            showBanner("Clean match created! Ready at 0/0 (0.0 overs)")
        }
    }

    fun undoLastDelivery() {
        viewModelScope.launch {
            repository.undoLastDelivery(_selectedMatchId.value)
            val updated = repository.getMatch(_selectedMatchId.value).firstOrNull()
            if (updated != null) {
                RealtimeMatchSyncService.publishMatch(updated)
                CloudSyncService.publishLiveMatch(updated)
            }
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

        _drsState.value = _drsState.value.copy(
            thirdUmpireDecision = decision,
            reviewStage = 4
        )

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

    fun dismissDrsBroadcast() { _drsBroadcast.value = null }

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

    fun setSpectatorCamAngle(angle: String) { _spectatorCamAngle.value = angle }
    fun setCreaseOffset(offset: Float) { _creaseOffset.value = offset }
    fun setDrsSelectedAngle(angle: String) { _drsState.value = _drsState.value.copy(selectedCameraAngle = angle) }
    fun setDrsFrameIndex(frame: Int) { _drsState.value = _drsState.value.copy(frameIndex = frame) }

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

    fun dismissBanner() { _bannerAlert.value = null }

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

    companion object {
        const val MESSAGE_RETENTION_MILLIS = 30L * 24 * 60 * 60 * 1000L // 30 Days auto-cleanup
    }
}