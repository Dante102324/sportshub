package com.dante.sportshuub

import org.json.JSONArray
import org.json.JSONObject

data class ChatTurn(
    val role: String,   // "user" or "assistant"
    val text: String
)

class AiException(message: String) : Exception(message)

// The server answered with an error status.
class AiHttpException(val code: Int, val body: String) : Exception("HTTP $code")

data class ToolCall(
    val id: String,
    val name: String,
    val arguments: String   // JSON text
)

data class AssistantMessage(
    val content: String,
    val toolCalls: List<ToolCall>
)

// Request/response format for OpenAI-compatible "chat completions" servers
// (Groq, OpenRouter, Mistral, Gemini's compatibility endpoint, ...). Kept free
// of Android classes.
object AiParsing {

    fun buildMessages(instructions: String, turns: List<ChatTurn>): JSONArray {

        val messages = JSONArray()

        messages.put(
            JSONObject().put("role", "system").put("content", instructions)
        )

        for (turn in turns) {
            messages.put(
                JSONObject().put("role", turn.role).put("content", turn.text)
            )
        }
        return messages
    }

    fun buildRequestBody(
        model: String,
        messages: JSONArray,
        tools: JSONArray?
    ): JSONObject {

        val body = JSONObject()
            .put("model", model)
            .put("messages", messages)
            .put("temperature", 0.3)

        if (tools != null && tools.length() > 0) {
            body.put("tools", tools)
            body.put("tool_choice", "auto")
        }
        return body
    }

    fun parseAssistantMessage(json: JSONObject): AssistantMessage {

        val message = json.optJSONArray("choices")
            ?.optJSONObject(0)
            ?.optJSONObject("message")
            ?: return AssistantMessage("", emptyList())

        val content = if (message.isNull("content")) "" else message.optString("content")

        val calls = mutableListOf<ToolCall>()
        val array = message.optJSONArray("tool_calls")

        if (array != null) {
            for (i in 0 until array.length()) {
                val call = array.optJSONObject(i) ?: continue
                val function = call.optJSONObject("function") ?: continue
                val name = function.optString("name")
                if (name.isEmpty()) continue

                calls.add(
                    ToolCall(
                        id = call.optString("id").ifEmpty { "call_$i" },
                        name = name,
                        arguments = function.optString("arguments").ifEmpty { "{}" }
                    )
                )
            }
        }
        return AssistantMessage(content, calls)
    }

    // The assistant's request to run tools, as it must be sent back.
    fun assistantToolCallMessage(message: AssistantMessage): JSONObject {

        val calls = JSONArray()

        for (call in message.toolCalls) {
            calls.put(
                JSONObject()
                    .put("id", call.id)
                    .put("type", "function")
                    .put(
                        "function",
                        JSONObject()
                            .put("name", call.name)
                            .put("arguments", call.arguments)
                    )
            )
        }

        return JSONObject()
            .put("role", "assistant")
            .put("content", JSONObject.NULL)
            .put("tool_calls", calls)
    }

    fun toolResultMessage(id: String, content: String): JSONObject =
        JSONObject()
            .put("role", "tool")
            .put("tool_call_id", id)
            .put("content", content)

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
            401 -> "The AI service rejected the API key (401). Check AI_API_KEY in local.properties."
            429 -> "The free usage limit was reached (429). Wait a little while and try again."
            else -> "AI service error $code" + if (detail.isNotEmpty()) ": $detail" else ""
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
