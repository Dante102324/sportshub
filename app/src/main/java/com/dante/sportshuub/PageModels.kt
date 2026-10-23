package com.dante.sportshuub

// ---- Search ----

data class SearchResult(
    val kind: String,          // "league", "team" or "player"
    val leagueKey: String,
    val id: String,            // ESPN team / player id ("" for leagues)
    val title: String,
    val subtitle: String,
    val logoUrl: String        // team logo or player headshot
)

// ---- Team page ----

data class TeamInfo(
    val name: String,
    val abbreviation: String,
    val logoUrl: String,
    val record: String,            // e.g. "50-20"
    val standingSummary: String    // e.g. "1st in Atlantic Division"
)

data class ScheduleItem(
    val eventId: String,
    val dateIso: String,
    val opponentName: String,
    val opponentAbbreviation: String,
    val opponentLogo: String,
    val home: Boolean?,            // null if unknown
    val state: String,             // "pre", "in" or "post"
    val statusText: String,
    val teamScore: String,
    val opponentScore: String,
    val won: Boolean?
)

data class RosterPlayer(
    val id: String,
    val name: String,
    val jersey: String,
    val position: String,
    val age: String,
    val height: String,
    val weight: String,
    val headshotUrl: String
)

// ---- Generic table used by the team / player / league pages ----

data class SimpleRow(
    val label: String,
    val values: List<String>,
    val id: String = ""            // used when a row can be opened
)

data class SimpleTable(
    val title: String,
    val columns: List<String>,
    val rows: List<SimpleRow>
)

// A team's line in the league standings, with every stat ESPN lists.
data class TeamStanding(
    val group: String,
    val stats: List<Pair<String, String>>   // label to value
)
