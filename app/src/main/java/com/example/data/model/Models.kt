package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "matches")
data class MatchEntity(
    @PrimaryKey val id: String,
    val tournamentName: String,
    val teamA: String,
    val teamB: String,
    val teamAShort: String,
    val teamBShort: String,
    val teamAColorHex: Long = 0xFF3B82F6,
    val teamBColorHex: Long = 0xFFEF4444,
    val currentInnings: Int = 2,
    val battingTeam: String,
    val bowlingTeam: String,
    val score: Int,
    val wickets: Int,
    val legalBalls: Int, // e.g. 84 = 14.0 overs (84 / 6 + (84 % 6) / 10f)
    val totalOvers: Int = 20,
    val target: Int = 0,
    val status: String = "LIVE", // "LIVE", "FINISHED", "UPCOMING"
    val statusDetail: String = "",
    val strikerName: String = "Striker",
    val strikerRuns: Int = 0,
    val strikerBalls: Int = 0,
    val strikerFours: Int = 0,
    val strikerSixes: Int = 0,
    val nonStrikerName: String = "Non-Striker",
    val nonStrikerRuns: Int = 0,
    val nonStrikerBalls: Int = 0,
    val nonStrikerFours: Int = 0,
    val nonStrikerSixes: Int = 0,
    val bowlerName: String = "Bowler",
    val bowlerBalls: Int = 0,
    val bowlerMaidens: Int = 0,
    val bowlerRuns: Int = 0,
    val bowlerWickets: Int = 0,
    val venue: String = "",
    val teamAFirstInningsScore: String = "",
    val teamAPlayers: String = "",
    val teamBPlayers: String = ""
)

@Entity(tableName = "ball_events")
data class BallEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val matchId: String,
    val overNumber: Int,
    val ballInOver: Int,
    val runs: Int,
    val isWicket: Boolean = false,
    val wicketType: String = "", // Bowled, Caught, LBW, Run Out
    val extraType: String = "None", // None, Wide, NoBall, LegBye, Bye
    val batsman: String,
    val bowler: String,
    val commentary: String,
    val shotAngle: Float = 45f, // 0 to 360 degrees for wagon wheel
    val pitchZone: String = "Good Length", // Yorker, Good Length, Short, Full
    val isBoundary: Boolean = false,
    val isSix: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "team_standings")
data class TeamStandingEntity(
    @PrimaryKey val teamId: String,
    val name: String,
    val shortName: String,
    val matchesPlayed: Int,
    val won: Int,
    val lost: Int,
    val tied: Int,
    val points: Int,
    val netRunRate: Double,
    val formGuide: String // e.g. "W,W,L,W,L"
)

@Entity(tableName = "player_stats")
data class PlayerStatEntity(
    @PrimaryKey val playerId: String,
    val name: String,
    val team: String,
    val role: String,
    val matches: Int,
    val runs: Int,
    val highestScore: Int,
    val wickets: Int,
    val strikeRate: Double,
    val economy: Double,
    val fantasyPoints: Int,
    val ranking: Int,
    val isOrangeCap: Boolean = false,
    val isPurpleCap: Boolean = false
)

@Entity(tableName = "notifications")
data class NotificationAlertEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val message: String,
    val type: String, // "WICKET", "SIX", "DRS", "MILESTONE", "MATCH_STATUS"
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)

data class HighlightClip(
    val id: String,
    val matchId: String,
    val title: String,
    val description: String,
    val durationText: String,
    val category: String, // "SIX", "WICKET", "DRS", "CLUTCH"
    val overText: String,
    val bowlerVsBatter: String,
    val animationType: String
)

enum class DeviceRole(
    val title: String,
    val hindiTitle: String,
    val phoneLabel: String,
    val description: String,
    val isOfficial: Boolean
) {
    BOWLER_END_UMPIRE(
        title = "Bowler-End Umpire",
        hindiTitle = "Main Umpire (Pitch Cam)",
        phoneLabel = "Phone 1",
        description = "Behind stumps camera streaming pitch, line & bowler crease",
        isOfficial = true
    ),
    SQUARE_LEG_UMPIRE(
        title = "Square Leg Umpire",
        hindiTitle = "Side Umpire (Crease Cam)",
        phoneLabel = "Phone 2",
        description = "Side-on camera recording crease line for run-outs & stumpings",
        isOfficial = true
    ),
    OFFICIAL_SCORER(
        title = "Official Match Scorer",
        hindiTitle = "Scorer (Runs & Wickets Count)",
        phoneLabel = "Phone 3",
        description = "Exclusive authority to count runs, wickets, extras & change overs",
        isOfficial = true
    ),
    THIRD_UMPIRE_DRS(
        title = "Third Umpire (DRS Decider)",
        hindiTitle = "DRS Officer (Out / Not Out)",
        phoneLabel = "Phone 4",
        description = "Inspects dual camera angles, slow-mo & declares final Out/Not Out",
        isOfficial = true
    ),
    SPECTATOR_VIEWER(
        title = "Live Spectator / Fan",
        hindiTitle = "Live Viewer (Baki Sab)",
        phoneLabel = "Public Phones",
        description = "Watch live dual-cam stream, real-time score & DRS verdicts",
        isOfficial = false
    )
}

