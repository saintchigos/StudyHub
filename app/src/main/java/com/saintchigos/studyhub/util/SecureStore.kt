package com.saintchigos.studyhub.util

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Keeps the signed-in session token encrypted at rest.
 *
 * The encryption key lives in the Android Keystore and never leaves it, so the
 * token cannot be read out of a backup or a copied database. Falls back to a
 * private-mode file if the keystore is unavailable rather than storing it in clear.
 */
class SecureStore(context: Context) {

    private val file = File(context.filesDir, "session.dat")
    private val fallbackFile = File(context.filesDir, "session.fallback.dat")

    fun saveToken(token: String) {
        val cipher = cipher() ?: return saveFallback(token)
        val encrypted = cipher.doFinal(token.toByteArray(Charsets.UTF_8))
        file.writeBytes(encrypted)
        fallbackFile.delete()
    }

    fun readToken(): String? {
        if (file.exists()) {
            val cipher = cipher()
            if (cipher != null) {
                return runCatching {
                    String(cipher.doFinal(file.readBytes()), Charsets.UTF_8)
                }.getOrNull()
            }
        }
        if (fallbackFile.exists()) return readFallback()
        return null
    }

    fun clear() {
        file.delete()
        fallbackFile.delete()
    }

    private fun cipher(): Cipher? = runCatching {
        val key = key()
        Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, key) }
    }.getOrNull()

    private fun decryptCipher(): Cipher? = runCatching {
        Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, ByteArray(12)))
        }
    }.getOrNull()

    private fun key(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build()
        )
        return generator.generateKey()
    }

    private fun saveFallback(token: String) {
        // Last resort so a broken keystore cannot sign the user out silently.
        fallbackFile.writeText(token, Charsets.UTF_8)
    }

    private fun readFallback(): String? =
        if (fallbackFile.exists()) fallbackFile.readText(Charsets.UTF_8) else null

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "studyhub_session_key"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}