// Berizaryad/app/src/main/java/com/example/berizaryad/navigation/AppNavigation.kt
package com.example.berizaryad.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.berizaryad.ui.screens.AuthScreen
import com.example.berizaryad.ui.screens.ProfileScreen
import com.example.berizaryad.ui.screens.SearchStationScreen
import com.example.berizaryad.ui.screens.StationInfoScreen
import com.example.berizaryad.ui.screens.StationListScreen
import com.example.berizaryad.viewmodel.AuthViewModel
import com.example.berizaryad.viewmodel.StationViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun AppNavigation(startDestination: String) {
    val navController = rememberNavController()
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val authViewModel = AuthViewModel(auth, db)
    val stationViewModel = StationViewModel(db)

    NavHost(navController = navController, startDestination = startDestination) {
        composable("auth") {
            AuthScreen(
                onNavigateToSearch = {
                    navController.navigate("search") {
                        popUpTo("auth") { inclusive = true }
                    }
                }
            )
        }

        composable("search") {
            SearchStationScreen(
                onNavigateToStationList = { navController.navigate("station_list") },
                onNavigateToProfile = { navController.navigate("profile") },
                onLoginClick = { navController.navigate("auth") },
                authViewModel = authViewModel,
                // Передаем функцию навигации к найденной станции, преобразуя String в Long
                onNavigateToFoundStation = { stationIdString ->
                    val idAsLong = stationIdString.toLongOrNull()
                    if (idAsLong != null) {
                        navController.navigate("station_info/$idAsLong")
                    } else {
                        // Обработка ошибки: неверный формат ID
                    }
                }
            )
        }

        composable("station_list") {
            StationListScreen(
                // Изменено: преобразуем Long в String для навигации
                onStationClick = { stationIdLong -> navController.navigate("station_info/$stationIdLong") },
                onBack = { navController.popBackStack() },
                stationViewModel = stationViewModel
            )
        }

        // Маршрут для информации о станции с аргументом Long
        composable(
            "station_info/{stationId}",
            arguments = listOf(navArgument("stationId") { type = NavType.LongType }) // Указываем тип Long
        ) { backStackEntry ->
            // Получаем Long напрямую
            val stationId = backStackEntry.arguments?.getLong("stationId") ?: 0L
            StationInfoScreen(
                stationId = stationId, // Передаем Long
                onBack = { navController.popBackStack() },
                stationViewModel = stationViewModel,
                authViewModel = authViewModel
            )
        }

        composable("profile") {
            ProfileScreen(
                onBack = { navController.popBackStack() },
                authViewModel = authViewModel
            )
        }
    }
}