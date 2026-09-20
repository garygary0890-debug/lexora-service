package com.lexora.service.feature.fieldwork

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.lexora.service.core.model.InventoryBalance
import com.lexora.service.core.model.InventoryItem
import com.lexora.service.core.model.InventoryLocation
import com.lexora.service.core.model.InventoryMovementType
import com.lexora.service.core.model.RequestStatus
import com.lexora.service.core.model.VisitStatus
import java.util.Locale

private enum class FieldWorkTab { VISITS, DISPATCH, ROUTES, WAREHOUSE }

@Composable
fun FieldWorkScreen(
    state: FieldWorkUiState,
    onCreateVisit: (String, String?) -> Unit,
    onAssignRequest: (String, String?) -> Unit,
    onSelectVisit: (String) -> Unit,
    onChangeVisitStatus: (String, VisitStatus) -> Unit,
    onAddChecklistItem: (String) -> Unit,
    onToggleChecklistItem: (String) -> Unit,
    onSaveTechnicalConclusion: (String) -> Unit,
    onSaveClientSignature: (String, String) -> Unit,
    onAddWork: (String, Double, String?, String?) -> Unit,
    onUseMaterial: (String, String, Double, String?) -> Unit,
    onCreateWarehouse: (String, String?) -> Unit,
    onCreateInventoryItem: (String, String, String, Double?) -> Unit,
    onReceipt: (String, String, Double, String?) -> Unit,
    onWriteOff: (String, String, Double, String?) -> Unit,
    onTransfer: (String, String, String, Double, String?) -> Unit,
    onReserve: (String, String, Double, String?, String?, String?) -> Unit,
    onReleaseReservation: (String, String, Double, String?, String?, String?) -> Unit,
) {
    var tab by remember { mutableStateOf(FieldWorkTab.DISPATCH) }
    var selectedVisitId by remember(state.visits, state.selectedVisitId) { mutableStateOf(state.selectedVisitId ?: state.visits.firstOrNull()?.id) }
    val selectedVisit = state.visits.firstOrNull { it.id == selectedVisitId }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Выездные работы", style = MaterialTheme.typography.headlineMedium)
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            FilterChip(tab == FieldWorkTab.DISPATCH, { tab = FieldWorkTab.DISPATCH }, label = { Text("Диспетчер") })
            FilterChip(tab == FieldWorkTab.ROUTES, { tab = FieldWorkTab.ROUTES }, label = { Text("Маршруты") })
            FilterChip(tab == FieldWorkTab.VISITS, { tab = FieldWorkTab.VISITS }, label = { Text("Выезды") })
            FilterChip(tab == FieldWorkTab.WAREHOUSE, { tab = FieldWorkTab.WAREHOUSE }, label = { Text("Склад") })
        }

        when (tab) {
            FieldWorkTab.DISPATCH -> DispatchBoard(state, onAssignRequest, onCreateVisit)
            FieldWorkTab.ROUTES -> RoutesBoard(state, onCreateVisit)
            FieldWorkTab.VISITS -> {
                VisitsBoard(state, selectedVisitId, { id -> selectedVisitId = id; onSelectVisit(id) }, onChangeVisitStatus)
                selectedVisit?.let { visit ->
                    ChecklistBlock(state, visit.id, onAddChecklistItem, onToggleChecklistItem)
                    ExecutionReportBlock(state, onSaveTechnicalConclusion, onSaveClientSignature, onAddWork, onUseMaterial)
                }
            }
            FieldWorkTab.WAREHOUSE -> WarehouseBlock(
                state, onCreateWarehouse, onCreateInventoryItem, onReceipt, onWriteOff, onTransfer, onReserve, onReleaseReservation,
            )
        }
    }
}

@Composable
private fun ChecklistBlock(state: FieldWorkUiState, visitId: String, onAdd: (String) -> Unit, onToggle: (String) -> Unit) {
    Text("Чек-лист", style = MaterialTheme.typography.titleLarge)
    state.checklist.filter { it.visitId == visitId }.forEach { item ->
        OutlinedButton(onClick = { onToggle(item.id) }, modifier = Modifier.fillMaxWidth()) {
            Text("${if (item.state.name == "DONE") "✓" else "○"} ${item.title}")
        }
    }
    Button(onClick = { onAdd(visitId) }) { Text("Добавить пункт") }
}

