package io.github.lkhanl.moneytracker.core

import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class StatementParserTest {
    private val sample = """
        21/10/2026 tarihli son 4 hanesi 0970 olan kartınıza ait Dönem İçi Hareketler
        İşlem Tarihi Açıklama Tutar Güncel / Toplam
        Taksitler ParaPuan
        10/10/2026 Ödeme - Tesekkür Ederiz - -11000,94 TRY 0/0,0 0,0
        13/10/2026 ELEVENLABS.IO 1410,56 TRY 0/0,0 0,0
        13/10/2026 MONEYPAY/TANKAR ENERJI-SA 1500,0 TRY 0/0,0 0,0
        14/10/2026 KOSEM GIDA 1375,0 TRY 0/0,0 0,0
        16/10/2026 TRENDYOL.COM İSTANBUL TRTR 691,6 TRY 1/1,0 0,0
        17/10/2026 Google Claude by Anth 1059,33 TRY 0/0,0 0,0
        21/10/2026 20261021 IYZICO/amazon.com.tr 1. Taksit 323,0 TRY 1/3,0 0,0
        15/10/2026 ELEVENLABS.IO -1411,93 TRY 0/0,0 0,0
        Bu döküman bilgi amaçlı hazırlanmıştır.
    """.trimIndent()

    private val parsed = StatementParser().parse(sample)

    @Test
    fun `tüm işlem satırlarını okur, başlık ve dipnotu atlar`() {
        assertEquals(8, parsed.size)
    }

    @Test
    fun `harcama, ödeme ve iadeyi ayırır`() {
        assertEquals(Kind.CARD_PAYMENT, parsed[0].kind)
        assertEquals(BigDecimal("11000.94"), parsed[0].amount)
        assertEquals(Kind.PURCHASE, parsed[1].kind)
        assertEquals(BigDecimal("-1410.56"), parsed[1].amount)
        assertEquals(Kind.REFUND, parsed[7].kind)
        assertEquals(BigDecimal("1411.93"), parsed[7].amount)
    }

    @Test
    fun `taksit bilgisini okur`() {
        assertEquals(Installment(1, 3), parsed[6].installment)
        assertEquals(null, parsed[4].installment)
    }

    @Test
    fun `aynı dosya tekrar yüklenince aynı kimlikler üretilir`() {
        assertEquals(parsed.map { it.id }, StatementParser().parse(sample).map { it.id })
    }

    @Test
    fun `aynı gün aynı tutardaki iki harcama ayrı kayıttır`() {
        val twice = "14/10/2026 KOSEM GIDA 50,0 TRY 0/0,0 0,0\n14/10/2026 KOSEM GIDA 50,0 TRY 0/0,0 0,0"
        val ts = StatementParser().parse(twice)
        assertEquals(2, ts.size)
        assertNotEquals(ts[0].id, ts[1].id)
    }
}
