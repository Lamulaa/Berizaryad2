// Berizaryad/app/src/main/java/com/example/berizaryad/ui/screens/AuthScreen.kt
package com.example.berizaryad.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.berizaryad.viewmodel.AuthViewModel
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    onLoginSuccess: () -> Unit,
    authViewModel: AuthViewModel
) {
    val context = LocalContext.current
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isRegistering by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var passwordVisibility by remember { mutableStateOf(false) }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Бери заряд",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it.filter { char -> char.isDigit() } }, // Разрешаем только цифры
            label = { Text("Номер телефона (7XXXXXXXXXX)") },
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            isError = errorMessage?.contains("телефон") == true
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Пароль") },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
            trailingIcon = {
                IconButton(onClick = { passwordVisibility = !passwordVisibility }) {
                    Icon(
                        imageVector = if (passwordVisibility) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = if (passwordVisibility) "Скрыть пароль" else "Показать пароль"
                    )
                }
            },
            visualTransformation = if (passwordVisibility) VisualTransformation.None else PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            isError = errorMessage?.contains("пароль") == true
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (errorMessage != null) {
            Text(
                text = errorMessage!!,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                // Сброс ошибки перед новой попыткой
                errorMessage = null

                // Валидация
                if (phone.length != 11 || !phone.startsWith("7")) {
                    errorMessage = "Введите корректный номер телефона (7XXXXXXXXXX)"
                    return@Button
                }
                if (password.length < 6) {
                    errorMessage = "Пароль должен содержать не менее 6 символов"
                    return@Button
                }

                isLoading = true
                val email = "$phone@example.com"

                if (isRegistering) {
                    // Регистрация
                    authViewModel.register(email, password) { errorMsg ->
                        isLoading = false
                        if (errorMsg != null) {
                            errorMessage = errorMsg
                        } else {
                            Toast.makeText(context, "Регистрация успешна", Toast.LENGTH_SHORT).show()
                            onLoginSuccess()
                        }
                    }
                } else {
                    // Вход
                    authViewModel.login(email, password) { errorMsg ->
                        isLoading = false
                        if (errorMsg != null) {
                            errorMessage = errorMsg
                        } else {
                            Toast.makeText(context, "Вход выполнен", Toast.LENGTH_SHORT).show()
                            onLoginSuccess()
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Icon(Icons.Default.Person, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (isRegistering) "Зарегистрироваться" else "Войти")
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = { isRegistering = !isRegistering }) {
            Text(if (isRegistering) "Уже есть аккаунт? Войти" else "Нет аккаунта? Зарегистрироваться")
        }
    }
}