// Berizaryad/app/src/main/java/com/example/berizaryad/viewmodel/StationViewModel.kt
package com.example.berizaryad.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.berizaryad.data.Comment
import com.example.berizaryad.data.StationData
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.*

class StationViewModel(private val db: FirebaseFirestore) : ViewModel() {

    private val _stations = MutableStateFlow<List<StationData>>(emptyList())
    val stations: StateFlow<List<StationData>> = _stations

    private val _station = MutableStateFlow<StationData?>(null)
    val station: StateFlow<StationData?> = _station

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _searchResult = MutableStateFlow<StationData?>(null)
    val searchResult: StateFlow<StationData?> = _searchResult

    // Функция для загрузки ВСЕХ станций
    fun loadAllStations(onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                _errorMessage.value = null
                val result = db.collection("stations").get().await()

                val stationsList = result.documents.mapNotNull { document ->
                    try {
                        // Попытка десериализации. Предполагаем, что поле 'id' в Firestore - Long.
                        val station: StationData? = document.toObject(StationData::class.java)
                        station?.copy(id = document.getLong("id") ?: 0L) // Явно получаем Long 'id'
                    } catch (e: Exception) {
                        Log.e("StationViewModel", "Ошибка десериализации станции ${document.id}", e)
                        null
                    }
                }
                _stations.value = stationsList
                onComplete(true)
            } catch (e: Exception) {
                _errorMessage.value = e.message
                Log.e("StationViewModel", "Ошибка загрузки списка станций", e)
                onComplete(false)
            }
        }
    }

    // Функция поиска станции по ID
    fun searchStationById(stationId: Long) {
        viewModelScope.launch {
            try {
                _errorMessage.value = null
                _searchResult.value = null

                // Ищем по полю 'id' (Long) или по ID документа (String)
                val querySnapshot = db.collection("stations")
                    .whereEqualTo("id", stationId) // Поиск по полю 'id' (Long)
                    .limit(1)
                    .get()
                    .await()

                if (!querySnapshot.isEmpty) {
                    val doc = querySnapshot.documents.first()
                    val station: StationData? = doc.toObject(StationData::class.java)?.copy(id = stationId)
                    _searchResult.value = station
                } else {
                    // Если не найдено по полю 'id', ищем по ID документа (String)
                    val documentSnapshot = db.collection("stations").document(stationId.toString()).get().await()
                    if (documentSnapshot.exists()) {
                        val station: StationData? = documentSnapshot.toObject(StationData::class.java)?.copy(id = stationId)
                        _searchResult.value = station
                    } else {
                        _searchResult.value = null
                        _errorMessage.value = "Станция не найдена"
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message
                Log.e("StationViewModel", "Ошибка поиска станции", e)
                _searchResult.value = null
            }
        }
    }

    // Загрузка конкретной станции с комментариями
    fun loadStation(stationId: Long) { // Принимаем Long
        viewModelScope.launch {
            try {
                _errorMessage.value = null
                // Ищем по полю 'id' (Long) или по ID документа (String)
                val querySnapshot = db.collection("stations")
                    .whereEqualTo("id", stationId) // Поиск по полю 'id' (Long)
                    .limit(1)
                    .get()
                    .await()

                val documentSnapshot = if (!querySnapshot.isEmpty) {
                    querySnapshot.documents.first()
                } else {
                    // Если не найдено по полю 'id', ищем по ID документа (String)
                    db.collection("stations").document(stationId.toString()).get().await()
                }

                if (documentSnapshot.exists()) {
                    val station: StationData? = documentSnapshot.toObject(StationData::class.java)?.copy(id = stationId)

                    if (station != null) {
                        // Загружаем комментарии
                        val commentsResult = db.collection("stations").document(documentSnapshot.id)
                            .collection("comments")
                            .orderBy("timestamp")
                            .get()
                            .await()
                        val commentsList = commentsResult.documents.mapNotNull { commentDoc ->
                            val comment: Comment? = commentDoc.toObject(Comment::class.java)
                            if (comment != null) {
                                comment.copy(id = commentDoc.id)
                            } else {
                                null
                            }
                        }

                        _station.value = station.copy(comments = commentsList)
                    } else {
                        _errorMessage.value = "Ошибка десериализации данных станции"
                    }
                } else {
                    _errorMessage.value = "Станция не найдена"
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message
                Log.e("StationViewModel", "Error loading station", e)
            }
        }
    }

    // Обновление статуса "Срочно/Хорошо"
    fun updateUrgentStatus(stationId: Long, isUrgent: Boolean, callback: (String?) -> Unit) { // Принимаем Long
        viewModelScope.launch {
            try {
                // Найдем документ по stationId (Long)
                val querySnapshot = db.collection("stations")
                    .whereEqualTo("id", stationId)
                    .limit(1)
                    .get()
                    .await()

                val documentRef = if (!querySnapshot.isEmpty) {
                    db.collection("stations").document(querySnapshot.documents.first().id)
                } else {
                    db.collection("stations").document(stationId.toString())
                }

                documentRef.update("urgent", isUrgent).await()
                _station.value = _station.value?.copy(urgent = isUrgent)
                callback(null)
            } catch (e: Exception) {
                callback(e.message)
                Log.e("StationViewModel", "Error updating urgent status", e)
            }
        }
    }

    // Отметить станцию как обслуженную
    fun markAsServiced(stationId: Long, commentText: String, authorName: String, callback: (String?) -> Unit) { // Принимаем Long
        viewModelScope.launch {
            try {
                val batch = db.batch()

                // Найдем документ по stationId (Long)
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

                batch.update(stationRef, mapOf(
                    "serviced" to true,
                    "servicedBy" to authorName,
                    "servicedDate" to Date()
                ))

                if (commentText.isNotBlank()) {
                    val commentRef = stationRef.collection("comments").document()
                    val comment = Comment(
                        id = commentRef.id,
                        userId = authorName,
                        userName = authorName,
                        text = commentText,
                        timestamp = Date()
                    )
                    batch.set(commentRef, comment)
                }

                batch.commit().await()
                loadStation(stationId) // Перезагружаем станцию с новыми комментариями
                callback(null)
            } catch (e: Exception) {
                callback(e.message)
                Log.e("StationViewModel", "Error marking station as serviced", e)
            }
        }
    }

    // Обновить информацию о доверенном лице
    fun updateTrustedPerson(stationId: Long, fio: String, phone: String, callback: (String?) -> Unit) { // Принимаем Long
        viewModelScope.launch {
            try {
                // Найдем документ по stationId (Long)
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

                stationRef.update(
                    mapOf(
                        "trustedPersonFio" to fio,
                        "trustedPersonPhone" to phone
                    )
                ).await()
                _station.value = _station.value?.copy(
                    trustedPersonFio = fio,
                    trustedPersonPhone = phone
                )
                callback(null)
            } catch (e: Exception) {
                callback(e.message)
                Log.e("StationViewModel", "Error updating trusted person", e)
            }
        }
    }

    // Загрузка фото
    fun uploadPhoto(data: ByteArray, stationId: Long, callback: (String?, String?) -> Unit) {
        viewModelScope.launch {
            try {
                val storage = FirebaseStorage.getInstance()
                val storageRef = storage.reference.child("stations/${stationId}_${System.currentTimeMillis()}.jpg")

                // Используем параметр data
                val uploadTask = storageRef.putBytes(data)
                val url = uploadTask.await().storage.downloadUrl.await().toString()
                callback(url, null)
            } catch (e: Exception) {
                callback(null, e.message)
                Log.e("StationViewModel", "Error uploading photo", e)
            }
        }
    }

    // Обновление URL фото в Firestore
    fun updatePhotoUrl(stationId: Long, photoUrl: String, callback: (String?) -> Unit) { // Принимаем Long
        viewModelScope.launch {
            try {
                // Найдем документ по stationId (Long)
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

                val currentPhotoUrls = stationRef.get().await().get("photoUrls") as? List<String> ?: emptyList()
                val updatedPhotoUrls = currentPhotoUrls + photoUrl

                stationRef.update("photoUrls", updatedPhotoUrls).await()
                _station.value = _station.value?.copy(photoUrls = updatedPhotoUrls)
                callback(null)
            } catch (e: Exception) {
                callback(e.message)
                Log.e("StationViewModel", "Error updating photo URL", e)
            }
        }
    }

    // Сбросить статус обслуживания
    fun resetServiceStatus(stationId: Long, callback: (String?) -> Unit) { // Принимаем Long
        viewModelScope.launch {
            try {
                val batch = db.batch()

                // Найдем документ по stationId (Long)
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

                batch.update(stationRef, mapOf(
                    "serviced" to false,
                    "servicedBy" to null,
                    "servicedDate" to null
                ))

                val commentsSnapshot = stationRef.collection("comments").get().await()
                for (doc in commentsSnapshot.documents) {
                    batch.delete(doc.reference)
                }

                batch.commit().await()
                loadStation(stationId) // Перезагружаем станцию
                callback(null)
            } catch (e: Exception) {
                callback(e.message)
                Log.e("StationViewModel", "Error resetting service status", e)
            }
        }
    }
}