data class DrsBroadcastAlert(
    val isVisible: Boolean = false,
    val decision: String = "", // "OUT", "NOT OUT", "UMPIRE'S CALL", "REVIEW IN PROGRESS"
    val appealType: String = "", // "LBW", "RUN_OUT", "STUMPED", "CAUGHT_BEHIND"
    val batsman: String = "",
    val bowler: String = "",
    val reason: String = "",
    val decidedByPhone: String = "Phone 4 (Third Umpire)",
    val timestamp: Long = System.currentTimeMillis()
)

data class DrsReviewState(
    val appealType: String = "LBW", // "LBW", "RUN_OUT", "STUMPED", "CAUGHT_BEHIND"
    val batsman: String = "Batsman",
    val bowler: String = "Bowler",
    val onFieldDecision: String = "NOT OUT", // "OUT" or "NOT OUT"
    val reviewBy: String = "Bowling Team",
    val isFrontFootNoBall: Boolean = false,
    val ultraEdgeSpike: Boolean = false, // Has snicko spike
    val ultraEdgeFramePosition: Float = 0.5f,
    // Hawk eye LBW parameters
    val pitching: String = "IN_LINE", // "IN_LINE", "OUTSIDE_OFF", "OUTSIDE_LEG"
    val impact: String = "IN_LINE", // "IN_LINE", "OUTSIDE"
    val wicketsHitting: String = "HITTING", // "HITTING", "MISSING", "UMPIRES_CALL"
    val aiConfidencePercent: Int = 94,
    val deviationDegree: Float = 1.8f,
    val impactDistanceFromStumpsMeters: Float = 1.9f,
    val thirdUmpireDecision: String = "OUT", // "OUT", "NOT OUT", "UMPIRES_CALL"
    val reviewStage: Int = 0, // 0: Idle, 1: Front Foot, 2: UltraEdge / Side Cam, 3: HawkEye / Crease Line, 4: Decision
    val selectedCameraAngle: String = "ANGLE_1_PITCH", // ANGLE_1_PITCH, ANGLE_2_SIDE_CREASE, SPLIT_VIEW
    val frameIndex: Int = 14, // 0..30 frames for slow-mo scrutiny
    val creaseLineOffset: Float = 0f // Draggable calibration for run-out line
)

data class CricHeroesProfile(
    val id: String = "player_1",
    val username: String = "", // Unique username e.g. @ayush_7
    val mobileNumber: String = "",
    val fullName: String = "",
    val jerseyName: String = "",
    val jerseyNumber: Int = 0,
    val primaryRole: String = "All-Rounder",
    val battingStyle: String = "Right-hand Bat",
    val bowlingStyle: String = "Right-arm Medium",
    val teamName: String = "",
    val city: String = "",
    val avatarEmoji: String = "🏏",
    val badgeTitle: String = "AYUUCRIC PLAYER",
    val matchesPlayed: Int = 0,
    val runs: Int = 0,
    val wickets: Int = 0,
    val strikeRate: Double = 0.0,
    val isVerified: Boolean = true,
    val isOnline: Boolean = true
) {
    val phoneNumber: String get() = if (mobileNumber.isNotBlank()) mobileNumber else "+919876543210"
}

data class RoleChangeRequest(
    val id: String,
    val applicantName: String,
    val applicantPhone: String,
    val requestedRole: DeviceRole,
    val reason: String,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "PENDING" // "PENDING", "APPROVED", "DENIED"
)

// Public match banter message & Direct Real-time Chat
data class ChatMessage(
    val id: String,
    val senderName: String,
    val senderRole: String = "Fan",
    val avatarEmoji: String = "🏏",
    val message: String,
    val isFromMe: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val reaction: String? = null
) {
    val text: String get() = message
    val senderUsername: String get() = senderName
}

// Real 1-on-1 Instagram-Style Personal Direct Message (DM)
data class DirectPersonalMessage(
    val id: String,
    val senderUsername: String,
    val recipientUsername: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val reactionEmoji: String? = null
)

// Hotstar-Style Live Broadcast Graphic Overlays (TV Lower-Thirds)
data class BroadcastOverlayEvent(
    val id: String = java.util.UUID.randomUUID().toString(),
    val type: OverlayType,
    val headline: String,
    val subheadline: String,
    val statDetail: String = "",
    val accentColorHex: Long = 0xFFFFD700
) {
    enum class OverlayType {
        MILESTONE_50,
        MILESTONE_100,
        MAXIMUM_SIX,
        BOUNDARY_FOUR,
        WICKET_DISMISSAL,
        PARTNERSHIP_RECORD
    }
}


