package com.example.data.model

data class BattingInningsRecord(
    val matchTitle: String = "Tournament Match",
    val opponent: String = "Opponent XI",
    val date: String = "Recent",
    val runs: Int = 0,
    val balls: Int = 0,
    val fours: Int = 0,
    val sixes: Int = 0,
    val isNotOut: Boolean = false,
    val strikeRate: Double = if (balls > 0) (runs.toDouble() / balls) * 100 else 0.0,
    val dismissal: String = "not out",
    val venue: String = "Cricket Ground"
)

data class BowlingSpellRecord(
    val matchTitle: String = "Tournament Match",
    val opponent: String = "Opponent XI",
    val date: String = "Recent",
    val overs: Double = 4.0,
    val maidens: Int = 0,
    val runs: Int = 0,
    val wickets: Int = 0,
    val economy: Double = if (overs > 0) runs / overs else 0.0,
    val dotBalls: Int = 0,
    val venue: String = "Cricket Ground"
)

data class BattingHistoricalStats(
    val matches: Int = 0,
    val innings: Int = 0,
    val notOuts: Int = 0,
    val runs: Int = 0,
    val highestScore: String = "0",
    val average: Double = 0.0,
    val strikeRate: Double = 0.0,
    val fifties: Int = 0,
    val hundreds: Int = 0,
    val fours: Int = 0,
    val sixes: Int = 0,
    val ducks: Int = 0,
    val ballsFaced: Int = 0,
    val recentInnings: List<BattingInningsRecord> = emptyList()
) {
    val runsInBoundaries: Int
        get() = (fours * 4) + (sixes * 6)

    val boundaryPercentage: Double
        get() = if (runs > 0) ((runsInBoundaries.toDouble() / runs) * 100).coerceIn(0.0, 100.0) else 0.0
}

data class BowlingHistoricalStats(
    val matches: Int = 0,
    val innings: Int = 0,
    val overs: Double = 0.0,
    val maidens: Int = 0,
    val runsConceded: Int = 0,
    val wickets: Int = 0,
    val bestBowling: String = "0/0",
    val average: Double = 0.0,
    val economyRate: Double = 0.0,
    val strikeRate: Double = 0.0,
    val threeWickets: Int = 0,
    val fiveWickets: Int = 0,
    val dotBalls: Int = 0,
    val recentSpells: List<BowlingSpellRecord> = emptyList()
) {
    val dotBallPercentage: Double
        get() {
            val totalBalls = (overs.toInt() * 6) + ((overs * 10).toInt() % 10)
            return if (totalBalls > 0) ((dotBalls.toDouble() / totalBalls) * 100).coerceIn(0.0, 100.0) else 0.0
        }
}

data class PlayerMilestone(
    val icon: String = "🏆",
    val title: String = "Achievement",
    val subtitle: String = "",
    val date: String = ""
)

data class PlayerHistoricalStats(
    val playerId: String = "",
    val uid: String = "",
    val username: String = "",
    val fullName: String = "",
    val jerseyName: String = "",
    val jerseyNumber: Int = 0,
    val primaryRole: String = "All-Rounder",
    val battingStyle: String = "Right-hand Bat",
    val bowlingStyle: String = "Right-arm Medium",
    val teamName: String = "",
    val city: String = "",
    val avatarEmoji: String = "🏏",
    val isVerified: Boolean = true,
    val batting: BattingHistoricalStats = BattingHistoricalStats(),
    val bowling: BowlingHistoricalStats = BowlingHistoricalStats(),
    val milestones: List<PlayerMilestone> = emptyList(),
    val lastSyncedTimestamp: Long = System.currentTimeMillis()
)
