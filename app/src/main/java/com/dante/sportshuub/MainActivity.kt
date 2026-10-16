package com.dante.sportshuub

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import android.widget.ScrollView
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import com.bumptech.glide.Glide
import android.util.Log

class MainActivity : FragmentActivity() {

    private val backgroundColor = Color.rgb(18, 18, 21)
    private val cardColor = Color.rgb(38, 38, 43)
    private val focusedColor = Color.rgb(65, 65, 72)
    private val white = Color.WHITE
    private val gray = Color.rgb(180, 180, 185)
    private var gamesLoaded = false
    private var liveGames: List<Game> = emptyList()
    private var upcomingGames: List<Game> = emptyList()
    private var finalGames: List<Game> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        TeamRepository.initializeDatabase(this)
        TeamLogoUpdater.updateMissingLogos(this)
        setContentView(createHomeScreen())
        loadRealGames()
    }
    private fun createHomeScreen(): View {

        val root = LinearLayout(this)

        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(backgroundColor)
        root.setPadding(dp(50), dp(30), dp(50), dp(30))

        // =====================================================
        // HEADER
        // =====================================================

        val header = LinearLayout(this)

        header.orientation = LinearLayout.HORIZONTAL
        header.gravity = Gravity.CENTER_VERTICAL

        val title = TextView(this)

        title.text = "SPORTS HUB"
        title.textSize = 34f
        title.setTextColor(white)

        header.addView(
            title,
            LinearLayout.LayoutParams(
                0,
                dp(70),
                1f
            )
        )

        val search = TextView(this)

        search.text = "🔍"
        search.textSize = 28f
        search.gravity = Gravity.CENTER
        search.setTextColor(white)

        search.isFocusable = true
        search.isFocusableInTouchMode = true

        header.addView(
            search,
            LinearLayout.LayoutParams(
                dp(70),
                dp(70)
            )
        )

        val settings = TextView(this)

        settings.text = "⚙"
        settings.textSize = 28f
        settings.gravity = Gravity.CENTER
        settings.setTextColor(white)
        settings.isFocusable = true
        settings.isFocusableInTouchMode = true
        settings.contentDescription = "Settings"

        settings.setOnClickListener {
            startActivity(
                android.content.Intent(
                    this,
                    SettingsActivity::class.java
                )
            )
        }

        header.addView(
            settings,
            LinearLayout.LayoutParams(
                dp(70),
                dp(70)
            )
        )


        root.addView(
            header,
            LinearLayout.LayoutParams(
                -1,
                dp(70)
            )
        )

        addGameSection(root, "LIVE NOW", liveGames, "No live games right now")
        addGameSection(root, "UPCOMING", upcomingGames, "No upcoming games today")
        addGameSection(root, "FINAL", finalGames, "No finished games yet today")


        val scrollView = ScrollView(this)

        scrollView.isFillViewport = true
        scrollView.isFocusable = false
        scrollView.isFocusableInTouchMode = false

        scrollView.addView(root)

        return scrollView
    }
    private fun addGameSection(
        root: LinearLayout,
        title: String,
        games: List<Game>,
        emptyText: String
    ) {
        root.addView(createSectionTitle(title))

        if (games.isEmpty()) {
            val empty = TextView(this)
            empty.text = if (gamesLoaded) emptyText else "Loading…"
            empty.textSize = 16f
            empty.setTextColor(gray)
            empty.setPadding(0, dp(10), 0, dp(20))
            root.addView(empty)
        } else {
            root.addView(createGameGrid(games.take(3)))
        }
    }

    private fun loadRealGames() {
        Thread {
            val espnGames = mutableListOf<EspnGame>()

            for (league in LeagueRegistry.leagues) {
                try {
                    espnGames.addAll(EspnApi.fetchScoreboard(league))
                } catch (e: Exception) {
                    Log.e("MainActivity", "${league.key} scoreboard failed: ${e.message}")
                }
            }

            val live = espnGames
                .filter { it.state == "in" }
                .map { it.toTileGame() }

            val upcoming = espnGames
                .filter { it.state == "pre" }
                .sortedBy { it.startTime }
                .map { it.toTileGame() }

            val finished = espnGames
                .filter { it.state == "post" }
                .sortedByDescending { it.startTime }
                .map { it.toTileGame() }

            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread

                liveGames = live
                upcomingGames = upcoming
                finalGames = finished
                gamesLoaded = true

                setContentView(createHomeScreen())
            }
        }.start()
    }

    private fun loadTeamLogo(
        game: Game,
        abbreviation: String,
        logoUrl: String,
        target: ImageView
    ) {
        // Prefer your existing cached team (works for the NBA names you already have).
        val known = TeamRepository.getTeam(game.league, abbreviation)

        // Otherwise build a team from the logo URL that ESPN supplied.
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
    private fun createSectionTitle(text: String): TextView {

        val title = TextView(this)

        title.text = text
        title.textSize = 20f
        title.setTextColor(gray)

        title.gravity = Gravity.CENTER_VERTICAL

        title.setPadding(
            0,
            dp(8),
            0,
            0
        )

        return title
    }

    private fun createGameGrid(
        games: List<Game>
    ): LinearLayout {

        val row = LinearLayout(this)

        row.orientation = LinearLayout.HORIZONTAL

        row.setPadding(
            0,
            dp(5),
            0,
            dp(12)
        )

        val cards = mutableListOf<FrameLayout>()

        for (game in games) {

            val card = createGameCard(game)

            cards.add(card)

            row.addView(
                card,
                LinearLayout.LayoutParams(
                    0,
                    dp(145),
                    1f
                ).apply {
                    setMargins(
                        dp(6),
                        0,
                        dp(6),
                        0
                    )
                }
            )
        }
        repeat(3 - games.size) {
            row.addView(
                View(this),
                LinearLayout.LayoutParams(0, dp(145), 1f).apply {
                    setMargins(dp(6), 0, dp(6), 0)
                }
            )
        }
        // =====================================================
        // EXPLICIT LEFT / RIGHT NAVIGATION
        // =====================================================

        for (i in cards.indices) {

            val card = cards[i]

            if (i > 0) {
                card.nextFocusLeftId = cards[i - 1].id
            }

            if (i < cards.size - 1) {
                card.nextFocusRightId = cards[i + 1].id
            }
        }

        return row
    }

    private fun createGameCard(
        game: Game
    ): FrameLayout {

        val card = FrameLayout(this)

        card.id = View.generateViewId()

        card.isFocusable = true
        card.isFocusableInTouchMode = true

        // =====================================================
        // VIDEO / IMAGE BACKGROUND
        // =====================================================

        val background = ImageView(this)

        background.scaleType = ImageView.ScaleType.CENTER_CROP

        background.setImageDrawable(
            ColorDrawable(
                Color.rgb(45, 45, 50)
            )
        )

        card.addView(
            background,
            FrameLayout.LayoutParams(
                -1,
                -1
            )
        )

        // =====================================================
        // DARK OVERLAY
        // =====================================================

        val overlay = View(this)

        overlay.setBackgroundColor(
            Color.argb(
                145,
                0,
                0,
                0
            )
        )

        card.addView(
            overlay,
            FrameLayout.LayoutParams(
                -1,
                -1
            )
        )

        // =====================================================
        // GAME INFORMATION
        // =====================================================

        val info = LinearLayout(this)

        info.orientation = LinearLayout.VERTICAL
        info.gravity = Gravity.CENTER

        info.setPadding(
            dp(12),
            dp(10),
            dp(12),
            dp(10)
        )

        // =====================================================
        // SPORT / LEAGUE
        // =====================================================

        val sport = TextView(this)

        sport.text = game.league
        sport.textSize = 12f
        sport.setTextColor(Color.LTGRAY)
        sport.gravity = Gravity.CENTER

        info.addView(
            sport,
            LinearLayout.LayoutParams(
                -1,
                dp(25)
            )
        )

        // =====================================================
        // TEAMS + LOGOS + SCORE
        // =====================================================

        val teamsLayout = LinearLayout(this)

        teamsLayout.orientation = LinearLayout.HORIZONTAL
        teamsLayout.gravity = Gravity.CENTER

        // -----------------------------------------------------
        // AWAY TEAM
        // -----------------------------------------------------

        val awayLayout = LinearLayout(this)

        awayLayout.orientation = LinearLayout.VERTICAL
        awayLayout.gravity = Gravity.CENTER

        val awayLogo = ImageView(this)

        awayLogo.scaleType = ImageView.ScaleType.CENTER_INSIDE

        awayLayout.addView(
            awayLogo,
            LinearLayout.LayoutParams(
                dp(55),
                dp(55)
            )
        )

        val awayName = TextView(this)

        awayName.text = game.awayTeam
        awayName.textSize = 17f
        awayName.typeface = Typeface.DEFAULT_BOLD
        awayName.setTextColor(Color.WHITE)
        awayName.gravity = Gravity.CENTER

        awayLayout.addView(
            awayName,
            LinearLayout.LayoutParams(
                dp(85),
                dp(35)
            )
        )

        // -----------------------------------------------------
        // SCORE
        // -----------------------------------------------------

        val score = TextView(this)

        score.text =
            if (game.awayScore.isNotEmpty()) {
                "${game.awayScore}  -  ${game.homeScore}"
            } else {
                "VS"
            }

        score.textSize = 22f
        score.typeface = Typeface.DEFAULT_BOLD
        score.setTextColor(Color.WHITE)
        score.gravity = Gravity.CENTER

        // -----------------------------------------------------
        // HOME TEAM
        // -----------------------------------------------------

        val homeLayout = LinearLayout(this)

        homeLayout.orientation = LinearLayout.VERTICAL
        homeLayout.gravity = Gravity.CENTER

        val homeLogo = ImageView(this)

        homeLogo.scaleType = ImageView.ScaleType.CENTER_INSIDE

        homeLayout.addView(
            homeLogo,
            LinearLayout.LayoutParams(
                dp(55),
                dp(55)
            )
        )

        val homeName = TextView(this)

        homeName.text = game.homeTeam
        homeName.textSize = 17f
        homeName.typeface = Typeface.DEFAULT_BOLD
        homeName.setTextColor(Color.WHITE)
        homeName.gravity = Gravity.CENTER

        homeLayout.addView(
            homeName,
            LinearLayout.LayoutParams(
                dp(85),
                dp(35)
            )
        )

        loadTeamLogo(game, game.awayTeam, game.awayLogo, awayLogo)
        loadTeamLogo(game, game.homeTeam, game.homeLogo, homeLogo)

        // -----------------------------------------------------
        // PUT TEAMS + SCORE TOGETHER
        // -----------------------------------------------------

        teamsLayout.addView(
            awayLayout,
            LinearLayout.LayoutParams(
                dp(90),
                dp(100)
            )
        )

        teamsLayout.addView(
            score,
            LinearLayout.LayoutParams(
                dp(75),
                dp(100)
            )
        )

        teamsLayout.addView(
            homeLayout,
            LinearLayout.LayoutParams(
                dp(90),
                dp(100)
            )
        )

        info.addView(
            teamsLayout,
            LinearLayout.LayoutParams(
                -1,
                dp(115)
            )
        )

        // =====================================================
        // STATUS
        // =====================================================

        val status = TextView(this)

        status.text = game.status
        status.textSize = 13f
        status.setTextColor(Color.WHITE)
        status.gravity = Gravity.CENTER

        info.addView(
            status,
            LinearLayout.LayoutParams(
                -1,
                dp(30)
            )
        )

        // =====================================================
        // ADD INFORMATION TO CARD
        // =====================================================

        card.addView(
            info,
            FrameLayout.LayoutParams(
                -1,
                -1
            )
        )

        // =====================================================
        // FOCUS EFFECT
        // =====================================================

        setNormalBackground(card)

        card.setOnFocusChangeListener { view, hasFocus ->

            if (hasFocus) {

                setFocusedBackground(
                    view as FrameLayout
                )

                view.scaleX = 1.05f
                view.scaleY = 1.05f

            } else {

                setNormalBackground(
                    view as FrameLayout
                )

                view.scaleX = 1f
                view.scaleY = 1f
            }
        }

        // =====================================================
        // CLICK
        // =====================================================

        card.setOnClickListener {

            val intent = android.content.Intent(
                this,
                GameDetailsActivity::class.java
            )

            intent.putExtra("id", game.id)
            intent.putExtra("sport", game.sport)
            intent.putExtra("league", game.league)
            intent.putExtra("awayTeam", game.awayTeam)
            intent.putExtra("awayScore", game.awayScore)
            intent.putExtra("homeTeam", game.homeTeam)
            intent.putExtra("homeScore", game.homeScore)
            intent.putExtra("status", game.status)
            intent.putExtra("startTime", game.startTime)
            intent.putExtra("awayLogo", game.awayLogo)
            intent.putExtra("homeLogo", game.homeLogo)
            intent.putExtra("network", game.network)
            intent.putExtra("streamingService", game.streamingService)
            intent.putExtra("iptvChannel", game.iptvChannel)

            startActivity(intent)
        }

        return card
    }
    private fun setNormalBackground(
        view: FrameLayout
    ) {

        val background = GradientDrawable()

        background.setColor(
            Color.TRANSPARENT
        )

        background.cornerRadius =
            dp(10).toFloat()

        view.background = background
    }

    private fun setFocusedBackground(
        view: FrameLayout
    ) {

        val background = GradientDrawable()

        background.setColor(
            Color.TRANSPARENT
        )

        background.cornerRadius =
            dp(10).toFloat()

        background.setStroke(
            dp(3),
            Color.WHITE
        )

        view.background = background
    }

    private fun dp(value: Int): Int {

        return (
                value *
                        resources.displayMetrics.density
                ).toInt()
    }
    private fun testCelticsDownload() {

        val celtics = TeamRepository.getTeam(
            "NBA",
            "BOS"
        )

        if (celtics == null) {
            Toast.makeText(
                this,
                "Celtics not found",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        LogoManager.loadLogo(
            this,
            celtics
        ) { bitmap ->

            if (bitmap != null) {

                Toast.makeText(
                    this,
                    "Celtics logo downloaded!",
                    Toast.LENGTH_LONG
                ).show()

            } else {

                Toast.makeText(
                    this,
                    "Logo download failed",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
