package com.dante.sportshuub

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.FragmentActivity

// A team's page: schedule, roster and season stats.
class TeamPageActivity : FragmentActivity() {

    companion object {
        const val EXTRA_LEAGUE = "leagueKey"
        const val EXTRA_TEAM_ID = "teamId"
        const val EXTRA_NAME = "name"
        const val EXTRA_ABBREVIATION = "abbreviation"
        const val EXTRA_LOGO = "logoUrl"

        fun intent(
            context: android.content.Context,
            leagueKey: String,
            teamId: String,
            name: String,
            abbreviation: String,
            logoUrl: String
        ): Intent =
            Intent(context, TeamPageActivity::class.java)
                .putExtra(EXTRA_LEAGUE, leagueKey)
                .putExtra(EXTRA_TEAM_ID, teamId)
                .putExtra(EXTRA_NAME, name)
                .putExtra(EXTRA_ABBREVIATION, abbreviation)
                .putExtra(EXTRA_LOGO, logoUrl)
    }

    private lateinit var page: PageScaffold
    private lateinit var league: LeagueInfo

    private var teamId = ""
    private var teamName = ""
    private var abbreviation = ""
    private var logoUrl = ""

    private var schedule: List<ScheduleItem>? = null
    private var roster: List<RosterPlayer>? = null
    private var standing: TeamStanding? = null
    private var standingLoaded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val found = LeagueRegistry.byKey(intent.getStringExtra(EXTRA_LEAGUE) ?: "")
        if (found == null) {
            finish()
            return
        }

        league = found
        teamId = intent.getStringExtra(EXTRA_TEAM_ID) ?: ""
        teamName = intent.getStringExtra(EXTRA_NAME) ?: ""
        abbreviation = intent.getStringExtra(EXTRA_ABBREVIATION) ?: ""
        logoUrl = intent.getStringExtra(EXTRA_LOGO) ?: ""

        page = PageScaffold(this)
        page.build(
            title = teamName.ifEmpty { "Team" },
            subtitle = league.displayName,
            tabs = listOf("SCHEDULE", "ROSTER", "STATS"),
            onTab = { showTab(it) }
        )

        setContentView(page.root)

        page.loadImage(logoUrl, page.logoView)

