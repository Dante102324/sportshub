package com.dante.sportshuub

import org.json.JSONArray
import org.json.JSONObject

data class ChatTurn(
    val role: String,   // "user" or "assistant"
    val text: String
)

class AiException(message: String) : Exception(message)

// Everything about the OpenAI request/response format, kept free of Android
// classes so it is easy to reason about.
object AiParsing {

    fun buildRequestBody(
        model: String,
        instructions: String,
        turns: List<ChatTurn>
    ): JSONObject {

        val input = JSONArray()

        for (turn in turns) {
            input.put(
                JSONObject()
                    .put("role", turn.role)
                    .put("content", turn.text)
            )
        }

        val tools = JSONArray().put(JSONObject().put("type", "web_search"))

        return JSONObject()
            .put("model", model)
            .put("instructions", instructions)
            .put("input", input)
            .put("tools", tools)
    }

    // The Responses API returns a list of output items; the answer is the text
    // of the "message" items (web-search calls appear as separate items).
    fun parseResponseText(json: JSONObject): String {

        val output = json.optJSONArray("output") ?: return ""
        val parts = mutableListOf<String>()

        for (i in 0 until output.length()) {
            val item = output.optJSONObject(i) ?: continue
            if (item.optString("type") != "message") continue

            val content = item.optJSONArray("content") ?: continue

            for (j in 0 until content.length()) {
                val piece = content.optJSONObject(j) ?: continue
                if (piece.optString("type") == "output_text") {
                    val text = piece.optString("text")
                    if (text.isNotBlank()) parts.add(text)
                }
            }
        }
        return parts.joinToString("\n")
    }

    fun parseErrorMessage(body: String): String {
        return try {
            JSONObject(body).optJSONObject("error")?.optString("message").orEmpty()
        } catch (e: Exception) {
            ""
        }
    }

    fun friendlyError(code: Int, body: String): String {
        val detail = parseErrorMessage(body)

        return when (code) {
            401 -> "OpenAI rejected the API key (401). Check OPENAI_API_KEY in local.properties."
            429 -> "OpenAI says the account is rate limited or out of credit (429)."
            else -> "OpenAI error $code" + if (detail.isNotEmpty()) ": $detail" else ""
        }
    }

    private val citation = Regex("\\(\\s*\\[[^\\]]*\\]\\([^)]*\\)\\s*\\)")
    private val link = Regex("\\[([^\\]]*)\\]\\([^)]*\\)")
    private val heading = Regex("(?m)^\\s*#+\\s*")
    private val bullet = Regex("(?m)^\\s*[-*•]\\s+")

    // The screen shows plain text, so remove markdown and link markers.
    fun cleanForDisplay(text: String): String {
        var t = text
        t = citation.replace(t, "")
        t = link.replace(t, "\$1")
        t = t.replace("**", "").replace("__", "").replace("`", "")
        t = heading.replace(t, "")
        t = bullet.replace(t, "")
        t = t.replace(Regex("[ \\t]{2,}"), " ")
        t = t.replace(Regex("\\s+([.,;!?])"), "\$1")
        return t.trim()
    }
}
