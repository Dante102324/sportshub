package com.dante.sportshuub

import android.content.Context

object DatabaseInitializer {

    fun initialize(context: Context) {

        Thread {

            val database = SportsHubDatabase.getInstance(context)
            val dao = database.teamDao()

            // Only populate the database if it is empty
            if (dao.getTeamCount() == 0) {

                val teams = TeamRepository.getAllTeams()

                val entities = teams.map {
                    it.toEntity()
                }

                dao.insertTeams(entities)

                android.util.Log.d(
                    "SPORTS_HUB_DATABASE",
                    "Inserted ${entities.size} teams into database"
                )
            }

        }.start()
    }
}