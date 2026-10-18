package com.dante.sportshuub

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.FragmentActivity

class GameDetailsActivity : FragmentActivity() {

    private val gray = Color.rgb(180, 180, 185)

    private lateinit var game: Game
    private var league: LeagueInfo? = null
    private var eventId = ""

    // Parts of the screen that get refreshed when real data arrives.
    private lateinit var scoreView: TextView
    private lateinit var statusView: TextView
    private lateinit var previewBox: TextView
    private lateinit var lineScoreTitle: TextView
    private lateinit var lineScoreBox: LinearLayout
    private lateinit var statsBox: LinearLayout
    private lateinit var leadersBox: LinearLayout

    private val handler = Handler(Looper.getMainLooper())
    private val refreshRunnable = Runnable { loadDetail() }
    private var hasDetail = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        game = Game(
            id = intent.getStringExtra("id") ?: "",
            sport = intent.getStringExtra("sport") ?: "",
            league = intent.getStringExtra("league") ?: "",
            awayTeam = intent.getStringExtra("awayTeam") ?: "",
            awayScore = intent.getStringExtra("awayScore") ?: "",
            homeTeam = intent.getStringExtra("homeTeam") ?: "",
            homeScore = intent.getStringExtra("homeScore") ?: "",
            status = intent.getStringExtra("status") ?: "",
            startTime = intent.getStringExtra("startTime") ?: "",
            awayLogo = intent.getStringExtra("awayLogo") ?: "",
            homeLogo = intent.getStringExtra("homeLogo") ?: "",
            network = intent.getStringExtra("network") ?: "",
            streamingService = intent.getStringExtra("streamingService") ?: "",
            iptvChannel = intent.getStringExtra("iptvChannel") ?: "",
            state = intent.getStringExtra("state") ?: "",
            awayRank = intent.getIntExtra("awayRank", 0).takeIf { it > 0 },
            homeRank = intent.getIntExtra("homeRank", 0).takeIf { it > 0 },
            leagueKey = intent.getStringExtra("leagueKey") ?: "",
            awayId = intent.getStringExtra("awayId") ?: "",
            homeId = intent.getStringExtra("homeId") ?: ""
        )

        // Tile ids look like "NBA_401234567" (league key + ESPN game id).
        league = LeagueRegistry.byKey(game.id.substringBefore("_"))
        eventId = game.id.substringAfter("_", "")

