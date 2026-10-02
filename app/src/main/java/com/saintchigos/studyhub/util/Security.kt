package com.saintchigos.studyhub.util

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Password and verification-code cryptography.
 *
 * Nothing secret is ever stored in plain text: passwords are stretched with
 * PBKDF2-HMAC-SHA256 and a per-user random salt, verification codes are hashed
 * before they touch the database, and comparisons are constant time so a wrong
 * guess cannot be timed character by character.
 *
 * These primitives are used identically by the on-device account store and by any
 * server the app is later pointed at, so a backend can re-derive the same values.
 */
object Security {

    private const val ALGORITHM = "PBKDF2WithHmacSHA256"
    const val ITERATIONS = 210_000
    private const val KEY_LENGTH_BITS = 256
    private const val SALT_BYTES = 16
    const val MIN_PASSWORD_LENGTH = 10

    private val random = SecureRandom()

    // ---- passwords ---------------------------------------------------------

    /**
     * Returns a self-describing string: `pbkdf2_sha256$iterations$salt$hash`.
     * Storing the iteration count means old hashes can be re-stretched later.
     */
    fun hashPassword(password: String): String {
        val salt = ByteArray(SALT_BYTES).also { random.nextBytes(it) }
        return hashPassword(password, salt, ITERATIONS)
    }

    fun hashPassword(password: String, salt: ByteArray, iterations: Int): String {
        val spec = PBEKeySpec(password.toCharArray(), salt, iterations, KEY_LENGTH_BITS)
        val bytes = try {
            SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
        return listOf(
            "pbkdf2_sha256",
            iterations.toString(),
            Base64.getUrlEncoder().withoutPadding().encodeToString(salt),
            Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
        ).joinToString("$")
    }

    /** Constant-time verification of a password against a stored hash. */
    fun verifyPassword(password: String, stored: String): Boolean {
        val parts = stored.split("$")
        if (parts.size != 4 || parts[0] != "pbkdf2_sha256") return false
        val iterations = parts[1].toIntOrNull() ?: return false
        val salt = runCatching { Base64.getUrlDecoder().decode(parts[2]) }.getOrNull() ?: return false
        val candidate = runCatching { hashPassword(password, salt, iterations) }.getOrNull() ?: return false
        return constantTimeEquals(candidate, stored)
    }

    // ---- verification codes ------------------------------------------------

    /** A 6 digit code, generated with a cryptographic RNG. */
    fun newVerificationCode(): String = (random.nextInt(900_000) + 100_000).toString()

    /** Codes are hashed before storage so a database copy cannot be replayed. */
    fun hashVerificationCode(code: String): String {
        val salt = ByteArray(8).also { random.nextBytes(it) }
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(salt)
        digest.update(code.trim().toByteArray(Charsets.UTF_8))
        return Base64.getUrlEncoder().withoutPadding().encodeToString(salt) + "." +
            Base64.getUrlEncoder().withoutPadding().encodeToString(digest.digest())
    }

    fun verifyVerificationCode(code: String, stored: String): Boolean {
        val parts = stored.split(".")
        if (parts.size != 2) return false
        val salt = runCatching { Base64.getUrlDecoder().decode(parts[0]) }.getOrNull() ?: return false
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(salt)
        digest.update(code.trim().toByteArray(Charsets.UTF_8))
        val expected = Base64.getUrlEncoder().withoutPadding().encodeToString(digest.digest())
        return constantTimeEquals(expected, parts[1])
    }

    /** 32 bytes of cryptographic randomness for session tokens. */
    fun newSessionToken(): String {
        val bytes = ByteArray(32).also { random.nextBytes(it) }
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    fun constantTimeEquals(a: String, b: String): Boolean {
        val left = a.toByteArray(Charsets.UTF_8)
        val right = b.toByteArray(Charsets.UTF_8)
        return MessageDigest.isEqual(left, right)
    }

    // ---- validation --------------------------------------------------------

    /** 3-20 characters, starts with a letter, letters/digits/underscore/dot only. */
    fun isValidUsername(value: String): Boolean {
        if (value.length !in 3..20) return false
        if (!value.first().isLetter()) return false
        return value.all { it.isLetterOrDigit() || it == '_' || it == '.' }
    }

    enum class PasswordStrength { TOO_SHORT, WEAK, FAIR, STRONG }

    /**
     * Deliberately simple and explainable to the user: length matters most, and a
     * password made only of one repeated character or a common word is rejected.
     */
    fun passwordStrength(password: String): PasswordStrength {
        if (password.length < MIN_PASSWORD_LENGTH) return PasswordStrength.TOO_SHORT
        val classes = listOf(
            password.any { it.isLowerCase() },
            password.any { it.isUpperCase() },
            password.any { it.isDigit() },
            password.any { !it.isLetterOrDigit() }
        ).count { it }
        val unique = password.toSet().size
        val tooSimple = COMMON_FRAGMENTS.any { password.lowercase().contains(it) } ||
            unique < 5 ||
            password.toSet().size == 1
        return when {
            tooSimple -> PasswordStrength.WEAK
            password.length >= 14 && classes >= 3 -> PasswordStrength.STRONG
            classes >= 2 -> PasswordStrength.FAIR
            else -> PasswordStrength.WEAK
        }
    }

    fun isPasswordAcceptable(password: String): Boolean =
        passwordStrength(password) != PasswordStrength.TOO_SHORT &&
            passwordStrength(password) != PasswordStrength.WEAK

    private val COMMON_FRAGMENTS = listOf(
        "password", "123456", "qwerty", "letmein", "welcome", "admin", "iloveyou", "abc123"
    )
}