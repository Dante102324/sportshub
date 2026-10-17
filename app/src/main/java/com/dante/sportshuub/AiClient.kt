package com.dante.sportshuub

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Talks to any OpenAI-compatible chat server (Groq by default, which has a
// free tier). The key, server and model come from local.properties, never from
// the source code (see app/build.gradle.kts).
object AiClient {

    fun isConfigured(): Boolean = BuildConfig.AI_API_KEY.isNotBlank()

    private fun instructions(liveContext: String): String {

        val now = SimpleDateFormat("EEEE, MMMM d, yyyy 'at' h:mm a z", Locale.US)
            .format(Date())

        return """
You are the sports assistant inside a TV sports app. The viewer asks questions out loud or by typing, and your answer may be read aloud.
Answer in one to four short sentences of plain text: no markdown, no lists, no links, no emojis. Say full team names instead of abbreviations.
Current date and time: $now.
Use the live scoreboard below first for scores, schedules and what is on today. You can call tools for standings, college rankings and game details (box score, stats, leaders); use them instead of guessing.
You cannot browse the web. For other questions (history, players, rules) answer from your own knowledge, mention that it may be out of date when it concerns recent events, and never invent scores, records or stats. If you don't know, say so.
If a question is not about sports, politely steer the viewer back to sports.

$liveContext
""".trim()
    }

    // Blocking. Call off the main thread.
    fun ask(turns: List<ChatTurn>, liveContext: String): String {

        if (!isConfigured()) {
            throw AiException("The AI isn't set up yet. Add AI_API_KEY to local.properties.")
        }

        val messages = AiParsing.buildMessages(instructions(liveContext), turns)

        return AiLoop.run(
            messages = messages,
            model = BuildConfig.AI_MODEL,
            tools = AiTools.definitions(),
            post = { body -> post(body) },
            executeTool = { name, arguments -> AiTools.execute(name, arguments) }
        )
    }

    private fun post(body: JSONObject): JSONObject {

        val url = BuildConfig.AI_BASE_URL.trimEnd('/') + "/chat/completions"
        val conn = URL(url).openConnection() as HttpURLConnection

        try {
            conn.requestMethod = "POST"
            conn.connectTimeout = 15_000
            conn.readTimeout = 60_000
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Authorization", "Bearer ${BuildConfig.AI_API_KEY}")

            conn.outputStream.use {
                it.write(body.toString().toByteArray(Charsets.UTF_8))
            }

            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() } ?: ""

            if (code !in 200..299) {
                throw AiHttpException(code, text)
            }

            return JSONObject(text)

        } finally {
            conn.disconnect()
        }
    }
}
