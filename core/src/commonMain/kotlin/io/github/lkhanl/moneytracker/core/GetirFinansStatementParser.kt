package io.github.lkhanl.moneytracker.core

/**
 * GetirFinans "Hesap Cüzdanı" PDF'inden çıkarılmış metni okur.
 *
 * Her hareket `Tarih Valör Açıklama Tutar Bakiye` sırasındadır; açıklama birkaç satıra, hatta bir
 * sonraki sayfaya taşabilir. PDF'ten metin çıkaran kütüphaneler satır kırılımlarını farklı yaptığı
 * için metin satır satır değil, tek bir sözcük dizisi olarak ele alınır ve iki tarihin yan yana
 * geldiği yerlerden hareketlere bölünür.
 *
 * Tutar sütunu işaretsizdir; yön önce açıklamadan ("GİDEN", "POS SATIS"…), anlaşılamazsa bir önceki
 * bakiyeyle karşılaştırılarak bulunur.
 */
class GetirFinansStatementParser(private val accountId: String = BankTemplates.GETIR_FINANS) {

    /** Metin bu bankanın hesap cüzdanına mı ait? */
    fun matches(text: String): Boolean {
        val flat = text.flatten()
        return flat.contains("GetirFinans", ignoreCase = true) && flat.contains("Cüzdan Bilgileri", ignoreCase = true)
    }

    fun parse(text: String): List<Transaction> {
        val flat = text.flatten()
        // Müşteri adı "Cüzdan Bilgileri" bölümünde; kütüphaneye göre etiketlerden önce ya da sonra gelebildiği
        // için ad ayrıca çıkarılmaz, karşı tarafın adı bu bölümde geçiyor mu diye bakılır.
        val tableStart = HEADER.find(flat)?.range?.first ?: 0
        val customerInfo = flat.substring(0, tableStart).upperTr()
        val isOwner = { name: String? -> name != null && name.length >= 5 && name.upperTr() in customerInfo }
        // Hareketler tablo başlığından sonra; öncesindeki "Hesap Açılış Tarihi" ilk hareketin tarihiyle karışmasın.
        val body = flat.substring(tableStart)
            .let { s -> FOOTER.find(s)?.let { s.substring(0, it.range.first) } ?: s }
            .replace(HEADER, " ")
            .flatten()

        val starts = RECORD_START.findAll(body).toList()
        var previousBalance: Money? = null
        val seen = mutableMapOf<String, Int>()
        return starts.mapIndexedNotNull { i, start ->
            val end = starts.getOrNull(i + 1)?.range?.first ?: body.length
            val rest = body.substring(start.range.last + 1, end)
            // Faiz satırında "FAİZ ORANI: 40.00 237.81 83,614.06" gibi üç sayı yan yana gelebilir; son ikili tutar ve bakiyedir.
            val pair = AMOUNT_PAIR.findAll(rest).lastOrNull() ?: return@mapIndexedNotNull null
            val amount = Amounts.parse(pair.groupValues[1])
            val balance = Amounts.parse(pair.groupValues[2])
            // Eşleşme ileriye bakış olduğu için sıfır genişlikte; sınırlar gruplardan alınır.
            val pairEnd = pair.groups[2]!!.range.last + 1
            val raw = (rest.substring(0, pair.range.first) + " " + rest.substring(pairEnd)).flatten()

            val entry = classify(raw, isOwner)
            val incoming = entry.incoming ?: previousBalance?.let { (it + amount).compareTo(balance) == 0 } ?: false
            previousBalance = balance

            val date = start.groupValues[1]
            // Açıklama metni kimliğe katılmaz: sayfa sonuna denk gelen bir hareket farklı ekstrelerde farklı
            // bölünür. Bakiye her hareketten sonra değiştiği için aynı gün aynı tutarlı işlemleri zaten ayırır.
            val key = "$date|${start.groupValues[2]}|$amount|$balance"
            val occurrence = seen.occurrence(key)
            Transaction(
                id = Ids.of(accountId, key, occurrence),
                accountId = accountId,
                occurredAt = parseDayMonthYear(date, '.').atStartOfDay(),
                amount = if (incoming) amount else amount.negate(),
                currency = "TRY",
                description = entry.description,
                kind = entry.kind ?: if (incoming) Kind.TRANSFER_IN else Kind.PURCHASE,
                source = Source.STATEMENT,
                counterparty = entry.counterparty,
            )
        }
    }

    private class Entry(
        val description: String,
        val kind: Kind?,
        /** Açıklamadan anlaşılan yön; null ise bakiyeden bulunur. */
        val incoming: Boolean?,
        val counterparty: String? = null,
    )

