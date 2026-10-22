package io.github.lkhanl.moneytracker.core

import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals

class AmountsTest {
    @Test
    fun `farklı banka yazımlarını okur`() {
        assertEquals(BigDecimal("100.00"), Amounts.parse("100.00"))
        assertEquals(BigDecimal("100.00"), Amounts.parse("100,00"))
        assertEquals(BigDecimal("1234.56"), Amounts.parse("1.234,56"))
        assertEquals(BigDecimal("1234.56"), Amounts.parse("1,234.56"))
        assertEquals(BigDecimal("1500.00"), Amounts.parse("1500,0"))
        assertEquals(BigDecimal("-11000.94"), Amounts.parse("-11000,94"))
        assertEquals(BigDecimal("1500.00"), Amounts.parse("1.500"))
    }
}
