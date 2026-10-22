package io.github.lkhanl.moneytracker.core

import java.time.LocalDateTime

/** Android NotificationListenerService'ten gelen ham bildirim. */
data class BankNotification(
    val packageName: String,
    val title: String,
    val text: String,
    val postedAt: LocalDateTime,
)

/**
 * Bir bankanın bir bildirim tipini tanıyan şablon.
 * [pattern] içinde `amount` adlı grup zorunlu, `counterparty` isteğe bağlıdır.
 */
data class NotificationTemplate(
    val accountId: String,
    val titles: Set<String>,
    val pattern: Regex,
    val kind: Kind,
)

class NotificationParser(private val templates: List<NotificationTemplate> = BankTemplates.notifications) {

    fun parse(n: BankNotification): Transaction? {
        val title = n.title.trim().lowercase()
        for (t in templates) {
            if (title !in t.titles) continue
            val m = t.pattern.find(n.text) ?: continue
            val amount = Amounts.parse(m.groups["amount"]!!.value)
            val signed = if (t.kind == Kind.TRANSFER_IN || t.kind == Kind.REFUND) amount else amount.negate()
            val counterparty = runCatching { m.groups["counterparty"]?.value?.trim() }.getOrNull()
            return Transaction(
                id = Ids.of(t.accountId, n.postedAt, n.text),
                accountId = t.accountId,
                occurredAt = n.postedAt,
                amount = signed,
                currency = "TRY",
                description = n.text.trim(),
                kind = t.kind,
                source = Source.NOTIFICATION,
                counterparty = counterparty,
            )
        }
        return null
    }
}
