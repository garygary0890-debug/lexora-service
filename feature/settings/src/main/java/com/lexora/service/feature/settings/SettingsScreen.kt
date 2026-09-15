package com.lexora.service.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lexora.service.core.model.LexoraModuleId
import com.lexora.service.core.model.ModuleDescriptor
import com.lexora.service.core.model.Organization
import com.lexora.service.core.model.ServiceUser

@Composable
fun SettingsScreen(
    organization: Organization,
    user: ServiceUser,
    modules: List<ModuleDescriptor>,
    appVersion: String,
    onModuleEnabledChange: (LexoraModuleId, Boolean) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Настройки Lexora Service")
        Text("Версия: $appVersion")
        Text("Организация: ${organization.name}")
        Text("Пользователь: ${user.displayName}")
        Text("Роли: ${user.roles.joinToString()}")

        Text("Модули")
        modules.forEach { module ->
            Card(Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
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
                            onModuleEnabledChange(module.id, enabled)
                        },
                    )
                }
            }
        }
    }
}
