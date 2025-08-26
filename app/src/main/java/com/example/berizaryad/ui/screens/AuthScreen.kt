// app/src/main/java/com/example/berizaryad/ui/screens/AuthScreen.kt
package com.example.berizaryad.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

private const val TAG = "AuthScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    onNavigateToSearch: () -> Unit // Эта функция должна быть передана из AppNavigation
) {
    Log.d(TAG, "AuthScreen Composable started")
    var fio by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoginMode by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text(
            text = "Бери заряд",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Поле ФИО (только для регистрации)
        if (!isLoginMode) {
            OutlinedTextField(
                value = fio,
                onValueChange = { fio = it },
                label = { Text("ФИО") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Поле номера телефона
        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text("Номер телефона") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Поле пароля
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Пароль") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Кнопка действия (Вход/Регистрация)
        Button(
            onClick = {
                // Базовая валидация
                if (phone.isEmpty() || password.isEmpty() || (!isLoginMode && fio.isEmpty())) {
                    errorMessage = "Заполните все обязательные поля"
                    return@Button
                }

                if (!isLoginMode && password.length < 6) {
                    errorMessage = "Пароль должен быть не менее 6 символов"
                    return@Button
                }

                isLoading = true
                errorMessage = null

                // Формируем email из номера телефона
                val email = "$phone@example.com"

                if (isLoginMode) {
                    // --- ЛОГИКА ВХОДА ---
                    Log.d(TAG, "Attempting to sign in user: $email")
                    auth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener { task ->
                            isLoading = false
                            if (task.isSuccessful) {
                                Log.d(TAG, "Sign in successful for: $email")
                                // --- ПЕРЕХОД ПОСЛЕ УСПЕШНОГО ВХОДА ---
                                onNavigateToSearch() // ВАЖНО: Вызываем эту функцию
                                // --- КОНЕЦ ПЕРЕХОДА ---
                            } else {
                                val exception = task.exception
                                Log.w(TAG, "Sign in failed for: $email", exception)
                                errorMessage = when (exception?.message) {
                                    null -> "Неизвестная ошибка входа"
                                    else -> "Ошибка входа: ${exception.localizedMessage}"
                                }
                            }
                        }
                } else {
                    // --- ЛОГИКА РЕГИСТРАЦИИ ---
                    Log.d(TAG, "Attempting to create user: $email")
                    auth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener { authTask ->
                            if (authTask.isSuccessful) {
                                Log.d(TAG, "User creation successful for: $email")
                                // После успешной регистрации сразу выполняем вход
                                auth.signInWithEmailAndPassword(email, password)
                                    .addOnCompleteListener { signInTask ->
                                        if (signInTask.isSuccessful) {
                                            // Вход успешен, теперь сохраняем данные в Firestore
                                            // --- ИЗМЕНЕНО: Роль по умолчанию установлена на "admin" ---
                                            val user = hashMapOf(
                                                "fio" to fio,
                                                "phone" to phone,
                                                "role" to "admin" // РОЛЬ ПО УМОЛЧАНИЮ ДЛЯ НОВЫХ ПОЛЬЗОВАТЕЛЕЙ
                                            )
                                            // --- КОНЕЦ ИЗМЕНЕНИЯ ---

                                            db.collection("users").document(phone)
                                                .set(user)
                                                .addOnCompleteListener { saveTask ->
                                                    isLoading = false
                                                    if (saveTask.isSuccessful) {
                                                        Log.d(TAG, "User data saved for: $phone")
                                                        // --- ПЕРЕХОД ПОСЛЕ УСПЕШНОЙ РЕГИСТРАЦИИ И СОХРАНЕНИЯ ---
                                                        onNavigateToSearch() // ВАЖНО: Вызываем эту функцию
                                                        // --- КОНЕЦ ПЕРЕХОДА ---
                                                    } else {
                                                        val saveException = saveTask.exception
                                                        Log.w(TAG, "Failed to save user data for: $phone", saveException)
                                                        errorMessage = "Ошибка сохранения данных: ${saveException?.localizedMessage ?: "Неизвестная ошибка"}"
                                                        // Опционально: можно попытаться удалить созданного пользователя
                                                        // auth.currentUser?.delete()
                                                    }
                                                }
                                        } else {
                                            // Ошибка автоматического входа после регистрации
                                            isLoading = false
                                            val signInException = signInTask.exception
                                            Log.w(TAG, "Auto sign-in failed after registration for: $email", signInException)
                                            errorMessage = "Регистрация успешна, но вход не удался: ${signInException?.localizedMessage ?: "Попробуйте войти вручную"}"
                                        }
                                    }
                            } else {
                                val authException = authTask.exception
                                isLoading = false
                                Log.w(TAG, "User creation failed for: $email", authException)
                                errorMessage = when (authException?.message) {
                                    null -> "Неизвестная ошибка регистрации"
                                    else -> "Ошибка регистрации: ${authException.localizedMessage}"
                                }
                            }
                        }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            if (isLoading) {
                // Индикатор загрузки внутри кнопки
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Icon(Icons.Default.Person, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (isLoginMode) "Войти" else "Зарегистрироваться")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Кнопка переключения режима
        OutlinedButton(
            onClick = {
                isLoginMode = !isLoginMode
                errorMessage = null // Очищаем ошибку при переключении
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            Text(if (isLoginMode) "Нет аккаунта? Зарегистрироваться" else "Уже есть аккаунт? Войти")
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
    Log.d(TAG, "AuthScreen Composable finished rendering")
}