// Berizaryad/app/src/main/java/com/example/berizaryad/ui/screens/ProfileScreen.kt
package com.example.berizaryad.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.berizaryad.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBack: () -> Unit, // Эта функция должна привести к переходу на предыдущий экран или экран авторизации
    authViewModel: AuthViewModel
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    var fio by remember { mutableStateOf(currentUser?.fio ?: "") }
    var isUpdating by remember { mutableStateOf(false) }

    // Наблюдаем за состоянием пользователя. Если он стал null, переходим назад.
    LaunchedEffect(currentUser) {
        if (currentUser == null) {
            // Это сработает, если ViewModel обновит состояние после logout()
            onBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Профиль") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
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
            Text(
                text = "Информация о пользователе",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            OutlinedTextField(
                value = currentUser?.phone ?: "",
                onValueChange = {},
                label = { Text("Телефон") },
                enabled = false,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = fio,
                onValueChange = { fio = it },
                label = { Text("ФИО") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    isUpdating = true
                    authViewModel.updateFio(fio) {
                        isUpdating = false
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isUpdating
            ) {
                if (isUpdating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Сохранить")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Кнопка "Выйти"
            Button(
                onClick = {
                    // Вызываем logout в ViewModel
                    authViewModel.logout()
                    // onBack() может быть вызван LaunchedEffect выше или напрямую здесь
                    // onBack()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Выйти", color = MaterialTheme.colorScheme.onError)
            }
        }
    }
}