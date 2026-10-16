package com.dante.sportshuub

// One row of the team-stats table (values are already formatted by ESPN).
data class StatRow(
    val label: String,
    val away: String,
    val home: String
)

// "Points - Jayson Tatum - 32"
data class LeaderLine(
    val category: String,
    val athlete: String,
    val value: String
)

data class TeamLeaders(
    val abbreviation: String,
    val lines: List<LeaderLine>
)

// Score by period (quarters, innings, periods...). Empty for some sports.
data class LineScore(
    val away: List<String>,
    val home: List<String>
)

data class EspnGameDetail(
    val state: String,        // "pre", "in" or "post"
    val statusText: String,   // e.g. "Final", live clock text
    val awayScore: String,
    val homeScore: String,
    val stats: List<StatRow>,
    val leaders: List<TeamLeaders>,   // [away, home]
    val lineScore: LineScore?
)