        setContentView(createDetailsScreen())
    }

    override fun onResume() {
        super.onResume()
        loadDetail()
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(refreshRunnable)
    }

    // =====================================================
    // LOADING REAL DATA
    // =====================================================

    private fun loadDetail() {

        val leagueInfo = league

        if (leagueInfo == null || eventId.isEmpty()) {
            showMessage(statsBox, "No details available for this game.")
            return
        }

        if (!hasDetail) {
            showMessage(statsBox, "Loading…")
        }

        Thread {
            val detail = try {
                EspnApi.fetchGameDetail(leagueInfo, eventId)
            } catch (e: Exception) {
                Log.e("GameDetails", "Detail failed: ${e.message}")
                null
            }

            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread

                if (detail != null) {
                    applyDetail(detail)

                    // Keep live games fresh while this screen is open.
                    handler.removeCallbacks(refreshRunnable)
                    if (detail.state == "in") {
                        handler.postDelayed(refreshRunnable, 30_000)
                    }
                } else if (!hasDetail) {
                    showMessage(statsBox, "Couldn't load stats right now.")
                }
            }
        }.start()
    }

    private fun applyDetail(d: EspnGameDetail) {

        hasDetail = true

        // The stream preview only makes sense while the game is being played.
        previewBox.visibility = if (d.state == "in") View.VISIBLE else View.GONE

        if (d.state != "pre" && d.awayScore.isNotEmpty() && d.homeScore.isNotEmpty()) {
            scoreView.text = "${d.awayScore}  -  ${d.homeScore}"
        }

        // Upcoming games keep the friendly local start time from the tile.
        if (d.state != "pre" && d.statusText.isNotEmpty()) {
            statusView.text = d.statusText
        }

        renderLineScore(d)
        renderStats(d)
        renderLeaders(d)
    }

    // =====================================================
    // RENDERING
    // =====================================================

    private fun renderLineScore(d: EspnGameDetail) {

        lineScoreBox.removeAllViews()

        val lineScore = d.lineScore

        if (lineScore == null || game.sport == "Soccer" || d.state == "pre") {
            lineScoreTitle.visibility = View.GONE
            lineScoreBox.visibility = View.GONE
            return
        }

        lineScoreTitle.visibility = View.VISIBLE
        lineScoreBox.visibility = View.VISIBLE

        val periods = maxOf(lineScore.away.size, lineScore.home.size)

        fun addRow(name: String, values: List<String>, total: String, bold: Boolean) {
            val row = LinearLayout(this)
            row.orientation = LinearLayout.HORIZONTAL
            row.setPadding(0, dp(3), 0, dp(3))

            row.addView(cell(name, 80, bold))

            for (i in 0 until periods) {
                row.addView(cell(values.getOrNull(i) ?: "", 46, bold))
            }

            row.addView(cell(total, 56, true))
            lineScoreBox.addView(row)
        }

        addRow("", (1..periods).map { it.toString() }, "T", true)
        addRow(game.awayTeam, lineScore.away, d.awayScore, false)
        addRow(game.homeTeam, lineScore.home, d.homeScore, false)
    }

    private fun renderStats(d: EspnGameDetail) {

        statsBox.removeAllViews()

        if (d.stats.isEmpty()) {
            showMessage(
                statsBox,
                if (d.state == "pre") "Stats will appear once the game starts."
                else "Stats aren't available for this game."
            )
            return
        }

        statsBox.addView(statRow(game.awayTeam, "", game.homeTeam, true))

        for (row in d.stats) {
            statsBox.addView(statRow(row.away, row.label, row.home, false))
        }
    }

    private fun renderLeaders(d: EspnGameDetail) {

        leadersBox.removeAllViews()

        val hasAny = d.leaders.any { it.lines.isNotEmpty() }

        if (!hasAny) {
            showMessage(
                leadersBox,
                if (d.state == "pre") "Leaders will appear once the game starts."
                else "Leaders aren't available for this game."
            )
            return
        }

        for (team in d.leaders) {

            if (team.lines.isEmpty()) continue

            val name = TextView(this)
            name.text = team.abbreviation
            name.textSize = 18f
            name.typeface = Typeface.DEFAULT_BOLD
            name.setTextColor(Color.WHITE)
            name.setPadding(0, dp(8), 0, dp(4))
            leadersBox.addView(name)

            for (line in team.lines) {
                val text = TextView(this)
                text.text = "${line.category}:  ${line.athlete}  ${line.value}"
                text.textSize = 16f
                text.setTextColor(Color.LTGRAY)
                text.setPadding(0, dp(2), 0, dp(2))
                leadersBox.addView(text)
            }
        }
    }

    private fun showMessage(box: LinearLayout, text: String) {

        box.removeAllViews()

        val message = TextView(this)
        message.text = text
        message.textSize = 16f
        message.setTextColor(gray)
        message.gravity = Gravity.CENTER
        message.setPadding(0, dp(8), 0, dp(8))

        box.addView(message)
    }

    private fun cell(text: String, widthDp: Int, bold: Boolean): TextView {

        val view = TextView(this)

        view.text = text
        view.textSize = 16f
        view.gravity = Gravity.CENTER
        view.setTextColor(Color.WHITE)
        if (bold) view.typeface = Typeface.DEFAULT_BOLD
        view.layoutParams = LinearLayout.LayoutParams(dp(widthDp), -2)

        return view
    }

    // away value | label | home value
    private fun statRow(
        away: String,
        label: String,
        home: String,
        bold: Boolean
    ): LinearLayout {

        val row = LinearLayout(this)

        row.orientation = LinearLayout.HORIZONTAL
        row.setPadding(0, dp(5), 0, dp(5))

        fun part(text: String, weight: Float, color: Int): TextView {
            val view = TextView(this)
            view.text = text
            view.textSize = 17f
            view.gravity = Gravity.CENTER
            view.setTextColor(color)
            if (bold) view.typeface = Typeface.DEFAULT_BOLD
            view.layoutParams = LinearLayout.LayoutParams(0, -2, weight)
            return view
        }

        row.addView(part(away, 1f, Color.WHITE))
        row.addView(part(label, 2f, gray))
        row.addView(part(home, 1f, Color.WHITE))

        return row
    }

    // =====================================================
    // LAYOUT
    // =====================================================

    private fun createDetailsScreen(): View {

        val root = LinearLayout(this)

        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(Color.rgb(18, 18, 21))
        root.setPadding(dp(60), dp(35), dp(60), dp(35))

        // ---- Back ----

        val back = TextView(this)

        back.text = "←  BACK"
        back.textSize = 18f
        back.setTextColor(Color.LTGRAY)
        back.isFocusable = true
        back.isFocusableInTouchMode = true
        back.setPadding(dp(15), dp(10), dp(15), dp(10))
        back.setOnClickListener { finish() }

        root.addView(back, LinearLayout.LayoutParams(-1, dp(55)))

        // ---- League ----

        val sport = TextView(this)

        sport.text = game.league
        sport.textSize = 20f
        sport.setTextColor(Color.LTGRAY)
        sport.gravity = Gravity.CENTER

        root.addView(sport, LinearLayout.LayoutParams(-1, dp(40)))

        // ---- Teams + logos + score ----

        val teamsLayout = LinearLayout(this)

        teamsLayout.orientation = LinearLayout.HORIZONTAL
        teamsLayout.gravity = Gravity.CENTER

        // Away team

        val awayLayout = LinearLayout(this)

        awayLayout.orientation = LinearLayout.VERTICAL
        awayLayout.gravity = Gravity.CENTER

        val awayLogo = ImageView(this)

        awayLogo.scaleType = ImageView.ScaleType.CENTER_INSIDE

        awayLayout.addView(awayLogo, LinearLayout.LayoutParams(dp(100), dp(100)))

        val awayName = TextView(this)

        awayName.text = TeamLabels.withRank(game.awayTeam, game.awayRank)
        awayName.textSize = 24f
        awayName.typeface = Typeface.DEFAULT_BOLD
        awayName.setTextColor(Color.WHITE)
        awayName.gravity = Gravity.CENTER

        awayLayout.addView(awayName, LinearLayout.LayoutParams(dp(150), dp(45)))

        // Score

        scoreView = TextView(this)

        scoreView.text =
            if (game.awayScore.isNotEmpty()) {
                "${game.awayScore}  -  ${game.homeScore}"
            } else {
                "VS"
            }

        scoreView.textSize = 40f
        scoreView.typeface = Typeface.DEFAULT_BOLD
        scoreView.setTextColor(Color.WHITE)
        scoreView.gravity = Gravity.CENTER

        // Home team

        val homeLayout = LinearLayout(this)

        homeLayout.orientation = LinearLayout.VERTICAL
        homeLayout.gravity = Gravity.CENTER

        val homeLogo = ImageView(this)

        homeLogo.scaleType = ImageView.ScaleType.CENTER_INSIDE

        homeLayout.addView(homeLogo, LinearLayout.LayoutParams(dp(100), dp(100)))

        val homeName = TextView(this)

        homeName.text = TeamLabels.withRank(game.homeTeam, game.homeRank)
        homeName.textSize = 24f
        homeName.typeface = Typeface.DEFAULT_BOLD
        homeName.setTextColor(Color.WHITE)
        homeName.gravity = Gravity.CENTER

        homeLayout.addView(homeName, LinearLayout.LayoutParams(dp(150), dp(45)))

        loadTeamLogo(game.awayTeam, game.awayLogo, awayLogo)
        loadTeamLogo(game.homeTeam, game.homeLogo, homeLogo)

        makeTeamLink(awayLayout, game.awayId, game.awayTeam, game.awayLogo)
        makeTeamLink(homeLayout, game.homeId, game.homeTeam, game.homeLogo)

        teamsLayout.addView(awayLayout, LinearLayout.LayoutParams(dp(160), dp(160)))
        teamsLayout.addView(scoreView, LinearLayout.LayoutParams(dp(220), dp(160)))
        teamsLayout.addView(homeLayout, LinearLayout.LayoutParams(dp(160), dp(160)))

        root.addView(teamsLayout, LinearLayout.LayoutParams(-1, dp(190)))

        // ---- Status / start time ----

        statusView = TextView(this)

        statusView.text = game.status
        statusView.textSize = 18f
        statusView.setTextColor(Color.WHITE)
        statusView.gravity = Gravity.CENTER

        root.addView(statusView, LinearLayout.LayoutParams(-1, dp(45)))

        // ---- Preview placeholder (live stream preview comes later) ----

        val preview = TextView(this)
        previewBox = preview
        preview.visibility = if (game.state == "in") View.VISIBLE else View.GONE

        preview.text = "GAME PREVIEW"
        preview.textSize = 22f
        preview.setTextColor(Color.GRAY)
        preview.gravity = Gravity.CENTER

        val previewBackground = GradientDrawable()

        previewBackground.setColor(Color.rgb(35, 35, 40))
        previewBackground.cornerRadius = dp(12).toFloat()

        preview.background = previewBackground

        root.addView(
            preview,
            LinearLayout.LayoutParams(-1, dp(220)).apply {
                setMargins(0, dp(10), 0, dp(15))
            }
        )

        // ---- Watch button ----

        val watch = Button(this)

        watch.text = "▶  WATCH GAME"
        watch.textSize = 18f
        watch.isFocusable = true
        watch.isFocusableInTouchMode = true

        watch.setOnClickListener {
            Toast.makeText(this, "Watch Router coming next", Toast.LENGTH_SHORT).show()
        }

        root.addView(
            watch,
            LinearLayout.LayoutParams(-1, dp(60)).apply {
                setMargins(0, 0, 0, dp(20))
            }
        )

        // ---- Line score ----

        lineScoreTitle = sectionTitle("SCORE BY PERIOD")
        lineScoreTitle.visibility = View.GONE
        root.addView(lineScoreTitle)

        lineScoreBox = LinearLayout(this)
        lineScoreBox.orientation = LinearLayout.VERTICAL
        lineScoreBox.visibility = View.GONE

        root.addView(
            lineScoreBox,
            LinearLayout.LayoutParams(-2, -2).apply {
                gravity = Gravity.CENTER_HORIZONTAL
            }
        )

        // ---- Game stats ----

        root.addView(sectionTitle("GAME STATS"))

        statsBox = LinearLayout(this)
        statsBox.orientation = LinearLayout.VERTICAL

        root.addView(
            statsBox,
            LinearLayout.LayoutParams(dp(560), -2).apply {
                gravity = Gravity.CENTER_HORIZONTAL
            }
        )

        // ---- Game leaders ----

        root.addView(sectionTitle("GAME LEADERS"))

        leadersBox = LinearLayout(this)
        leadersBox.orientation = LinearLayout.VERTICAL

        root.addView(
            leadersBox,
            LinearLayout.LayoutParams(dp(560), -2).apply {
                gravity = Gravity.CENTER_HORIZONTAL
            }
        )

        val scrollView = ScrollView(this)

        scrollView.isFocusable = false
        scrollView.isFocusableInTouchMode = false

        scrollView.addView(root)

        return scrollView
    }

    private fun loadTeamLogo(
        abbreviation: String,
        logoUrl: String,
        target: ImageView
    ) {
        // Prefer your existing cached team (works for the NBA names you already have).
        val known = TeamRepository.getTeam(game.league, abbreviation)

        // Otherwise build a team from the logo URL ESPN supplied.
        val team = known ?: if (logoUrl.startsWith("http")) {
            val safeKey = "${game.league}_$abbreviation"
                .lowercase()
                .replace(Regex("[^a-z0-9]+"), "_")

            Team(
                id = safeKey,
                name = abbreviation,
                abbreviation = abbreviation,
                sport = game.sport,
                league = game.league,
                logoFileName = "espn_$safeKey",
                logoUrl = logoUrl
            )
        } else {
            null
        }

        if (team != null) {
            LogoManager.loadLogo(this, team) { bitmap ->
                if (bitmap != null) {
                    target.setImageBitmap(bitmap)
                }
            }
        }
    }

    // Selecting a team opens its page (when we know which team it is).
    private fun makeTeamLink(
        layout: LinearLayout,
        teamId: String,
        abbreviation: String,
        logoUrl: String
    ) {
        val key = game.leagueKey.ifEmpty { game.id.substringBefore("_") }
        if (teamId.isEmpty() || LeagueRegistry.byKey(key) == null) return

        layout.isFocusable = true
        layout.isFocusableInTouchMode = true

        fun style(focused: Boolean) {
            layout.background = GradientDrawable().apply {
                setColor(Color.TRANSPARENT)
                cornerRadius = dp(12).toFloat()
                if (focused) setStroke(dp(3), Color.WHITE)
            }
        }

        style(false)
        layout.setOnFocusChangeListener { _, focused -> style(focused) }
        layout.setOnClickListener {
            startActivity(
                TeamPageActivity.intent(this, key, teamId, abbreviation, abbreviation, logoUrl)
            )
        }
    }

    private fun sectionTitle(text: String): TextView {

        val title = TextView(this)

        title.text = text
        title.textSize = 21f
        title.typeface = Typeface.DEFAULT_BOLD
        title.setTextColor(Color.WHITE)
        title.gravity = Gravity.CENTER

        title.setPadding(0, dp(20), 0, dp(10))

        return title
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
}
