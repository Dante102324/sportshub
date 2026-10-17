package com.dante.sportshuub

import android.content.Context
import android.widget.ImageView

// Loads a team logo into an ImageView, using the logo cache.
object TeamLogos {

    fun load(
        context: Context,
        leagueDisplayName: String,
        sport: String,
        abbreviation: String,
        logoUrl: String,
        target: ImageView
    ) {
        // Prefer your existing cached team (works for the NBA names you already have).
        val known = TeamRepository.getTeam(leagueDisplayName, abbreviation)

        // Otherwise build a team from the logo URL that ESPN supplied.
        val team = known ?: if (logoUrl.startsWith("http")) {
            val safeKey = "${leagueDisplayName}_$abbreviation"
                .lowercase()
                .replace(Regex("[^a-z0-9]+"), "_")

            Team(
                id = safeKey,
                name = abbreviation,
                abbreviation = abbreviation,
                sport = sport,
                league = leagueDisplayName,
                logoFileName = "espn_$safeKey",
                logoUrl = logoUrl
            )
        } else {
            null
        }

        if (team != null) {
            LogoManager.loadLogo(context, team) { bitmap ->
                if (bitmap != null) {
                    target.setImageBitmap(bitmap)
                }
            }
        }
    }
}
