package io.github.lkhanl.moneytracker.data

import androidx.room.TypeConverter
import kotlinx.datetime.LocalDateTime

class Converters {
    @TypeConverter
    fun fromDateTime(value: LocalDateTime): String = value.toString()

    @TypeConverter
    fun toDateTime(value: String): LocalDateTime = LocalDateTime.parse(value)
}
