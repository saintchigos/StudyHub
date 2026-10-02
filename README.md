# StudyHub

An Android student dashboard: timetable, assignments and exam countdowns in one place, with
class reminders that actually get your attention.

Built with Kotlin, Jetpack Compose (Material 3), Room and Navigation Compose.

Powered by **Chigos Media**.

## Features

- **Home** - open tasks, overdue count, enrolled courses, next exam with countdown, today's classes, due-soon list.
- **Timetable** - weekly class schedule with day filter. Tap any class to edit its day, time and room, or delete it.
- **Assignments** - tick off work as you finish it; grouped by deadline with overdue highlighting and priority.
- **Exams** - countdown badges, start time, duration, venue and revision notes.
- **Courses** - add and remove courses, then attach classes, assignments and exams to them.

### Programmes and start-up setup

New students do not type their timetable out by hand.

1. On first launch a start-up page explains how the app works and asks you to accept the
   terms and conditions.
2. Pick your **programme**, **year** and **semester**. The whole timetable is generated for you.
3. If your programme is not listed, add it yourself, choose your year and semester, and add
   your courses.
4. Applying a plan never destroys anything. It only fills in what is missing, so you can
   apply more than one plan and keep your own edits.

The shipped catalogue contains **Computers and Statistics, Year 3, Semester A** with its
five courses and thirteen class times.

### Settings

Reachable from the **Settings** tab in the bottom navigation.

- See which programmes are applied, add another, or remove one.
- **Semester ended** clears the class times and reminders for the applied plan so you stop
  getting alerts for last semester, while keeping your courses, assignments and exams. You
  can then load your next semester.
- **Class reminders** shows whether notifications and exact alarms are permitted, explains
  what is blocking them, and links to the relevant system settings.
- **App settings** covers appearance (system, light, dark), how long before your first class
  and every other class you get warned, alert sound, vibration, and a test reminder.
- **Terms and conditions**, **privacy**, app version, and support.
- **Return to start-up page** reopens the programme picker without deleting anything.
- **Delete all my data** removes every course, class, assignment and exam from the device.

### Reminders

- 10 minutes before the **first class of the day**, and 5 minutes before every other class.
  Both lead times are adjustable in Settings.
- A high-importance heads-up notification with sound and vibration, plus an in-app banner.
- Alarms are scheduled seven days ahead and re-armed whenever the timetable changes, on
  reboot, and after an app update.
- If Android will not allow exact alarms, reminders fall back to a short window instead of
  being dropped.

## Data and privacy

Everything is stored locally in a Room database. There is no account server and nothing is
uploaded. Support is available on WhatsApp: **+266 6284 8760**.

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
  MainActivity.kt              entry point, theming and bottom navigation
  data/
    Entities.kt                Room entities and joined query projections
    ProgrammeEntities.kt       programme, plan and template entities
    PlanCatalogue.kt           timetable templates shipped with the app
    StudyHubDao.kt             all database queries
    StudyHubDatabase.kt        database, migration and plan apply/remove logic
  reminder/                    reminder rules, alarms, receivers
  ui/
    StudyHubViewModel.kt       state holder
    components/                reusable composables and settings pieces
    screens/                   one file per tab, plus onboarding and setup
    theme/                     colours, typography, theme
  util/                        time helpers and stored preferences
```

## Notes

- Times are stored as minutes from midnight for weekly classes, and as epoch milliseconds for
  one-off deadlines so they survive timezone changes.
- Deleting a course cascades to its classes, assignments and exams.
- Database version 2 adds the programme tables through a real migration, so upgrading never
  wipes an existing timetable.
- Students who already had a timetable before programmes existed are matched to the closest
  catalogue plan automatically and are not sent back to the start-up page.