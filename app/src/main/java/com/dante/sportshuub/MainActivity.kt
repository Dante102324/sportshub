package com.dante.sportshuub

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.fragment.app.FragmentActivity

class MainActivity : FragmentActivity() {

    private val backgroundColor = Color.rgb(18, 18, 21)
    private val white = Color.WHITE
    private val gray = Color.rgb(180, 180, 185)

    private var gamesLoaded = false
    private var allGames: List<EspnGame> = emptyList()

    private var selectedTab = 0
    private val tabNames = listOf("LIVE", "UPCOMING", "FINAL", "FAVORITES", "TOP GAMES")
    private val tabViews = mutableListOf<TextView>()
    private var contentContainer: LinearLayout? = null

    private var lastFavoritesSignature = ""

    // Team keys of every team in the user's favorite conferences.
    private var conferenceTeamKeys: Set<String> = emptySet()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        TeamRepository.initializeDatabase(this)
        TeamLogoUpdater.updateMissingLogos(this)
        setContentView(createHomeScreen())
        loadRealGames()
    }

    override fun onResume() {
        super.onResume()

        // Redraw only when favorites changed (e.g. after visiting Settings).
        if (gamesLoaded && favoritesSignature() != lastFavoritesSignature) {
            lastFavoritesSignature = favoritesSignature()

            Thread {
                val keys = try {
                    ConferenceRepository.favoriteConferenceTeamKeys(this)
                } catch (e: Exception) {
                    Log.e("MainActivity", "Conference lookup failed: ${e.message}")
                    emptySet()
                }

                runOnUiThread {
                    if (isFinishing || isDestroyed) return@runOnUiThread
                    conferenceTeamKeys = keys
                    showTab(selectedTab)
                }
            }.start()
        }
    }

    private fun favoritesSignature(): String =
        FavoritesStore.getFavoriteLeagues(this).sorted().joinToString(",") +
            "|" +
            FavoritesStore.getFavoriteTeams(this).sorted().joinToString(",") +
            "|" +
            FavoritesStore.getFavoriteConferences(this).sorted().joinToString(",")

    // =====================================================
    // HOME SCREEN
    // =====================================================

    private fun createHomeScreen(): View {

        val root = LinearLayout(this)

        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(backgroundColor)
        root.setPadding(dp(50), dp(30), dp(50), dp(30))

        // ---- Header ----

        val header = LinearLayout(this)

        header.orientation = LinearLayout.HORIZONTAL
        header.gravity = Gravity.CENTER_VERTICAL

        val title = TextView(this)

        title.text = "SPORTS HUB"
        title.textSize = 34f
        title.setTextColor(white)

        header.addView(
            title,
            LinearLayout.LayoutParams(0, dp(70), 1f)
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
            LinearLayout.LayoutParams(dp(70), dp(70))
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
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        header.addView(
            settings,
            LinearLayout.LayoutParams(dp(70), dp(70))
        )

        root.addView(
            header,
            LinearLayout.LayoutParams(-1, dp(70))
        )

        // ---- Tabs ----

        root.addView(createTabBar())

        // ---- Games for the selected tab ----

        val content = LinearLayout(this)
        content.orientation = LinearLayout.VERTICAL
        contentContainer = content

        val scrollView = ScrollView(this)
        scrollView.isFocusable = false
        scrollView.isFocusableInTouchMode = false
        scrollView.addView(content)

        root.addView(
            scrollView,
            LinearLayout.LayoutParams(-1, 0, 1f)
        )

        showTab(selectedTab)

        return root
    }

    // =====================================================
    // TABS
    // =====================================================

    private fun createTabBar(): LinearLayout {

        val bar = LinearLayout(this)

        bar.orientation = LinearLayout.HORIZONTAL
        bar.setPadding(0, dp(6), 0, dp(10))

        tabViews.clear()

        for (i in tabNames.indices) {

            val tab = TextView(this)

            tab.text = tabNames[i]
            tab.textSize = 16f
            tab.typeface = Typeface.DEFAULT_BOLD
            tab.gravity = Gravity.CENTER
            tab.setPadding(dp(22), dp(10), dp(22), dp(10))
            tab.isFocusable = true
            tab.isFocusableInTouchMode = true

            tab.setOnClickListener { selectTab(i) }
            tab.setOnFocusChangeListener { _, _ -> styleTabs() }

            tabViews.add(tab)

            bar.addView(
                tab,
                LinearLayout.LayoutParams(-2, -2).apply {
                    setMargins(0, 0, dp(10), 0)
                }
            )
        }

        styleTabs()

        return bar
    }

    private fun styleTabs() {

        for (i in tabViews.indices) {

            val tab = tabViews[i]
            val selected = i == selectedTab

            val bg = GradientDrawable()

            bg.cornerRadius = dp(22).toFloat()
            bg.setColor(
                if (selected) Color.rgb(65, 65, 72) else Color.TRANSPARENT
            )

            if (tab.isFocused) {
                bg.setStroke(dp(3), Color.WHITE)
            }

            tab.background = bg
            tab.setTextColor(if (selected) white else gray)
        }
    }

    private fun selectTab(index: Int) {
        selectedTab = index
        styleTabs()
        showTab(index)
    }

    private fun createMessage(text: String): TextView {

        val message = TextView(this)

        message.text = text
        message.textSize = 18f
        message.setTextColor(gray)
        message.setPadding(0, dp(20), 0, dp(20))

        return message
    }

    private fun showTab(index: Int) {

        val content = contentContainer ?: return

        content.removeAllViews()

        if (!gamesLoaded) {
            content.addView(createMessage("Loading…"))
            return
        }

        val favLeagues = FavoritesStore.getFavoriteLeagues(this)
        // Teams in favorite conferences count as favorite teams.
        val favTeams = FavoritesStore.getFavoriteTeams(this) + conferenceTeamKeys

        lastFavoritesSignature = favoritesSignature()

        val games: List<EspnGame> = when (index) {
            0 -> GameRanking.sortFavoritesFirst(
                allGames.filter { it.state == "in" },
                favLeagues,
                favTeams
            )
            1 -> GameRanking.sortFavoritesFirst(
                allGames.filter { it.state == "pre" },
                favLeagues,
                favTeams
            )
            2 -> GameRanking.sortFavoritesFirst(
                allGames.filter { it.state == "post" },
                favLeagues,
                favTeams,
                newestFirst = true
            )
            3 -> GameRanking.favoriteGames(allGames, favLeagues, favTeams)
            else -> try {
                GameRanking.topGames(allGames)
            } catch (e: Exception) {
                Log.e("MainActivity", "Top games failed", e)
                emptyList()
            }
        }

        if (games.isEmpty()) {
            val text = when (index) {
                0 -> "No live games right now"
                1 -> "No upcoming games today"
                2 -> "No finished games yet today"
                3 ->
                    if (favLeagues.isEmpty() && favTeams.isEmpty() &&
                    FavoritesStore.getFavoriteConferences(this).isEmpty()
                ) {
                        "No favorites yet. Pick teams and competitions in Settings (⚙)."
                    } else {
                        "No games for your favorites today"
                    }
                else -> "No games to show right now"
            }
            content.addView(createMessage(text))
            return
        }

        addGameRows(content, games.take(60).map { it.toTileGame() })
    }

    private fun addGameRows(
        container: LinearLayout,
        games: List<Game>
    ) {

        val perRow = 3

        for (start in 0 until games.size step perRow) {

            val rowGames = games.subList(
                start,
                minOf(start + perRow, games.size)
            )

            val row = LinearLayout(this)

            row.orientation = LinearLayout.HORIZONTAL
            row.setPadding(0, dp(5), 0, dp(12))

            val cards = mutableListOf<FrameLayout>()

            for (game in rowGames) {

                val card = createGameCard(game)

                cards.add(card)

                row.addView(
                    card,
                    LinearLayout.LayoutParams(0, dp(CARD_HEIGHT), 1f).apply {
                        setMargins(dp(6), 0, dp(6), 0)
                    }
                )
            }

            // Keep short rows the same card size as full rows.
            repeat(perRow - rowGames.size) {
                row.addView(
                    View(this),
                    LinearLayout.LayoutParams(0, dp(CARD_HEIGHT), 1f).apply {
                        setMargins(dp(6), 0, dp(6), 0)
                    }
                )
            }

            for (i in cards.indices) {

                if (i > 0) {
                    cards[i].nextFocusLeftId = cards[i - 1].id
                }

                if (i < cards.size - 1) {
                    cards[i].nextFocusRightId = cards[i + 1].id
                }
            }

            container.addView(row)
        }
    }

    // =====================================================
    // LOADING REAL GAMES
    // =====================================================

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

            Log.d(
                "MainActivity",
                "Games loaded: live=${espnGames.count { it.state == "in" }}, " +
                    "upcoming=${espnGames.count { it.state == "pre" }}, " +
                    "final=${espnGames.count { it.state == "post" }}"
            )

            Log.d(
                "MainActivity",
                "Per league: " + espnGames.groupingBy { it.leagueKey }.eachCount()
            )

            val confKeys = try {
                ConferenceRepository.favoriteConferenceTeamKeys(this)
            } catch (e: Exception) {
                Log.e("MainActivity", "Conference lookup failed: ${e.message}")
                emptySet()
            }

            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread

                allGames = espnGames
                conferenceTeamKeys = confKeys
                gamesLoaded = true
                showTab(selectedTab)
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

    // =====================================================
    // GAME TILE
    // =====================================================

    private fun createGameCard(
        game: Game
    ): FrameLayout {

        val card = FrameLayout(this)

        card.id = View.generateViewId()

        card.isFocusable = true
        card.isFocusableInTouchMode = true

        // ---- Background + dark overlay ----

        val background = ImageView(this)

        background.scaleType = ImageView.ScaleType.CENTER_CROP
        background.setImageDrawable(ColorDrawable(Color.rgb(45, 45, 50)))

        card.addView(background, FrameLayout.LayoutParams(-1, -1))

        val overlay = View(this)

        overlay.setBackgroundColor(Color.argb(145, 0, 0, 0))

        card.addView(overlay, FrameLayout.LayoutParams(-1, -1))

        // ---- Game information ----
        // Heights below add up to exactly the space inside the card
        // (160dp card - 16dp padding = 144dp = 20 + 96 + 28), so the
        // status line / start time at the bottom is never clipped.

        val info = LinearLayout(this)

        info.orientation = LinearLayout.VERTICAL
        info.gravity = Gravity.CENTER

        info.setPadding(dp(12), dp(8), dp(12), dp(8))

        // Sport / league

        val sport = TextView(this)

        sport.text = game.league
        sport.textSize = 12f
        sport.setTextColor(Color.LTGRAY)
        sport.gravity = Gravity.CENTER

        info.addView(sport, LinearLayout.LayoutParams(-1, dp(20)))

        // Teams + logos + score

        val teamsLayout = LinearLayout(this)

        teamsLayout.orientation = LinearLayout.HORIZONTAL
        teamsLayout.gravity = Gravity.CENTER

        // Away team

        val awayLayout = LinearLayout(this)

        awayLayout.orientation = LinearLayout.VERTICAL
        awayLayout.gravity = Gravity.CENTER

        val awayLogo = ImageView(this)

        awayLogo.scaleType = ImageView.ScaleType.CENTER_INSIDE

        awayLayout.addView(awayLogo, LinearLayout.LayoutParams(dp(52), dp(52)))

        val awayName = TextView(this)

        awayName.text = game.awayTeam
        awayName.textSize = 17f
        awayName.typeface = Typeface.DEFAULT_BOLD
        awayName.setTextColor(Color.WHITE)
        awayName.gravity = Gravity.CENTER

        awayLayout.addView(awayName, LinearLayout.LayoutParams(dp(85), dp(34)))

        // Score

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

        // Home team

        val homeLayout = LinearLayout(this)

        homeLayout.orientation = LinearLayout.VERTICAL
        homeLayout.gravity = Gravity.CENTER

        val homeLogo = ImageView(this)

        homeLogo.scaleType = ImageView.ScaleType.CENTER_INSIDE

        homeLayout.addView(homeLogo, LinearLayout.LayoutParams(dp(52), dp(52)))

        val homeName = TextView(this)

        homeName.text = game.homeTeam
        homeName.textSize = 17f
        homeName.typeface = Typeface.DEFAULT_BOLD
        homeName.setTextColor(Color.WHITE)
        homeName.gravity = Gravity.CENTER

        homeLayout.addView(homeName, LinearLayout.LayoutParams(dp(85), dp(34)))

        loadTeamLogo(game, game.awayTeam, game.awayLogo, awayLogo)
        loadTeamLogo(game, game.homeTeam, game.homeLogo, homeLogo)

        teamsLayout.addView(awayLayout, LinearLayout.LayoutParams(dp(90), dp(96)))
        teamsLayout.addView(score, LinearLayout.LayoutParams(dp(75), dp(96)))
        teamsLayout.addView(homeLayout, LinearLayout.LayoutParams(dp(90), dp(96)))

        info.addView(teamsLayout, LinearLayout.LayoutParams(-1, dp(96)))

        // Status / start time

        val status = TextView(this)

        status.text = game.status
        status.textSize = 13f
        status.setTextColor(Color.WHITE)
        status.gravity = Gravity.CENTER

        info.addView(status, LinearLayout.LayoutParams(-1, dp(28)))

        card.addView(info, FrameLayout.LayoutParams(-1, -1))

        // ---- Focus effect ----

        setNormalBackground(card)

        card.setOnFocusChangeListener { view, hasFocus ->

            if (hasFocus) {

                setFocusedBackground(view as FrameLayout)

                view.scaleX = 1.05f
                view.scaleY = 1.05f

            } else {

                setNormalBackground(view as FrameLayout)

                view.scaleX = 1f
                view.scaleY = 1f
            }
        }

        // ---- Click ----

        card.setOnClickListener {

            val intent = Intent(this, GameDetailsActivity::class.java)

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
            intent.putExtra("state", game.state)

            startActivity(intent)
        }

        return card
    }

    private fun setNormalBackground(view: FrameLayout) {

        val background = GradientDrawable()

        background.setColor(Color.TRANSPARENT)
        background.cornerRadius = dp(10).toFloat()

        view.background = background
    }

    private fun setFocusedBackground(view: FrameLayout) {

        val background = GradientDrawable()

        background.setColor(Color.TRANSPARENT)
        background.cornerRadius = dp(10).toFloat()
        background.setStroke(dp(3), Color.WHITE)

        view.background = background
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    companion object {
        private const val CARD_HEIGHT = 160
    }
}
