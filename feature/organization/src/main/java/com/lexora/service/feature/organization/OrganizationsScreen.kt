package com.lexora.service.feature.organization

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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

@Composable
fun OrganizationsScreen(
    activeOrganization: Organization,
    organizations: List<Organization>,
    allowedOrganizationIds: Set<String>,
    canManageOrganization: Boolean,
    onCreateOrganization: (String) -> Unit,
    onSwitchOrganization: (String) -> Unit,
) {
    var newOrganizationName by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Организации")
        Text("Активная организация: ${activeOrganization.name}")

        organizations.forEach { organization ->
            val allowed = organization.id in allowedOrganizationIds
            Card(Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(organization.name)
                    Text(
                        when {
                            organization.id == activeOrganization.id -> "Активная"
                            allowed -> "Доступна пользователю"
                            else -> "Нет членства пользователя"
                        },
                    )
                    if (organization.id != activeOrganization.id) {
                        OutlinedButton(
                            enabled = allowed,
                            onClick = { onSwitchOrganization(organization.id) },
                        ) { Text("Сделать активной") }
                    }
                }
            }
        }

        if (canManageOrganization) {
            Text("Создать организацию")
            OutlinedTextField(
                value = newOrganizationName,
                onValueChange = { newOrganizationName = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Название организации") },
                singleLine = true,
            )
            Button(
                enabled = newOrganizationName.isNotBlank(),
                onClick = {
                    onCreateOrganization(newOrganizationName.trim())
                    newOrganizationName = ""
                },
            ) { Text("Создать") }
        }
    }
}
