package com.lexora.service.feature.clients

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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lexora.service.core.model.Client
import com.lexora.service.core.model.ClientType

@Composable
fun ClientsScreen(
    clients: List<Client>,
    archivedClients: List<Client>,
    onSave: (ClientDraft, String?) -> Unit,
    onArchive: (String) -> Unit,
    onRestore: (String) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var showArchive by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Client?>(null) }
    var creating by remember { mutableStateOf(false) }

    val source = if (showArchive) archivedClients else clients
    val normalizedQuery = query.trim().lowercase()
    val filtered = source.filter { client ->
        normalizedQuery.isBlank() || listOfNotNull(
            client.displayName,
            client.phone,
            client.email,
            client.taxId,
        ).any { it.lowercase().contains(normalizedQuery) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.clients_title), style = MaterialTheme.typography.headlineMedium)
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text(stringResource(R.string.clients_search_hint)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { creating = true }) {
                Text(stringResource(R.string.clients_add))
            }
            OutlinedButton(onClick = { showArchive = !showArchive }) {
                Text(stringResource(if (showArchive) R.string.clients_hide_archive else R.string.clients_show_archive))
            }
        }

        if (filtered.isEmpty()) {
            Text(stringResource(R.string.clients_empty))
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                filtered.forEach { client ->
                    ClientCard(
                        client = client,
                        onEdit = { editing = client },
                        onArchive = { onArchive(client.id) },
                        onRestore = { onRestore(client.id) },
                    )
                }
            }
        }
    }

    if (creating) {
        ClientEditor(
            existing = null,
            allActiveClients = clients,
            onDismiss = { creating = false },
            onSave = { draft ->
                onSave(draft, null)
                creating = false
            },
        )
    }

    editing?.let { client ->
        ClientEditor(
            existing = client,
            allActiveClients = clients,
            onDismiss = { editing = null },
            onSave = { draft ->
                onSave(draft, client.id)
                editing = null
            },
        )
    }
}

@Composable
private fun ClientCard(
    client: Client,
    onEdit: () -> Unit,
    onArchive: () -> Unit,
    onRestore: () -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(client.displayName, style = MaterialTheme.typography.titleMedium)
                if (client.archived) {
                    Text(stringResource(R.string.client_archived_badge), style = MaterialTheme.typography.labelMedium)
                }
            }
            Text(
                stringResource(
                    if (client.type == ClientType.PERSON) R.string.client_type_person else R.string.client_type_company
                ),
                style = MaterialTheme.typography.bodySmall,
            )
            client.phone?.takeIf { it.isNotBlank() }?.let { Text(it) }
            client.email?.takeIf { it.isNotBlank() }?.let { Text(it) }
            client.taxId?.takeIf { it.isNotBlank() }?.let { Text(stringResource(R.string.client_tax_id_value, it)) }
            Text(stringResource(R.string.client_sync_state, client.syncState.name), style = MaterialTheme.typography.labelSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!client.archived) {
                    TextButton(onClick = onEdit) { Text(stringResource(R.string.clients_edit)) }
                    TextButton(onClick = onArchive) { Text(stringResource(R.string.clients_archive)) }
                } else {
                    TextButton(onClick = onRestore) { Text(stringResource(R.string.clients_restore)) }
                }
            }
        }
    }
}

data class ClientDraft(
    val type: ClientType,
    val displayName: String,
    val phone: String,
    val email: String,
    val taxId: String,
    val kpp: String,
    val registrationAddress: String,
    val actualAddress: String,
    val note: String,
    val consentPersonalData: Boolean,
)

