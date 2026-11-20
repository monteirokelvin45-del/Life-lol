package com.lifeos.core.ai.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.ByteBuffer
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

enum class AiLlmProvider(
    val displayName: String,
    val endpointUrl: String,
    val defaultModel: String
) {
    OFFLINE_LOCAL(
        displayName = "Offline Local + ML Kit OCR (Sem Chave)",
        endpointUrl = "",
        defaultModel = "lifeos-local-nlp-v2"
    ),
    GOOGLE_GEMINI(
        displayName = "Google Gemini 2.0 Flash (AI Studio)",
        endpointUrl = "https://generativelanguage.googleapis.com/v1beta/openai/chat/completions",
        defaultModel = "gemini-2.0-flash"
    ),
    GROQ_CLOUD(
        displayName = "Groq Llama 3.3 70B (Ultra-Rápido)",
        endpointUrl = "https://api.groq.com/openai/v1/chat/completions",
        defaultModel = "llama-3.3-70b-versatile"
    ),
    OPENAI(
        displayName = "OpenAI GPT-4o-mini",
        endpointUrl = "https://api.openai.com/v1/chat/completions",
        defaultModel = "gpt-4o-mini"
    )
}

@Serializable
data class StoredAiConfig(
    val providerName: String = AiLlmProvider.OFFLINE_LOCAL.name,
    val apiKey: String = ""
) {
    val provider: AiLlmProvider
        get() = runCatching { AiLlmProvider.valueOf(providerName) }
            .getOrDefault(AiLlmProvider.OFFLINE_LOCAL)
}

/**
 * Cofre criptografado no Android Keystore (AES-256-GCM) para guardar a chave de API
 * de LLM (Gemini / Groq / OpenAI) sem jamais expô-la em texto plano no disco.
 */
@Singleton
class AiCredentialsStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val json: Json
) {

    @Synchronized
    fun loadConfig(): StoredAiConfig {
        val file = File(context.noBackupFilesDir, CONFIG_FILE_NAME)
        if (!file.exists() || file.length() <= 13) {
            return StoredAiConfig()
        }
        return runCatching {
            val plainBytes = decrypt(file.readBytes(), getOrCreateKey())
            json.decodeFromString(StoredAiConfig.serializer(), plainBytes.decodeToString())
        }.getOrDefault(StoredAiConfig())
    }

    @Synchronized
    fun saveConfig(provider: AiLlmProvider, apiKey: String) {
        val file = File(context.noBackupFilesDir, CONFIG_FILE_NAME)
        val payload = json.encodeToString(
            StoredAiConfig(
                providerName = provider.name,
                apiKey = apiKey.trim()
            )
        ).encodeToByteArray()
        file.parentFile?.mkdirs()
        file.writeBytes(encrypt(payload, getOrCreateKey()))
    }

    private fun getOrCreateKey(): SecretKey {
        val ks = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        val existing = ks.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
        if (existing != null) return existing.secretKey

        val kg = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
        kg.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return kg.generateKey()
    }

    private fun encrypt(plain: ByteArray, key: SecretKey): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv
        val enc = cipher.doFinal(plain)
        return ByteBuffer.allocate(1 + iv.size + enc.size)
            .put(iv.size.toByte())
            .put(iv)
            .put(enc)
            .array()
    }

    private fun decrypt(blob: ByteArray, key: SecretKey): ByteArray {
        val buf = ByteBuffer.wrap(blob)
        val ivLen = buf.get().toInt() and 0xFF
        val iv = ByteArray(ivLen).also { buf.get(it) }
        val enc = ByteArray(buf.remaining()).also { buf.get(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
        return cipher.doFinal(enc)
    }

    companion object {
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val KEY_ALIAS = "com.lifeos.security.ai.credentials.v1"
        private const val CONFIG_FILE_NAME = "lifeos_ai_config.bin"
    }
}
