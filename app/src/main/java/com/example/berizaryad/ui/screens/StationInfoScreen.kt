// Berizaryad/app/src/main/java/com/example/berizaryad/ui/screens/StationInfoScreen.kt
package com.example.berizaryad.ui.screens

import android.app.Activity
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.berizaryad.data.StationData
import com.example.berizaryad.viewmodel.AuthViewModel
import com.example.berizaryad.viewmodel.StationViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StationInfoScreen(
    stationId: Long, // Принимаем ID как Long
    onBack: () -> Unit,
    stationViewModel: StationViewModel,
    authViewModel: AuthViewModel
) {
    val context = LocalContext.current as Activity
    var station by remember { mutableStateOf<StationData?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Состояния для редактирования ответственного лица
    var showInfoDialog by remember { mutableStateOf(false) }
    var editingTrustedFio by remember { mutableStateOf("") }
    var editingTrustedPhone by remember { mutableStateOf("") }

    // Состояния для комментариев
    var showCommentDialog by remember { mutableStateOf(false) }
    var commentText by remember { mutableStateOf("") }

    // Состояние для отображения диалога разрешений
    var showPermissionDialog by remember { mutableStateOf(false) }

    // Состояние для отслеживания процесса обновления
    var isUpdating by remember { mutableStateOf(false) }

    // Загрузка данных станции при запуске
    LaunchedEffect(stationId) {
        stationViewModel.loadStation(stationId) { loadedStation, error ->
            isLoading = false
            if (error != null) {
                errorMessage = error
                Log.e("StationInfoScreen", "Error loading station: $error")
            } else {
                station = loadedStation
                // Инициализируем состояния редактирования
                editingTrustedFio = loadedStation?.responsibleName ?: ""
                editingTrustedPhone = loadedStation?.responsiblePhone ?: ""
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Станция №${station?.number ?: stationId}") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (errorMessage != null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Ошибка: $errorMessage",
                    color = MaterialTheme.colorScheme.error
                )
            }
        } else if (station != null) {
            val currentStation = station!!
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                // Заголовок с номером и приоритетом
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Станция №${currentStation.number}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    if (currentStation.urgent) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = "Срочно",
                            tint = Color.Red
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                // Адрес
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Адрес:",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = currentStation.address ?: "Не указан")
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                // Ответственное лицо
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Ответственное лицо:",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = currentStation.responsibleName ?: "Не указано")
                                Text(text = currentStation.responsiblePhone ?: "Не указан")
                            }
                            IconButton(onClick = { showInfoDialog = true }) {
                                Icon(Icons.Default.Edit, contentDescription = "Редактировать")
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                // Статусы
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedButton(
                        onClick = {
                            isUpdating = true
                            stationViewModel.updateUrgentStatus(stationId, false) { errorMsg ->
                                isUpdating = false
                                if (errorMsg != null) {
                                    Toast.makeText(context, "Ошибка: $errorMsg", Toast.LENGTH_SHORT)
                                        .show()
                                } else {
                                    Toast.makeText(
                                        context,
                                        "Статус изменен на 'Хорошо'",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    // Перезагрузить данные станции
                                    stationViewModel.loadStation(stationId) { updatedStation, _ ->
                                        station = updatedStation
                                    }
                                }
                            }
                        },
                        enabled = currentStation.urgent && !isUpdating,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        if (isUpdating && !currentStation.urgent) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text("Хорошо")
                    }
                    OutlinedButton(
                        onClick = {
                            isUpdating = true
                            stationViewModel.updateUrgentStatus(stationId, true) { errorMsg ->
                                isUpdating = false
                                if (errorMsg != null) {
                                    Toast.makeText(context, "Ошибка: $errorMsg", Toast.LENGTH_SHORT)
                                        .show()
                                } else {
                                    Toast.makeText(
                                        context,
                                        "Статус изменен на 'Срочно'",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    // Перезагрузить данные станции
                                    stationViewModel.loadStation(stationId) { updatedStation, _ ->
                                        station = updatedStation
                                    }
                                }
                            }
                        },
                        enabled = !currentStation.urgent && !isUpdating,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
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

                // Информация о последнем обслуживании
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Последнее обслуживание:",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        if (currentStation.serviced) {
                            Text(text = "Статус: Обслужена")
                            Text(
                                text = "Дата: ${
                                    SimpleDateFormat(
                                        "dd.MM.yyyy HH:mm",
                                        Locale.getDefault()
                                    ).format(currentStation.servicedDate ?: Date())
                                }"
                            )
                            Text(text = "Кем: ${currentStation.servicedByName ?: "Неизвестно"}")
                            if (!currentStation.lastComment.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Комментарий: ${currentStation.lastComment}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        } else {
                            Text(text = "Статус: Не обслужена")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                // Кнопка "Обслужить"
                Button(
                    onClick = { showCommentDialog = true },
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
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Обслужить")
                }
            }
        }
    }

    // Диалог для редактирования ответственного лица
    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            title = { Text("Редактировать") },
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
                        stationViewModel.updateTrustedPerson(
                            stationId,
                            editingTrustedFio,
                            editingTrustedPhone
                        ) { errorMsg ->
                            isUpdating = false
                            if (errorMsg != null) {
                                Toast.makeText(context, "Ошибка: $errorMsg", Toast.LENGTH_SHORT)
                                    .show()
                            } else {
                                Toast.makeText(
                                    context,
                                    "Информация обновлена",
                                    Toast.LENGTH_SHORT
                                ).show()
                                station = station?.copy(
                                    responsibleName = editingTrustedFio,
                                    responsiblePhone = editingTrustedPhone
                                )
                            }
                        }
                    }
                ) {
                    Text("Сохранить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showInfoDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    // Диалог для добавления комментария
    if (showCommentDialog) {
        AlertDialog(
            onDismissRequest = { showCommentDialog = false },
            title = { Text("Обслужить станцию") },
            text = {
                Column {
                    Text("Добавьте комментарий (не обязательно):")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = commentText,
                        onValueChange = { commentText = it },
                        label = { Text("Комментарий") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCommentDialog = false
                        isUpdating = true
                        stationViewModel.markAsServiced(stationId, commentText) { errorMsg ->
                            isUpdating = false
                            if (errorMsg != null) {
                                Toast.makeText(context, "Ошибка: $errorMsg", Toast.LENGTH_SHORT)
                                    .show()
                            } else {
                                Toast.makeText(
                                    context,
                                    "Станция обслужена",
                                    Toast.LENGTH_SHORT
                                ).show()
                                commentText = "" // Очистить поле комментария
                                // Перезагрузить данные станции
                                stationViewModel.loadStation(stationId) { updatedStation, _ ->
                                    station = updatedStation
                                    // Обновляем состояния редактирования после перезагрузки
                                    if (updatedStation != null) {
                                        editingTrustedFio = updatedStation.responsibleName ?: ""
                                        editingTrustedPhone = updatedStation.responsiblePhone ?: ""
                                    }
                                }
                            }
                        }
                    }
                ) {
                    Text("Обслужить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCommentDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}