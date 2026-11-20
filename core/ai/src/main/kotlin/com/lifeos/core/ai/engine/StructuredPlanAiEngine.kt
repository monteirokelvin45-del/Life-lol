package com.lifeos.core.ai.engine

import com.lifeos.core.ai.schema.LifeOsExtractionSchema
import com.lifeos.core.domain.model.AiExtractedPlanPayload
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Motor de IA com Structured JSON Output via Ktor (Pilar 5) + fallback determinístico offline.
 */
@Singleton
class StructuredPlanAiEngine @Inject constructor(
    private val httpClient: HttpClient,
    private val json: Json
) {

    suspend fun extractStructuredPlan(
        documentText: String,
        apiKey: String? = null,
        endpointUrl: String = DEFAULT_CHAT_COMPLETIONS_URL,
        modelName: String = DEFAULT_MODEL_NAME
    ): AiExtractedPlanPayload = withContext(Dispatchers.IO) {
        if (documentText.isBlank()) {
            return@withContext AiExtractedPlanPayload()
        }

        // Caso haja chave de API configurada, aciona LLM com response_format json_object estrito
        if (!apiKey.isNullOrBlank()) {
            val remoteResult = runCatching {
                callRemoteStructuredLlm(
                    documentText = documentText,
                    apiKey = apiKey,
                    endpointUrl = endpointUrl,
                    modelName = modelName
                )
            }.getOrNull()

            if (remoteResult != null) {
                return@withContext remoteResult
            }
        }

        // Fallback Offline-First determinístico instantâneo
        LocalRuleBasedPlanParser.parse(documentText)
    }

    private suspend fun callRemoteStructuredLlm(
        documentText: String,
        apiKey: String,
        endpointUrl: String,
        modelName: String
    ): AiExtractedPlanPayload {
        val requestPayload = ChatCompletionRequest(
            model = modelName,
            responseFormat = ResponseFormatSpec(type = "json_object"),
            messages = listOf(
                ChatMessage(role = "system", content = LifeOsExtractionSchema.buildSystemPrompt()),
                ChatMessage(role = "user", content = documentText)
            )
        )

        val response = httpClient.post(endpointUrl) {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $apiKey")
            setBody(requestPayload)
        }

        val rawBody = response.bodyAsText()
        val parsedEnvelope = json.decodeFromString(ChatCompletionResponse.serializer(), rawBody)
        val contentJson = parsedEnvelope.choices.firstOrNull()?.message?.content.orEmpty()
        return json.decodeFromString(AiExtractedPlanPayload.serializer(), contentJson)
    }

    @Serializable
    private data class ChatCompletionRequest(
        val model: String,
        @SerialName("response_format") val responseFormat: ResponseFormatSpec,
        val messages: List<ChatMessage>,
        val temperature: Double = 0.1
    )

    @Serializable
    private data class ResponseFormatSpec(
        val type: String
    )

    @Serializable
    private data class ChatMessage(
        val role: String,
        val content: String
    )

    @Serializable
    private data class ChatCompletionResponse(
        val choices: List<ChoiceItem> = emptyList()
    )

    @Serializable
    private data class ChoiceItem(
        val message: ChatMessage
    )

    companion object {
        private const val DEFAULT_CHAT_COMPLETIONS_URL = "https://api.openai.com/v1/chat/completions"
        private const val DEFAULT_MODEL_NAME = "gpt-4o-mini"
    }
}
