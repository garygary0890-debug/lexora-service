package com.lexora.service.feature.wash

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun WashScreen() {
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text("Модуль «Автомойка»")
        Text("Заготовка: очередь • посты • услуги • контроль качества")
    }
}
