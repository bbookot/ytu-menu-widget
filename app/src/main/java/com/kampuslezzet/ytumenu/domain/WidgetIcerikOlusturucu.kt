package com.kampuslezzet.ytumenu.domain

import com.kampuslezzet.ytumenu.core.date.GosterimModu
import com.kampuslezzet.ytumenu.core.date.GosterimPlanlayici
import com.kampuslezzet.ytumenu.core.date.Saat
import com.kampuslezzet.ytumenu.core.date.TatilTakvimi
import com.kampuslezzet.ytumenu.core.date.TurkceTarih
import com.kampuslezzet.ytumenu.core.date.TurkiyeSaati
import com.kampuslezzet.ytumenu.data.local.Tercihler
import com.kampuslezzet.ytumenu.data.repository.MenuRepository
import com.kampuslezzet.ytumenu.domain.model.GunDurumu
import com.kampuslezzet.ytumenu.domain.model.OgunMenusu
import java.time.LocalDateTime

/** Widget'ın çizmesi gereken her şey; saf veri. */
data class WidgetIcerik(
    val baslik: String,
    val rozet: String?,
    val servisAcik: Boolean,
    val mod: GosterimModu,
    /** Menü varsa gösterilecek öğünler; null ise [mesaj] gösterilir. */
    val ogle: OgunBolumu?,
    val aksam: OgunBolumu?,
    val mesaj: String?,
    val altMesaj: String?,
    val kapaliGun: Boolean,
    val yenileniyor: Boolean,
    val sonGuncelleme: String?,
    val sonrakiGecis: LocalDateTime,
    val senkronGerekli: Boolean,
)

data class OgunBolumu(val baslik: String, val menu: OgunMenusu?)

class WidgetIcerikOlusturucu(
    private val repo: MenuRepository,
    private val tercihler: Tercihler,
    private val saat: Saat,
) {
    suspend fun olustur(): WidgetIcerik {
        val simdi = saat.simdi()
        val g = GosterimPlanlayici.planla(simdi)
        val yarin = g.mod == GosterimModu.YARIN
        val durum = repo.gunDurumu(g.gun)

        val baslik = (if (yarin) "Yarın, " else "Bugün, ") + TurkceTarih.uzun(g.gun)
        val sonrakiAcik = TatilTakvimi.sonrakiAcikGun(g.gun)
        val sonrakiAcikMetni = "Sonraki açık gün: ${TurkceTarih.gunVeTarih(sonrakiAcik)}"

        var ogle: OgunBolumu? = null
        var aksam: OgunBolumu? = null
        var mesaj: String? = null
        var altMesaj: String? = null
        var kapali = false

        when (durum) {
            is GunDurumu.Menu -> {
                if (g.ogleGoster) ogle = OgunBolumu(if (yarin) "Yarın Öğle" else "Öğle", durum.menu.ogle)
                if (g.aksamGoster) aksam = OgunBolumu(if (yarin) "Yarın Akşam" else "Akşam", durum.menu.aksam)
            }
            GunDurumu.HaftaSonu -> {
                kapali = true
                mesaj = if (yarin) "Yarın hafta sonu, yemekhane hizmeti verilmeyecek."
                else "Hafta sonu yemekhane hizmeti verilmemektedir."
                altMesaj = sonrakiAcikMetni
            }
            is GunDurumu.ResmiTatil -> {
                kapali = true
                mesaj = if (yarin) "Yarın resmi tatil (${durum.ad}). Yemekhane kapalı olacak."
                else "Bugün resmi tatil (${durum.ad}). Yemekhane kapalıdır."
                altMesaj = sonrakiAcikMetni
            }
            is GunDurumu.HenuzYayinlanmadi -> {
                mesaj = (if (yarin) "Yarın: " else "") + "${durum.ayAdi} ayı menüsü henüz yayınlanmadı."
                altMesaj = "Her gün otomatik olarak kontrol ediliyor."
            }
            GunDurumu.MenuYok -> {
                mesaj = if (yarin) "Yarın için yayınlanmış bir menü bulunmuyor." else "Bugün için yayınlanmış bir menü bulunmuyor."
            }
            GunDurumu.Yukleniyor -> mesaj = "Menü yükleniyor…"
            GunDurumu.BaglantiHatasi -> {
                mesaj = "Menüye ulaşılamadı."
                altMesaj = "İnternet bağlantını kontrol edip ⟳ ile tekrar dene."
            }
        }

        val rozet = when {
            kapali -> null
            durum !is GunDurumu.Menu -> if (yarin) "Bugünkü servis sona erdi" else null
            else -> g.rozet
        }

        val yenilemeBaslangici = tercihler.yenilemeBaslangici()
        val simdiMs = TurkiyeSaati.epochMilis(simdi)
        val yenileniyor = yenilemeBaslangici != null && simdiMs - yenilemeBaslangici < 60_000

        val sonGuncelleme = tercihler.sonGuncelleme()?.let { ms ->
            val z = TurkiyeSaati.zaman(ms)
            val gun = if (z.toLocalDate() == simdi.toLocalDate()) "" else "${z.dayOfMonth}.${"%02d".format(z.monthValue)} "
            "Son güncelleme $gun${"%02d:%02d".format(z.hour, z.minute)}"
        }

        return WidgetIcerik(
            baslik = baslik,
            rozet = rozet,
            servisAcik = g.servisAcik && durum is GunDurumu.Menu,
            mod = g.mod,
            ogle = ogle,
            aksam = aksam,
            mesaj = mesaj,
            altMesaj = altMesaj,
            kapaliGun = kapali,
            yenileniyor = yenileniyor,
            sonGuncelleme = sonGuncelleme,
            sonrakiGecis = g.sonrakiGecis,
            senkronGerekli = repo.senkronGerekli(g.gun),
        )
    }
}
