package io.github.lkhanl.moneytracker.core

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration.Companion.hours
import kotlin.test.Test
import kotlin.test.assertEquals

class TransferMatcherTest {
    private val at = LocalDateTime(2026, 10, 21, 20, 50)

    private fun transfer(accountId: String, kind: Kind, amount: String, time: LocalDateTime) = Transaction(
        id = Ids.of(accountId, time, amount),
        accountId = accountId,
        occurredAt = time,
        amount = Money.parse(amount).let { if (kind == Kind.TRANSFER_OUT) it.negate() else it },
        currency = "TRY",
        description = "transfer",
        kind = kind,
        source = Source.STATEMENT,
    )

    private fun qnbOut(amount: String, time: LocalDateTime) = transfer(BankTemplates.QNB, Kind.TRANSFER_OUT, amount, time)

    private fun getirIn(amount: String, time: LocalDateTime) =
        transfer(BankTemplates.GETIR_FINANS, Kind.TRANSFER_IN, amount, time)

    @Test
    fun `kendi hesaplar arası transfer iç transfer olur`() {
        val result = TransferMatcher().markInternal(listOf(qnbOut("100.00", at), getirIn("100.00", at)))
        assertEquals(listOf(Kind.INTERNAL_TRANSFER, Kind.INTERNAL_TRANSFER), result.map { it.kind })
    }

    @Test
    fun `tutar ya da zaman tutmazsa dokunmaz`() {
        val result = TransferMatcher().markInternal(
            listOf(qnbOut("100.00", at), getirIn("99.00", at), getirIn("100.00", (at.toInstant(TimeZone.UTC) + 1.hours).toLocalDateTime(TimeZone.UTC)))
        )
        assertEquals(listOf(Kind.TRANSFER_OUT, Kind.TRANSFER_IN, Kind.TRANSFER_IN), result.map { it.kind })
    }
}
