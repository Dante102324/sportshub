package com.dante.sportshuub

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.addCallback
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.FragmentActivity

class FavoriteTeamsActivity : FragmentActivity() {

    private lateinit var titleText: TextView
    private lateinit var filterBox: EditText
    private lateinit var body: LinearLayout

    private val selectedTeams = mutableSetOf<String>()
    private val teamCache = mutableMapOf<String, List<EspnTeamInfo>>()
    private var currentLeague: LeagueInfo? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        selectedTeams.addAll(FavoritesStore.getFavoriteTeams(this))

        setContentView(createScreen())
        showLeagueList()

        // Back goes from the team list to the competition list, then closes.
        onBackPressedDispatcher.addCallback(this) {
            if (currentLeague != null) {
                showLeagueList()
            } else {
                finish()
            }
        }
    }

    private fun createScreen(): View {

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(TvUi.backgroundColor)
            setPadding(dp(28), dp(24), dp(28), dp(24))
        }

        titleText = TextView(this).apply {
            textSize = 30f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, dp(12))
        }
        root.addView(titleText)

        filterBox = EditText(this).apply {
            hint = "Search teams"
            setHintTextColor(TvUi.gray)
            setTextColor(Color.WHITE)
            textSize = 17f
            setSingleLine()
            inputType = InputType.TYPE_CLASS_TEXT
            setPadding(dp(16), 0, dp(16), 0)
            isFocusable = true
            isFocusableInTouchMode = true
            background = GradientDrawable().apply {
                setColor(TvUi.cardColor)
                cornerRadius = dp(9).toFloat()
            }
        }
        filterBox.doAfterTextChanged { renderCurrent() }

        root.addView(
            filterBox,
            LinearLayout.LayoutParams(-1, dp(54)).apply {
                bottomMargin = dp(10)
            }
        )

        body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val scroll = ScrollView(this)
        scroll.addView(body)

        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        root.addView(TvUi.row(this, "Done") { finish() })

        return root
    }

    // ---------------------------------------------------------
    // Step 1: choose a competition
    // ---------------------------------------------------------

    private fun showLeagueList() {

        currentLeague = null

        titleText.text = "FAVORITE TEAMS"
        filterBox.visibility = View.GONE

        body.removeAllViews()
        body.addView(note("Choose a competition, then pick your teams."))

        for (league in LeagueRegistry.leagues) {
            body.addView(
                TvUi.row(this, "${league.displayName}  ›") {
                    showTeams(league)
                }
            )
        }
    }

    // ---------------------------------------------------------
    // Step 2: pick teams in that competition
    // ---------------------------------------------------------

    private fun showTeams(league: LeagueInfo) {

        currentLeague = league

        titleText.text = league.displayName.uppercase()
        filterBox.setText("")
        filterBox.visibility = View.VISIBLE

        val cached = teamCache[league.key]
        if (cached != null) {
            renderCurrent()
            return
        }

        body.removeAllViews()
        body.addView(note("Loading teams…"))

        Thread {
            val teams = try {
                EspnApi.fetchTeams(league)
            } catch (e: Exception) {
                Log.e("FavoriteTeams", "${league.key} teams failed: ${e.message}")
                null
            }

            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                // The user may have gone back while this was loading.
                if (currentLeague?.key != league.key) return@runOnUiThread

                if (teams == null) {
                    body.removeAllViews()
                    body.addView(note("Couldn't load teams. Check your connection and try again."))
                } else {
                    teamCache[league.key] = teams
                    renderCurrent()
                }
            }
        }.start()
    }

    private fun renderCurrent() {

        val league = currentLeague ?: return
        val teams = teamCache[league.key] ?: return

        val query = filterBox.text.toString().trim().lowercase()

        val shown = if (query.isEmpty()) {
            teams
        } else {
            teams.filter {
                it.name.lowercase().contains(query) ||
                    it.abbreviation.lowercase() == query
            }
        }

        body.removeAllViews()

        if (shown.isEmpty()) {
            body.addView(
                note(
                    if (teams.isEmpty()) "No teams available for this competition."
                    else "No teams match your search."
                )
            )
            return
        }

        for (team in shown) {
            body.addView(createTeamRow(league, team))
        }
    }

    private fun createTeamRow(league: LeagueInfo, team: EspnTeamInfo): TextView {

        val key = FavoritesStore.teamKey(league.key, team.espnId)

        fun label() =
            (if (key in selectedTeams) "☑  " else "☐  ") + team.name

        return TvUi.row(this, label()) { row ->
            if (key in selectedTeams) {
                selectedTeams.remove(key)
            } else {
                selectedTeams.add(key)
            }
            FavoritesStore.setFavoriteTeams(this, selectedTeams)
            row.text = label()
        }
    }

    private fun note(text: String): TextView =
        TextView(this).apply {
            this.text = text
            textSize = 16f
            setTextColor(TvUi.gray)
            setPadding(0, dp(4), 0, dp(14))
        }

    private fun dp(value: Int): Int = TvUi.dp(this, value)
}
