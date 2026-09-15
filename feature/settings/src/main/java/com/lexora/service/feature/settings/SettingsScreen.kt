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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.lexora.service.core.data.ModuleLicenseRepository
import com.lexora.service.core.data.ReferenceDataRepository
import com.lexora.service.core.database.LexoraServiceDatabase
import com.lexora.service.core.model.LexoraModuleId
import com.lexora.service.core.model.ModuleDescriptor
import com.lexora.service.core.model.ModuleLicenseStatus
import com.lexora.service.core.model.ModuleStoreItem
import com.lexora.service.core.model.Organization
import com.lexora.service.core.model.ReferenceDirectory
import com.lexora.service.core.model.ReferenceDirectoryItem
import com.lexora.service.core.model.ServiceUser
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    organization: Organization,
    user: ServiceUser,
    modules: List<ModuleDescriptor>,
    appVersion: String = "0.24.0",
    onModuleEnabledChange: (LexoraModuleId, Boolean) -> Unit,
) {
    val context = LocalContext.current
    val moduleRepository = remember {
        ModuleLicenseRepository(LexoraServiceDatabase.create(context.applicationContext).serviceDao())
    }
    val referenceRepository = remember { ReferenceDataRepository.create(context) }
    val scope = rememberCoroutineScope()
    var persistedModules by remember { mutableStateOf(modules) }
    var storeItems by remember { mutableStateOf<List<ModuleStoreItem>>(emptyList()) }
    var directories by remember { mutableStateOf<List<ReferenceDirectory>>(emptyList()) }
    var selectedDirectoryId by remember { mutableStateOf<String?>(null) }
    var directoryItems by remember { mutableStateOf<List<ReferenceDirectoryItem>>(emptyList()) }
    var directoryCode by remember { mutableStateOf("") }
    var directoryName by remember { mutableStateOf("") }
    var itemCode by remember { mutableStateOf("") }
    var itemName by remember { mutableStateOf("") }
    var referenceMessage by remember { mutableStateOf<String?>(null) }

    suspend fun reloadModules() {
        persistedModules = moduleRepository.descriptors(organization.id)
        storeItems = moduleRepository.storeCatalog()
    }

    suspend fun reloadDirectories(preferredId: String? = selectedDirectoryId) {
        directories = referenceRepository.directories(organization.id)
        selectedDirectoryId = preferredId?.takeIf { id -> directories.any { it.id == id } }
            ?: directories.firstOrNull()?.id
        directoryItems = selectedDirectoryId?.let { referenceRepository.items(it) }.orEmpty()
    }

    LaunchedEffect(organization.id) {
        reloadModules()
        reloadDirectories()
    }

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
                scope.launch {
                    runCatching {
                        referenceRepository.createDirectory(organization.id, user.id, directoryCode, directoryName)
                    }.onSuccess { created ->
                        directoryCode = ""
                        directoryName = ""
                        referenceMessage = "Справочник создан."
                        reloadDirectories(created.id)
                    }.onFailure {
                        referenceMessage = "Не удалось создать справочник: проверьте уникальность кода."
                    }
                }
            },
        ) { Text("Добавить справочник") }

        if (directories.isEmpty()) {
            Text("Справочники ещё не созданы.")
        } else {
            directories.forEach { directory ->
                FilterChip(
                    selected = directory.id == selectedDirectoryId,
                    onClick = {
                        scope.launch {
                            selectedDirectoryId = directory.id
                            directoryItems = referenceRepository.items(directory.id)
                        }
                    },
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
                                scope.launch {
                                    runCatching {
                                        referenceRepository.createItem(
                                            organization.id,
                                            user.id,
                                            directory.id,
                                            itemCode,
                                            itemName,
                                        )
                                    }.onSuccess {
                                        itemCode = ""
                                        itemName = ""
                                        referenceMessage = "Значение добавлено."
                                        reloadDirectories(directory.id)
                                    }.onFailure {
                                        referenceMessage = "Не удалось добавить значение: проверьте уникальность кода."
                                    }
                                }
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
                                    onCheckedChange = { active ->
                                        scope.launch {
                                            referenceRepository.setItemActive(organization.id, user.id, item.id, active)
                                            reloadDirectories(directory.id)
                                        }
                                    },
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
                                onCheckedChange = { active ->
                                    scope.launch {
                                        referenceRepository.setDirectoryActive(organization.id, user.id, directory.id, active)
                                        reloadDirectories(directory.id)
                                    }
                                },
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
                                scope.launch {
                                    moduleRepository.setEnabled(organization.id, module.id, enabled)
                                    reloadModules()
                                    onModuleEnabledChange(module.id, enabled)
                                }
                            },
                        )
                    }
                    if (module.id != LexoraModuleId.CORE) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (module.licenseStatus != ModuleLicenseStatus.ACTIVE) {
                                Button(onClick = {
                                    scope.launch {
                                        moduleRepository.applyLicenseStatus(organization.id, module.id, ModuleLicenseStatus.ACTIVE)
                                        reloadModules()
                                    }
                                }) { Text("Активировать локально") }
                            } else {
                                Button(onClick = {
                                    scope.launch {
                                        moduleRepository.applyLicenseStatus(organization.id, module.id, ModuleLicenseStatus.SUSPENDED)
                                        reloadModules()
                                        onModuleEnabledChange(module.id, false)
                                    }
                                }) { Text("Приостановить") }
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
