package com.example.domain.engine

import com.example.data.model.BallEventEntity
import com.example.data.model.DismissedBatsman
import com.example.data.model.MatchEntity
import com.example.data.model.NotificationAlertEntity
import com.example.data.model.formatDismissedBatsmenJson
import com.example.data.model.parseDismissedBatsmen

/**
 * Pure domain input representing an incoming ball/delivery to be processed.
 */
data class DeliveryInput(
    val matchId: String,
    val runs: Int,
    val isWicket: Boolean = false,
    val wicketType: String = "",
    val extraType: String = "None",
    val commentary: String = "",
    val shotAngle: Float = 0f,
    val pitchZone: String = "Good Length",
    val newBatsmanName: String? = null,
    val dismissedBatsman: String? = null,
    val newBatsmanOnStrike: Boolean = true
)

/**
 * Result of a delivery calculation, containing the new match state,
 * the recorded ball event, and any triggered alerts.
 */
data class DeliveryResult(
    val updatedMatch: MatchEntity,
    val ballEvent: BallEventEntity,
    val eventNotification: NotificationAlertEntity? = null,
    val milestoneNotification: NotificationAlertEntity? = null
)

/**
 * MatchScoringEngine:
 * Pure, isolated domain engine for cricket rules, scoring calculations,
 * strike rotation, dismissals, extras, and bowler figures.
 *
 * Fully decoupled from Android frameworks and Room database for unit testing.
 */
object MatchScoringEngine {

