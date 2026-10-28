package com.kampuslezzet.ytumenu.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.kampuslezzet.ytumenu.R
import com.kampuslezzet.ytumenu.domain.OgunBolumu
import com.kampuslezzet.ytumenu.domain.WidgetIcerik
import com.kampuslezzet.ytumenu.graph
import com.kampuslezzet.ytumenu.ui.MainActivity
import com.kampuslezzet.ytumenu.work.Zamanlayici

class MenuWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val graph = context.graph
        val icerik = graph.widgetIcerikOlusturucu.olustur()

        // Bir sonraki geçiş anına AlarmManager kur
        Zamanlayici.ogunGecisiPlanla(context, icerik.sonrakiGecis)

        // Eğer eksik ay varsa veya bugüne veri lazımsa arka planda senkronizasyon tetikle
        if (icerik.senkronGerekli) {
            Zamanlayici.menuSenkronize(context, zorla = false)
        }

        provideContent {
            GlanceTheme {
                WidgetGovde(context = context, icerik = icerik)
            }
        }
    }
}

@Composable
private fun WidgetGovde(context: Context, icerik: WidgetIcerik) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.widgetBackground)
            .cornerRadius(24.dp)
            .padding(12.dp)
            .clickable(actionStartActivity<MainActivity>())
    ) {
        Column(modifier = GlanceModifier.fillMaxSize()) {
            // Üst Başlık & Yenile Butonu
            UstBaslik(icerik)

            Spacer(modifier = GlanceModifier.height(6.dp))

            // İçerik: Ya öğünler ya da mesaj kartı
            Box(
                modifier = GlanceModifier
                    .defaultWeight()
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                if (icerik.mesaj != null) {
                    MesajGosterimi(icerik.mesaj, icerik.altMesaj, icerik.sonrakiAcikGun, icerik.sonrakiAcikMenu)
                } else {
                    OgunlerGosterimi(icerik.ogle, icerik.aksam)
                }
            }

            // Alt Bilgi (Son güncelleme / Yenileniyor)
            AltBilgi(icerik)
        }
    }
}

