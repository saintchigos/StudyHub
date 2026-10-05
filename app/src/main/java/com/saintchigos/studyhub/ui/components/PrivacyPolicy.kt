package com.saintchigos.studyhub.ui.components

/**
 * What StudyHub collects, written for students rather than lawyers.
 *
 * Kept as plain data so the full page and any future export read from one source.
 */
val PRIVACY_SECTIONS = listOf(
    "What stays on your phone" to listOf(
        "Your courses, class times, assignments, exams and any programme you add " +
            "yourself are stored in a database on this device only.",
        "If you never create an account, nothing about you is ever sent anywhere."
    ),
    "What an account stores" to listOf(
        "Creating an account keeps your username, display name and which programme, " +
            "year and semester you are in.",
        "Your password is never stored as plain text. StudyHub keeps only a salted " +
            "hash of it, so it cannot be read back out of the app.",
        "Your timetable, assignments and exams are not uploaded with your account."
    ),
    "What other students can see" to listOf(
        "In the community, other students in the same programme and semester can see " +
            "your display name and your messages.",
        "You can block anyone. A blocked student disappears from your class list and " +
            "can no longer message you.",
        "You can report a student to us through the community screen if they " +
            "misbehave."
    ),
    "Right now, everything is on your phone" to listOf(
        "Accounts and community messages are currently stored on this device only. " +
            "There is no community server yet, so nobody on another phone can see " +
            "your account or your messages.",
        "This will change when cloud sync is switched on. Until then, uninstalling " +
            "the app removes your account and community data with it."
    ),
    "Permissions" to listOf(
        "Notifications and exact alarms are used only to warn you before class. You " +
            "can turn reminders off without affecting your timetable.",
        "StudyHub does not ask for your contacts, photos, location or microphone."
    ),
    "Deleting your data" to listOf(
        "Delete all my data in Settings removes every course, class, assignment and " +
            "exam from this device immediately.",
        "Deleting your account removes your community profile, messages and " +
            "connections."
    ),
    "Contact" to listOf(
        "Questions about your data? Ask us on WhatsApp: +266 6284 8760"
    )
)
