package io.github.lkhanl.moneytracker.core

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime

/** "21/10/2026" ya da "21.10.2026" gibi gün-ay-yıl sıralı tarihi okur. */
fun parseDayMonthYear(text: String, separator: Char): LocalDate {
    val (d, m, y) = text.split(separator).map { it.toInt() }
    return LocalDate(y, m, d)
}

fun LocalDate.atStartOfDay(): LocalDateTime = LocalDateTime(this, LocalTime(0, 0))

/** Anahtarın kaçıncı kez görüldüğünü döndürür (1'den başlar). */
internal fun MutableMap<String, Int>.occurrence(key: String): Int =
    (getOrElse(key) { 0 } + 1).also { put(key, it) }

/**
 * Türkçe kurallarıyla büyük harf: "i" → "İ", "ı" → "I". Platformun yerel ayarına bağlı
 * `uppercase(Locale)` ortak kodda olmadığı için.
 */
fun String.uppercaseTr(): String = buildString(length) {
    for (c in this@uppercaseTr) when (c) {
        'i' -> append('İ')
        'ı' -> append('I')
        else -> append(c.uppercase())
    }
}

/** Türkçe kurallarıyla küçük harf: "I" → "ı", "İ" → "i". */
fun String.lowercaseTr(): String = buildString(length) {
    for (c in this@lowercaseTr) when (c) {
        'I' -> append('ı')
        'İ' -> append('i')
        else -> append(c.lowercase())
    }
}
