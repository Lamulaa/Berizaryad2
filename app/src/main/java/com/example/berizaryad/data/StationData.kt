// Berizaryad/app/src/main/java/com/example/berizaryad/data/StationData.kt
package com.example.berizaryad.data

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import java.util.Date

/**
 * Модель данных для станции зарядки.
 * Структура соответствует данным в Firestore.
 * Поля, которые могут отсутствовать или быть null в Firestore, сделаны Nullable (String?).
 */
@IgnoreExtraProperties // Игнорировать поля в Firestore, которых нет в этой модели
data class StationData(
    @DocumentId
    val documentId: String = "", // ID документа в Firestore (всегда String, не может быть null)
    val id: Long = 0L,           // Уникальный числовой ID станции (Long)
    val number: String? = null,     // Номер станции (String?) - может быть null
    val address: String? = null,    // Адрес станции (String?) - может быть null
    val organization: String? = null, // Организация (String?) - может быть null
    val serviced: Boolean = false, // Статус обслуживания (Boolean) - не может быть null
    val servicedBy: String? = null, // Номер телефона пользователя, который обслужил (String?) - может быть null
    val servicedByName: String? = null, // ФИО пользователя, который обслужил (String?) - может быть null
    val servicedDate: Date? = null, // Дата и время обслуживания (Date?) - может быть null
    val slots: Int = 0,         // Количество слотов (Int) - не может быть null
    val status: String? = null, // Текстовый статус ("good", "bad" и т.д.) (String?) - может быть null
    val urgent: Boolean = false, // Приоритет ("Срочно" - true, "Хорошо" - false) (Boolean) - не может быть null
    val photoUrl: String? = null, // URL основного фото (String?) - может быть null
    val photoUrls: List<String> = emptyList(),
    val lastComment: String? = null, // Последний комментарий (String?) - может быть null
    val responsibleName: String? = null, // ФИО ответственного лица (String?) - может быть null
    val responsiblePhone: String? = null, // Телефон ответственного лица (String?) - может быть null
    val comments: List<Comment> = emptyList() // Список комментариев (List<Comment>) - не может быть null, но может быть пустым
)

/**
 * Модель для комментария.
 * Также сделаем поля nullable для надежности.
 */
@IgnoreExtraProperties
data class Comment(
    val text: String? = null,
    val userId: String? = null,
    val userName: String? = null,
    val timestamp: Date? = null
)