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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.lexora.service.core.data.PersistentOrganizationRepository
import com.lexora.service.core.data.WashRepository
import com.lexora.service.core.database.LexoraServiceDatabase
import com.lexora.service.core.model.Organization
import com.lexora.service.core.model.WashChemicalUsage
import com.lexora.service.core.model.WashPost
import com.lexora.service.core.model.WashPostStatus
import com.lexora.service.core.model.WashQueueItem
import com.lexora.service.core.model.WashQueueStatus
import com.lexora.service.core.model.WashTechCard
import kotlinx.coroutines.launch

@Composable
fun WashScreen(organization: Organization? = null) {
    val context = LocalContext.current
    val database = remember { LexoraServiceDatabase.create(context.applicationContext) }
    val repository = remember { WashRepository.create(context) }
    val organizationRepository = remember(database) { PersistentOrganizationRepository(database.serviceDao()) }
    val scope = rememberCoroutineScope()
    var resolvedOrganization by remember(organization?.id) { mutableStateOf(organization) }

    var posts by remember { mutableStateOf<List<WashPost>>(emptyList()) }
    var queue by remember { mutableStateOf<List<WashQueueItem>>(emptyList()) }
    var techCards by remember { mutableStateOf<List<WashTechCard>>(emptyList()) }
    var chemicalUsage by remember { mutableStateOf<List<WashChemicalUsage>>(emptyList()) }

    LaunchedEffect(organization?.id) {
        resolvedOrganization = organization ?: organizationRepository.ensureBootstrapOrganization()
    }

    val activeOrganization = resolvedOrganization ?: return

    suspend fun reload() {
        posts = repository.posts(activeOrganization.id)
        queue = repository.queue(activeOrganization.id)
        techCards = repository.techCards(activeOrganization.id)
        chemicalUsage = repository.chemicalUsage(activeOrganization.id)
    }

    LaunchedEffect(activeOrganization.id) { reload() }

    WashContent(
        posts = posts,
        queue = queue,
        techCards = techCards,
        chemicalUsage = chemicalUsage,
        onAddPost = { scope.launch { repository.addPost(activeOrganization.id, null, "Пост ${posts.size + 1}"); reload() } },
        onTogglePostStatus = { id -> scope.launch { posts.firstOrNull { it.id == id }?.let { repository.togglePostStatus(it); reload() } } },
        onAddQueueItem = { scope.launch { repository.addQueueItem(activeOrganization.id); reload() } },
        onAdvanceQueueItem = { id -> scope.launch { queue.firstOrNull { it.id == id }?.let { repository.advanceQueueItem(activeOrganization.id, it); reload() } } },
        onAddTechCard = { scope.launch { repository.addTechCard(activeOrganization.id); reload() } },
        onAddChemicalUsage = { scope.launch { repository.addChemicalUsage(activeOrganization.id); reload() } },
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
