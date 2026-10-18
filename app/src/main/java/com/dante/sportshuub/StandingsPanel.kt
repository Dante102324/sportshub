package com.dante.sportshuub

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import java.util.concurrent.ConcurrentHashMap

// The STANDINGS tab: pick a competition, then a conference/division or (for
// college football and basketball) the latest poll.
class StandingsPanel(
    private val activity: FragmentActivity,
    private val fixedLeague: LeagueInfo? = null
) {

    private class LeagueData(
        val loadedAt: Long,
        val tables: List<StandingsTable>,
        val polls: List<RankingPoll>
    )

    companion object {
        private const val TTL_MS = 5L * 60 * 1000
        private val cache = ConcurrentHashMap<String, LeagueData>()
        private val collegeKeys = setOf("CFB", "CBB")
    }

    private val prefs =
        activity.getSharedPreferences("sports_hub_standings", Context.MODE_PRIVATE)

    private val gray = Color.rgb(180, 180, 185)

    private var league: LeagueInfo =
        fixedLeague
            ?: LeagueRegistry.byKey(prefs.getString("league", "NBA") ?: "NBA")
            ?: LeagueRegistry.leagues.first()

    // "" = the default view for the competition
    private var viewKey = ""

    private var leagueButton: TextView? = null
    private var viewButton: TextView? = null
    private var tableBox: LinearLayout? = null

    // =====================================================
    // SCREEN
    // =====================================================

    fun show(parent: LinearLayout) {

        val controls = LinearLayout(activity)

        controls.orientation = LinearLayout.HORIZONTAL
        controls.setPadding(0, dp(4), 0, dp(10))

        val viewBtn = controlButton { pickView() }
        viewButton = viewBtn

        if (fixedLeague == null) {
            val leagueBtn = controlButton { pickLeague() }
            leagueButton = leagueBtn

            controls.addView(
                leagueBtn,
                LinearLayout.LayoutParams(-2, dp(48)).apply {
                    setMargins(0, 0, dp(12), 0)
                }
            )
        }

        controls.addView(
            viewBtn,
            LinearLayout.LayoutParams(-2, dp(48)).apply {
                setMargins(0, 0, dp(12), 0)
            }
        )

        if (fixedLeague == null) {
            controls.addView(
                controlButton { openLeaguePage() }.also { it.text = "LEAGUE PAGE  ›" },
                LinearLayout.LayoutParams(-2, dp(48))
            )
        }

        parent.addView(controls)

        val box = LinearLayout(activity)
        box.orientation = LinearLayout.VERTICAL
        tableBox = box

        parent.addView(box)

        updateLeagueButton()
        viewBtn.text = "VIEW  ▾"

        load()
    }

    private fun openLeaguePage() {
        activity.startActivity(
            Intent(activity, LeaguePageActivity::class.java)
                .putExtra(LeaguePageActivity.EXTRA_LEAGUE, league.key)
        )
    }

    private fun openTeam(teamId: String, name: String, abbreviation: String, logoUrl: String) {
        if (teamId.isEmpty()) return
        activity.startActivity(
            TeamPageActivity.intent(activity, league.key, teamId, name, abbreviation, logoUrl)
        )
    }

    private fun updateLeagueButton() {
        leagueButton?.text = "COMPETITION:  ${league.displayName}  ▾"
    }

    // =====================================================
    // LOADING
    // =====================================================

    private fun load() {

        val box = tableBox ?: return
        val target = league

        val cached = cache[target.key]
        if (cached != null && System.currentTimeMillis() - cached.loadedAt < TTL_MS) {
            render(cached)
            return
        }

        box.removeAllViews()
        box.addView(message("Loading…"))

        Thread {
            val tables = try {
                EspnApi.fetchStandings(target)
            } catch (e: Exception) {
                emptyList()
            }

            val polls =
                if (target.key in collegeKeys) RankingsRepository.getPolls(target)
                else emptyList()

            val data = LeagueData(System.currentTimeMillis(), tables, polls)

            if (tables.isNotEmpty() || polls.isNotEmpty()) {
                cache[target.key] = data
            }

            activity.runOnUiThread {
                if (activity.isFinishing || activity.isDestroyed) return@runOnUiThread
                // Ignore results for a competition or tab the user already left.
                if (target.key != league.key) return@runOnUiThread
                if (tableBox !== box) return@runOnUiThread

                render(data)
            }
        }.start()
    }

    // =====================================================
    // VIEW OPTIONS
    // =====================================================

    // key to label. Polls come first for the college leagues.
    private fun options(data: LeagueData): List<Pair<String, String>> {

        val out = mutableListOf<Pair<String, String>>()

        data.polls.forEachIndexed { i, poll ->
            out.add("poll:$i" to "Rankings — ${poll.name}")
        }

        if (data.tables.isNotEmpty()) {
            out.add("all" to "All groups")

            data.tables.forEachIndexed { i, table ->
                out.add("group:$i" to table.name.ifEmpty { "Group ${i + 1}" })
            }
        }
        return out
    }

    private fun currentKey(options: List<Pair<String, String>>): String {
        if (options.any { it.first == viewKey }) return viewKey
        return options.firstOrNull()?.first ?: ""
    }

    private fun pickLeague() {

        val leagues = LeagueRegistry.leagues
        val names = leagues.map { it.displayName }.toTypedArray()

        AlertDialog.Builder(activity)
            .setTitle("Competition")
            .setItems(names) { _, which ->
                league = leagues[which]
                viewKey = ""
                prefs.edit().putString("league", league.key).apply()
                updateLeagueButton()
                viewButton?.text = "VIEW  ▾"
                load()
            }
            .show()
    }

    private fun pickView() {

        val data = cache[league.key] ?: return
        val options = options(data)
        if (options.isEmpty()) return

        AlertDialog.Builder(activity)
            .setTitle("View")
            .setItems(options.map { it.second }.toTypedArray()) { _, which ->
                viewKey = options[which].first
                render(data)
            }
            .show()
    }

    // =====================================================
    // RENDERING
    // =====================================================

    private fun render(data: LeagueData) {

        val box = tableBox ?: return

        box.removeAllViews()

        val options = options(data)

        if (options.isEmpty()) {
            viewButton?.text = "VIEW  ▾"
            box.addView(
                message("No standings available for ${league.displayName} right now.")
            )
            return
        }

        val key = currentKey(options)
        val label = options.first { it.first == key }.second

        viewButton?.text = "VIEW:  $label  ▾"

        when {
            key.startsWith("poll:") -> {
                val index = key.removePrefix("poll:").toIntOrNull() ?: 0
                data.polls.getOrNull(index)?.let { addPoll(box, it) }
            }
            key == "all" -> {
                data.tables.forEach { addTable(box, it) }
            }
            key.startsWith("group:") -> {
                val index = key.removePrefix("group:").toIntOrNull() ?: 0
                data.tables.getOrNull(index)?.let { addTable(box, it) }
            }
        }
    }

    private fun addTable(box: LinearLayout, table: StandingsTable) {

        box.addView(title(table.name))

        box.addView(
            buildRow("#", "", "", "TEAM", table.columns, header = true)
        )

        for (row in table.rows) {
            box.addView(
                buildRow(
                    row.position.toString(),
                    row.logoUrl,
                    row.abbreviation,
                    row.teamName,
                    row.values,
                    header = false,
                    onClick = { openTeam(row.teamId, row.teamName, row.abbreviation, row.logoUrl) }
                )
            )
        }
    }

    private fun addPoll(box: LinearLayout, poll: RankingPoll) {

        val heading =
            if (poll.week.isNotEmpty()) "${poll.name} — ${poll.week}" else poll.name

        box.addView(title(heading))

        // Only show columns the poll actually has.
        val showPoints = poll.ranks.any { it.points.isNotEmpty() }
        val showPrevious = poll.ranks.any { it.previous.isNotEmpty() }

        val columns = mutableListOf("REC")
        if (showPoints) columns.add("PTS")
        if (showPrevious) columns.add("PREV")

        box.addView(buildRow("#", "", "", "TEAM", columns, header = true))

        for (team in poll.ranks) {

            val values = mutableListOf(team.record.ifEmpty { "-" })
            if (showPoints) values.add(team.points.ifEmpty { "-" })
            if (showPrevious) values.add(team.previous.ifEmpty { "NR" })

            box.addView(
                buildRow(
                    team.rank.toString(),
                    team.logoUrl,
                    team.abbreviation,
                    team.name,
                    values,
                    header = false,
                    onClick = { openTeam(team.teamId, team.name, team.abbreviation, team.logoUrl) }
                )
            )
        }
    }

    private fun buildRow(
        position: String,
        logoUrl: String,
        abbreviation: String,
        name: String,
        values: List<String>,
        header: Boolean,
        onClick: (() -> Unit)? = null
    ): LinearLayout {

        val row = LinearLayout(activity)

        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        row.setPadding(dp(8), dp(4), dp(8), dp(4))

        val color = if (header) gray else Color.WHITE

        row.addView(
            cell(position, 44, color, header),
            LinearLayout.LayoutParams(dp(44), dp(36))
        )

        if (header || logoUrl.isEmpty()) {
            row.addView(View(activity), LinearLayout.LayoutParams(dp(32), dp(32)))
        } else {
            val logo = ImageView(activity)
            logo.scaleType = ImageView.ScaleType.CENTER_INSIDE
            row.addView(logo, LinearLayout.LayoutParams(dp(32), dp(32)))

            TeamLogos.load(activity, league.displayName, league.sport, abbreviation, logoUrl, logo)
        }

        val nameView = TextView(activity)
        nameView.text = name
        nameView.textSize = 17f
        nameView.setTextColor(color)
        nameView.maxLines = 1
        nameView.ellipsize = TextUtils.TruncateAt.END
        nameView.setPadding(dp(12), 0, dp(8), 0)
        if (header) nameView.typeface = Typeface.DEFAULT_BOLD

        row.addView(nameView, LinearLayout.LayoutParams(0, -2, 1f))

        for (value in values) {
            row.addView(
                cell(value, 72, color, header),
                LinearLayout.LayoutParams(dp(72), dp(36))
            )
        }

        if (!header) {
            row.isFocusable = true
            row.isFocusableInTouchMode = true
            styleRow(row, false)
            row.setOnFocusChangeListener { v, hasFocus -> styleRow(v, hasFocus) }

            if (onClick != null) {
                row.setOnClickListener { onClick() }
            }
        }

        return row
    }

    private fun styleRow(view: View, focused: Boolean) {

        val bg = GradientDrawable()

        bg.cornerRadius = dp(8).toFloat()
        bg.setColor(if (focused) Color.rgb(50, 50, 58) else Color.TRANSPARENT)

        if (focused) {
            bg.setStroke(dp(2), Color.WHITE)
        }

        view.background = bg
    }

    private fun cell(text: String, widthDp: Int, color: Int, bold: Boolean): TextView {

        val view = TextView(activity)

        view.text = text
        view.textSize = 16f
        view.gravity = Gravity.CENTER
        view.setTextColor(color)
        view.maxLines = 1
        if (bold) view.typeface = Typeface.DEFAULT_BOLD

        return view
    }

    private fun title(text: String): TextView {

        val view = TextView(activity)

        view.text = text
        view.textSize = 19f
        view.typeface = Typeface.DEFAULT_BOLD
        view.setTextColor(Color.WHITE)
        view.setPadding(dp(8), dp(14), 0, dp(6))

        return view
    }

    private fun message(text: String): TextView {

        val view = TextView(activity)

        view.text = text
        view.textSize = 18f
        view.setTextColor(gray)
        view.setPadding(0, dp(20), 0, dp(20))

        return view
    }

    private fun controlButton(onClick: () -> Unit): TextView {

        val view = TextView(activity)

        view.textSize = 16f
        view.typeface = Typeface.DEFAULT_BOLD
        view.setTextColor(Color.WHITE)
        view.gravity = Gravity.CENTER_VERTICAL
        view.setPadding(dp(20), 0, dp(20), 0)
        view.isFocusable = true
        view.isFocusableInTouchMode = true

        styleControl(view, false)

        view.setOnFocusChangeListener { v, hasFocus -> styleControl(v, hasFocus) }
        view.setOnClickListener { onClick() }

        return view
    }

    private fun styleControl(view: View, focused: Boolean) {

        val bg = GradientDrawable()

        bg.cornerRadius = dp(24).toFloat()
        bg.setColor(Color.rgb(38, 38, 43))

        if (focused) {
            bg.setStroke(dp(3), Color.WHITE)
        }

        view.background = bg
    }

    private fun dp(value: Int): Int =
        (value * activity.resources.displayMetrics.density).toInt()
}