    private fun classify(raw: String, isOwner: (String?) -> Boolean): Entry {
        val upper = raw.upperTr()
        return when {
            "POS SATIS IADE" in upper || "POS SATIŞ İADE" in upper ->
                Entry(merchant(raw)?.let { "İade: $it" } ?: "POS iadesi", Kind.REFUND, incoming = true)

            "POS SATIS" in upper || "POS SATIŞ" in upper ->
                Entry(merchant(raw) ?: raw, Kind.PURCHASE, incoming = false)

            "NAKIT IADE" in upper || "NAKİT İADE" in upper ->
                Entry("Nakit iade kazancı", Kind.REFUND, incoming = true)

            "FAİZ ÖDEMESİ" in upper || "FAIZ ODEMESI" in upper ->
                Entry("Vadeli hesap faizi", Kind.TRANSFER_IN, incoming = true)

            "STOPAJ" in upper -> Entry("Faiz stopajı", Kind.PURCHASE, incoming = false)

            "FATURA ÖDEMESİ" in upper || "FATURA ODEMESI" in upper -> {
                val institution = INSTITUTION.find(raw)?.groupValues?.get(1)?.trim()
                Entry(institution?.let { "$it faturası" } ?: "Fatura ödemesi", Kind.PURCHASE, incoming = false)
            }

            upper.startsWith("GİDEN") || upper.startsWith("GIDEN") -> {
                val name = RECEIVER.find(raw)?.groupValues?.get(1)?.trim()
                val bank = BANK.find(raw)?.groupValues?.get(1)?.trim()
                val own = isOwner(name)
                Entry(
                    description = listOfNotNull(transferType(upper), bank).joinToString(" · "),
                    kind = if (own) Kind.INTERNAL_TRANSFER else Kind.TRANSFER_OUT,
                    incoming = false,
                    counterparty = name,
                )
            }

            upper.startsWith("GELEN") -> {
                val name = SENDER.find(raw)?.groupValues?.get(1)?.trim()
                val bank = BANK.find(raw)?.groupValues?.get(1)?.trim()
                val own = isOwner(name)
                Entry(
                    description = listOfNotNull(transferType(upper), bank).joinToString(" · "),
                    kind = if (own) Kind.INTERNAL_TRANSFER else Kind.TRANSFER_IN,
                    incoming = true,
                    counterparty = name,
                )
            }

            else -> Entry(raw, kind = null, incoming = null)
        }
    }

    private fun transferType(upper: String): String = when {
        "FAST" in upper -> "FAST"
        "EFT" in upper -> "EFT"
        "HAVALE" in upper -> "Havale"
        else -> "Transfer"
    }

    /** "POS İŞYERİ ADI: TRENDYOL YEMEK ISTANBUL TR İŞLEM TARİHİ: …" → "TRENDYOL YEMEK ISTANBUL". */
    private fun merchant(raw: String): String? =
        MERCHANT.find(raw)?.groupValues?.get(1)?.replace(Regex("""\s+TR$"""), "")?.trim()?.takeIf { it.isNotEmpty() }

    private companion object {
        val HEADER = Regex("""Tarih\s+Valör\s+Tarihi\s+Açıklama\s+Tutar\s+Bakiye""")
        val FOOTER = Regex("""DEĞERLİ MÜŞTERİMİZ""", RegexOption.IGNORE_CASE)
        val RECORD_START = Regex("""(?<![\d.])(\d{2}\.\d{2}\.\d{4}) (\d{2}\.\d{2}\.\d{4}) """)

        private const val AMOUNT = """\d{1,3}(?:,\d{3})*\.\d{2}"""
        /** Örtüşen eşleşmeleri de bulmak için ileriye bakış içinde yakalar. */
        val AMOUNT_PAIR = Regex("""(?<![\d.,])(?=($AMOUNT) ($AMOUNT)(?![\d.,]))""")

        val MERCHANT = Regex("""İŞYERİ ADI:\s*(.+?)\s*İŞLEM TARİHİ""")
        val RECEIVER = Regex("""ALICI\s*:\s*(.+?)\s+-\s""")
        val SENDER = Regex("""G[ÖO]NDEREN\s*:\s*(.+?)\s+-\s""")
        val BANK = Regex("""BANKA\s*ADI\s*:\s*(.+?)\s+-\s""")
        val INSTITUTION = Regex("""KURUM\s+ADI\s*:\s*(.+?)\s+TESİSAT""")

        fun String.flatten() = replace(Regex("""\s+"""), " ").trim()
        fun String.upperTr() = uppercaseTr()
    }
}
