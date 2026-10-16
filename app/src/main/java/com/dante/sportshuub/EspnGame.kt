package com.dante.sportshuub

data class EspnGameTeam(
    val espnId: String,
    val abbreviation: String,
    val name: String,
    val logoUrl: String,
    val score: Int?
)

data class EspnGame(
    val id: String,
    val leagueKey: String,
    val startTime: String,
    val state: String,       // "pre" = upcoming, "in" = live, "post" = finished
    val statusText: String,
    val home: EspnGameTeam,
    val away: EspnGameTeam
)