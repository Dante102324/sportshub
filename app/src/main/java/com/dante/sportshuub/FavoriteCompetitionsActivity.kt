package com.dante.sportshuub

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class FavoriteCompetitionsActivity : Activity() {

    private val selected = mutableSetOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        selected.addAll(FavoritesStore.getFavoriteLeagues(this))
        setContentView(createScreen())
    }

    private fun createScreen(): View {

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(TvUi.backgroundColor)
            setPadding(dp(28), dp(24), dp(28), dp(24))
        }

        root.addView(
            TextView(this).apply {
                text = "FAVORITE COMPETITIONS"
                textSize = 30f
                setTextColor(Color.WHITE)
                typeface = Typeface.DEFAULT_BOLD
            }
        )

        root.addView(
            TextView(this).apply {
                text = "Games from these competitions are shown first."
                textSize = 16f
                setTextColor(TvUi.gray)
                setPadding(0, dp(6), 0, dp(18))
            }
        )

        val list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        for (league in LeagueRegistry.leagues) {
            list.addView(
                TvUi.row(this, label(league)) { row ->
                    if (league.key in selected) {
                        selected.remove(league.key)
                    } else {
                        selected.add(league.key)
                    }
                    FavoritesStore.setFavoriteLeagues(this, selected)
                    row.text = label(league)
                }
            )
        }

        val scroll = ScrollView(this)
        scroll.addView(list)

        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        root.addView(TvUi.row(this, "Done") { finish() })

        return root
    }

    private fun label(league: LeagueInfo): String =
        (if (league.key in selected) "☑  " else "☐  ") + league.displayName

    private fun dp(value: Int): Int = TvUi.dp(this, value)
}