@Composable
private fun ExecutionReportBlock(
    state: FieldWorkUiState,
    onSaveConclusion: (String) -> Unit,
    onSaveSignature: (String, String) -> Unit,
    onAddWork: (String, Double, String?, String?) -> Unit,
    onUseMaterial: (String, String, Double, String?) -> Unit,
) {
    val report = state.execution ?: return
    Text("Отчет исполнителя", style = MaterialTheme.typography.titleLarge)
    if (!report.canComplete) Text("До завершения: ${report.completionProblems().joinToString("; ")}")

    var conclusion by remember(report.visit.id, report.visit.resultNote) { mutableStateOf(report.visit.resultNote.orEmpty()) }
    OutlinedTextField(conclusion, { conclusion = it }, label = { Text("Техническое заключение") }, minLines = 3, modifier = Modifier.fillMaxWidth())
    Button(onClick = { onSaveConclusion(conclusion) }, enabled = conclusion.isNotBlank()) { Text("Сохранить заключение") }

    Text("Выполненные работы", style = MaterialTheme.typography.titleMedium)
    if (report.works.isEmpty()) Text("Работы еще не внесены")
    report.works.forEach { work -> Text("• ${work.title} — ${formatQty(work.quantity)} ${work.unit.orEmpty()}${work.note?.let { " · $it" }.orEmpty()}") }
    var workTitle by remember(report.visit.id) { mutableStateOf("") }
    var workQty by remember(report.visit.id) { mutableStateOf("1") }
    var workUnit by remember(report.visit.id) { mutableStateOf("шт.") }
    var workNote by remember(report.visit.id) { mutableStateOf("") }
    OutlinedTextField(workTitle, { workTitle = it }, label = { Text("Выполненная работа") }, modifier = Modifier.fillMaxWidth())
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(workQty, { workQty = it }, label = { Text("Количество") }, modifier = Modifier.weight(1f))
        OutlinedTextField(workUnit, { workUnit = it }, label = { Text("Ед.") }, modifier = Modifier.weight(1f))
    }
    OutlinedTextField(workNote, { workNote = it }, label = { Text("Комментарий к работе") }, modifier = Modifier.fillMaxWidth())
    Button(onClick = {
        val qty = workQty.toDoubleOrNull() ?: return@Button
        onAddWork(workTitle, qty, workUnit.ifBlank { null }, workNote.ifBlank { null })
        workTitle = ""; workQty = "1"; workNote = ""
    }, enabled = workTitle.isNotBlank() && (workQty.toDoubleOrNull() ?: 0.0) > 0) { Text("Добавить работу") }

    Text("Использованные материалы", style = MaterialTheme.typography.titleMedium)
    if (report.materials.isEmpty()) Text("Материалы не использовались или еще не внесены")
    report.materials.forEach { material -> Text("• ${material.title} — ${formatQty(material.quantity)} ${material.unit.orEmpty()}${material.note?.let { " · $it" }.orEmpty()}") }
    MaterialIssueForm(state, onUseMaterial)

    Text("Подпись клиента", style = MaterialTheme.typography.titleMedium)
    ClientSignaturePad(report.visit.customerName.orEmpty(), report.hasClientSignature, onSaveSignature)
}

@Composable
private fun ClientSignaturePad(initialName: String, alreadySigned: Boolean, onSave: (String, String) -> Unit) {
    var customerName by remember(initialName) { mutableStateOf(initialName) }
    val points = remember { mutableStateListOf<Offset?>() }
    OutlinedTextField(customerName, { customerName = it }, label = { Text("ФИО клиента / представителя") }, modifier = Modifier.fillMaxWidth())
    if (alreadySigned) Text("Подпись подтверждена и сохранена")
    Card(Modifier.fillMaxWidth().height(180.dp)) {
        Box(Modifier.fillMaxSize().pointerInput(Unit) {
            detectDragGestures(
                onDragStart = { offset -> points.add(null); points.add(offset) },
                onDrag = { change, _ -> points.add(change.position) },
            )
        }) {
            Canvas(Modifier.fillMaxSize()) {
                val path = Path()
                var started = false
                points.forEach { point ->
                    if (point == null) { started = false }
                    else if (!started) { path.moveTo(point.x, point.y); started = true }
                    else path.lineTo(point.x, point.y)
                }
                drawPath(path, MaterialTheme.colorScheme.onSurface, style = Stroke(width = 4f))
            }
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = { points.clear() }) { Text("Очистить") }
        Button(
            onClick = { onSave(customerName, serializeSignature(points)) },
            enabled = customerName.isNotBlank() && points.filterNotNull().size >= 4,
        ) { Text("Подтвердить подпись") }
    }
}

