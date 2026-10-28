package com.kampuslezzet.ytumenu.data.local

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.YearMonth

private val Context.dataStore by preferencesDataStore(name = "ytumenu")

/** Küçük durum bilgileri: ay bazında son kontrol/hata zamanları, yenileme durumu vb. */
class Tercihler(private val context: Context) {

    private suspend fun oku(): Preferences = context.dataStore.data.first()

    private fun kontrolAnahtari(ay: YearMonth) = longPreferencesKey("kontrol_$ay")
    private fun hataAnahtari(ay: YearMonth) = longPreferencesKey("hata_$ay")
    private val sonGuncellemeAnahtari = longPreferencesKey("son_guncelleme")
    private val yenilemeAnahtari = longPreferencesKey("yenileme_baslangic")
    private val widgetSurumuAnahtari = intPreferencesKey("widget_surumu")
    private val widgetGunOfsetiAnahtari = intPreferencesKey("widget_gun_ofseti")

    suspend fun widgetGunOfseti(): Int = oku()[widgetGunOfsetiAnahtari] ?: 0

    suspend fun widgetGunOfsetiAyarla(ofset: Int) {
        context.dataStore.edit { it[widgetGunOfsetiAnahtari] = ofset.coerceAtLeast(0) }
    }

    suspend fun sonKontrol(ay: YearMonth): Long? = oku()[kontrolAnahtari(ay)]
    suspend fun sonHata(ay: YearMonth): Long? = oku()[hataAnahtari(ay)]
    suspend fun sonGuncelleme(): Long? = oku()[sonGuncellemeAnahtari]

    suspend fun kontrolBasarili(ay: YearMonth, zaman: Long) {
        context.dataStore.edit {
            it[kontrolAnahtari(ay)] = zaman
            it.remove(hataAnahtari(ay))
            it[sonGuncellemeAnahtari] = zaman
        }
    }

    suspend fun kontrolHatali(ay: YearMonth, zaman: Long) {
        context.dataStore.edit { it[hataAnahtari(ay)] = zaman }
    }

    /** Yenile butonuna basıldığında "Yenileniyor…" göstermek için. */
    suspend fun yenilemeBasladi(zaman: Long?) {
        context.dataStore.edit {
            if (zaman == null) it.remove(yenilemeAnahtari) else it[yenilemeAnahtari] = zaman
        }
    }

    suspend fun yenilemeBaslangici(): Long? = oku()[yenilemeAnahtari]

    /** Widget'ı yeniden çizdirmek için artırılan sayaç. */
    val widgetSurumu: Flow<Int> = context.dataStore.data.map { it[widgetSurumuAnahtari] ?: 0 }

    suspend fun widgetSurumunuArtir() {
        context.dataStore.edit { it[widgetSurumuAnahtari] = (it[widgetSurumuAnahtari] ?: 0) + 1 }
    }
}
