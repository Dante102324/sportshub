package com.dante.sportshuub

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface TeamDao {

    @Query("SELECT * FROM teams")
    fun getAllTeams(): List<TeamEntity>

    @Query(
        "SELECT * FROM teams " +
                "WHERE league = :league " +
                "AND abbreviation = :abbreviation " +
                "LIMIT 1"
    )
    fun getTeam(
        league: String,
        abbreviation: String
    ): TeamEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertTeam(team: TeamEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertTeams(teams: List<TeamEntity>)

    @Query("DELETE FROM teams")
    fun deleteAllTeams()

    @Query("SELECT COUNT(*) FROM teams")
    fun getTeamCount(): Int
}