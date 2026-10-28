package com.kampuslezzet.ytumenu.data.repository

import com.kampuslezzet.ytumenu.core.date.Saat
import com.kampuslezzet.ytumenu.core.date.TatilTakvimi
import com.kampuslezzet.ytumenu.core.date.TurkceTarih
import com.kampuslezzet.ytumenu.core.date.TurkiyeSaati
import com.kampuslezzet.ytumenu.core.date.haftaSonuMu
import com.kampuslezzet.ytumenu.data.local.MenuDao
import com.kampuslezzet.ytumenu.data.local.MenuEntity
import com.kampuslezzet.ytumenu.data.local.Tercihler
import com.kampuslezzet.ytumenu.data.remote.sks.SksApi
import com.kampuslezzet.ytumenu.data.remote.sks.SksParser
import com.kampuslezzet.ytumenu.domain.model.GunDurumu
import com.kampuslezzet.ytumenu.domain.model.GunlukMenu
import com.kampuslezzet.ytumenu.domain.model.OgunMenusu
import com.kampuslezzet.ytumenu.domain.model.YemekCozucu
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import java.time.YearMonth

class MenuRepository(
    private val dao: MenuDao,
    private val api: SksApi,
    private val tercihler: Tercihler,
    private val saat: Saat,
) {
    private val kilit = Mutex()

    /** Bir günün durumu — sadece yerel veriye bakar, ağa çıkmaz. */
    suspend fun gunDurumu(gun: LocalDate): GunDurumu {
        if (gun.haftaSonuMu()) return GunDurumu.HaftaSonu
        TatilTakvimi.tatilAdi(gun)?.let { return GunDurumu.ResmiTatil(it) }
        dao.gun(gun.toString())?.let { return GunDurumu.Menu(it.alanaCevir()) }

        val ay = YearMonth.from(gun)
        if (dao.aydakiGunSayisi(ay.toString()) > 0) return GunDurumu.MenuYok
        val kontrol = tercihler.sonKontrol(ay)
        val hata = tercihler.sonHata(ay)
        return when {
            hata != null && (kontrol == null || hata > kontrol) -> GunDurumu.BaglantiHatasi
            kontrol == null -> GunDurumu.Yukleniyor
            else -> GunDurumu.HenuzYayinlanmadi(TurkceTarih.ayAdi(ay))
        }
    }

    /** Bu güne ait ay için bugün henüz ağ kontrolü yapılmadıysa ve veri yoksa true. */
    suspend fun senkronGerekli(gun: LocalDate): Boolean {
        if (TatilTakvimi.kapaliMi(gun)) return false
        val ay = YearMonth.from(gun)
        if (dao.aydakiGunSayisi(ay.toString()) > 0) return false
        val kontrol = tercihler.sonKontrol(ay) ?: return true
        return TurkiyeSaati.tarih(kontrol) != saat.bugun()
    }

    /**
     * SKS'den gerekli ayları çeker.
     * - Yayınlanmış ve son 3 gün içinde kontrol edilmiş aylar atlanır.
     * - Yayınlanmamış aylar için günde en fazla bir istek atılır.
     * - [zorla] true ise (Yenile butonu) tüm sınırlar yok sayılır.
     * @return tüm istekler başarılıysa true
     */
    suspend fun senkronize(zorla: Boolean): Boolean = kilit.withLock {
        val bugun = saat.bugun()
        val aylar = linkedSetOf(YearMonth.from(bugun), YearMonth.from(bugun.plusDays(1)))
        if (bugun.lengthOfMonth() - bugun.dayOfMonth < 5) aylar += YearMonth.from(bugun).plusMonths(1)

        var basarili = true
        for (ay in aylar) {
            val yayinda = dao.aydakiGunSayisi(ay.toString()) > 0
            val sonKontrolGunu = tercihler.sonKontrol(ay)?.let(TurkiyeSaati::tarih)
            val atla = when {
                zorla -> false
                sonKontrolGunu == null -> false
                yayinda -> !sonKontrolGunu.isBefore(bugun.minusDays(3))
                else -> sonKontrolGunu == bugun
            }
            if (atla) continue

            val simdi = TurkiyeSaati.epochMilis(saat.simdi())
            try {
                val gunler = SksParser.coz(api.ayMenusu(ay)).filter { YearMonth.from(it.tarih) == ay }
                if (gunler.isNotEmpty()) {
                    dao.ayiDegistir(ay.toString(), gunler.map { g ->
                        MenuEntity(
                            tarih = g.tarih.toString(),
                            yilAy = ay.toString(),
                            ogleNormal = g.ogleNormal.joinToString("\n"),
                            ogleVejetaryen = g.ogleVejetaryen.joinToString("\n"),
                            aksamNormal = g.aksamNormal.joinToString("\n"),
                            aksamVejetaryen = g.aksamVejetaryen.joinToString("\n"),
                            cekilmeZamani = simdi,
                        )
                    })
                }
                tercihler.kontrolBasarili(ay, simdi)
            } catch (e: Exception) {
                tercihler.kontrolHatali(ay, simdi)
                basarili = false
            }
        }
        dao.eskileriSil(bugun.minusDays(40).toString())
        basarili
    }

    /** Uygulama ekranı için bugünden itibaren menüsü olan günler. */
    fun sonrakiGunler(adet: Int = 10): Flow<List<GunlukMenu>> =
        dao.sonrakiGunler(saat.bugun().toString(), adet).map { liste -> liste.map { it.alanaCevir() } }

    private fun MenuEntity.alanaCevir(): GunlukMenu {
        fun ogun(normal: String, vej: String): OgunMenusu? {
            val n = YemekCozucu.coz(normal.lines())
            val v = YemekCozucu.coz(vej.lines())
            return if (n.isEmpty() && v.isEmpty()) null else OgunMenusu(n, v)
        }
        return GunlukMenu(
            tarih = LocalDate.parse(tarih),
            ogle = ogun(ogleNormal, ogleVejetaryen),
            aksam = ogun(aksamNormal, aksamVejetaryen),
        )
    }
}
