package io.github.lkhanl.moneytracker.core

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GetirFinansStatementParserTest {
    // Gerçek hesap cüzdanının yapısı; ad, IBAN ve kart numaraları değiştirildi.
    private val sample = """
        04.11.2026
        Cüzdan Bilgileri
        Müşteri Adı Soyadı / Unvan
        AYŞE YILMAZ
        Müşteri No
        1234567
        Hesap Açılış Tarihi
        30.09.2024
        Şube Adı
        GetirFinans
        Hesap bakiyeniz 54,709.62 TRY ‘dir.
        Tarih Valör Tarihi Açıklama Tutar Bakiye
        01.11.2026 01.11.2026
        YURT ICI POS SATIS KART NO:
        111111******2222 POS İŞYERİ ADI:
        IYZICO/TIKLAGELSIN.COM ISTANBUL
        TR İŞLEM TARİHİ: 20261101
        PROVİZYON NO: 443217 RRN:
        630515830082
        420.00 13,243.17
        01.11.2026 01.11.2026
        GİDEN FAST. ALICI: Ayşe Yılmaz -
        BANKAADI:T.GARANTİ BANKASI A.Ş. -
        KAYNAK :DİĞER - AÇIKLAMA :
        getirfinans ile gönderildi - ALICI
        IBAN:TR000000000000000000000001
        2,200.00 11,043.17
        02.11.2026 02.11.2026 VADELI HESAP FAİZ ÖDEMESİ
        81902000 FAİZ ORANI: 40.00 237.81 83,614.06
        02.11.2026 02.11.2026 Stopaj Gideri 41.62 83,572.44
        02.11.2026 02.11.2026
        KURUM FATURA ÖDEMESİ KURUM
        ADI:Turkcell TESİSAT NO:5000000000
        FATURA NO:7000000000
        626.90 82,945.54
        03.11.2026 03.11.2026 KART NO: 111111******2222 RRN:
        630516915914 PROVİZYON NO: 420.00 83,365.54
        Tarih Valör Tarihi Açıklama Tutar Bakiye
        370718 İŞLEM TARİHİ: 20261101 YURT
        ICI POS SATIS IADE ISLEMI
        03.11.2026 03.11.2026
        GİDEN FAST. ALICI: Mehmet Kaya -
        BANKAADI:T.VAKIFLAR BANKASI T.A.O.
        - KAYNAK :DİĞER - AÇIKLAMA : - ALICI
        IBAN:TR000000000000000000000002
        3,000.00 80,365.54
        04.11.2026 05.11.2026 nakit iade kazancın KART NO:
        111111******2222 10.43 80,375.97
        04.11.2026 04.11.2026
        GİDEN FAST. ALICI: Ayşe Yılmaz -
        BANKAADI:QNB BANK A.Ş. - KAYNAK
        :DİĞER - AÇIKLAMA : - ALICI
        IBAN:TR000000000000000000000003
        1.00 80,374.97
        04.11.2026 04.11.2026
        GİDEN FAST. ALICI: Ayşe Yılmaz -
        BANKAADI:QNB BANK A.Ş. - KAYNAK
        :DİĞER - AÇIKLAMA : - ALICI
        IBAN:TR000000000000000000000003
        1.00 80,373.97
        04.11.2026 04.11.2026 BILINMEYEN ISLEM 100.00 80,473.97
        DEĞERLİ MÜŞTERİMİZ, LÜTFEN AŞAĞIDAKİ HUSUSLARI DİKKATLE OKUYUNUZ.
        1. Hesap cüzdanı, 12.34 56.78 şubesinin resmi mührünü taşır.
    """.trimIndent()

    private val parser = GetirFinansStatementParser()
    private val parsed = parser.parse(sample)

    @Test
    fun `hesap cüzdanını tanır`() {
        assertTrue(parser.matches(sample))
        assertFalse(parser.matches("21/10/2026 tarihli kartınıza ait Dönem İçi Hareketler"))
    }

    @Test
    fun `tüm hareketleri okur - başlık ve dipnotu atlar`() {
        assertEquals(11, parsed.size)
        assertTrue(parsed.all { it.accountId == BankTemplates.GETIR_FINANS && it.source == Source.STATEMENT })
        assertEquals(LocalDate(2026, 11, 1).atStartOfDay(), parsed[0].occurredAt)
    }

    @Test
    fun `kart harcaması işyeri adıyla eksi tutar olur`() {
        val t = parsed[0]
        assertEquals(Kind.PURCHASE, t.kind)
        assertEquals(Money.parse("-420.00"), t.amount)
        assertEquals("IYZICO/TIKLAGELSIN.COM ISTANBUL", t.description)
    }

    @Test
    fun `kendi adına giden transfer hesaplar arası sayılır`() {
        val t = parsed[1]
        assertEquals(Kind.INTERNAL_TRANSFER, t.kind)
        assertEquals(Money.parse("-2200.00"), t.amount)
        assertEquals("Ayşe Yılmaz", t.counterparty)
        assertEquals("FAST · T.GARANTİ BANKASI A.Ş.", t.description)
    }

    @Test
    fun `başkasına giden transfer`() {
        val t = parsed[6]
        assertEquals(Kind.TRANSFER_OUT, t.kind)
        assertEquals(Money.parse("-3000.00"), t.amount)
        assertEquals("Mehmet Kaya", t.counterparty)
    }

    @Test
    fun `faiz satırında oran tutarla karışmaz`() {
        assertEquals(Money.parse("237.81"), parsed[2].amount)
        assertEquals("Vadeli hesap faizi", parsed[2].description)
        assertEquals(Money.parse("-41.62"), parsed[3].amount)
    }

    @Test
    fun `fatura kurum adıyla`() {
        assertEquals("Turkcell faturası", parsed[4].description)
        assertEquals(Money.parse("-626.90"), parsed[4].amount)
    }

    @Test
    fun `sayfaya taşan iade ve nakit iade giriş olur`() {
        assertEquals(Kind.REFUND, parsed[5].kind)
        assertEquals(Money.parse("420.00"), parsed[5].amount)
        assertEquals(Kind.REFUND, parsed[7].kind)
        assertEquals(Money.parse("10.43"), parsed[7].amount)
    }

    @Test
    fun `aynı gün aynı tutarlı iki transfer ayrı kimlik alır`() {
        assertEquals(Money.parse("-1.00"), parsed[8].amount)
        assertEquals(Money.parse("-1.00"), parsed[9].amount)
        assertTrue(parsed[8].id != parsed[9].id)
        assertEquals(parsed.size, parsed.map { it.id }.distinct().size)
    }

    @Test
    fun `tanınmayan işlemin yönü bakiyeden bulunur`() {
        assertEquals(Money.parse("100.00"), parsed[10].amount)
        assertEquals(Kind.TRANSFER_IN, parsed[10].kind)
    }

    @Test
    fun `kendinden gelen transfer hesaplar arası - başkasından gelen gelir sayılır`() {
        val text = """
            Cüzdan Bilgileri
            Müşteri Adı Soyadı / Unvan Müşteri No Hesap Açılış Tarihi
            AYŞE YILMAZ 1234567 30.09.2024
            Tarih Valör Tarihi Açıklama Tutar Bakiye
            26.10.2026 27.10.2026
            GELEN FAST. - GONDEREN :AYŞE
            YILMAZ - BANKAADI:T.GARANTİ
            BANKASI A.Ş. - FAST SORGU
            NO:9661000064- ALICI
            IBAN:TR000000000000000000000001
            1,000.00 13,379.98
            27.10.2026 27.10.2026
            GELEN FAST. - GONDEREN :ACME YAZILIM A.Ş. - BANKAADI:QNB BANK A.Ş. -
            FAST SORGU NO:1728695485- ALICI IBAN:TR000000000000000000000001
            50,000.00 63,379.98
        """.trimIndent()
        val (own, salary) = parser.parse(text)
        assertEquals(Kind.INTERNAL_TRANSFER, own.kind)
        assertEquals(Money.parse("1000.00"), own.amount)
        assertEquals("AYŞE YILMAZ", own.counterparty)
        assertEquals("FAST · T.GARANTİ BANKASI A.Ş.", own.description)
        assertEquals(Kind.TRANSFER_IN, salary.kind)
        assertEquals("ACME YAZILIM A.Ş.", salary.counterparty)
    }

    @Test
    fun `satır kırılımı farklı olsa da aynı sonucu verir`() {
        val oneLine = parser.parse(sample.replace("\n", " "))
        assertEquals(parsed, oneLine)
    }
}
