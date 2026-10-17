package com.dante.sportshuub

data class Game(
    val id: String,
    val sport: String,
    val league: String,
    val awayTeam: String,
    val awayScore: String,
    val homeTeam: String,
    val homeScore: String,
    val status: String,
    val startTime: String = "",
    val awayLogo: String = "",
    val homeLogo: String = "",
    val network: String = "",
    val streamingService: String = "",
    val iptvChannel: String = "",
    val state: String = "",  // "pre", "in" or "post"
    val awayRank: Int? = null,   // poll rank for ranked college teams
    val homeRank: Int? = null
)