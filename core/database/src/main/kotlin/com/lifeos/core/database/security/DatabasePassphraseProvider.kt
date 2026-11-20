package com.lifeos.core.database.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.File
import java.nio.ByteBuffer
import java.security.KeyStore
import java.security.SecureRandom
import java.util.Arrays
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Gerenciador criptográfico da passphrase AES-256 do SQLCipher apoiado no Android Keystore.
 *
 * Arquitetura de Segurança em Repouso (Pilar 4):
 * 1. Gera uma chave aleatória de 256 bits (32 bytes) via CSPRNG (`SecureRandom`).
 * 2. Envelopa (AES-256-GCM, tag de autenticação de 128 bits) usando uma Master Key
 *    não-exportável residente no TEE/StrongBox do `AndroidKeyStore`.
 * 3. Armazena apenas `[ivLength (1 byte) | IV (12 bytes) | Ciphertext+Tag]` em `noBackupFilesDir`,
 *    impedindo exfiltração por ADB backup ou nuvem.
 * 4. Nunca converte a chave em `java.lang.String`, retornando `ByteArray` zerável em RAM.
 */
class DatabasePassphraseProvider(
    private val context: Context
) {

    @Synchronized
    fun getOrCreatePassphrase(): ByteArray {
        val blobFile = File(context.noBackupFilesDir, ENCRYPTED_KEY_FILENAME)
        val masterKey = getOrCreateHardwareMasterKey()

        if (blobFile.exists() && blobFile.length() > MIN_BLOB_BYTES) {
            return decryptPassphrase(blobFile.readBytes(), masterKey)
        }

        val freshPassphrase = ByteArray(PASSPHRASE_SIZE_BYTES)
        SecureRandom().nextBytes(freshPassphrase)

        val encryptedBlob = encryptPassphrase(freshPassphrase, masterKey)
        blobFile.parentFile?.mkdirs()
        blobFile.writeBytes(encryptedBlob)

        return freshPassphrase
    }

    private fun getOrCreateHardwareMasterKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE_PROVIDER).apply {
            load(null)
        }

        val existingEntry = keyStore.getEntry(KEYSTORE_ALIAS, null) as? KeyStore.SecretKeyEntry
        if (existingEntry != null) {
            return existingEntry.secretKey
        }

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE_PROVIDER
        )

        val spec = KeyGenParameterSpec.Builder(
            KEYSTORE_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(AES_KEY_SIZE_BITS)
            .setRandomizedEncryptionRequired(true)
            .build()

        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    private fun encryptPassphrase(plainPassphrase: ByteArray, masterKey: SecretKey): ByteArray {
        val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, masterKey)
        val iv = cipher.iv
        val cipherText = cipher.doFinal(plainPassphrase)

        return ByteBuffer.allocate(1 + iv.size + cipherText.size)
            .put(iv.size.toByte())
            .put(iv)
            .put(cipherText)
            .array()
    }

    private fun decryptPassphrase(blob: ByteArray, masterKey: SecretKey): ByteArray {
        val buffer = ByteBuffer.wrap(blob)
        val ivLength = buffer.get().toInt() and 0xFF
        require(ivLength in 12..16 && blob.size > 1 + ivLength) {
            "Envelope criptográfico do banco Life OS inválido ou corrompido."
        }

        val iv = ByteArray(ivLength)
        buffer.get(iv)

        val cipherText = ByteArray(buffer.remaining())
        buffer.get(cipherText)

        return try {
            val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, masterKey, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
            cipher.doFinal(cipherText)
        } finally {
            Arrays.fill(iv, 0.toByte())
            Arrays.fill(cipherText, 0.toByte())
        }
    }

    companion object {
        private const val ANDROID_KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val KEYSTORE_ALIAS = "com.lifeos.security.sqlcipher.master_key.v1"
        private const val ENCRYPTED_KEY_FILENAME = "lifeos_sqlcipher_keyset.bin"
        private const val AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding"
        private const val AES_KEY_SIZE_BITS = 256
        private const val GCM_TAG_LENGTH_BITS = 128
        private const val PASSPHRASE_SIZE_BYTES = 32
        private const val MIN_BLOB_BYTES = 29 // 1 (ivLen) + 12 (IV) + 16 (GCM tag)
    }
}
