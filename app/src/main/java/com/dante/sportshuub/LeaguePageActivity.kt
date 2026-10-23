package com.dante.sportshuub

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.FragmentActivity

// A competition's page: standings (and rankings for college), and its teams.
class LeaguePageActivity : FragmentActivity() {

    companion object {
        const val EXTRA_LEAGUE = "leagueKey"
    }

    private lateinit var page: PageScaffold
    private lateinit var league: LeagueInfo

    private var teams: List<EspnTeamInfo>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val found = LeagueRegistry.byKey(intent.getStringExtra(EXTRA_LEAGUE) ?: "")
        if (found == null) {
            finish()
            return
        }

        league = found

        page = PageScaffold(this)
        page.build(
            title = league.displayName,
            subtitle = league.sport,
            tabs = listOf("STANDINGS", "TEAMS"),
            onTab = { showTab(it) }
        )

        setContentView(page.root)

        showTab(0)
    }

    private fun showTab(index: Int) {
        if (index == 0) {
            page.content.removeAllViews()
            StandingsPanel(this, league).show(page.content)
        } else {
            showTeams()
        }
    }

    private fun showTeams() {

        val cached = teams
        if (cached != null) {
            renderTeams(cached)
            return
        }

        page.showMessage("Loading teams…")

        Thread {
            val result = runCatching { EspnApi.fetchTeams(league) }
            result.onFailure { Log.e("LeaguePage", "Teams failed: ${it.message}") }

            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                result.onSuccess { teams = it }
                if (page.currentTab != 1) return@runOnUiThread

                val data = teams
                if (data == null) {
                    page.showMessage("Couldn't load the teams right now.")
                } else {
                    renderTeams(data)
                }
            }
        }.start()
    }

    private fun renderTeams(list: List<EspnTeamInfo>) {

        if (list.isEmpty()) {
            page.showMessage("No teams are available for ${league.displayName} right now.")
            return
        }

        page.showTables(
            listOf(
                SimpleTable(
                    "${list.size} teams",
                    emptyList(),
                    list.map { SimpleRow(it.name, emptyList(), it.espnId) }
                )
            ),
            onRow = { row ->
                list.firstOrNull { it.espnId == row.id }?.let { team ->
                    startActivity(
                        TeamPageActivity.intent(
                            this,
                            league.key,
                            team.espnId,
                            team.name,
                            team.abbreviation,
                            team.logoUrl
                        )
                    )
                }
            }
        )
    }
}
