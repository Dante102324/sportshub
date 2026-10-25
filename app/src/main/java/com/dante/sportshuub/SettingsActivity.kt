package com.dante.sportshuub

import android.app.Activity
import android.app.AlertDialog
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import android.widget.Switch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SettingsActivity : Activity() {

    private val backgroundColor = Color.rgb(18, 18, 21)
    private val cardColor = Color.rgb(38, 38, 43)
    private val white = Color.WHITE
    private val gray = Color.rgb(180, 180, 185)

    private lateinit var progressText: TextView
    private lateinit var lastUpdatedText: TextView
    private lateinit var favoritesText: TextView
    private lateinit var homeStatusText: TextView

    private val preferences by lazy {
        getSharedPreferences("sportshub_settings", Context.MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(createSettingsScreen())
    }

    override fun onResume() {
        super.onResume()
        if (::favoritesText.isInitialized) {
            refreshFavoritesText()
        }
        if (::homeStatusText.isInitialized) {
            refreshHomeStatus()
        }
    }

    private fun createSettingsScreen(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(backgroundColor)
            setPadding(dp(28), dp(24), dp(28), dp(24))
        }

        val scrollView = ScrollView(this)

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val title = TextView(this).apply {
            text = "SETTINGS"
            textSize = 30f
            setTextColor(white)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER_VERTICAL
        }

        content.addView(title)

        val subtitle = TextView(this).apply {
            text = "Manage your favorites, teams and team logos"
            textSize = 16f
            setTextColor(gray)
            setPadding(0, dp(6), 0, dp(22))
        }

        content.addView(subtitle)

        // ---------------------------------------------------------
        // FAVORITES
        // ---------------------------------------------------------

        content.addView(sectionTitle("FAVORITES"))

        content.addView(
            makeButton("Favorite Competitions") {
                startActivity(
                    Intent(this, FavoriteCompetitionsActivity::class.java)
                )
            }
        )

        content.addView(
            makeButton("Favorite Teams") {
                startActivity(
                    Intent(this, FavoriteTeamsActivity::class.java)
                )
            }
        )

        content.addView(
            makeButton("Favorite Conferences (College)") {
                startActivity(
                    Intent(this, FavoriteConferencesActivity::class.java)
                )
            }
        )

        favoritesText = TextView(this).apply {
            textSize = 14f
            setTextColor(gray)
            setPadding(dp(4), dp(4), dp(4), dp(20))
        }

        content.addView(favoritesText)
        refreshFavoritesText()

        // ---------------------------------------------------------
        // HOME SCREEN
        // ---------------------------------------------------------

        content.addView(sectionTitle("HOME SCREEN"))

        content.addView(
            makeButton("Make Sports Hub my home screen") {
                requestHomeRole()
            }
        )

        content.addView(
            makeButton("Exit to TV home menu") {
                exitToTvHomeMenu()
            }
        )

        homeStatusText = TextView(this).apply {
            textSize = 14f
            setTextColor(white)
            setPadding(dp(4), dp(2), dp(4), dp(8))
        }

        content.addView(homeStatusText)
        refreshHomeStatus()

        content.addView(
            TextView(this).apply {
                text = "While Sports Hub is your home screen, the Home button always returns here. " +
                    "Exit switches that off until you open Sports Hub from the TV menu again or restart."
                textSize = 13f
                setTextColor(gray)
                setPadding(dp(4), dp(4), dp(4), dp(20))
            }
        )

        // ---------------------------------------------------------
        // SPORTS AI
        // ---------------------------------------------------------

        content.addView(sectionTitle("SPORTS AI"))

        content.addView(
            makeButton("Ask Sports AI (typed)") {
                startActivity(Intent(this, AskAiActivity::class.java))
            }
        )

        content.addView(
            TextView(this).apply {
                text = "For voice questions, use the 🎤 icon on the home screen."
                textSize = 13f
                setTextColor(gray)
                setPadding(dp(4), dp(4), dp(4), dp(20))
            }
        )

        // ---------------------------------------------------------
        // TEAM LOGOS
        // ---------------------------------------------------------

        content.addView(sectionTitle("TEAM LOGOS"))

        content.addView(
            makeButton("Download Missing Logos") {
                startLogoUpdate(refreshAll = false)
            }
        )

        content.addView(
            makeButton("Refresh All Logos") {
                startLogoUpdate(refreshAll = true)
            }
        )

        progressText = TextView(this).apply {
            text = "No logo update running"
            textSize = 15f
            setTextColor(gray)
            setPadding(dp(4), dp(12), dp(4), dp(8))
        }

        content.addView(progressText)

        lastUpdatedText = TextView(this).apply {
            textSize = 14f
            setTextColor(gray)
            setPadding(dp(4), dp(4), dp(4), dp(20))
        }

        content.addView(lastUpdatedText)
        refreshLastUpdatedText()

        content.addView(sectionTitle("AUTOMATIC UPDATES"))

        val automaticSwitch = Switch(this).apply {
            text = "Enable automatic logo updates"
            textSize = 16f
            setTextColor(white)
            isFocusable = true
            isChecked = preferences.getBoolean("automatic_updates_enabled", false)

            setOnCheckedChangeListener { _, checked ->
                preferences.edit()
                    .putBoolean("automatic_updates_enabled", checked)
                    .apply()

                Toast.makeText(
                    this@SettingsActivity,
                    if (checked) {
                        "Preference saved. Background scheduling is not connected yet."
                    } else {
                        "Automatic updates preference disabled."
                    },
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        content.addView(
            automaticSwitch,
            LinearLayout.LayoutParams(
                -1,
                dp(55)
            )
        )

        val automaticNote = TextView(this).apply {
            text = "Background scheduling will be connected in a later step."
            textSize = 13f
            setTextColor(gray)
            setPadding(0, dp(4), 0, dp(22))
        }

        content.addView(automaticNote)

        content.addView(sectionTitle("TEAMS & LEAGUES"))


        content.addView(
            makeButton("Refresh Teams & Leagues") {
                progressText.text = "Refreshing team database..."

                TeamRepository.refreshDatabase(this) { success, message ->
                    progressText.text = message

                    Toast.makeText(
                        this,
                        message,
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        )
        val databaseNote = TextView(this).apply {
            text = "We'll connect the team and league refresh action after checking your database initialization code."
            textSize = 13f
            setTextColor(gray)
            setPadding(0, dp(8), 0, dp(20))
        }

        content.addView(databaseNote)

        content.addView(
            makeButton("Back to SportsHub") {
                finish()
            }
        )

        scrollView.addView(content)

        root.addView(
            scrollView,
            LinearLayout.LayoutParams(-1, 0, 1f)
        )

        return root
    }

    private fun startLogoUpdate(refreshAll: Boolean) {
        progressText.text = if (refreshAll) {
            "Refreshing all team logos..."
        } else {
            "Downloading missing logos..."
        }

        setLogoButtonsEnabled(false)

        val updateAction: (Int, Int) -> Unit = { completed, total ->
            runOnUiThread {
                progressText.text = "Logos processed: $completed / $total"
            }
        }

        val completeAction: () -> Unit = {
            runOnUiThread {
                val timestamp = System.currentTimeMillis()

                preferences.edit()
                    .putLong("last_logo_update_attempt", timestamp)
                    .apply()

                refreshLastUpdatedText()

                progressText.text = "Logo update attempt finished."
                setLogoButtonsEnabled(true)

                Toast.makeText(
                    this,
                    "Logo update attempt finished. Check the progress and logs for failures.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        if (refreshAll) {
            TeamLogoUpdater.updateAllLogos(
                this,
                onProgress = updateAction,
                onComplete = completeAction
            )
        } else {
            TeamLogoUpdater.updateMissingLogos(
                this,
                onProgress = updateAction,
                onComplete = completeAction
            )
        }
    }

    private fun setLogoButtonsEnabled(enabled: Boolean) {
        // Find the logo action buttons by their text.
        // This avoids needing extra layout files or a separate adapter.
        val root = window.decorView
        setButtonsEnabledRecursively(root, enabled)
    }

    private fun setButtonsEnabledRecursively(
        view: View,
        enabled: Boolean
    ) {
        if (view is TextView &&
            (view.text == "Download Missing Logos" ||
                    view.text == "Refresh All Logos")
        ) {
            view.isEnabled = enabled
            view.alpha = if (enabled) 1f else 0.5f
        }

        if (view is android.view.ViewGroup) {
            for (i in 0 until view.childCount) {
                setButtonsEnabledRecursively(view.getChildAt(i), enabled)
            }
        }
    }

    private fun refreshLastUpdatedText() {
        val timestamp = preferences.getLong("last_logo_update_attempt", 0L)

        lastUpdatedText.text = if (timestamp == 0L) {
            "Last logo update attempt: Never"
        } else {
            val formatted = SimpleDateFormat(
                "MMM d, yyyy 'at' h:mm a",
                Locale.getDefault()
            ).format(Date(timestamp))

            "Last logo update attempt: $formatted"
        }
    }

    // What the system says the Home button opens right now.
    private fun refreshHomeStatus() {

        val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)

        @Suppress("DEPRECATION")
        val resolved = packageManager.resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY)
        val owner = resolved?.activityInfo?.packageName

        homeStatusText.text = when {
            owner == packageName -> "Home button: opens Sports Hub."
            owner == null || owner == "android" ->
                "Home button: no default home app is set (the TV asks which to use)."
            else -> "Home button: currently opens $owner"
        }
    }

    private fun requestHomeRole() {

        val roleManager = getSystemService(RoleManager::class.java)

        if (roleManager == null || !roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) {
            showHomeHelp()
            return
        }

        if (roleManager.isRoleHeld(RoleManager.ROLE_HOME)) {
            Toast.makeText(this, "Sports Hub is already your home screen.", Toast.LENGTH_LONG).show()
            return
        }

        try {
            startActivityForResult(roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME), 1001)
        } catch (e: Exception) {
            showHomeHelp()
        }
    }

    // Many TVs don't let apps pick the home screen themselves.
    private fun showHomeHelp() {
        AlertDialog.Builder(this)
            .setTitle("Set as home screen")
            .setMessage(
                "This TV doesn't let apps change the home screen directly. " +
                    "From a computer, with ADB debugging on, run:\n\n" +
                    "adb shell cmd package set-home-activity com.dante.sportshuub/.MainActivity"
            )
            .setPositiveButton("OK", null)
            .show()
    }

    private fun exitToTvHomeMenu() {

        LauncherMode.markExited(this)

        if (LauncherMode.openSystemHome(this)) {
            finishAffinity()
        } else {
            LauncherMode.clearExited(this)
            Toast.makeText(this, "No other TV home menu was found on this device.", Toast.LENGTH_LONG).show()
        }
    }

    private fun refreshFavoritesText() {
        val leagues = FavoritesStore.getFavoriteLeagues(this).size
        val teams = FavoritesStore.getFavoriteTeams(this).size
        val conferences = FavoritesStore.getFavoriteConferences(this).size
        favoritesText.text =
            "Competitions: $leagues    Teams: $teams    Conferences: $conferences"
    }

    private fun sectionTitle(text: String): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = 17f
            setTextColor(gray)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, dp(14), 0, dp(10))
        }
    }

    private fun makeButton(
        text: String,
        action: () -> Unit
    ): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = 17f
            setTextColor(white)
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), 0, dp(16), 0)
            isFocusable = true
            isClickable = true

            background = GradientDrawable().apply {
                setColor(cardColor)
                cornerRadius = dp(9).toFloat()
            }

            setOnFocusChangeListener { view, hasFocus ->
                view.alpha = if (hasFocus) 0.85f else 1f
            }

            setOnClickListener {
                action()
            }

            contentDescription = text

            layoutParams = LinearLayout.LayoutParams(
                -1,
                dp(54)
            ).apply {
                bottomMargin = dp(10)
            }
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
}
