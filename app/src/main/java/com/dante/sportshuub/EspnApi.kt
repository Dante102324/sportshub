package com.dante.sportshuub

import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

object EspnApi {

    private const val BASE = "https://site.api.espn.com/apis/site/v2/sports"
    private const val STANDINGS_BASE = "https://site.api.espn.com/apis/v2/sports"

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

    // =====================================================
    // GAME DETAIL (box score stats, leaders, line score)
    // =====================================================

    // Blocking. Call off the main thread.
    fun fetchGameDetail(league: LeagueInfo, eventId: String): EspnGameDetail {
        val url = "$BASE/${league.espnSport}/${league.espnLeague}/summary?event=$eventId"
        return parseGameDetail(fetchJson(url))
    }

    private fun findByTeamId(array: JSONArray?, teamId: String): JSONObject? {
        if (array == null || teamId.isEmpty()) return null
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            if (item.optJSONObject("team")?.optString("id") == teamId) return item
        }
        return null
    }

    private fun periodValues(competitor: JSONObject?): List<String> {
        val array = competitor?.optJSONArray("linescores") ?: return emptyList()
        val out = mutableListOf<String>()
        for (i in 0 until array.length()) {
            out.add(array.optJSONObject(i)?.optString("displayValue") ?: "")
        }
        return out
    }

    // name -> (label, value), in ESPN's order
    private fun statEntries(box: JSONObject?): LinkedHashMap<String, Pair<String, String>> {
        val map = LinkedHashMap<String, Pair<String, String>>()
        val stats = box?.optJSONArray("statistics") ?: return map

        for (i in 0 until stats.length()) {
            val s = stats.optJSONObject(i) ?: continue
            val value = s.optString("displayValue")
            if (value.isEmpty()) continue

            val name = s.optString("name").ifEmpty { s.optString("label") }
            if (name.isEmpty()) continue

            val label = s.optString("label")
                .ifEmpty { s.optString("displayName").ifEmpty { name } }

            map[name] = Pair(label, value)
        }
        return map
    }

    private fun parseLeaders(
        leadersArray: JSONArray?,
        teamId: String,
        abbreviation: String
    ): TeamLeaders {
        val lines = mutableListOf<LeaderLine>()
        val categories = findByTeamId(leadersArray, teamId)?.optJSONArray("leaders")

        if (categories != null) {
            for (i in 0 until categories.length()) {
                val category = categories.optJSONObject(i) ?: continue
                val top = category.optJSONArray("leaders")?.optJSONObject(0) ?: continue

                val athlete = top.optJSONObject("athlete")
                val name = athlete?.optString("displayName").orEmpty()
                    .ifEmpty { athlete?.optString("shortName").orEmpty() }

                val label = category.optString("displayName")
                    .ifEmpty { category.optString("name") }

                if (name.isNotEmpty()) {
                    lines.add(LeaderLine(label, name, top.optString("displayValue")))
                }
            }
        }
        return TeamLeaders(abbreviation, lines.take(4))
    }

    fun parseGameDetail(json: JSONObject): EspnGameDetail {
        val competition = json.optJSONObject("header")
            ?.optJSONArray("competitions")
            ?.optJSONObject(0)

        val statusType = competition?.optJSONObject("status")?.optJSONObject("type")

        var homeComp: JSONObject? = null
        var awayComp: JSONObject? = null
        val competitors = competition?.optJSONArray("competitors")
        if (competitors != null) {
            for (i in 0 until competitors.length()) {
                val c = competitors.optJSONObject(i) ?: continue
                when (c.optString("homeAway")) {
                    "home" -> homeComp = c
                    "away" -> awayComp = c
                }
            }
        }

        val homeId = homeComp?.optJSONObject("team")?.optString("id") ?: ""
        val awayId = awayComp?.optJSONObject("team")?.optString("id") ?: ""
        val homeAbbr = homeComp?.optJSONObject("team")?.optString("abbreviation") ?: ""
        val awayAbbr = awayComp?.optJSONObject("team")?.optString("abbreviation") ?: ""

        // ---- Team stats ----
        val boxTeams = json.optJSONObject("boxscore")?.optJSONArray("teams")
        val awayStats = statEntries(findByTeamId(boxTeams, awayId))
        val homeStats = statEntries(findByTeamId(boxTeams, homeId))

        val keys = LinkedHashSet<String>()
        keys.addAll(awayStats.keys)
        keys.addAll(homeStats.keys)

        val rows = keys.take(16).mapNotNull { key ->
            val label = (awayStats[key] ?: homeStats[key])?.first ?: return@mapNotNull null
            StatRow(
                label = label,
                away = awayStats[key]?.second ?: "-",
                home = homeStats[key]?.second ?: "-"
            )
        }

        // ---- Leaders ----
        val leadersArray = json.optJSONArray("leaders")
        val leaders = listOf(
            parseLeaders(leadersArray, awayId, awayAbbr),
            parseLeaders(leadersArray, homeId, homeAbbr)
        )

        // ---- Line score ----
        val awayPeriods = periodValues(awayComp)
        val homePeriods = periodValues(homeComp)
        val lineScore =
            if (awayPeriods.isNotEmpty() || homePeriods.isNotEmpty()) {
                LineScore(awayPeriods, homePeriods)
            } else {
                null
            }

        return EspnGameDetail(
            state = statusType?.optString("state") ?: "pre",
            statusText = statusType?.optString("shortDetail") ?: "",
            awayScore = awayComp?.optString("score") ?: "",
            homeScore = homeComp?.optString("score") ?: "",
            stats = rows,
            leaders = leaders,
            lineScore = lineScore
        )
    }

    // =====================================================
    // CONFERENCES (college): who belongs to which conference
    // =====================================================

    // Blocking. Call off the main thread. Returns an empty list if ESPN's
    // standings can't be read.
    fun fetchConferences(league: LeagueInfo): List<ConferenceInfo> {
        val base = "$STANDINGS_BASE/${league.espnSport}/${league.espnLeague}/standings"

        val urls = mutableListOf<String>()
        if (league.standingsGroup.isNotEmpty()) {
            urls.add("$base?group=${league.standingsGroup}")
        }
        urls.add(base)

        for (url in urls) {
            try {
                val conferences = parseConferences(league, fetchJson(url))
                if (conferences.isNotEmpty()) return conferences
                Log.w("EspnApi", "No conferences found at $url")
            } catch (e: Exception) {
                Log.e("EspnApi", "Conferences failed at $url: ${e.message}")
            }
        }
        return emptyList()
    }

    fun parseConferences(league: LeagueInfo, json: JSONObject): List<ConferenceInfo> {
        val out = mutableListOf<ConferenceInfo>()
        collectConferences(league.key, json, out)
        return out.distinctBy { it.id }
    }

    // A conference is a leaf node that carries standings entries (its teams).
    private fun collectConferences(
        leagueKey: String,
        node: JSONObject,
        out: MutableList<ConferenceInfo>
    ) {
        val children = node.optJSONArray("children")

        if (children != null && children.length() > 0) {
            for (i in 0 until children.length()) {
                val child = children.optJSONObject(i) ?: continue
                collectConferences(leagueKey, child, out)
            }
            return
        }

        val entries = node.optJSONObject("standings")?.optJSONArray("entries") ?: return
        val id = node.optString("id")
        if (id.isEmpty()) return

        val teamIds = mutableListOf<String>()
        for (i in 0 until entries.length()) {
            val teamId = entries.optJSONObject(i)?.optJSONObject("team")?.optString("id")
            if (!teamId.isNullOrEmpty()) teamIds.add(teamId)
        }
        if (teamIds.isEmpty()) return

        val name = node.optString("name").ifEmpty { node.optString("abbreviation") }
        val shortName = node.optString("abbreviation")
            .ifEmpty { node.optString("shortName") }

        out.add(ConferenceInfo(leagueKey, id, name, shortName, teamIds))
    }
}
