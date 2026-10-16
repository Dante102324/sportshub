package com.dante.sportshuub

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

// "7:30 PM" if the game is today, otherwise "Fri 7:30 PM" (device local time).
private fun formatStartTime(iso: String): String {
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'", Locale.US)
        parser.timeZone = TimeZone.getTimeZone("UTC")
        val date = parser.parse(iso) ?: return ""

        val time = SimpleDateFormat("h:mm a", Locale.getDefault()).format(date)

        val gameCal = Calendar.getInstance()
        gameCal.time = date
        val now = Calendar.getInstance()

        val sameDay =
            gameCal.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                gameCal.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)

        if (sameDay) {
            time
        } else {
            SimpleDateFormat("EEE", Locale.getDefault()).format(date) + " " + time
        }
    } catch (e: Exception) {
        ""
    }
}

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
        status = if (state == "pre") formatStartTime(startTime).ifEmpty { statusText } else statusText,
        startTime = startTime,
        awayLogo = away.logoUrl,
        homeLogo = home.logoUrl
    )
}
