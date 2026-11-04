package io.github.lkhanl.moneytracker.data

import io.github.lkhanl.moneytracker.core.BankTemplates
import io.github.lkhanl.moneytracker.core.Kind
import io.github.lkhanl.moneytracker.core.Money
import io.github.lkhanl.moneytracker.core.Source
import io.github.lkhanl.moneytracker.core.Transaction
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import kotlinx.datetime.atTime
import kotlinx.datetime.onDay
import kotlinx.datetime.plusMonth
import kotlinx.datetime.yearMonth
import kotlin.math.roundToLong
import kotlin.random.Random

/**
 * Geliştirme sırasında ekranları doldurmak için aylara yayılmış örnek işlemler üretir.
 * Her ay için tohum sabit olduğundan çıktı her çalıştırmada aynıdır; kimlikler de sabit
 * olduğu için tekrar eklendiğinde veritabanı tarafından yok sayılır.
 */
object DemoData {

    val FROM: YearMonth = YearMonth(2025, 12)
    val TO: YearMonth = YearMonth(2026, 11)

    private class Template(
        val accountId: String,
        val kind: Kind,
        val description: String,
        val min: Int,
        val max: Int,
        val counterparty: String? = null,
    )

    private val incomes = listOf(
        Template(BankTemplates.GETIR_FINANS, Kind.TRANSFER_IN, "Maaş", 42_000, 48_000, "ACME YAZILIM A.Ş."),
        Template(BankTemplates.GETIR_FINANS, Kind.TRANSFER_IN, "Gelen transfer", 500, 3_000, "AHMET YILMAZ"),
    )

    private val expenses = listOf(
        Template(BankTemplates.QNB_CREDIT_CARD, Kind.PURCHASE, "MIGROS TICARET A.S.", 300, 2_500),
        Template(BankTemplates.QNB_CREDIT_CARD, Kind.PURCHASE, "A101 YENI MAGAZACILIK", 150, 900),
        Template(BankTemplates.QNB_CREDIT_CARD, Kind.PURCHASE, "BIM BIRLESIK MAGAZALAR", 100, 700),
        Template(BankTemplates.QNB_CREDIT_CARD, Kind.PURCHASE, "OPET PETROLCULUK", 1_000, 2_800),
        Template(BankTemplates.QNB_CREDIT_CARD, Kind.PURCHASE, "SHELL PETROL", 900, 2_500),
        Template(BankTemplates.QNB_CREDIT_CARD, Kind.PURCHASE, "NETFLIX.COM", 230, 230),
        Template(BankTemplates.QNB_CREDIT_CARD, Kind.PURCHASE, "SPOTIFY", 100, 100),
        Template(BankTemplates.QNB_CREDIT_CARD, Kind.PURCHASE, "IYZICO/TRENDYOL", 250, 4_000),
        Template(BankTemplates.QNB_CREDIT_CARD, Kind.PURCHASE, "HEPSIBURADA", 300, 5_000),
        Template(BankTemplates.QNB_CREDIT_CARD, Kind.PURCHASE, "KAHVE DUNYASI", 80, 350),
        Template(BankTemplates.QNB, Kind.TRANSFER_OUT, "Giden transfer", 1_000, 15_000, "EV SAHIBI MEHMET KAYA"),
        Template(BankTemplates.QNB, Kind.TRANSFER_OUT, "Giden transfer", 200, 2_000, "AYSE DEMIR"),
    )

    /** [today] sonrasına düşen günler üretilmez; içinde bulunulan ay kısmen dolar. */
    fun generate(today: LocalDate = today()): List<Transaction> {
        val result = mutableListOf<Transaction>()
        var month = FROM
        while (month <= TO) {
            result += forMonth(month, today)
            month = month.plusMonth()
        }
        return result
    }

    private fun forMonth(month: YearMonth, today: LocalDate): List<Transaction> {
        val lastDay = when {
            month > today.yearMonth -> return emptyList()
            month == today.yearMonth -> today.day
            else -> month.numberOfDays
        }
        val random = Random(month.year * 100 + month.month.ordinal + 1)

        // Her ay bir maaş, arada bir ek gelir ve 2-4 harcama.
        val picks = buildList {
            add(incomes[0])
            if (random.nextInt(3) == 0) add(incomes[1])
            addAll(expenses.shuffled(random).take(random.nextInt(2, 5)))
        }

        return picks.mapIndexed { i, t ->
            val day = random.nextInt(1, lastDay + 1)
            // Farklı saatler, iç transfer eşleştirmesine (2 dk) takılmamayı garanti eder.
            val time = month.onDay(day).atTime(8 + i, random.nextInt(60))
            val value = if (t.min == t.max) t.min.toDouble() else random.nextDouble(t.min.toDouble(), t.max.toDouble())
            val amount = Money((value * 100).roundToLong())
            Transaction(
                id = "demo-$month-$i",
                accountId = t.accountId,
                occurredAt = time,
                amount = if (t.kind == Kind.TRANSFER_IN) amount else amount.negate(),
                currency = "TRY",
                description = t.description,
                kind = t.kind,
                source = Source.STATEMENT,
                counterparty = t.counterparty,
            )
        }
    }
}
