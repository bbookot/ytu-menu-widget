package com.kampuslezzet.ytumenu.core.date

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/** Yemek salonu servis saatleri. */
object OgunSaatleri {
    val OGLE_BASLANGIC: LocalTime = LocalTime.of(11, 30)
    val OGLE_BITIS: LocalTime = LocalTime.of(14, 30)
    val AKSAM_BASLANGIC: LocalTime = LocalTime.of(17, 0)
    val AKSAM_BITIS: LocalTime = LocalTime.of(19, 15)
}

enum class GosterimModu { BUGUN, YARIN }

/**
 * Widget'ın o anda neyi göstereceği.
 * @property gun Menüsü gösterilecek tarih.
 * @property sonrakiGecis Görünümün bir sonraki değişeceği an (alarm kurulur).
 */
data class Gosterim(
    val gun: LocalDate,
    val mod: GosterimModu,
    val ogleGoster: Boolean,
    val aksamGoster: Boolean,
    val rozet: String,
    val servisAcik: Boolean,
    val sonrakiGecis: LocalDateTime,
)

/**
 * Saat dilimine göre görünüm kuralı:
 *  00:00–11:30 bugün öğle+akşam · 11:30–14:30 öğle+akşam (öğle açık)
 *  14:30–17:00 sadece akşam · 17:00–19:15 sadece akşam (akşam açık)
 *  19:15–24:00 yarının öğle+akşam menüsü
 */
object GosterimPlanlayici {

    fun planla(simdi: LocalDateTime): Gosterim {
        val saat = simdi.toLocalTime()
        val bugun = simdi.toLocalDate()
        return with(OgunSaatleri) {
            when {
                saat < OGLE_BASLANGIC -> Gosterim(
                    bugun, GosterimModu.BUGUN, true, true,
                    "Öğle yemeği 11:30'da başlıyor", false, bugun.atTime(OGLE_BASLANGIC),
                )
                saat < OGLE_BITIS -> Gosterim(
                    bugun, GosterimModu.BUGUN, true, true,
                    "Öğle servisi açık · 14:30'a kadar", true, bugun.atTime(OGLE_BITIS),
                )
                saat < AKSAM_BASLANGIC -> Gosterim(
                    bugun, GosterimModu.BUGUN, false, true,
                    "Akşam yemeği 17:00'de başlıyor", false, bugun.atTime(AKSAM_BASLANGIC),
                )
                saat < AKSAM_BITIS -> Gosterim(
                    bugun, GosterimModu.BUGUN, false, true,
                    "Akşam servisi açık · 19:15'e kadar", true, bugun.atTime(AKSAM_BITIS),
                )
                else -> {
                    val yarin = bugun.plusDays(1)
                    Gosterim(
                        yarin, GosterimModu.YARIN, true, true,
                        "Bugünkü servis sona erdi", false, yarin.atStartOfDay(),
                    )
                }
            }
        }
    }
}
