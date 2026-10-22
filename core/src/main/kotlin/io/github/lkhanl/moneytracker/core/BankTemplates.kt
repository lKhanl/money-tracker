package io.github.lkhanl.moneytracker.core

/**
 * Banka şablonları. Yeni banka eklemek için buraya bir şablon ve
 * test dosyasına gerçek (maskelenmiş) bir örnek eklemek yeterli.
 */
object BankTemplates {
    const val QNB = "qnb"
    const val GETIR_FINANS = "getir-finans"
    const val QNB_CREDIT_CARD = "qnb-kredi-karti"

    private const val AMOUNT = """(?<amount>[\d.,]+)"""

    val notifications = listOf(
        NotificationTemplate(
            accountId = QNB,
            titles = setOf("qnb"),
            pattern = Regex("""hesabınızdan (?<counterparty>.+?) isimli alıcıya $AMOUNT TL gönderildi"""),
            kind = Kind.TRANSFER_OUT,
        ),
        NotificationTemplate(
            accountId = GETIR_FINANS,
            titles = setOf("getirfinans", "getir finans"),
            pattern = Regex("""(?<counterparty>.+?) tarafından gönderilen $AMOUNT TL hesabına geldi"""),
            kind = Kind.TRANSFER_IN,
        ),
    )
}
