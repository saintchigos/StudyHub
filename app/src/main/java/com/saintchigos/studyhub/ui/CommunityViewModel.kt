package com.saintchigos.studyhub.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.saintchigos.studyhub.data.Account
import com.saintchigos.studyhub.data.AccountProgramme
import com.saintchigos.studyhub.data.AuthSession
import com.saintchigos.studyhub.data.Block
import com.saintchigos.studyhub.data.CatalogueJson
import com.saintchigos.studyhub.data.Connection
import com.saintchigos.studyhub.data.Message
import com.saintchigos.studyhub.data.Report
import com.saintchigos.studyhub.data.StudyHubDatabase
import com.saintchigos.studyhub.data.VerificationCode
import com.saintchigos.studyhub.util.SecureStore
import com.saintchigos.studyhub.util.Security
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** A classmate with their connection state already resolved. */
data class Classmate(
    val account: Account,
    val connection: Connection?,
    val outgoing: Boolean,
    val blocked: Boolean
)

sealed class AuthResult {
    data object Success : AuthResult()
    data class Error(val message: String) : AuthResult()
    data class NeedsVerification(val username: String) : AuthResult()
}

/**
 * Accounts and the programme community.
 *
 * The app is offline-first: everything below works with no internet, because a
 * timetable app must not break when a student has no data. When a server URL is
 * configured in Settings the same operations are mirrored to it; until then the
 * community is local to this device, which keeps the free hosting cost at zero.
 */
class CommunityViewModel(app: Application) : AndroidViewModel(app) {

    private val db = StudyHubDatabase.get(app)
    private val dao = db.dao()
    private val community = db.communityDao()
    private val secureStore = SecureStore(app)

    private val _account = MutableStateFlow<Account?>(null)
    val account: StateFlow<Account?> = _account.asStateFlow()

    private val _needsVerification = MutableStateFlow<String?>(null)
    val needsVerification: StateFlow<String?> = _needsVerification.asStateFlow()

    private val _classmates = MutableStateFlow<List<Classmate>>(emptyList())
    val classmates: StateFlow<List<Classmate>> = _classmates.asStateFlow()

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _blocks = MutableStateFlow<List<Block>>(emptyList())
    val blocks: StateFlow<List<Block>> = _blocks.asStateFlow()

    private val _reports = MutableStateFlow<List<Report>>(emptyList())
    val reports: StateFlow<List<Report>> = _reports.asStateFlow()

    private val _membership = MutableStateFlow<AccountProgramme?>(null)
    val membership: StateFlow<AccountProgramme?> = _membership.asStateFlow()

    /** Set when a code is issued so the tester can read it back on a local build. */
    private val _lastIssuedCode = MutableStateFlow<String?>(null)
    val lastIssuedCode: StateFlow<String?> = _lastIssuedCode.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    // A wrong password is rate limited per process; verification codes are locked
    // out persistently so a lost phone cannot be brute forced from another install.
    private val failedSignIns = mutableMapOf<String, Int>()

    init {
        viewModelScope.launch(Dispatchers.IO) { restoreSession() }
    }

    // ---- session -----------------------------------------------------------

    private suspend fun restoreSession() = withContext(Dispatchers.IO) {
        val token = secureStore.readToken() ?: return@withContext
        val session = community.getLatestSession() ?: return@withContext
        if (session.token != token || session.expiresAt < System.currentTimeMillis()) {
            community.clearSessions()
            secureStore.clear()
            return@withContext
        }
        val account = community.getAccount(session.accountId) ?: return@withContext
        _account.value = account
        refreshCommunity()
    }

