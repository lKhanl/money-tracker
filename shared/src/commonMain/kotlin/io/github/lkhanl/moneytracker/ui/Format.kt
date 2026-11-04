package io.github.lkhanl.moneytracker.ui

import io.github.lkhanl.moneytracker.core.BankTemplates
import io.github.lkhanl.moneytracker.core.Category
import io.github.lkhanl.moneytracker.core.Kind
import io.github.lkhanl.moneytracker.core.Money
import io.github.lkhanl.moneytracker.resources.Res
import io.github.lkhanl.moneytracker.resources.ic_cat_fuel
import io.github.lkhanl.moneytracker.resources.ic_cat_market
import io.github.lkhanl.moneytracker.resources.ic_cat_other
import io.github.lkhanl.moneytracker.resources.ic_cat_shopping
import io.github.lkhanl.moneytracker.resources.ic_cat_subscription
import io.github.lkhanl.moneytracker.resources.ic_cat_transfer
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.Month
import kotlinx.datetime.YearMonth
import org.jetbrains.compose.resources.DrawableResource
import kotlin.math.abs
import kotlin.math.roundToLong

// Türkçe biçimler elle yazılıdır: ortak kodda java.text / DateTimeFormatter yok, uygulama da yalnızca Türkçe.

private val monthNames = listOf(
    "Ocak", "Şubat", "Mart", "Nisan", "Mayıs", "Haziran",
    "Temmuz", "Ağustos", "Eylül", "Ekim", "Kasım", "Aralık",
)
private val monthShortNames = listOf("Oca", "Şub", "Mar", "Nis", "May", "Haz", "Tem", "Ağu", "Eyl", "Eki", "Kas", "Ara")
private val dayNames = listOf("Pazartesi", "Salı", "Çarşamba", "Perşembe", "Cuma", "Cumartesi", "Pazar")

val Month.turkishName: String get() = monthNames[ordinal]
val Month.turkishShortName: String get() = monthShortNames[ordinal]
private val DayOfWeek.turkishName: String get() = dayNames[ordinal]

private fun Int.twoDigits() = toString().padStart(2, '0')

/** Binlik ayracı nokta olan tam sayı: 1234567 → "1.234.567". */
private fun Long.grouped(): String = abs(this).toString().reversed().chunked(3).joinToString(".").reversed()

/** "₺1.234,56", "-₺250,00". */
fun Money.formatMoney(): String {
    val a = abs(minor)
    return (if (minor < 0) "-" else "") + "₺" + (a / 100).grouped() + "," + (a % 100).toInt().twoDigits()
}

/** En fazla bir ondalık, gereksiz sıfır yazılmaz: 3.5 → "3,5", 330.0 → "330". */
fun Double.formatOneDecimal(): String {
    val tenths = (this * 10).roundToLong()
    val whole = (tenths / 10).grouped()
    val fraction = abs(tenths % 10)
    return (if (tenths < 0) "-" else "") + whole + (if (fraction != 0L) ",$fraction" else "")
}

/** "4 Kas 21:08" */
fun LocalDateTime.formatDateTime(): String = "$day ${month.turkishShortName} ${formatTime()}"

/** "21:08" */
fun LocalDateTime.formatTime(): String = "${hour.twoDigits()}:${minute.twoDigits()}"

/** "4 Kasım 2026, Çarşamba" */
fun LocalDate.formatDay(): String = "$day ${month.turkishName} $year, ${dayOfWeek.turkishName}"

/** "Kasım 2026" */
fun YearMonth.formatMonth(): String = "${month.turkishName} $year"

val Category.label: String
    get() = when (this) {
        Category.MARKET -> "Market"
        Category.FUEL -> "Akaryakıt"
        Category.SUBSCRIPTION -> "Abonelik"
        Category.SHOPPING -> "Alışveriş"
        Category.TRANSFER -> "Transfer"
        Category.OTHER -> "Diğer"
    }

val Category.icon: DrawableResource
    get() = when (this) {
        Category.MARKET -> Res.drawable.ic_cat_market
        Category.FUEL -> Res.drawable.ic_cat_fuel
        Category.SUBSCRIPTION -> Res.drawable.ic_cat_subscription
        Category.SHOPPING -> Res.drawable.ic_cat_shopping
        Category.TRANSFER -> Res.drawable.ic_cat_transfer
        Category.OTHER -> Res.drawable.ic_cat_other
    }

val Kind.label: String
    get() = when (this) {
        Kind.PURCHASE -> "Harcama"
        Kind.TRANSFER_OUT -> "Giden transfer"
        Kind.TRANSFER_IN -> "Gelen transfer"
        Kind.CARD_PAYMENT -> "Kart ödemesi"
        Kind.REFUND -> "İade"
        Kind.INTERNAL_TRANSFER -> "Hesaplar arası"
    }

fun accountLabel(accountId: String): String = when (accountId) {
    BankTemplates.QNB -> "QNB"
    BankTemplates.GETIR_FINANS -> "Getir Finans"
    BankTemplates.QNB_CREDIT_CARD -> "QNB Kredi Kartı"
    else -> accountId
}
