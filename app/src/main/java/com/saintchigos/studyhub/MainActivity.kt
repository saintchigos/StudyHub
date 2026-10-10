package com.saintchigos.studyhub

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarViewWeek
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Grade
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.SpaceDashboard
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.saintchigos.studyhub.reminder.ClassAlarms
import com.saintchigos.studyhub.reminder.ReminderRules
import com.saintchigos.studyhub.ui.Adaptive
import com.saintchigos.studyhub.ui.CommunityViewModel
import com.saintchigos.studyhub.ui.FocusViewModel
import com.saintchigos.studyhub.ui.StudyHubViewModel
import com.saintchigos.studyhub.ui.components.ClassAlertBanner
import com.saintchigos.studyhub.ui.screens.AccountScreen
import com.saintchigos.studyhub.ui.screens.AlarmsScreen
import com.saintchigos.studyhub.ui.screens.AssignmentsScreen
import com.saintchigos.studyhub.ui.screens.CommunityScreen
import com.saintchigos.studyhub.ui.screens.CoursesScreen
import com.saintchigos.studyhub.ui.screens.DashboardScreen
import com.saintchigos.studyhub.ui.screens.ExamsScreen
import com.saintchigos.studyhub.ui.screens.FocusScreen
import com.saintchigos.studyhub.ui.screens.LegalKind
import com.saintchigos.studyhub.ui.screens.LegalScreen
import com.saintchigos.studyhub.ui.screens.OnboardingScreen
import com.saintchigos.studyhub.ui.screens.SettingsScreen
import com.saintchigos.studyhub.ui.screens.TimetableScreen
import com.saintchigos.studyhub.ui.theme.StudyHubTheme
import com.saintchigos.studyhub.util.TimeUtil
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.saintchigos.studyhub.data.StudyHubDatabase
import com.saintchigos.studyhub.reminder.NextClass
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* best effort */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        ClassAlarms.ensureChannel(this)
        NextClass.ensureChannel(this)
        com.saintchigos.studyhub.reminder.DeadlineAlarms.ensureChannel(this)
        requestNotificationPermissionIfNeeded()

        // The "what's next" card is a claim about the near future, so it is refreshed
        // whenever the app is opened rather than only when the timetable is edited.
        // A class can start while the phone sat in a pocket, and the card has to stop
        // advertising it as the next thing to go to.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                val sessions = withContext(Dispatchers.IO) {
                    StudyHubDatabase.get(applicationContext).dao().getAllSessions()
                }
                NextClass.update(applicationContext, sessions)
            }
        }

        setContent {
            StudyHubApp()
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = androidx.core.content.ContextCompat.checkSelfPermission(
            this, Manifest.permission.POST_NOTIFICATIONS
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (!granted) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

/**
 * Bottom bar tabs. Icons are filled and distinct at a glance, because a student
 * should not have to read six labels to find the timetable.
 *
 * Five, not seven. At 360dp each of seven items got about 51dp, so the labels
 * crowded each other and the touch targets fell under the 48dp minimum. Exams,
 * Focus and Courses now live on Home and in More instead, which is where a
 * student looks for them anyway rather than mid-tap during a lesson.
 */
private enum class Destination(val route: String, val label: String, val icon: ImageVector) {
    Dashboard("dashboard", "Home", Icons.Filled.SpaceDashboard),
    Timetable("timetable", "Classes", Icons.Filled.CalendarViewWeek),
    Assignments("assignments", "Tasks", Icons.Filled.Checklist),
    Exams("exams", "Exams", Icons.Filled.Quiz),
    Settings("settings", "More", Icons.Filled.Tune)
}

/**
 * Reachable from Home and Settings rather than the bottom bar: seven tabs crowd a
 * low-end phone and squeeze the touch targets below 48dp.
 */
private const val ROUTE_COMMUNITY = "community"
private const val ROUTE_ACCOUNT = "account"
private const val ROUTE_TERMS = "legal/terms"
private const val ROUTE_PRIVACY = "legal/privacy"
private const val ROUTE_FOCUS = "focus"
private const val ROUTE_ALARMS = "alarms"
private const val ROUTE_COURSES = "courses"

@Composable
fun StudyHubApp(viewModel: StudyHubViewModel = viewModel()) {
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val darkTheme = when (themeMode) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    }
    val accent by viewModel.accent.collectAsStateWithLifecycle()
    val dynamicColor by viewModel.dynamicColor.collectAsStateWithLifecycle()
    val amoled by viewModel.amoled.collectAsStateWithLifecycle()
    val fontScale by viewModel.fontScale.collectAsStateWithLifecycle()

    StudyHubTheme(
        darkTheme = darkTheme,
        accent = accent,
        dynamicColor = dynamicColor,
        amoled = amoled,
        fontScale = fontScale,
    ) {
        // A soft wash of the accent fading into the page, so the app has a colour of its
        // own behind the cards instead of a flat white or black sheet.
        val wash = MaterialTheme.colorScheme.primaryContainer
        val page = MaterialTheme.colorScheme.surface
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        0f to wash.copy(alpha = 0.75f),
                        0.45f to page,
                        1f to page
                    )
                )
        ) {
            Surface(color = androidx.compose.ui.graphics.Color.Transparent) {
                AppRoot(viewModel)
            }
        }
    }
}

