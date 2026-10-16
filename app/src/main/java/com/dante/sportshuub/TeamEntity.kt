package com.dante.sportshuub

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "teams")
data class TeamEntity(

    @PrimaryKey
    val id: String,

    val name: String,

    val abbreviation: String,

    val sport: String,

    val league: String,

    val logoFileName: String,

    val logoUrl: String
)