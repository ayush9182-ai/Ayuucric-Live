package com.example.util

import com.example.data.model.MatchEntity
import com.example.data.model.PlayerStatEntity
import com.example.data.model.TeamStandingEntity
import com.example.data.model.parseDismissedBatsmen

object TournamentStatsCalculator {

    fun computeStandingsAndStats(matches: List<MatchEntity>): Pair<List<TeamStandingEntity>, List<PlayerStatEntity>> {
        if (matches.isEmpty()) {
            return Pair(emptyList(), emptyList())
        }

        // --- 1. Compute Team Standings ---
        val teamMap = mutableMapOf<String, MutableTeamData>()

        for (match in matches) {
            val teamA = match.teamA.trim().ifBlank { "Team A" }
            val teamB = match.teamB.trim().ifBlank { "Team B" }

            val dataA = teamMap.getOrPut(teamA) {
                MutableTeamData(
                    teamId = "team_${teamA.lowercase().replace(" ", "_")}",
                    name = teamA,
                    shortName = match.teamAShort.ifBlank { teamA.take(3).uppercase() }
                )
            }
            val dataB = teamMap.getOrPut(teamB) {
                MutableTeamData(
                    teamId = "team_${teamB.lowercase().replace(" ", "_")}",
                    name = teamB,
                    shortName = match.teamBShort.ifBlank { teamB.take(3).uppercase() }
                )
            }

            val hasActivity = match.score > 0 || match.legalBalls > 0 || match.status == "FINISHED"
            if (hasActivity) {
                dataA.matchesPlayed++
                dataB.matchesPlayed++

                if (match.status == "FINISHED") {
                    if (match.target > 0) {
                        // Innings 2 finished
                        if (match.score >= match.target) {
                            // Bowling team (batting in 2nd innings) won
                            if (match.battingTeam == teamB) {
                                dataB.won++
                                dataB.points += 2
                                dataB.forms.add("W")
                                dataA.lost++
                                dataA.forms.add("L")
                            } else {
                                dataA.won++
                                dataA.points += 2
                                dataA.forms.add("W")
                                dataB.lost++
                                dataB.forms.add("L")
                            }
                        } else {
                            // Defending team won
                            if (match.battingTeam == teamB) {
                                dataA.won++
                                dataA.points += 2
                                dataA.forms.add("W")
                                dataB.lost++
                                dataB.forms.add("L")
                            } else {
                                dataB.won++
                                dataB.points += 2
                                dataB.forms.add("W")
                                dataA.lost++
                                dataA.forms.add("L")
                            }
                        }
                    } else {
                        // First innings finished or tie
                        dataA.tied++
                        dataA.points += 1
                        dataA.forms.add("T")
                        dataB.tied++
                        dataB.points += 1
                        dataB.forms.add("T")
                    }
                }
            }
        }

        val standings = teamMap.values
            .filter { it.matchesPlayed > 0 }
            .sortedWith(compareByDescending<MutableTeamData> { it.points }.thenByDescending { it.won })
            .map { t ->
                TeamStandingEntity(
                    teamId = t.teamId,
                    name = t.name,
                    shortName = t.shortName,
                    matchesPlayed = t.matchesPlayed,
                    won = t.won,
                    lost = t.lost,
                    tied = t.tied,
                    points = t.points,
                    netRunRate = if (t.matchesPlayed > 0) (t.won - t.lost) * 0.45 else 0.0,
                    formGuide = if (t.forms.isEmpty()) "–" else t.forms.takeLast(5).joinToString(",")
                )
            }

        // --- 2. Compute Player Stats ---
        val playerMap = mutableMapOf<String, MutablePlayerData>()

        for (match in matches) {
            val team = match.battingTeam.ifBlank { match.teamA }
            val bowlTeam = match.bowlingTeam.ifBlank { match.teamB }

            // Active Striker
            if (match.strikerName.isNotBlank() && !match.strikerName.equals("Striker", ignoreCase = true)) {
                val p = playerMap.getOrPut(match.strikerName.trim()) {
                    MutablePlayerData(match.strikerName.trim(), team, "Batter")
                }
                p.matches++
                p.runs += match.strikerRuns
                p.balls += match.strikerBalls
                if (match.strikerRuns > p.highestScore) p.highestScore = match.strikerRuns
            }

            // Active Non-Striker
            if (match.nonStrikerName.isNotBlank() && !match.nonStrikerName.equals("Non-Striker", ignoreCase = true)) {
                val p = playerMap.getOrPut(match.nonStrikerName.trim()) {
                    MutablePlayerData(match.nonStrikerName.trim(), team, "Batter")
                }
                p.matches++
                p.runs += match.nonStrikerRuns
                p.balls += match.nonStrikerBalls
                if (match.nonStrikerRuns > p.highestScore) p.highestScore = match.nonStrikerRuns
            }

            // Active Bowler
            if (match.bowlerName.isNotBlank() && !match.bowlerName.equals("Bowler", ignoreCase = true)) {
                val p = playerMap.getOrPut(match.bowlerName.trim()) {
                    MutablePlayerData(match.bowlerName.trim(), bowlTeam, "Bowler")
                }
                p.matches++
                p.wickets += match.bowlerWickets
                p.bowlingRuns += match.bowlerRuns
                p.bowlingBalls += match.bowlerBalls
            }

            // Dismissed Batsmen from this match
            val dismissed = parseDismissedBatsmen(match.dismissedBatsmenJson)
            for (d in dismissed) {
                if (d.name.isNotBlank()) {
                    val p = playerMap.getOrPut(d.name.trim()) {
                        MutablePlayerData(d.name.trim(), team, "Batter")
                    }
                    p.runs += d.runs
                    p.balls += d.balls
                    if (d.runs > p.highestScore) p.highestScore = d.runs
                }
            }
        }

        val sortedPlayers = playerMap.values
            .filter { it.runs > 0 || it.wickets > 0 || it.balls > 0 }
            .sortedWith(compareByDescending<MutablePlayerData> { it.runs * 2 + it.wickets * 25 }.thenByDescending { it.runs })

        val maxRuns = sortedPlayers.maxOfOrNull { it.runs } ?: 0
        val maxWickets = sortedPlayers.maxOfOrNull { it.wickets } ?: 0

        val playerStats = sortedPlayers.mapIndexed { idx, p ->
            val sr = if (p.balls > 0) (p.runs.toDouble() / p.balls) * 100 else 0.0
            val eco = if (p.bowlingBalls > 0) (p.bowlingRuns.toDouble() / (p.bowlingBalls / 6.0)) else 0.0
            val fantasy = (p.runs * 1.5 + p.wickets * 25 + (if (p.highestScore >= 50) 16 else 0)).toInt()

            PlayerStatEntity(
                playerId = "p_${p.name.lowercase().replace(" ", "_")}",
                name = p.name,
                team = p.team,
                role = if (p.wickets > 0 && p.runs > 20) "All-Rounder" else p.role,
                matches = p.matches.coerceAtLeast(1),
                runs = p.runs,
                highestScore = p.highestScore,
                wickets = p.wickets,
                strikeRate = Math.round(sr * 10.0) / 10.0,
                economy = Math.round(eco * 10.0) / 10.0,
                fantasyPoints = fantasy,
                ranking = idx + 1,
                isOrangeCap = p.runs > 0 && p.runs == maxRuns,
                isPurpleCap = p.wickets > 0 && p.wickets == maxWickets
            )
        }

        return Pair(standings, playerStats)
    }

    private data class MutableTeamData(
        val teamId: String,
        val name: String,
        val shortName: String,
        var matchesPlayed: Int = 0,
        var won: Int = 0,
        var lost: Int = 0,
        var tied: Int = 0,
        var points: Int = 0,
        val forms: MutableList<String> = mutableListOf()
    )

    private data class MutablePlayerData(
        val name: String,
        val team: String,
        var role: String,
        var matches: Int = 0,
        var runs: Int = 0,
        var balls: Int = 0,
        var highestScore: Int = 0,
        var wickets: Int = 0,
        var bowlingBalls: Int = 0,
        var bowlingRuns: Int = 0
    )
}
