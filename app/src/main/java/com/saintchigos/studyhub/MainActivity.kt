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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.saintchigos.studyhub.reminder.ClassAlarms
import com.saintchigos.studyhub.reminder.ReminderRules
import com.saintchigos.studyhub.ui.StudyHubViewModel
import com.saintchigos.studyhub.ui.components.ClassAlertBanner
import com.saintchigos.studyhub.ui.screens.AssignmentsScreen
import com.saintchigos.studyhub.ui.screens.CoursesScreen
import com.saintchigos.studyhub.ui.screens.DashboardScreen
import com.saintchigos.studyhub.ui.screens.ExamsScreen
import com.saintchigos.studyhub.ui.screens.OnboardingScreen
import com.saintchigos.studyhub.ui.screens.SettingsScreen
import com.saintchigos.studyhub.ui.screens.TimetableScreen
import com.saintchigos.studyhub.ui.theme.StudyHubTheme
import com.saintchigos.studyhub.util.TimeUtil
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* best effort */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        ClassAlarms.ensureChannel(this)
        requestNotificationPermissionIfNeeded()

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

private enum class Destination(val route: String, val label: String, val icon: ImageVector) {
    Dashboard("dashboard", "Home", Icons.Filled.Home),
    Timetable("timetable", "Timetable", Icons.Filled.CalendarMonth),
    Assignments("assignments", "Tasks", Icons.Filled.TaskAlt),
    Exams("exams", "Exams", Icons.Filled.Event),
    Courses("courses", "Courses", Icons.Filled.School),
    Settings("settings", "Settings", Icons.Filled.Settings)
}

@Composable
fun StudyHubApp(viewModel: StudyHubViewModel = viewModel()) {
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val darkTheme = when (themeMode) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    }

    StudyHubTheme(darkTheme = darkTheme) {
        Surface(color = MaterialTheme.colorScheme.surface) {
            AppRoot(viewModel)
        }
    }
}

@Composable
private fun AppRoot(viewModel: StudyHubViewModel) {
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


    Scaffold(
        bottomBar = {
            NavigationBar {
                Destination.entries.forEach { destination ->
                    val selected =
                        currentDestination?.hierarchy?.any { it.route == destination.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(destination.icon, contentDescription = destination.label)
                        },
                        label = { Text(destination.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
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
                startDestination = Destination.Dashboard.route
            ) {
                composable(Destination.Dashboard.route) { DashboardScreen(viewModel) }
                composable(Destination.Timetable.route) { TimetableScreen(viewModel) }
                composable(Destination.Assignments.route) { AssignmentsScreen(viewModel) }
                composable(Destination.Exams.route) { ExamsScreen(viewModel) }
                composable(Destination.Courses.route) { CoursesScreen(viewModel) }
composable(Destination.Settings.route) { SettingsScreen(viewModel) }
            }
        }
    }
}