@Composable
private fun AppRoot(viewModel: StudyHubViewModel) {
    val communityViewModel: CommunityViewModel = viewModel()
    val focusViewModel: FocusViewModel = viewModel()
    val setupComplete by viewModel.setupComplete.collectAsStateWithLifecycle()

    // First run shows the programme picker; students who already have a timetable
    // are matched to their plan and go straight in.
    if (!setupComplete) {
        OnboardingScreen(viewModel)
        return
    }

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val context = LocalContext.current

    var alert by remember { mutableStateOf<com.saintchigos.studyhub.reminder.ClassReminder?>(null) }
    val dismissed = remember { mutableStateOf(setOf<String>()) }

    // Poll for a class that is about to start so the banner fires even in-app.
    LaunchedEffect(Unit) {
        while (true) {
            val due = ReminderRules.imminent(viewModel.allSessions.value, TimeUtil.now())
            if (due != null && due.key !in dismissed.value) {
                alert = due
            }
            delay(15_000)
        }
    }


    // Home's "Add task" and "Add test" buttons need the destination screen to open its
    // dialog on arrival. Held here as plain state rather than passed as a navigation
    // argument, because `restoreState` reuses a saved entry whose arguments do not
    // include the one-off flag, so the dialog silently did not open.
    var openAddTask by remember { mutableStateOf(false) }
    var openAddExam by remember { mutableStateOf(false) }

    // One flag shared by the bar and the scroll connection below.
    var barHidden by remember { mutableStateOf(false) }

    // A rail on a tablet, the bottom bar on a phone. Measured from the real window
    // width rather than assumed from the device model, because the same app is on a
    // 360dp phone and a tablet that is 601dp even held upright.
    val widthDp = LocalConfiguration.current.screenWidthDp
    val useRail = Adaptive.useRail(widthDp)

    val goTo: (Destination) -> Unit = { destination ->
        navController.navigate(destination.route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        bottomBar = {
            // Slides out of the way while reading a long list and comes back on the
            // first upward scroll. On a 360x800dp screen the bar is 80dp, which is a
            // lot of a study app's usable height, and the content underneath is what
            // the student is actually reading.
            //
            // Translated rather than removed, so the Scaffold keeps reserving the
            // same inset and the content does not jump when it hides.
            //
            // No rail on wide screens: it runs down the side and costs the content no
            // height, so there is nothing to hide.
            if (!useRail) {
                val density = LocalDensity.current
                val hiddenPx = with(density) { 200.dp.toPx() }

                NavigationBar(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            translationY = if (barHidden) hiddenPx else 0f
                        }
                ) {
                    Destination.entries.forEach { destination ->
                        val selected =
                            currentDestination?.hierarchy?.any { it.route == destination.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = { goTo(destination) },
                            icon = {
                                Icon(destination.icon, contentDescription = destination.label)
                            },
                            label = { Text(destination.label) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Row(modifier = Modifier.padding(innerPadding)) {
            if (useRail) {
                // A rail sits in the padding, not floating over the content, so the
                // list is measured from the rail's edge and never hides behind it.
                NavigationRail {
                    Destination.entries.forEach { destination ->
                        val selected =
                            currentDestination?.hierarchy?.any { it.route == destination.route } == true
                        NavigationRailItem(
                            selected = selected,
                            onClick = { goTo(destination) },
                            icon = { Icon(destination.icon, contentDescription = destination.label) },
                            label = { Text(destination.label) }
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
            // Stops a single column of text running the full 1000dp of a landscape
            // tablet, where a line becomes unreadable. The phone is never near this.
            Column(modifier = Modifier.widthIn(max = Adaptive.MAX_CONTENT_DP.dp)) {
            ClassAlertBanner(
                reminder = alert,
                onDismiss = {
                    alert?.let { dismissed.value = dismissed.value + it.key }
                    alert = null
                },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )

            NavHost(
                navController = navController,
                startDestination = Destination.Dashboard.route,
                // Watching the nested scroll here rather than giving each screen its
                // own state means the bar behaves the same on every list in the app.
                modifier = Modifier.nestedScroll(
                    remember {
                        object : NestedScrollConnection {
                            override fun onPostScroll(
                                consumed: Offset,
                                available: Offset,
                                source: NestedScrollSource
                            ): Offset {
                                // available.y > 0 means there is more content below,
                                // so the student is scrolling down.
                                if (available.y > 8f) barHidden = true
                                if (available.y < -8f) barHidden = false
                                return Offset.Zero
                            }
                        }
                    }
                )
            ) {
                composable(Destination.Timetable.route) { TimetableScreen(viewModel) }
                composable(Destination.Assignments.route) {
                    // Keyed on the flag so the dialog opens once on arrival rather
                    // than on every recomposition, and the flag is cleared straight
                    // after so it cannot reopen when the student comes back.
                    val openAdd = openAddTask
                    LaunchedEffect(openAdd) { if (openAdd) openAddTask = false }
                    AssignmentsScreen(
                        viewModel = viewModel,
                        openAddOnStart = openAdd
                    )
                }
                composable(Destination.Exams.route) {
                    val openAdd = openAddExam
                    LaunchedEffect(openAdd) { if (openAdd) openAddExam = false }
                    ExamsScreen(
                        viewModel = viewModel,
                        openAddOnStart = openAdd
                    )
                }
                composable(ROUTE_COURSES) { CoursesScreen(viewModel) }
                composable(ROUTE_FOCUS) { FocusScreen(focusViewModel) }
composable(ROUTE_ALARMS) { AlarmsScreen(focusViewModel) }
                composable(Destination.Dashboard.route) {
                    DashboardScreen(
                        viewModel = viewModel,
                        communityViewModel = communityViewModel,
                        onOpenAccount = { navController.navigate(ROUTE_ACCOUNT) },
                        onOpenCommunity = { navController.navigate(ROUTE_COMMUNITY) },
                        onOpenFocus = { navController.navigate(ROUTE_FOCUS) },
                        // The alarm tile now has its own screen instead of landing on the study timer.
                        onOpenAlarms = {
                            navController.navigate(ROUTE_ALARMS) {
                                launchSingleTop = true
                            }
                        },
                        onOpenCourses = { navController.navigate(ROUTE_COURSES) },
                        onOpenTasks = { navController.navigate(Destination.Assignments.route) },
                        onOpenAddTask = {
                            openAddTask = true
                            navController.navigate(Destination.Assignments.route)
                        },
                        onOpenExams = { navController.navigate(Destination.Exams.route) },
                        onOpenAddExam = {
                            openAddExam = true
                            navController.navigate(Destination.Exams.route)
                        },
                        onOpenTimetable = { navController.navigate(Destination.Timetable.route) }
                    )
                }
                composable(Destination.Settings.route) {
                    SettingsScreen(
                        viewModel = viewModel,
                        communityViewModel = communityViewModel,
                        onOpenCommunity = { navController.navigate(ROUTE_COMMUNITY) },
                        onOpenAccount = { navController.navigate(ROUTE_ACCOUNT) },
                        // Focus and Courses left the bottom bar, so More is now the
                        // other way into them.
                        onOpenFocus = { navController.navigate(ROUTE_FOCUS) },
                        onOpenCourses = { navController.navigate(ROUTE_COURSES) },
                        onOpenLegal = { kind ->
                            navController.navigate(
                                if (kind == LegalKind.Terms) ROUTE_TERMS else ROUTE_PRIVACY
                            )
                        }
                    )
                }
                composable(ROUTE_COMMUNITY) {
                    CommunityScreen(
                        viewModel = communityViewModel,
                        onBack = { navController.popBackStack() },
                        onOpenAccount = { navController.navigate(ROUTE_ACCOUNT) }
                    )
                }
                composable(ROUTE_ACCOUNT) {
                    AccountScreen(
                        viewModel = communityViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(ROUTE_TERMS) {
                    LegalScreen(kind = LegalKind.Terms, onBack = { navController.popBackStack() })
                }
                composable(ROUTE_PRIVACY) {
                    LegalScreen(
                        kind = LegalKind.Privacy,
                        onBack = { navController.popBackStack() }
                    )
                }
            }
            }
            }
        }
    }
}