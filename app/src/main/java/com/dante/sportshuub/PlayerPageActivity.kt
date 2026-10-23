package com.dante.sportshuub

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.FragmentActivity

// A player's page: this season, season-by-season and career stats, game log.
class PlayerPageActivity : FragmentActivity() {

    companion object {
        private const val EXTRA_LEAGUE = "leagueKey"
        private const val EXTRA_PLAYER_ID = "playerId"
        private const val EXTRA_NAME = "name"
        private const val EXTRA_SUBTITLE = "subtitle"
        private const val EXTRA_HEADSHOT = "headshot"

        fun intent(
            context: Context,
            leagueKey: String,
            playerId: String,
            name: String,
            subtitle: String,
            headshotUrl: String
        ): Intent =
            Intent(context, PlayerPageActivity::class.java)
                .putExtra(EXTRA_LEAGUE, leagueKey)
                .putExtra(EXTRA_PLAYER_ID, playerId)
                .putExtra(EXTRA_NAME, name)
                .putExtra(EXTRA_SUBTITLE, subtitle)
                .putExtra(EXTRA_HEADSHOT, headshotUrl)
    }

    private lateinit var page: PageScaffold
    private lateinit var league: LeagueInfo
    private var playerId = ""

    private var season: SimpleTable? = null
    private var seasonLoaded = false
    private var yearly: List<SimpleTable>? = null
    private var gameLog: SimpleTable? = null
    private var gameLogLoaded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val found = LeagueRegistry.byKey(intent.getStringExtra(EXTRA_LEAGUE) ?: "")
        if (found == null) {
            finish()
            return
        }

        league = found
        playerId = intent.getStringExtra(EXTRA_PLAYER_ID) ?: ""

        page = PageScaffold(this)
        page.build(
            title = intent.getStringExtra(EXTRA_NAME) ?: "Player",
            subtitle = intent.getStringExtra(EXTRA_SUBTITLE) ?: league.displayName,
            tabs = listOf("THIS SEASON", "YEARLY & CAREER", "GAME LOG"),
            onTab = { showTab(it) }
        )

        setContentView(page.root)

        // Use the headshot we were given, or ESPN's standard address for it.
        val given = intent.getStringExtra(EXTRA_HEADSHOT).orEmpty()
        val headshot = given.ifEmpty {
            "https://a.espncdn.com/i/headshots/${league.espnLeague}/players/full/$playerId.png"
        }
        page.loadImage(headshot, page.logoView)

        showTab(0)
    }

    private fun showTab(index: Int) {
        when (index) {
            0 -> showSeason()
            1 -> showYearly()
            else -> showGameLog()
        }
    }

    private fun unsupportedNote(what: String): String =
        if (league.sport == "Soccer") {
            "$what aren't available for soccer players from this data source."
        } else {
            "$what aren't available for this player right now."
        }

    // ---------------------------------------------------------
    // This season
    // ---------------------------------------------------------

    private fun showSeason() {

        if (seasonLoaded) {
            renderSeason()
            return
        }

        page.showMessage("Loading stats…")

        Thread {
            val result = runCatching { EspnPages.fetchPlayerOverview(league, playerId) }
            result.onFailure { Log.e("PlayerPage", "Overview failed: ${it.message}") }

            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                season = result.getOrNull()
                seasonLoaded = true
                if (page.currentTab == 0) renderSeason()
            }
        }.start()
    }

    private fun renderSeason() {
        val table = season
        if (table == null) {
            page.showMessage(unsupportedNote("Season stats"))
        } else {
            page.showTables(listOf(table))
        }
    }

    // ---------------------------------------------------------
    // Yearly and career
    // ---------------------------------------------------------

    private fun showYearly() {

        val cached = yearly
        if (cached != null) {
            renderYearly(cached)
            return
        }

        page.showMessage("Loading stats…")

        Thread {
            val result = runCatching { EspnPages.fetchPlayerStats(league, playerId) }
            result.onFailure { Log.e("PlayerPage", "Stats failed: ${it.message}") }

            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                yearly = result.getOrNull() ?: emptyList()
                if (page.currentTab == 1) renderYearly(yearly ?: emptyList())
            }
        }.start()
    }

    private fun renderYearly(tables: List<SimpleTable>) {
        if (tables.isEmpty()) {
            page.showMessage(unsupportedNote("Season-by-season and career stats"))
        } else {
            page.showTables(tables)
        }
    }

    // ---------------------------------------------------------
    // Game log
    // ---------------------------------------------------------

    private fun showGameLog() {

        if (gameLogLoaded) {
            renderGameLog()
            return
        }

        page.showMessage("Loading game log…")

        Thread {
            val result = runCatching { EspnPages.fetchPlayerGameLog(league, playerId) }
            result.onFailure { Log.e("PlayerPage", "Game log failed: ${it.message}") }

            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                gameLog = result.getOrNull()
                gameLogLoaded = true
                if (page.currentTab == 2) renderGameLog()
            }
        }.start()
    }

    private fun renderGameLog() {
        val table = gameLog
        if (table == null) {
            page.showMessage(unsupportedNote("Game logs"))
        } else {
            page.showTables(listOf(table))
        }
    }
}
