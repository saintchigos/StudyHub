package com.saintchigos.studyhub.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A StudyHub account. The password is only ever stored as a PBKDF2 hash produced by
 * [com.saintchigos.studyhub.util.Security], and the account cannot be used until a
 * verification code has been confirmed.
 */
@Entity(
    tableName = "accounts",
    indices = [Index(value = ["username"], unique = true)]
)
data class Account(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val username: String,
    val displayName: String = "",
    val passwordHash: String,
    val verified: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * A pending verification code. The code itself is hashed, so reading this table
 * cannot be used to confirm someone's account.
 */
@Entity(
    tableName = "verification_codes",
    indices = [Index(value = ["username"])]
)
data class VerificationCode(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val username: String,
    val codeHash: String,
    val expiresAt: Long,
    val attempts: Int = 0,
    val consumed: Boolean = false
)

/** Signed-in session for the local account. */
@Entity(
    tableName = "auth_sessions",
    foreignKeys = [ForeignKey(
        entity = Account::class,
        parentColumns = ["id"],
        childColumns = ["accountId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("accountId")]
)
data class AuthSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountId: Long,
    val token: String,
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + SESSION_DURATION_MS
) {
    companion object {
        const val SESSION_DURATION_MS = 30L * 24 * 60 * 60 * 1000
    }
}

/**
 * Which programme each account belongs to. This is what scopes the community: people
 * only ever see classmates in the same programme, year and semester.
 */
@Entity(
    tableName = "account_programmes",
    indices = [
        Index("accountId"),
        Index(value = ["programmeSlug", "year", "semester"], unique = true)
    ]
)
data class AccountProgramme(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountId: Long,
    val programmeSlug: String,
    val programmeName: String,
    val year: Int,
    val semester: String
)

/**
 * A connection request between two accounts. Connections are always explicit:
 * nobody appears in your community until they accept, and blocking cuts the link.
 */
@Entity(
    tableName = "connections",
    indices = [
        Index("requesterId"),
        Index("addresseeId"),
        Index(value = ["requesterId", "addresseeId"], unique = true)
    ]
)
data class Connection(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val requesterId: Long,
    val addresseeId: Long,
    val status: String = STATUS_PENDING,
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val STATUS_PENDING = "pending"
        const val STATUS_ACCEPTED = "accepted"
        const val STATUS_DECLINED = "declined"
    }
}

/** Someone has asked not to be contacted by another account. */
@Entity(
    tableName = "blocks",
    indices = [
        Index("accountId"),
        Index(value = ["accountId", "blockedId"], unique = true)
    ]
)
data class Block(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountId: Long,
    val blockedId: Long,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * A complaint about another account. Reports are kept on the device so a student can
 * always see what they reported; moderators review them from the server side.
 */
@Entity(
    tableName = "reports",
    indices = [Index("reporterId"), Index("reportedId")]
)
data class Report(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val reporterId: Long,
    val reportedId: Long,
    val reason: String,
    val details: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val handled: Boolean = false
)

/** A message inside a programme community. */
@Entity(
    tableName = "messages",
    indices = [
        Index(value = ["programmeSlug", "year", "semester"]),
        Index("senderId")
    ]
)
data class Message(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val programmeSlug: String,
    val year: Int,
    val semester: String,
    val senderId: Long,
    val senderName: String,
    val body: String,
    val createdAt: Long = System.currentTimeMillis(),
    /** False until the server has accepted it, so nothing is ever silently lost. */
    val synced: Boolean = false
)