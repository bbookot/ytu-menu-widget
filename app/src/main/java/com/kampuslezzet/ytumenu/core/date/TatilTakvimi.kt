package com.kampuslezzet.ytumenu.core.date

import java.time.LocalDate
import java.time.MonthDay

/**
 * Türkiye resmi tatilleri. Sabit tarihli tatiller her yıl geçerlidir;
 * dini bayramlar Diyanet takvimine göre yıl yıl girilir.
 * Listeyi yeni yıllarla genişletmek için [DINI_BAYRAMLAR] tablosuna ekleme yapın.
 */
object TatilTakvimi {

    private val SABIT = mapOf(
        MonthDay.of(1, 1) to "Yılbaşı",
        MonthDay.of(4, 23) to "Ulusal Egemenlik ve Çocuk Bayramı",
        MonthDay.of(5, 1) to "Emek ve Dayanışma Günü",
        MonthDay.of(5, 19) to "Atatürk'ü Anma, Gençlik ve Spor Bayramı",
        MonthDay.of(7, 15) to "Demokrasi ve Millî Birlik Günü",
        MonthDay.of(8, 30) to "Zafer Bayramı",
        MonthDay.of(10, 29) to "Cumhuriyet Bayramı",
    )

    private const val RAMAZAN = "Ramazan Bayramı"
    private const val KURBAN = "Kurban Bayramı"

    private val DINI_BAYRAMLAR: Map<LocalDate, String> = buildMap {
        fun ekle(ad: String, ilkGun: LocalDate, gunSayisi: Int) {
            repeat(gunSayisi) { putIfAbsent(ilkGun.plusDays(it.toLong()), ad) }
        }
        // 2026
        ekle(RAMAZAN, LocalDate.of(2026, 3, 20), 3)
        ekle(KURBAN, LocalDate.of(2026, 5, 27), 4)
        // 2027
        ekle(RAMAZAN, LocalDate.of(2027, 3, 9), 3)
        ekle(KURBAN, LocalDate.of(2027, 5, 16), 4)
    }

    /** Dini bayram verisinin kapsadığı son yıl. */
    val SON_YIL: Int = DINI_BAYRAMLAR.keys.maxOf { it.year }

    /** O gün resmi tatilse tatilin adını döner, değilse null. */
    fun tatilAdi(tarih: LocalDate): String? =
        DINI_BAYRAMLAR[tarih] ?: SABIT[MonthDay.from(tarih)]

    fun kapaliMi(tarih: LocalDate): Boolean = tarih.haftaSonuMu() || tatilAdi(tarih) != null

    /** [tarih]'ten sonraki ilk açık (hafta içi ve tatil olmayan) gün. */
    fun sonrakiAcikGun(tarih: LocalDate): LocalDate {
        var gun = tarih.plusDays(1)
        repeat(60) {
            if (!kapaliMi(gun)) return gun
            gun = gun.plusDays(1)
        }
        return gun
    }
}
