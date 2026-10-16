package com.dante.sportshuub

data class LeagueInfo(
    val key: String,          // our stable key; matches TeamEntity.league (e.g. "NBA")
    val displayName: String,
    val sport: String,        // our sport label
    val espnSport: String,    // ESPN URL segment, e.g. "basketball"
    val espnLeague: String,   // ESPN URL segment, e.g. "nba" or "eng.1"
    val scoreboardQuery: String = ""   // extra URL parameters, e.g. "?groups=80"
)

object LeagueRegistry {

    val leagues = listOf(
        LeagueInfo("NBA", "NBA", "Basketball", "basketball", "nba"),
        LeagueInfo("NFL", "NFL", "Football", "football", "nfl"),
        LeagueInfo("MLB", "MLB", "Baseball", "baseball", "mlb"),
        LeagueInfo("NHL", "NHL", "Hockey", "hockey", "nhl"),
        LeagueInfo(
            "CFB", "College Football", "Football", "football", "college-football",
            "?groups=80&limit=300"
        ),
        LeagueInfo(
            "CBB", "College Basketball", "Basketball", "basketball", "mens-college-basketball",
            "?groups=50&limit=300"
        ),
        LeagueInfo("MLS", "MLS", "Soccer", "soccer", "usa.1"),
        LeagueInfo("BRASILEIRAO", "Brasileirão", "Soccer", "soccer", "bra.1"),
        LeagueInfo("LIBERTADORES", "Copa Libertadores", "Soccer", "soccer", "conmebol.libertadores"),
        LeagueInfo("SUDAMERICANA", "Copa Sudamericana", "Soccer", "soccer", "conmebol.sudamericana"),
        LeagueInfo("UCL", "Champions League", "Soccer", "soccer", "uefa.champions"),
        LeagueInfo("UEL", "Europa League", "Soccer", "soccer", "uefa.europa"),
        LeagueInfo("LALIGA", "La Liga", "Soccer", "soccer", "esp.1"),
        LeagueInfo("LIGUE1", "Ligue 1", "Soccer", "soccer", "fra.1"),
        LeagueInfo("BUNDESLIGA", "Bundesliga", "Soccer", "soccer", "ger.1"),
        LeagueInfo("EPL", "Premier League", "Soccer", "soccer", "eng.1"),
        LeagueInfo("SERIEA", "Serie A", "Soccer", "soccer", "ita.1")
    )

    fun byKey(key: String): LeagueInfo? =
        leagues.firstOrNull { it.key.equals(key, ignoreCase = true) }
}
