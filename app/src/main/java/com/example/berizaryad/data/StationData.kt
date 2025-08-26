// Berizaryad/app/src/main/java/com/example/berizaryad/data/StationData.kt
package com.example.berizaryad.data

import java.util.Date

/**
 * Модель данных для станции зарядки.
 * Структура соответствует данным в Firestore.
 * ID теперь Long.
 */
data class StationData(
    // Тип ID - Long
    val id: Long = 0L,
    val number: String = "",
    val address: String = "",
    val organization: String = "",
    val serviced: Boolean = false,
    val servicedBy: String? = null,
    val servicedByName: String? = null,
    val servicedDate: Date? = null,
    val photoUrl: String = "",
    val photoUrls: List<String> = emptyList(),
    val comments: List<Comment> = emptyList(),
    val urgent: Boolean = false,
    // Поля из вашего скриншота Firebase
    val trustedPersonFio: String = "",
    val trustedPersonPhone: String = ""
    // Добавьте другие поля, если они есть в Firestore
)

/**
 * Модель для комментариев.
 */
data class Comment(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val text: String = "",
    val timestamp: Date = Date()
)