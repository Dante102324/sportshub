package com.dante.sportshuub

data class Team(
    val id: String,
    val name: String,
    val abbreviation: String,
    val sport: String,
    val league: String,
    val logoFileName: String,
    val logoUrl: String
)