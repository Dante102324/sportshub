package com.dante.sportshuub

import android.content.Context
import android.graphics.Bitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object LogoManager {

    fun loadLogo(
        context: Context,
        team: Team,
        onLogoLoaded: (Bitmap?) -> Unit
    ) {

        // Check local cache first
        val cachedLogo =
            LogoCache.getCachedLogo(context, team)

        if (cachedLogo != null) {

            onLogoLoaded(cachedLogo)
            return
        }

        // No local logo, so download it
        if (team.logoUrl.isEmpty()) {

            onLogoLoaded(null)
            return
        }

        CoroutineScope(Dispatchers.IO).launch {

            val downloadedLogo =
                LogoCache.downloadLogo(
                    context,
                    team,
                    team.logoUrl
                )

            withContext(Dispatchers.Main) {

                onLogoLoaded(downloadedLogo)
            }
        }
    }
}