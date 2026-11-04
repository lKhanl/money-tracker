package io.github.lkhanl.moneytracker.core

import kotlinx.datetime.YearMonth
import kotlin.test.Test
import kotlin.test.assertEquals

class CategorizerAndSummaryTest {
    private val categorizer = Categorizer()

    @Test
    fun `işyeri adını temizler`() {
        assertEquals("TANKAR ENERJI-SA", categorizer.merchant("MONEYPAY/TANKAR ENERJI-SA"))
        assertEquals("amazon.com.tr", categorizer.merchant("20261021 IYZICO/amazon.com.tr 1. Taksit"))
        assertEquals("TRENDYOL.COM", categorizer.merchant("TRENDYOL.COM İSTANBUL TRTR"))
    }

    @Test
    fun `aylık özet ödemeyi saymaz - iadeyi düşer`() {
        val statement = """
            10/10/2026 Ödeme - Tesekkür Ederiz - -11000,94 TRY 0/0,0 0,0
            13/10/2026 ELEVENLABS.IO 1410,56 TRY 0/0,0 0,0
            13/10/2026 MONEYPAY/TANKAR ENERJI-SA 1500,0 TRY 0/0,0 0,0
            14/10/2026 KOSEM GIDA 1375,0 TRY 0/0,0 0,0
            17/10/2026 Google Claude by Anth 1059,33 TRY 0/0,0 0,0
            15/10/2026 ELEVENLABS.IO -1411,93 TRY 0/0,0 0,0
        """.trimIndent()
        val ts = StatementParser().parse(statement).map(categorizer::categorize)
        val summary = MonthlySummary.of(YearMonth(2026, 10), ts)

        assertEquals(Money.parse("3932.96"), summary.totalSpent)
        assertEquals(Category.FUEL, summary.topCategory)
        assertEquals(Money.parse("1057.96"), summary.byCategory.toMap()[Category.SUBSCRIPTION])
    }
}
