package com.lexora.service.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import com.lexora.service.core.database.LexoraServiceDatabase
import com.lexora.service.core.model.LexoraModuleId
import com.lexora.service.core.model.ModuleDescriptor
import com.lexora.service.core.model.ModuleLicenseStatus
import com.lexora.service.core.model.ModuleStoreItem
import com.lexora.service.core.model.Organization
import com.lexora.service.core.model.ServiceUser
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    organization: Organization,
    user: ServiceUser,
    modules: List<ModuleDescriptor>,
    appVersion: String = "0.22.0",
    onModuleEnabledChange: (LexoraModuleId, Boolean) -> Unit,
) {
    val context = LocalContext.current
    val repository = remember {
        ModuleLicenseRepository(LexoraServiceDatabase.create(context.applicationContext).serviceDao())
    }
    val scope = rememberCoroutineScope()
    var persistedModules by remember { mutableStateOf(modules) }
    var storeItems by remember { mutableStateOf<List<ModuleStoreItem>>(emptyList()) }

    suspend fun reload() {
        persistedModules = repository.descriptors(organization.id)
        storeItems = repository.storeCatalog()
    }

    LaunchedEffect(organization.id) { reload() }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Настройки Lexora Service")
        Text("Версия: $appVersion")
        Text("Организация: ${organization.name}")
        Text("Пользователь: ${user.displayName}")
        Text("Роли: ${user.roles.joinToString()}")

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
                                    repository.setEnabled(organization.id, module.id, enabled)
                                    reload()
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
                                        repository.applyLicenseStatus(organization.id, module.id, ModuleLicenseStatus.ACTIVE)
                                        reload()
                                    }
                                }) { Text("Активировать локально") }
                            } else {
                                Button(onClick = {
                                    scope.launch {
                                        repository.applyLicenseStatus(organization.id, module.id, ModuleLicenseStatus.SUSPENDED)
                                        reload()
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
