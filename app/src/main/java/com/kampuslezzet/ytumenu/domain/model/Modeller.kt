package com.kampuslezzet.ytumenu.domain.model

import java.time.LocalDate
import java.util.Locale

enum class Kategori { CORBA, ANA_YEMEK, YAN_YEMEK, ICECEK, SALATA, TATLI_MEYVE, DIGER }

data class YemekKalemi(val ad: String, val kalori: Int?, val kategori: Kategori)

data class OgunMenusu(val normal: List<YemekKalemi>, val vejetaryen: List<YemekKalemi>) {
    /** Vejetaryen menüde olup normal menüde bulunmayan kalemler (widget'ta yer kazanmak için). */
    val vejetaryenFarki: List<YemekKalemi>
        get() {
            val normalAdlar = normal.map { it.ad.lowercase(TR) }.toSet()
            return vejetaryen.filter { it.ad.lowercase(TR) !in normalAdlar }
        }

    val toplamKalori: Int? get() = normal.mapNotNull { it.kalori }.takeIf { it.isNotEmpty() }?.sum()
}

data class GunlukMenu(val tarih: LocalDate, val ogle: OgunMenusu?, val aksam: OgunMenusu?)

/** Belirli bir gün için yemekhanenin durumu. */
sealed interface GunDurumu {
    data object HaftaSonu : GunDurumu
    data class ResmiTatil(val ad: String) : GunDurumu
    data class Menu(val menu: GunlukMenu) : GunDurumu
    data class HenuzYayinlanmadi(val ayAdi: String) : GunDurumu
    /** Ayın menüsü yayında ama bu güne ait kayıt yok. */
    data object MenuYok : GunDurumu
    /** Henüz hiç kontrol yapılmadı (ilk kurulum). */
    data object Yukleniyor : GunDurumu
    data object BaglantiHatasi : GunDurumu
}

internal val TR: Locale = Locale.forLanguageTag("tr-TR")
