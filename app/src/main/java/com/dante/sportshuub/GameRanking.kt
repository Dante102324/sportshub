package com.dante.sportshuub

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

object GameRanking {

    // Small base boost per competition for Top Games. Edit freely.
    private val leagueWeight = mapOf(
        "UCL" to 60,
        "NFL" to 55,
        "CFB" to 55,
        "NBA" to 50,
        "EPL" to 50,
        "LALIGA" to 45,
        "BUNDESLIGA" to 40,
        "SERIEA" to 40,
        "UEL" to 35,
        "MLB" to 35,
        "NHL" to 35,
        "LIBERTADORES" to 35,
        "CBB" to 30,
        "LIGUE1" to 30,
        "MLS" to 25,
        "BRASILEIRAO" to 30,
        "SUDAMERICANA" to 25
    )

    fun isFavorite(
        game: EspnGame,
        leagues: Set<String>,
        teams: Set<String>
    ): Boolean {
        return game.leagueKey in leagues ||
            FavoritesStore.teamKey(game.leagueKey, game.home.espnId) in teams ||
            FavoritesStore.teamKey(game.leagueKey, game.away.espnId) in teams
    }

    // Favorites first, then by start time (newest first for finished games).
    fun sortFavoritesFirst(
        games: List<EspnGame>,
        leagues: Set<String>,
        teams: Set<String>,
        newestFirst: Boolean = false
    ): List<EspnGame> {
        val byFavorite = compareBy<EspnGame> {
            if (isFavorite(it, leagues, teams)) 0 else 1
        }
        val comparator =
            if (newestFirst) byFavorite.thenByDescending { it.startTime }
            else byFavorite.thenBy { it.startTime }
        return games.sortedWith(comparator)
    }

    // Games involving favorites: live, then upcoming, then finished.
    fun favoriteGames(
        games: List<EspnGame>,
        leagues: Set<String>,
        teams: Set<String>
    ): List<EspnGame> {
        val stateOrder = mapOf("in" to 0, "pre" to 1, "post" to 2)
        return games
            .filter { isFavorite(it, leagues, teams) }
            .sortedWith(
                compareBy<EspnGame> { stateOrder[it.state] ?: 3 }
                    .thenBy { it.startTime }
            )
    }

    fun topGames(games: List<EspnGame>, limit: Int = 12): List<EspnGame> {
        val now = System.currentTimeMillis()
        return games
            .sortedByDescending { topScore(it, now) }
            .take(limit)
    }

    private fun topScore(game: EspnGame, now: Long): Int {
        var score = leagueWeight[game.leagueKey] ?: 20

        // 1. Rivalries
        if (Rivalries.find(game) != null) {
            score += 120
        }

        // 2. Ranked teams (college)
        val homeRank = game.home.rank
        val awayRank = game.away.rank
        if (homeRank != null && awayRank != null) {
            score += 100
            if (homeRank <= 10 && awayRank <= 10) score += 40
        } else if (homeRank != null || awayRank != null) {
            score += 15
        }

        // 3. Two strong records (pros and soccer): the weaker team sets the bonus.
        val homePct = winPct(game.home.record)
        val awayPct = winPct(game.away.record)
        if (homePct != null && awayPct != null) {
            val floor = minOf(homePct, awayPct)
            score += ((floor - 0.5) * 200).toInt().coerceAtLeast(0)
        }

        // 4. Game state
        when (game.state) {
            "in" -> score += 100
            "pre" -> {
                val start = parseStartMillis(game.startTime)
                if (start != null) {
                    val hoursAway = (start - now) / 3_600_000.0
                    score += when {
                        hoursAway <= 3 -> 40
                        hoursAway <= 12 -> 20
                        else -> 0
                    }
                }
            }
            "post" -> score -= 40
        }

        return score
    }

    // "5-1", "10-2-1", etc. Returns null until at least 4 games are played.
    private fun winPct(record: String): Double? {
        val numbers = record.split("-").mapNotNull { it.trim().toIntOrNull() }
        if (numbers.size < 2) return null

        val total = numbers.sum()
        if (total < 4) return null

        return numbers[0].toDouble() / total
    }

    private fun parseStartMillis(iso: String): Long? {
        return try {
            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'", Locale.US)
            parser.timeZone = TimeZone.getTimeZone("UTC")
            parser.parse(iso)?.time
        } catch (e: Exception) {
            null
        }
    }
}
