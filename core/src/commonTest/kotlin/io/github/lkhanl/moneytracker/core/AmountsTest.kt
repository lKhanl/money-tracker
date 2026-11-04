package io.github.lkhanl.moneytracker.core

import kotlin.test.Test
import kotlin.test.assertEquals

class AmountsTest {
    @Test
    fun `farklı banka yazımlarını okur`() {
        assertEquals(Money.parse("100.00"), Amounts.parse("100.00"))
        assertEquals(Money.parse("100.00"), Amounts.parse("100,00"))
        assertEquals(Money.parse("1234.56"), Amounts.parse("1.234,56"))
        assertEquals(Money.parse("1234.56"), Amounts.parse("1,234.56"))
        assertEquals(Money.parse("1500.00"), Amounts.parse("1500,0"))
        assertEquals(Money.parse("-11000.94"), Amounts.parse("-11000,94"))
        assertEquals(Money.parse("1500.00"), Amounts.parse("1.500"))
    }

    @Test
    fun `tutar metni BigDecimal ile aynı biçimde yazılır`() {
        // İşlem kimlikleri bu metinden üretildiği için eski sürümle aynı kalmalı.
        assertEquals("420.00", Amounts.parse("420,00").toString())
        assertEquals("-0.50", Money(-50).toString())
        assertEquals("83614.06", Amounts.parse("83,614.06").toString())
    }
}
