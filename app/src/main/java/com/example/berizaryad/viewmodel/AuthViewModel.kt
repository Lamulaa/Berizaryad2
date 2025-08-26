// Berizaryad/app/src/main/java/com/example/berizaryad/viewmodel/AuthViewModel.kt
package com.example.berizaryad.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class UserData(
    val phone: String,
    val fio: String?
)

class AuthViewModel(private val auth: FirebaseAuth, private val db: FirebaseFirestore) : ViewModel() {

    private val _currentUser = MutableStateFlow<UserData?>(null)
    val currentUser: StateFlow<UserData?> = _currentUser

    // Добавляем слушатель изменений состояния аутентификации
    private val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        val user = firebaseAuth.currentUser
        if (user != null) {
            // Пользователь вошел
            loadUserData(user)
        } else {
            // Пользователь вышел
            _currentUser.value = null
        }
    }

    init {
        // Регистрируем слушатель при инициализации ViewModel
        auth.addAuthStateListener(authStateListener)
    }

    override fun onCleared() {
        super.onCleared()
        // Отменяем регистрацию слушателя при уничтожении ViewModel
        auth.removeAuthStateListener(authStateListener)
    }

    private fun loadUserData(user: FirebaseUser) {
        viewModelScope.launch {
            try {
                val phone = user.email?.substringBefore("@") ?: ""
                val document = db.collection("users").document(phone).get().await()
                if (document.exists()) {
                    val fio = document.getString("fio")
                    _currentUser.value = UserData(phone, fio)
                } else {
                    // Если документа нет, можно создать или оставить null
                    _currentUser.value = UserData(phone, "")
                }
            } catch (e: Exception) {
                // Обработка ошибки загрузки данных
                _currentUser.value = null
            }
        }
    }

    fun updateFio(fio: String, callback: () -> Unit) {
        viewModelScope.launch {
            val phone = _currentUser.value?.phone
            if (phone != null) {
                try {
                    db.collection("users").document(phone).update("fio", fio).await()
                    _currentUser.value = _currentUser.value?.copy(fio = fio)
                    callback()
                } catch (e: Exception) {
                    // Обработка ошибки
                    callback()
                }
            } else {
                callback()
            }
        }
    }

    fun logout() {
        // Выход из Firebase. Слушатель authStateListener обновит _currentUser
        auth.signOut()
        // Навигация должна быть обработана в UI слое (например, в ProfileScreen)
    }
}