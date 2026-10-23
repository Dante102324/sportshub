package com.dante.sportshuub

import org.json.JSONObject
import java.net.URLEncoder

// Network calls behind the search, team, player and league pages.
// All of these block: call them off the main thread.
object EspnPages {

    private const val SITE = "https://site.api.espn.com/apis/site/v2/sports"
    private const val STANDINGS = "https://site.api.espn.com/apis/v2/sports"
    private const val WEB = "https://site.web.api.espn.com/apis"

    private fun teamBase(league: LeagueInfo, teamId: String) =
        "$SITE/${league.espnSport}/${league.espnLeague}/teams/$teamId"

    private fun athleteBase(league: LeagueInfo, playerId: String) =
        "$WEB/common/v3/sports/${league.espnSport}/${league.espnLeague}/athletes/$playerId"

    fun searchPlayers(query: String): List<SearchResult> {
        val url = "$WEB/search/v2?limit=30&query=" + URLEncoder.encode(query, "UTF-8")
        return PageParsing.parseSearch(EspnApi.fetchJson(url))
    }

    fun fetchTeamInfo(league: LeagueInfo, teamId: String): TeamInfo? =
        PageParsing.parseTeamInfo(EspnApi.fetchJson(teamBase(league, teamId)))

    fun fetchSchedule(league: LeagueInfo, teamId: String): List<ScheduleItem> =
        PageParsing.parseSchedule(EspnApi.fetchJson(teamBase(league, teamId) + "/schedule"), teamId)

    fun fetchRoster(league: LeagueInfo, teamId: String): List<RosterPlayer> =
        PageParsing.parseRoster(EspnApi.fetchJson(teamBase(league, teamId) + "/roster"))

    // The team's line in the league standings (all stats), or null.
    fun fetchTeamStanding(league: LeagueInfo, teamId: String): TeamStanding? {

        val base = "$STANDINGS/${league.espnSport}/${league.espnLeague}/standings"

        val urls = mutableListOf<String>()
        if (league.standingsGroup.isNotEmpty()) urls.add("$base?group=${league.standingsGroup}")
        urls.add(base)

        for (url in urls) {
            try {
                val found = PageParsing.parseTeamStanding(EspnApi.fetchJson(url), teamId)
                if (found != null) return found
            } catch (e: Exception) {
                // try the next address
            }
        }
        return null
    }

    fun fetchPlayerOverview(league: LeagueInfo, playerId: String): SimpleTable? =
        PageParsing.parseOverviewStats(EspnApi.fetchJson(athleteBase(league, playerId) + "/overview"))

    fun fetchPlayerStats(league: LeagueInfo, playerId: String): List<SimpleTable> =
        PageParsing.parsePlayerStats(EspnApi.fetchJson(athleteBase(league, playerId) + "/stats"))

    fun fetchPlayerGameLog(league: LeagueInfo, playerId: String): SimpleTable? =
        PageParsing.parseGameLog(EspnApi.fetchJson(athleteBase(league, playerId) + "/gamelog"))
}
