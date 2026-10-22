package io.github.lkhanl.moneytracker.core

import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Kredi kartı "Dönem İçi Hareketler" PDF'inden çıkarılmış metni okur.
 * Örnek satır: `13/10/2026 KOSEM GIDA 1375,0 TRY 0/0,0 0,0`
 * Pozitif tutar harcama, negatif tutar ödeme ya da iadedir.
 */
class StatementParser(private val accountId: String = BankTemplates.QNB_CREDIT_CARD) {

    private val line = Regex(
        """^(\d{2}/\d{2}/\d{4}) (.+?) (-?[\d.]*\d,\d+) TRY (\d+)/(\d+),\d+ [\d.,]+$"""
    )
    private val dateFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    fun parse(text: String): List<Transaction> {
        val seen = mutableMapOf<String, Int>()
        return text.lines().mapNotNull { raw ->
            val m = line.find(raw.trim()) ?: return@mapNotNull null
            val (date, desc, amountRaw, current, total) = m.destructured
            val value = Amounts.parse(amountRaw)
            val description = desc.trim()
            val kind = when {
                value.signum() > 0 -> Kind.PURCHASE
                description.startsWith("Ödeme") -> Kind.CARD_PAYMENT
                else -> Kind.REFUND
            }
            // Aynı gün aynı işyerinde aynı tutar iki kez olabilir; sıra numarası bunları ayırır.
            val key = "$date|$description|$amountRaw"
            val occurrence = seen.merge(key, 1, Int::plus)!!
            Transaction(
                id = Ids.of(accountId, key, occurrence),
                accountId = accountId,
                occurredAt = LocalDate.parse(date, dateFormat).atStartOfDay(),
                amount = value.negate(),
                currency = "TRY",
                description = description,
                kind = kind,
                source = Source.STATEMENT,
                installment = total.toInt().takeIf { it > 1 }?.let { Installment(current.toInt(), it) },
            )
        }
    }
}
