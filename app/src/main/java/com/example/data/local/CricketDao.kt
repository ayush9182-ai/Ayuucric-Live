package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.BallEventEntity
import com.example.data.model.MatchEntity
import com.example.data.model.NotificationAlertEntity
import com.example.data.model.PlayerStatEntity
import com.example.data.model.TeamStandingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CricketDao {

    @Query("SELECT * FROM matches")
    fun getAllMatches(): Flow<List<MatchEntity>>

    @Query("SELECT * FROM matches WHERE id = :matchId LIMIT 1")
    fun getMatchById(matchId: String): Flow<MatchEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatch(match: MatchEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatches(matches: List<MatchEntity>)

    @Update
    suspend fun updateMatch(match: MatchEntity)

    @Query("SELECT * FROM ball_events WHERE matchId = :matchId ORDER BY id DESC")
    fun getBallEventsForMatch(matchId: String): Flow<List<BallEventEntity>>

    @Query("SELECT * FROM ball_events WHERE matchId = :matchId ORDER BY id ASC")
    suspend fun getBallEventsAscending(matchId: String): List<BallEventEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBallEvent(ballEvent: BallEventEntity)

    @Query("DELETE FROM ball_events WHERE id = (SELECT MAX(id) FROM ball_events WHERE matchId = :matchId)")
    suspend fun deleteLastBallEvent(matchId: String)

    @Query("SELECT * FROM team_standings ORDER BY points DESC, netRunRate DESC")
    fun getTeamStandings(): Flow<List<TeamStandingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStandings(standings: List<TeamStandingEntity>)

    @Query("SELECT * FROM player_stats ORDER BY ranking ASC")
    fun getPlayerStats(): Flow<List<PlayerStatEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlayerStats(stats: List<PlayerStatEntity>)

    @Query("SELECT * FROM notifications ORDER BY timestamp DESC")
    fun getNotifications(): Flow<List<NotificationAlertEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationAlertEntity)

    @Query("UPDATE notifications SET isRead = 1")
    suspend fun markAllNotificationsRead()

    @androidx.room.Delete
    suspend fun deleteMatch(match: MatchEntity)

    @Query("DELETE FROM matches WHERE id = :matchId")
    suspend fun deleteMatchById(matchId: String)

    @Query("DELETE FROM matches")
    suspend fun deleteAllMatches()

    @Query("DELETE FROM ball_events")
    suspend fun deleteAllBallEvents()

    @Query("DELETE FROM ball_events WHERE matchId = :matchId")
    suspend fun deleteBallEventsForMatch(matchId: String)

    @Query("DELETE FROM team_standings")
    suspend fun deleteAllStandings()

    @Query("DELETE FROM player_stats")
    suspend fun deleteAllPlayerStats()
}
