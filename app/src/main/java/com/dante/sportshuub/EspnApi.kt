package com.dante.sportshuub

import android.util.Log
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

object EspnApi {

    private const val TAG = "EspnApi"
    private const val BASE = "https://site.api.espn.com/apis/site/v2/sports"

    fun scoreboardUrl(league: LeagueInfo): String =
        "$BASE/${league.espnSport}/${league.espnLeague}/scoreboard"

    // Blocking network call. Always run this off the main thread.
    fun fetchJson(url: String): JSONObject {
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            conn.requestMethod = "GET"

            val code = conn.responseCode
            if (code != HttpURLConnection.HTTP_OK) {
                throw IOException("HTTP $code")
            }

            val text = conn.inputStream.bufferedReader().use { it.readText() }
            return JSONObject(text)
        } finally {
            conn.disconnect()
        }
    }
    private fun parseTeam(competitor: JSONObject): EspnGameTeam {
        val team = competitor.optJSONObject("team")
        return EspnGameTeam(
            espnId = team?.optString("id") ?: "",
            abbreviation = team?.optString("abbreviation") ?: "",
            name = team?.optString("displayName") ?: "",
            logoUrl = team?.optString("logo") ?: "",
            score = competitor.optString("score").toIntOrNull()
        )
    }

    fun parseScoreboard(league: LeagueInfo, json: JSONObject): List<EspnGame> {
        val events = json.optJSONArray("events") ?: return emptyList()
        val games = mutableListOf<EspnGame>()

        for (i in 0 until events.length()) {
            val event = events.optJSONObject(i) ?: continue
            val competition = event.optJSONArray("competitions")
                ?.optJSONObject(0) ?: continue
            val competitors = competition.optJSONArray("competitors") ?: continue

            var homeJson: JSONObject? = null
            var awayJson: JSONObject? = null
            for (j in 0 until competitors.length()) {
                val c = competitors.optJSONObject(j) ?: continue
                when (c.optString("homeAway")) {
                    "home" -> homeJson = c
                    "away" -> awayJson = c
                }
            }
            if (homeJson == null || awayJson == null) continue

            val statusType = event.optJSONObject("status")?.optJSONObject("type")

            games.add(
                EspnGame(
                    id = event.optString("id"),
                    leagueKey = league.key,
                    startTime = event.optString("date"),
                    state = statusType?.optString("state") ?: "pre",
                    statusText = statusType?.optString("shortDetail") ?: "",
                    home = parseTeam(homeJson),
                    away = parseTeam(awayJson)
                )
            )
        }
        return games
    }

    // Blocking. Call off the main thread.
    fun fetchScoreboard(league: LeagueInfo): List<EspnGame> =
        parseScoreboard(league, fetchJson(scoreboardUrl(league)))

    // TEMPORARY: logs every game on every league's scoreboard.
    fun testScoreboards() {
        Thread {
            for (league in LeagueRegistry.leagues) {
                try {
                    val games = fetchScoreboard(league)
                    Log.d(TAG, "${league.key}: parsed ${games.size} games")
                    for (g in games) {
                        Log.d(
                            TAG,
                            "  ${g.away.abbreviation} ${g.away.score} @ " +
                                    "${g.home.abbreviation} ${g.home.score} " +
                                    "| ${g.state} | ${g.statusText}"
                        )
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "${league.key}: FAILED (${e.message})")
                }
            }
        }.start()
    }
    // TEMPORARY: checks every league in the registry and logs the result.
    fun testAllLeagues() {
        Thread {
            for (league in LeagueRegistry.leagues) {
                try {
                    val json = fetchJson(scoreboardUrl(league))
                    val games = json.optJSONArray("events")?.length() ?: 0
                    val feedName = json.optJSONArray("leagues")
                        ?.optJSONObject(0)
                        ?.optString("name") ?: "unknown"
                    Log.d(TAG, "${league.key}: OK -> \"$feedName\", $games games on scoreboard")
                } catch (e: Exception) {
                    Log.e(TAG, "${league.key}: FAILED (${e.message})")
                }
            }
        }.start()
    }
}