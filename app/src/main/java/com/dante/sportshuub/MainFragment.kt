package com.dante.sportshuub

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.leanback.app.BrowseSupportFragment
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.ListRowPresenter
import androidx.leanback.widget.OnItemViewClickedListener
import androidx.leanback.widget.Presenter
import androidx.leanback.widget.Row
import androidx.leanback.widget.RowPresenter
import android.widget.Toast

data class SportsGame(
    val awayTeam: String,
    val homeTeam: String,
    val awayScore: String,
    val homeScore: String,
    val status: String,
    val sport: String
)

class MainFragment : BrowseSupportFragment() {

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        setupUI()
        loadSportsGames()
        setupListeners()
    }

    private fun setupUI() {

        title = "DANTE'S SPORTS HUB"

        // Keep the TV-style side navigation enabled.
        headersState = BrowseSupportFragment.HEADERS_ENABLED

        // Dark sports-TV background.
        brandColor = Color.rgb(15, 15, 18)
        searchAffordanceColor = Color.rgb(45, 45, 50)
    }

    private fun loadSportsGames() {

        val rowsAdapter = ArrayObjectAdapter(ListRowPresenter())

        val gamePresenter = SportsGamePresenter()

        // =========================
        // LIVE NOW
        // =========================

        val liveGames = ArrayObjectAdapter(gamePresenter)

        liveGames.add(
            SportsGame(
                "BOS",
                "MIA",
                "102",
                "98",
                "Q4 • 1:42",
                "NBA"
            )
        )

        liveGames.add(
            SportsGame(
                "FLA",
                "PAL",
                "2",
                "1",
                "78'",
                "SOCCER"
            )
        )

        liveGames.add(
            SportsGame(
                "NYY",
                "BOS",
                "5",
                "3",
                "7th",
                "MLB"
            )
        )

        liveGames.add(
            SportsGame(
                "DAL",
                "PHI",
                "24",
                "21",
                "Q3 • 6:12",
                "NFL"
            )
        )

        rowsAdapter.add(
            ListRow(
                HeaderItem(0, "LIVE NOW"),
                liveGames
            )
        )

        // =========================
        // UPCOMING
        // =========================

        val upcomingGames = ArrayObjectAdapter(gamePresenter)

        upcomingGames.add(
            SportsGame(
                "ALA",
                "UGA",
                "",
                "",
                "7:30 PM",
                "COLLEGE FOOTBALL"
            )
        )

        upcomingGames.add(
            SportsGame(
                "BOS",
                "NYK",
                "",
                "",
                "8:00 PM",
                "NBA"
            )
        )

        upcomingGames.add(
            SportsGame(
                "FLA",
                "FLU",
                "",
                "",
                "9:00 PM",
                "SOCCER"
            )
        )

        rowsAdapter.add(
            ListRow(
                HeaderItem(1, "UPCOMING"),
                upcomingGames
            )
        )

        // =========================
        // FINAL
        // =========================

        val finalGames = ArrayObjectAdapter(gamePresenter)

        finalGames.add(
            SportsGame(
                "BOS",
                "TOR",
                "6",
                "2",
                "FINAL",
                "MLB"
            )
        )

        finalGames.add(
            SportsGame(
                "LAL",
                "GSW",
                "118",
                "111",
                "FINAL",
                "NBA"
            )
        )

        rowsAdapter.add(
            ListRow(
                HeaderItem(2, "FINAL"),
                finalGames
            )
        )

        adapter = rowsAdapter
    }

    private fun setupListeners() {

        onItemViewClickedListener =
            OnItemViewClickedListener { _, item, _, _ ->

                if (item is SportsGame) {

                    Toast.makeText(
                        context,
                        "${item.awayTeam} vs ${item.homeTeam}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

        setOnSearchClickedListener {

            Toast.makeText(
                context,
                "Sports AI search will go here.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // ============================================================
    // GAME TILE
    // ============================================================

    private class SportsGamePresenter : Presenter() {

        override fun onCreateViewHolder(
            parent: ViewGroup
        ): ViewHolder {

            val card = TextView(parent.context)

            card.layoutParams = ViewGroup.LayoutParams(
                dp(parent, 300),
                dp(parent, 170)
            )

            card.isFocusable = true
            card.isFocusableInTouchMode = true

            card.setPadding(
                dp(parent, 18),
                dp(parent, 15),
                dp(parent, 18),
                dp(parent, 15)
            )

            card.gravity = Gravity.CENTER_VERTICAL

            card.textSize = 18f
            card.setTextColor(Color.WHITE)

            setNormalBackground(card)

            card.setOnFocusChangeListener { view, hasFocus ->

                if (hasFocus) {
                    setFocusedBackground(view as TextView)
                    view.scaleX = 1.05f
                    view.scaleY = 1.05f
                } else {
                    setNormalBackground(view as TextView)
                    view.scaleX = 1.0f
                    view.scaleY = 1.0f
                }
            }

            return ViewHolder(card)
        }

        override fun onBindViewHolder(
            viewHolder: ViewHolder,
            item: Any?
        ) {

            val game = item as SportsGame
            val card = viewHolder.view as TextView

            val scoreText =
                if (game.awayScore.isNotEmpty()) {
                    "${game.awayTeam}  ${game.awayScore}\n" +
                            "${game.homeTeam}  ${game.homeScore}"
                } else {
                    "${game.awayTeam}\n" +
                            "vs\n" +
                            "${game.homeTeam}"
                }

            card.text =
                "${game.sport}\n\n" +
                        "$scoreText\n\n" +
                        game.status
        }

        override fun onUnbindViewHolder(viewHolder: ViewHolder) {
        }

        private fun setNormalBackground(view: TextView) {

            val background = GradientDrawable()

            background.setColor(
                Color.rgb(30, 30, 35)
            )

            background.cornerRadius = dpFloat(view, 10)

            view.background = background
        }

        private fun setFocusedBackground(view: TextView) {

            val background = GradientDrawable()

            background.setColor(
                Color.rgb(60, 60, 68)
            )

            background.cornerRadius = dpFloat(view, 10)

            background.setStroke(
                dp(view, 3),
                Color.WHITE
            )

            view.background = background
        }

        private fun dp(
            view: View,
            value: Int
        ): Int {

            return (
                    value *
                            view.resources.displayMetrics.density
                    ).toInt()
        }

        private fun dpFloat(
            view: View,
            value: Int
        ): Float {

            return value *
                    view.resources.displayMetrics.density
        }
    }
}