package com.saintchigos.studyhub

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.saintchigos.studyhub.ui.StudyHubViewModel
import com.saintchigos.studyhub.ui.screens.AssignmentsScreen
import com.saintchigos.studyhub.ui.screens.CoursesScreen
import com.saintchigos.studyhub.ui.screens.DashboardScreen
import com.saintchigos.studyhub.ui.screens.ExamsScreen
import com.saintchigos.studyhub.ui.screens.TimetableScreen
import com.saintchigos.studyhub.ui.theme.StudyHubTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StudyHubTheme {
                Surface(
                    modifier = Modifier,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    StudyHubApp()
                }
            }
        }
    }
}

private enum class Destination(val route: String, val label: String, val icon: ImageVector) {
    Dashboard("dashboard", "Home", Icons.Filled.Home),
    Timetable("timetable", "Timetable", Icons.Filled.CalendarMonth),
    Assignments("assignments", "Tasks", Icons.Filled.TaskAlt),
    Exams("exams", "Exams", Icons.Filled.Event),
    Courses("courses", "Courses", Icons.Filled.School)
}

@Composable
fun StudyHubApp(viewModel: StudyHubViewModel = viewModel()) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    Scaffold(
        bottomBar = {
            NavigationBar {
                Destination.entries.forEach { destination ->
                    val selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true
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
        NavHost(
            navController = navController,
            startDestination = Destination.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Destination.Dashboard.route) { DashboardScreen(viewModel) }
            composable(Destination.Timetable.route) { TimetableScreen(viewModel) }
            composable(Destination.Assignments.route) { AssignmentsScreen(viewModel) }
            composable(Destination.Exams.route) { ExamsScreen(viewModel) }
            composable(Destination.Courses.route) { CoursesScreen(viewModel) }
        }
    }
}