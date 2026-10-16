package com.dante.sportshuub

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object TeamLogoUpdater {

    private const val TAG = "TeamLogoUpdater"

    private var updateJob: Job? = null

    fun updateMissingLogos(
        context: Context,
        onProgress: ((completed: Int, total: Int) -> Unit)? = null,
        onComplete: (() -> Unit)? = null
    ) {

        if (updateJob?.isActive == true) {
            Log.d(TAG, "Logo update already running")
            return
        }

        val appContext = context.applicationContext

        updateJob = CoroutineScope(Dispatchers.IO).launch {

            try {

                val teams = TeamRepository.getAllTeams()

                val teamsNeedingLogos = teams.filter { team ->

                    LogoCache.getCachedLogo(
                        appContext,
                        team
                    ) == null &&
                            team.logoUrl.isNotEmpty()
                }

                val total = teamsNeedingLogos.size

                Log.d(
                    TAG,
                    "$total logos need to be downloaded"
                )

                if (total == 0) {

                    Log.d(
                        TAG,
                        "All team logos are already cached"
                    )

                    return@launch
                }

                var completed = 0

                coroutineScope {

                    teamsNeedingLogos.map { team ->

                        launch {

                            try {

                                LogoCache.downloadLogo(
                                    appContext,
                                    team,
                                    team.logoUrl
                                )

                                synchronized(this@TeamLogoUpdater) {

                                    completed++

                                    Log.d(
                                        TAG,
                                        "Downloaded ${team.name} ($completed/$total)"
                                    )

                                    onProgress?.invoke(
                                        completed,
                                        total
                                    )
                                }

                            } catch (e: Exception) {

                                Log.e(
                                    TAG,
                                    "Failed to download ${team.name}",
                                    e
                                )
                            }
                        }

                    }.joinAll()
                }

            } catch (e: Exception) {

                Log.e(
                    TAG,
                    "Logo update failed",
                    e
                )

            } finally {

                withContext(Dispatchers.Main) {
                    onComplete?.invoke()
                }
            }
        }
    }

    fun updateAllLogos(
        context: Context,
        onProgress: ((completed: Int, total: Int) -> Unit)? = null,
        onComplete: (() -> Unit)? = null
    ) {

        if (updateJob?.isActive == true) {
            Log.d(TAG, "Logo update already running")
            return
        }

        val appContext = context.applicationContext

        updateJob = CoroutineScope(Dispatchers.IO).launch {

            try {

                val teams = TeamRepository.getAllTeams()

                val total = teams.size

                var completed = 0

                coroutineScope {

                    teams.map { team ->

                        launch {

                            try {

                                LogoCache.downloadLogo(
                                    appContext,
                                    team,
                                    team.logoUrl
                                )

                                synchronized(this@TeamLogoUpdater) {

                                    completed++

                                    onProgress?.invoke(
                                        completed,
                                        total
                                    )
                                }

                            } catch (e: Exception) {

                                Log.e(
                                    TAG,
                                    "Failed to update ${team.name}",
                                    e
                                )
                            }
                        }

                    }.joinAll()
                }

                Log.d(
                    TAG,
                    "Finished updating all $total logos"
                )

            } catch (e: Exception) {

                Log.e(
                    TAG,
                    "Full logo update failed",
                    e
                )

            } finally {

                withContext(Dispatchers.Main) {
                    onComplete?.invoke()
                }
            }
        }
    }

    fun cancelUpdate() {

        updateJob?.cancel()

        updateJob = null

        Log.d(
            TAG,
            "Logo update cancelled"
        )
    }
}