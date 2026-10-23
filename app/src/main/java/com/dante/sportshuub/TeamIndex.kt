package com.dante.sportshuub

import android.util.Log

// A searchable list of every team in every competition, loaded once from ESPN
// and kept in memory.
object TeamIndex {

    class Entry(val league: LeagueInfo, val team: EspnTeamInfo)

    private val entries = mutableListOf<Entry>()

    @Volatile
    private var started = false

    @Volatile
    var loadedLeagues = 0
        private set

    val totalLeagues: Int get() = LeagueRegistry.leagues.size

    val isComplete: Boolean get() = loadedLeagues >= totalLeagues

    // Starts loading in the background (once). onProgress runs on that thread.
    fun ensureLoading(onProgress: () -> Unit) {

        synchronized(this) {
            if (started) return
            started = true
        }

        Thread {
            for (league in LeagueRegistry.leagues) {
                try {
                    val teams = EspnApi.fetchTeams(league)
                    synchronized(entries) {
                        teams.forEach { entries.add(Entry(league, it)) }
                    }
                } catch (e: Exception) {
                    Log.e("TeamIndex", "${league.key} teams failed: ${e.message}")
                }
                loadedLeagues++
                onProgress()
            }
        }.start()
    }

    fun search(query: String, limit: Int = 25): List<Entry> {

        val q = query.trim().lowercase()
        if (q.isEmpty()) return emptyList()

        val words = q.split(" ").filter { it.isNotEmpty() }

        val matches = synchronized(entries) {
            entries.filter { entry ->
                val name = entry.team.name.lowercase()
                words.all { name.contains(it) } ||
                    entry.team.abbreviation.lowercase() == q
            }
        }

        return matches
            .sortedWith(
                compareBy<Entry> { !it.team.name.lowercase().startsWith(q) }
                    .thenBy { it.team.name }
            )
            .take(limit)
    }
}
