package com.lexora.service.feature.home

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

internal fun formatCurrentDateLabel(date: LocalDate): String =
    date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy, EEE", Locale("ru")))
