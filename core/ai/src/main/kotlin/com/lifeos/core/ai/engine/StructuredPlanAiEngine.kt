package com.lifeos.core.ai.engine

import com.lifeos.core.ai.schema.LifeOsExtractionSchema
import com.lifeos.core.ai.security.AiCredentialsStore
import com.lifeos.core.ai.security.AiLlmProvider
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
 * Motor Híbrido de IA do Life OS (Pilar 5):
 * 1. Cloud LLM com JSON Schema estrito (Google Gemini 2.0 Flash / Groq Llama 3.3 70B / OpenAI GPT-4o-mini)
 *    quando o utilizador configura sua chave no Cofre Keystore.
 * 2. Motor Semântico Local + Google ML Kit OCR (100% Offline, latência <5ms, custo zero)
 *    como modo padrão imediato e fallback resiliente sem internet.
 */
@Singleton
class StructuredPlanAiEngine @Inject constructor(
    private val httpClient: HttpClient,
    private val json: Json,
    private val credentialsStore: AiCredentialsStore
) {

    suspend fun extractStructuredPlan(
        documentText: String,
        overrideApiKey: String? = null
    ): AiExtractedPlanPayload = withContext(Dispatchers.IO) {
        if (documentText.isBlank()) {
            return@withContext AiExtractedPlanPayload()
        }

        val savedConfig = credentialsStore.loadConfig()
        val effectiveKey = overrideApiKey?.takeIf { it.isNotBlank() } ?: savedConfig.apiKey
        val activeProvider = if (!overrideApiKey.isNullOrBlank() && savedConfig.provider == AiLlmProvider.OFFLINE_LOCAL) {
            AiLlmProvider.GOOGLE_GEMINI
        } else {
            savedConfig.provider
        }

        if (activeProvider != AiLlmProvider.OFFLINE_LOCAL && effectiveKey.isNotBlank()) {
            val remoteResult = runCatching {
                callRemoteStructuredLlm(
                    documentText = documentText,
                    apiKey = effectiveKey,
                    endpointUrl = activeProvider.endpointUrl,
                    modelName = activeProvider.defaultModel
                )
            }.getOrNull()

            if (remoteResult != null && !isEmptyPayload(remoteResult)) {
                return@withContext remoteResult
            }
        }

        LocalRuleBasedPlanParser.parse(documentText)
    }

    private fun isEmptyPayload(p: AiExtractedPlanPayload): Boolean {
        return p.detectedHabits.isEmpty() &&
            p.detectedWorkouts.isEmpty() &&
            p.detectedStudies.isEmpty() &&
            p.detectedFinances.isEmpty()
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
        val rawContent = parsedEnvelope.choices.firstOrNull()?.message?.content.orEmpty().trim()
        val cleanedJson = rawContent
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
        return json.decodeFromString(AiExtractedPlanPayload.serializer(), cleanedJson)
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
}
