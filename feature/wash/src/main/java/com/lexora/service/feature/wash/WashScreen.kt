package com.lexora.service.feature.wash

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lexora.service.core.model.WashChemicalUsage
import com.lexora.service.core.model.WashPost
import com.lexora.service.core.model.WashPostStatus
import com.lexora.service.core.model.WashQueueItem
import com.lexora.service.core.model.WashQueueStatus
import com.lexora.service.core.model.WashTechCard

@Composable
fun WashScreen(
    state: WashUiState,
    onAddPost: () -> Unit,
    onTogglePostStatus: (String) -> Unit,
    onAddQueueItem: () -> Unit,
    onAdvanceQueueItem: (String) -> Unit,
    onAddTechCard: () -> Unit,
    onAddChemicalUsage: () -> Unit,
) {
    WashContent(
        posts = state.posts,
        queue = state.queue,
        techCards = state.techCards,
        chemicalUsage = state.chemicalUsage,
        onAddPost = onAddPost,
        onTogglePostStatus = onTogglePostStatus,
        onAddQueueItem = onAddQueueItem,
        onAdvanceQueueItem = onAdvanceQueueItem,
        onAddTechCard = onAddTechCard,
        onAddChemicalUsage = onAddChemicalUsage,
    )
}
@Composable
private fun WashContent(
    posts: List<WashPost>,
    queue: List<WashQueueItem>,
    techCards: List<WashTechCard>,
    chemicalUsage: List<WashChemicalUsage>,
    onAddPost: () -> Unit,
    onTogglePostStatus: (String) -> Unit,
    onAddQueueItem: () -> Unit,
    onAdvanceQueueItem: (String) -> Unit,
    onAddTechCard: () -> Unit,
    onAddChemicalUsage: () -> Unit,
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Автомойка", style = MaterialTheme.typography.headlineMedium)
        Text("Посты", style = MaterialTheme.typography.titleMedium)
        Button(onClick = onAddPost) { Text("Добавить пост") }
        posts.forEach { post -> Card(Modifier.fillMaxWidth()) { Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) { Column { Text(post.name, style = MaterialTheme.typography.titleSmall); Text("Статус: ${post.status.name}") }; OutlinedButton(onClick = { onTogglePostStatus(post.id) }) { Text(if (post.status == WashPostStatus.AVAILABLE) "Занять" else "Освободить") } } } }
        Text("Очередь", style = MaterialTheme.typography.titleMedium)
        Button(onClick = onAddQueueItem) { Text("Добавить в очередь") }
        queue.sortedBy { it.position }.forEach { item -> Card(Modifier.fillMaxWidth()) { Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) { Column { Text("№${item.position}"); item.requestId?.let { Text("Заявка: $it", style = MaterialTheme.typography.bodySmall) }; item.vehicleId?.let { Text("Автомобиль: $it", style = MaterialTheme.typography.bodySmall) }; item.postId?.let { Text("Пост: $it", style = MaterialTheme.typography.bodySmall) }; Text("Статус: ${item.status.name}") }; if (item.status != WashQueueStatus.COMPLETED && item.status != WashQueueStatus.CANCELLED) OutlinedButton(onClick = { onAdvanceQueueItem(item.id) }) { Text("Далее") } } } }
        Text("Технологические карты", style = MaterialTheme.typography.titleMedium)
        Button(onClick = onAddTechCard) { Text("Добавить техкарту") }
        techCards.forEach { card -> Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) { Text(card.name, style = MaterialTheme.typography.titleSmall); Text("Норматив: ${card.durationMinutes} мин"); Text(card.stepsText) } } }
        Text("Расход химии", style = MaterialTheme.typography.titleMedium)
        Button(onClick = onAddChemicalUsage) { Text("Зафиксировать расход") }
        chemicalUsage.take(20).forEach { usage -> Text("${usage.chemicalName}: ${usage.quantityMl} мл") }
    }
}
