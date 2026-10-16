package com.dante.sportshuub

data class EspnGameTeam(
    val espnId: String,
    val abbreviation: String,
    val name: String,
    val logoUrl: String,
    val score: Int?,
    val rank: Int? = null,     // poll rank (1-25) for ranked college teams
    val record: String = ""    // season record, e.g. "5-1"
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

data class EspnTeamInfo(
    val espnId: String,
    val name: String,
    val abbreviation: String,
    val logoUrl: String
)
