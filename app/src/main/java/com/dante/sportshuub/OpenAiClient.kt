package com.dante.sportshuub

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Talks to OpenAI's Responses API. The key and model come from
// local.properties (see app/build.gradle.kts), never from the source code.
object OpenAiClient {

    private const val URL_RESPONSES = "https://api.openai.com/v1/responses"

    fun isConfigured(): Boolean = BuildConfig.OPENAI_API_KEY.isNotBlank()

    private fun instructions(liveContext: String): String {

        val now = SimpleDateFormat("EEEE, MMMM d, yyyy 'at' h:mm a z", Locale.US)
            .format(Date())

        return """
You are the sports assistant inside a TV sports app. The viewer asks questions out loud or by typing, and your answer may be read aloud.
Answer in one to four short sentences of plain text: no markdown, no lists, no links, no emojis. Say full team names instead of abbreviations.
Current date and time: $now.
Use the live scoreboard below first for scores, schedules and what is on today. For anything else (standings, player and team stats, injuries, news, history) use web search when the answer could have changed recently, and say so if you are not sure.
If a question is not about sports, politely steer the viewer back to sports.

$liveContext
""".trim()
    }

    // Blocking. Call off the main thread.
    fun ask(turns: List<ChatTurn>, liveContext: String): String {

        if (!isConfigured()) {
            throw AiException("The AI isn't set up yet. Add OPENAI_API_KEY to local.properties.")
        }

        val body = AiParsing.buildRequestBody(
            BuildConfig.OPENAI_MODEL,
            instructions(liveContext),
            turns
        )

        val conn = URL(URL_RESPONSES).openConnection() as HttpURLConnection

        try {
            conn.requestMethod = "POST"
            conn.connectTimeout = 15_000
            conn.readTimeout = 90_000   // web search can take a while
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Authorization", "Bearer ${BuildConfig.OPENAI_API_KEY}")

            conn.outputStream.use {
                it.write(body.toString().toByteArray(Charsets.UTF_8))
            }

            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() } ?: ""

            if (code !in 200..299) {
                throw AiException(AiParsing.friendlyError(code, text))
            }

            val answer = AiParsing.parseResponseText(JSONObject(text))

            if (answer.isBlank()) {
                throw AiException("I didn't get an answer back. Try asking another way.")
            }

            return AiParsing.cleanForDisplay(answer)

        } finally {
            conn.disconnect()
        }
    }
}
