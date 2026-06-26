package com.example.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.CalendarScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.ReservationListScreen
import com.example.ui.screens.ReservationFormScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            if (currentRoute != "login") {
                AppBottomNavigation(navController)
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "login",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("login") { LoginScreen(navController) }
            composable("dashboard") { DashboardScreen(navController = navController) }
            composable("calendar") { CalendarScreen(navController) }
            composable("bookings") { ReservationListScreen(navController) }
            composable("reports") { com.example.ui.screens.ReportScreen() }
            composable(
                route = "form?id={id}&unit={unit}&date={date}",
                arguments = listOf(
                    androidx.navigation.navArgument("id") { 
                        type = androidx.navigation.NavType.StringType 
                        nullable = true
                        defaultValue = null
                    },
                    androidx.navigation.navArgument("unit") { 
                        type = androidx.navigation.NavType.StringType 
                        nullable = true
                        defaultValue = null
                    },
                    androidx.navigation.navArgument("date") { 
                        type = androidx.navigation.NavType.StringType 
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getString("id")
                val unit = backStackEntry.arguments?.getString("unit")
                val date = backStackEntry.arguments?.getString("date")
                ReservationFormScreen(navController, reservationId = id, prefillUnit = unit, prefillDate = date)
            }
            composable(
                route = "detail/{id}",
                arguments = listOf(androidx.navigation.navArgument("id") { type = androidx.navigation.NavType.StringType })
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getString("id") ?: ""
                com.example.ui.screens.ReservationDetailScreen(navController, id)
            }
        }
    }
}

