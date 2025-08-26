// Berizaryad/app/src/main/java/com/example/berizaryad/MainActivity.kt
package com.example.berizaryad

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.berizaryad.navigation.AppNavigation
import com.example.berizaryad.ui.theme.BeriZaryadTheme
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Определяем начальный маршрут
        val currentUser = FirebaseAuth.getInstance().currentUser
        val startDestination = if (currentUser != null) {
            "search" // Если авторизован, переходим к главному экрану
        } else {
            "auth" // Если нет - к экрану авторизации
        }

        setContent {
            BeriZaryadTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(startDestination = startDestination)
                }
            }
        }
    }
}