@Composable
private fun ClientEditor(
    existing: Client?,
    allActiveClients: List<Client>,
    onDismiss: () -> Unit,
    onSave: (ClientDraft) -> Unit,
) {
    var type by remember(existing?.id) { mutableStateOf(existing?.type ?: ClientType.PERSON) }
    var name by remember(existing?.id) { mutableStateOf(existing?.displayName.orEmpty()) }
    var phone by remember(existing?.id) { mutableStateOf(existing?.phone.orEmpty()) }
    var email by remember(existing?.id) { mutableStateOf(existing?.email.orEmpty()) }
    var taxId by remember(existing?.id) { mutableStateOf(existing?.taxId.orEmpty()) }
    var kpp by remember(existing?.id) { mutableStateOf(existing?.kpp.orEmpty()) }
    var registrationAddress by remember(existing?.id) { mutableStateOf(existing?.registrationAddress.orEmpty()) }
    var actualAddress by remember(existing?.id) { mutableStateOf(existing?.actualAddress.orEmpty()) }
    var note by remember(existing?.id) { mutableStateOf(existing?.note.orEmpty()) }
    var consent by remember(existing?.id) { mutableStateOf(existing?.consentPersonalData ?: false) }
    var submitted by remember(existing?.id) { mutableStateOf(false) }

    val normalizedPhone = normalizePhone(phone)
    val normalizedTaxId = taxId.filter(Char::isDigit)
    val duplicatePhone = normalizedPhone.isNotBlank() && allActiveClients.any {
        it.id != existing?.id && normalizePhone(it.phone.orEmpty()) == normalizedPhone
    }
    val duplicateTaxId = normalizedTaxId.isNotBlank() && allActiveClients.any {
        it.id != existing?.id && it.taxId.orEmpty().filter(Char::isDigit) == normalizedTaxId
    }
    val taxIdInvalid = normalizedTaxId.isNotBlank() && normalizedTaxId.length !in setOf(10, 12)
    val kppDigits = kpp.filter(Char::isDigit)
    val kppInvalid = type == ClientType.COMPANY && kppDigits.isNotBlank() && kppDigits.length != 9
    val nameInvalid = name.isBlank()
    val canSave = !nameInvalid && !taxIdInvalid && !kppInvalid && !duplicatePhone && !duplicateTaxId

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) stringResource(R.string.clients_add) else stringResource(R.string.clients_edit)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = type == ClientType.PERSON,
                        onClick = { type = ClientType.PERSON },
                        label = { Text(stringResource(R.string.client_type_person)) },
                    )
                    FilterChip(
                        selected = type == ClientType.COMPANY,
                        onClick = { type = ClientType.COMPANY },
                        label = { Text(stringResource(R.string.client_type_company)) },
                    )
                }
                OutlinedTextField(name, { name = it }, label = { Text(stringResource(R.string.client_name)) }, modifier = Modifier.fillMaxWidth())
                if (submitted && nameInvalid) Text(stringResource(R.string.client_name_required), color = MaterialTheme.colorScheme.error)
                OutlinedTextField(phone, { phone = it }, label = { Text(stringResource(R.string.client_phone)) }, modifier = Modifier.fillMaxWidth())
                if (duplicatePhone) Text(stringResource(R.string.client_duplicate_phone), color = MaterialTheme.colorScheme.error)
                OutlinedTextField(email, { email = it }, label = { Text(stringResource(R.string.client_email)) }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(taxId, { taxId = it.filter(Char::isDigit).take(12) }, label = { Text(stringResource(R.string.client_tax_id)) }, modifier = Modifier.fillMaxWidth())
                if (taxIdInvalid) Text(stringResource(R.string.client_tax_id_invalid), color = MaterialTheme.colorScheme.error)
                if (duplicateTaxId) Text(stringResource(R.string.client_duplicate_tax_id), color = MaterialTheme.colorScheme.error)
                if (type == ClientType.COMPANY) {
                    OutlinedTextField(kpp, { kpp = it.filter(Char::isDigit).take(9) }, label = { Text(stringResource(R.string.client_kpp)) }, modifier = Modifier.fillMaxWidth())
                    if (kppInvalid) Text(stringResource(R.string.client_kpp_invalid), color = MaterialTheme.colorScheme.error)
                }
                OutlinedTextField(registrationAddress, { registrationAddress = it }, label = { Text(stringResource(R.string.client_registration_address)) }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(actualAddress, { actualAddress = it }, label = { Text(stringResource(R.string.client_actual_address)) }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(note, { note = it }, label = { Text(stringResource(R.string.client_note)) }, modifier = Modifier.fillMaxWidth())
                Row {
                    Checkbox(checked = consent, onCheckedChange = { consent = it })
                    Text(stringResource(R.string.client_consent), modifier = Modifier.padding(top = 12.dp))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                submitted = true
                if (canSave) {
                    onSave(
                        ClientDraft(
                            type = type,
                            displayName = name.trim(),
                            phone = phone.trim(),
                            email = email.trim(),
                            taxId = normalizedTaxId,
                            kpp = if (type == ClientType.COMPANY) kppDigits else "",
                            registrationAddress = registrationAddress.trim(),
                            actualAddress = actualAddress.trim(),
                            note = note.trim(),
                            consentPersonalData = consent,
                        )
                    )
                }
            }) { Text(stringResource(R.string.common_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) } },
    )
}

private fun normalizePhone(value: String): String = value.filter(Char::isDigit).takeLast(10)
