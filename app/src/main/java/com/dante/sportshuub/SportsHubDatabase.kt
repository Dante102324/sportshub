package com.dante.sportshuub

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [TeamEntity::class],
    version = 1,
    exportSchema = false
)
abstract class SportsHubDatabase : RoomDatabase() {

    abstract fun teamDao(): TeamDao

    companion object {

        @Volatile
        private var INSTANCE: SportsHubDatabase? = null

        fun getInstance(context: Context): SportsHubDatabase {

            return INSTANCE ?: synchronized(this) {

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SportsHubDatabase::class.java,
                    "sports_hub_database"
                ).build()

                INSTANCE = instance

                instance
            }
        }
    }
}