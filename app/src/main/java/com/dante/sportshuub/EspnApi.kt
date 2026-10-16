package com.dante.sportshuub

import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

object EspnApi {

    private const val BASE = "https://site.api.espn.com/apis/site/v2/sports"

    fun scoreboardUrl(league: LeagueInfo): String =
        "$BASE/${league.espnSport}/${league.espnLeague}/scoreboard${league.scoreboardQuery}"

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

        // ESPN uses 99 for "unranked"; only keep real top-25 ranks.
        val rank = competitor.optJSONObject("curatedRank")
            ?.optInt("current", 99)
            ?.takeIf { it in 1..25 }

        val record = competitor.optJSONArray("records")
            ?.optJSONObject(0)
            ?.optString("summary") ?: ""

        return EspnGameTeam(
            espnId = team?.optString("id") ?: "",
            abbreviation = team?.optString("abbreviation") ?: "",
            name = team?.optString("displayName") ?: "",
            logoUrl = team?.optString("logo") ?: "",
            score = competitor.optString("score").toIntOrNull(),
            rank = rank,
            record = record
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

    // Blocking. Call off the main thread.
    fun fetchTeams(league: LeagueInfo): List<EspnTeamInfo> {
        val url = "$BASE/${league.espnSport}/${league.espnLeague}/teams?limit=1000"
        val json = fetchJson(url)

        val teams = json.optJSONArray("sports")
            ?.optJSONObject(0)
            ?.optJSONArray("leagues")
            ?.optJSONObject(0)
            ?.optJSONArray("teams") ?: return emptyList()

        val result = mutableListOf<EspnTeamInfo>()

        for (i in 0 until teams.length()) {
            val team = teams.optJSONObject(i)?.optJSONObject("team") ?: continue

            result.add(
                EspnTeamInfo(
                    espnId = team.optString("id"),
                    name = team.optString("displayName"),
                    abbreviation = team.optString("abbreviation"),
                    logoUrl = team.optJSONArray("logos")
                        ?.optJSONObject(0)
                        ?.optString("href") ?: ""
                )
            )
        }

        return result.sortedBy { it.name }
    }
}
