package com.example.data.repository

import com.example.data.local.CricketDao
import com.example.data.model.BallEventEntity
import com.example.data.model.HighlightClip
import com.example.data.model.MatchEntity
import com.example.data.model.NotificationAlertEntity
import com.example.data.model.PlayerStatEntity
import com.example.data.model.TeamStandingEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class CricketRepository(private val dao: CricketDao) {

    val allMatches: Flow<List<MatchEntity>> = dao.getAllMatches()
    val teamStandings: Flow<List<TeamStandingEntity>> = dao.getTeamStandings()
    val playerStats: Flow<List<PlayerStatEntity>> = dao.getPlayerStats()
    val notifications: Flow<List<NotificationAlertEntity>> = dao.getNotifications()

    fun getMatch(matchId: String): Flow<MatchEntity?> = dao.getMatchById(matchId)
    fun getBallEvents(matchId: String): Flow<List<BallEventEntity>> = dao.getBallEventsForMatch(matchId)

    fun getFallbackMatches(): List<MatchEntity> = listOf(sampleMatch)
    fun getFallbackMatch(id: String = "match_live_1"): MatchEntity = sampleMatch
    fun getFallbackBallEvents(id: String = "match_live_1"): List<BallEventEntity> = sampleBallEvents

    suspend fun initializeDefaultDataIfEmpty() {
        val existingMatches = dao.getAllMatches().first()
        if (existingMatches.isEmpty()) {
            val initialMatches = listOf(sampleMatch)
            dao.insertMatches(initialMatches)
        }

        // Clean out any pre-existing dummy records so table and stats start completely clean
        dao.deleteAllStandings()
        dao.deleteAllPlayerStats()

        // Seed Notifications
        val seedNotifications = listOf(
            NotificationAlertEntity(
                title = "Match Alert: Live Match Ready! 🏏",
                message = "Scorecard initialized and ready for ball-by-ball recording.",
                type = "MATCH_STATUS"
            )
        )
        seedNotifications.forEach { dao.insertNotification(it) }
    }

    suspend fun clearAllStandingsAndStats() {
        dao.deleteAllStandings()
        dao.deleteAllPlayerStats()
    }

    suspend fun recordDelivery(
        matchId: String,
        runs: Int,
        isWicket: Boolean,
        wicketType: String,
        extraType: String,
        commentary: String,
        shotAngle: Float,
        pitchZone: String
    ) {
        val match = dao.getMatchById(matchId).first() ?: return

        val isLegal = extraType != "Wide" && extraType != "NoBall"
        val totalRunToAdd = runs + if (extraType == "Wide" || extraType == "NoBall") 1 else 0
        val newScore = match.score + totalRunToAdd
        val newWickets = if (isWicket) (match.wickets + 1).coerceAtMost(10) else match.wickets
        val newLegalBalls = if (isLegal) match.legalBalls + 1 else match.legalBalls
        val newOver = newLegalBalls / 6
        val newBallInOver = newLegalBalls % 6

        // Striker updates
        val strikerRuns = if (extraType == "Wide") match.strikerRuns else match.strikerRuns + runs
        val strikerBalls = if (extraType == "Wide") match.strikerBalls else match.strikerBalls + 1
        val striker4s = match.strikerFours + if (runs == 4) 1 else 0
        val striker6s = match.strikerSixes + if (runs == 6) 1 else 0

        // Bowler updates
        val bowlerBalls = if (isLegal) match.bowlerBalls + 1 else match.bowlerBalls
        val bowlerRuns = match.bowlerRuns + totalRunToAdd
        val bowlerWickets = if (isWicket && wicketType != "Run Out") match.bowlerWickets + 1 else match.bowlerWickets

        // Runs needed calculation
        val remainingRuns = (match.target - newScore).coerceAtLeast(0)
        val remainingBalls = ((match.totalOvers * 6) - newLegalBalls).coerceAtLeast(0)
        val detail = if (match.target > 0 && newScore >= match.target) {
            "${match.battingTeam} won by ${10 - newWickets} wickets!"
        } else if (match.target > 0 && (newWickets >= 10 || remainingBalls == 0)) {
            "${match.bowlingTeam} won by ${remainingRuns} runs!"
        } else if (match.target > 0) {
            "Need $remainingRuns runs in $remainingBalls balls (RRR: ${"%.2f".format(if (remainingBalls > 0) remainingRuns * 6f / remainingBalls else 0f)})"
        } else {
            "${match.battingTeam} batting • $newScore/$newWickets (${newLegalBalls / 6}.${newLegalBalls % 6} ov)"
        }

        // Auto-switch strike on 1 or 3 runs, and on over completion
        val switchForRuns = (runs == 1 || runs == 3 || runs == 5)
        val switchForOverEnd = (isLegal && newLegalBalls > 0 && newLegalBalls % 6 == 0)
        val shouldSwitchStrike = (switchForRuns != switchForOverEnd)

        var finalStrikerName: String
        var finalStrikerRuns: Int
        var finalStrikerBalls: Int
        var finalStrikerFours: Int
        var finalStrikerSixes: Int

        var finalNonStrikerName: String
        var finalNonStrikerRuns: Int
        var finalNonStrikerBalls: Int
        var finalNonStrikerFours: Int
        var finalNonStrikerSixes: Int

        if (isWicket) {
            val squadList = (if (match.currentInnings == 1) match.teamAPlayers else match.teamBPlayers)
                .split(",")
                .map { it.trim() }
                .filter { it.isNotBlank() }

            val nextBatsman = squadList.firstOrNull { it != match.strikerName && it != match.nonStrikerName }
                ?: "Batsman ${newWickets + 2}"

            if (switchForOverEnd) {
                finalStrikerName = match.nonStrikerName
                finalStrikerRuns = match.nonStrikerRuns
                finalStrikerBalls = match.nonStrikerBalls
                finalStrikerFours = match.nonStrikerFours
                finalStrikerSixes = match.nonStrikerSixes

                finalNonStrikerName = nextBatsman
                finalNonStrikerRuns = 0
                finalNonStrikerBalls = 0
                finalNonStrikerFours = 0
                finalNonStrikerSixes = 0
            } else {
                finalStrikerName = nextBatsman
                finalStrikerRuns = 0
                finalStrikerBalls = 0
                finalStrikerFours = 0
                finalStrikerSixes = 0

                finalNonStrikerName = match.nonStrikerName
                finalNonStrikerRuns = match.nonStrikerRuns
                finalNonStrikerBalls = match.nonStrikerBalls
                finalNonStrikerFours = match.nonStrikerFours
                finalNonStrikerSixes = match.nonStrikerSixes
            }
        } else if (shouldSwitchStrike) {
            finalStrikerName = match.nonStrikerName
            finalStrikerRuns = match.nonStrikerRuns
            finalStrikerBalls = match.nonStrikerBalls
            finalStrikerFours = match.nonStrikerFours
            finalStrikerSixes = match.nonStrikerSixes

            finalNonStrikerName = match.strikerName
            finalNonStrikerRuns = strikerRuns
            finalNonStrikerBalls = strikerBalls
            finalNonStrikerFours = striker4s
            finalNonStrikerSixes = striker6s
        } else {
            finalStrikerName = match.strikerName
            finalStrikerRuns = strikerRuns
            finalStrikerBalls = strikerBalls
            finalStrikerFours = striker4s
            finalStrikerSixes = striker6s

            finalNonStrikerName = match.nonStrikerName
            finalNonStrikerRuns = match.nonStrikerRuns
            finalNonStrikerBalls = match.nonStrikerBalls
            finalNonStrikerFours = match.nonStrikerFours
            finalNonStrikerSixes = match.nonStrikerSixes
        }

        val updatedMatch = match.copy(
            score = newScore,
            wickets = newWickets,
            legalBalls = newLegalBalls,
            status = if (match.target > 0 && (newScore >= match.target || newWickets >= 10 || remainingBalls == 0)) "FINISHED" else "LIVE",
            statusDetail = detail,
            strikerName = finalStrikerName,
            strikerRuns = finalStrikerRuns,
            strikerBalls = finalStrikerBalls,
            strikerFours = finalStrikerFours,
            strikerSixes = finalStrikerSixes,
            nonStrikerName = finalNonStrikerName,
            nonStrikerRuns = finalNonStrikerRuns,
            nonStrikerBalls = finalNonStrikerBalls,
            nonStrikerFours = finalNonStrikerFours,
            nonStrikerSixes = finalNonStrikerSixes,
            bowlerBalls = bowlerBalls,
            bowlerRuns = bowlerRuns,
            bowlerWickets = bowlerWickets
        )

        dao.updateMatch(updatedMatch)

        val ballEvent = BallEventEntity(
            matchId = matchId,
            overNumber = if (isLegal && newBallInOver == 0 && newLegalBalls > 0) newOver - 1 else newOver,
            ballInOver = if (isLegal && newBallInOver == 0) 6 else newBallInOver,
            runs = totalRunToAdd,
            isWicket = isWicket,
            wicketType = wicketType,
            extraType = extraType,
            batsman = match.strikerName,
            bowler = match.bowlerName,
            commentary = commentary,
            shotAngle = shotAngle,
            pitchZone = pitchZone,
            isBoundary = runs == 4,
            isSix = runs == 6
        )
        dao.insertBallEvent(ballEvent)

        // Generate instant notification for big events
        if (isWicket) {
            dao.insertNotification(
                NotificationAlertEntity(
                    title = "⚡ WICKET! ${match.strikerName} $wicketType",
                    message = "${match.bowlerName} strikes! Score is now $newScore/$newWickets.",
                    type = "WICKET"
                )
            )
        } else if (runs == 6 && extraType == "NoBall") {
            dao.insertNotification(
                NotificationAlertEntity(
                    title = "🔥 7 RUNS! NO-BALL + SIX by ${match.strikerName}!",
                    message = "Monster hit on a No-Ball! Free Hit next ball!",
                    type = "SIX"
                )
            )
        } else if (runs == 6) {
            dao.insertNotification(
                NotificationAlertEntity(
                    title = "🚀 HUGE SIX by ${match.strikerName}!",
                    message = "Clean hit into the stands! Score $newScore/$newWickets.",
                    type = "SIX"
                )
            )
        }
    }

    suspend fun switchStrikers(matchId: String) {
        val match = dao.getMatchById(matchId).first() ?: return
        val updated = match.copy(
            strikerName = match.nonStrikerName,
            strikerRuns = match.nonStrikerRuns,
            strikerBalls = match.nonStrikerBalls,
            strikerFours = match.nonStrikerFours,
            strikerSixes = match.nonStrikerSixes,
            nonStrikerName = match.strikerName,
            nonStrikerRuns = match.strikerRuns,
            nonStrikerBalls = match.strikerBalls,
            nonStrikerFours = match.strikerFours,
            nonStrikerSixes = match.strikerSixes
        )
        dao.updateMatch(updated)
    }

    fun getHighlightClips(): List<HighlightClip> {
        return listOf(
            HighlightClip(
                id = "hl_1",
                matchId = "match_live_1",
                title = "Rohit's Towering 88m Six over Long-On",
                description = "Picked the slower ball early and smoked it effortlessly out of the park! High-voltage moment in the 17th over.",
                durationText = "0:38",
                category = "SIX",
                overText = "Over 16.6",
                bowlerVsBatter = "Mohit Chawla to Rohit Verma",
                animationType = "MONSTER_SIX"
            ),
            HighlightClip(
                id = "hl_2",
                matchId = "match_live_1",
                title = "Jasprit's Lethal Toe-Crushing Yorker",
                description = "Uproots middle stump at 138 km/h! Unplayable swinging delivery that broke the opening partnership.",
                durationText = "0:42",
                category = "WICKET",
                overText = "Over 8.3",
                bowlerVsBatter = "Jasprit Singh to Rahul Dev",
                animationType = "YORKER_WICKET"
            ),
            HighlightClip(
                id = "hl_3",
                matchId = "match_live_1",
                title = "Hawk-Eye DRS Drama: Huge Overturn Decision",
                description = "Umpire gave Not Out, but ultra-slow-mo and 3D Hawk-Eye confirmed ball was crashing into leg stump!",
                durationText = "1:05",
                category = "DRS",
                overText = "Over 12.1",
                bowlerVsBatter = "DRS Review: LBW Appeal",
                animationType = "DRS_OVERTURN"
            ),
            HighlightClip(
                id = "hl_4",
                matchId = "match_live_1",
                title = "Flying Boundary Catch on the Ropes!",
                description = "Spectacular acrobatic dive and relay catch just inches inside the boundary cushion.",
                durationText = "0:35",
                category = "CLUTCH",
                overText = "Over 14.5",
                bowlerVsBatter = "Deep Mid-Wicket Wonder Catch",
                animationType = "BOUNDARY_CATCH"
            ),
            HighlightClip(
                id = "hl_5",
                matchId = "match_live_1",
                title = "Direct Hit Bullseye Run Out",
                description = "Only one stump to aim at from 30 yards! Direct hit catches the batter centimeters short of safety crease.",
                durationText = "0:30",
                category = "WICKET",
                overText = "Over 4.2",
                bowlerVsBatter = "Direct Hit Run Out",
                animationType = "DIRECT_HIT_RUNOUT"
            )
        )
    }

    suspend fun createLocalMatch(
        id: String,
        tournamentName: String,
        teamA: String,
        teamB: String,
        totalOvers: Int,
        strikerName: String,
        nonStrikerName: String,
        bowlerName: String,
        venue: String
    ): MatchEntity {
        val shortA = teamA.take(3).uppercase()
        val shortB = teamB.take(3).uppercase()
        val newMatch = MatchEntity(
            id = id,
            tournamentName = tournamentName,
            teamA = teamA,
            teamB = teamB,
            teamAShort = shortA,
            teamBShort = shortB,
            teamAColorHex = 0xFF10B981,
            teamBColorHex = 0xFFF59E0B,
            currentInnings = 1,
            battingTeam = teamA,
            bowlingTeam = teamB,
            score = 0,
            wickets = 0,
            legalBalls = 0,
            totalOvers = totalOvers,
            target = 0,
            status = "LIVE",
            statusDetail = "Match in progress: 1st Innings ($totalOvers overs)",
            strikerName = strikerName,
            strikerRuns = 0,
            strikerBalls = 0,
            strikerFours = 0,
            strikerSixes = 0,
            nonStrikerName = nonStrikerName,
            nonStrikerRuns = 0,
            nonStrikerBalls = 0,
            nonStrikerFours = 0,
            nonStrikerSixes = 0,
            bowlerName = bowlerName,
            bowlerBalls = 0,
            bowlerMaidens = 0,
            bowlerRuns = 0,
            bowlerWickets = 0,
            venue = venue,
            teamAFirstInningsScore = "Yet to bat"
        )
        dao.insertMatch(newMatch)
        return newMatch
    }

    suspend fun undoLastDelivery(matchId: String) {
        val currentEvents = dao.getBallEventsForMatch(matchId).first()
        val lastEvent = currentEvents.firstOrNull() ?: return
        val match = dao.getMatchById(matchId).first() ?: return

        dao.deleteLastBallEvent(matchId)

        val isLegal = lastEvent.extraType != "Wide" && lastEvent.extraType != "NoBall"
        val restoredBalls = if (isLegal && match.legalBalls > 0) match.legalBalls - 1 else match.legalBalls
        val restoredScore = (match.score - lastEvent.runs).coerceAtLeast(0)
        val restoredWickets = if (lastEvent.isWicket && match.wickets > 0) match.wickets - 1 else match.wickets

        dao.updateMatch(
            match.copy(
                score = restoredScore,
                wickets = restoredWickets,
                legalBalls = restoredBalls,
                status = "LIVE",
                statusDetail = "Delivery undone by Official Scorer"
            )
        )
    }

    suspend fun updateNewBatsman(matchId: String, newBatsmanName: String) {
        val match = dao.getMatchById(matchId).first() ?: return
        dao.updateMatch(
            match.copy(
                strikerName = newBatsmanName,
                strikerRuns = 0,
                strikerBalls = 0,
                strikerFours = 0,
                strikerSixes = 0
            )
        )
    }

    suspend fun updateNewBowler(matchId: String, newBowlerName: String) {
        val match = dao.getMatchById(matchId).first() ?: return
        dao.updateMatch(
            match.copy(
                bowlerName = newBowlerName,
                bowlerBalls = 0,
                bowlerRuns = 0,
                bowlerMaidens = 0,
                bowlerWickets = 0
            )
        )
    }

    suspend fun updateMatchSquad(
        matchId: String,
        striker: String,
        nonStriker: String,
        bowler: String,
        teamAPlayers: String,
        teamBPlayers: String
    ) {
        val match = dao.getMatchById(matchId).first() ?: return
        dao.updateMatch(
            match.copy(
                strikerName = striker,
                nonStrikerName = nonStriker,
                bowlerName = bowler,
                teamAPlayers = teamAPlayers,
                teamBPlayers = teamBPlayers
            )
        )
    }

    suspend fun sendCustomNotification(title: String, message: String, type: String) {
        dao.insertNotification(
            NotificationAlertEntity(
                title = title,
                message = message,
                type = type
            )
        )
    }

    suspend fun resetAllToZeroAndStartFresh(
        matchName: String = "Local Cricket Match",
        teamA: String = "Team A",
        teamB: String = "Team B",
        overs: Int = 10,
        striker: String = "Striker",
        nonStriker: String = "Non-Striker",
        bowler: String = "Bowler",
        venue: String = "Local Ground"
    ): MatchEntity {
        dao.deleteAllMatches()
        dao.deleteAllBallEvents()
        val shortA = teamA.take(3).uppercase().ifBlank { "TMA" }
        val shortB = teamB.take(3).uppercase().ifBlank { "TMB" }
        val freshMatch = MatchEntity(
            id = "match_live_1",
            tournamentName = matchName.ifBlank { "Local Cricket Match" },
            teamA = teamA.ifBlank { "Team A" },
            teamB = teamB.ifBlank { "Team B" },
            teamAShort = shortA,
            teamBShort = shortB,
            teamAColorHex = 0xFF2563EB,
            teamBColorHex = 0xFFDC2626,
            currentInnings = 1,
            battingTeam = teamA.ifBlank { "Team A" },
            bowlingTeam = teamB.ifBlank { "Team B" },
            score = 0,
            wickets = 0,
            legalBalls = 0,
            totalOvers = if (overs > 0) overs else 10,
            target = 0,
            status = "LIVE",
            statusDetail = "1st Innings • 0/0 (0.0 ov) • Ready for 1st Ball",
            strikerName = striker.ifBlank { "Striker" },
            strikerRuns = 0,
            strikerBalls = 0,
            strikerFours = 0,
            strikerSixes = 0,
            nonStrikerName = nonStriker.ifBlank { "Non-Striker" },
            nonStrikerRuns = 0,
            nonStrikerBalls = 0,
            nonStrikerFours = 0,
            nonStrikerSixes = 0,
            bowlerName = bowler.ifBlank { "Opening Bowler" },
            bowlerBalls = 0,
            bowlerMaidens = 0,
            bowlerRuns = 0,
            bowlerWickets = 0,
            venue = venue.ifBlank { "Local Ground" },
            teamAFirstInningsScore = "Yet to bat"
        )
        dao.insertMatch(freshMatch)
        return freshMatch
    }

    suspend fun resetCurrentMatchToZero(matchId: String) {
        dao.deleteBallEventsForMatch(matchId)
        val match = dao.getMatchById(matchId).first() ?: return
        dao.updateMatch(
            match.copy(
                score = 0,
                wickets = 0,
                legalBalls = 0,
                target = 0,
                status = "LIVE",
                statusDetail = "1st Innings • 0/0 (0.0 ov) • Ready for 1st Ball",
                strikerRuns = 0,
                strikerBalls = 0,
                strikerFours = 0,
                strikerSixes = 0,
                nonStrikerRuns = 0,
                nonStrikerBalls = 0,
                nonStrikerFours = 0,
                nonStrikerSixes = 0,
                bowlerBalls = 0,
                bowlerMaidens = 0,
                bowlerRuns = 0,
                bowlerWickets = 0,
                currentInnings = 1
            )
        )
    }

    companion object {
        val sampleMatch = MatchEntity(
            id = "match_live_1",
            tournamentName = "Gully Premier League 2026",
            teamA = "Team A",
            teamB = "Team B",
            teamAShort = "TMA",
            teamBShort = "TMB",
            teamAColorHex = 0xFF2563EB,
            teamBColorHex = 0xFFDC2626,
            currentInnings = 1,
            battingTeam = "Team A",
            bowlingTeam = "Team B",
            score = 0,
            wickets = 0,
            legalBalls = 0,
            totalOvers = 10,
            target = 0,
            status = "LIVE",
            statusDetail = "1st Innings • 0/0 (0.0 ov) • Ready for 1st Ball",
            strikerName = "Striker",
            strikerRuns = 0,
            strikerBalls = 0,
            strikerFours = 0,
            strikerSixes = 0,
            nonStrikerName = "Non-Striker",
            nonStrikerRuns = 0,
            nonStrikerBalls = 0,
            nonStrikerFours = 0,
            nonStrikerSixes = 0,
            bowlerName = "Opening Bowler",
            bowlerBalls = 0,
            bowlerMaidens = 0,
            bowlerRuns = 0,
            bowlerWickets = 0,
            venue = "Local Turf Ground",
            teamAFirstInningsScore = "Yet to bat"
        )

        val sampleBallEvents = emptyList<BallEventEntity>()
    }
}
