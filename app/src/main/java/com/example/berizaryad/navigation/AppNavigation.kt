// Berizaryad/app/src/main/java/com/example/berizaryad/AppNavigation.kt
package com.example.berizaryad

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.berizaryad.ui.screens.AuthScreen // Убедитесь, что импорт правильный
import com.example.berizaryad.ui.screens.SearchStationScreen
import com.example.berizaryad.ui.screens.StationInfoScreen
import com.example.berizaryad.ui.screens.ProfileScreen
import com.example.berizaryad.viewmodel.AuthViewModel
import com.example.berizaryad.viewmodel.StationViewModel
import com.google.firebase.auth.FirebaseAuth

@Composable
fun AppNavigation(
    auth: FirebaseAuth,
    authViewModel: AuthViewModel,
    stationViewModel: StationViewModel
) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "auth") {
        composable("auth") {
            // Передаем ViewModel в AuthScreen
            AuthScreen(
                onLoginSuccess = {
                    // После успешного логина переходим к поиску
                    navController.navigate("search") {
                        popUpTo("auth") { inclusive = true }
                    }
                },
                authViewModel = authViewModel // Передаем ViewModel
            )
        }

        composable("search") {
            SearchStationScreen(
                onStationSelected = { stationId ->
                    // Передаем ID станции (Long) в экран информации
                    navController.navigate("station_info/$stationId")
                },
                stationViewModel = stationViewModel,
                onNavigateToProfile = {
                    navController.navigate("profile")
                }
            )
        }

        // Маршрут для экрана информации о станции, принимающий Long ID
        composable(
            "station_info/{stationId}",
            arguments = listOf(navArgument("stationId") { type = NavType.LongType }) // Указываем тип Long
        ) { backStackEntry ->
            val stationId = backStackEntry.arguments?.getLong("stationId") ?: 0L
            StationInfoScreen(
                stationId = stationId, // Передаем Long ID
                onBack = {
                    navController.popBackStack()
                },
                stationViewModel = stationViewModel,
                authViewModel = authViewModel
            )
        }

        composable("profile") {
            ProfileScreen(
                auth = auth,
                onLogout = {
                    auth.signOut()
                    navController.navigate("auth") {
                        popUpTo("search") { inclusive = true }
                    }
                }
            )
        }
    }
}