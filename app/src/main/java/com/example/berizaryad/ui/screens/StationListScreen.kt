// Berizaryad/app/src/main/java/com/example/berizaryad/ui/screens/StationListScreen.kt
package com.example.berizaryad.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.berizaryad.data.StationData
import com.example.berizaryad.viewmodel.StationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StationListScreen(
    // Изменено: принимаем Long
    onStationClick: (Long) -> Unit,
    onBack: () -> Unit,
    stationViewModel: StationViewModel
) {
    val stations by stationViewModel.stations.collectAsState()
    val errorMessage by stationViewModel.errorMessage.collectAsState()
    var isLoading by remember { mutableStateOf(true) }

    // Загружаем список при открытии экрана
    LaunchedEffect(Unit) {
        isLoading = true
        stationViewModel.loadAllStations { success ->
            isLoading = false
            // Обработка ошибок загрузки может быть здесь или в ViewModel
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Список станций") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
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
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
            ) {
                items(stations) { station ->
                    // Передаем station.id как Long
                    StationItem(station = station, onItemClick = { onStationClick(station.id) })
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun StationItem(station: StationData, onItemClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onItemClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Станция №${station.number}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                // Индикатор статуса обслуживания
                Icon(
                    imageVector = if (station.serviced) Icons.Default.Check else Icons.Default.Close,
                    contentDescription = if (station.serviced) "Обслужена" else "Не обслужена",
                    tint = if (station.serviced) Color.Green else MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(text = "Адрес: ${station.address}")

            Spacer(modifier = Modifier.height(8.dp))

            // Отображение статуса "Срочно"/"Хорошо"
            val statusText = if (station.urgent) "Приоритет: Срочно" else "Приоритет: Хорошо"
            val statusColor = if (station.urgent) Color.Red else Color(0xFF2E7D32) // Темно-зеленый
            Text(
                text = statusText,
                color = statusColor
            )

            // Иконка "вперед" в конце элемента
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = "Перейти",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}