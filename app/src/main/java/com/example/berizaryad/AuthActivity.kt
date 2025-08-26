package com.example.berizaryad

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.berizaryad.ui.theme.BeriZaryadTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AuthActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        setContent {
            BeriZaryadTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AuthScreen(
                        onLoginSuccess = {
                            // Закрываем AuthActivity и возвращаемся в MainActivity
                            finish()
                        },
                        auth = auth,
                        db = db
                    )
                }
            }
        }
    }
}

@Composable
fun AuthScreen(
    onLoginSuccess: () -> Unit,
    auth: FirebaseAuth,
    db: FirebaseFirestore
) {
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isRegistering by remember { mutableStateOf(false)
    }
    var isLoading by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (isRegistering) "Регистрация" else "Вход",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text("Телефон (7XXXXXXXXXX)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Пароль") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (phone.length != 11 || !phone.startsWith("7")) {
                    Toast.makeText(context, "Введите корректный номер телефона (7XXXXXXXXXX)", Toast.LENGTH_LONG).show()
                    return@Button
                }
                if (password.length < 6) {
                    Toast.makeText(context, "Пароль должен содержать не менее 6 символов", Toast.LENGTH_LONG).show()
                    return@Button
                }

                isLoading = true
                if (isRegistering) {
                    // Регистрация
                    auth.createUserWithEmailAndPassword("$phone@example.com", password)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                val user = hashMapOf(
                                    "phone" to phone,
                                    "fio" to "", // ФИО будет заполнено позже или админом
                                    "role" to "admin" // По умолчанию роль worker
                                )
                                db.collection("users").document(phone)
                                    .set(user)
                                    .addOnSuccessListener {
                                        Toast.makeText(context, "Регистрация успешна", Toast.LENGTH_SHORT).show()
                                        onLoginSuccess()
                                    }
                                    .addOnFailureListener { e ->
                                        Toast.makeText(context, "Ошибка сохранения данных: ${e.message}", Toast.LENGTH_LONG).show()
                                        isLoading = false
                                    }
                            } else {
                                Toast.makeText(context, "Ошибка регистрации: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                                isLoading = false
                            }
                        }
                } else {
                    // Вход
                    auth.signInWithEmailAndPassword("$phone@example.com", password)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                Toast.makeText(context, "Вход выполнен", Toast.LENGTH_SHORT).show()
                                onLoginSuccess()
                            } else {
                                Toast.makeText(context, "Ошибка входа: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                                isLoading = false
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
            Text(if (isRegistering) "Зарегистрироваться" else "Войти")
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(onClick = { isRegistering = !isRegistering }) {
            Text(if (isRegistering) "Уже есть аккаунт? Войти" else "Нет аккаунта? Зарегистрироваться")
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(onClick = {
            auth.signInAnonymously()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(context, "Вход как гость", Toast.LENGTH_SHORT).show()
                        onLoginSuccess()
                    } else {
                        Toast.makeText(context, "Ошибка входа как гость: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }) {
            Text("Войти как гость")
        }
    }
}