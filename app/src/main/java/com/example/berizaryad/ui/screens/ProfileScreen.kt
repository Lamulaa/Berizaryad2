// Berizaryad/app/src/main/java/com/example/berizaryad/ui/screens/ProfileScreen.kt
package com.example.berizaryad.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    auth: FirebaseAuth,
    onLogout: () -> Unit
) {
    val currentUser = auth.currentUser
    var userEmail by remember { mutableStateOf("") }
    var userPhone by remember { mutableStateOf("") }

    // Получаем информацию о пользователе
    LaunchedEffect(currentUser) {
        userEmail = currentUser?.email ?: "Неизвестный email"
        // Предполагаем, что телефон хранится в email до символа '@'
        userPhone = currentUser?.email?.substringBefore("@") ?: "Неизвестный телефон"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Профиль") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Аватар пользователя (заглушка)
            Card(
                modifier = Modifier.size(100.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Аватар",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(60.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))

            // Информация о пользователе
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.outlinedCardColors()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Информация о пользователе",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Email: $userEmail")
                    Text(text = "Телефон: $userPhone")
                    // Роль можно добавить, если она будет храниться в Firestore
                    // Text(text = "Роль: ${userRole ?: 'Не определена'}")
                }
            }
            Spacer(modifier = Modifier.height(24.dp))

            // Кнопка выхода
            Button(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.Logout, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Выйти")
            }
        }
    }
}