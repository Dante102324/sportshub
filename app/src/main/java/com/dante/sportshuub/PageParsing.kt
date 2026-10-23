package com.dante.sportshuub

import org.json.JSONArray
import org.json.JSONObject

// Turns ESPN's JSON into the small models the pages show. Free of Android
// classes, and every field is optional so a missing piece never crashes a page.
object PageParsing {

    private fun strings(array: JSONArray?): List<String> {
        if (array == null) return emptyList()
        return (0 until array.length()).map { array.optString(it) }
    }

    private fun firstLogo(team: JSONObject?): String =
        team?.optJSONArray("logos")?.optJSONObject(0)?.optString("href")
            ?: team?.optString("logo").orEmpty()

    // ---------------------------------------------------------
    // Search
    // ---------------------------------------------------------

    private val idInLink = Regex("/id/(\\d+)")

    fun parseSearch(json: JSONObject): List<SearchResult> {

        val groups = json.optJSONArray("results") ?: return emptyList()
        val out = mutableListOf<SearchResult>()

        for (g in 0 until groups.length()) {
            val group = groups.optJSONObject(g) ?: continue
            val contents = group.optJSONArray("contents") ?: continue

            for (i in 0 until contents.length()) {
                val item = contents.optJSONObject(i) ?: continue

                val type = item.optString("type").ifEmpty { group.optString("type") }
                if (type != "player") continue

                val sport = item.optString("sport")
                val slug = item.optString("defaultLeagueSlug")

                // Only players from competitions the app covers.
                val league = LeagueRegistry.leagues.firstOrNull {
                    it.espnSport == sport && it.espnLeague == slug
                } ?: continue

                var id = item.optString("uid").substringAfter("~a:", "").substringBefore("~")
                if (id.isEmpty()) {
                    val link = item.optJSONObject("link")?.optString("web").orEmpty()
                    id = idInLink.find(link)?.groupValues?.get(1).orEmpty()
                }
                if (id.isEmpty()) continue

                val name = item.optString("displayName")
                if (name.isEmpty()) continue

                val team = item.optString("description")
                val subtitle = if (team.isNotEmpty()) "$team  ·  ${league.displayName}" else league.displayName

                out.add(
                    SearchResult(
                        kind = "player",
                        leagueKey = league.key,
                        id = id,
                        title = name,
                        subtitle = subtitle,
                        logoUrl = item.optJSONObject("image")?.optString("default").orEmpty()
                    )
                )
            }
        }
        return out.distinctBy { it.leagueKey + it.id }
    }

    // ---------------------------------------------------------
    // Team
    // ---------------------------------------------------------

    fun parseTeamInfo(json: JSONObject): TeamInfo? {
        val team = json.optJSONObject("team") ?: return null

        val record = team.optJSONObject("record")
            ?.optJSONArray("items")
            ?.optJSONObject(0)
            ?.optString("summary").orEmpty()

        return TeamInfo(
            name = team.optString("displayName"),
            abbreviation = team.optString("abbreviation"),
            logoUrl = firstLogo(team),
            record = record,
            standingSummary = team.optString("standingSummary")
        )
    }

    private fun scoreOf(competitor: JSONObject?): String {
        if (competitor == null) return ""
        val asObject = competitor.optJSONObject("score")
        return asObject?.optString("displayValue") ?: competitor.optString("score")
    }

    fun parseSchedule(json: JSONObject, teamId: String): List<ScheduleItem> {

        val events = json.optJSONArray("events") ?: return emptyList()
        val out = mutableListOf<ScheduleItem>()

        for (i in 0 until events.length()) {
            val event = events.optJSONObject(i) ?: continue
            val competition = event.optJSONArray("competitions")?.optJSONObject(0) ?: continue
            val competitors = competition.optJSONArray("competitors") ?: continue

            var us: JSONObject? = null
            var them: JSONObject? = null

            for (j in 0 until competitors.length()) {
                val c = competitors.optJSONObject(j) ?: continue
                val id = c.optJSONObject("team")?.optString("id").orEmpty()
                    .ifEmpty { c.optString("id") }
                if (id == teamId) us = c else them = c
            }
            if (us == null || them == null) continue

            val statusType = competition.optJSONObject("status")?.optJSONObject("type")
            val state = statusType?.optString("state") ?: "pre"
            val theirTeam = them.optJSONObject("team")

            val home = when (us.optString("homeAway")) {
                "home" -> true
                "away" -> false
                else -> null
            }

            val won = if (state == "post" && us.has("winner")) us.optBoolean("winner") else null

            out.add(
                ScheduleItem(
                    eventId = event.optString("id"),
                    dateIso = event.optString("date"),
                    opponentName = theirTeam?.optString("displayName").orEmpty(),
                    opponentAbbreviation = theirTeam?.optString("abbreviation").orEmpty(),
                    opponentLogo = firstLogo(theirTeam),
                    home = home,
                    state = state,
                    statusText = statusType?.optString("shortDetail").orEmpty(),
                    teamScore = scoreOf(us),
                    opponentScore = scoreOf(them),
                    won = won
                )
            )
        }
        return out
    }

