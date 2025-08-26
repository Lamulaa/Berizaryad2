// Berizaryad/app/src/main/java/com/example/berizaryad/ui/screens/SearchStationScreen.kt
package com.example.berizaryad.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.berizaryad.viewmodel.AuthViewModel
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchStationScreen(
    onNavigateToStationList: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onLoginClick: () -> Unit,
    authViewModel: AuthViewModel,
    // Добавим callback для перехода к найденной станции
    onNavigateToFoundStation: (String) -> Unit
) {
    var stationNumber by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val currentUser by authViewModel.currentUser.collectAsState()

    val db = FirebaseFirestore.getInstance()

    // Проверяем, авторизован ли пользователь
    val isLoggedIn = currentUser != null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (isLoggedIn) {
            // Если пользователь авторизован
            Text(
                text = "Добро пожаловать, ${currentUser?.fio ?: currentUser?.phone ?: "Пользователь"}!",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            OutlinedTextField(
                value = stationNumber,
                onValueChange = { newValue ->
                    // Ограничиваем ввод только цифрами
                    stationNumber = newValue.filter { it.isDigit() }
                },
                label = { Text("Введите номер станции") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    val trimmedNumber = stationNumber.trim()
                    if (trimmedNumber.isNotEmpty()) {
                        isLoading = true
                        errorMessage = null

                        // --- ИСПРАВЛЕННЫЙ ПОИСК ---
                        // Ищем документ, где поле "number" равно введенному значению
                        db.collection("stations")
                            .whereEqualTo("id", trimmedNumber) // Ищем по полю number
                            .limit(1) // Ограничиваем 1 результатом
                            .get()
                            .addOnSuccessListener { querySnapshot ->
                                isLoading = false
                                if (!querySnapshot.isEmpty) {
                                    // Найден документ по полю number
                                    val document = querySnapshot.documents.first()
                                    // Переходим к экрану информации о найденной станции
                                    onNavigateToFoundStation(document.id)
                                } else {
                                    errorMessage = "Станция с номером $trimmedNumber не найдена."
                                    Log.d("SearchStationScreen", "Station not found by number: $trimmedNumber")
                                }
                            }
                            .addOnFailureListener { exception ->
                                isLoading = false
                                errorMessage = exception.message ?: "Ошибка поиска"
                                Log.w("SearchStationScreen", "Error searching for station by number: $trimmedNumber", exception)
                            }
                        // --- КОНЕЦ ИСПРАВЛЕННОГО ПОИСКА ---
                    } else {
                        errorMessage = "Введите номер станции"
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = stationNumber.isNotEmpty() && !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Icon(Icons.Default.Search, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Поиск")
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Кнопка "Список"
            OutlinedButton(
                onClick = onNavigateToStationList, // Переход к списку станций
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.AutoMirrored.Filled.List, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Список всех станций")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Кнопка профиля
            OutlinedButton(
                onClick = onNavigateToProfile,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Person, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Профиль")
            }

        } else {
            // Если пользователь НЕ авторизован
            Text(
                text = "Добро пожаловать!",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            Button(
                onClick = onLoginClick, // Переход на экран логина
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Войти")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Отображение ошибки
        errorMessage?.let { message ->
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}