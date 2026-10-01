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
import com.example.data.audio.SmartGeminiCommentaryService
import com.example.data.audio.StadiumSoundManager
import com.example.data.chat.RealChatRepository
import com.example.data.firebase.RealtimeMatchSyncService
import com.example.data.firebase.UserDirectorySyncService
import com.example.data.local.CricketDatabase
import com.example.data.model.BallEventEntity
import com.example.data.model.BroadcastOverlayEvent
import com.example.data.model.ChatMessage
import com.example.data.model.CricHeroesProfile
import com.example.data.model.DeviceRole
import com.example.data.model.DrsBroadcastAlert
import com.example.data.model.DrsDecisionCardData
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
    HAWKEYE_RADAR,
    MATCH_DETAIL
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

    private val scoringMutex = Mutex()
    private val chatRepository = RealChatRepository()
    private var matchChatJob: Job? = null
    private var directChatJob: Job? = null

    private val repository: CricketRepository
    val allMatches: StateFlow<List<MatchEntity>>
    val teamStandings: StateFlow<List<TeamStandingEntity>>
    val playerStats: StateFlow<List<PlayerStatEntity>>
    val notifications: StateFlow<List<NotificationAlertEntity>>

    private val _selectedMatchId = MutableStateFlow("")
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

    fun initTtsCommentary() {
        viewModelScope.launch(Dispatchers.Default) {
            sidhuCommentaryManager.initializeTts()
        }
    }

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
            val res = SmartGeminiCommentaryService.testAllApiKeys(key)
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

    private val _viewingMatchDetailId = MutableStateFlow<String?>(null)
    val viewingMatchDetailId = _viewingMatchDetailId.asStateFlow()

    fun openMatchDetail(matchId: String) {
        _selectedMatchId.value = matchId
        _viewingMatchDetailId.value = matchId
    }

    fun closeMatchDetail() {
        _viewingMatchDetailId.value = null
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

    fun recordQuickRun(runs: Int) {
        val curr = currentMatch.value
        if (curr?.status == "FINISHED") {
            showBanner("🏆 Match samapt ho chuka hai! Ab scoring band hai.")
            return
        }
        if (curr?.status == "INNINGS_BREAK" || (curr?.currentInnings == 1 && curr.legalBalls >= curr.totalOvers * 6)) {
            showBanner("🏏 1st Innings samapt! 'Start 2nd Innings' par tap karke agali paari shuru karein.")
            _showStartSecondInningsDialog.value = true
            return
        }
        recordBall(runs = runs)
    }

    fun recordQuickWicket() {
        val curr = currentMatch.value
        if (curr?.status == "FINISHED") {
            showBanner("🏆 Match samapt ho chuka hai! Ab scoring band hai.")
            return
        }
        if (curr?.status == "INNINGS_BREAK" || (curr?.currentInnings == 1 && curr.legalBalls >= curr.totalOvers * 6)) {
            showBanner("🏏 1st Innings samapt! 'Start 2nd Innings' par tap karke agali paari shuru karein.")
            _showStartSecondInningsDialog.value = true
            return
        }
        recordBall(runs = 0, isWicket = true, wicketType = "Out")
    }

    fun recordQuickExtra(extraType: String = "Wide") {
        val curr = currentMatch.value
        if (curr?.status == "FINISHED") {
            showBanner("🏆 Match samapt ho chuka hai! Ab scoring band hai.")
            return
        }
        if (curr?.status == "INNINGS_BREAK" || (curr?.currentInnings == 1 && curr.legalBalls >= curr.totalOvers * 6)) {
            showBanner("🏏 1st Innings samapt! 'Start 2nd Innings' par tap karke agali paari shuru karein.")
            _showStartSecondInningsDialog.value = true
            return
        }
        recordBall(runs = 0, extraType = extraType)
    }

    // DRS Review State
    private val _drsState = MutableStateFlow(DrsReviewState())
    val drsState = _drsState.asStateFlow()

    private val _drsDecisionPopup = MutableStateFlow<DrsDecisionCardData?>(null)
    val drsDecisionPopup = _drsDecisionPopup.asStateFlow()
    private val dismissedDrsDecisionIds = java.util.Collections.synchronizedSet(mutableSetOf<String>())
    fun dismissDrsDecisionPopup() {
        val currId = _drsDecisionPopup.value?.id
        if (currId != null) {
            dismissedDrsDecisionIds.add(currId)
        }
        lastHandledDrsAppealTs = System.currentTimeMillis()
        _drsDecisionPopup.value = null
        _drsBroadcast.value = null
        resetDrs()
        if (_selectedMatchId.value.isNotBlank()) {
            RealtimeMatchSyncService.clearDrsDecision(_selectedMatchId.value)
            RealtimeMatchSyncService.clearDrsAppeal(_selectedMatchId.value)
        }
        _currentTab.value = AppScreenTab.LIVE_CENTER
    }

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

    private val _liveMatchAlert = MutableStateFlow<MatchEntity?>(null)
    val liveMatchAlert = _liveMatchAlert.asStateFlow()
    fun dismissLiveMatchAlert() { _liveMatchAlert.value = null }
    private var lastAlertedMatchId: String = ""
    private var lastHandledDrsAppealTs: Long = System.currentTimeMillis()
    private var lastRemoteSpokenBallTs: Long = 0L
    private var commentaryBallCounter: Int = 0
    private var consecutiveBowlerWickets: Int = 0
    private var lastWicketBowlerName: String = ""
    private var wicketsInCurrentOver: Int = 0
    private var currentOverTracked: Int = -1
    private var lastNotifiedDmMsgId: String = ""

    private val _isDmMuted = MutableStateFlow(com.example.notification.MatchNotificationHelper.isDmMuted(application))
    val isDmMuted: StateFlow<Boolean> = _isDmMuted.asStateFlow()

    fun toggleDmMute() {
        val newMute = !_isDmMuted.value
        _isDmMuted.value = newMute
        com.example.notification.MatchNotificationHelper.setDmMuted(getApplication(), newMute)
        showBanner(if (newMute) "🔕 DM Notifications Mute ho gaye" else "🔔 DM Notifications Chalu ho gaye")
    }

    private val _isMatchStartMuted = MutableStateFlow(com.example.notification.MatchNotificationHelper.isMatchStartMuted(application))
    val isMatchStartMuted: StateFlow<Boolean> = _isMatchStartMuted.asStateFlow()

    fun toggleMatchStartMute() {
        val newMute = !_isMatchStartMuted.value
        _isMatchStartMuted.value = newMute
        com.example.notification.MatchNotificationHelper.setMatchStartMuted(getApplication(), newMute)
        showBanner(if (newMute) "🔕 Match Start Notifications Mute ho gaye" else "🔔 Match Start Notifications Chalu ho gaye")
    }

    private val rolePrefs = application.getSharedPreferences("ayuu_device_roles", Context.MODE_PRIVATE)
    private val savedRole = try {
        DeviceRole.valueOf(rolePrefs.getString("active_role", DeviceRole.SPECTATOR_VIEWER.name) ?: DeviceRole.SPECTATOR_VIEWER.name)
    } catch (_: Exception) {
        DeviceRole.SPECTATOR_VIEWER
    }

    private val _currentDeviceRole = MutableStateFlow(savedRole)
    val currentDeviceRole = _currentDeviceRole.asStateFlow()

    private val _officialPin = MutableStateFlow(
        rolePrefs.getString("admin_pin", "200910")?.let {
            if (it == "1234" || it == "0007" || it.isBlank()) "200910" else it
        } ?: "200910"
    )
    val officialPin = _officialPin.asStateFlow()

    private val _isAuthorizedOfficial = MutableStateFlow(savedRole != DeviceRole.SPECTATOR_VIEWER)
    val isAuthorizedOfficial = _isAuthorizedOfficial.asStateFlow()

    private val _roleRequests = MutableStateFlow<List<RoleChangeRequest>>(emptyList())
    val roleRequests = _roleRequests.asStateFlow()

    private val _showRoleRequestsDialog = MutableStateFlow(false)
    val showRoleRequestsDialog = _showRoleRequestsDialog.asStateFlow()

    private val _isRefreshingStandings = MutableStateFlow(false)
    val isRefreshingStandings = _isRefreshingStandings.asStateFlow()

    private val profilePrefs = application.getSharedPreferences("ayuu_cricheroes_profile", Context.MODE_PRIVATE)

    fun isUserLoggedIn(): Boolean {
        val authUser = try { com.google.firebase.auth.FirebaseAuth.getInstance().currentUser } catch (_: Throwable) { null }
        val hasLocalProfile = !profilePrefs.getString("name", "").isNullOrBlank()
        return authUser != null || (hasLocalProfile && profilePrefs.getBoolean("is_logged_in", false))
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
        try {
            com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
        } catch (e: Throwable) {
            Log.e("CricketViewModel", "Firebase sign out error: ${e.message}", e)
        }
        profilePrefs.edit().clear().apply()
        _userProfile.value = loadProfileFromPrefs()
        _communityPlayers.value = emptyList()
        _showLoginScreen.value = true
        showBanner("Logged out successfully.")
    }

    private val _showWhatsAppShareDialog = MutableStateFlow(false)
    val showWhatsAppShareDialog = _showWhatsAppShareDialog.asStateFlow()
    fun setShowWhatsAppShareDialog(show: Boolean) { _showWhatsAppShareDialog.value = show }

    private val _showEditVenueDialog = MutableStateFlow(false)
    val showEditVenueDialog = _showEditVenueDialog.asStateFlow()

    private val _editingMatchVenue = MutableStateFlow<MatchEntity?>(null)
    val editingMatchVenue = _editingMatchVenue.asStateFlow()

    fun openEditVenueDialog(match: MatchEntity? = currentMatch.value) {
        if (match == null) return
        _editingMatchVenue.value = match
        _showEditVenueDialog.value = true
    }

    fun closeEditVenueDialog() {
        _showEditVenueDialog.value = false
        _editingMatchVenue.value = null
    }

    private val _isSearchingGrounds = MutableStateFlow(false)
    val isSearchingGrounds = _isSearchingGrounds.asStateFlow()

    private val _groundSearchResults = MutableStateFlow<List<com.example.data.maps.GroundVenueInfo>>(com.example.data.maps.GoogleMapsGroundingService.POPULAR_GROUNDS)
    val groundSearchResults = _groundSearchResults.asStateFlow()

    fun searchGroundsWithMaps(query: String) {
        viewModelScope.launch {
            _isSearchingGrounds.value = true
            try {
                val effectiveApiKey = customApiKey.value.ifBlank { null }
                val results = com.example.data.maps.GoogleMapsGroundingService.searchGroundsWithMapsGrounding(query, effectiveApiKey)
                _groundSearchResults.value = results
            } catch (e: Exception) {
                Log.w("CricketViewModel", "searchGroundsWithMaps error: ${e.message}")
            } finally {
                _isSearchingGrounds.value = false
            }
        }
    }

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
            appUpdateManager.checkForUpdates()
        }
        viewModelScope.launch {
            appUpdateManager.updateState.collect { state ->
                if (state.isUpdateAvailable) {
                    _showUpdateDialog.value = true
                }
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
            val dialogue = "ओए बॉलिंग चेंज गुरु! अब गेंदबाज़ी का मोर्चा संभालेंगे $newBowlerName! देखते हैं क्या गुल खिलाते हैं, ठोको ताली!"
            scoringMutex.withLock {
                repository.updateNewBowler(_selectedMatchId.value, newBowlerName)
                val updated = repository.getMatch(_selectedMatchId.value).firstOrNull()
                if (updated != null) {
                    sidhuCommentaryManager.speak(dialogue)
                    lastRemoteSpokenBallTs = System.currentTimeMillis() + 1500L
                    RealtimeMatchSyncService.publishMatch(updated, sidhuDialogue = dialogue)
                }
            }
            showBanner("🎳 Naye Bowler: $newBowlerName attack par aaye hain!")
        }
    }

    fun changeBatsman(isStriker: Boolean, newName: String) {
        viewModelScope.launch {
            scoringMutex.withLock {
                repository.updateNewBatsman(_selectedMatchId.value, newName, isStriker)
                val updated = repository.getMatch(_selectedMatchId.value).firstOrNull()
                if (updated != null) {
                    RealtimeMatchSyncService.publishMatch(updated)
                }
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
            scoringMutex.withLock {
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
                    val scoreVoice = if (updatedMatch != null) "${updatedMatch.score} रन, ${updatedMatch.wickets} विकेट" else ""
                    val dialogue = "ओए गुरु! $dismissedBatsman आउट होकर पवेलियन लौट गए! अब नए बल्लेबाज़ $newBatsmanName मैदान पर आए हैं! स्कोर है $scoreVoice! ठोको ताली गुरु!"
                    sidhuCommentaryManager.speak(dialogue)
                    lastRemoteSpokenBallTs = System.currentTimeMillis() + 1500L
                    if (updatedMatch != null) {
                        RealtimeMatchSyncService.publishMatch(updatedMatch, sidhuDialogue = dialogue)
                    }
                } catch (e: Exception) {
                    Log.w("CricketViewModel", "Error in post-wicket sound or publish: ${e.message}", e)
                }
            }
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
            SharingStarted.WhileSubscribed(5000),
            repository.getFallbackMatches()
        )

        val computedStandingsAndStats = repository.allMatches.map { matches ->
            TournamentStatsCalculator.computeStandingsAndStats(matches)
        }.flowOn(Dispatchers.Default)

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
            if (id.isBlank()) kotlinx.coroutines.flow.flowOf(null)
            else repository.getMatch(id)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

        @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
        currentBallEvents = _selectedMatchId.flatMapLatest { id ->
            if (id.isBlank()) kotlinx.coroutines.flow.flowOf(emptyList())
            else repository.getBallEvents(id)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        highlightClips = repository.getHighlightClips()
        _selectedClip.value = highlightClips.firstOrNull()

        viewModelScope.launch(Dispatchers.IO) {
            repository.initializeDefaultDataIfEmpty()
            checkPeriodicUpdateReminder()
            syncMatchesFromCloud()
            listenToCloudStandings()

            // Restore user profile from Firestore if logged in
            val authUser = try { com.google.firebase.auth.FirebaseAuth.getInstance().currentUser } catch (_: Throwable) { null }
            if (authUser != null) {
                val email = authUser.email?.trim()?.lowercase().orEmpty()
                val isAyush = email == "ayushsunil591983@gmail.com" || email.contains("ayush")
                val restored = UserDirectorySyncService.fetchUserProfile(authUser.uid)
                    ?: if (isAyush) UserDirectorySyncService.fetchUserProfileByUsername("ayush_7") else null
                if (restored != null) {
                    val finalProfile = restored.copy(
                        id = authUser.uid,
                        uid = authUser.uid,
                        isVerified = true,
                        isGuest = false
                    )
                    saveUserProfile(finalProfile)
                }
            }
        }

        // Auto-select latest active match or clear if empty (filter out dummy matches)
        viewModelScope.launch(Dispatchers.Default) {
            allMatches.collect { rawMatches ->
                val matches = rawMatches.filter { it.id != "match_live_1" && !it.id.startsWith("dummy") && !it.id.startsWith("sample") }
                if (matches.isNotEmpty()) {
                    val currentExists = matches.any { it.id == _selectedMatchId.value }
                    if (!currentExists || _selectedMatchId.value.isBlank() || _selectedMatchId.value == "match_live_1") {
                        val activeOrFirst = matches.firstOrNull { it.status == "LIVE" } ?: matches.firstOrNull()
                        if (activeOrFirst != null) {
                            _selectedMatchId.value = activeOrFirst.id
                        }
                    }
                } else {
                    _selectedMatchId.value = ""
                }
            }
        }

        // Live Score Widget and Push Notification auto-updater
        viewModelScope.launch(Dispatchers.Default) {
            currentMatch.collect { match ->
                if (match != null && match.id != "match_live_1" && !match.id.startsWith("dummy") && !match.id.startsWith("sample")) {
                    com.example.widget.LiveScoreAppWidgetProvider.updateAllWidgets(getApplication(), match)
                } else {
                    com.example.widget.LiveScoreAppWidgetProvider.updateAllWidgets(getApplication(), null)
                }
            }
        }

        // Observe selected match ID for match chat updates
        viewModelScope.launch(Dispatchers.Default) {
            _selectedMatchId.collect { id ->
                listenToMatchChat(id)
            }
        }

        // Real-time Firebase match sync for instant cross-device updates (filter out dummy matches)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                RealtimeMatchSyncService.observeAllMatches().collect { rawRemoteMatches ->
                    val remoteMatches = rawRemoteMatches.filter { it.id != "match_live_1" && !it.id.startsWith("dummy") && !it.id.startsWith("sample") }
                    if (remoteMatches.isNotEmpty()) {
                        // Always upsert remote matches to Room so every phone has the live matches
                        repository.upsertMatchesFromFirestore(remoteMatches)

                        // Find the newest LIVE match by timestamp
                        val latestLiveMatch = remoteMatches.filter { it.status == "LIVE" }
                            .maxByOrNull { match ->
                                match.id.removePrefix("match_local_").toLongOrNull() ?: 0L
                            }

                        if (latestLiveMatch != null) {
                            val currentMatchObj = remoteMatches.firstOrNull { it.id == _selectedMatchId.value }
                            val currentTs = _selectedMatchId.value.removePrefix("match_local_").toLongOrNull() ?: 0L
                            val newTs = latestLiveMatch.id.removePrefix("match_local_").toLongOrNull() ?: 0L

                            // If no match selected, or current match is completed/not LIVE, or a newer LIVE match has started
                            if (_selectedMatchId.value.isBlank() || currentMatchObj == null || currentMatchObj.status != "LIVE" || newTs > currentTs) {
                                if (_selectedMatchId.value != latestLiveMatch.id) {
                                    _selectedMatchId.value = latestLiveMatch.id
                                    Log.d("CricketViewModel", "Auto-switched to latest live match: ${latestLiveMatch.id} (${latestLiveMatch.teamA} vs ${latestLiveMatch.teamB})")
                                }
                            }
                        } else {
                            val currentExists = remoteMatches.any { it.id == _selectedMatchId.value }
                            if (!currentExists || _selectedMatchId.value.isBlank()) {
                                val activeOrFirst = remoteMatches.maxByOrNull { it.id.removePrefix("match_local_").toLongOrNull() ?: 0L } ?: remoteMatches.first()
                                _selectedMatchId.value = activeOrFirst.id
                            }
                        }
                    }
                }
            } catch (e: Throwable) {
                Log.w("CricketViewModel", "Firebase match sync listener: ${e.message}")
            }
        }

        // Real-time DRS Appeal Voice Broadcast across all phones (Sidhu Paaji Announcement)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
                _selectedMatchId.flatMapLatest { id ->
                    RealtimeMatchSyncService.observeDrsAppeal(id)
                }.collect { appealPair ->
                    if (appealPair != null) {
                        val (appealType, ts) = appealPair
                        val isRecent = (System.currentTimeMillis() - ts) < 12_000L
                        if (ts > lastHandledDrsAppealTs && isRecent && _drsState.value.thirdUmpireDecision.isBlank()) {
                            lastHandledDrsAppealTs = ts
                            // Sidhu Paaji announces loud and clear to everyone!
                            sidhuCommentaryManager.speak("ओए गुरु! मैदान पर DRS ले लिया गया है! चक दे फट्टे, अब तीसरा अंपायर फैसला करेगा! दूध का दूध और पानी का पानी होने वाला है, ठोको ताली गुरु!")
                            com.example.notification.MatchNotificationHelper.notifyDrsTaken(
                                getApplication(),
                                appealType,
                                currentMatch.value?.strikerName ?: "Batter",
                                currentMatch.value?.bowlerName ?: "Bowler"
                            )
                            _drsState.value = _drsState.value.copy(
                                appealType = appealType,
                                reviewStage = 1,
                                thirdUmpireDecision = ""
                            )
                        }
                    }
                }
            } catch (e: Throwable) {
                Log.w("CricketViewModel", "DRS appeal sync listener: ${e.message}")
            }
        }

        // Real-time DRS Decision events sync across all devices
        viewModelScope.launch(Dispatchers.IO) {
            try {
                @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
                _selectedMatchId.flatMapLatest { id ->
                    RealtimeMatchSyncService.observeDrsDecisions(id)
                }.collect { incomingCard ->
                    if (incomingCard != null && !dismissedDrsDecisionIds.contains(incomingCard.id)) {
                        if (System.currentTimeMillis() - incomingCard.timestamp < 10_000L) {
                            _drsDecisionPopup.value = incomingCard
                        }
                    }
                }
            } catch (e: Throwable) {
                Log.w("CricketViewModel", "DRS sync listener: ${e.message}")
            }
        }

        // Real-time Sidhu Paaji live commentary broadcast on other phones (Spectators / Umpires / Viewers)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
                _selectedMatchId.flatMapLatest { id ->
                    RealtimeMatchSyncService.observeLiveBallAudio(id)
                }.collect { audioPair ->
                    if (audioPair != null) {
                        val (dialogue, ts) = audioPair
                        if (ts > lastRemoteSpokenBallTs) {
                            lastRemoteSpokenBallTs = ts
                            if (isSidhuCommentaryEnabled.value) {
                                sidhuCommentaryManager.speak(dialogue)
                            }
                        }
                    }
                }
            } catch (e: Throwable) {
                Log.w("CricketViewModel", "Live ball commentary audio sync listener: ${e.message}")
            }
        }

        // Real-time Firestore community player directory synchronization
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val myProf = _userProfile.value
                if (myProf.username.isNotBlank()) {
                    UserDirectorySyncService.registerUserProfile(myProf)
                }
                UserDirectorySyncService.observeCommunityUsers().collect { remoteUsers ->
                    if (remoteUsers.isNotEmpty()) {
                        val currentMyProfile = _userProfile.value
                        val merged = mutableListOf<CricHeroesProfile>()
                        if (currentMyProfile.username.isNotBlank()) {
                            merged.add(currentMyProfile)
                        }
                        remoteUsers.forEach { cu ->
                            val cleanCu = cu.username.trim().removePrefix("@").lowercase()
                            val isDuplicate = merged.any {
                                (cleanCu.isNotBlank() && it.username.trim().removePrefix("@").lowercase() == cleanCu) ||
                                it.uid == cu.uid ||
                                (it.fullName.trim().equals(cu.fullName.trim(), ignoreCase = true) && it.jerseyNumber == cu.jerseyNumber && it.jerseyNumber > 0)
                            }
                            if (!isDuplicate) {
                                merged.add(cu)
                            }
                        }
                        _communityPlayers.value = merged
                    }
                }
            } catch (e: Throwable) {
                Log.w("CricketViewModel", "Firestore user directory sync: ${e.message}")
            }
        }

        // Real-time Direct Chat unread badges & red dot listener across all conversations
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                firestore.collection("direct_chats").addSnapshotListener { snapshots, error ->
                    if (error != null || snapshots == null) return@addSnapshotListener
                    val myProfile = _userProfile.value
                    val myUsername = myProfile.username.trim().removePrefix("@").lowercase()
                    val myUid = UserDirectorySyncService.getEffectiveUid(myProfile)

                    val newCounts = mutableMapOf<String, Int>()
                    val newLastMsgs = mutableMapOf<String, String>()
                    val newLastTimestamps = mutableMapOf<String, Long>()

                    for (doc in snapshots.documents) {
                        @Suppress("UNCHECKED_CAST")
                        val participants = doc.get("participantUsernames") as? List<String>
                        @Suppress("UNCHECKED_CAST")
                        val uids = doc.get("participants") as? List<String>

                        val isMyThread = (myUsername.isNotBlank() && participants?.any { it.equals(myUsername, ignoreCase = true) } == true) ||
                                (myUid.isNotBlank() && uids?.contains(myUid) == true)

                        if (isMyThread) {
                            val otherParticipant = participants?.firstOrNull { !it.equals(myUsername, ignoreCase = true) }?.trim()?.removePrefix("@")?.lowercase().orEmpty()
                            val partnerKey = otherParticipant.ifBlank {
                                doc.getString("lastSenderUsername")?.trim()?.removePrefix("@")?.lowercase().orEmpty()
                            }

                            val lastSender = doc.getString("lastSenderUsername")?.trim()?.removePrefix("@")?.lowercase().orEmpty()
                            val lastMsgAt = (doc.get("lastMessageAt") as? Number)?.toLong() ?: 0L
                            val lastMsg = doc.getString("lastMessage").orEmpty()
                            val isSnap = doc.getBoolean("lastIsSnap") ?: false

                            if (partnerKey.isNotBlank()) {
                                newLastMsgs[partnerKey] = if (isSnap) "🔥 Sent a Snap" else if (lastMsg.isBlank()) "📷 Sent a photo" else lastMsg
                                newLastTimestamps[partnerKey] = lastMsgAt

                                // Only mark unread if the LAST message was sent by the OTHER person
                                // AND the timestamp is strictly newer than when I last read this chat
                                if (lastSender.isNotBlank() && !lastSender.equals(myUsername, ignoreCase = true)) {
                                    val lastReadFromPref = dmReadPrefs.getLong("read_$partnerKey", 0L)
                                    val lastReadMemory = lastReadDmTimestamps[partnerKey] ?: 0L
                                    val lastRead = maxOf(lastReadFromPref, lastReadMemory)
                                    val isReadInFirestore = doc.getBoolean("readBy_${myUsername}") ?: false

                                    val isUnread = lastMsgAt > lastRead && lastMsgAt > 0L && !isReadInFirestore
                                    if (isUnread) {
                                        newCounts[partnerKey] = 1
                                    }
                                }
                            }
                        }
                    }
                    _unreadDmCounts.value = newCounts
                    _lastDmMessageBySender.value = newLastMsgs
                    _lastDmTimestampBySender.value = newLastTimestamps
                }
            } catch (e: Throwable) {
                Log.w("CricketViewModel", "direct_chats unread observer error: ${e.message}")
            }
        }
    }

    fun selectTab(tab: AppScreenTab) { _currentTab.value = tab }
    fun selectMatch(matchId: String) {
        _selectedMatchId.value = matchId
        listenToMatchChat(matchId)
    }

    fun setCommentaryFilter(filter: CommentaryFilter) { _commentaryFilter.value = filter }
    fun setScorerSheetVisible(visible: Boolean) {
        if (visible) {
            val curr = currentMatch.value
            if (curr?.status == "FINISHED") {
                showBanner("🏆 Match khatam ho chuka hai! Scoring band kar di gayi hai.")
                _showScorerSheet.value = false
                return
            }
            if (curr?.status == "INNINGS_BREAK" || (curr?.currentInnings == 1 && curr.legalBalls >= curr.totalOvers * 6)) {
                showBanner("🏏 1st Innings ke ${curr?.totalOvers} overs samapt! 'Start 2nd Innings' par tap karein.")
                _showScorerSheet.value = false
                _showStartSecondInningsDialog.value = true
                return
            }
        }
        _showScorerSheet.value = visible
    }

    fun switchStrikers() {
        viewModelScope.launch {
            scoringMutex.withLock {
                repository.switchStrikers(_selectedMatchId.value)
                val updated = repository.getMatch(_selectedMatchId.value).firstOrNull()
                if (updated != null) {
                    RealtimeMatchSyncService.publishMatch(updated)
                }
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
            scoringMutex.withLock {
                val prevMatch = currentMatch.value ?: return@launch

                // 1. Strict guard: Match already finished
                if (prevMatch.status == "FINISHED") {
                    showBanner("🏆 Match samapt ho chuka hai! (${prevMatch.statusDetail})")
                    _showScorerSheet.value = false
                    return@launch
                }

                // 2. Strict guard: 1st Innings break or overs completed
                if (prevMatch.status == "INNINGS_BREAK" || (prevMatch.currentInnings == 1 && prevMatch.legalBalls >= prevMatch.totalOvers * 6)) {
                    showBanner("🏏 1st Innings ke ${prevMatch.totalOvers} overs samapt! 'Start 2nd Innings' par tap karke agali paari shuru karein.")
                    _showScorerSheet.value = false
                    _showStartSecondInningsDialog.value = true
                    return@launch
                }

                // 3. Strict guard: 2nd Innings Target already reached
                if (prevMatch.currentInnings == 2 && prevMatch.target > 0 && prevMatch.score >= prevMatch.target) {
                    showBanner("🏆 Match jeet chuke hain! (${prevMatch.statusDetail})")
                    _showScorerSheet.value = false
                    return@launch
                }

                // 4. Strict guard: 2nd Innings overs already completed
                if (prevMatch.currentInnings == 2 && prevMatch.legalBalls >= prevMatch.totalOvers * 6) {
                    showBanner("🏆 2nd Innings ke ${prevMatch.totalOvers} overs samapt! Match khatam.")
                    _showScorerSheet.value = false
                    return@launch
                }

                val prevInnings = prevMatch.currentInnings
                val prevBatTeam = prevMatch.battingTeam
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
                    val isInningsTransition = updatedMatch.status == "INNINGS_BREAK"
                    val isMatchFinished = updatedMatch.status == "FINISHED"

                    if (isMatchFinished) {
                        _showScorerSheet.value = false
                    }
                    if (isInningsTransition) {
                        _showScorerSheet.value = false
                        _showStartSecondInningsDialog.value = true
                    }

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
                            batsman = prevMatch?.strikerName ?: updatedMatch.strikerName,
                            bowler = prevMatch?.bowlerName ?: updatedMatch.bowlerName,
                            commentary = commentary
                        )

                        val currentBowlerName = prevMatch?.bowlerName ?: updatedMatch.bowlerName
                        val currentStrikerName = prevMatch?.strikerName ?: updatedMatch.strikerName
                        val currentStrikerRuns = updatedMatch.strikerRuns

                        if (overNum != currentOverTracked) {
                            wicketsInCurrentOver = 0
                            currentOverTracked = overNum
                        }

                        if (isWicket) {
                            wicketsInCurrentOver++
                            if (lastWicketBowlerName.equals(currentBowlerName, ignoreCase = true)) {
                                consecutiveBowlerWickets++
                            } else {
                                lastWicketBowlerName = currentBowlerName
                                consecutiveBowlerWickets = 1
                            }
                        } else if (runs > 0) {
                            consecutiveBowlerWickets = 0
                        }

                        val isHatTrick = isWicket && consecutiveBowlerWickets >= 3
                        val isHatTrickBallChance = (!isWicket) && consecutiveBowlerWickets == 2
                        val isOverWicketStorm = isWicket && wicketsInCurrentOver >= 2
                        val ballsLeftInMatch = (updatedMatch.totalOvers * 6) - updatedMatch.legalBalls
                        val runsNeededInMatch = updatedMatch.target - updatedMatch.score
                        val isTightChase = updatedMatch.currentInnings == 2 && updatedMatch.target > 0 && ballsLeftInMatch in 1..12 && runsNeededInMatch in 1..24
                        val isLastBallThriller = updatedMatch.currentInnings == 2 && updatedMatch.target > 0 && ballsLeftInMatch in 0..1 && runsNeededInMatch in 1..6
                        val isBatsmanFifty = currentStrikerRuns >= 50 && (currentStrikerRuns - runs) < 50
                        val isBatsmanCentury = currentStrikerRuns >= 100 && (currentStrikerRuns - runs) < 100
                        val isTurningPoint = isHatTrick || isOverWicketStorm || isTightChase || isLastBallThriller || isBatsmanFifty || isBatsmanCentury

                        val turningDetail = when {
                            isHatTrick -> "3 गेंदों पर 3 विकेट! हैट्रिक!"
                            isOverWicketStorm -> "एक ही ओवर में $wicketsInCurrentOver विकेट गिर चुके हैं!"
                            isLastBallThriller -> "अंतिम 1 गेंद पर चाहिए $runsNeededInMatch रन!"
                            isTightChase -> "$ballsLeftInMatch गेंदों पर चाहिए $runsNeededInMatch रन!"
                            isBatsmanCentury -> "$currentStrikerName का शानदार 100 रन शतक!"
                            isBatsmanFifty -> "$currentStrikerName की शानदार 50 रन फिफ्टी!"
                            else -> ""
                        }

                        val useGeminiThisBall = (commentaryBallCounter++ % 2 == 0)
                        val isHybrid = SmartGeminiCommentaryService.isHybridModeEnabled(getApplication())
                        val hasKeys = SmartGeminiCommentaryService.hasAvailableKeys(getApplication())

                        val sidhuDialogue = when {
                            isInningsTransition -> {
                                "🎙️ सिद्धू पाजी (Edge Neural): ओए चक दे फट्टे गुरु! पहली पारी समाप्त हो चुकी है! $prevBatTeam ने बनाए ${updatedMatch.score} रन! अब दूसरी पारी शुरू करने के लिए 'Start 2nd Innings' पर टैप करें गुरु! ठोको ताली!"
                            }
                            isMatchFinished -> {
                                "🎙️ सिद्धू पाजी (Edge Neural): ओए गुरु मुकाबला समाप्त हो गया! ${updatedMatch.statusDetail} क्या रोमांचक मुकाबला था गुरु! ठोको ताली!"
                            }
                            else -> {
                                var geminiLine: String? = null
                                if (isHybrid && hasKeys && useGeminiThisBall) {
                                    geminiLine = SmartGeminiCommentaryService.generateSidhuAiCommentary(
                                        context = getApplication(),
                                        ball = ballEvent,
                                        strikerName = currentStrikerName,
                                        bowlerName = currentBowlerName,
                                        currentScore = currentScoreAudio,
                                        style = sidhuVoiceStyle.value,
                                        isHatTrick = isHatTrick,
                                        isTurningPoint = isTurningPoint,
                                        specialSituation = turningDetail
                                    )
                                }

                                if (geminiLine != null) {
                                    "🎙️ सिद्धू पाजी (Gemini AI): $geminiLine"
                                } else {
                                    val inBuiltLine = when {
                                        isHatTrick -> {
                                            SidhuCommentaryGenerator.generateHatTrickCommentary(
                                                bowlerName = currentBowlerName,
                                                strikerName = currentStrikerName,
                                                wicketType = wicketType,
                                                currentScore = currentScoreAudio,
                                                style = sidhuVoiceStyle.value
                                            )
                                        }
                                        isHatTrickBallChance -> {
                                            SidhuCommentaryGenerator.generateHatTrickBallChanceCommentary(
                                                bowlerName = currentBowlerName,
                                                strikerName = currentStrikerName,
                                                currentScore = currentScoreAudio,
                                                style = sidhuVoiceStyle.value
                                            )
                                        }
                                        isTurningPoint -> {
                                            val turnType = when {
                                                isOverWicketStorm -> "OVER_WICKET_STORM"
                                                isLastBallThriller -> "LAST_BALL"
                                                isTightChase -> "TIGHT_CHASE"
                                                isBatsmanCentury -> "BATSMAN_CENTURY"
                                                isBatsmanFifty -> "BATSMAN_FIFTY"
                                                else -> "TURNING_POINT"
                                            }
                                            SidhuCommentaryGenerator.generateTurningPointCommentary(
                                                turningType = turnType,
                                                bowlerName = currentBowlerName,
                                                strikerName = currentStrikerName,
                                                currentScore = currentScoreAudio,
                                                detail = turningDetail,
                                                style = sidhuVoiceStyle.value
                                            )
                                        }
                                        else -> {
                                            SidhuCommentaryGenerator.generateBallCommentary(
                                                ball = ballEvent,
                                                strikerName = currentStrikerName,
                                                bowlerName = currentBowlerName,
                                                currentScore = currentScoreAudio,
                                                style = sidhuVoiceStyle.value
                                            )
                                        }
                                    }
                                    "🎙️ सिद्धू पाजी (Edge Neural): $inBuiltLine"
                                }
                            }
                        }

                        sidhuCommentaryManager.speak(sidhuDialogue)
                        lastRemoteSpokenBallTs = System.currentTimeMillis() + 1500L

                        RealtimeMatchSyncService.publishMatch(updatedMatch, ballEvent, sidhuDialogue)
                    } catch (e: Exception) {
                        Log.w("CricketViewModel", "Commentary/Sync error: ${e.message}")
                    }

                    try {
                        val strikerName = updatedMatch.strikerName
                        val bowlerName = updatedMatch.bowlerName
                        val currentStrikerRuns = updatedMatch.strikerRuns

                        if (isInningsTransition) {
                            StadiumSoundManager.playSixCheer()
                            triggerBroadcastOverlay(
                                BroadcastOverlayEvent(
                                    type = BroadcastOverlayEvent.OverlayType.MILESTONE_100,
                                    headline = "🏏 1ST INNINGS COMPLETED!",
                                    subheadline = "Target: ${updatedMatch.target} Runs for ${updatedMatch.battingTeam}",
                                    statDetail = "Chase Begins Now",
                                    accentColorHex = 0xFFFFD700
                                )
                            )
                            showBanner("🏏 1st Innings Khatam! Target: ${updatedMatch.target} runs (${updatedMatch.battingTeam} Batting) 🚀")
                            try {
                                com.example.notification.MatchNotificationHelper.notifyMatchLive(getApplication(), updatedMatch, forceNotify = true)
                                com.example.widget.LiveScoreAppWidgetProvider.updateAllWidgets(getApplication(), updatedMatch)
                            } catch (_: Throwable) {}
                        } else if (isMatchFinished) {
                            StadiumSoundManager.playSixCheer()
                            triggerBroadcastOverlay(
                                BroadcastOverlayEvent(
                                    type = BroadcastOverlayEvent.OverlayType.MILESTONE_100,
                                    headline = "🏆 MATCH FINISHED!",
                                    subheadline = updatedMatch.statusDetail,
                                    statDetail = "Final Result",
                                    accentColorHex = 0xFFFFD700
                                )
                            )
                            showBanner("🏆 Match Finished! ${updatedMatch.statusDetail}")
                            try {
                                com.example.notification.MatchNotificationHelper.notifyMatchLive(getApplication(), updatedMatch, forceNotify = true)
                                com.example.widget.LiveScoreAppWidgetProvider.updateAllWidgets(getApplication(), updatedMatch)
                            } catch (_: Throwable) {}
                            viewModelScope.launch(Dispatchers.IO) {
                                try {
                                    val allM = repository.allMatches.first()
                                    val (computedStandings, _) = TournamentStatsCalculator.computeStandingsAndStats(allM)
                                    if (computedStandings.isNotEmpty()) {
                                        repository.saveTeamStandings(computedStandings)
                                        RealtimeMatchSyncService.publishTournamentStandings(computedStandings)
                                    }
                                } catch (_: Throwable) {}
                            }
                        } else if (isWicket) {
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
                        if (!isInningsTransition && !isMatchFinished && isLegal && updatedMatch.legalBalls > 0 && updatedMatch.legalBalls % 6 == 0) {
                            _showChangeBowlerDialog.value = true
                            showBanner("Over Khatam! Agle over ke liye Bowler chunein 🎳")
                        }
                    } catch (e: Exception) {
                        Log.w("CricketViewModel", "Error in post-delivery processing: ${e.message}", e)
                    }
                }
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

    // DRS Manual Review Controls
    fun startDrsReview(appealType: String = "LBW", batsman: String = "Batter", bowler: String = "Bowler", onFieldDecision: String = "NOT OUT") {
        if (_currentDeviceRole.value == DeviceRole.SPECTATOR_VIEWER) {
            showBanner("⛔ Spectators DRS nahi le sakte! Spectators ko bas match dekhne aur message karne ka haq hai.")
            return
        }
        drsAutoJob?.cancel()
        val currMatch = currentMatch.value
        val actualBatsman = if (batsman.isNotBlank() && batsman != "Batter") batsman else (currMatch?.strikerName ?: "Batter")
        val actualBowler = if (bowler.isNotBlank() && bowler != "Bowler") bowler else (currMatch?.bowlerName ?: "Bowler")

        _drsState.value = DrsReviewState(
            appealType = appealType,
            batsman = actualBatsman,
            bowler = actualBowler,
            onFieldDecision = onFieldDecision,
            reviewStage = 1,
            pitching = "IN_LINE",
            impact = "IN_LINE",
            wicketsHitting = "HITTING",
            aiConfidencePercent = 100,
            thirdUmpireDecision = "" // Pending manual review by Third Umpire
        )

        // 1. Sidhu Paaji audio announcement
        sidhuCommentaryManager.speak("ओए गुरु! DRS ले लिया गया है! चक दे फट्टे, अब तीसरा अंपायर फैसला करेगा! दूध का दूध और पानी का पानी होने वाला है, ठोको ताली गुरु!")
        com.example.notification.MatchNotificationHelper.notifyDrsTaken(
            getApplication(),
            appealType,
            actualBatsman,
            actualBowler
        )

        // 2. Broadcast to Firestore so ALL connected phones hear Sidhu Paaji
        if (_selectedMatchId.value.isNotBlank()) {
            RealtimeMatchSyncService.publishDrsAppeal(_selectedMatchId.value, appealType)
        }

        showBanner("⚡ DRS Review Shuru! Third Umpire manually decision check karein.")
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

        // Broadcast decision card
        val decisionCard = DrsDecisionCardData(
            matchId = _selectedMatchId.value,
            appealType = _drsState.value.appealType,
            batsman = _drsState.value.batsman,
            bowler = _drsState.value.bowler,
            onFieldDecision = _drsState.value.onFieldDecision,
            thirdUmpireDecision = decision,
            pitching = pitching,
            impact = impact,
            wickets = wickets,
            timestamp = System.currentTimeMillis()
        )
        _drsDecisionPopup.value = decisionCard
        RealtimeMatchSyncService.publishDrsDecision(decisionCard)
        if (_selectedMatchId.value.isNotBlank()) {
            RealtimeMatchSyncService.clearDrsAppeal(_selectedMatchId.value)
        }
    }

    fun resetDrs() {
        drsAutoJob?.cancel()
        lastHandledDrsAppealTs = System.currentTimeMillis()
        _drsState.value = DrsReviewState(reviewStage = 0, thirdUmpireDecision = "")
        if (_selectedMatchId.value.isNotBlank()) {
            RealtimeMatchSyncService.clearDrsAppeal(_selectedMatchId.value)
        }
    }

    fun deleteMatch(matchId: String, enteredPin: String? = null): Boolean {
        if (matchId.isBlank()) return false
        val cleanPin = enteredPin?.trim() ?: ""
        val authorized = isAppOwner() ||
                         _currentDeviceRole.value == DeviceRole.OFFICIAL_SCORER ||
                         _currentDeviceRole.value.isOfficial ||
                         cleanPin == "200910" ||
                         (cleanPin.isNotBlank() && cleanPin == _officialPin.value.trim()) ||
                         (_userProfile.value.pin.isNotBlank() && cleanPin == _userProfile.value.pin.trim())
        if (!authorized) {
            showBanner("⛔ Access Denied: Match delete karne ka adhikar sirf Official Scorer ya Admin (Ayush) ke paas hai!")
            return false
        }
        viewModelScope.launch {
            repository.deleteMatch(matchId)
            try {
                com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("live_matches")
                    .document(matchId)
                    .delete()
            } catch (_: Throwable) {}
            if (_selectedMatchId.value == matchId) {
                val remaining = repository.allMatches.first().filter { it.id != matchId }
                _selectedMatchId.value = remaining.firstOrNull()?.id ?: ""
            }
            showBanner("Match delete kar diya gaya 🗑️")
        }
        return true
    }

    fun endCurrentMatch(summary: String = "Match Completed") {
        val currentId = _selectedMatchId.value
        if (currentId.isBlank()) return
        viewModelScope.launch {
            repository.endMatch(currentId, summary)
            showBanner("🏁 Match Completed: $summary")
        }
    }

    fun clearAllMatchesAndStartFresh() {
        viewModelScope.launch {
            repository.clearAllMatchesAndStartFresh()
            _selectedMatchId.value = ""
            showBanner("Sabhi purane matches clear ho gaye! Naya match start karein.")
        }
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

    fun verifyPinAndSetRole(role: DeviceRole, enteredPin: String = ""): Boolean {
        if (role == DeviceRole.SPECTATOR_VIEWER) {
            setDeviceRole(role)
            return true
        }
        val cleanPin = enteredPin.trim()
        if (cleanPin.isBlank()) {
            showBanner("Official role unlock karne ke liye Security PIN darj karein!")
            return false
        }
        val currentPin = _officialPin.value.trim()
        val isPinValid = cleanPin == "200910" || (currentPin.isNotBlank() && cleanPin == currentPin)
        val isRoleApproved = _roleRequests.value.any { it.requestedRole == role && it.status == "APPROVED" }

        if (isPinValid || isRoleApproved) {
            setDeviceRole(role)
            _isAuthorizedOfficial.value = true
            showBanner("Official Role Authorized: ${role.title} activated.")
            return true
        }
        showBanner("Galat Security PIN! Sirf sahi PIN se hi role change kiya ja sakta hai.")
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

        try {
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("role_requests")
                .document(request.id)
                .set(
                    hashMapOf(
                        "id" to request.id,
                        "applicantName" to request.applicantName,
                        "applicantPhone" to request.applicantPhone,
                        "requestedRole" to request.requestedRole.name,
                        "reason" to request.reason,
                        "status" to "PENDING",
                        "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                    )
                )
        } catch (e: Throwable) {
            android.util.Log.w("CricketViewModel", "Could not sync role request to Firestore: ${e.message}")
        }
    }

    fun approveRoleRequest(requestId: String) {
        val req = _roleRequests.value.firstOrNull { it.id == requestId } ?: return
        _roleRequests.value = _roleRequests.value.map {
            if (it.id == requestId) it.copy(status = "APPROVED") else it
        }
        setDeviceRole(req.requestedRole)
        _isAuthorizedOfficial.value = true
        showBanner("Admin Approved: ${req.requestedRole.title} activated!")

        try {
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("role_requests")
                .document(requestId)
                .update("status", "APPROVED")
        } catch (e: Throwable) {
            android.util.Log.w("CricketViewModel", "Could not approve role request in Firestore: ${e.message}")
        }
    }

    fun denyRoleRequest(requestId: String) {
        _roleRequests.value = _roleRequests.value.map {
            if (it.id == requestId) it.copy(status = "DENIED") else it
        }
        showBanner("Role request rejected by Admin.")

        try {
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("role_requests")
                .document(requestId)
                .update("status", "DENIED")
        } catch (e: Throwable) {
            android.util.Log.w("CricketViewModel", "Could not deny role request in Firestore: ${e.message}")
        }
    }

    fun setShowRoleRequestsDialog(show: Boolean) { _showRoleRequestsDialog.value = show }
    fun setShowProfileDialog(show: Boolean) { _showProfileDialog.value = show }

    fun isCurrentProfileGuest(): Boolean {
        val authUser = try { com.google.firebase.auth.FirebaseAuth.getInstance().currentUser } catch (_: Throwable) { null }
        val prof = _userProfile.value
        return (authUser == null || authUser.isAnonymous) && (prof.mobileNumber.isBlank() || prof.mobileNumber == "Unlinked")
    }

    fun saveUserProfile(profile: CricHeroesProfile) {
        val authUser = try { com.google.firebase.auth.FirebaseAuth.getInstance().currentUser } catch (_: Throwable) { null }
        val isGuestUser = (authUser == null || authUser.isAnonymous) && (profile.mobileNumber.isBlank() || profile.mobileNumber == "Unlinked")
        val effectiveUid = authUser?.uid ?: profile.id
        val updatedProfile = profile.copy(
            id = effectiveUid,
            uid = effectiveUid,
            isVerified = (authUser != null && !authUser.isAnonymous),
            isGuest = isGuestUser
        )

        _userProfile.value = updatedProfile
        if (updatedProfile.pin.isNotBlank()) {
            val cleanUser = updatedProfile.username.trim().removePrefix("@").lowercase()
            if (cleanUser == "ayush_7" || isAppOwner()) {
                rolePrefs.edit().putString("admin_pin", updatedProfile.pin).apply()
                _officialPin.value = updatedProfile.pin
            }
        }
        profilePrefs.edit()
            .putBoolean("is_logged_in", true)
            .putString("id", updatedProfile.id)
            .putString("username", updatedProfile.username)
            .putString("pin", updatedProfile.pin)
            .putString("mobile", updatedProfile.mobileNumber)
            .putString("name", updatedProfile.fullName)
            .putString("jersey_name", updatedProfile.jerseyName)
            .putInt("jersey_num", updatedProfile.jerseyNumber)
            .putString("role", updatedProfile.primaryRole)
            .putString("bat_style", updatedProfile.battingStyle)
            .putString("bowl_style", updatedProfile.bowlingStyle)
            .putString("team", updatedProfile.teamName)
            .putString("city", updatedProfile.city)
            .putString("avatar", updatedProfile.avatarEmoji)
            .putInt("runs", updatedProfile.runs)
            .putInt("wickets", updatedProfile.wickets)
            .putInt("matches_played", updatedProfile.matchesPlayed)
            .putString("strike_rate", updatedProfile.strikeRate.toString())
            .putBoolean("is_guest", isGuestUser)
            .apply()

        // Sync to Android OS Launcher Home Screen Widget immediately
        try {
            com.example.widget.LiveScoreAppWidgetProvider.updateAllWidgets(getApplication(), null)
        } catch (_: Throwable) {}

        // Authoritative cloud sync to Firestore
        try {
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users")
                .document(effectiveUid)
                .set(
                    hashMapOf(
                        "uid" to effectiveUid,
                        "username" to updatedProfile.username,
                        "pin" to updatedProfile.pin,
                        "mobile" to updatedProfile.mobileNumber,
                        "name" to updatedProfile.fullName,
                        "jerseyName" to updatedProfile.jerseyName,
                        "jerseyNumber" to updatedProfile.jerseyNumber,
                        "primaryRole" to updatedProfile.primaryRole,
                        "battingStyle" to updatedProfile.battingStyle,
                        "bowlingStyle" to updatedProfile.bowlingStyle,
                        "teamName" to updatedProfile.teamName,
                        "city" to updatedProfile.city,
                        "avatarEmoji" to updatedProfile.avatarEmoji,
                        "isVerified" to (authUser != null && !authUser.isAnonymous),
                        "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                    ),
                    com.google.firebase.firestore.SetOptions.merge()
                )
        } catch (e: Throwable) {
            Log.w("CricketViewModel", "Could not sync user profile to Firestore: ${e.message}")
        }

        registerCommunityPlayer(updatedProfile)

        viewModelScope.launch {
            UserDirectorySyncService.registerUserProfile(updatedProfile)
        }

        _showProfileDialog.value = false
        showBanner("Player Profile Verified! @${updatedProfile.username} (#${updatedProfile.jerseyNumber})")
    }

    fun setShowChatDialog(show: Boolean) { _showChatDialog.value = show }

    fun isAppOwner(): Boolean {
        val authUser = try { com.google.firebase.auth.FirebaseAuth.getInstance().currentUser } catch (_: Throwable) { null }
        val email = authUser?.email?.trim()?.lowercase() ?: ""
        val username = _userProfile.value.username.trim().removePrefix("@").lowercase()
        val fullName = _userProfile.value.fullName.lowercase()
        return email == "ayushsunil591983@gmail.com" ||
               email == "ayushku11012011@gmail.com" ||
               email.contains("ayush") ||
               username == "ayush_7" ||
               fullName.contains("ayush") ||
               _currentDeviceRole.value == DeviceRole.OFFICIAL_SCORER ||
               _currentDeviceRole.value.isOfficial
    }

    // Live Match Chat Listening (Firestore real-time snapshot with 30 days retention via RealChatRepository)
    fun listenToMatchChat(matchId: String) {
        matchChatJob?.cancel()
        if (matchId.isBlank()) {
            _chatMessages.value = emptyList()
            return
        }
        matchChatJob = viewModelScope.launch {
            try {
                val myUser = _userProfile.value.username
                val myUid = UserDirectorySyncService.getEffectiveUid(_userProfile.value)
                chatRepository.observeMatchMessages(
                    matchId = matchId,
                    myUsername = myUser,
                    myUid = myUid
                ).collect { validMessages ->
                    _chatMessages.value = validMessages
                }
            } catch (e: Exception) {
                Log.w("CricketViewModel", "observeMatchMessages error: ${e.message}")
            }
        }
    }

    fun deleteMatchChatMessage(messageId: String) {
        val currentMatchId = _selectedMatchId.value
        if (currentMatchId.isBlank() || messageId.isBlank()) return
        viewModelScope.launch {
            val res = chatRepository.deleteMatchChatMessage(currentMatchId, messageId)
            if (res.isSuccess) {
                _chatMessages.value = _chatMessages.value.filter { it.id != messageId }
                showBanner("Match chat message delete ho gaya 🗑️")
            } else {
                Log.w("CricketViewModel", "Failed to delete match chat message: ${res.exceptionOrNull()?.message}")
            }
        }
    }

    // Send Match Live Chat Message
    fun sendChatMessage(text: String) {
        if (text.isBlank() || _selectedMatchId.value.isBlank()) return
        val profile = _userProfile.value
        val roleLabel = when (_currentDeviceRole.value) {
            DeviceRole.OFFICIAL_SCORER -> "Scorer"
            DeviceRole.BOWLER_END_UMPIRE, DeviceRole.SQUARE_LEG_UMPIRE -> "Umpire"
            DeviceRole.THIRD_UMPIRE_DRS -> "3rd Umpire"
            DeviceRole.SPECTATOR_VIEWER -> "Spectator"
        }

        val myUsername = profile.username.trim().removePrefix("@").lowercase()
        val myUid = UserDirectorySyncService.getEffectiveUid(profile)

        viewModelScope.launch {
            chatRepository.sendMatchChatMessage(
                matchId = _selectedMatchId.value,
                senderUid = myUid,
                senderUsername = myUsername,
                senderName = profile.fullName.ifBlank { profile.jerseyName },
                senderRole = roleLabel,
                avatarEmoji = profile.avatarEmoji.ifBlank { "🏏" },
                text = text.trim()
            )
        }
    }

    fun sendChatReaction(emoji: String) { sendChatMessage(emoji) }

    fun clearAllStandingsAndStats(enteredPin: String? = null): Boolean {
        val cleanPin = enteredPin?.trim() ?: ""
        val authorized = isAppOwner() ||
                         _currentDeviceRole.value == DeviceRole.OFFICIAL_SCORER ||
                         _currentDeviceRole.value.isOfficial ||
                         cleanPin == "200910" ||
                         (cleanPin.isNotBlank() && cleanPin == _officialPin.value.trim()) ||
                         (_userProfile.value.pin.isNotBlank() && cleanPin == _userProfile.value.pin.trim())
        if (!authorized) {
            showBanner("⛔ Server Security: Sirf verified Admin (Ayush) ya Official Scorer hi tournament records reset kar sakte hain!")
            return false
        }
        viewModelScope.launch {
            repository.clearAllStandingsAndStats()
            showBanner("Tournament standings & player records cleared!")
        }
        return true
    }

    private fun loadProfileFromPrefs(): CricHeroesProfile {
        var localId = profilePrefs.getString("id", "") ?: ""
        if (localId.isBlank()) {
            localId = UUID.randomUUID().toString()
            profilePrefs.edit().putString("id", localId).apply()
        }
        val firebaseUid = try {
            com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid?.ifBlank { null }
        } catch (_: Throwable) { null }
        val effectiveUid = firebaseUid ?: localId

        return CricHeroesProfile(
            id = effectiveUid,
            uid = effectiveUid,
            username = profilePrefs.getString("username", "ayush_7")?.ifBlank { "ayush_7" } ?: "ayush_7",
            pin = profilePrefs.getString("pin", "") ?: "",
            mobileNumber = profilePrefs.getString("mobile", "+91 98765 43210")?.ifBlank { "+91 98765 43210" } ?: "+91 98765 43210",
            fullName = profilePrefs.getString("name", "Ayush Kumar")?.ifBlank { "Ayush Kumar" } ?: "Ayush Kumar",
            jerseyName = profilePrefs.getString("jersey_name", "Ayush")?.ifBlank { "Ayush" } ?: "Ayush",
            jerseyNumber = profilePrefs.getInt("jersey_num", 7).let { if (it > 0) it else 7 },
            primaryRole = profilePrefs.getString("role", "Top-Order Batter") ?: "Top-Order Batter",
            battingStyle = profilePrefs.getString("bat_style", "Right-hand Bat") ?: "Right-hand Bat",
            bowlingStyle = profilePrefs.getString("bowl_style", "Right-arm Fast") ?: "Right-arm Fast",
            teamName = profilePrefs.getString("team", "Local XI")?.ifBlank { "Local XI" } ?: "Local XI",
            city = profilePrefs.getString("city", "India")?.ifBlank { "India" } ?: "India",
            avatarEmoji = profilePrefs.getString("avatar", "🦁") ?: "🦁",
            runs = profilePrefs.getInt("runs", 540).let { if (it > 0) it else 540 },
            wickets = profilePrefs.getInt("wickets", 16).let { if (it > 0) it else 16 },
            matchesPlayed = profilePrefs.getInt("matches_played", 18).let { if (it > 0) it else 18 },
            strikeRate = profilePrefs.getString("strike_rate", "165.4")?.toDoubleOrNull() ?: 165.4,
            isVerified = true,
            isOnline = true,
            isGuest = profilePrefs.getBoolean("is_guest", false)
        )
    }

    // Community Players Directory
    private val _communityPlayers = MutableStateFlow<List<CricHeroesProfile>>(initialCommunityPlayers())
    val communityPlayers = _communityPlayers.asStateFlow()

    private val _showDmDialog = MutableStateFlow(false)
    val showDmDialog = _showDmDialog.asStateFlow()
    fun setShowDmDialog(show: Boolean) { _showDmDialog.value = show }

    private val dmReadPrefs by lazy {
        getApplication<Application>().getSharedPreferences("ayuu_dm_read_state", Context.MODE_PRIVATE)
    }

    private val _unreadDmCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val unreadDmCounts = _unreadDmCounts.asStateFlow()

    private val _lastDmMessageBySender = MutableStateFlow<Map<String, String>>(emptyMap())
    val lastDmMessageBySender = _lastDmMessageBySender.asStateFlow()

    private val _lastDmTimestampBySender = MutableStateFlow<Map<String, Long>>(emptyMap())
    val lastDmTimestampBySender = _lastDmTimestampBySender.asStateFlow()

    private val lastReadDmTimestamps = mutableMapOf<String, Long>()

    val totalUnreadDmCount: StateFlow<Int> = _unreadDmCounts
        .map { map -> map.values.sum() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun markAllDmsAsRead() {
        val now = System.currentTimeMillis()
        val editor = dmReadPrefs.edit()
        for (key in _unreadDmCounts.value.keys) {
            editor.putLong("read_$key", now)
            lastReadDmTimestamps[key] = now
        }
        editor.apply()
        _unreadDmCounts.value = emptyMap()
    }

    fun markDmAsRead(otherUsername: String) {
        val clean = otherUsername.trim().removePrefix("@").lowercase()
        val now = System.currentTimeMillis()
        lastReadDmTimestamps[clean] = now
        dmReadPrefs.edit().putLong("read_$clean", now).apply()
        if (_unreadDmCounts.value.containsKey(clean)) {
            val updated = _unreadDmCounts.value.toMutableMap()
            updated.remove(clean)
            _unreadDmCounts.value = updated
        }
    }

    private val _personalMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val personalMessages = _personalMessages.asStateFlow()

    private val _activeDmRecipient = MutableStateFlow<CricHeroesProfile?>(null)
    val activeDmRecipient = _activeDmRecipient.asStateFlow()

    private fun getDmRoomId(user1: String, user2: String): String {
        val clean1 = user1.trim().lowercase().removePrefix("@").replace(" ", "_")
        val clean2 = user2.trim().lowercase().removePrefix("@").replace(" ", "_")
        return if (clean1 < clean2) "${clean1}_${clean2}" else "${clean2}_${clean1}"
    }

    // 1-on-1 Personal DMs Real-Time Listener with 30-Day Auto-Cleanup via RealChatRepository
    fun openDirectMessageWith(player: CricHeroesProfile) {
        _activeDmRecipient.value = player
        val myProfile = _userProfile.value
        val myUsername = myProfile.username.trim().removePrefix("@").lowercase().ifBlank { "user_me" }
        val otherUsername = player.username.trim().removePrefix("@").lowercase()
        val myUid = UserDirectorySyncService.getEffectiveUid(myProfile)
        val otherUid = player.uid.ifBlank { player.id }
        val now = System.currentTimeMillis()

        // Mark as read immediately & persist in SharedPreferences
        lastReadDmTimestamps[otherUsername] = now
        dmReadPrefs.edit().putLong("read_$otherUsername", now).apply()

        if (_unreadDmCounts.value.containsKey(otherUsername)) {
            val updated = _unreadDmCounts.value.toMutableMap()
            updated.remove(otherUsername)
            _unreadDmCounts.value = updated
        }

        val threadId = RealChatRepository.getDmThreadId(
            uid1 = myUid,
            username1 = myUsername,
            uid2 = otherUid,
            username2 = otherUsername
        )

        // Sync read receipt to Firestore thread
        viewModelScope.launch(Dispatchers.IO) {
            try {
                FirebaseFirestore.getInstance().collection("direct_chats")
                    .document(threadId)
                    .set(
                        mapOf(
                            "readBy_${myUsername}" to true,
                            "lastReadAt_${myUsername}" to now
                        ),
                        com.google.firebase.firestore.SetOptions.merge()
                    )
            } catch (_: Throwable) {}
        }

        directChatJob?.cancel()
        directChatJob = viewModelScope.launch {
            chatRepository.observeDirectMessages(
                threadId = threadId,
                currentUserId = myUid,
                currentUsername = myUsername
            ).collect { validMsgs ->
                _personalMessages.value = validMsgs
                val latest = validMsgs.lastOrNull()
                if (latest != null && !latest.isFromMe && latest.id != lastNotifiedDmMsgId && (System.currentTimeMillis() - latest.timestamp < 30_000L)) {
                    lastNotifiedDmMsgId = latest.id
                    com.example.notification.MatchNotificationHelper.notifyDirectMessage(
                        context = getApplication(),
                        senderUsername = latest.senderUsername,
                        senderName = latest.senderName,
                        messageText = latest.message,
                        isSnap = latest.isSnap
                    )
                }
            }
        }
    }

    fun deleteDirectMessage(messageId: String) {
        val recipient = _activeDmRecipient.value ?: return
        val myProfile = _userProfile.value
        val myUsername = myProfile.username.trim().removePrefix("@").lowercase().ifBlank { "user_me" }
        val otherUsername = recipient.username.trim().removePrefix("@").lowercase()
        val myUid = UserDirectorySyncService.getEffectiveUid(myProfile)
        val otherUid = recipient.uid.ifBlank { recipient.id }

        val threadId = RealChatRepository.getDmThreadId(
            uid1 = myUid,
            username1 = myUsername,
            uid2 = otherUid,
            username2 = otherUsername
        )

        viewModelScope.launch {
            val res = chatRepository.deleteDirectMessageByThread(threadId, messageId, myUid)
            if (res.isSuccess) {
                _personalMessages.value = _personalMessages.value.filter { it.id != messageId }
                showBanner("Message delete kar diya gaya 🗑️")
            } else {
                Log.w("CricketViewModel", "Failed to delete DM: ${res.exceptionOrNull()?.message}")
            }
        }
    }

    fun closeDirectMessageChat() {
        directChatJob?.cancel()
        _activeDmRecipient.value = null
        _personalMessages.value = emptyList()
    }

    // Send 1-on-1 Personal Direct Message (Text, Photo, Video, or Snap)
    fun sendDirectMessage(
        recipient: CricHeroesProfile,
        text: String,
        mediaUrl: String = "",
        mediaType: String = "",
        isSnap: Boolean = false
    ) {
        if (text.isBlank() && mediaUrl.isBlank()) return
        val myProfile = _userProfile.value
        val myUser = myProfile.username.trim().removePrefix("@").lowercase()
        val myUid = UserDirectorySyncService.getEffectiveUid(myProfile)
        val otherUser = recipient.username.trim().removePrefix("@").lowercase()
        val recipientUid = recipient.uid.ifBlank { recipient.id }

        viewModelScope.launch {
            chatRepository.sendDirectMessage(
                senderUid = myUid,
                senderUsername = myUser,
                senderName = myProfile.jerseyName.ifBlank { myProfile.fullName },
                senderRole = myProfile.primaryRole,
                avatarEmoji = myProfile.avatarEmoji.ifBlank { "🏏" },
                recipientUid = recipientUid,
                recipientUsername = otherUser,
                text = text.trim(),
                mediaUrl = mediaUrl,
                mediaType = mediaType,
                isSnap = isSnap
            )
        }
    }

    fun sendDirectMessage(
        recipientUsername: String,
        text: String,
        mediaUrl: String = "",
        mediaType: String = "",
        isSnap: Boolean = false
    ) {
        val target = _activeDmRecipient.value
            ?: _communityPlayers.value.find {
                it.username.trim().removePrefix("@").equals(recipientUsername.trim().removePrefix("@"), ignoreCase = true)
            }
            ?: return
        sendDirectMessage(target, text, mediaUrl, mediaType, isSnap)
    }

    fun markSnapOpened(msg: ChatMessage) {
        viewModelScope.launch {
            val myUsername = _userProfile.value.username.trim().removePrefix("@").lowercase()
            val otherUser = if (msg.senderUsername.trim().removePrefix("@").lowercase() == myUsername) {
                msg.receiverUsername.trim().removePrefix("@").lowercase()
            } else {
                msg.senderUsername.trim().removePrefix("@").lowercase()
            }
            val threadId = RealChatRepository.getDmThreadId(
                uid1 = _userProfile.value.uid,
                username1 = myUsername,
                uid2 = if (msg.senderUsername.trim().removePrefix("@").lowercase() == myUsername) msg.recipientUid else msg.senderUid,
                username2 = otherUser
            )
            chatRepository.markSnapOpened(threadId, msg.id)
        }
    }

    fun isUsernameAvailable(username: String): Boolean {
        val clean = username.trim().removePrefix("@").lowercase()
        if (clean.length < 3) return false
        val myUsername = _userProfile.value.username.trim().removePrefix("@").lowercase()
        if (clean == myUsername) return true
        // Ayush is the verified creator & admin - @ayush_7 is ALWAYS allowed for him
        if (clean == "ayush_7") {
            return true
        }
        val authUser = try { com.google.firebase.auth.FirebaseAuth.getInstance().currentUser } catch (_: Throwable) { null }
        val myUid = _userProfile.value.id
        val myFirebaseUid = authUser?.uid
        return _communityPlayers.value.none {
            val otherClean = it.username.trim().removePrefix("@").lowercase()
            otherClean == clean && it.id != myUid && it.uid != myUid && (myFirebaseUid == null || it.uid != myFirebaseUid)
        }
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
                UserDirectorySyncService.observeCommunityUsers().collect { remoteUsers ->
                    if (remoteUsers.isNotEmpty()) {
                        val myProfile = _userProfile.value
                        val merged = mutableListOf<CricHeroesProfile>()
                        if (myProfile.username.isNotBlank()) {
                            merged.add(myProfile)
                        }
                        remoteUsers.forEach { cu ->
                            val cleanCu = cu.username.trim().removePrefix("@").lowercase()
                            val isDuplicate = merged.any {
                                (cleanCu.isNotBlank() && it.username.trim().removePrefix("@").lowercase() == cleanCu) ||
                                it.uid == cu.uid ||
                                (it.fullName.trim().equals(cu.fullName.trim(), ignoreCase = true) && it.jerseyNumber == cu.jerseyNumber && it.jerseyNumber > 0)
                            }
                            if (!isDuplicate) {
                                merged.add(cu)
                            }
                        }
                        _communityPlayers.value = merged
                    }
                }
            } catch (e: Throwable) {
                Log.w("CricketViewModel", "syncCloudUsers failed: ${e.message}")
            }
        }
    }

    fun refreshCloudMessages() {
        // Firestore real-time snapshot listeners are reactive
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

    private val _viewingHistoricalPlayer = MutableStateFlow<CricHeroesProfile?>(null)
    val viewingHistoricalPlayer = _viewingHistoricalPlayer.asStateFlow()

    fun openHistoricalStatsForPlayer(profile: CricHeroesProfile) { _viewingHistoricalPlayer.value = profile }
    fun closeHistoricalStats() { _viewingHistoricalPlayer.value = null }

    fun updateMatchSquad(
        striker: String,
        nonStriker: String,
        bowler: String,
        teamAPlayers: String,
        teamBPlayers: String
    ) {
        val match = currentMatch.value ?: return
        viewModelScope.launch {
            scoringMutex.withLock {
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
                    val squadDialogue = "ओए गुरु! दोनों टीमों का प्लेइंग स्क्वाड और खिलाड़ी तय हो चुके हैं! दोनों कप्तानों ने अपने लड़ाके मैदान में उतार दिए हैं! अब होगा असली दंगल, चक दे फट्टे, ठोको ताली गुरु!"
                    sidhuCommentaryManager.speak(squadDialogue)
                    lastRemoteSpokenBallTs = System.currentTimeMillis() + 1500L
                    RealtimeMatchSyncService.publishMatch(updated, sidhuDialogue = squadDialogue)
                }
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
            scoringMutex.withLock {
                val matchId = "match_local_${System.currentTimeMillis()}"
                if (pin.isNotBlank()) {
                    _officialPin.value = pin
                    rolePrefs.edit().putString("admin_pin", pin).apply()
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
                    val freshDialogue = "ओए चक दे फट्टे! नया मुकाबला शुरू हो चुका है गुरु! ${created.teamA} बनाम ${created.teamB}! मैदान सज चुका है, खिलाड़ी तैयार हैं! ठोको ताली गुरु!"
                    sidhuCommentaryManager.speak(freshDialogue)
                    lastRemoteSpokenBallTs = System.currentTimeMillis() + 1500L
                    RealtimeMatchSyncService.publishMatch(created, sidhuDialogue = freshDialogue)
                    com.example.notification.MatchNotificationHelper.notifyMatchLive(getApplication(), created, forceNotify = true)
                    com.example.widget.LiveScoreAppWidgetProvider.updateAllWidgets(getApplication(), created)
                }
                showBanner("Match Created! Official match roles are now active.")
            }
        }
    }

    private val _showStartSecondInningsDialog = MutableStateFlow(false)
    val showStartSecondInningsDialog = _showStartSecondInningsDialog.asStateFlow()

    fun openStartSecondInningsDialog() {
        val currentMatchObj = currentMatch.value
        if (currentMatchObj == null) {
            showBanner("Koi match select nahi hai!")
            return
        }
        if (currentMatchObj.currentInnings >= 2) {
            showBanner("Match ki 2nd Innings pehle se chal rahi hai!")
            return
        }
        val isScorerOrOwner = _currentDeviceRole.value == DeviceRole.OFFICIAL_SCORER || isAppOwner() || _currentDeviceRole.value.isOfficial
        if (!isScorerOrOwner) {
            showBanner("⛔ Sirf Official Match Scorer hi 2nd Innings shuru kar sakta hai!")
            return
        }
        _showStartSecondInningsDialog.value = true
    }

    fun closeStartSecondInningsDialog() {
        _showStartSecondInningsDialog.value = false
    }

    fun startSecondInnings(striker: String, nonStriker: String, bowler: String): Boolean {
        val matchId = _selectedMatchId.value
        if (matchId.isBlank()) return false
        val isScorerOrOwner = _currentDeviceRole.value == DeviceRole.OFFICIAL_SCORER || isAppOwner() || _currentDeviceRole.value.isOfficial
        if (!isScorerOrOwner) {
            showBanner("⛔ Sirf Official Match Scorer hi 2nd Innings shuru kar sakta hai!")
            return false
        }
        viewModelScope.launch {
            scoringMutex.withLock {
                val updated = repository.startSecondInnings(matchId, striker, nonStriker, bowler)
                if (updated != null) {
                    _showStartSecondInningsDialog.value = false
                    val commentary = "ओए चक दे फट्टे गुरु! पहली पारी समाप्त! अब ${updated.battingTeam} को जीत के लिए चाहिए ${updated.target} रन! दूसरी पारी का मुकाबला शुरू हो चुका है गुरु! ठोको ताली!"
                    sidhuCommentaryManager.speak(commentary)
                    RealtimeMatchSyncService.publishMatch(updated, sidhuDialogue = commentary)
                    try {
                        com.example.notification.MatchNotificationHelper.notifyMatchLive(getApplication(), updated, forceNotify = true)
                        com.example.widget.LiveScoreAppWidgetProvider.updateAllWidgets(getApplication(), updated)
                    } catch (_: Throwable) {}
                    showBanner("🏏 2nd Innings Started! Target: ${updated.target} runs for ${updated.battingTeam}")
                }
            }
        }
        return true
    }

    fun switchBattingTeamAtZero(): Boolean {
        val matchId = _selectedMatchId.value
        if (matchId.isBlank()) return false
        val currentMatchObj = currentMatch.value ?: return false
        if (currentMatchObj.legalBalls > 0) {
            showBanner("Gend phenk chuke hain, ab innings beech me switch nahi ho sakti!")
            return false
        }
        viewModelScope.launch {
            scoringMutex.withLock {
                val updated = repository.switchBattingTeamAtZero(matchId)
                if (updated != null) {
                    val commentary = "ओए टॉस और बैटिंग बदल दी गई है गुरु! अब ${updated.battingTeam} करेगी पहले बल्लेबाजी और ${updated.bowlingTeam} गेंदबाजी! ठोको ताली गुरु!"
                    sidhuCommentaryManager.speak(commentary)
                    RealtimeMatchSyncService.publishMatch(updated, sidhuDialogue = commentary)
                    try {
                        com.example.notification.MatchNotificationHelper.notifyMatchLive(getApplication(), updated, forceNotify = true)
                        com.example.widget.LiveScoreAppWidgetProvider.updateAllWidgets(getApplication(), updated)
                    } catch (_: Throwable) {}
                    showBanner("Batting Switched! 🏏 ${updated.battingTeam} is now Batting first")
                }
            }
        }
        return true
    }

    fun resetCurrentMatchToZero(enteredPin: String? = null): Boolean {
        val cleanPin = enteredPin?.trim() ?: ""
        val authorized = isAppOwner() ||
                         _currentDeviceRole.value == DeviceRole.OFFICIAL_SCORER ||
                         _currentDeviceRole.value.isOfficial ||
                         cleanPin == "200910" ||
                         (cleanPin.isNotBlank() && cleanPin == _officialPin.value.trim()) ||
                         (_userProfile.value.pin.isNotBlank() && cleanPin == _userProfile.value.pin.trim())
        if (!authorized) {
            showBanner("⛔ Access Denied: Match records reset karne ka adhikar sirf Official Scorer ya Owner (Ayush) ke paas hai!")
            return false
        }
        viewModelScope.launch {
            scoringMutex.withLock {
                repository.resetCurrentMatchToZero(_selectedMatchId.value)
                val updated = repository.getMatch(_selectedMatchId.value).firstOrNull()
                if (updated != null) {
                    RealtimeMatchSyncService.publishMatch(updated)
                }
                showBanner("Match reset! Score is now 0/0 (0.0 ov)")
            }
        }
        return true
    }

    fun resetAllAndStartFresh(params: com.example.data.model.CreateMatchParams) {
        resetAllAndStartFresh(
            name = params.name,
            teamA = params.teamA,
            teamB = params.teamB,
            overs = params.overs,
            striker = params.striker,
            nonStriker = params.nonStriker,
            bowler = params.bowler,
            venue = params.venue,
            pin = params.pin,
            battingTeam = params.battingTeam,
            bowlingTeam = params.bowlingTeam,
            tossDetail = params.tossDetail,
            teamAPlayers = params.teamAPlayers,
            teamBPlayers = params.teamBPlayers,
            matchDate = params.matchDate,
            matchTime = params.matchTime,
            venueAddress = params.venueAddress,
            venueCoordinates = params.venueCoordinates
        )
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
        pin: String,
        battingTeam: String = "",
        bowlingTeam: String = "",
        tossDetail: String = "",
        teamAPlayers: String = "",
        teamBPlayers: String = "",
        matchDate: String = "",
        matchTime: String = "",
        venueAddress: String = "",
        venueCoordinates: String = ""
    ) {
        viewModelScope.launch {
            scoringMutex.withLock {
                if (pin.isNotBlank()) {
                    _officialPin.value = pin
                    rolePrefs.edit().putString("admin_pin", pin).apply()
                }
                val fresh = repository.resetAllToZeroAndStartFresh(
                    matchName = name.ifBlank { "Local Match" },
                    teamA = teamA.ifBlank { "Team A" },
                    teamB = teamB.ifBlank { "Team B" },
                    overs = if (overs > 0) overs else 10,
                    striker = striker.ifBlank { "Striker" },
                    nonStriker = nonStriker.ifBlank { "Non-Striker" },
                    bowler = bowler.ifBlank { "Opening Bowler" },
                    venue = venue.ifBlank { "Local Ground" },
                    battingTeam = battingTeam,
                    bowlingTeam = bowlingTeam,
                    tossDetail = tossDetail,
                    teamAPlayers = teamAPlayers,
                    teamBPlayers = teamBPlayers,
                    matchDate = matchDate,
                    matchTime = matchTime,
                    venueAddress = venueAddress,
                    venueCoordinates = venueCoordinates
                )
                _selectedMatchId.value = fresh.id
                _showCreateMatchDialog.value = false
                val freshDialogue = "ओए चक दे फट्टे! नया मुकाबला शुरू हो चुका है गुरु! ${fresh.teamA} बनाम ${fresh.teamB}, ${fresh.totalOvers} ओवर का महामुकाबला! ${fresh.strikerName} और ${fresh.nonStrikerName} क्रीज पर तैयार हैं, गेंद ${fresh.bowlerName} के हाथ में! अब सब मिलकर ठोको ताली गुरु!"
                sidhuCommentaryManager.speak(freshDialogue)
                lastRemoteSpokenBallTs = System.currentTimeMillis() + 1500L
                RealtimeMatchSyncService.publishMatch(fresh, sidhuDialogue = freshDialogue)
                com.example.notification.MatchNotificationHelper.notifyMatchLive(getApplication(), fresh, forceNotify = true)
                com.example.widget.LiveScoreAppWidgetProvider.updateAllWidgets(getApplication(), fresh)
                showBanner("Match shuru! Toss: ${tossDetail.ifBlank { "$battingTeam batting karegi" }}")
            }
        }
    }

    fun updateMatchVenue(
        matchId: String,
        venue: String,
        date: String = "",
        time: String = "",
        address: String = "",
        coordinates: String = ""
    ) {
        viewModelScope.launch {
            scoringMutex.withLock {
                val updated = repository.updateMatchVenueAndSchedule(
                    matchId = matchId,
                    venue = venue,
                    date = date,
                    time = time,
                    address = address,
                    coordinates = coordinates
                )
                if (updated != null) {
                    _showEditVenueDialog.value = false
                    _editingMatchVenue.value = null
                    RealtimeMatchSyncService.publishMatch(updated)
                    showBanner("📍 Match Venue & Schedule updated: ${updated.venue}")
                }
            }
        }
    }

    fun syncMatchesFromCloud() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                RealtimeMatchSyncService.ensureAuth()
                val remoteMatches = RealtimeMatchSyncService.fetchRemoteMatchesOnce()
                    .filter { it.id != "match_live_1" && !it.id.startsWith("dummy") && !it.id.startsWith("sample") }
                if (remoteMatches.isNotEmpty()) {
                    repository.upsertMatchesFromFirestore(remoteMatches)
                    val activeOrFirst = remoteMatches.firstOrNull { it.status == "LIVE" } ?: remoteMatches.first()
                    if (_selectedMatchId.value.isBlank() || !remoteMatches.any { it.id == _selectedMatchId.value }) {
                        _selectedMatchId.value = activeOrFirst.id
                    }
                    showBanner("Cloud Sync: ${remoteMatches.size} live match sync ho gaye! ☁️")
                } else {
                    showBanner("Cloud checking: Abhi naya live match sync ho raha hai... ⏳")
                }
            } catch (e: Exception) {
                Log.w("CricketViewModel", "syncMatchesFromCloud error: ${e.message}")
            }
        }
    }

    fun refreshStandingsFromFirestore() {
        viewModelScope.launch(Dispatchers.IO) {
            _isRefreshingStandings.value = true
            try {
                RealtimeMatchSyncService.ensureAuth()
                val cloudStandings = RealtimeMatchSyncService.fetchTournamentStandingsOnce()
                if (cloudStandings.isNotEmpty()) {
                    repository.saveTeamStandings(cloudStandings)
                    showBanner("🏆 Points Table updated from Cloud Firestore (${cloudStandings.size} teams)!")
                } else {
                    val allM = repository.allMatches.first()
                    val (computedStandings, _) = TournamentStatsCalculator.computeStandingsAndStats(allM)
                    if (computedStandings.isNotEmpty()) {
                        repository.saveTeamStandings(computedStandings)
                        RealtimeMatchSyncService.publishTournamentStandings(computedStandings)
                        showBanner("🏆 Points Table synced & saved to Cloud Firestore!")
                    } else {
                        showBanner("Cloud Firestore Points Table checked!")
                    }
                }
            } catch (e: Exception) {
                Log.w("CricketViewModel", "refreshStandingsFromFirestore error: ${e.message}")
                showBanner("Points Table sync error: ${e.message}")
            } finally {
                _isRefreshingStandings.value = false
            }
        }
    }

    private fun listenToCloudStandings() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                RealtimeMatchSyncService.listenToTournamentStandings().collect { cloudStandings ->
                    if (cloudStandings.isNotEmpty()) {
                        repository.saveTeamStandings(cloudStandings)
                    }
                }
            } catch (e: Exception) {
                Log.w("CricketViewModel", "listenToCloudStandings error: ${e.message}")
            }
        }
    }

    fun undoLastDelivery() {
        viewModelScope.launch {
            scoringMutex.withLock {
                repository.undoLastDelivery(_selectedMatchId.value)
                val updated = repository.getMatch(_selectedMatchId.value).firstOrNull()
                if (updated != null) {
                    RealtimeMatchSyncService.publishMatch(updated)
                }
                showBanner("Last delivery undone by Official Scorer!")
            }
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

        // Announce decision via Sidhu Paaji voice
        val sidhuSpeech = when (decision) {
            "OUT" -> "ओए गुरु! फैसला आ गया - OUT! पवेलियन लौटना पड़ेगा, तीसरा अंपायर ने उंगली उठा दी! ठोको ताली गुरु!"
            "NOT OUT" -> "ओए गुरु! फैसला आ गया - NOT OUT! बल्लेबाज सुरक्षित है, जान बची तो लाखों पाए!"
            else -> "ओए गुरु! अंपायर्स कॉल! फील्ड अंपायर का फैसला बरकरार रहेगा!"
        }
        sidhuCommentaryManager.speak(sidhuSpeech)

        try {
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("live_matches")
                .document(current.id)
                .collection("drs_verdicts")
                .add(
                    hashMapOf(
                        "matchId" to current.id,
                        "decision" to decision,
                        "appealType" to appealType,
                        "batsman" to current.strikerName,
                        "bowler" to current.bowlerName,
                        "reason" to reason,
                        "decidedBy" to "Third Umpire (Official)",
                        "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                    )
                )
        } catch (e: Throwable) {
            android.util.Log.w("CricketViewModel", "Could not broadcast DRS verdict to Firestore: ${e.message}")
        }

        // TV Broadcast Decision Card popup across ALL devices (Spectators & Officials)
        val decisionCard = DrsDecisionCardData(
            matchId = current.id,
            appealType = appealType,
            batsman = current.strikerName,
            bowler = current.bowlerName,
            onFieldDecision = _drsState.value.onFieldDecision,
            thirdUmpireDecision = decision,
            pitching = _drsState.value.pitching,
            impact = _drsState.value.impact,
            wickets = _drsState.value.wicketsHitting,
            timestamp = System.currentTimeMillis()
        )
        _drsDecisionPopup.value = decisionCard
        RealtimeMatchSyncService.publishDrsDecision(decisionCard)
        RealtimeMatchSyncService.clearDrsAppeal(current.id)

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
        lastHandledDrsAppealTs = System.currentTimeMillis()
        _drsBroadcast.value = null
        _drsDecisionPopup.value = null
        resetDrs()
        if (_selectedMatchId.value.isNotBlank()) {
            RealtimeMatchSyncService.clearDrsDecision(_selectedMatchId.value)
            RealtimeMatchSyncService.clearDrsAppeal(_selectedMatchId.value)
        }
        _currentTab.value = AppScreenTab.LIVE_CENTER
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

    fun showBanner(msg: String) {
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
        matchChatJob?.cancel()
        directChatJob?.cancel()
        sidhuCommentaryManager.shutdown()
    }

    companion object {
        const val MESSAGE_RETENTION_MILLIS = 30L * 24 * 60 * 60 * 1000L // 30 Days auto-cleanup
    }
}