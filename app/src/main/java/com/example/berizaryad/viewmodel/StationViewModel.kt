// Berizaryad/app/src/main/java/com/example/berizaryad/viewmodel/StationViewModel.kt
package com.example.berizaryad.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.berizaryad.data.StationData
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date

/**
 * ViewModel для управления данными станций.
 * Отвечает за загрузку, обновление и обслуживание станций.
 */
class StationViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val currentUserPhone: String
        get() = auth.currentUser?.phoneNumber ?: ""

    companion object {
        private const val TAG = "StationViewModel"
    }

    /**
     * Загружает список всех станций из Firestore.
     * @param callback Функция обратного вызова. Принимает список станций (List<StationData>) и сообщение об ошибке (String?).
     */
    fun loadStations(
        callback: (List<StationData>?, String?) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val snapshot = db.collection("stations").get().await()
                val stations = snapshot.documents.mapNotNull { document ->
                    document.toObject<StationData>()?.copy(documentId = document.id)
                }
                callback(stations, null)
            } catch (e: Exception) {
                Log.e(TAG, "Error loading stations", e)
                callback(null, e.message)
            }
        }
    }

    /**
     * Загружает данные конкретной станции по её ID.
     * @param stationId ID станции (Long).
     * @param callback Функция обратного вызова. Принимает данные станции (StationData?) и сообщение об ошибке (String?).
     */
    fun loadStation(
        stationId: Long,
        callback: (StationData?, String?) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val querySnapshot = db.collection("stations")
                    .whereEqualTo("id", stationId)
                    .limit(1)
                    .get()
                    .await()

                if (!querySnapshot.isEmpty) {
                    val document = querySnapshot.documents.first()
                    val station = document.toObject<StationData>()?.copy(documentId = document.id)
                    callback(station, null)
                } else {
                    callback(null, "Станция с ID $stationId не найдена")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading station with id $stationId", e)
                callback(null, e.message)
            }
        }
    }

    /**
     * Помечает станцию как обслуженную.
     * @param stationId ID станции (Long).
     * @param commentText Текст комментария (String?).
     * @param callback Функция обратного вызова. Принимает сообщение об ошибке (String?).
     */
    fun markAsServiced(
        stationId: Long,
        commentText: String?,
        callback: (String?) -> Unit
    ) {
        viewModelScope.launch {
            try {
                // Найдем ссылку на документ станции
                val querySnapshot = db.collection("stations")
                    .whereEqualTo("id", stationId)
                    .limit(1)
                    .get()
                    .await()

                val stationRef = if (!querySnapshot.isEmpty) {
                    db.collection("stations").document(querySnapshot.documents.first().id)
                } else {
                    // Если документ не найден по id, создаем ссылку по строковому представлению id
                    // (на случай, если id хранится как имя документа, хотя это маловероятно при авто-ID)
                    db.collection("stations").document(stationId.toString())
                }

                // Подготавливаем данные для обновления
                val updateData = hashMapOf<String, Any?>(
                    "serviced" to true,
                    "servicedBy" to currentUserPhone,
                    "servicedByName" to currentUserPhone, // Можно заменить на настоящее имя
                    "servicedDate" to Timestamp(Date()),
                    "lastComment" to (commentText ?: "")
                )

                // Обновляем статус станции
                stationRef.update(updateData).await()

                // Добавляем комментарий в подколлекцию, если текст не пустой
                if (!commentText.isNullOrBlank()) {
                    val commentRef = stationRef.collection("comments").document()
                    val comment = mapOf(
                        "text" to commentText,
                        "userId" to currentUserPhone,
                        "userName" to currentUserPhone, // Можно заменить на настоящее имя
                        "timestamp" to Date()
                    )
                    commentRef.set(comment).await()
                }

                // Перезагружаем станцию, чтобы получить обновленные данные
                loadStation(stationId, callback = { _, _ -> callback(null) })
            } catch (e: Exception) {
                Log.e(TAG, "Error marking station as serviced", e)
                callback(e.message)
            }
        }
    }

    /**
     * Обновляет статус срочности станции.
     * @param stationId ID станции (Long).
     * @param isUrgent Новый статус срочности (Boolean).
     * @param callback Функция обратного вызова. Принимает сообщение об ошибке (String?).
     */
    fun updateUrgentStatus(
        stationId: Long,
        isUrgent: Boolean,
        callback: (String?) -> Unit
    ) {
        viewModelScope.launch {
            try {
                // Найдем ссылку на документ станции
                val querySnapshot = db.collection("stations")
                    .whereEqualTo("id", stationId)
                    .limit(1)
                    .get()
                    .await()

                val stationRef = if (!querySnapshot.isEmpty) {
                    db.collection("stations").document(querySnapshot.documents.first().id)
                } else {
                    db.collection("stations").document(stationId.toString())
                }

                // Обновляем поле urgent
                stationRef.update("urgent", isUrgent).await()
                callback(null)
            } catch (e: Exception) {
                Log.e(TAG, "Error updating urgent status", e)
                callback(e.message)
            }
        }
    }


    /**
     * Обновляет информацию о доверенном лице станции.
     * @param stationId ID станции (Long).
     * @param fio ФИО доверенного лица (String).
     * @param phone Телефон доверенного лица (String).
     * @param callback Функция обратного вызова. Принимает сообщение об ошибке (String?).
     */
    fun updateTrustedPerson(
        stationId: Long,
        fio: String,
        phone: String,
        callback: (String?) -> Unit
    ) {
        viewModelScope.launch {
            try {
                // Найдем ссылку на документ станции
                val querySnapshot = db.collection("stations")
                    .whereEqualTo("id", stationId)
                    .limit(1)
                    .get()
                    .await()

                val stationRef = if (!querySnapshot.isEmpty) {
                    db.collection("stations").document(querySnapshot.documents.first().id)
                } else {
                    db.collection("stations").document(stationId.toString())
                }

                // Обновляем поля responsibleName и responsiblePhone
                val updateData = hashMapOf<String, Any>(
                    "responsibleName" to fio,
                    "responsiblePhone" to phone
                )
                stationRef.update(updateData).await()
                callback(null)
            } catch (e: Exception) {
                Log.e(TAG, "Error updating trusted person", e)
                callback(e.message)
            }
        }
    }

    // --- Методы для работы с комментариями ---

    /**
     * Загружает комментарии для конкретной станции.
     * @param stationId ID станции (Long).
     * @param callback Функция обратного вызова. Принимает список комментариев (List<Map<String, Any?>>) и сообщение об ошибке (String?).
     */
    fun loadComments(
        stationId: Long,
        callback: (List<Map<String, Any?>>?, String?) -> Unit
    ) {
        viewModelScope.launch {
            try {
                // Найдем документ станции
                val querySnapshot = db.collection("stations")
                    .whereEqualTo("id", stationId)
                    .limit(1)
                    .get()
                    .await()

                if (!querySnapshot.isEmpty) {
                    val stationDoc = querySnapshot.documents.first()
                    // Получаем подколлекцию комментариев
                    val commentsSnapshot = stationDoc.reference.collection("comments")
                        .orderBy("timestamp") // Сортировка по времени
                        .get()
                        .await()
                    val comments = commentsSnapshot.documents.map { it.data ?: emptyMap() }
                    callback(comments, null)
                } else {
                    callback(null, "Станция с ID $stationId не найдена для загрузки комментариев")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading comments for station $stationId", e)
                callback(null, e.message)
            }
        }
    }

    /**
     * Добавляет новый комментарий к станции.
     * @param stationId ID станции (Long).
     * @param text Текст комментария (String).
     * @param callback Функция обратного вызова. Принимает сообщение об ошибке (String?).
     */
    fun addComment(
        stationId: Long,
        text: String,
        callback: (String?) -> Unit
    ) {
        viewModelScope.launch {
            try {
                // Найдем документ станции
                val querySnapshot = db.collection("stations")
                    .whereEqualTo("id", stationId)
                    .limit(1)
                    .get()
                    .await()

                if (!querySnapshot.isEmpty) {
                    val stationDoc = querySnapshot.documents.first()
                    val stationRef = stationDoc.reference

                    // Создаем новый комментарий
                    val newComment = hashMapOf(
                        "text" to text,
                        "userId" to currentUserPhone,
                        "userName" to currentUserPhone, // Можно заменить на настоящее имя
                        "timestamp" to Date()
                    )

                    // Добавляем комментарий в подколлекцию
                    val commentRef = stationRef.collection("comments").document()
                    commentRef.set(newComment).await()

                    // Обновляем поле lastComment в основном документе станции
                    stationRef.update("lastComment", text).await()

                    callback(null)
                } else {
                    callback("Станция с ID $stationId не найдена для добавления комментария")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error adding comment to station $stationId", e)
                callback(e.message)
            }
        }
    }

    // --- Методы для работы с несколькими станциями ---

    /**
     * Помечает несколько станций как обслуженные.
     * @param stationIds Список ID станций (List<Long>).
     * @param commentText Текст комментария (String?).
     * @param callback Функция обратного вызова. Принимает сообщение об ошибке (String?).
     */
    fun markMultipleAsServiced(
        stationIds: List<Long>,
        commentText: String?,
        callback: (String?) -> Unit
    ) {
        viewModelScope.launch {
            try {
                // Создаем список Deferred задач для поиска документов
                val deferredRefs = stationIds.map { stationId ->
                    async {
                        val querySnapshot = db.collection("stations")
                            .whereEqualTo("id", stationId)
                            .limit(1)
                            .get()
                            .await()
                        if (!querySnapshot.isEmpty) {
                            querySnapshot.documents.first().id to db.collection("stations").document(querySnapshot.documents.first().id)
                        } else {
                            stationId.toString() to db.collection("stations").document(stationId.toString())
                        }
                    }
                }

                // Дожидаемся завершения всех задач поиска
                val stationRefs = deferredRefs.awaitAll().associate { it }

                // Создаем batch для обновления
                val batch = db.batch()

                // Добавляем операции обновления в batch
                stationRefs.values.forEach { stationRef ->
                    val updateData = hashMapOf<String, Any?>(
                        "serviced" to true,
                        "servicedBy" to currentUserPhone,
                        "servicedByName" to currentUserPhone,
                        "servicedDate" to Timestamp(Date()),
                        "lastComment" to (commentText ?: "")
                    )
                    batch.update(stationRef, updateData)

                    // Добавляем комментарий в подколлекцию, если текст не пустой
                    if (!commentText.isNullOrBlank()) {
                        val commentRef = stationRef.collection("comments").document()
                        val comment = mapOf(
                            "text" to commentText,
                            "userId" to currentUserPhone,
                            "userName" to currentUserPhone,
                            "timestamp" to Date()
                        )
                        batch.set(commentRef, comment)
                    }
                }

                // Выполняем batch
                batch.commit().await()
                callback(null)
            } catch (e: Exception) {
                Log.e(TAG, "Error marking multiple stations as serviced", e)
                callback(e.message)
            }
        }
    }
}