    // The roster is either a flat list of players or groups with "items".
    fun parseRoster(json: JSONObject): List<RosterPlayer> {

        val athletes = json.optJSONArray("athletes") ?: return emptyList()
        val players = mutableListOf<JSONObject>()

        for (i in 0 until athletes.length()) {
            val entry = athletes.optJSONObject(i) ?: continue
            val items = entry.optJSONArray("items")

            if (items != null) {
                for (j in 0 until items.length()) {
                    items.optJSONObject(j)?.let { players.add(it) }
                }
            } else {
                players.add(entry)
            }
        }

        return players.mapNotNull { p ->
            val id = p.optString("id")
            val name = p.optString("displayName").ifEmpty { p.optString("fullName") }
            if (id.isEmpty() || name.isEmpty()) return@mapNotNull null

            RosterPlayer(
                id = id,
                name = name,
                jersey = p.optString("jersey"),
                position = p.optJSONObject("position")?.optString("abbreviation").orEmpty(),
                age = p.optString("age"),
                height = p.optString("displayHeight").ifEmpty { p.optString("height") },
                weight = p.optString("displayWeight").ifEmpty { p.optString("weight") },
                headshotUrl = p.optJSONObject("headshot")?.optString("href").orEmpty()
            )
        }
    }

    // A team's row in the league standings, with every stat ESPN lists.
    fun parseTeamStanding(json: JSONObject, teamId: String): TeamStanding? {

        val leaves = mutableListOf<JSONObject>()
        collectLeaves(json, leaves)

        for (node in leaves) {
            val entries = node.optJSONObject("standings")?.optJSONArray("entries") ?: continue

            for (i in 0 until entries.length()) {
                val entry = entries.optJSONObject(i) ?: continue
                if (entry.optJSONObject("team")?.optString("id") != teamId) continue

                val stats = mutableListOf<Pair<String, String>>()
                val array = entry.optJSONArray("stats")

                if (array != null) {
                    for (j in 0 until array.length()) {
                        val stat = array.optJSONObject(j) ?: continue
                        val value = stat.optString("displayValue")
                        if (value.isEmpty()) continue

                        val label = stat.optString("displayName")
                            .ifEmpty { stat.optString("shortDisplayName") }
                            .ifEmpty { stat.optString("name") }

                        stats.add(label to value)
                    }
                }
                return TeamStanding(node.optString("name"), stats)
            }
        }
        return null
    }

    private fun collectLeaves(node: JSONObject, out: MutableList<JSONObject>) {
        val children = node.optJSONArray("children")
        if (children != null && children.length() > 0) {
            for (i in 0 until children.length()) {
                children.optJSONObject(i)?.let { collectLeaves(it, out) }
            }
            return
        }
        out.add(node)
    }

    // ---------------------------------------------------------
    // Player
    // ---------------------------------------------------------

    // "This season" snapshot from the overview.
    fun parseOverviewStats(json: JSONObject): SimpleTable? {
        val stats = json.optJSONObject("statistics") ?: return null
        val labels = strings(stats.optJSONArray("labels"))
        val values = strings(stats.optJSONArray("displayValues"))
        if (labels.isEmpty() || values.isEmpty()) return null

        return SimpleTable("This season", labels, listOf(SimpleRow("Season", values)))
    }

