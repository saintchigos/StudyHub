# StudyHub

An Android student dashboard: timetable, assignments and exam countdowns in one place.

Built with Kotlin, Jetpack Compose (Material 3), Room and Navigation Compose.

## Features

- **Home** - open tasks, overdue count, enrolled courses, next exam with countdown, today's classes, due-soon list.
- **Timetable** - weekly class schedule with day filter.
- **Assignments** - tick off work as you finish it; grouped by deadline with overdue highlighting and priority.
- **Exams** - countdown badges, start time, duration, venue and revision notes.
- **Courses** - add and remove courses, then attach classes, assignments and exams to them.

Data is stored locally in a Room database. No network, no account, no permissions.

On first launch the app seeds itself with four sample courses so every screen has something to show.

## Requirements

- JDK 17
- Android SDK with platform 35 and build-tools 35.0.0

The local `local.properties` file points at your SDK and is not tracked by git.

## Build

```powershell
.\gradlew.bat assembleDebug
```

The debug APK lands at `app\build\outputs\apk\debug\app-debug.apk`.

## Install on a connected device

```powershell
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

## Project layout

```
app/src/main/java/com/saintchigos/studyhub/
  MainActivity.kt              entry point and bottom navigation
  data/
    Entities.kt                Room entities and joined query projections
    StudyHubDao.kt             all database queries
    StudyHubDatabase.kt        database instance and sample seeding
  ui/
    StudyHubViewModel.kt       state holder
    components/Common.kt       reusable composables
    screens/                   one file per tab
    theme/                     colours, typography, theme
  util/TimeUtil.kt             time parsing, formatting and countdowns
```

## Notes

- Times are stored as minutes from midnight for weekly classes, and as epoch milliseconds for
  one-off deadlines so they survive timezone changes.
- Deleting a course cascades to its classes, assignments and exams.