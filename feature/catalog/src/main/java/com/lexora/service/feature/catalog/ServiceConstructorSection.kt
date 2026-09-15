package com.lexora.service.feature.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
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
import com.lexora.service.core.model.ServiceComponentType
import com.lexora.service.core.model.ServiceRecipeSummary

@Composable
fun ServiceConstructorSection(
    summaries: List<ServiceRecipeSummary>,
    onAddComponent: (String, ServiceComponentType, String, Double, String, Long) -> Unit,
    onSetComponentActive: (String, Boolean) -> Unit,
) {
    Text("Конструктор услуг", style = MaterialTheme.typography.titleMedium)
    if (summaries.isEmpty()) {
        Text("Для активных услуг состав пока не настроен.")
        return
    }

    summaries.forEach { summary ->
        var type by remember(summary.recipe.id) { mutableStateOf(ServiceComponentType.WORK) }
        var name by remember(summary.recipe.id) { mutableStateOf("") }
        var quantityText by remember(summary.recipe.id) { mutableStateOf("1") }
        var unit by remember(summary.recipe.id) { mutableStateOf("шт.") }
        var costText by remember(summary.recipe.id) { mutableStateOf("0") }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(summary.recipe.name, style = MaterialTheme.typography.titleSmall)
                summary.recipe.durationMinutes?.let { Text("Нормативная длительность: $it мин") }
                Text("Расчётная себестоимость состава: ${summary.calculatedCostMinor / 100.0} RUB")

                summary.components.forEach { component ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text("${component.type.name} · ${component.name}")
                            Text("${component.quantity} ${component.unit} × ${component.unitCostMinor / 100.0}")
                        }
                        OutlinedButton(onClick = { onSetComponentActive(component.id, !component.active) }) {
                            Text(if (component.active) "Отключить" else "Включить")
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = type == ServiceComponentType.WORK, onClick = { type = ServiceComponentType.WORK }, label = { Text("Работа") })
                    FilterChip(selected = type == ServiceComponentType.MATERIAL, onClick = { type = ServiceComponentType.MATERIAL }, label = { Text("Материал") })
                }
                OutlinedTextField(name, { name = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Наименование компонента") })
                OutlinedTextField(quantityText, { quantityText = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Количество") })
                OutlinedTextField(unit, { unit = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Единица") })
                OutlinedTextField(costText, { costText = it.filter(Char::isDigit) }, modifier = Modifier.fillMaxWidth(), label = { Text("Стоимость единицы, коп.") })
                Button(
                    enabled = name.isNotBlank() && (quantityText.toDoubleOrNull() ?: 0.0) > 0.0,
                    onClick = {
                        onAddComponent(
                            summary.recipe.id,
                            type,
                            name,
                            quantityText.toDoubleOrNull() ?: 1.0,
                            unit,
                            costText.toLongOrNull() ?: 0L,
                        )
                        name = ""
                        quantityText = "1"
                        costText = "0"
                    },
                ) { Text("Добавить компонент") }
            }
        }
    }
}
