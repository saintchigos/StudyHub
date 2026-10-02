package com.saintchigos.studyhub.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

/**
 * Accounts, the programme community and moderation.
 *
 * Everything here is scoped so a student can only ever reach people who share their
 * programme, year and semester; the SQL itself enforces that rather than trusting
 * the UI to filter correctly.
 */
@Dao
interface CommunityDao {

    // ---- accounts ----------------------------------------------------------

    @Insert
    suspend fun insertAccount(account: Account): Long

    @Query("SELECT * FROM accounts WHERE username = :username COLLATE NOCASE LIMIT 1")
    suspend fun getAccountByUsername(username: String): Account?

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getAccount(id: Long): Account?

    @Query("SELECT COUNT(*) FROM accounts WHERE username = :username COLLATE NOCASE")
    suspend fun countAccounts(username: String): Int

    @Update
    suspend fun updateAccount(account: Account)

    @Query("SELECT id FROM accounts WHERE username = :username COLLATE NOCASE LIMIT 1")
    suspend fun getAccountId(username: String): Long?

    // ---- verification ------------------------------------------------------

    @Insert
    suspend fun insertVerificationCode(code: VerificationCode): Long

    @Query("SELECT * FROM verification_codes WHERE username = :username AND consumed = 0 ORDER BY id DESC LIMIT 1")
    suspend fun getLatestVerificationCode(username: String): VerificationCode?

    @Update
    suspend fun updateVerificationCode(code: VerificationCode)

    @Query("DELETE FROM verification_codes WHERE username = :username")
    suspend fun clearVerificationCodes(username: String)

    // ---- sessions ----------------------------------------------------------

    @Insert
    suspend fun insertSession(session: AuthSession): Long

    @Query("SELECT * FROM auth_sessions ORDER BY id DESC LIMIT 1")
    suspend fun getLatestSession(): AuthSession?

    @Query("DELETE FROM auth_sessions")
    suspend fun clearSessions()

    // ---- programme membership --------------------------------------------

    @Insert
    suspend fun insertAccountProgramme(programme: AccountProgramme): Long

    @Query("SELECT * FROM account_programmes WHERE accountId = :accountId LIMIT 1")
    suspend fun getAccountProgramme(accountId: Long): AccountProgramme?

    /**
     * Classmates in the same programme/year/semester, excluding anyone either side has
     * blocked, and never the signed-in student themselves.
     */
    @Query(
        """
        SELECT a.* FROM accounts a
        INNER JOIN account_programmes p ON p.accountId = a.id
        WHERE p.programmeSlug = :slug AND p.year = :year AND p.semester = :semester
          AND a.id != :selfId
          AND a.id NOT IN (SELECT blockedId FROM blocks WHERE accountId = :selfId)
          AND a.id NOT IN (SELECT accountId FROM blocks WHERE blockedId = :selfId)
        ORDER BY a.displayName, a.username
        """
    )
    suspend fun getClassmates(slug: String, year: Int, semester: String, selfId: Long): List<Account>

    @Query(
        """
        SELECT c.* FROM connections c
        WHERE c.requesterId = :accountId OR c.addresseeId = :accountId
        ORDER BY c.createdAt DESC
        """
    )
    suspend fun getConnections(accountId: Long): List<Connection>

    @Query(
        """
        SELECT * FROM connections
        WHERE (requesterId = :fromId AND addresseeId = :toId)
           OR (requesterId = :toId AND addresseeId = :fromId)
        LIMIT 1
        """
    )
    suspend fun getConnectionBetween(fromId: Long, toId: Long): Connection?

    @Insert
    suspend fun insertConnection(connection: Connection): Long

    @Update
    suspend fun updateConnection(connection: Connection)

    // ---- moderation --------------------------------------------------------

    @Query("SELECT * FROM blocks WHERE accountId = :accountId ORDER BY createdAt DESC")
    suspend fun getBlocks(accountId: Long): List<Block>

    @Query("SELECT * FROM blocks WHERE accountId = :accountId AND blockedId = :blockedId LIMIT 1")
    suspend fun getBlock(accountId: Long, blockedId: Long): Block?

    @Insert
    suspend fun insertBlock(block: Block): Long

    @Query("DELETE FROM blocks WHERE accountId = :accountId AND blockedId = :blockedId")
    suspend fun deleteBlock(accountId: Long, blockedId: Long)

    @Insert
    suspend fun insertReport(report: Report): Long

    @Query("SELECT * FROM reports WHERE reporterId = :accountId ORDER BY createdAt DESC")
    suspend fun getReports(accountId: Long): List<Report>

    // ---- messages ----------------------------------------------------------

    @Insert
    suspend fun insertMessage(message: Message): Long

    @Query(
        """
        SELECT * FROM messages
        WHERE programmeSlug = :slug AND year = :year AND semester = :semester
          AND senderId NOT IN (SELECT blockedId FROM blocks WHERE accountId = :selfId)
          AND senderId NOT IN (SELECT accountId FROM blocks WHERE blockedId = :selfId)
        ORDER BY createdAt ASC
        """
    )
    suspend fun getMessages(slug: String, year: Int, semester: String, selfId: Long): List<Message>

    @Query("DELETE FROM messages")
    suspend fun clearMessages()

    // ---- wiping local account state ---------------------------------------

    @Query("DELETE FROM messages")
    suspend fun deleteAllMessages()

    @Query("DELETE FROM reports")
    suspend fun deleteAllReports()

    @Query("DELETE FROM blocks")
    suspend fun deleteAllBlocks()

    @Query("DELETE FROM connections")
    suspend fun deleteAllConnections()

    @Query("DELETE FROM account_programmes")
    suspend fun deleteAllAccountProgrammes()

    @Query("DELETE FROM verification_codes")
    suspend fun deleteAllVerificationCodes()

    @Query("DELETE FROM auth_sessions")
    suspend fun deleteAllSessions()

    @Query("DELETE FROM accounts")
    suspend fun deleteAllAccounts()
}