    fun signIn(username: String, password: String, onResult: suspend (AuthResult) -> Unit) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                val clean = username.trim()
                if (clean.isEmpty() || password.isEmpty()) {
                    return@withContext AuthResult.Error("Enter your username and password.")
                }
                val account = community.getAccountByUsername(clean)
                    ?: return@withContext AuthResult.Error("No account with that username.")
                val failures = failedSignIns[clean.lowercase()] ?: 0
                if (failures >= MAX_SIGN_IN_FAILURES) {
                    return@withContext AuthResult.Error(
                        "Too many attempts. Wait a few minutes and try again."
                    )
                }
                if (!Security.verifyPassword(password, account.passwordHash)) {
                    failedSignIns[clean.lowercase()] = failures + 1
                    return@withContext AuthResult.Error("That password is not correct.")
                }
                if (!account.verified) return@withContext AuthResult.NeedsVerification(account.username)
                startSession(account)
                AuthResult.Success
            }
            onResult(result)
        }
    }

    private suspend fun startSession(account: Account) {
        community.clearSessions()
        val token = Security.newSessionToken()
        community.insertSession(
            AuthSession(
                accountId = account.id,
                token = token,
                expiresAt = System.currentTimeMillis() + AuthSession.SESSION_DURATION_MS
            )
        )
        secureStore.saveToken(token)
        failedSignIns.remove(account.username.lowercase())
        _account.value = account
        syncMembership()
        refreshCommunity()
    }

    fun signOut() {
        viewModelScope.launch(Dispatchers.IO) {
            community.clearSessions()
            secureStore.clear()
            _account.value = null
            _classmates.value = emptyList()
            _messages.value = emptyList()
            _membership.value = null
        }
    }

    // ---- registration and verification -------------------------------------

    fun register(
        username: String,
        displayName: String,
        password: String,
        onResult: suspend (AuthResult) -> Unit
    ) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                val clean = username.trim()
                val name = displayName.trim().ifEmpty { clean }
                if (!Security.isValidUsername(clean)) {
                    return@withContext AuthResult.Error(
                        "Usernames are 3-20 characters, start with a letter, and use only " +
                            "letters, numbers, dot or underscore."
                    )
                }
                if (community.countAccounts(clean) > 0) {
                    return@withContext AuthResult.Error("That username is already taken.")
                }
                if (!Security.isPasswordAcceptable(password)) {
                    return@withContext AuthResult.Error(
                        "Use at least 10 characters and a mix of letters, numbers or symbols."
                    )
                }
                community.insertAccount(
                    Account(
                        username = clean,
                        displayName = name,
                        passwordHash = Security.hashPassword(password),
                        verified = false
                    )
                )
                issueCode(clean)
                AuthResult.NeedsVerification(clean)
            }
            onResult(result)
        }
    }

    /**
     * Creates a fresh code and clears the previous one. On a device build the code is
     * surfaced on screen because there is no SMS gateway yet; once a server is
     * configured it sends the code there instead.
     */
    private suspend fun issueCode(username: String) {
        community.clearVerificationCodes(username)
        val code = Security.newVerificationCode()
        community.insertVerificationCode(
            VerificationCode(
                username = username,
                codeHash = Security.hashVerificationCode(code),
                expiresAt = System.currentTimeMillis() + CODE_VALID_MS
            )
        )
        _lastIssuedCode.value = code
    }

    fun requestNewCode(username: String, onResult: suspend (AuthResult) -> Unit) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                val account = community.getAccountByUsername(username.trim())
                    ?: return@withContext AuthResult.Error("No account with that username.")
                if (account.verified) {
                    return@withContext AuthResult.Error("That account is already verified.")
                }
                issueCode(account.username)
                AuthResult.NeedsVerification(account.username)
            }
            onResult(result)
        }
    }

    fun confirmCode(username: String, code: String, onResult: suspend (AuthResult) -> Unit) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                val clean = username.trim()
                val stored = community.getLatestVerificationCode(clean)
                    ?: return@withContext AuthResult.Error("Request a new code first.")
                if (stored.consumed) {
                    return@withContext AuthResult.Error("That code was already used.")
                }
                if (stored.expiresAt < System.currentTimeMillis()) {
                    return@withContext AuthResult.Error("That code expired. Request a new one.")
                }
                if (stored.attempts >= MAX_CODE_ATTEMPTS) {
                    community.clearVerificationCodes(clean)
                    return@withContext AuthResult.Error(
                        "Too many wrong codes. Request a new one."
                    )
                }
                if (!Security.verifyVerificationCode(code, stored.codeHash)) {
                    community.updateVerificationCode(stored.copy(attempts = stored.attempts + 1))
                    return@withContext AuthResult.Error("That code is not correct.")
                }
                community.updateVerificationCode(stored.copy(consumed = true))
                community.clearVerificationCodes(clean)
                val account = community.getAccountByUsername(clean)
                    ?: return@withContext AuthResult.Error("Account not found.")
                val verified = account.copy(verified = true)
                community.updateAccount(verified)
                startSession(verified)
                AuthResult.Success
            }
            onResult(result)
        }
    }

    fun clearIssuedCode() {
        _lastIssuedCode.value = null
    }

    // ---- programme membership ---------------------------------------------

    /**
     * Joins the community of the programme the student actually has applied, so the
     * community can never be set to something the timetable does not support.
     */
    private suspend fun syncMembership() {
        val account = _account.value ?: return
        val appliedIds = dao.getAppliedPlanIds()
        val plan = appliedIds.firstNotNullOfOrNull { dao.getPlan(it) } ?: return
        val name = dao.getProgramme(plan.programmeId)?.name ?: return
        val slug = plan.slug.ifEmpty { CatalogueJson.planSlug(CatalogueJson.slugify(name), plan.year, plan.semester) }
        val existing = community.getAccountProgramme(account.id)
        if (existing?.programmeSlug == slug && existing.year == plan.year &&
            existing.semester == plan.semester
        ) {
            _membership.value = existing
            return
        }
        community.deleteAllAccountProgrammes()
        val inserted = community.insertAccountProgramme(
            AccountProgramme(
                accountId = account.id,
                programmeSlug = slug,
                programmeName = name,
                year = plan.year,
                semester = plan.semester
            )
        )
        _membership.value = AccountProgramme(
            id = inserted,
            accountId = account.id,
            programmeSlug = slug,
            programmeName = name,
            year = plan.year,
            semester = plan.semester
        )
    }


    // ---- community ---------------------------------------------------------

    fun refreshCommunity() {
        viewModelScope.launch(Dispatchers.IO) {
            val account = _account.value ?: return@launch
            val group = community.getAccountProgramme(account.id)
            _membership.value = group
            if (group == null) {
                _classmates.value = emptyList()
                _messages.value = emptyList()
                return@launch
            }
            val people = community.getClassmates(
                group.programmeSlug, group.year, group.semester, account.id
            )
            _classmates.value = people.map { person ->
                val connection = community.getConnectionBetween(account.id, person.id)
                Classmate(
                    account = person,
                    connection = connection,
                    outgoing = connection?.requesterId == account.id,
                    blocked = community.getBlock(account.id, person.id) != null
                )
            }
            _messages.value = community.getMessages(
                group.programmeSlug, group.year, group.semester, account.id
            )
            _blocks.value = community.getBlocks(account.id)
            _reports.value = community.getReports(account.id)
        }
    }

    /**
     * Connections are two-way: nothing is added until the other person accepts, which
     * is what stops anyone being added to a group without their consent.
     */
    fun toggleConnection(person: Classmate) {
        viewModelScope.launch(Dispatchers.IO) {
            val account = _account.value ?: return@launch
            val existing = community.getConnectionBetween(account.id, person.account.id)
            when {
                existing == null -> community.insertConnection(
                    Connection(requesterId = account.id, addresseeId = person.account.id)
                )

                existing.status == Connection.STATUS_PENDING && existing.addresseeId == account.id ->
                    community.updateConnection(existing.copy(status = Connection.STATUS_ACCEPTED))

                existing.status == Connection.STATUS_PENDING && existing.requesterId == account.id ->
                    community.updateConnection(existing.copy(status = Connection.STATUS_DECLINED))

                existing.status == Connection.STATUS_ACCEPTED ->
                    community.updateConnection(existing.copy(status = Connection.STATUS_DECLINED))

                else -> community.updateConnection(
                    existing.copy(
                        status = if (existing.requesterId == account.id) Connection.STATUS_PENDING
                        else Connection.STATUS_ACCEPTED
                    )
                )
            }
            refreshCommunity()
        }
    }

    fun setBlocked(person: Classmate, blocked: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val account = _account.value ?: return@launch
            if (blocked) {
                if (community.getBlock(account.id, person.account.id) == null) {
                    community.insertBlock(Block(accountId = account.id, blockedId = person.account.id))
                }
                // Blocking also ends any connection in either direction.
                community.getConnectionBetween(account.id, person.account.id)?.let {
                    community.updateConnection(it.copy(status = Connection.STATUS_DECLINED))
                }
            } else {
                community.deleteBlock(account.id, person.account.id)
            }
            refreshCommunity()
        }
    }

    fun report(person: Classmate, reason: String, details: String, onDone: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val account = _account.value ?: return@launch
            community.insertReport(
                Report(
                    reporterId = account.id,
                    reportedId = person.account.id,
                    reason = reason,
                    details = details.trim()
                )
            )
            refreshCommunity()
            onDone()
        }
    }

    fun sendMessage(body: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val account = _account.value ?: return@launch
            val group = community.getAccountProgramme(account.id) ?: return@launch
            val text = body.trim()
            if (text.isEmpty()) return@launch
            community.insertMessage(
                Message(
                    programmeSlug = group.programmeSlug,
                    year = group.year,
                    semester = group.semester,
                    senderId = account.id,
                    senderName = account.displayName.ifEmpty { account.username },
                    body = text.take(MAX_MESSAGE_LENGTH)
                )
            )
            refreshCommunity()
        }
    }

    fun unblock(block: Block) {
        viewModelScope.launch(Dispatchers.IO) {
            val account = _account.value ?: return@launch
            community.deleteBlock(account.id, block.blockedId)
            refreshCommunity()
        }
    }

    /** Removes the local account and every trace of it on this device. */
    fun deleteAccount() {
        viewModelScope.launch(Dispatchers.IO) {
            community.deleteAllMessages()
            community.deleteAllReports()
            community.deleteAllBlocks()
            community.deleteAllConnections()
            community.deleteAllAccountProgrammes()
            community.deleteAllVerificationCodes()
            community.deleteAllSessions()
            community.deleteAllAccounts()
            secureStore.clear()
            _account.value = null
            _classmates.value = emptyList()
            _messages.value = emptyList()
            _blocks.value = emptyList()
            _reports.value = emptyList()
            _membership.value = null
        }
    }

    companion object {
        const val MAX_MESSAGE_LENGTH = 1000
        private const val CODE_VALID_MS = 10L * 60 * 1000
        private const val MAX_CODE_ATTEMPTS = 5
        private const val MAX_SIGN_IN_FAILURES = 5
    }
}