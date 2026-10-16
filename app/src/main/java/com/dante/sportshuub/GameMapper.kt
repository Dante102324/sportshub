package com.dante.sportshuub

// Converts an ESPN game into the Game class your tiles already use.
fun EspnGame.toTileGame(): Game {
    val league = LeagueRegistry.byKey(leagueKey)
    val started = state != "pre"

    return Game(
        id = "${leagueKey}_$id",
        sport = league?.sport ?: "",
        league = league?.displayName ?: leagueKey,
        awayTeam = away.abbreviation,
        awayScore = if (started) (away.score?.toString() ?: "") else "",
        homeTeam = home.abbreviation,
        homeScore = if (started) (home.score?.toString() ?: "") else "",
        status = statusText,
        startTime = startTime,
        awayLogo = away.logoUrl,
        homeLogo = home.logoUrl
    )
}