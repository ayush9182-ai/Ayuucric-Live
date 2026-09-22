package com.example.domain.engine

import com.example.data.model.MatchEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MatchScoringEngineTest {

    private lateinit var baseMatch: MatchEntity

    @Before
    fun setUp() {
        baseMatch = MatchEntity(
            id = "test_match_1",
            tournamentName = "Premier League",
            teamA = "India",
            teamB = "Australia",
            teamAShort = "IND",
            teamBShort = "AUS",
            battingTeam = "India",
            bowlingTeam = "Australia",
            totalOvers = 20,
            score = 100,
            wickets = 2,
            legalBalls = 60, // 10.0 overs
            strikerName = "Rohit",
            strikerRuns = 40,
            strikerBalls = 25,
            strikerFours = 4,
            strikerSixes = 2,
            nonStrikerName = "Kohli",
            nonStrikerRuns = 35,
            nonStrikerBalls = 20,
            nonStrikerFours = 3,
            nonStrikerSixes = 1,
            bowlerName = "Starc",
            bowlerBalls = 12,
            bowlerRuns = 18,
            bowlerWickets = 1,
            target = 180,
            status = "LIVE",
            statusDetail = "Need 80 runs in 60 balls",
            currentInnings = 2
        )
    }

    @Test
    fun dotBall_incrementsBowlerBalls_andLegalBalls_keepsStrike() {
        val input = DeliveryInput(
            matchId = baseMatch.id,
            runs = 0,
            isWicket = false,
            extraType = "None"
        )

        val result = MatchScoringEngine.processDelivery(baseMatch, input)
        val match = result.updatedMatch

        assertEquals(100, match.score)
        assertEquals(2, match.wickets)
        assertEquals(61, match.legalBalls)
        assertEquals(13, match.bowlerBalls)
        assertEquals(18, match.bowlerRuns)
        assertEquals("Rohit", match.strikerName)
        assertEquals("Kohli", match.nonStrikerName)
        assertEquals(26, match.strikerBalls)
    }

    @Test
    fun singleRun_incrementsScore_switchesStrike() {
        val input = DeliveryInput(
            matchId = baseMatch.id,
            runs = 1,
            isWicket = false,
            extraType = "None"
        )

        val result = MatchScoringEngine.processDelivery(baseMatch, input)
        val match = result.updatedMatch

        assertEquals(101, match.score)
        assertEquals(61, match.legalBalls)
        assertEquals(41, result.updatedMatch.nonStrikerRuns) // Rohit moved to non-striker
        assertEquals("Kohli", match.strikerName) // Kohli is now on strike
        assertEquals("Rohit", match.nonStrikerName)
    }

    @Test
    fun boundaryFour_awardsFour_strikerRetainsStrike() {
        val input = DeliveryInput(
            matchId = baseMatch.id,
            runs = 4,
            isWicket = false,
            extraType = "None"
        )

        val result = MatchScoringEngine.processDelivery(baseMatch, input)
        val match = result.updatedMatch

        assertEquals(104, match.score)
        assertEquals(44, match.strikerRuns)
        assertEquals(5, match.strikerFours)
        assertEquals("Rohit", match.strikerName)
        assertTrue(result.ballEvent.isBoundary)
    }

    @Test
    fun boundarySix_awardsSix_strikerRetainsStrike() {
        val input = DeliveryInput(
            matchId = baseMatch.id,
            runs = 6,
            isWicket = false,
            extraType = "None"
        )

        val result = MatchScoringEngine.processDelivery(baseMatch, input)
        val match = result.updatedMatch

        assertEquals(106, match.score)
        assertEquals(46, match.strikerRuns)
        assertEquals(3, match.strikerSixes)
        assertEquals("Rohit", match.strikerName)
        assertTrue(result.ballEvent.isSix)
    }

    @Test
    fun wideDelivery_addsPenalty_legalBallsUnchanged_batsmanBallsUnchanged() {
        val input = DeliveryInput(
            matchId = baseMatch.id,
            runs = 0,
            isWicket = false,
            extraType = "Wide"
        )

        val result = MatchScoringEngine.processDelivery(baseMatch, input)
        val match = result.updatedMatch

        assertEquals(101, match.score) // 1 penalty
        assertEquals(60, match.legalBalls) // Legal ball does not advance
        assertEquals(25, match.strikerBalls) // Striker ball faced does not increment
        assertEquals(12, match.bowlerBalls) // Bowler ball does not advance
        assertEquals(19, match.bowlerRuns) // Bowler charged for wide
        assertEquals("Rohit", match.strikerName)
    }

    @Test
    fun noBallWithRuns_addsPenaltyAndRuns_legalBallsUnchanged() {
        val input = DeliveryInput(
            matchId = baseMatch.id,
            runs = 4,
            isWicket = false,
            extraType = "NoBall"
        )

        val result = MatchScoringEngine.processDelivery(baseMatch, input)
        val match = result.updatedMatch

        assertEquals(105, match.score) // 4 runs + 1 penalty
        assertEquals(60, match.legalBalls) // No-ball is not a legal ball
        assertEquals(44, match.strikerRuns) // Batsman gets the 4 runs
        assertEquals(26, match.strikerBalls) // Batsman faced the delivery
        assertEquals("Rohit", match.strikerName)
    }

    @Test
    fun overCompletion_rotatesStrike() {
        // Match with 5 balls into the over (e.g. 5 legal balls)
        val nearOverMatch = baseMatch.copy(legalBalls = 5)
        val input = DeliveryInput(
            matchId = nearOverMatch.id,
            runs = 0,
            isWicket = false,
            extraType = "None"
        )

        val result = MatchScoringEngine.processDelivery(nearOverMatch, input)
        val match = result.updatedMatch

        assertEquals(6, match.legalBalls)
        // 6th legal ball ends over -> strike rotates
        assertEquals("Kohli", match.strikerName)
        assertEquals("Rohit", match.nonStrikerName)
    }

    @Test
    fun singleOnLastBallOfOver_strikeDoesNotRotateTwice() {
        val nearOverMatch = baseMatch.copy(legalBalls = 5)
        val input = DeliveryInput(
            matchId = nearOverMatch.id,
            runs = 1,
            isWicket = false,
            extraType = "None"
        )

        val result = MatchScoringEngine.processDelivery(nearOverMatch, input)
        val match = result.updatedMatch

        assertEquals(6, match.legalBalls)
        // Single took Rohit to non-striker, but end-of-over brought Rohit back to strike!
        assertEquals("Rohit", match.strikerName)
        assertEquals("Kohli", match.nonStrikerName)
    }

    @Test
    fun wicketBowled_incrementsWickets_recordsDismissal_bringsNewBatsman() {
        val input = DeliveryInput(
            matchId = baseMatch.id,
            runs = 0,
            isWicket = true,
            wicketType = "Bowled",
            newBatsmanName = "Surya",
            dismissedBatsman = "Rohit",
            newBatsmanOnStrike = true
        )

        val result = MatchScoringEngine.processDelivery(baseMatch, input)
        val match = result.updatedMatch

        assertEquals(3, match.wickets)
        assertEquals(2, match.bowlerWickets)
        assertEquals("Surya", match.strikerName)
        assertEquals("Kohli", match.nonStrikerName)
        assertEquals(0, match.strikerRuns)
        assertTrue(match.dismissedBatsmenJson.contains("Rohit"))
        assertTrue(match.dismissedBatsmenJson.contains("b Starc"))
    }

    @Test
    fun targetReached_finishesMatch() {
        val matchNearWin = baseMatch.copy(score = 178, target = 180)
        val input = DeliveryInput(
            matchId = matchNearWin.id,
            runs = 4,
            isWicket = false,
            extraType = "None"
        )

        val result = MatchScoringEngine.processDelivery(matchNearWin, input)
        val match = result.updatedMatch

        assertEquals(182, match.score)
        assertEquals("FINISHED", match.status)
        assertTrue(match.statusDetail.contains("won by"))
    }
}
