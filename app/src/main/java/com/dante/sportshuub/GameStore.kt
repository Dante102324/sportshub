package com.dante.sportshuub

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// The latest scoreboard data, shared so the AI screen can see what the home
// screen already loaded.
object GameStore {

    @Volatile
    var games: List<EspnGame> = emptyList()

    @Volatile
    var updatedAt: Long = 0L
}

// Turns the scoreboard into a compact text list for the AI to read.
object SportsAiContext {

    private const val MAX_GAMES = 150

    private fun teamText(team: EspnGameTeam, showScore: Boolean): String {
        val name = team.name.ifEmpty { team.abbreviation }
        val rank = team.rank?.let { "#$it " } ?: ""
        val score = if (showScore && team.score != null) " ${team.score}" else ""
        return "$rank$name$score"
    }

    fun build(
        games: List<EspnGame> = GameStore.games,
        updatedAt: Long = GameStore.updatedAt
    ): String {

        if (games.isEmpty()) {
            return "Live scoreboard: not loaded yet. Use web search for scores and schedules."
        }

        val order = mapOf("in" to 0, "pre" to 1, "post" to 2)

        val lines = games
            .sortedWith(
                compareBy<EspnGame> { order[it.state] ?: 3 }.thenBy { it.startTime }
            )
            .take(MAX_GAMES)
            .map { game ->
                val league = LeagueRegistry.byKey(game.leagueKey)?.displayName ?: game.leagueKey
                val tag = when (game.state) {
                    "in" -> "LIVE"
                    "post" -> "FINAL"
                    else -> "UPCOMING"
                }
                val showScore = game.state != "pre"

                "$league [$tag]: ${teamText(game.away, showScore)} at " +
                    "${teamText(game.home, showScore)} (${game.statusText})"
            }

        val time = if (updatedAt > 0) {
            SimpleDateFormat("h:mm a", Locale.US).format(Date(updatedAt))
        } else {
            "recently"
        }

        return "Live scoreboard (loaded $time):\n" + lines.joinToString("\n")
    }
}
