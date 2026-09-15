package com.lexora.service.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lexora.service.core.model.LexoraModuleId
import com.lexora.service.core.model.ModuleDescriptor
import com.lexora.service.core.model.Organization

@Composable
fun HomeScreen(
    organization: Organization,
    modules: List<ModuleDescriptor>,
    onOpenClients: () -> Unit,
    onOpenVehicles: () -> Unit,
    onOpenAssets: () -> Unit,
    onOpenOrganization: () -> Unit,
    onOpenRequests: () -> Unit,
    onOpenFieldWork: () -> Unit,
    onOpenDocuments: () -> Unit = {},
    onOpenReports: () -> Unit = {},
    onOpenCatalog: () -> Unit = {},
    onOpenNotifications: () -> Unit = {},
    onOpenWash: () -> Unit,
    onOpenTires: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val washAvailable = modules.any { it.id == LexoraModuleId.WASH && it.enabled && it.licensed }
    val tiresAvailable = modules.any { it.id == LexoraModuleId.TIRES && it.enabled && it.licensed }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Lexora Service", style = MaterialTheme.typography.headlineMedium)
        Text("Рабочий стол", style = MaterialTheme.typography.titleMedium)
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Активная организация", style = MaterialTheme.typography.labelMedium)
                Text(organization.name, style = MaterialTheme.typography.titleMedium)
            }
        }
        Text("Подключенные модули", style = MaterialTheme.typography.titleMedium)
        modules.filter { it.enabled && it.licensed }.forEach { module -> Text("• ${module.title}") }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onOpenClients) { Text("Клиенты") }
            Button(onClick = onOpenVehicles) { Text("Автомобили") }
        }
        Button(onClick = onOpenRequests) { Text("Обращения и заявки") }
        Button(onClick = onOpenFieldWork) { Text("Выезды и работы") }
        Button(onClick = onOpenDocuments) { Text("Документы и платежи") }
        Button(onClick = onOpenCatalog) { Text("Услуги и прайс-листы") }
        Button(onClick = onOpenNotifications) { Text("Уведомления") }
        Button(onClick = onOpenReports) { Text("Отчёты и интеграции") }
        Button(onClick = onOpenAssets) { Text("Объекты и оборудование") }
        Button(onClick = onOpenOrganization) { Text("Филиалы и сотрудники") }
        if (washAvailable || tiresAvailable) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (washAvailable) Button(onClick = onOpenWash) { Text("Автомойка") }
                if (tiresAvailable) Button(onClick = onOpenTires) { Text("Шиномонтаж") }
            }
        }
        OutlinedButton(onClick = onOpenSettings) { Text("Настройки") }
    }
}
