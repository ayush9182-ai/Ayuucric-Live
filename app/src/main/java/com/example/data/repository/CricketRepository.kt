package com.example.data.repository

import com.example.data.local.CricketDao
import com.example.data.model.BallEventEntity
import com.example.data.model.DismissedBatsman
import com.example.data.model.HighlightClip
import com.example.data.model.MatchEntity
import com.example.data.model.NotificationAlertEntity
import com.example.data.model.PlayerStatEntity
import com.example.data.model.TeamStandingEntity
import com.example.data.model.formatDismissedBatsmenJson
import com.example.data.model.parseDismissedBatsmen
import com.example.domain.engine.DeliveryInput
import com.example.domain.engine.MatchScoringEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class CricketRepository(private val dao: CricketDao) {

    val allMatches: Flow<List<MatchEntity>> = dao.getAllMatches()
    val teamStandings: Flow<List<TeamStandingEntity>> = dao.getTeamStandings()
    val playerStats: Flow<List<PlayerStatEntity>> = dao.getPlayerStats()
    val notifications: Flow<List<NotificationAlertEntity>> = dao.getNotifications()

    fun getMatch(matchId: String): Flow<MatchEntity?> = dao.getMatchById(matchId)
    fun getBallEvents(matchId: String): Flow<List<BallEventEntity>> = dao.getBallEventsForMatch(matchId)

    fun getFallbackMatches(): List<MatchEntity> = emptyList()
    fun getFallbackMatch(id: String = ""): MatchEntity? = null
    fun getFallbackBallEvents(id: String = ""): List<BallEventEntity> = emptyList()

    suspend fun initializeDefaultDataIfEmpty() {
        val existingMatches = dao.getAllMatches().first()
        // Clean out any pre-existing template dummy match so scorecard and live feed start clean
        val dummyMatches = existingMatches.filter { it.id == "match_live_1" && it.teamA == "Team A" && it.legalBalls == 0 }
        dummyMatches.forEach {
            dao.deleteMatch(it)
            dao.deleteBallEventsForMatch(it.id)
        }

        // Clean out any pre-existing dummy records so table and stats start completely clean
        dao.deleteAllStandings()
        dao.deleteAllPlayerStats()
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
        pitchZone: String,
        newBatsmanName: String? = null,
        dismissedBatsman: String? = null,
        newBatsmanOnStrike: Boolean = true
    ): MatchEntity? {
        val match = dao.getMatchById(matchId).first() ?: return null

        val input = DeliveryInput(
            matchId = matchId,
            runs = runs,
            isWicket = isWicket,
            wicketType = wicketType,
            extraType = extraType,
            commentary = commentary,
            shotAngle = shotAngle,
            pitchZone = pitchZone,
            newBatsmanName = newBatsmanName,
            dismissedBatsman = dismissedBatsman,
            newBatsmanOnStrike = newBatsmanOnStrike
        )

        val result = MatchScoringEngine.processDelivery(match, input)

        dao.updateMatch(result.updatedMatch)
        dao.insertBallEvent(result.ballEvent)
        result.eventNotification?.let { dao.insertNotification(it) }
        result.milestoneNotification?.let { dao.insertNotification(it) }

        return result.updatedMatch
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
                title = "Towering 88m Six over Long-On",
                description = "Picked the slower ball early and smoked it effortlessly out of the park! High-voltage moment in the 17th over.",
                durationText = "0:38",
                category = "SIX",
                overText = "Over 16.6",
                bowlerVsBatter = "Monster Six over Long-On",
                animationType = "MONSTER_SIX"
            ),
            HighlightClip(
                id = "hl_2",
                matchId = "match_live_1",
                title = "Lethal Toe-Crushing Yorker",
                description = "Uproots middle stump at 138 km/h! Unplayable swinging delivery that broke the opening partnership.",
                durationText = "0:42",
                category = "WICKET",
                overText = "Over 8.3",
                bowlerVsBatter = "Fast In-Swinging Yorker",
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

    suspend fun updateNewBatsman(matchId: String, newBatsmanName: String, isStriker: Boolean = true) {
        val match = dao.getMatchById(matchId).first() ?: return
        val cleanName = newBatsmanName.trim()
        var teamAPlayers = match.teamAPlayers
        var teamBPlayers = match.teamBPlayers

        if (!cleanName.startsWith("Batsman")) {
            if (match.currentInnings == 1) {
                val list = teamAPlayers.split(",").map { it.trim() }.filter { it.isNotBlank() }
                if (!list.any { it.equals(cleanName, ignoreCase = true) }) {
                    teamAPlayers = if (teamAPlayers.isBlank()) cleanName else "$teamAPlayers, $cleanName"
                }
            } else {
                val list = teamBPlayers.split(",").map { it.trim() }.filter { it.isNotBlank() }
                if (!list.any { it.equals(cleanName, ignoreCase = true) }) {
                    teamBPlayers = if (teamBPlayers.isBlank()) cleanName else "$teamBPlayers, $cleanName"
                }
            }
        }

        val updated = if (isStriker) {
            match.copy(
                strikerName = cleanName,
                strikerRuns = 0,
                strikerBalls = 0,
                strikerFours = 0,
                strikerSixes = 0,
                teamAPlayers = teamAPlayers,
                teamBPlayers = teamBPlayers
            )
        } else {
            match.copy(
                nonStrikerName = cleanName,
                nonStrikerRuns = 0,
                nonStrikerBalls = 0,
                nonStrikerFours = 0,
                nonStrikerSixes = 0,
                teamAPlayers = teamAPlayers,
                teamBPlayers = teamBPlayers
            )
        }
        dao.updateMatch(updated)
    }

    suspend fun updateNewBowler(matchId: String, newBowlerName: String) {
        val match = dao.getMatchById(matchId).first() ?: return
        val cleanName = newBowlerName.trim()
        val allEvents = dao.getBallEventsForMatch(matchId).first()
        val bowlerEvents = allEvents.filter { it.bowler.equals(cleanName, ignoreCase = true) }

        val prevBalls = bowlerEvents.count { it.extraType != "Wide" && it.extraType != "NoBall" }
        val prevRuns = bowlerEvents.sumOf { it.runs + if (it.extraType == "Wide" || it.extraType == "NoBall") 1 else 0 }
        val prevWickets = bowlerEvents.count { it.isWicket && it.wicketType != "Run Out" }

        val isTeamABowling = match.currentInnings == 2
        var teamAPlayers = match.teamAPlayers
        var teamBPlayers = match.teamBPlayers
        if (!cleanName.startsWith("Bowler")) {
            if (isTeamABowling) {
                val list = teamAPlayers.split(",").map { it.trim() }.filter { it.isNotBlank() }
                if (!list.any { it.equals(cleanName, ignoreCase = true) }) {
                    teamAPlayers = if (teamAPlayers.isBlank()) cleanName else "$teamAPlayers, $cleanName"
                }
            } else {
                val list = teamBPlayers.split(",").map { it.trim() }.filter { it.isNotBlank() }
                if (!list.any { it.equals(cleanName, ignoreCase = true) }) {
                    teamBPlayers = if (teamBPlayers.isBlank()) cleanName else "$teamBPlayers, $cleanName"
                }
            }
        }

        dao.updateMatch(
            match.copy(
                bowlerName = cleanName,
                bowlerBalls = prevBalls,
                bowlerRuns = prevRuns,
                bowlerMaidens = 0,
                bowlerWickets = prevWickets,
                teamAPlayers = teamAPlayers,
                teamBPlayers = teamBPlayers
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
                currentInnings = 1,
                dismissedBatsmenJson = ""
            )
        )
    }

    suspend fun upsertMatchesFromFirestore(matches: List<MatchEntity>) {
        dao.insertMatches(matches)
    }

    suspend fun upsertMatchFromFirestore(match: MatchEntity) {
        dao.insertMatch(match)
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
