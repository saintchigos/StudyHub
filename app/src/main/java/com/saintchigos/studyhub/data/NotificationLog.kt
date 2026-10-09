package com.saintchigos.studyhub.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.runBlocking

/**
 * Files a reminder into the in-app inbox behind the bell.
 *
 * Best effort by design. This is called from a BroadcastReceiver that is already
 * posting a notification, so a database hiccup here must never be the reason a
 * student does not get told about class. Everything is wrapped and logged, and a
 * failure is dropped rather than thrown.
 *
 * Named [InboxLog] rather than [NotificationLog] so it cannot be confused with the
 * entity it writes.
 */
object InboxLog {

    private const val TAG = "StudyHubInbox"

    /** Keeps the inbox to a recent, glanceable window on a low-end phone. */
    private const val KEEP = 200

    fun record(context: Context, kind: String, title: String, body: String): Thread {
        val appContext = context.applicationContext
        // A BroadcastReceiver has no coroutine scope, and this must not delay the
        // notification it is called alongside, so the write goes on its own thread.
        val worker = Thread {
            runBlocking {
                try {
                    val dao = StudyHubDatabase.get(appContext).dao()
                    dao.insertNotification(
                        NotificationLog(
                            kind = kind,
                            title = title,
                            body = body,
                            createdAt = System.currentTimeMillis()
                        )
                    )
                    trim(dao)
                } catch (t: Throwable) {
                    // Swallowed on purpose: a failed inbox write must never stop the
                    // student being told about class.
                    Log.w(TAG, "Could not file reminder in the inbox", t)
                }
            }
        }
        worker.start()
        return worker
    }

    /**
     * Drops the oldest rows once the inbox is full.
     *
     * Read rows go first and oldest first, so the unread reminders the student
     * still has to act on survive even on a phone that has been running for weeks.
     */
    private suspend fun trim(dao: StudyHubDao) {
        val all = dao.notificationIdsNewestFirst()
        if (all.size <= KEEP) return
        val surplus = all.size - KEEP
        // Sorted newest first, so taking from the end is oldest first.
        all.takeLast(surplus).forEach { dao.deleteNotification(it) }
    }
}