package com.dante.sportshuub

import android.util.Log

// Loads the college polls from ESPN and keeps them in memory for a while.
object RankingsRepository {

    private const val TAG = "Rankings"
    private const val TTL_MS = 30L * 60 * 1000

    private val collegeLeagueKeys = listOf("CFB", "CBB")

    private val cache = mutableMapOf<String, Pair<Long, List<RankingPoll>>>()

    // Blocking. Call off the main thread.
    // Polls come back in order of preference: the College Football Playoff
    // ranking (once ESPN publishes it), then the AP Top 25, then the rest.
    @Synchronized
    fun getPolls(league: LeagueInfo): List<RankingPoll> {

        val hit = cache[league.key]
        if (hit != null && System.currentTimeMillis() - hit.first < TTL_MS) {
            return hit.second
        }

        val polls = try {
            sortByPreference(EspnApi.fetchRankings(league))
        } catch (e: Exception) {
            Log.e(TAG, "${league.key} rankings failed: ${e.message}")
            return hit?.second ?: emptyList()
        }

        cache[league.key] = Pair(System.currentTimeMillis(), polls)
        return polls
    }

    fun sortByPreference(polls: List<RankingPoll>): List<RankingPoll> =
        polls.sortedBy { preferenceOrder(it) }

    private fun preferenceOrder(poll: RankingPoll): Int {
        val name = poll.name.lowercase()
        val type = poll.type.lowercase()

        return when {
            name.contains("playoff") || type == "cfp" -> 0
            name.startsWith("ap ") || name.contains("ap top") || type == "ap" -> 1
            else -> 2
        }
    }

    // Blocking. team key (see FavoritesStore.teamKey) -> rank (1..25) from the
    // preferred poll of each college league.
    fun rankMap(): Map<String, Int> {

        val result = mutableMapOf<String, Int>()

        for (key in collegeLeagueKeys) {
            val league = LeagueRegistry.byKey(key) ?: continue
            val poll = getPolls(league).firstOrNull() ?: continue

            for (team in poll.ranks) {
                if (team.rank in 1..25) {
                    result[FavoritesStore.teamKey(key, team.teamId)] = team.rank
                }
            }
        }
        return result
    }

    // Fills in rankings that the scoreboard itself didn't include.
    fun applyRanks(games: List<EspnGame>, ranks: Map<String, Int>): List<EspnGame> {

        if (ranks.isEmpty()) return games

        return games.map { game ->
            if (game.leagueKey !in collegeLeagueKeys) {
                game
            } else {
                game.copy(
                    home = game.home.copy(
                        rank = game.home.rank
                            ?: ranks[FavoritesStore.teamKey(game.leagueKey, game.home.espnId)]
                    ),
                    away = game.away.copy(
                        rank = game.away.rank
                            ?: ranks[FavoritesStore.teamKey(game.leagueKey, game.away.espnId)]
                    )
                )
            }
        }
    }
}
