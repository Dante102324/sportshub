package com.dante.sportshuub

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object TimeFormat {

    private val patterns = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy-MM-dd'T'HH:mm'Z'",
        "yyyy-MM-dd"
    )

    private fun parse(iso: String): Date? {
        for (pattern in patterns) {
            try {
                val parser = SimpleDateFormat(pattern, Locale.US)
                parser.timeZone = TimeZone.getTimeZone("UTC")
                return parser.parse(iso) ?: continue
            } catch (e: Exception) {
                // try the next pattern
            }
        }
        return null
    }

    // "Sat, Oct 18  7:30 PM" in the device's time zone.
    fun dateTime(iso: String): String {
        val date = parse(iso) ?: return iso
        return SimpleDateFormat("EEE, MMM d  h:mm a", Locale.getDefault()).format(date)
    }

    // "Oct 18". Game logs use midnight-UTC dates, so keep the UTC calendar day.
    fun shortDateUtc(iso: String): String {
        val date = parse(iso) ?: return iso
        val format = SimpleDateFormat("MMM d", Locale.getDefault())
        format.timeZone = TimeZone.getTimeZone("UTC")
        return format.format(date)
    }
}
