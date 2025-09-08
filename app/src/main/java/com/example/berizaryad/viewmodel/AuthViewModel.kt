// Berizaryad/app/src/main/java/com/example/berizaryad/viewmodel/AuthViewModel.kt
package com.example.berizaryad.viewmodel

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException

class AuthViewModel : ViewModel() {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    // Свойство для получения текущего пользователя
    val currentUser get() = auth.currentUser

    /**
     * Регистрация нового пользователя.
     * @param email Email пользователя (формат: 7XXXXXXXXXX@example.com).
     * @param password Пароль пользователя.
     * @param callback Функция обратного вызова. Принимает null при успехе или строку с ошибкой.
     */
    fun register(email: String, password: String, callback: (String?) -> Unit) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Регистрация успешна
                    callback(null)
                } else {
                    // Обработка ошибок регистрации
                    val exception = task.exception
                    val errorMessage = when (exception) {
                        is FirebaseAuthWeakPasswordException -> "Пароль слишком слабый."
                        is FirebaseAuthInvalidCredentialsException -> "Некорректный email."
                        is FirebaseAuthUserCollisionException -> "Пользователь с таким email уже существует."
                        else -> exception?.message ?: "Неизвестная ошибка регистрации."
                    }
                    callback(errorMessage)
                }
            }
    }

    /**
     * Вход пользователя в систему.
     * @param email Email пользователя (формат: 7XXXXXXXXXX@example.com).
     * @param password Пароль пользователя.
     * @param callback Функция обратного вызова. Принимает null при успехе или строку с ошибкой.
     */
    fun login(email: String, password: String, callback: (String?) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Вход успешен
                    callback(null)
                } else {
                    // Обработка ошибок входа
                    val exception = task.exception
                    val errorMessage = when (exception) {
                        is FirebaseAuthInvalidCredentialsException -> "Неверный email или пароль."
                        is FirebaseAuthUserCollisionException -> "Аккаунт с этим email заблокирован."
                        else -> exception?.message ?: "Неизвестная ошибка входа."
                    }
                    callback(errorMessage)
                }
            }
    }

    /**
     * Выход пользователя из системы.
     */
    fun logout() {
        auth.signOut()
    }
}