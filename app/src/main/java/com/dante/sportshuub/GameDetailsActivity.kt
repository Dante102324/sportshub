package com.dante.sportshuub

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import android.widget.ImageView

class GameDetailsActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val game = Game(
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
            iptvChannel = intent.getStringExtra("iptvChannel") ?: ""
        )

        setContentView(createDetailsScreen(game))
    }

    private fun createDetailsScreen(game: Game): View {

        val root = LinearLayout(this)

        root.orientation = LinearLayout.VERTICAL

        root.setBackgroundColor(
            Color.rgb(18, 18, 21)
        )

        root.setPadding(
            dp(60),
            dp(35),
            dp(60),
            dp(35)
        )

        // BACK

        val back = TextView(this)

        back.text = "←  BACK"
        back.textSize = 18f
        back.setTextColor(Color.LTGRAY)

        back.isFocusable = true
        back.isFocusableInTouchMode = true

        back.setPadding(
            dp(15),
            dp(10),
            dp(15),
            dp(10)
        )

        back.setOnClickListener {
            finish()
        }

        root.addView(
            back,
            LinearLayout.LayoutParams(
                -1,
                dp(55)
            )
        )

        // SPORT

        val sport = TextView(this)

        sport.text = game.sport
        sport.textSize = 20f
        sport.setTextColor(Color.LTGRAY)
        sport.gravity = Gravity.CENTER

        root.addView(
            sport,
            LinearLayout.LayoutParams(
                -1,
                dp(40)
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
                dp(100),
                dp(100)
            )
        )

        val awayName = TextView(this)

        awayName.text = game.awayTeam
        awayName.textSize = 24f
        awayName.typeface = Typeface.DEFAULT_BOLD
        awayName.setTextColor(Color.WHITE)
        awayName.gravity = Gravity.CENTER

        awayLayout.addView(
            awayName,
            LinearLayout.LayoutParams(
                dp(150),
                dp(45)
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

        score.textSize = 40f
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
                dp(100),
                dp(100)
            )
        )

        val homeName = TextView(this)

        homeName.text = game.homeTeam
        homeName.textSize = 24f
        homeName.typeface = Typeface.DEFAULT_BOLD
        homeName.setTextColor(Color.WHITE)
        homeName.gravity = Gravity.CENTER

        homeLayout.addView(
            homeName,
            LinearLayout.LayoutParams(
                dp(150),
                dp(45)
            )
        )

// =====================================================
// TEAM LOGOS
// =====================================================

        val awayTeamData = TeamRepository.getTeam(
            game.league,
            game.awayTeam
        )

        if (awayTeamData != null) {

            LogoManager.loadLogo(
                this,
                awayTeamData
            ) { bitmap ->

                if (bitmap != null) {
                    awayLogo.setImageBitmap(bitmap)
                }
            }
        }

        val homeTeamData = TeamRepository.getTeam(
            game.league,
            game.homeTeam
        )

        if (homeTeamData != null) {

            LogoManager.loadLogo(
                this,
                homeTeamData
            ) { bitmap ->

                if (bitmap != null) {
                    homeLogo.setImageBitmap(bitmap)
                }
            }
        }

// -----------------------------------------------------
// PUT EVERYTHING TOGETHER
// -----------------------------------------------------

        teamsLayout.addView(
            awayLayout,
            LinearLayout.LayoutParams(
                dp(160),
                dp(160)
            )
        )

        teamsLayout.addView(
            score,
            LinearLayout.LayoutParams(
                dp(180),
                dp(160)
            )
        )

        teamsLayout.addView(
            homeLayout,
            LinearLayout.LayoutParams(
                dp(160),
                dp(160)
            )
        )

        root.addView(
            teamsLayout,
            LinearLayout.LayoutParams(
                -1,
                dp(190)
            )
        )

        // STATUS

        val status = TextView(this)

        status.text = game.status
        status.textSize = 18f
        status.setTextColor(Color.WHITE)
        status.gravity = Gravity.CENTER

        root.addView(
            status,
            LinearLayout.LayoutParams(
                -1,
                dp(45)
            )
        )

        // GAME PREVIEW

        val preview = TextView(this)

        preview.text = "GAME PREVIEW"
        preview.textSize = 22f
        preview.setTextColor(Color.GRAY)
        preview.gravity = Gravity.CENTER

        val previewBackground = GradientDrawable()

        previewBackground.setColor(
            Color.rgb(35, 35, 40)
        )

        previewBackground.cornerRadius =
            dp(12).toFloat()

        preview.background = previewBackground

        root.addView(
            preview,
            LinearLayout.LayoutParams(
                -1,
                dp(220)
            ).apply {
                setMargins(
                    0,
                    dp(10),
                    0,
                    dp(15)
                )
            }
        )

        // WATCH BUTTON

        val watch = Button(this)

        watch.text = "▶  WATCH GAME"
        watch.textSize = 18f

        watch.isFocusable = true
        watch.isFocusableInTouchMode = true

        watch.setOnClickListener {

            Toast.makeText(
                this,
                "Watch Router coming next",
                Toast.LENGTH_SHORT
            ).show()
        }

        root.addView(
            watch,
            LinearLayout.LayoutParams(
                -1,
                dp(60)
            ).apply {
                setMargins(
                    0,
                    0,
                    0,
                    dp(20)
                )
            }
        )

        // GAME STATS

        root.addView(
            sectionTitle("GAME STATS")
        )

        root.addView(
            statText(
                "              BOS              MIA\n\n" +
                        "FG%          48%              44%\n" +
                        "3PT          14               11\n" +
                        "REB          42               38\n" +
                        "AST          25               21"
            )
        )

        // GAME LEADERS

        root.addView(
            sectionTitle("GAME LEADERS")
        )

        root.addView(
            statText(
                "BOS   Player Name — 32 PTS\n\n" +
                        "MIA   Player Name — 28 PTS"
            )
        )

        val scrollView = ScrollView(this)

        scrollView.isFocusable = false
        scrollView.isFocusableInTouchMode = false

        scrollView.addView(root)

        return scrollView
    }

    private fun sectionTitle(text: String): TextView {

        val title = TextView(this)

        title.text = text
        title.textSize = 21f
        title.typeface = Typeface.DEFAULT_BOLD
        title.setTextColor(Color.WHITE)

        title.setPadding(
            0,
            dp(20),
            0,
            dp(10)
        )

        return title
    }

    private fun statText(text: String): TextView {

        val stats = TextView(this)

        stats.text = text
        stats.textSize = 17f
        stats.setTextColor(Color.LTGRAY)

        stats.setPadding(
            0,
            dp(5),
            0,
            dp(10)
        )

        return stats
    }

    private fun dp(value: Int): Int {

        return (
                value * resources.displayMetrics.density
                ).toInt()
    }
}