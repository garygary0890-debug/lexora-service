package com.lexora.service.feature.users

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lexora.service.core.model.Organization
import com.lexora.service.core.model.ServiceUser
import com.lexora.service.core.model.UserRole

@Composable
fun UsersScreen(
    organization: Organization,
    currentUser: ServiceUser,
    users: List<ServiceUser>,
    canManageUsers: Boolean,
    onCreateUser: (String, UserRole) -> Unit,
    onSetRole: (String, UserRole, Boolean) -> Unit,
    onSetUserActive: (String, Boolean) -> Unit = { _, _ -> },
) {
    var displayName by remember { mutableStateOf("") }
    var initialRole by remember { mutableStateOf(UserRole.MANAGER) }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Пользователи и роли")
        Text("Организация: ${organization.name}")

        if (!canManageUsers) {
            Text("Недостаточно прав: требуется разрешение MANAGE_USERS.")
            return@Column
        }

        Text("Новый локальный пользователь")
        Text("На текущем этапе пользователь создаётся без пароля и серверной учётной записи. Аутентификация будет подключена отдельно.")
        OutlinedTextField(
            value = displayName,
            onValueChange = { displayName = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Имя пользователя") },
            singleLine = true,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            UserRole.entries.forEach { role ->
                FilterChip(
                    selected = initialRole == role,
                    onClick = { initialRole = role },
                    label = { Text(role.name) },
                )
            }
        }
        Button(
            enabled = displayName.isNotBlank(),
            onClick = {
                onCreateUser(displayName.trim(), initialRole)
                displayName = ""
            },
        ) { Text("Создать пользователя") }

        Text("Пользователи организации")
        if (users.isEmpty()) {
            Text("Пользователи не найдены.")
        }
        users.forEach { managedUser ->
            Card(Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text(managedUser.displayName)
                            Text(if (managedUser.active) "Активен" else "Отключён")
                            if (managedUser.id == currentUser.id) Text("Текущий пользователь")
                        }
                        OutlinedButton(
                            enabled = managedUser.id != currentUser.id,
                            onClick = { onSetUserActive(managedUser.id, !managedUser.active) },
                        ) {
                            Text(if (managedUser.active) "Отключить" else "Активировать")
                        }
                    }
                    Text("Роли в организации:")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        UserRole.entries.forEach { role ->
                            val hasRole = role in managedUser.roles
                            val protectedSelfAdmin = managedUser.id == currentUser.id && role == UserRole.ADMIN && hasRole
                            FilterChip(
                                selected = hasRole,
                                enabled = managedUser.active && !protectedSelfAdmin,
                                onClick = { onSetRole(managedUser.id, role, !hasRole) },
                                label = { Text(role.name) },
                            )
                        }
                    }
                    if (!managedUser.active) {
                        Text("Роли сохранены, но не дают доступ до повторной активации пользователя.")
                    }
                    if (managedUser.id == currentUser.id && UserRole.ADMIN in managedUser.roles) {
                        Text("Собственную роль ADMIN нельзя отозвать или деактивировать из этого экрана.")
                    }
                }
            }
        }
    }
}
