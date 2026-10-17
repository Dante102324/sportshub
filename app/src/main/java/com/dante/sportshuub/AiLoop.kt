package com.dante.sportshuub

import org.json.JSONArray
import org.json.JSONObject

// Runs one question: the model may ask for tools (standings, rankings, game
// details), we run them, and it then writes the final answer.
object AiLoop {

    private const val MAX_ROUNDS = 4

    fun run(
        messages: JSONArray,
        model: String,
        tools: JSONArray,
        post: (JSONObject) -> JSONObject,
        executeTool: (name: String, arguments: String) -> String
    ): String {

        var useTools = true
        var round = 0

        while (round < MAX_ROUNDS) {

            val body = AiParsing.buildRequestBody(
                model,
                messages,
                if (useTools) tools else null
            )

            val response = try {
                post(body)
            } catch (e: AiHttpException) {
                // Some free models stumble on tool calls: retry once without tools.
                if (useTools && e.code == 400) {
                    useTools = false
                    continue
                }
                throw AiException(AiParsing.friendlyError(e.code, e.body))
            }

            val message = AiParsing.parseAssistantMessage(response)

            if (message.toolCalls.isEmpty()) {
                if (message.content.isBlank()) {
                    throw AiException("I didn't get an answer back. Try asking another way.")
                }
                return AiParsing.cleanForDisplay(message.content)
            }

            messages.put(AiParsing.assistantToolCallMessage(message))

            for (call in message.toolCalls) {
                val result = try {
                    executeTool(call.name, call.arguments)
                } catch (e: Exception) {
                    "Error: ${e.message ?: "that lookup failed"}"
                }
                messages.put(AiParsing.toolResultMessage(call.id, result))
            }

            round++
        }

        // Too many lookups: ask for an answer using what we have.
        val last = try {
            post(AiParsing.buildRequestBody(model, messages, null))
        } catch (e: AiHttpException) {
            throw AiException(AiParsing.friendlyError(e.code, e.body))
        }

        val text = AiParsing.parseAssistantMessage(last).content

        if (text.isBlank()) {
            throw AiException("I didn't get an answer back. Try asking another way.")
        }
        return AiParsing.cleanForDisplay(text)
    }
}
