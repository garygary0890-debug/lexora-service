package com.lexora.service.feature.settings

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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lexora.service.core.model.LexoraModuleId
import com.lexora.service.core.model.ModuleDescriptor
import com.lexora.service.core.model.ModuleLicenseStatus
import com.lexora.service.core.model.ModuleStoreItem
import com.lexora.service.core.model.Organization
import com.lexora.service.core.model.ReferenceDirectory
import com.lexora.service.core.model.ReferenceDirectoryItem
import com.lexora.service.core.model.ServiceUser

@Composable
fun SettingsScreen(
    organization: Organization,
    user: ServiceUser,
    viewModel: SettingsViewModel,
    appVersion: String = "0.25.0",
    onModuleEnabledChange: (LexoraModuleId, Boolean) -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val data = state.data
    val persistedModules = data?.modules.orEmpty()
    val storeItems = data?.storeItems.orEmpty()
    val directories = data?.directories.orEmpty()
    val selectedDirectoryId = data?.selectedDirectoryId
    val directoryItems = data?.directoryItems.orEmpty()
    val referenceMessage = state.message
    var directoryCode by remember { mutableStateOf("") }
    var directoryName by remember { mutableStateOf("") }
    var itemCode by remember { mutableStateOf("") }
    var itemName by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Настройки Lexora Service")
        Text("Версия: $appVersion")
        Text("Организация: ${organization.name}")
        Text("Пользователь: ${user.displayName}")
        Text("Роли: ${user.roles.joinToString()}")

        Text("Настраиваемые справочники")
        Text("Справочники принадлежат активной организации. Системные workflow-статусы заявок и документов этим разделом не заменяются.")
        OutlinedTextField(
            value = directoryCode,
            onValueChange = { directoryCode = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Код справочника") },
            singleLine = true,
        )
        OutlinedTextField(
            value = directoryName,
            onValueChange = { directoryName = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Название справочника") },
            singleLine = true,
        )
        Button(
            enabled = directoryCode.isNotBlank() && directoryName.isNotBlank(),
            onClick = {
                viewModel.createDirectory(directoryCode, directoryName)
                directoryCode = ""
                directoryName = ""
            },
        ) { Text("Добавить справочник") }

        if (directories.isEmpty()) {
            Text("Справочники ещё не созданы.")
        } else {
            directories.forEach { directory ->
                FilterChip(
                    selected = directory.id == selectedDirectoryId,
                    onClick = { viewModel.selectDirectory(directory.id) },
                    label = { Text("${directory.code} · ${directory.name}${if (!directory.active) " · неактивен" else ""}") },
                )
            }

            val selectedDirectory = directories.firstOrNull { it.id == selectedDirectoryId }
            selectedDirectory?.let { directory ->
                Card(Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("Элементы: ${directory.name}")
                        OutlinedTextField(
                            value = itemCode,
                            onValueChange = { itemCode = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Код значения") },
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = itemName,
                            onValueChange = { itemName = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Название значения") },
                            singleLine = true,
                        )
                        Button(
                            enabled = directory.active && itemCode.isNotBlank() && itemName.isNotBlank(),
                            onClick = {
                                viewModel.createItem(directory.id, itemCode, itemName)
                                itemCode = ""
                                itemName = ""
                            },
                        ) { Text("Добавить значение") }

                        directoryItems.forEach { item ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("${item.code} · ${item.name}", modifier = Modifier.weight(1f))
                                Switch(
                                    checked = item.active,
                                    onCheckedChange = { active -> viewModel.setItemActive(directory.id, item.id, active) },
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("Справочник активен", modifier = Modifier.weight(1f))
                            Switch(
                                checked = directory.active,
                                enabled = !directory.system,
                                onCheckedChange = { active -> viewModel.setDirectoryActive(directory.id, active) },
                            )
                        }
                    }
                }
            }
        }
        referenceMessage?.let { Text(it) }

        Text("Лицензии модулей")
        persistedModules.forEach { module ->
            Card(Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(module.title)
                            Text("Лицензия: ${module.licenseStatus}")
                        }
                        Switch(
                            checked = module.enabled,
                            enabled = module.id != LexoraModuleId.CORE && module.licensed,
                            onCheckedChange = { enabled ->
                                viewModel.setModuleEnabled(module.id, enabled) { onModuleEnabledChange(module.id, enabled) }
                            },
                        )
                    }
                    if (module.id != LexoraModuleId.CORE) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (module.licenseStatus != ModuleLicenseStatus.ACTIVE) {
                                Button(onClick = { viewModel.setLicense(module.id, ModuleLicenseStatus.ACTIVE) }) { Text("Активировать локально") }
                            } else {
                                Button(onClick = { viewModel.setLicense(module.id, ModuleLicenseStatus.SUSPENDED) { onModuleEnabledChange(module.id, false) } }) { Text("Приостановить") }
                            }
                        }
                    }
                }
            }
        }

        Text("Магазин модулей")
        storeItems.forEach { item ->
            Card(Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(item.title)
                    Text(item.description)
                    Text(
                        when {
                            item.includedInCore -> "Входит в базовое ядро"
                            item.availableForLicensing -> "Доступен для лицензирования"
                            else -> "Недоступен для лицензирования"
                        },
                    )
                }
            }
        }

        Text("Платёжный контур магазина пока не подключён; текущие действия предназначены для локальной настройки лицензий на этапе разработки.")
    }
}
