// Berizaryad/app/src/main/java/com/example/berizaryad/ui/screens/StationInfoScreen.kt
package com.example.berizaryad.ui.screens

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.berizaryad.data.StationData
import com.example.berizaryad.viewmodel.AuthViewModel
import com.example.berizaryad.viewmodel.StationViewModel
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StationInfoScreen(
    stationId: Long,
    onBack: () -> Unit,
    stationViewModel: StationViewModel,
    authViewModel: AuthViewModel
) {
    val station by stationViewModel.station.collectAsState()
    val errorMessage by stationViewModel.errorMessage.collectAsState()
    val currentUser by authViewModel.currentUser.collectAsState()
    val context = LocalContext.current

    var comment by remember { mutableStateOf("") }
    var isUpdating by remember { mutableStateOf(false) }
    var capturedImageBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var showCommentDialog by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }
    var trustedPersonFio by remember { mutableStateOf("") }
    var trustedPersonPhone by remember { mutableStateOf("") }

    // Локальные состояния для редактирования ответственного
    var editingTrustedFio by remember { mutableStateOf("") }
    var editingTrustedPhone by remember { mutableStateOf("") }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview(),
        onResult = { bitmap ->
            capturedImageBitmap = bitmap
        }
    )

    LaunchedEffect(stationId) {
        stationViewModel.loadStation(stationId)
        Log.d("StationInfoScreen", "Loading station with ID: $stationId")
    }

    LaunchedEffect(station) {
        station?.let {
            // Обновляем локальные состояния при изменении station
            trustedPersonFio = it.trustedPersonFio
            trustedPersonPhone = it.trustedPersonPhone
            editingTrustedFio = it.trustedPersonFio
            editingTrustedPhone = it.trustedPersonPhone
        }
    }

    // Диалог для ввода комментария
    if (showCommentDialog) {
        AlertDialog(
            onDismissRequest = { showCommentDialog = false },
            title = { Text("Добавить комментарий") },
            text = {
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("Комментарий") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCommentDialog = false
                        isUpdating = true
                        // Исправлено: передаем stationId как String и currentUser?.phone
                        stationViewModel.markAsServiced(
                            stationId,
                            comment,
                            currentUser?.phone ?: "Неизвестный пользователь"
                        ) { errorMsg ->
                            isUpdating = false
                            if (errorMsg != null) {
                                // Обработка ошибки
                            } else {
                                comment = "" // Очищаем комментарий после успешного обслуживания
                            }
                        }
                    },
                    // Кнопка "Подтвердить" теперь активна всегда
                    // enabled = comment.isNotBlank()
                ) {
                    Text("Подтвердить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCommentDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    // Диалог для отображения информации о доверенном лице
    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            title = { Text("Информация о доверенном лице") },
            text = {
                Column {
                    OutlinedTextField(
                        value = editingTrustedFio,
                        onValueChange = { editingTrustedFio = it },
                        label = { Text("ФИО") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editingTrustedPhone,
                        onValueChange = { editingTrustedPhone = it },
                        label = { Text("Телефон") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showInfoDialog = false
                        isUpdating = true
                        // Исправлено: передаем stationId как String
                        stationViewModel.updateTrustedPerson(
                            stationId,
                            editingTrustedFio,
                            editingTrustedPhone
                        ) { errorMsg ->
                            isUpdating = false
                            if (errorMsg == null) {
                                // Обновляем локальные состояния при успешном сохранении
                                trustedPersonFio = editingTrustedFio
                                trustedPersonPhone = editingTrustedPhone
                            } else {
                                // Обработка ошибки
                            }
                        }
                    }
                ) {
                    Text("Сохранить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showInfoDialog = false }) {
                    Text("Закрыть")
                }
            }
        )
    }

    // Диалог для отображения истории комментариев
    if (showHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showHistoryDialog = false },
            title = { Text("История обслуживания") },
            text = {
                LazyColumn {
                    // Исправлено: Используем userName из комментария
                    items(station?.comments ?: emptyList()) { commentEntry ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(
                                    text = commentEntry.text,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                // Отображаем userName (ФИО), а не userId (номер телефона)
                                Text(
                                    text = "Автор: ${commentEntry.userName}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Дата: ${
                                        SimpleDateFormat(
                                            "dd.MM.yyyy HH:mm",
                                            Locale.getDefault()
                                        ).format(commentEntry.timestamp)
                                    }",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showHistoryDialog = false }) {
                    Text("Закрыть")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Информация о станции") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (station == null) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                if (errorMessage != null) {
                    Text(text = "Ошибка: $errorMessage", color = MaterialTheme.colorScheme.error)
                } else {
                    CircularProgressIndicator()
                }
            }
        } else {
            // --- ИЗМЕНЕНО: Присваиваем station значение локальной переменной ---
            val currentStation = station!!
            // --- КОНЕЦ ИЗМЕНЕНИЯ ---
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
            ) {
                item {
                    // Заголовок с номером и статусом
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Станция №${currentStation.number}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Card(
                            colors = if (currentStation.urgent) {
                                CardDefaults.cardColors(containerColor = Color.Red)
                            } else {
                                CardDefaults.outlinedCardColors()
                            }
                        ) {
                            Text(
                                text = if (currentStation.urgent) "Срочно" else "Хорошо",
                                modifier = Modifier.padding(8.dp),
                                color = if (currentStation.urgent) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Информация о доверенном лице
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.outlinedCardColors()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Доверенное лицо",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            // Используем поля из модели данных через currentStation
                            Text(text = "ФИО: $trustedPersonFio")
                            Text(text = "Телефон: $trustedPersonPhone")
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = { showInfoDialog = true }) {
                                Text("Изменить информацию")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Фото
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.outlinedCardColors()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Фото станции",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            if (currentStation.photoUrl.isNotEmpty()) {
                                val painter = rememberAsyncImagePainter(
                                    model = ImageRequest.Builder(context)
                                        .data(currentStation.photoUrl)
                                        .crossfade(true)
                                        .build()
                                )
                                Image(
                                    painter = painter,
                                    contentDescription = "Фото станции",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp)
                                        .clickable { cameraLauncher.launch(null) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AddAPhoto,
                                        contentDescription = "Добавить фото"
                                    )
                                    Text("Нет фото. Нажмите, чтобы сделать.", modifier = Modifier.padding(top = 32.dp))
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = {
                                    if (capturedImageBitmap != null) {
                                        isUpdating = true
                                        val baos = ByteArrayOutputStream()
                                        capturedImageBitmap?.compress(
                                            android.graphics.Bitmap.CompressFormat.JPEG,
                                            80,
                                            baos
                                        )
                                        val data = baos.toByteArray()

                                        // Исправлено: передаем data и stationId как String
                                        stationViewModel.uploadPhoto(data, stationId) { downloadUrl, errorMsg ->
                                            isUpdating = false
                                            if (errorMsg != null) {
                                                // Обработка ошибки загрузки фото
                                            } else {
                                                // Фото успешно загружено, обновляем URL в Firestore
                                                // Исправлено: передаем stationId как String
                                                stationViewModel.updatePhotoUrl(
                                                    stationId,
                                                    downloadUrl ?: ""
                                                ) { updateErrorMsg ->
                                                    if (updateErrorMsg != null) {
                                                        // Обработка ошибки обновления URL в Firestore
                                                    }
                                                }
                                                capturedImageBitmap = null // Сбрасываем битмап после загрузки
                                            }
                                        }
                                    } else {
                                        cameraLauncher.launch(null)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !isUpdating
                            ) {
                                if (isUpdating) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                Icon(
                                    imageVector = Icons.Default.Camera,
                                    contentDescription = null
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(if (capturedImageBitmap != null) "Загрузить фото" else "Сделать фото")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Кнопки изменения статуса "Срочно"/"Хорошо"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Кнопка "Хорошо"
                        Button(
                            onClick = {
                                isUpdating = true
                                // Исправлено: передаем stationId как String
                                stationViewModel.updateUrgentStatus(stationId, false) { errorMsg ->
                                    isUpdating = false
                                    if (errorMsg != null) {
                                        // Обработка ошибки
                                    }
                                }
                            },
                            enabled = currentStation.urgent && !isUpdating, // Активна, если сейчас "Срочно"
                            colors = if (!currentStation.urgent) {
                                ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF2E7D32)) // Темно-зеленый
                            } else {
                                ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF2E7D32),
                                    contentColor = Color.White
                                ) // Темно-зеленый
                            }
                        ) {
                            if (isUpdating && !currentStation.urgent) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text("Хорошо")
                        }

                        // Кнопка "Срочно"
                        OutlinedButton(
                            onClick = {
                                isUpdating = true
                                // Исправлено: передаем stationId как String
                                stationViewModel.updateUrgentStatus(stationId, true) { errorMsg ->
                                    isUpdating = false
                                    if (errorMsg != null) {
                                        // Обработка ошибки
                                    }
                                }
                            },
                            enabled = !currentStation.urgent && !isUpdating // Активна, если сейчас "Хорошо"
                        ) {
                            if (isUpdating && currentStation.urgent) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text("Срочно")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Кнопка "Обслужить"
                    Button(
                        onClick = {
                            // Открываем диалог для ввода комментария
                            showCommentDialog = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        enabled = !isUpdating // && userCanEdit // Убираем проверку userCanEdit, так как все admin
                    ) {
                        if (isUpdating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Icon(imageVector = Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Обслужить")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Кнопка "Подробности" (история комментариев)
                    OutlinedButton(
                        onClick = { showHistoryDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.History, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("История обслуживания")
                    }
                }
            }
        }
    }
}