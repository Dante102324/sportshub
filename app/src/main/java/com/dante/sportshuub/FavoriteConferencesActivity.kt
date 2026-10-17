package com.dante.sportshuub

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.addCallback
import androidx.fragment.app.FragmentActivity

class FavoriteConferencesActivity : FragmentActivity() {

    private lateinit var titleText: TextView
    private lateinit var body: LinearLayout

    private val selected = mutableSetOf<String>()
    private val cache = mutableMapOf<String, List<ConferenceInfo>>()
    private var currentLeague: LeagueInfo? = null

    // Conferences only exist for the college leagues.
    private val collegeLeagueKeys = listOf("CFB", "CBB")

    // Shown first in the list.
    private val bigConferences =
        setOf("sec", "big 12", "acc", "big ten", "pac-12", "big east")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        selected.addAll(FavoritesStore.getFavoriteConferences(this))

        setContentView(createScreen())
        showLeagueList()

        // Back goes from the conference list to the league list, then closes.
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

        body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val scroll = ScrollView(this)
        scroll.addView(body)

        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        root.addView(TvUi.row(this, "Done") { finish() })

        return root
    }

    private fun showLeagueList() {

        currentLeague = null

        titleText.text = "FAVORITE CONFERENCES"

        body.removeAllViews()
        body.addView(note("Games with teams from these conferences are shown first."))

        for (key in collegeLeagueKeys) {
            val league = LeagueRegistry.byKey(key) ?: continue

            body.addView(
                TvUi.row(this, "${league.displayName}  ›") {
                    showConferences(league)
                }
            )
        }
    }

    private fun showConferences(league: LeagueInfo) {

        currentLeague = league

        titleText.text = league.displayName.uppercase()

        val cached = cache[league.key]
        if (cached != null) {
            render(league, cached)
            return
        }

        body.removeAllViews()
        body.addView(note("Loading conferences…"))

        Thread {
            val conferences = ConferenceRepository.getConferences(this, league)

            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                // The user may have gone back while this was loading.
                if (currentLeague?.key != league.key) return@runOnUiThread

                if (conferences.isEmpty()) {
                    body.removeAllViews()
                    body.addView(
                        note("Couldn't load conferences. Check your connection and try again.")
                    )
                } else {
                    cache[league.key] = conferences
                    render(league, conferences)
                }
            }
        }.start()
    }

    private fun render(league: LeagueInfo, conferences: List<ConferenceInfo>) {

        body.removeAllViews()

        val sorted = conferences.sortedWith(
            compareBy<ConferenceInfo> { it.shortName.lowercase() !in bigConferences }
                .thenBy { it.name }
        )

        for (conference in sorted) {
            body.addView(createRow(league, conference))
        }
    }

    private fun createRow(league: LeagueInfo, conference: ConferenceInfo): TextView {

        val key = FavoritesStore.conferenceKey(league.key, conference.id)

        val title =
            if (conference.shortName.isNotEmpty() && conference.shortName != conference.name) {
                "${conference.shortName} — ${conference.name}"
            } else {
                conference.name
            }

        fun label() = (if (key in selected) "☑  " else "☐  ") + title

        return TvUi.row(this, label()) { row ->
            if (key in selected) {
                selected.remove(key)
            } else {
                selected.add(key)
            }
            FavoritesStore.setFavoriteConferences(this, selected)
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