    fun processDelivery(currentMatch: MatchEntity, input: DeliveryInput): DeliveryResult {
        // Strict Guard 1: If match is already FINISHED, absolutely reject any new deliveries
        if (currentMatch.status == "FINISHED") {
            return DeliveryResult(
                updatedMatch = currentMatch,
                ballEvent = BallEventEntity(
                    matchId = input.matchId,
                    overNumber = currentMatch.legalBalls / 6,
                    ballInOver = currentMatch.legalBalls % 6,
                    runs = 0,
                    batsman = currentMatch.strikerName,
                    bowler = currentMatch.bowlerName,
                    commentary = "Match finished (${currentMatch.statusDetail})"
                ),
                eventNotification = null,
                milestoneNotification = null
            )
        }

        // Strict Guard 2: If 2nd innings and target was already reached or all overs bowled
        val maxTotalBalls = (currentMatch.totalOvers * 6).coerceAtLeast(6)
        if (currentMatch.currentInnings == 2 && currentMatch.target > 0 && currentMatch.score >= currentMatch.target) {
            val finished = currentMatch.copy(status = "FINISHED")
            return DeliveryResult(
                updatedMatch = finished,
                ballEvent = BallEventEntity(
                    matchId = input.matchId,
                    overNumber = currentMatch.legalBalls / 6,
                    ballInOver = currentMatch.legalBalls % 6,
                    runs = 0,
                    batsman = currentMatch.strikerName,
                    bowler = currentMatch.bowlerName,
                    commentary = "Target already achieved (${currentMatch.statusDetail})"
                ),
                eventNotification = null,
                milestoneNotification = null
            )
        }
        if (currentMatch.currentInnings == 2 && currentMatch.legalBalls >= maxTotalBalls) {
            val finished = currentMatch.copy(status = "FINISHED")
            return DeliveryResult(
                updatedMatch = finished,
                ballEvent = BallEventEntity(
                    matchId = input.matchId,
                    overNumber = currentMatch.legalBalls / 6,
                    ballInOver = currentMatch.legalBalls % 6,
                    runs = 0,
                    batsman = currentMatch.strikerName,
                    bowler = currentMatch.bowlerName,
                    commentary = "All overs bowled (${currentMatch.statusDetail})"
                ),
                eventNotification = null,
                milestoneNotification = null
            )
        }

        // Strict Guard 3: If 1st innings completed (innings break or overs finished), reject new deliveries until 2nd innings starts
        if (currentMatch.status == "INNINGS_BREAK" || (currentMatch.currentInnings == 1 && currentMatch.legalBalls >= maxTotalBalls)) {
            val breakMatch = if (currentMatch.status != "INNINGS_BREAK") currentMatch.copy(status = "INNINGS_BREAK") else currentMatch
            return DeliveryResult(
                updatedMatch = breakMatch,
                ballEvent = BallEventEntity(
                    matchId = input.matchId,
                    overNumber = currentMatch.legalBalls / 6,
                    ballInOver = currentMatch.legalBalls % 6,
                    runs = 0,
                    batsman = currentMatch.strikerName,
                    bowler = currentMatch.bowlerName,
                    commentary = "1st Innings completed. Please start 2nd Innings."
                ),
                eventNotification = null,
                milestoneNotification = null
            )
        }

        val runs = input.runs
        val isWicket = input.isWicket
        val wicketType = input.wicketType
        val extraType = input.extraType

        val isWide = extraType.equals("Wide", ignoreCase = true) || extraType.equals("WD", ignoreCase = true)
        val isNoBall = extraType.equals("NoBall", ignoreCase = true) || extraType.equals("NB", ignoreCase = true)
        val isBye = extraType.equals("Bye", ignoreCase = true) || extraType.equals("B", ignoreCase = true)
        val isLegBye = extraType.equals("LegBye", ignoreCase = true) || extraType.equals("LB", ignoreCase = true)
        val isLegal = !isWide && !isNoBall

        val penalty = if (isWide || isNoBall) 1 else 0
        val totalRunToAdd = runs + penalty
        val newScore = currentMatch.score + totalRunToAdd
        val newWickets = if (isWicket) (currentMatch.wickets + 1).coerceAtMost(10) else currentMatch.wickets
        // Never allow legalBalls to exceed maxTotalBalls
        val newLegalBalls = if (isLegal) (currentMatch.legalBalls + 1).coerceAtMost(maxTotalBalls) else currentMatch.legalBalls
        val newOver = newLegalBalls / 6
        val newBallInOver = newLegalBalls % 6

        // Striker updates (Wides, Byes, and Leg Byes are extras and do not go to batsman's personal score)
        val strikerRuns = if (isWide || isBye || isLegBye) currentMatch.strikerRuns else currentMatch.strikerRuns + runs
        val strikerBalls = if (isWide) currentMatch.strikerBalls else currentMatch.strikerBalls + 1
        val striker4s = currentMatch.strikerFours + if (runs == 4 && !isBye && !isLegBye) 1 else 0
        val striker6s = currentMatch.strikerSixes + if (runs == 6 && !isBye && !isLegBye) 1 else 0

        // Bowler updates (Byes and Leg Byes are not charged to bowler's figures)
        val bowlerBalls = if (isLegal) currentMatch.bowlerBalls + 1 else currentMatch.bowlerBalls
        val bowlerRuns = currentMatch.bowlerRuns + if (isBye || isLegBye) 0 else totalRunToAdd
        val bowlerWickets = if (isWicket && !wicketType.equals("Run Out", ignoreCase = true)) {
            currentMatch.bowlerWickets + 1
        } else {
            currentMatch.bowlerWickets
        }

        // Match status & target calculation
        val remainingRuns = (currentMatch.target - newScore).coerceAtLeast(0)
        val remainingBalls = ((currentMatch.totalOvers * 6) - newLegalBalls).coerceAtLeast(0)
        val statusDetail = calculateStatusDetail(
            currentMatch = currentMatch,
            newScore = newScore,
            newWickets = newWickets,
            newLegalBalls = newLegalBalls,
            remainingRuns = remainingRuns,
            remainingBalls = remainingBalls
        )

        // Strike rotation logic:
        // Runs of 1, 3, or 5 rotate strike.
        // Over completion (6th legal ball) rotates strike.
        // If both happen, they cancel out (XOR logic).
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

        var teamAPlayers = currentMatch.teamAPlayers
        var teamBPlayers = currentMatch.teamBPlayers

        val outBatterName = if (!input.dismissedBatsman.isNullOrBlank()) {
            input.dismissedBatsman
        } else {
            currentMatch.strikerName
        }
        var newDismissedBatsmenJson = currentMatch.dismissedBatsmenJson

        if (isWicket) {
            val isStrikerOut = outBatterName.equals(currentMatch.strikerName, ignoreCase = true)
            val remainingBatterName = if (isStrikerOut) currentMatch.nonStrikerName else currentMatch.strikerName
            val remainingRunsVal = if (isStrikerOut) currentMatch.nonStrikerRuns else strikerRuns
            val remainingBallsVal = if (isStrikerOut) currentMatch.nonStrikerBalls else strikerBalls
            val remainingFours = if (isStrikerOut) currentMatch.nonStrikerFours else striker4s
            val remainingSixes = if (isStrikerOut) currentMatch.nonStrikerSixes else striker6s

            val outRuns = if (isStrikerOut) strikerRuns else currentMatch.nonStrikerRuns
            val outBalls = if (isStrikerOut) strikerBalls else currentMatch.nonStrikerBalls
            val outFours = if (isStrikerOut) striker4s else currentMatch.nonStrikerFours
            val outSixes = if (isStrikerOut) striker6s else currentMatch.nonStrikerSixes

            val dismissalDesc = when {
                wicketType.contains("Run Out", ignoreCase = true) -> "run out"
                wicketType.contains("Caught", ignoreCase = true) -> "c Fielder b ${currentMatch.bowlerName}"
                wicketType.contains("Bowled", ignoreCase = true) -> "b ${currentMatch.bowlerName}"
                wicketType.contains("LBW", ignoreCase = true) -> "lbw b ${currentMatch.bowlerName}"
                wicketType.contains("Stumped", ignoreCase = true) -> "st Keeper b ${currentMatch.bowlerName}"
                wicketType.isNotBlank() -> "$wicketType b ${currentMatch.bowlerName}"
                else -> "b ${currentMatch.bowlerName}"
            }

            val currentDismissedList = parseDismissedBatsmen(currentMatch.dismissedBatsmenJson).toMutableList()
            currentDismissedList.add(
                DismissedBatsman(
                    name = outBatterName,
                    runs = outRuns,
                    balls = outBalls,
                    fours = outFours,
                    sixes = outSixes,
                    dismissal = dismissalDesc,
                    strikeRate = if (outBalls > 0) String.format("%.1f", (outRuns.toFloat() / outBalls) * 100) else "0.0"
                )
            )
            newDismissedBatsmenJson = formatDismissedBatsmenJson(currentDismissedList)

            val squadList = (if (currentMatch.currentInnings == 1) currentMatch.teamAPlayers else currentMatch.teamBPlayers)
                .split(",")
                .map { it.trim() }
                .filter { it.isNotBlank() }

            val fallbackNext = squadList.firstOrNull {
                !it.equals(currentMatch.strikerName, ignoreCase = true) && !it.equals(currentMatch.nonStrikerName, ignoreCase = true)
            } ?: "Batsman ${newWickets + 2}"

            val incomingBatsman = if (!input.newBatsmanName.isNullOrBlank()) input.newBatsmanName.trim() else fallbackNext

            // Automatically add new incoming batsman to team squad list
            if (!incomingBatsman.startsWith("Batsman ")) {
                if (currentMatch.currentInnings == 1) {
                    val currentList = teamAPlayers.split(",").map { it.trim() }.filter { it.isNotBlank() }
                    if (!currentList.any { it.equals(incomingBatsman, ignoreCase = true) }) {
                        teamAPlayers = if (teamAPlayers.isBlank()) incomingBatsman else "$teamAPlayers, $incomingBatsman"
                    }
                } else {
                    val currentList = teamBPlayers.split(",").map { it.trim() }.filter { it.isNotBlank() }
                    if (!currentList.any { it.equals(incomingBatsman, ignoreCase = true) }) {
                        teamBPlayers = if (teamBPlayers.isBlank()) incomingBatsman else "$teamBPlayers, $incomingBatsman"
                    }
                }
            }

            val effectiveStrikeIsNew = if (switchForOverEnd) !input.newBatsmanOnStrike else input.newBatsmanOnStrike

            if (effectiveStrikeIsNew) {
                finalStrikerName = incomingBatsman
                finalStrikerRuns = 0
                finalStrikerBalls = 0
                finalStrikerFours = 0
                finalStrikerSixes = 0

                finalNonStrikerName = remainingBatterName
                finalNonStrikerRuns = remainingRunsVal
                finalNonStrikerBalls = remainingBallsVal
                finalNonStrikerFours = remainingFours
                finalNonStrikerSixes = remainingSixes
            } else {
                finalStrikerName = remainingBatterName
                finalStrikerRuns = remainingRunsVal
                finalStrikerBalls = remainingBallsVal
                finalStrikerFours = remainingFours
                finalStrikerSixes = remainingSixes

                finalNonStrikerName = incomingBatsman
                finalNonStrikerRuns = 0
                finalNonStrikerBalls = 0
                finalNonStrikerFours = 0
                finalNonStrikerSixes = 0
            }
        } else if (shouldSwitchStrike) {
            finalStrikerName = currentMatch.nonStrikerName
            finalStrikerRuns = currentMatch.nonStrikerRuns
            finalStrikerBalls = currentMatch.nonStrikerBalls
            finalStrikerFours = currentMatch.nonStrikerFours
            finalStrikerSixes = currentMatch.nonStrikerSixes

            finalNonStrikerName = currentMatch.strikerName
            finalNonStrikerRuns = strikerRuns
            finalNonStrikerBalls = strikerBalls
            finalNonStrikerFours = striker4s
            finalNonStrikerSixes = striker6s
        } else {
            finalStrikerName = currentMatch.strikerName
            finalStrikerRuns = strikerRuns
            finalStrikerBalls = strikerBalls
            finalStrikerFours = striker4s
            finalStrikerSixes = striker6s

            finalNonStrikerName = currentMatch.nonStrikerName
            finalNonStrikerRuns = currentMatch.nonStrikerRuns
            finalNonStrikerBalls = currentMatch.nonStrikerBalls
            finalNonStrikerFours = currentMatch.nonStrikerFours
            finalNonStrikerSixes = currentMatch.nonStrikerSixes
        }

        val isFirstInnings = currentMatch.currentInnings == 1
        val currentBatSquad = (if (currentMatch.battingTeam.equals(currentMatch.teamA, ignoreCase = true)) teamAPlayers else teamBPlayers)
            .split(",").map { it.trim() }.filter { it.isNotBlank() }.distinct()
        val maxAllOutWickets = if (currentBatSquad.size in 2..10) currentBatSquad.size - 1 else 10

        val isFirstInningsAllOut = isFirstInnings && newWickets >= maxAllOutWickets
        val isFirstInningsOversComplete = isFirstInnings && isLegal && newLegalBalls >= (currentMatch.totalOvers * 6)
        val isFirstInningsComplete = isFirstInningsAllOut || isFirstInningsOversComplete

        val updatedMatch = if (isFirstInningsComplete) {
            val firstInningsScoreStr = "$newScore/$newWickets (${newLegalBalls / 6}.${newLegalBalls % 6} ov)"
            val targetRuns = newScore + 1
            currentMatch.copy(
                score = newScore,
                wickets = newWickets,
                legalBalls = newLegalBalls,
                target = targetRuns,
                teamAFirstInningsScore = firstInningsScoreStr,
                status = "INNINGS_BREAK",
                statusDetail = "1st Innings Complete: ${currentMatch.battingTeam} scored $newScore/$newWickets (${newLegalBalls / 6}.${newLegalBalls % 6} ov). Target: $targetRuns runs.",
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
                bowlerWickets = bowlerWickets,
                teamAPlayers = teamAPlayers,
                teamBPlayers = teamBPlayers,
                dismissedBatsmenJson = newDismissedBatsmenJson
            )
        } else {
            val isFinished = currentMatch.currentInnings == 2 && (
                (currentMatch.target > 0 && newScore >= currentMatch.target) ||
                newWickets >= maxAllOutWickets ||
                (isLegal && newLegalBalls >= maxTotalBalls)
            )
            currentMatch.copy(
                score = newScore,
                wickets = newWickets,
                legalBalls = newLegalBalls,
                status = if (isFinished) "FINISHED" else "LIVE",
                statusDetail = statusDetail,
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
                bowlerWickets = bowlerWickets,
                teamAPlayers = teamAPlayers,
                teamBPlayers = teamBPlayers,
                dismissedBatsmenJson = newDismissedBatsmenJson
            )
        }

        val ballEvent = BallEventEntity(
            matchId = input.matchId,
            overNumber = if (isLegal && newBallInOver == 0 && newLegalBalls > 0) newOver - 1 else newOver,
            ballInOver = if (isLegal && newBallInOver == 0) 6 else newBallInOver,
            runs = totalRunToAdd,
            isWicket = isWicket,
            wicketType = wicketType,
            extraType = extraType,
            batsman = outBatterName,
            bowler = currentMatch.bowlerName,
            commentary = input.commentary,
            shotAngle = input.shotAngle,
            pitchZone = input.pitchZone,
            isBoundary = runs == 4,
            isSix = runs == 6
        )

        // Generate instant notifications
        var eventAlert: NotificationAlertEntity? = null
        if (isFirstInningsComplete) {
            val targetRuns = newScore + 1
            eventAlert = NotificationAlertEntity(
                title = "🏏 1ST INNINGS COMPLETED! Target: $targetRuns",
                message = "${currentMatch.battingTeam} scored $newScore/$newWickets. ${currentMatch.bowlingTeam} need $targetRuns runs to win. 2nd Innings has started!",
                type = "INNINGS_BREAK"
            )
        } else if (updatedMatch.status == "FINISHED") {
            eventAlert = NotificationAlertEntity(
                title = "🏆 MATCH FINISHED!",
                message = updatedMatch.statusDetail,
                type = "MATCH_FINISHED"
            )
        } else if (isWicket) {
            eventAlert = NotificationAlertEntity(
                title = "⚡ WICKET! $outBatterName $wicketType",
                message = "${currentMatch.bowlerName} strikes! Score is now $newScore/$newWickets.",
                type = "WICKET"
            )
        } else if (runs == 6 && extraType == "NoBall") {
            eventAlert = NotificationAlertEntity(
                title = "🔥 7 RUNS! NO-BALL + SIX by ${currentMatch.strikerName}!",
                message = "Monster hit on a No-Ball! Free Hit next ball!",
                type = "SIX"
            )
        } else if (runs == 6) {
            eventAlert = NotificationAlertEntity(
                title = "🚀 HUGE SIX by ${currentMatch.strikerName}!",
                message = "Clean hit into the stands! Score $newScore/$newWickets.",
                type = "SIX"
            )
        } else if (runs == 4) {
            eventAlert = NotificationAlertEntity(
                title = "🏏 FOUR! Boundary by ${currentMatch.strikerName}!",
                message = "Races away across the turf! Score $newScore/$newWickets.",
                type = "FOUR"
            )
        }

        // Milestone alerts
        var milestoneAlert: NotificationAlertEntity? = null
        if (!isWicket && currentMatch.strikerRuns < 50 && strikerRuns >= 50) {
            milestoneAlert = NotificationAlertEntity(
                title = "🎖️ FIFTY! Half Century for ${currentMatch.strikerName}!",
                message = "Magnificent 50 off $strikerBalls balls!",
                type = "MILESTONE"
            )
        } else if (!isWicket && currentMatch.strikerRuns < 100 && strikerRuns >= 100) {
            milestoneAlert = NotificationAlertEntity(
                title = "👑 CENTURY! 100 for ${currentMatch.strikerName}!",
                message = "Sensational hundred off $strikerBalls balls!",
                type = "MILESTONE"
            )
        }

        return DeliveryResult(
            updatedMatch = updatedMatch,
            ballEvent = ballEvent,
            eventNotification = eventAlert,
            milestoneNotification = milestoneAlert
        )
    }

