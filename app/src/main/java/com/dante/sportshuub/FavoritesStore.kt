package com.dante.sportshuub

import android.content.Context

object FavoritesStore {

    private const val PREFS = "sports_hub_favorites"
    private const val KEY_LEAGUES = "favorite_leagues"
    private const val KEY_TEAMS = "favorite_teams"
    private const val KEY_CONFERENCES = "favorite_conferences"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    // Team entries look like "NBA:2". All soccer competitions share one scope,
    // so a club favorited under La Liga also counts in the Champions League.
    fun teamKey(leagueKey: String, espnTeamId: String): String {
        val scope =
            if (LeagueRegistry.byKey(leagueKey)?.sport == "Soccer") "SOCCER"
            else leagueKey
        return "$scope:$espnTeamId"
    }

    // Conference entries look like "CFB:8" (league key + ESPN group id).
    fun conferenceKey(leagueKey: String, groupId: String): String =
        "$leagueKey:$groupId"

    fun getFavoriteConferences(context: Context): Set<String> =
        HashSet(prefs(context).getStringSet(KEY_CONFERENCES, emptySet()) ?: emptySet())

    fun setFavoriteConferences(context: Context, conferences: Set<String>) {
        prefs(context).edit().putStringSet(KEY_CONFERENCES, HashSet(conferences)).apply()
    }

    fun getFavoriteLeagues(context: Context): Set<String> =
        HashSet(prefs(context).getStringSet(KEY_LEAGUES, emptySet()) ?: emptySet())

    fun setFavoriteLeagues(context: Context, leagues: Set<String>) {
        prefs(context).edit().putStringSet(KEY_LEAGUES, HashSet(leagues)).apply()
    }

    fun getFavoriteTeams(context: Context): Set<String> =
        HashSet(prefs(context).getStringSet(KEY_TEAMS, emptySet()) ?: emptySet())

    fun setFavoriteTeams(context: Context, teams: Set<String>) {
        prefs(context).edit().putStringSet(KEY_TEAMS, HashSet(teams)).apply()
    }
}