private fun serializeSignature(points: List<Offset?>): String = buildString {
    append("points:v1:")
    points.forEach { p -> if (p == null) append("|") else append(String.format(Locale.US, "%.1f,%.1f;", p.x, p.y)) }
}

@Composable
private fun MaterialIssueForm(state: FieldWorkUiState, onUseMaterial: (String, String, Double, String?) -> Unit) {
    if (state.inventoryLocations.isEmpty() || state.inventoryItems.isEmpty()) {
        Text("Для списания материала создайте склад и карточку материала на вкладке «Склад».")
        return
    }
    var locationId by remember(state.inventoryLocations) { mutableStateOf(state.inventoryLocations.first().id) }
    var itemId by remember(state.inventoryItems) { mutableStateOf(state.inventoryItems.first().id) }
    var quantity by remember { mutableStateOf("1") }
    var note by remember { mutableStateOf("") }
    Selector("Склад списания", state.inventoryLocations, locationId, { it.id }, { it.name }) { locationId = it }
    Selector("Материал", state.inventoryItems, itemId, { it.id }, { "${it.sku} · ${it.name}" }) { itemId = it }
    OutlinedTextField(quantity, { quantity = it }, label = { Text("Количество") }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(note, { note = it }, label = { Text("Комментарий") }, modifier = Modifier.fillMaxWidth())
    Button(onClick = { onUseMaterial(locationId, itemId, quantity.toDoubleOrNull() ?: 0.0, note.ifBlank { null }) }, enabled = (quantity.toDoubleOrNull() ?: 0.0) > 0) { Text("Списать в выезд") }
}

@Composable
private fun WarehouseBlock(
    state: FieldWorkUiState,
    onCreateWarehouse: (String, String?) -> Unit,
    onCreateItem: (String, String, String, Double?) -> Unit,
    onReceipt: (String, String, Double, String?) -> Unit,
    onWriteOff: (String, String, Double, String?) -> Unit,
    onTransfer: (String, String, String, Double, String?) -> Unit,
    onReserve: (String, String, Double, String?, String?, String?) -> Unit,
    onRelease: (String, String, Double, String?, String?, String?) -> Unit,
) {
    Text("Складской учет", style = MaterialTheme.typography.titleLarge)
    var warehouseName by remember { mutableStateOf("") }
    OutlinedTextField(warehouseName, { warehouseName = it }, label = { Text("Новый склад") }, modifier = Modifier.fillMaxWidth())
    Button(onClick = { onCreateWarehouse(warehouseName, null); warehouseName = "" }, enabled = warehouseName.isNotBlank()) { Text("Создать склад") }

    var sku by remember { mutableStateOf("") }; var itemName by remember { mutableStateOf("") }; var unit by remember { mutableStateOf("шт.") }; var minStock by remember { mutableStateOf("") }
    Text("Новая карточка материала", style = MaterialTheme.typography.titleMedium)
    OutlinedTextField(sku, { sku = it }, label = { Text("Артикул") }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(itemName, { itemName = it }, label = { Text("Наименование") }, modifier = Modifier.fillMaxWidth())
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(unit, { unit = it }, label = { Text("Ед.") }, modifier = Modifier.weight(1f))
        OutlinedTextField(minStock, { minStock = it }, label = { Text("Мин. остаток") }, modifier = Modifier.weight(1f))
    }
    Button(onClick = { onCreateItem(sku, itemName, unit, minStock.toDoubleOrNull()); sku = ""; itemName = "" }, enabled = sku.isNotBlank() && itemName.isNotBlank() && unit.isNotBlank()) { Text("Добавить материал") }

    if (state.inventoryLocations.isEmpty() || state.inventoryItems.isEmpty()) {
        Text("Создайте минимум один склад и один материал, чтобы проводить движения.")
        return
    }

    Text("Остатки", style = MaterialTheme.typography.titleMedium)
    state.inventoryBalances.forEach { balance -> BalanceRow(state, balance) }
    if (state.inventoryBalances.isEmpty()) Text("Остатков пока нет")
    if (state.lowStock.isNotEmpty()) Text("Ниже минимального остатка: ${state.lowStock.size}", color = MaterialTheme.colorScheme.error)

    var locationId by remember(state.inventoryLocations) { mutableStateOf(state.inventoryLocations.first().id) }
    var targetId by remember(state.inventoryLocations) { mutableStateOf(state.inventoryLocations.getOrElse(1) { state.inventoryLocations.first() }.id) }
    var itemId by remember(state.inventoryItems) { mutableStateOf(state.inventoryItems.first().id) }
    var qty by remember { mutableStateOf("1") }
    var note by remember { mutableStateOf("") }
    var requestId by remember(state.requests) { mutableStateOf(state.requests.firstOrNull { it.status !in setOf(RequestStatus.CLOSED, RequestStatus.CANCELLED) }?.id) }
    Selector("Склад", state.inventoryLocations, locationId, { it.id }, { it.name }) { locationId = it }
    Selector("Материал", state.inventoryItems, itemId, { it.id }, { "${it.sku} · ${it.name}" }) { itemId = it }
    OutlinedTextField(qty, { qty = it }, label = { Text("Количество") }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(note, { note = it }, label = { Text("Основание / комментарий") }, modifier = Modifier.fillMaxWidth())
    val quantity = qty.toDoubleOrNull() ?: 0.0
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = { onReceipt(locationId, itemId, quantity, note.ifBlank { null }) }, enabled = quantity > 0) { Text("Приход") }
        OutlinedButton(onClick = { onWriteOff(locationId, itemId, quantity, note.ifBlank { null }) }, enabled = quantity > 0) { Text("Списание") }
    }

    Text("Перемещение", style = MaterialTheme.typography.titleMedium)
    Selector("Склад назначения", state.inventoryLocations, targetId, { it.id }, { it.name }) { targetId = it }
    Button(onClick = { onTransfer(locationId, targetId, itemId, quantity, note.ifBlank { null }) }, enabled = quantity > 0 && targetId != locationId) { Text("Переместить") }

    Text("Резерв под заявку / заказ", style = MaterialTheme.typography.titleMedium)
    if (state.requests.isNotEmpty()) Selector("Заявка", state.requests, requestId.orEmpty(), { it.id }, { "${it.number} · ${it.title}" }) { requestId = it }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = { onReserve(locationId, itemId, quantity, requestId, null, note.ifBlank { null }) }, enabled = quantity > 0 && requestId != null) { Text("Зарезервировать") }
        OutlinedButton(onClick = { onRelease(locationId, itemId, quantity, requestId, null, note.ifBlank { null }) }, enabled = quantity > 0 && requestId != null) { Text("Снять резерв") }
    }

    Text("Последние движения", style = MaterialTheme.typography.titleMedium)
    state.inventoryMovements.take(30).forEach { movement ->
        val item = state.inventoryItems.firstOrNull { it.id == movement.itemId }
        val location = state.inventoryLocations.firstOrNull { it.id == movement.locationId }
        Text("${movement.type.label()} · ${item?.name ?: movement.itemId} · ${formatQty(movement.quantity)} · ${location?.name ?: movement.locationId}")
    }
}

