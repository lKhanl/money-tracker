package io.github.lkhanl.moneytracker.core

import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

class TransferMatcherTest {
    private val parser = NotificationParser()
    private val at = LocalDateTime.of(2026, 10, 21, 20, 50)

    private fun qnbOut(amount: String, time: LocalDateTime) = parser.parse(
        BankNotification("q", "QNB", "QNB hesabınızdan A*** B*** isimli alıcıya $amount TL gönderildi.", time)
    )!!

    private fun getirIn(amount: String, time: LocalDateTime) = parser.parse(
        BankNotification("g", "getirfinans", "A** B** tarafından gönderilen $amount TL hesabına geldi", time)
    )!!

    @Test
    fun `kendi hesaplar arası transfer iç transfer olur`() {
        val result = TransferMatcher().markInternal(listOf(qnbOut("100.00", at), getirIn("100,00", at)))
        assertEquals(listOf(Kind.INTERNAL_TRANSFER, Kind.INTERNAL_TRANSFER), result.map { it.kind })
    }

    @Test
    fun `tutar ya da zaman tutmazsa dokunmaz`() {
        val result = TransferMatcher().markInternal(
            listOf(qnbOut("100.00", at), getirIn("99,00", at), getirIn("100,00", at.plusHours(1)))
        )
        assertEquals(listOf(Kind.TRANSFER_OUT, Kind.TRANSFER_IN, Kind.TRANSFER_IN), result.map { it.kind })
    }
}