        loadHeader()
        showTab(0)
    }

    // Record and standing in the subtitle.
    private fun loadHeader() {
        Thread {
            val info = try {
                EspnPages.fetchTeamInfo(league, teamId)
            } catch (e: Exception) {
                Log.e("TeamPage", "Team info failed: ${e.message}")
                null
            }

            runOnUiThread {
                if (isFinishing || isDestroyed || info == null) return@runOnUiThread

                if (info.name.isNotEmpty()) {
                    teamName = info.name
                    page.titleView.text = info.name
                }
                if (info.abbreviation.isNotEmpty()) abbreviation = info.abbreviation
                if (info.logoUrl.isNotEmpty() && logoUrl.isEmpty()) {
                    logoUrl = info.logoUrl
                    page.loadImage(logoUrl, page.logoView)
                }

                val parts = listOf(league.displayName, info.record, info.standingSummary)
                    .filter { it.isNotEmpty() }
                page.subtitleView.text = parts.joinToString("  ·  ")
            }
        }.start()
    }

    private fun showTab(index: Int) {
        when (index) {
            0 -> showSchedule()
            1 -> showRoster()
            else -> showStats()
        }
    }

    // ---------------------------------------------------------
    // Schedule
    // ---------------------------------------------------------

    private fun showSchedule() {

        val cached = schedule
        if (cached != null) {
            renderSchedule(cached)
            return
        }

        page.showMessage("Loading schedule…")

        Thread {
            val result = runCatching { EspnPages.fetchSchedule(league, teamId) }
            result.onFailure { Log.e("TeamPage", "Schedule failed: ${it.message}") }

            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                result.onSuccess { schedule = it }
                if (page.currentTab != 0) return@runOnUiThread

                val data = schedule
                if (data == null) {
                    page.showMessage("Couldn't load the schedule right now.")
                } else {
                    renderSchedule(data)
                }
            }
        }.start()
    }

    private fun renderSchedule(items: List<ScheduleItem>) {

        if (items.isEmpty()) {
            page.showMessage("No schedule is available for this team right now.")
            return
        }

        val rows = items.map { item ->
            val where = when (item.home) {
                true -> "vs"
                false -> "@"
                null -> "vs"
            }

            val result = when (item.state) {
                "post" -> {
                    val letter = when (item.won) {
                        true -> "W"
                        false -> "L"
                        null -> ""
                    }
                    "$letter ${item.teamScore}-${item.opponentScore}".trim()
                }
                "in" -> "LIVE ${item.teamScore}-${item.opponentScore}"
                else -> "Upcoming"
            }

            SimpleRow(
                label = TimeFormat.dateTime(item.dateIso),
                values = listOf("$where ${item.opponentAbbreviation}", result),
                id = item.eventId
            )
        }

        val views = page.showTables(
            listOf(SimpleTable("", listOf("OPPONENT", "RESULT"), rows)),
            onRow = { row ->
                items.firstOrNull { it.eventId == row.id }?.let { openGame(it) }
            }
        )

        // Jump to the next game that hasn't been played.
        val next = items.indexOfFirst { it.state != "post" }
        if (next >= 0) {
            views.getOrNull(next)?.let { target ->
                target.post { target.requestFocus() }
            }
        }
    }

    private fun openGame(item: ScheduleItem) {

        val weAreHome = item.home != false

        val intent = Intent(this, GameDetailsActivity::class.java)

        intent.putExtra("id", "${league.key}_${item.eventId}")
        intent.putExtra("sport", league.sport)
        intent.putExtra("league", league.displayName)
        intent.putExtra("leagueKey", league.key)
        intent.putExtra("state", item.state)
        intent.putExtra("status", item.statusText)
        intent.putExtra("startTime", item.dateIso)

        val shownScoreUs = if (item.state != "pre") item.teamScore else ""
        val shownScoreThem = if (item.state != "pre") item.opponentScore else ""

        if (weAreHome) {
            intent.putExtra("homeTeam", abbreviation)
            intent.putExtra("homeLogo", logoUrl)
            intent.putExtra("homeScore", shownScoreUs)
            intent.putExtra("homeId", teamId)
            intent.putExtra("awayTeam", item.opponentAbbreviation)
            intent.putExtra("awayLogo", item.opponentLogo)
            intent.putExtra("awayScore", shownScoreThem)
        } else {
            intent.putExtra("awayTeam", abbreviation)
            intent.putExtra("awayLogo", logoUrl)
            intent.putExtra("awayScore", shownScoreUs)
            intent.putExtra("awayId", teamId)
            intent.putExtra("homeTeam", item.opponentAbbreviation)
            intent.putExtra("homeLogo", item.opponentLogo)
            intent.putExtra("homeScore", shownScoreThem)
        }

        startActivity(intent)
    }

    // ---------------------------------------------------------
    // Roster
    // ---------------------------------------------------------

    private fun showRoster() {

        val cached = roster
        if (cached != null) {
            renderRoster(cached)
            return
        }

        page.showMessage("Loading roster…")

        Thread {
            val result = runCatching { EspnPages.fetchRoster(league, teamId) }
            result.onFailure { Log.e("TeamPage", "Roster failed: ${it.message}") }

            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                result.onSuccess { roster = it }
                if (page.currentTab != 1) return@runOnUiThread

                val data = roster
                if (data == null) {
                    page.showMessage("Couldn't load the roster right now.")
                } else {
                    renderRoster(data)
                }
            }
        }.start()
    }

    private fun renderRoster(players: List<RosterPlayer>) {

        if (players.isEmpty()) {
            page.showMessage("No roster is available for this team right now.")
            return
        }

        val rows = players.map { p ->
            val label = if (p.jersey.isNotEmpty()) "#${p.jersey}  ${p.name}" else p.name
            SimpleRow(label, listOf(p.position, p.age, p.height, p.weight), p.id)
        }

        page.showTables(
            listOf(SimpleTable("", listOf("POS", "AGE", "HT", "WT"), rows)),
            onRow = { row ->
                players.firstOrNull { it.id == row.id }?.let { p ->
                    startActivity(
                        PlayerPageActivity.intent(
                            this,
                            league.key,
                            p.id,
                            p.name,
                            listOf(p.position, teamName).filter { it.isNotEmpty() }.joinToString("  ·  "),
                            p.headshotUrl
                        )
                    )
                }
            }
        )
    }

    // ---------------------------------------------------------
    // Stats
    // ---------------------------------------------------------

    private fun showStats() {

        if (standingLoaded) {
            renderStats(standing)
            return
        }

        page.showMessage("Loading stats…")

        Thread {
            val result = runCatching { EspnPages.fetchTeamStanding(league, teamId) }
            result.onFailure { Log.e("TeamPage", "Stats failed: ${it.message}") }

            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                standing = result.getOrNull()
                standingLoaded = true
                if (page.currentTab != 2) return@runOnUiThread
                renderStats(standing)
            }
        }.start()
    }

    private fun renderStats(data: TeamStanding?) {

        if (data == null || data.stats.isEmpty()) {
            page.showMessage("Season stats aren't available for this team right now.")
            return
        }

        val title = if (data.group.isNotEmpty()) "Season stats  ·  ${data.group}" else "Season stats"

        page.showTables(
            listOf(
                SimpleTable(
                    title,
                    listOf("VALUE"),
                    data.stats.map { (label, value) -> SimpleRow(label, listOf(value)) }
                )
            )
        )
    }
}