@Composable
private fun BalanceRow(state: FieldWorkUiState, balance: InventoryBalance) {
    val item = state.inventoryItems.firstOrNull { it.id == balance.itemId }
    val location = state.inventoryLocations.firstOrNull { it.id == balance.locationId }
    val available = balance.quantity - balance.reservedQuantity
    Text("${location?.name ?: balance.locationId}: ${item?.name ?: balance.itemId} — ${formatQty(balance.quantity)} ${item?.unit.orEmpty()} (резерв ${formatQty(balance.reservedQuantity)}, доступно ${formatQty(available)})")
}

@Composable
private fun <T> Selector(label: String, values: List<T>, selectedId: String, id: (T) -> String, title: (T) -> String, onSelect: (String) -> Unit) {
    Text(label, style = MaterialTheme.typography.labelLarge)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        values.take(12).forEach { value ->
            FilterChip(selected = id(value) == selectedId, onClick = { onSelect(id(value)) }, label = { Text(title(value)) })
        }
    }
}

@Composable
private fun DispatchBoard(state: FieldWorkUiState, onAssignRequest: (String, String?) -> Unit, onCreateVisit: (String, String?) -> Unit) {
    val unassigned = state.requests.filter { it.assigneeEmployeeId == null && it.status !in setOf(RequestStatus.CLOSED, RequestStatus.CANCELLED) }
    if (unassigned.isEmpty()) { Text("Все открытые заявки распределены."); return }
    unassigned.forEach { request ->
        val decision = state.dispatch.firstOrNull { it.requestId == request.id }
        val employee = state.employees.firstOrNull { it.id == decision?.employeeId }
        Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("${request.number} · ${request.title}", style = MaterialTheme.typography.titleMedium)
            Text("Приоритет: ${request.priority.name}")
            Text("Рекомендация: ${employee?.displayName ?: "нет подходящего исполнителя"}")
            if (employee != null) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onAssignRequest(request.id, employee.id) }) { Text("Назначить") }
                OutlinedButton(onClick = { onCreateVisit(request.id, employee.id) }) { Text("Создать выезд") }
            }
        } }
    }
}

