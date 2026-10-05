# StudyHub: agent rules

## Project
Android student app (Kotlin, Jetpack Compose Material 3, Room v2, Navigation
Compose) being turned into a Kotlin Multiplatform + Compose Multiplatform
app for Android, iOS, desktop and later web. It will become a small social
platform for students: timetable, assignments, exams, accounts and cloud
sync, campus community, and peer tutorship. Backend: Supabase free tier.
Users are university students in Southern Africa: low data, low-end phones,
unreliable connectivity, so offline-first always.

## Hard rules
- Never break reminders, the programme/plan system or existing user data.
  Every database change needs a real Room migration with a test.
- Room (or the multiplatform DB) is the local source of truth. Sync is additive.
- Verify every library and version supports the targets before using it.
  If a target is not stable, say so instead of forcing it.
- Keep code simple and readable. No unnecessary libraries.
- No secrets in git. Use local.properties, BuildConfig or env variables.
- Platform differences go behind expect/actual or interfaces. Reminders:
  Android exact alarms + boot/update receivers; iOS rolling local
  notifications within the ~64 pending limit; desktop tray notifications;
  web Notifications API.
- Adaptive UI by window size class: bottom bar on phones, rail/sidebar on
  large screens. 48dp touch targets, content descriptions, text scaling.

## Workflow
- Work one phase at a time on its own branch (phase-1, phase-2, ...).
- Before coding a phase: read the relevant code and write a short plan.
- Explain each new concept in a few lines with a small worked example
  before showing the code.
- After each phase: build and run tests, commit with a clear message, then
  report: changed files, new files, Gradle changes, how to test, and
  anything unverified. Create a zip with
  `git archive -o ../StudyHub-phaseN.zip HEAD`.
- Never claim something works unless you built or ran it. Say what you
  could not test (for example iOS on a machine without a Mac).

## Build notes for this machine
- 8 GB RAM with about 1.5 GB free. The 2 GB daemon heap in `gradle.properties`
  crashes the JVM with `Native memory allocation (malloc) failed`. Build with:
  `.\gradlew.bat --% :app:assembleDebug --no-daemon --max-workers=1 -Dorg.gradle.jvmargs=-Xmx1024m`
- Use `--%` so PowerShell passes `-D` flags through untouched.
