package com.funmanager.ui.components

import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val VI = Locale.forLanguageTag("vi-VN")
private val DAY_HEADER = DateTimeFormatter.ofPattern("EEEE, dd/MM", VI)
private val SHORT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy", VI)

fun YearMonth.label(): String = "Tháng $monthValue/$year"

fun LocalDate.dayHeader(): String = format(DAY_HEADER).replaceFirstChar { it.titlecase(VI) }

fun LocalDate.shortDate(): String = format(SHORT_DATE)
