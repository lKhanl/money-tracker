package io.github.lkhanl.moneytracker.core

import java.math.BigDecimal
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NotificationParserTest {
    private val parser = NotificationParser()
    private val at = LocalDateTime.of(2026, 10, 21, 20, 50)

    @Test
    fun `QNB giden transfer`() {
        val t = parser.parse(
            BankNotification(
                "qnb", "QNB",
                "QNB hesabınızdan Oğ***** Er***** isimli alıcıya 100.00 TL gönderildi. " +
                    "Mevcut bakiyenizi Hesaplar menüsünden inceleyebilirsiniz.",
                at,
            )
        )!!
        assertEquals(BankTemplates.QNB, t.accountId)
        assertEquals(Kind.TRANSFER_OUT, t.kind)
        assertEquals(BigDecimal("-100.00"), t.amount)
        assertEquals("Oğ***** Er*****", t.counterparty)
    }

    @Test
    fun `Getir Finans gelen transfer`() {
        val t = parser.parse(
            BankNotification("getir", "getirfinans", "Oğ** Er** tarafından gönderilen 100,00 TL hesabına geldi 😊", at)
        )!!
        assertEquals(BankTemplates.GETIR_FINANS, t.accountId)
        assertEquals(Kind.TRANSFER_IN, t.kind)
        assertEquals(BigDecimal("100.00"), t.amount)
    }

    @Test
    fun `tanınmayan bildirim yok sayılır`() {
        assertNull(parser.parse(BankNotification("x", "QNB", "Kampanyamızı kaçırmayın!", at)))
        assertNull(parser.parse(BankNotification("x", "WhatsApp", "100.00 TL gönderildi", at)))
    }
}
