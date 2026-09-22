package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.BallEventEntity
import com.example.data.model.MatchEntity
import com.example.data.model.NotificationAlertEntity
import com.example.data.model.PlayerStatEntity
import com.example.data.model.TeamStandingEntity

@Database(
    entities = [
        MatchEntity::class,
        BallEventEntity::class,
        TeamStandingEntity::class,
        PlayerStatEntity::class,
        NotificationAlertEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class CricketDatabase : RoomDatabase() {
    abstract fun cricketDao(): CricketDao

    companion object {
        @Volatile
        private var INSTANCE: CricketDatabase? = null

        fun getInstance(context: Context): CricketDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CricketDatabase::class.java,
                    "cricket_live_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
