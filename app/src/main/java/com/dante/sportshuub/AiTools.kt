package com.dante.sportshuub

import org.json.JSONArray
import org.json.JSONObject

// Lookups the AI can ask for. They read live ESPN data, so answers about
// standings, rankings and games don't depend on what the model remembers.
object AiTools {

    private const val MAX_CHARS = 5000

    private fun leagueList(): String =
        LeagueRegistry.leagues.joinToString(", ") { "${it.key} (${it.displayName})" }

    private fun stringProperty(description: String) =
        JSONObject().put("type", "string").put("description", description)

    private fun tool(name: String, description: String, properties: JSONObject, required: List<String>) =
        JSONObject()
            .put("type", "function")
            .put(
                "function",
                JSONObject()
                    .put("name", name)
                    .put("description", description)
                    .put(
                        "parameters",
                        JSONObject()
                            .put("type", "object")
                            .put("properties", properties)
                            .put("required", JSONArray(required))
                    )
            )

    fun definitions(): JSONArray {

        val leagueProp = stringProperty("League key. One of: ${leagueList()}")

        return JSONArray()
            .put(
                tool(
                    "get_standings",
                    "Current standings for a league. Use for records, seeds, who leads a division or conference.",
                    JSONObject()
                        .put("league", leagueProp)
                        .put(
                            "group",
                            stringProperty(
                                "Optional conference or division name to narrow the table, e.g. SEC, East, AFC North"
                            )
                        ),
                    listOf("league")
                )
            )
            .put(
                tool(
                    "get_rankings",
                    "Latest college poll (College Football Playoff when published, otherwise AP Top 25) for college football (CFB) or college basketball (CBB).",
                    JSONObject().put("league", leagueProp),
                    listOf("league")
                )
            )
            .put(
                tool(
                    "get_game_details",
                    "Box score details for one game: score by period, team stats and leaders. Use the game id shown in the live scoreboard.",
                    JSONObject().put("game_id", stringProperty("Game id from the scoreboard, like NBA_401234567")),
                    listOf("game_id")
                )
            )
    }

    // Blocking. Called off the main thread.
    fun execute(name: String, argumentsJson: String): String {

        val args = try {
            JSONObject(argumentsJson)
        } catch (e: Exception) {
            JSONObject()
        }

        return when (name) {
            "get_standings" -> {
                val league = findLeague(args.optString("league"))
                    ?: return "Unknown league. Use one of: ${leagueList()}"
                val tables = EspnApi.fetchStandings(league)
                formatStandings(league, tables, args.optString("group"))
            }

            "get_rankings" -> {
                val league = findLeague(args.optString("league"))
                    ?: return "Unknown league. Use one of: ${leagueList()}"
                formatRankings(league, RankingsRepository.getPolls(league))
            }

            "get_game_details" -> {
                val gameId = args.optString("game_id")
                val league = LeagueRegistry.byKey(gameId.substringBefore("_"))
                    ?: return "Unknown game id."
                val eventId = gameId.substringAfter("_", "")
                if (eventId.isEmpty()) return "Unknown game id."

                val game = GameStore.games.firstOrNull { "${it.leagueKey}_${it.id}" == gameId }
                formatDetail(game, EspnApi.fetchGameDetail(league, eventId))
            }

            else -> "Unknown tool: $name"
        }
    }

    private fun findLeague(text: String): LeagueInfo? =
        LeagueRegistry.leagues.firstOrNull {
            it.key.equals(text.trim(), ignoreCase = true) ||
                it.displayName.equals(text.trim(), ignoreCase = true)
        }

    private fun cap(text: String): String =
        if (text.length <= MAX_CHARS) text else text.take(MAX_CHARS) + "\n(shortened)"

    fun formatStandings(
        league: LeagueInfo,
        tables: List<StandingsTable>,
        group: String
    ): String {

        if (tables.isEmpty()) return "No standings are available for ${league.displayName} right now."

        val wanted = group.trim()

        var shown = if (wanted.isEmpty()) tables else tables.filter {
            it.name.contains(wanted, ignoreCase = true)
        }

        if (shown.isEmpty()) {
            return "No group matching '$wanted'. Groups: " + tables.joinToString { it.name }
        }

        var note = ""
        if (wanted.isEmpty() && shown.size > 6) {
            note = "\n(${shown.size - 6} more groups: " +
                shown.drop(6).joinToString { it.name } +
                ". Ask again with the group parameter.)"
            shown = shown.take(6)
        }

        val out = StringBuilder("${league.displayName} standings\n")

        for (table in shown) {
            out.append("\n${table.name}\n")
            out.append("Team: ${table.columns.joinToString(" ")}\n")

            for (row in table.rows.take(20)) {
                out.append("${row.position}. ${row.teamName}: ${row.values.joinToString(" ")}\n")
            }
        }

        return cap(out.toString() + note)
    }

    fun formatRankings(league: LeagueInfo, polls: List<RankingPoll>): String {

        val poll = polls.firstOrNull()
            ?: return "No rankings have been published yet for ${league.displayName}."

        val title = if (poll.week.isNotEmpty()) "${poll.name} (${poll.week})" else poll.name
        val out = StringBuilder("$title\n")

        for (team in poll.ranks.take(25)) {
            out.append("${team.rank}. ${team.name}")
            if (team.record.isNotEmpty()) out.append(" (${team.record})")
            if (team.points.isNotEmpty()) out.append(", ${team.points} points")
            out.append("\n")
        }
        return cap(out.toString())
    }

    fun formatDetail(game: EspnGame?, detail: EspnGameDetail): String {

        val away = game?.away?.name?.ifEmpty { game.away.abbreviation } ?: "Away"
        val home = game?.home?.name?.ifEmpty { game.home.abbreviation } ?: "Home"

        val state = when (detail.state) {
            "in" -> "in progress"
            "post" -> "final"
            else -> "not started"
        }

        val out = StringBuilder("$away at $home ($state, ${detail.statusText})\n")

        if (detail.state != "pre") {
            out.append("Score: $away ${detail.awayScore}, $home ${detail.homeScore}\n")
        }

        val lineScore = detail.lineScore
        if (lineScore != null && detail.state != "pre") {
            out.append("By period: $away ${lineScore.away.joinToString(" ")}; ")
            out.append("$home ${lineScore.home.joinToString(" ")}\n")
        }

        if (detail.stats.isNotEmpty()) {
            out.append("Team stats ($away / $home):\n")
            for (row in detail.stats) {
                out.append("${row.label}: ${row.away} / ${row.home}\n")
            }
        }

        for (team in detail.leaders) {
            if (team.lines.isEmpty()) continue
            out.append("Leaders ${team.abbreviation}: ")
            out.append(team.lines.joinToString("; ") { "${it.category} ${it.athlete} ${it.value}" })
            out.append("\n")
        }

        return cap(out.toString())
    }
}