@Composable
private fun RoutesBoard(state: FieldWorkUiState, onCreateVisit: (String, String?) -> Unit) {
    if (state.routes.isEmpty()) { Text("Нет маршрутов: сначала назначьте исполнителей на открытые заявки."); return }
    state.routes.forEach { route ->
        val employee = state.employees.firstOrNull { it.id == route.employeeId }
        Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(employee?.displayName ?: route.employeeId, style = MaterialTheme.typography.titleMedium)
            route.stops.forEachIndexed { index, stop ->
                val request = state.requests.firstOrNull { it.id == stop.requestId }
                Text("${index + 1}. ${request?.number ?: stop.requestId} · ${stop.address}")
                val hasVisit = state.visits.any { it.requestId == stop.requestId && it.status != VisitStatus.CANCELLED }
                if (!hasVisit) OutlinedButton(onClick = { onCreateVisit(stop.requestId, route.employeeId) }) { Text("Создать выезд") }
            }
        } }
    }
}

@Composable
private fun VisitsBoard(state: FieldWorkUiState, selectedVisitId: String?, onSelect: (String) -> Unit, onChangeStatus: (String, VisitStatus) -> Unit) {
    if (state.visits.isEmpty()) { Text("Выезды еще не созданы."); return }
    state.visits.forEach { visit ->
        val request = state.requests.firstOrNull { it.id == visit.requestId }
        val employee = state.employees.firstOrNull { it.id == visit.employeeId }
        Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("${request?.number ?: visit.requestId} · ${request?.title.orEmpty()}", style = MaterialTheme.typography.titleMedium)
            Text("Исполнитель: ${employee?.displayName ?: "не назначен"}")
            Text("Статус: ${visit.status.name}${if (visit.id == selectedVisitId) " · выбран" else ""}")
            OutlinedButton(onClick = { onSelect(visit.id) }) { Text("Открыть") }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                when (visit.status) {
                    VisitStatus.PLANNED -> Button(onClick = { onChangeStatus(visit.id, VisitStatus.EN_ROUTE) }) { Text("Выехал") }
                    VisitStatus.EN_ROUTE -> Button(onClick = { onChangeStatus(visit.id, VisitStatus.ON_SITE) }) { Text("На месте") }
                    VisitStatus.ON_SITE -> Button(onClick = { onChangeStatus(visit.id, VisitStatus.COMPLETED) }) { Text("Завершить") }
                    VisitStatus.COMPLETED, VisitStatus.CANCELLED -> Unit
                }
                if (visit.status != VisitStatus.COMPLETED && visit.status != VisitStatus.CANCELLED) OutlinedButton(onClick = { onChangeStatus(visit.id, VisitStatus.CANCELLED) }) { Text("Отменить") }
            }
        } }
    }
}

private fun InventoryMovementType.label() = when (this) {
    InventoryMovementType.RECEIPT -> "Приход"
    InventoryMovementType.ISSUE -> "Выдача"
    InventoryMovementType.TRANSFER -> "Перемещение"
    InventoryMovementType.WRITE_OFF -> "Списание"
    InventoryMovementType.RESERVATION -> "Резерв"
    InventoryMovementType.RELEASE_RESERVATION -> "Снятие резерва"
    InventoryMovementType.ADJUSTMENT -> "Корректировка"
}

private fun formatQty(value: Double): String = if (value % 1.0 == 0.0) value.toLong().toString() else String.format(Locale.US, "%.3f", value).trimEnd('0').trimEnd('.')
