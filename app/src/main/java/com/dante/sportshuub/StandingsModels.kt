package com.dante.sportshuub

// ---- Standings tables ----

data class StandingsRow(
    val position: Int,
    val teamId: String,
    val teamName: String,
    val abbreviation: String,
    val logoUrl: String,
    val values: List<String>     // one value per column
)

data class StandingsTable(
    val name: String,            // conference / division / league name
    val columns: List<String>,   // column headers, e.g. W, L, PCT
    val rows: List<StandingsRow>
)

// ---- Polls (AP Top 25, CFP, ...) ----

data class RankedTeam(
    val rank: Int,
    val teamId: String,
    val name: String,            // school name, e.g. "Alabama"
    val abbreviation: String,
    val logoUrl: String,
    val record: String,          // e.g. "6-0"
    val points: String,          // poll points, empty if the poll has none
    val previous: String         // last week's rank, empty if unranked
)

data class RankingPoll(
    val name: String,            // e.g. "AP Top 25"
    val type: String,
    val week: String,            // e.g. "Week 8" (may be empty)
    val ranks: List<RankedTeam>
)
