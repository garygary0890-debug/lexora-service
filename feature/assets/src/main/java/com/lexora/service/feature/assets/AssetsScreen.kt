package com.lexora.service.feature.assets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import com.lexora.service.core.model.Equipment
import com.lexora.service.core.model.ServiceObject

data class ServiceObjectDraft(
    val clientId: String? = null,
    val name: String = "",
    val address: String = "",
    val accessMode: String = "",
    val responsibleContact: String = "",
)

data class EquipmentDraft(
    val serviceObjectId: String? = null,
    val type: String = "",
    val make: String = "",
    val model: String = "",
    val serialNumber: String = "",
    val inventoryNumber: String = "",
    val barcode: String = "",
    val commissionedNote: String = "",
    val warrantyNote: String = "",
)

private enum class AssetsTab { OBJECTS, EQUIPMENT }

@Composable
fun AssetsScreen(
    objects: List<ServiceObject>,
    archivedObjects: List<ServiceObject>,
    equipment: List<Equipment>,
    archivedEquipment: List<Equipment>,
    clients: List<Client>,
    onSaveObject: (ServiceObjectDraft, String?) -> Unit,
    onArchiveObject: (String) -> Unit,
    onRestoreObject: (String) -> Unit,
    onSaveEquipment: (EquipmentDraft, String?) -> Unit,
    onArchiveEquipment: (String) -> Unit,
    onRestoreEquipment: (String) -> Unit,
) {
    var tab by remember { mutableStateOf(AssetsTab.OBJECTS) }
    var query by remember { mutableStateOf("") }
    var showArchived by remember { mutableStateOf(false) }
    var creatingObject by remember { mutableStateOf(false) }
    var editingObject by remember { mutableStateOf<ServiceObject?>(null) }
    var creatingEquipment by remember { mutableStateOf(false) }
    var editingEquipment by remember { mutableStateOf<Equipment?>(null) }

    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.assets_title), style = MaterialTheme.typography.headlineMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = tab == AssetsTab.OBJECTS,
                onClick = { tab = AssetsTab.OBJECTS },
                label = { Text(stringResource(R.string.assets_objects)) },
            )
            FilterChip(
                selected = tab == AssetsTab.EQUIPMENT,
                onClick = { tab = AssetsTab.EQUIPMENT },
                label = { Text(stringResource(R.string.assets_equipment)) },
            )
        }
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.assets_search)) },
            singleLine = true,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = !showArchived,
                onClick = { showArchived = false },
                label = { Text(stringResource(R.string.assets_active)) },
            )
            FilterChip(
                selected = showArchived,
                onClick = { showArchived = true },
                label = { Text(stringResource(R.string.assets_archived)) },
            )
        }

        when (tab) {
            AssetsTab.OBJECTS -> {
                Button(onClick = { creatingObject = true }) { Text(stringResource(R.string.assets_add_object)) }
                val source = if (showArchived) archivedObjects else objects
                val q = query.trim().lowercase()
                val filtered = source.filter { item ->
                    q.isBlank() || listOfNotNull(item.name, item.address, item.responsibleContact)
                        .any { it.lowercase().contains(q) }
                }
                if (filtered.isEmpty()) {
                    Text(stringResource(R.string.assets_empty))
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        filtered.forEach { item ->
                            val clientName = clients.firstOrNull { it.id == item.clientId }?.displayName
                            Card(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(item.name, style = MaterialTheme.typography.titleMedium)
                                    item.address?.let { Text(it) }
                                    clientName?.let { Text("${stringResource(R.string.assets_client)}: $it") }
                                    item.responsibleContact?.let { Text(it) }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        if (showArchived) {
                                            TextButton(onClick = { onRestoreObject(item.id) }) { Text(stringResource(R.string.assets_restore)) }
                                        } else {
                                            TextButton(onClick = { editingObject = item }) { Text(stringResource(R.string.assets_edit)) }
                                            TextButton(onClick = { onArchiveObject(item.id) }) { Text(stringResource(R.string.assets_archive)) }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            AssetsTab.EQUIPMENT -> {
                Button(onClick = { creatingEquipment = true }) { Text(stringResource(R.string.assets_add_equipment)) }
                val source = if (showArchived) archivedEquipment else equipment
                val q = query.trim().lowercase()
                val filtered = source.filter { item ->
                    q.isBlank() || listOfNotNull(item.type, item.make, item.model, item.serialNumber, item.inventoryNumber, item.barcode)
                        .any { it.lowercase().contains(q) }
                }
                if (filtered.isEmpty()) {
                    Text(stringResource(R.string.assets_empty))
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        filtered.forEach { item ->
                            val objectName = objects.plus(archivedObjects).firstOrNull { it.id == item.serviceObjectId }?.name
                            Card(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(listOfNotNull(item.make, item.model).filter { it.isNotBlank() }.joinToString(" ").ifBlank { item.type }, style = MaterialTheme.typography.titleMedium)
                                    item.serialNumber?.let { Text("${stringResource(R.string.assets_serial)}: $it") }
                                    objectName?.let { Text("${stringResource(R.string.assets_object)}: $it") }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        if (showArchived) {
                                            TextButton(onClick = { onRestoreEquipment(item.id) }) { Text(stringResource(R.string.assets_restore)) }
                                        } else {
                                            TextButton(onClick = { editingEquipment = item }) { Text(stringResource(R.string.assets_edit)) }
                                            TextButton(onClick = { onArchiveEquipment(item.id) }) { Text(stringResource(R.string.assets_archive)) }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (creatingObject || editingObject != null) {
        ServiceObjectEditor(
            existing = editingObject,
            clients = clients.filterNot { it.archived },
            onDismiss = { creatingObject = false; editingObject = null },
            onSave = { draft ->
                onSaveObject(draft, editingObject?.id)
                creatingObject = false
                editingObject = null
            },
        )
    }

    if (creatingEquipment || editingEquipment != null) {
        EquipmentEditor(
            existing = editingEquipment,
            objects = objects,
            allEquipment = equipment + archivedEquipment,
            onDismiss = { creatingEquipment = false; editingEquipment = null },
            onSave = { draft ->
                onSaveEquipment(draft, editingEquipment?.id)
                creatingEquipment = false
                editingEquipment = null
            },
        )
    }
}

@Composable
private fun ServiceObjectEditor(
    existing: ServiceObject?,
    clients: List<Client>,
    onDismiss: () -> Unit,
    onSave: (ServiceObjectDraft) -> Unit,
) {
    var draft by remember(existing?.id) {
        mutableStateOf(
            ServiceObjectDraft(
                clientId = existing?.clientId,
                name = existing?.name.orEmpty(),
                address = existing?.address.orEmpty(),
                accessMode = existing?.accessMode.orEmpty(),
                responsibleContact = existing?.responsibleContact.orEmpty(),
            )
        )
    }
    var pickClient by remember { mutableStateOf(false) }
    var submitted by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) stringResource(R.string.assets_add_object) else stringResource(R.string.assets_edit)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(draft.name, { draft = draft.copy(name = it) }, label = { Text(stringResource(R.string.assets_name)) })
                if (submitted && draft.name.isBlank()) Text(stringResource(R.string.assets_name_required), color = MaterialTheme.colorScheme.error)
                OutlinedTextField(draft.address, { draft = draft.copy(address = it) }, label = { Text(stringResource(R.string.assets_address)) })
                OutlinedTextField(draft.accessMode, { draft = draft.copy(accessMode = it) }, label = { Text(stringResource(R.string.assets_access_mode)) })
                OutlinedTextField(draft.responsibleContact, { draft = draft.copy(responsibleContact = it) }, label = { Text(stringResource(R.string.assets_contact)) })
                OutlinedButton(onClick = { pickClient = true }) {
                    Text(clients.firstOrNull { it.id == draft.clientId }?.displayName ?: stringResource(R.string.assets_no_client))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                submitted = true
                if (draft.name.isNotBlank()) onSave(draft.copy(name = draft.name.trim()))
            }) { Text(stringResource(R.string.assets_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.assets_cancel)) } },
    )

    if (pickClient) {
        AlertDialog(
            onDismissRequest = { pickClient = false },
            title = { Text(stringResource(R.string.assets_client)) },
            text = {
                Column {
                    TextButton(onClick = { draft = draft.copy(clientId = null); pickClient = false }) { Text(stringResource(R.string.assets_no_client)) }
                    clients.forEach { client ->
                        TextButton(onClick = { draft = draft.copy(clientId = client.id); pickClient = false }) { Text(client.displayName) }
                    }
                }
            },
            confirmButton = {},
        )
    }
}

@Composable
private fun EquipmentEditor(
    existing: Equipment?,
    objects: List<ServiceObject>,
    allEquipment: List<Equipment>,
    onDismiss: () -> Unit,
    onSave: (EquipmentDraft) -> Unit,
) {
    var draft by remember(existing?.id) {
        mutableStateOf(
            EquipmentDraft(
                serviceObjectId = existing?.serviceObjectId,
                type = existing?.type.orEmpty(),
                make = existing?.make.orEmpty(),
                model = existing?.model.orEmpty(),
                serialNumber = existing?.serialNumber.orEmpty(),
                inventoryNumber = existing?.inventoryNumber.orEmpty(),
                barcode = existing?.barcode.orEmpty(),
                commissionedNote = existing?.commissionedNote.orEmpty(),
                warrantyNote = existing?.warrantyNote.orEmpty(),
            )
        )
    }
    var pickObject by remember { mutableStateOf(false) }
    var submitted by remember { mutableStateOf(false) }
    val normalizedSerial = draft.serialNumber.trim().uppercase()
    val duplicateSerial = normalizedSerial.isNotBlank() && allEquipment.any {
        it.id != existing?.id && it.serialNumber?.trim()?.uppercase() == normalizedSerial
    }
    val nameMissing = draft.type.isBlank() && draft.make.isBlank() && draft.model.isBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) stringResource(R.string.assets_add_equipment) else stringResource(R.string.assets_edit)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(draft.type, { draft = draft.copy(type = it) }, label = { Text(stringResource(R.string.assets_type)) })
                OutlinedTextField(draft.make, { draft = draft.copy(make = it) }, label = { Text(stringResource(R.string.assets_make)) })
                OutlinedTextField(draft.model, { draft = draft.copy(model = it) }, label = { Text(stringResource(R.string.assets_model)) })
                if (submitted && nameMissing) Text(stringResource(R.string.assets_name_required), color = MaterialTheme.colorScheme.error)
                OutlinedTextField(draft.serialNumber, { draft = draft.copy(serialNumber = it) }, label = { Text(stringResource(R.string.assets_serial)) })
                if (duplicateSerial) Text(stringResource(R.string.assets_serial_duplicate), color = MaterialTheme.colorScheme.error)
                OutlinedTextField(draft.inventoryNumber, { draft = draft.copy(inventoryNumber = it) }, label = { Text(stringResource(R.string.assets_inventory)) })
                OutlinedTextField(draft.barcode, { draft = draft.copy(barcode = it) }, label = { Text(stringResource(R.string.assets_qr)) })
                OutlinedTextField(draft.commissionedNote, { draft = draft.copy(commissionedNote = it) }, label = { Text(stringResource(R.string.assets_commissioned)) })
                OutlinedTextField(draft.warrantyNote, { draft = draft.copy(warrantyNote = it) }, label = { Text(stringResource(R.string.assets_warranty)) })
                OutlinedButton(onClick = { pickObject = true }) {
                    Text(objects.firstOrNull { it.id == draft.serviceObjectId }?.name ?: stringResource(R.string.assets_no_object))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                submitted = true
                if (!nameMissing && !duplicateSerial) onSave(draft)
            }) { Text(stringResource(R.string.assets_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.assets_cancel)) } },
    )

    if (pickObject) {
        AlertDialog(
            onDismissRequest = { pickObject = false },
            title = { Text(stringResource(R.string.assets_object)) },
            text = {
                Column {
                    TextButton(onClick = { draft = draft.copy(serviceObjectId = null); pickObject = false }) { Text(stringResource(R.string.assets_no_object)) }
                    objects.forEach { item ->
                        TextButton(onClick = { draft = draft.copy(serviceObjectId = item.id); pickObject = false }) { Text(item.name) }
                    }
                }
            },
            confirmButton = {},
        )
    }
}
