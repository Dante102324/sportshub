package com.dante.sportshuub

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.FragmentActivity

// Search for a league, team or player and open its page.
class SearchActivity : FragmentActivity() {

    private lateinit var input: EditText
    private lateinit var status: TextView
    private lateinit var results: LinearLayout

    private var players: List<SearchResult> = emptyList()
    private var playerStatus = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(createScreen())

        // Teams for every competition are loaded once, in the background.
        TeamIndex.ensureLoading {
            runOnUiThread {
                if (!isFinishing && !isDestroyed) refresh()
            }
        }

        refresh()
    }

    private fun createScreen(): View {

        val root = LinearLayout(this)

        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(TvUi.backgroundColor)
        root.setPadding(dp(50), dp(30), dp(50), dp(24))

        // ---- Header ----

        val header = LinearLayout(this)
        header.orientation = LinearLayout.HORIZONTAL
        header.gravity = Gravity.CENTER_VERTICAL

        header.addView(
            chip("←  BACK") { finish() },
            LinearLayout.LayoutParams(-2, dp(48)).apply { setMargins(0, 0, dp(24), 0) }
        )

        val title = TextView(this)
        title.text = "SEARCH"
        title.textSize = 28f
        title.typeface = Typeface.DEFAULT_BOLD
        title.setTextColor(Color.WHITE)

        header.addView(title, LinearLayout.LayoutParams(0, -2, 1f))

        root.addView(header, LinearLayout.LayoutParams(-1, dp(60)))

        // ---- Search box ----

        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        row.setPadding(0, dp(10), 0, dp(6))

        input = EditText(this)
        input.hint = "Player, team or league"
        input.setHintTextColor(TvUi.gray)
        input.setTextColor(Color.WHITE)
        input.textSize = 18f
        input.setSingleLine()
        input.imeOptions = EditorInfo.IME_ACTION_SEARCH
        input.setPadding(dp(18), 0, dp(18), 0)
        input.isFocusable = true
        input.isFocusableInTouchMode = true
        input.background = GradientDrawable().apply {
            setColor(TvUi.cardColor)
            cornerRadius = dp(24).toFloat()
        }

        input.doAfterTextChanged {
            // A new query: forget the old player results until it is submitted.
            players = emptyList()
            playerStatus = ""
            refresh()
        }

        input.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
                searchPlayers()
                true
            } else {
                false
            }
        }

        row.addView(
            input,
            LinearLayout.LayoutParams(0, dp(52), 1f).apply { setMargins(0, 0, dp(12), 0) }
        )

        row.addView(
            chip("SEARCH") { searchPlayers() },
            LinearLayout.LayoutParams(-2, dp(52))
        )

        root.addView(row)

        // ---- Status + results ----

        status = TextView(this)
        status.textSize = 15f
        status.setTextColor(TvUi.gray)
        status.setPadding(dp(8), dp(4), 0, dp(8))

        root.addView(status)

        results = LinearLayout(this)
        results.orientation = LinearLayout.VERTICAL

        val scroll = ScrollView(this)
        scroll.isFocusable = false
        scroll.addView(results)

        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        return root
    }

    // Leagues and teams update as you type; players are looked up on Search.
    private fun refresh() {

        val query = input.text.toString().trim()

        results.removeAllViews()

        if (query.isEmpty()) {
            status.text =
                if (TeamIndex.isComplete) "Type a name, then press Search to include players."
                else "Loading teams… (${TeamIndex.loadedLeagues}/${TeamIndex.totalLeagues})"
            return
        }

        val q = query.lowercase()

        val leagues = LeagueRegistry.leagues.filter {
            it.displayName.lowercase().contains(q) || it.key.lowercase() == q
        }

        val teams = TeamIndex.search(query)

        for (league in leagues) {
            addRow("LEAGUE", league.displayName, league.sport) {
                startActivity(
                    android.content.Intent(this, LeaguePageActivity::class.java)
                        .putExtra(LeaguePageActivity.EXTRA_LEAGUE, league.key)
                )
            }
        }

        for (entry in teams) {
            addRow("TEAM", entry.team.name, entry.league.displayName) {
                startActivity(
                    TeamPageActivity.intent(
                        this,
                        entry.league.key,
                        entry.team.espnId,
                        entry.team.name,
                        entry.team.abbreviation,
                        entry.team.logoUrl
                    )
                )
            }
        }

        for (player in players) {
            addRow("PLAYER", player.title, player.subtitle) {
                startActivity(
                    PlayerPageActivity.intent(
                        this,
                        player.leagueKey,
                        player.id,
                        player.title,
                        player.subtitle,
                        player.logoUrl
                    )
                )
            }
        }

        val count = leagues.size + teams.size + players.size

        status.text = when {
            playerStatus.isNotEmpty() -> playerStatus
            count == 0 && !TeamIndex.isComplete ->
                "Loading teams… (${TeamIndex.loadedLeagues}/${TeamIndex.totalLeagues})"
            count == 0 -> "No leagues or teams match. Press Search to look for players."
            else -> "Press Search to include players."
        }
    }

    private fun searchPlayers() {

        val query = input.text.toString().trim()
        if (query.length < 2) return

        playerStatus = "Searching players…"
        refresh()

        Thread {
            val found = try {
                EspnPages.searchPlayers(query)
            } catch (e: Exception) {
                Log.e("Search", "Player search failed: ${e.message}")
                null
            }

            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                // Ignore results for a query that has since changed.
                if (input.text.toString().trim() != query) return@runOnUiThread

                if (found == null) {
                    players = emptyList()
                    playerStatus = "Couldn't search for players right now."
                } else {
                    players = found
                    playerStatus = if (found.isEmpty()) "No players found for \"$query\"." else ""
                }
                refresh()
            }
        }.start()
    }

    private fun addRow(kind: String, title: String, subtitle: String, onClick: () -> Unit) {

        val text = "$kind   ·   $title" + if (subtitle.isNotEmpty()) "   ($subtitle)" else ""

        results.addView(TvUi.row(this, text) { onClick() })
    }

    private fun chip(text: String, onClick: () -> Unit): TextView {

        val view = TextView(this)

        view.text = text
        view.textSize = 16f
        view.typeface = Typeface.DEFAULT_BOLD
        view.setTextColor(Color.WHITE)
        view.gravity = Gravity.CENTER
        view.setPadding(dp(22), 0, dp(22), 0)
        view.isFocusable = true
        view.isFocusableInTouchMode = true

        fun style(focused: Boolean) {
            view.background = GradientDrawable().apply {
                setColor(Color.rgb(38, 38, 43))
                cornerRadius = dp(26).toFloat()
                if (focused) setStroke(dp(3), Color.WHITE)
            }
        }

        style(false)
        view.setOnFocusChangeListener { _, hasFocus -> style(hasFocus) }
        view.setOnClickListener { onClick() }

        return view
    }

    private fun dp(value: Int): Int = TvUi.dp(this, value)
}