@Composable
private fun UstBaslik(icerik: WidgetIcerik) {
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(
                text = "🍽️ ${icerik.baslik}",
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 1
            )
            if (icerik.rozet != null) {
                Text(
                    text = icerik.rozet,
                    style = TextStyle(
                        color = if (icerik.servisAcik) GlanceTheme.colors.primary else GlanceTheme.colors.onSurfaceVariant,
                        fontSize = 10.sp,
                        fontWeight = if (icerik.servisAcik) FontWeight.Bold else FontWeight.Normal
                    ),
                    maxLines = 1
                )
            }
        }

        // Yenile Butonu
        Box(
            modifier = GlanceModifier
                .size(32.dp)
                .cornerRadius(16.dp)
                .background(GlanceTheme.colors.secondaryContainer)
                .clickable(actionRunCallback<RefreshAction>()),
            contentAlignment = Alignment.Center
        ) {
            Image(
                provider = ImageProvider(R.drawable.ic_yenile),
                contentDescription = "Yenile",
                modifier = GlanceModifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun MesajGosterimi(
    mesaj: String,
    altMesaj: String?,
    sonrakiAcikGun: java.time.LocalDate? = null,
    sonrakiAcikMenu: com.kampuslezzet.ytumenu.domain.model.GunlukMenu? = null,
) {
    Column(
        modifier = GlanceModifier.fillMaxWidth().padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = mesaj,
            style = TextStyle(
                color = GlanceTheme.colors.onSurface,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            ),
            maxLines = 2
        )
        if (altMesaj != null) {
            Spacer(modifier = GlanceModifier.height(2.dp))
            Text(
                text = altMesaj,
                style = TextStyle(
                    color = GlanceTheme.colors.primary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 1
            )
        }

        // Eğer tatil günüyse ve bir sonraki günün menüsü hazırsa göster
        if (sonrakiAcikMenu != null) {
            Spacer(modifier = GlanceModifier.height(6.dp))
            Box(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(GlanceTheme.colors.outline)
            ) {}
            Spacer(modifier = GlanceModifier.height(4.dp))

            Row(modifier = GlanceModifier.fillMaxWidth()) {
                val ogle = sonrakiAcikMenu.ogle
                val aksam = sonrakiAcikMenu.aksam
                if (ogle != null) {
                    Box(modifier = GlanceModifier.defaultWeight()) {
                        OzetMenuYazisi("Öğle", ogle)
                    }
                }
                if (aksam != null) {
                    Spacer(modifier = GlanceModifier.width(6.dp))
                    Box(modifier = GlanceModifier.defaultWeight()) {
                        OzetMenuYazisi("Akşam", aksam)
                    }
                }
            }
        }
    }
}

@Composable
private fun OzetMenuYazisi(baslik: String, ogun: com.kampuslezzet.ytumenu.domain.model.OgunMenusu) {
    Column {
        Text(
            text = baslik,
            style = TextStyle(
                color = GlanceTheme.colors.primary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        )
        ogun.normal.take(3).forEach { y ->
            Text(
                text = "• ${y.ad}",
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 9.sp
                ),
                maxLines = 1
            )
        }
        val vej = ogun.vejetaryenFarki
        if (vej.isNotEmpty()) {
            Text(
                text = "🌱 " + vej.first().ad,
                style = TextStyle(
                    color = GlanceTheme.colors.secondary,
                    fontSize = 8.sp
                ),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun OgunlerGosterimi(ogle: OgunBolumu?, aksam: OgunBolumu?) {
    Row(
        modifier = GlanceModifier.fillMaxSize(),
        verticalAlignment = Alignment.Top
    ) {
        if (ogle != null && aksam != null) {
            // İki Öğün Yan Yana
            Box(modifier = GlanceModifier.defaultWeight()) {
                TekOgunSutunu(ogle)
            }
            Spacer(modifier = GlanceModifier.width(8.dp))
            Box(
                modifier = GlanceModifier
                    .width(1.dp)
                    .fillMaxSize()
                    .background(GlanceTheme.colors.outline)
            ) {}
            Spacer(modifier = GlanceModifier.width(8.dp))
            Box(modifier = GlanceModifier.defaultWeight()) {
                TekOgunSutunu(aksam)
            }
        } else {
            // Tek Öğün (örn. sadece akşam)
            val tek = ogle ?: aksam
            if (tek != null) {
                TekOgunSutunu(tek, genisMod = true)
            }
        }
    }
}

@Composable
private fun TekOgunSutunu(bolum: OgunBolumu, genisMod: Boolean = false) {
    Column(modifier = GlanceModifier.fillMaxSize()) {
        Text(
            text = bolum.baslik,
            style = TextStyle(
                color = GlanceTheme.colors.primary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            ),
            maxLines = 1
        )
        Spacer(modifier = GlanceModifier.height(2.dp))

        val menu = bolum.menu
        if (menu == null || menu.normal.isEmpty()) {
            Text(
                text = "Menü bulunmuyor",
                style = TextStyle(
                    color = GlanceTheme.colors.onSurfaceVariant,
                    fontSize = 11.sp
                )
            )
        } else {
            // Normal Menü Kalemleri (ilk 4-5 kalem)
            val limit = if (genisMod) 5 else 4
            menu.normal.take(limit).forEach { yemek ->
                Text(
                    text = "• ${yemek.ad}",
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurface,
                        fontSize = if (genisMod) 11.sp else 10.sp
                    ),
                    maxLines = 1
                )
            }

            // Vejetaryen Alternatif (daha küçük fontta)
            val vejFarki = menu.vejetaryenFarki
            if (vejFarki.isNotEmpty()) {
                Spacer(modifier = GlanceModifier.height(2.dp))
                val vejetaryenMetin = "🌱 " + vejFarki.joinToString(", ") { it.ad }
                Text(
                    text = vejetaryenMetin,
                    style = TextStyle(
                        color = GlanceTheme.colors.secondary,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun AltBilgi(icerik: WidgetIcerik) {
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        horizontalAlignment = Alignment.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val altYazi = when {
            icerik.yenileniyor -> "Yenileniyor…"
            icerik.sonGuncelleme != null -> icerik.sonGuncelleme
            else -> ""
        }
        if (altYazi.isNotEmpty()) {
            Text(
                text = altYazi,
                style = TextStyle(
                    color = GlanceTheme.colors.onSurfaceVariant,
                    fontSize = 9.sp
                )
            )
        }
    }
}