    // Season-by-season rows (when ESPN sends them) plus career totals.
    fun parsePlayerStats(json: JSONObject): List<SimpleTable> {

        val categories = json.optJSONArray("categories") ?: return emptyList()
        val tables = mutableListOf<SimpleTable>()

        for (i in 0 until categories.length()) {
            val category = categories.optJSONObject(i) ?: continue

            val labels = strings(category.optJSONArray("labels"))
            if (labels.isEmpty()) continue

            val rows = mutableListOf<SimpleRow>()
            val seasons = category.optJSONArray("statistics")

            if (seasons != null) {
                for (j in 0 until seasons.length()) {
                    val season = seasons.optJSONObject(j) ?: continue
                    val stats = strings(season.optJSONArray("stats"))
                    if (stats.isEmpty()) continue

                    val name = season.optJSONObject("season")?.optString("displayName")
                        ?: season.optString("season")

                    rows.add(SimpleRow(name.ifEmpty { "Season ${j + 1}" }, stats))
                }
            }

            val totals = strings(category.optJSONArray("totals"))
            if (totals.isNotEmpty()) {
                rows.add(SimpleRow(if (seasons != null) "Career" else "Totals", totals))
            }

            if (rows.isNotEmpty()) {
                tables.add(
                    SimpleTable(
                        category.optString("displayName").ifEmpty { category.optString("name") },
                        labels,
                        rows
                    )
                )
            }
        }
        return tables
    }

    // Newest games first. ESPN sends either an "events" list that carries the
    // stats, or an "events" lookup plus "seasonTypes" that carry the stats.
    fun parseGameLog(json: JSONObject): SimpleTable? {

        val labels = strings(json.optJSONArray("labels"))
        if (labels.isEmpty()) return null

        val info = mutableMapOf<String, JSONObject>()
        val eventsArray = json.optJSONArray("events")
        val eventsMap = json.optJSONObject("events")

        if (eventsArray != null) {
            for (i in 0 until eventsArray.length()) {
                val e = eventsArray.optJSONObject(i) ?: continue
                info[e.optString("id").ifEmpty { e.optString("eventId") }] = e
            }
        } else if (eventsMap != null) {
            val keys = eventsMap.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                eventsMap.optJSONObject(key)?.let { info[key] = it }
            }
        }

        // (event id, stats) in the order ESPN lists them
        val entries = mutableListOf<Pair<String, List<String>>>()

        val seasonTypes = json.optJSONArray("seasonTypes")
        if (seasonTypes != null) {
            for (s in 0 until seasonTypes.length()) {
                val cats = seasonTypes.optJSONObject(s)?.optJSONArray("categories") ?: continue
                for (c in 0 until cats.length()) {
                    val events = cats.optJSONObject(c)?.optJSONArray("events") ?: continue
                    for (e in 0 until events.length()) {
                        val ev = events.optJSONObject(e) ?: continue
                        entries.add(ev.optString("eventId") to strings(ev.optJSONArray("stats")))
                    }
                }
                if (entries.isNotEmpty()) break   // the first season type that has games
            }
        } else if (eventsArray != null) {
            for (i in 0 until eventsArray.length()) {
                val e = eventsArray.optJSONObject(i) ?: continue
                val stats = strings(e.optJSONArray("stats"))
                if (stats.isNotEmpty()) {
                    entries.add(e.optString("id").ifEmpty { e.optString("eventId") } to stats)
                }
            }
        }

        if (entries.isEmpty()) return null

        val rows = entries.reversed().take(30).map { (eventId, stats) ->
            val event = info[eventId]

            val date = event?.optString("gameDate").orEmpty().ifEmpty { event?.optString("date").orEmpty() }
            val opponent = event?.optJSONObject("opponent")?.optString("abbreviation").orEmpty()
            val atVs = event?.optString("atVs").orEmpty().ifEmpty { "vs" }
            val result = event?.optString("gameResult").orEmpty()
            val score = event?.optString("score").orEmpty()

            val label = listOf(
                if (date.isNotEmpty()) TimeFormat.shortDateUtc(date) else "",
                if (opponent.isNotEmpty()) "$atVs $opponent" else ""
            ).filter { it.isNotEmpty() }.joinToString("  ").ifEmpty { "Game" }

            val outcome = listOf(result, score).filter { it.isNotEmpty() }.joinToString(" ")

            // Keep the stats aligned with the column labels.
            val aligned = labels.indices.map { stats.getOrNull(it) ?: "" }

            SimpleRow(label, listOf(outcome) + aligned, eventId)
        }

        return SimpleTable("Game log", listOf("RESULT") + labels, rows)
    }
}
