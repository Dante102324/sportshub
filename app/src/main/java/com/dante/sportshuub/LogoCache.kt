package com.dante.sportshuub

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object LogoCache {

    private const val LOGO_FOLDER = "team_logos"

    /**
     * Returns the locally cached logo for a team.
     * Returns null if the logo has not been downloaded yet.
     */
    fun getCachedLogo(
        context: Context,
        team: Team
    ): Bitmap? {

        val file = getLogoFile(context, team)

        android.util.Log.d(
            "LogoCache",
            "Checking ${team.name}: ${file.absolutePath} exists=${file.exists()}"
        )

        if (!file.exists()) {
            return null
        }

        return BitmapFactory.decodeFile(file.absolutePath)
    }
    /**
     * Downloads a team's logo and saves it locally.
     */
    suspend fun downloadLogo(
        context: Context,
        team: Team,
        logoUrl: String
    ): Bitmap? = withContext(Dispatchers.IO) {

        try {

            val connection =
                URL(logoUrl).openConnection() as HttpURLConnection

            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            connection.requestMethod = "GET"

            connection.connect()

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {

                android.util.Log.e(
                    "LogoCache",
                    "Failed to download ${team.name}. HTTP ${connection.responseCode}"
                )

                connection.disconnect()
                return@withContext null
            }

            val bitmap =
                BitmapFactory.decodeStream(connection.inputStream)

            connection.inputStream.close()
            connection.disconnect()

            if (bitmap != null) {

                val file = getLogoFile(context, team)

                android.util.Log.d(
                    "LogoCache",
                    "Saving ${team.name} to ${file.absolutePath}"
                )

                FileOutputStream(file).use { output ->

                    bitmap.compress(
                        Bitmap.CompressFormat.PNG,
                        100,
                        output
                    )
                }

                android.util.Log.d(
                    "LogoCache",
                    "Saved ${team.name}: exists=${file.exists()}, size=${file.length()} bytes"
                )
            } else {

                android.util.Log.e(
                    "LogoCache",
                    "Bitmap was null for ${team.name}"
                )
            }

            bitmap

        } catch (e: Exception) {

            android.util.Log.e(
                "LogoCache",
                "Exception downloading ${team.name}",
                e
            )

            null
        }
    }

    /**
     * Deletes the cached logo for a team.
     */
    fun deleteLogo(
        context: Context,
        team: Team
    ) {

        val file = getLogoFile(context, team)

        if (file.exists()) {
            file.delete()
        }
    }

    /**
     * Deletes every cached team logo.
     */
    fun clearCache(context: Context) {

        val folder =
            File(
                context.filesDir,
                LOGO_FOLDER
            )

        if (folder.exists()) {
            folder.deleteRecursively()
        }
    }

    /**
     * Gets the file where a team's logo is stored.
     */
    private fun getLogoFile(
        context: Context,
        team: Team
    ): File {

        val folder =
            File(
                context.filesDir,
                LOGO_FOLDER
            )

        if (!folder.exists()) {
            folder.mkdirs()
        }

        return File(
            folder,
            "${team.id}.png"
        )
    }
}