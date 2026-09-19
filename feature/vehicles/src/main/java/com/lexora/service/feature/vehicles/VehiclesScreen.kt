package com.lexora.service.feature.vehicles

import androidx.compose.foundation.clickable
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
import com.lexora.service.core.model.ServiceHistoryRecord
import com.lexora.service.core.model.ServiceHistorySourceType
import com.lexora.service.core.model.Vehicle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


data class VehicleDraft(
    val clientId: String? = null,
    val registrationNumber: String = "",
    val vin: String = "",
    val make: String = "",
    val model: String = "",
    val year: String = "",
    val bodyType: String = "",
    val color: String = "",
    val mileageKm: String = "",
)

@Composable
fun VehiclesScreen(
    vehicles: List<Vehicle>,
    archivedVehicles: List<Vehicle>,
    clients: List<Client>,
    onSave: (VehicleDraft, String?) -> Unit,
    onArchive: (String) -> Unit,
    onRestore: (String) -> Unit,
    historyVehicle: Vehicle? = null,
    historyRecords: List<ServiceHistoryRecord> = emptyList(),
    historyLoading: Boolean = false,
    onOpenHistory: (String) -> Unit = {},
    onCloseHistory: () -> Unit = {},
) {
    var query by remember { mutableStateOf("") }
    var showArchived by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Vehicle?>(null) }
    var creating by remember { mutableStateOf(false) }
    val source = if (showArchived) archivedVehicles else vehicles
    val filtered = source.filter { vehicle ->
        val q = query.trim().lowercase()
        q.isBlank() || listOfNotNull(vehicle.registrationNumber, vehicle.vin, vehicle.make, vehicle.model)
            .any { it.lowercase().contains(q) }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.vehicles_title), style = MaterialTheme.typography.headlineMedium)
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.vehicles_search_hint)) },
            singleLine = true,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = !showArchived,
                onClick = { showArchived = false },
                label = { Text(stringResource(R.string.vehicles_active)) },
            )
            FilterChip(
                selected = showArchived,
                onClick = { showArchived = true },
                label = { Text(stringResource(R.string.vehicles_archived)) },
            )
        }
        Button(onClick = { creating = true }) { Text(stringResource(R.string.vehicles_add)) }

        if (filtered.isEmpty()) {
            Text(stringResource(R.string.vehicles_empty))
        } else {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                filtered.forEach { vehicle ->
                    val owner = clients.firstOrNull { it.id == vehicle.clientId }?.displayName
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable(enabled = !showArchived) { editing = vehicle },
                    ) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(vehicle.registrationNumber, style = MaterialTheme.typography.titleMedium)
                            val carName = listOfNotNull(vehicle.make, vehicle.model).filter { it.isNotBlank() }.joinToString(" ")
                            if (carName.isNotBlank()) Text(carName)
                            vehicle.vin?.takeIf { it.isNotBlank() }?.let { Text("VIN: $it") }
                            Text("${stringResource(R.string.vehicles_owner)}: ${owner ?: stringResource(R.string.vehicles_no_owner)}")
                            vehicle.mileageKm?.let { Text("${stringResource(R.string.vehicles_mileage)}: $it") }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = { onOpenHistory(vehicle.id) }) { Text("История") }
                                if (showArchived) {
                                    TextButton(onClick = { onRestore(vehicle.id) }) { Text(stringResource(R.string.vehicles_restore)) }
                                } else {
                                    TextButton(onClick = { editing = vehicle }) { Text(stringResource(R.string.vehicles_edit)) }
                                    TextButton(onClick = { onArchive(vehicle.id) }) { Text(stringResource(R.string.vehicles_archive)) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (creating || editing != null) {
        VehicleEditorDialog(
            vehicle = editing,
            clients = clients,
            existingVehicles = vehicles + archivedVehicles,
            onDismiss = { creating = false; editing = null },
            onSave = { draft ->
                onSave(draft, editing?.id)
                creating = false
                editing = null
            },
        )
    }

    historyVehicle?.let { vehicle ->
        ServiceHistoryDialog(
            vehicle = vehicle,
            records = historyRecords,
            loading = historyLoading,
            onDismiss = onCloseHistory,
        )
    }
}

@Composable
private fun ServiceHistoryDialog(
    vehicle: Vehicle,
    records: List<ServiceHistoryRecord>,
    loading: Boolean,
    onDismiss: () -> Unit,
) {
    val formatter = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("История обслуживания · ${vehicle.registrationNumber}") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                when {
                    loading -> Text("Загрузка истории…")
                    records.isEmpty() -> Text("История обслуживания пока пуста.")
                    else -> records.forEach { record ->
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(record.title, style = MaterialTheme.typography.titleSmall)
                                Text("${record.sourceType.displayName()} · ${formatter.format(Date(record.occurredAtEpochMs))}")
                                record.mileageKm?.let { Text("Пробег: $it км") }
                                record.description?.takeIf { it.isNotBlank() }?.let { Text(it) }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Закрыть") } },
    )
}

private fun ServiceHistorySourceType.displayName(): String = when (this) {
    ServiceHistorySourceType.SERVICE_REQUEST -> "Заявка"
    ServiceHistorySourceType.SERVICE_VISIT -> "Работы"
    ServiceHistorySourceType.SERVICE_DOCUMENT -> "Документ"
    ServiceHistorySourceType.PAYMENT -> "Оплата"
    ServiceHistorySourceType.MANUAL -> "Запись"
}

@Composable
private fun VehicleEditorDialog(
    vehicle: Vehicle?,
    clients: List<Client>,
    existingVehicles: List<Vehicle>,
    onDismiss: () -> Unit,
    onSave: (VehicleDraft) -> Unit,
) {
    var draft by remember(vehicle) {
        mutableStateOf(
            VehicleDraft(
                clientId = vehicle?.clientId,
                registrationNumber = vehicle?.registrationNumber.orEmpty(),
                vin = vehicle?.vin.orEmpty(),
                make = vehicle?.make.orEmpty(),
                model = vehicle?.model.orEmpty(),
                year = vehicle?.year?.toString().orEmpty(),
                bodyType = vehicle?.bodyType.orEmpty(),
                color = vehicle?.color.orEmpty(),
                mileageKm = vehicle?.mileageKm?.toString().orEmpty(),
            )
        )
    }
    var error by remember { mutableStateOf<String?>(null) }
    var ownerPicker by remember { mutableStateOf(false) }

    fun validate(): Boolean {
        val year = draft.year.toIntOrNull()
        val mileage = draft.mileageKm.toIntOrNull()
        if (draft.registrationNumber.isBlank()) {
            error = "Госномер обязателен."
            return false
        }
        if (draft.year.isNotBlank() && (year == null || year !in 1900..2100)) {
            error = "INVALID_YEAR"
            return false
        }
        if (draft.mileageKm.isNotBlank() && (mileage == null || mileage < 0)) {
            error = "INVALID_MILEAGE"
            return false
        }
        val normalizedReg = draft.registrationNumber.filterNot(Char::isWhitespace).uppercase()
        val normalizedVin = draft.vin.filterNot(Char::isWhitespace).uppercase()
        val duplicate = existingVehicles.any { other ->
            other.id != vehicle?.id && (
                other.registrationNumber.filterNot(Char::isWhitespace).uppercase() == normalizedReg ||
                    (normalizedVin.isNotBlank() && other.vin?.filterNot(Char::isWhitespace)?.uppercase() == normalizedVin)
                )
        }
        if (duplicate) {
            error = "DUPLICATE"
            return false
        }
        error = null
        return true
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (vehicle == null) stringResource(R.string.vehicles_add) else stringResource(R.string.vehicles_edit)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(draft.registrationNumber, { draft = draft.copy(registrationNumber = it) }, label = { Text(stringResource(R.string.vehicles_registration)) })
                OutlinedTextField(draft.vin, { draft = draft.copy(vin = it) }, label = { Text(stringResource(R.string.vehicles_vin)) })
                OutlinedTextField(draft.make, { draft = draft.copy(make = it) }, label = { Text(stringResource(R.string.vehicles_make)) })
                OutlinedTextField(draft.model, { draft = draft.copy(model = it) }, label = { Text(stringResource(R.string.vehicles_model)) })
                OutlinedTextField(draft.year, { draft = draft.copy(year = it.filter(Char::isDigit)) }, label = { Text(stringResource(R.string.vehicles_year)) })
                OutlinedTextField(draft.bodyType, { draft = draft.copy(bodyType = it) }, label = { Text(stringResource(R.string.vehicles_body_type)) })
                OutlinedTextField(draft.color, { draft = draft.copy(color = it) }, label = { Text(stringResource(R.string.vehicles_color)) })
                OutlinedTextField(draft.mileageKm, { draft = draft.copy(mileageKm = it.filter(Char::isDigit)) }, label = { Text(stringResource(R.string.vehicles_mileage)) })

                OutlinedButton(onClick = { ownerPicker = true }) {
                    val owner = clients.firstOrNull { it.id == draft.clientId }?.displayName
                    Text(owner ?: stringResource(R.string.vehicles_no_owner))
                }
                error?.let {
                    val text = when (it) {
                        "DUPLICATE" -> stringResource(R.string.vehicles_duplicate)
                        "INVALID_YEAR" -> stringResource(R.string.vehicles_invalid_year)
                        "INVALID_MILEAGE" -> stringResource(R.string.vehicles_invalid_mileage)
                        else -> it
                    }
                    Text(text, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { if (validate()) onSave(draft) }) { Text(stringResource(R.string.vehicles_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.vehicles_cancel)) } },
    )

    if (ownerPicker) {
        AlertDialog(
            onDismissRequest = { ownerPicker = false },
            title = { Text(stringResource(R.string.vehicles_owner)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = { draft = draft.copy(clientId = null); ownerPicker = false }) {
                        Text(stringResource(R.string.vehicles_no_owner))
                    }
                    clients.filterNot { it.archived }.forEach { client ->
                        TextButton(onClick = { draft = draft.copy(clientId = client.id); ownerPicker = false }) {
                            Text(client.displayName)
                        }
                    }
                }
            },
            confirmButton = {},
        )
    }
}
