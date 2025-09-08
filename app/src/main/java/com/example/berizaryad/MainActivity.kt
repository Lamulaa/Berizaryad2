// Berizaryad/app/src/main/java/com/example/berizaryad/MainActivity.kt
package com.example.berizaryad

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.berizaryad.ui.theme.BeriZaryadTheme
import com.google.firebase.auth.FirebaseAuth
import com.example.berizaryad.viewmodel.AuthViewModel
import com.example.berizaryad.viewmodel.StationViewModel

class MainActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var authViewModel: AuthViewModel
    private lateinit var stationViewModel: StationViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()
        authViewModel = AuthViewModel()
        stationViewModel = StationViewModel()

        setContent {
            BeriZaryadTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Передаем необходимые ViewModel'ы в навигацию
                    AppNavigation(auth, authViewModel, stationViewModel)
                }
            }
        }
    }
}