    private fun calculateStatusDetail(
        currentMatch: MatchEntity,
        newScore: Int,
        newWickets: Int,
        newLegalBalls: Int,
        remainingRuns: Int,
        remainingBalls: Int
    ): String {
        return if (currentMatch.target > 0 && newScore >= currentMatch.target) {
            "${currentMatch.battingTeam} won by ${10 - newWickets} wickets! 🏆"
        } else if (currentMatch.target > 0 && (newWickets >= 10 || remainingBalls == 0)) {
            if (newScore == currentMatch.target - 1) {
                "Match Tied! 🤝 Super Over!"
            } else {
                "${currentMatch.bowlingTeam} won by ${remainingRuns} runs! 🏆"
            }
        } else if (currentMatch.target > 0) {
            val rrr = if (remainingBalls > 0) remainingRuns * 6f / remainingBalls else 0f
            "Need $remainingRuns runs in $remainingBalls balls (RRR: ${"%.2f".format(rrr)})"
        } else if (newWickets >= 10) {
            "1st Innings Complete (${currentMatch.battingTeam} All Out $newScore/$newWickets) • Target: ${newScore + 1}"
        } else if (remainingBalls == 0) {
            "1st Innings Complete (${currentMatch.totalOvers} ov) • $newScore/$newWickets • Target: ${newScore + 1}"
        } else {
            "${currentMatch.battingTeam} batting • $newScore/$newWickets (${newLegalBalls / 6}.${newLegalBalls % 6} ov)"
        }
    }
}
