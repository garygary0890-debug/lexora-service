package com.lexora.service.feature.tires

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun TiresScreen() {
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text("Модуль «Шиномонтаж»")
        Text("Заготовка: очередь • посты • диагностика • хранение шин")
    }
}
