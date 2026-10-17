package com.dante.sportshuub

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class ConferenceInfo(
    val leagueKey: String,      // "CFB" or "CBB"
    val id: String,             // ESPN group id
    val name: String,           // e.g. "Southeastern Conference"
    val shortName: String,      // e.g. "SEC"
    val teamIds: List<String>   // ESPN team ids in the conference
)

// Loads conference membership from ESPN and keeps a day-old copy on the device,
// so the home screen doesn't need an extra request on every launch.
object ConferenceRepository {

    private const val PREFS = "sports_hub_conferences"
    private const val TTL_MS = 24L * 60 * 60 * 1000

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    // Blocking. Call off the main thread. Empty list = couldn't load.
    fun getConferences(context: Context, league: LeagueInfo): List<ConferenceInfo> {

        val fresh = readCache(context, league.key, ignoreAge = false)
        if (fresh != null) return fresh

        val loaded = try {
            EspnApi.fetchConferences(league)
        } catch (e: Exception) {
            emptyList()
        }

        if (loaded.isNotEmpty()) {
            writeCache(context, league.key, loaded)
            return loaded
        }

        // Offline? An older copy is better than nothing.
        return readCache(context, league.key, ignoreAge = true) ?: emptyList()
    }

    // Blocking. Team keys (see FavoritesStore.teamKey) of every team that is in
    // one of the user's favorite conferences.
    fun favoriteConferenceTeamKeys(context: Context): Set<String> {

        val favorites = FavoritesStore.getFavoriteConferences(context)
        if (favorites.isEmpty()) return emptySet()

        val result = mutableSetOf<String>()

        for (leagueKey in favorites.map { it.substringBefore(":") }.toSet()) {
            val league = LeagueRegistry.byKey(leagueKey) ?: continue

            for (conference in getConferences(context, league)) {
                if (FavoritesStore.conferenceKey(leagueKey, conference.id) in favorites) {
                    for (teamId in conference.teamIds) {
                        result.add(FavoritesStore.teamKey(leagueKey, teamId))
                    }
                }
            }
        }
        return result
    }

    private fun readCache(
        context: Context,
        leagueKey: String,
        ignoreAge: Boolean
    ): List<ConferenceInfo>? {
        return try {
            val text = prefs(context).getString("cache_$leagueKey", null) ?: return null
            val json = JSONObject(text)

            val age = System.currentTimeMillis() - json.optLong("time", 0L)
            if (!ignoreAge && age > TTL_MS) return null

            val items = json.optJSONArray("items") ?: return null
            val result = mutableListOf<ConferenceInfo>()

            for (i in 0 until items.length()) {
                val item = items.optJSONObject(i) ?: continue
                val teams = item.optJSONArray("teams") ?: JSONArray()
                val teamIds = mutableListOf<String>()
                for (j in 0 until teams.length()) teamIds.add(teams.optString(j))

                result.add(
                    ConferenceInfo(
                        leagueKey = leagueKey,
                        id = item.optString("id"),
                        name = item.optString("name"),
                        shortName = item.optString("short"),
                        teamIds = teamIds
                    )
                )
            }
            if (result.isEmpty()) null else result
        } catch (e: Exception) {
            null
        }
    }

    private fun writeCache(
        context: Context,
        leagueKey: String,
        conferences: List<ConferenceInfo>
    ) {
        val items = JSONArray()

        for (c in conferences) {
            items.put(
                JSONObject()
                    .put("id", c.id)
                    .put("name", c.name)
                    .put("short", c.shortName)
                    .put("teams", JSONArray(c.teamIds))
            )
        }

        val json = JSONObject()
            .put("time", System.currentTimeMillis())
            .put("items", items)

        prefs(context).edit().putString("cache_$leagueKey", json.toString()).apply()
    }
}
