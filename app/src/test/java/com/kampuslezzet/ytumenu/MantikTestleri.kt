package com.kampuslezzet.ytumenu

import com.kampuslezzet.ytumenu.core.date.GosterimPlanlayici
import com.kampuslezzet.ytumenu.core.date.OgunSaatleri
import com.kampuslezzet.ytumenu.core.date.TatilTakvimi
import com.kampuslezzet.ytumenu.core.date.haftaSonuMu
import com.kampuslezzet.ytumenu.data.remote.sks.SksParser
import com.kampuslezzet.ytumenu.domain.model.Kategori
import com.kampuslezzet.ytumenu.domain.model.OgunMenusu
import com.kampuslezzet.ytumenu.domain.model.YemekCozucu
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class MantikTestleri {

    @Test
    fun `tatil takvimi sabit tatilleri dogru taniyor`() {
        val cumhuriyet = LocalDate.of(2026, 10, 29)
        assertEquals("Cumhuriyet Bayramı", TatilTakvimi.tatilAdi(cumhuriyet))
        assertTrue(TatilTakvimi.kapaliMi(cumhuriyet))

        val yilbasi = LocalDate.of(2027, 1, 1)
        assertEquals("Yılbaşı", TatilTakvimi.tatilAdi(yilbasi))
    }

    @Test
    fun `tatil takvimi dini bayramlari dogru taniyor`() {
        // 2026 Ramazan Bayramı 1. Gün (20 Mart 2026)
        val ramazan = LocalDate.of(2026, 3, 20)
        assertEquals("Ramazan Bayramı", TatilTakvimi.tatilAdi(ramazan))
        assertTrue(TatilTakvimi.kapaliMi(ramazan))
    }

    @Test
    fun `hafta sonu kontrolu dogru calisiyor`() {
        val cumartesi = LocalDate.of(2026, 10, 24)
        val pazar = LocalDate.of(2026, 10, 25)
        val pazartesi = LocalDate.of(2026, 10, 26)

        assertTrue(cumartesi.haftaSonuMu())
        assertTrue(pazar.haftaSonuMu())
        assertFalse(pazartesi.haftaSonuMu())
    }

    @Test
    fun `ogun saatlerine gore gosterim planlayici gecisleri dogru yapiyor`() {
        val tarih = LocalDate.of(2026, 10, 28) // Çarşamba

        // 10:00 -> Öğle + Akşam görünmeli, bugün
        val sabah = GosterimPlanlayici.planla(tarih.atTime(10, 0))
        assertEquals(tarih, sabah.gun)
        assertTrue(sabah.ogleGoster)
        assertTrue(sabah.aksamGoster)
        assertFalse(sabah.servisAcik)

        // 12:00 -> Öğle servisi açık
        val ogleVakti = GosterimPlanlayici.planla(tarih.atTime(12, 0))
        assertTrue(ogleVakti.servisAcik)
        assertTrue(ogleVakti.ogleGoster)

        // 15:00 -> Sadece akşam görünmeli
        val ikindi = GosterimPlanlayici.planla(tarih.atTime(15, 0))
        assertEquals(tarih, ikindi.gun)
        assertFalse(ikindi.ogleGoster)
        assertTrue(ikindi.aksamGoster)

        // 20:00 -> Akşam servisi bitti, yarının menüsü görünmeli
        val gece = GosterimPlanlayici.planla(tarih.atTime(20, 0))
        assertEquals(tarih.plusDays(1), gece.gun)
        assertTrue(gece.ogleGoster)
        assertTrue(gece.aksamGoster)
    }

    @Test
    fun `yemek cozucu kalori ve kategorileri dogru ayikliyor`() {
        val satirlar = listOf(
            "MERCİMEK ÇORBA (120 kcal)",
            "FIRIN TAVUK (320 kcal)",
            "PİRİNÇ PİLAVI (230 kcal)",
            "AYRAN (90 kcal)",
            "SÜTLAÇ (210 kcal)"
        )
        val sonuclar = YemekCozucu.coz(satirlar)
        assertEquals(5, sonuclar.size)

        assertEquals("Mercimek Çorba", sonuclar[0].ad)
        assertEquals(120, sonuclar[0].kalori)
        assertEquals(Kategori.CORBA, sonuclar[0].kategori)

        assertEquals("Fırın Tavuk", sonuclar[1].ad)
        assertEquals(Kategori.ANA_YEMEK, sonuclar[1].kategori)

        assertEquals("Pirinç Pilavı", sonuclar[2].ad)
        assertEquals(Kategori.YAN_YEMEK, sonuclar[2].kategori)

        assertEquals("Ayran", sonuclar[3].ad)
        assertEquals(Kategori.ICECEK, sonuclar[3].kategori)

        assertEquals("Sütlaç", sonuclar[4].ad)
        assertEquals(Kategori.TATLI_MEYVE, sonuclar[4].kategori)
    }

    @Test
    fun `vejetaryen farki dogru hesaplaniyor`() {
        val normal = YemekCozucu.coz(listOf("ÇORBA (100 kcal)", "ETLİ KÖFTE (300 kcal)", "PİLAV (200 kcal)"))
        val vej = YemekCozucu.coz(listOf("ÇORBA (100 kcal)", "SEBZE KÖFTE (220 kcal)", "PİLAV (200 kcal)"))

        val ogun = OgunMenusu(normal, vej)
        val fark = ogun.vejetaryenFarki

        assertEquals(1, fark.size)
        assertEquals("Sebze Köfte", fark[0].ad)
    }

    @Test
    fun `sks parser ornek json verisini dogru ayristiriyor`() {
        val ornekJson = """
            [
              {
                "date": "28-10-2026",
                "lunch_menu": "<p>TARHANA ÇORBA (120 kcal)</p><p>ÇITIR TAVUK (330 kcal)</p>",
                "lunch2_menu": "<p>TARHANA ÇORBA (120 kcal)</p><p>SEBZE KÖFTE (200 kcal)</p>",
                "dinner_menu": "<p>EZOGELİN ÇORBA (110 kcal)</p>",
                "dinner2_menu": ""
              }
            ]
        """.trimIndent()

        val parsed = SksParser.coz(ornekJson)
        assertEquals(1, parsed.size)
        assertEquals(LocalDate.of(2026, 10, 28), parsed[0].tarih)
        assertEquals(2, parsed[0].ogleNormal.size)
        assertEquals(1, parsed[0].aksamNormal.size)
    }
}
