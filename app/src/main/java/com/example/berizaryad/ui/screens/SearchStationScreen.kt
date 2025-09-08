// Berizaryad/app/src/main/java/com/example/berizaryad/ui/screens/SearchStationScreen.kt
package com.example.berizaryad.ui.screens

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.berizaryad.data.StationData
import com.example.berizaryad.viewmodel.StationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchStationScreen(
    onStationSelected: (Long) -> Unit,
    stationViewModel: StationViewModel,
    onNavigateToProfile: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var stations by remember { mutableStateOf<List<StationData>>(emptyList()) }
    var filteredStations by remember { mutableStateOf<List<StationData>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Фильтры
    var showOnlyUrgent by remember { mutableStateOf(false) }
    var showOnlyServiced by remember { mutableStateOf(false) }
    var showOnlyUnserviced by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        Log.d("SearchStationScreen", "Loading all stations...")
        stationViewModel.loadStations { loadedStations, error ->
            isLoading = false // Лучше установить в false после получения результата
            if (error != null) {
                errorMessage = error
                Log.e("SearchStationScreen", "Error loading stations: $error")
            } else if (loadedStations != null && loadedStations.isNotEmpty()) {
                stations = loadedStations
                filteredStations = loadedStations
                Log.d("SearchStationScreen", "Loaded ${loadedStations.size} stations")
            } else {
                // Случай, когда loadedStations == null или пустой список, но ошибки нет (редкий, но возможный)
                errorMessage = "Список станций пуст"
                Log.w("SearchStationScreen", "No stations loaded or empty list received")
            }
        }
    }

    // Функция для применения фильтров и поиска
    fun applyFiltersAndSearch() {
        var result = stations

        // Применяем фильтры
        if (showOnlyUrgent) {
            result = result.filter { it.urgent }
        }
        if (showOnlyServiced) {
            result = result.filter { it.serviced }
        }
        if (showOnlyUnserviced) {
            result = result.filter { !it.serviced }
        }

        // Применяем поиск
        if (searchQuery.isNotBlank()) {
            val query = searchQuery.lowercase()
            result = result.filter {
                // Используем elvis operator ?: для обработки null
                it.id.toString().contains(query) ||
                        it.number?.lowercase()?.contains(query) == true || // Safe call + elvis
                        it.address?.lowercase()?.contains(query) == true ||
                        it.organization?.lowercase()?.contains(query) == true ||
                        it.responsibleName?.lowercase()?.contains(query) == true ||
                        it.responsiblePhone?.lowercase()?.contains(query) == true
            }
        }

        filteredStations = result
    }

    // Пересчитываем фильтры при изменении состояния
    LaunchedEffect(searchQuery, showOnlyUrgent, showOnlyServiced, showOnlyUnserviced, stations) {
        applyFiltersAndSearch()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Поиск станции") },
                actions = {
                    IconButton(onClick = onNavigateToProfile) {
                        Icon(Icons.Default.Person, contentDescription = "Профиль")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (errorMessage != null) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text(text = "Ошибка: $errorMessage", color = MaterialTheme.colorScheme.error)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                // Добавляем фильтры в начало списка
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = CardDefaults.outlinedCardColors()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = "Фильтры", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(8.dp))

                            // Поле поиска
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                label = { Text("Поиск по ID, номеру, адресу...") },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            // Чекбоксы фильтров
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                FilterChip(
                                    selected = showOnlyUrgent,
                                    onClick = { showOnlyUrgent = !showOnlyUrgent },
                                    label = { Text("Срочно") }
                                )
                                FilterChip(
                                    selected = showOnlyServiced,
                                    onClick = { showOnlyServiced = !showOnlyServiced },
                                    label = { Text("Обслужено") }
                                )
                                FilterChip(
                                    selected = showOnlyUnserviced,
                                    onClick = { showOnlyUnserviced = !showOnlyUnserviced },
                                    label = { Text("Не обслужено") }
                                )
                            }
                        }
                    }
                }

                // Отображаем отфильтрованный список станций
                items(filteredStations) { station ->
                    StationItem(
                        station = station,
                        onClick = { onStationSelected(station.id) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Если ничего не найдено
                if (filteredStations.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Станции не найдены")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StationItem(station: StationData, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.outlinedCardColors()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Используем elvis operator ?: для отображения номера или placeholder
                Text(
                    text = "Станция №${station.number ?: "Без номера"}",
                    style = MaterialTheme.typography.titleMedium
                )
                if (station.urgent) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text(
                            text = "Срочно",
                            modifier = Modifier.padding(4.dp),
                            color = MaterialTheme.colorScheme.onError
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            // Используем elvis operator ?: для отображения адреса или placeholder
            Text(text = station.address ?: "Адрес не указан")
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "Организация: ${station.organization ?: "Не указана"}")
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "Ответственный: ${station.responsibleName ?: "Не назначен"}")
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "Телефон: ${station.responsiblePhone ?: "Не указан"}")
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (station.serviced) "Обслужена" else "Не обслужена",
                    color = if (station.serviced) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
                // Можно добавить отображение даты обслуживания
                station.servicedDate?.let {
                    Text(text = android.text.format.DateFormat.format("dd.MM.yyyy", it).toString())
                }
            }
        }
    }
}