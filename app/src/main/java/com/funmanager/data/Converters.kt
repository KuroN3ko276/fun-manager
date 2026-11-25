package com.funmanager.data

import androidx.room.TypeConverter
import java.time.LocalDate
import java.time.YearMonth

class Converters {
    @TypeConverter
    fun localDateToEpochDay(date: LocalDate): Long = date.toEpochDay()

    @TypeConverter
    fun epochDayToLocalDate(epochDay: Long): LocalDate = LocalDate.ofEpochDay(epochDay)

    @TypeConverter
    fun yearMonthToString(month: YearMonth): String = month.toString()

    @TypeConverter
    fun stringToYearMonth(value: String): YearMonth = YearMonth.parse(value)
}
