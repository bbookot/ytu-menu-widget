package com.kampuslezzet.ytumenu.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.kampuslezzet.ytumenu.BuildConfig
import com.kampuslezzet.ytumenu.core.date.TurkceTarih
import com.kampuslezzet.ytumenu.domain.model.GunlukMenu
import com.kampuslezzet.ytumenu.domain.model.OgunMenusu
import com.kampuslezzet.ytumenu.graph
import com.kampuslezzet.ytumenu.update.SurumBilgisi
import com.kampuslezzet.ytumenu.work.Zamanlayici
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    companion object {
        const val EKSTRA_GUNCELLE = "ekstra_guncelle"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val guncellemeIcinMi = intent.getBooleanExtra(EKSTRA_GUNCELLE, false)

        setContent {
            val sistemKoyuMu = isSystemInDarkTheme()
            // null: Sistem Teması, true: Koyu, false: Açık
            var manuelKoyuMod by remember { mutableStateOf<Boolean?>(null) }
            val koyuModAktif = manuelKoyuMod ?: sistemKoyuMu

            val renkler = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                if (koyuModAktif) dynamicDarkColorScheme(this) else dynamicLightColorScheme(this)
            } else {
                if (koyuModAktif) darkColorScheme() else lightColorScheme()
            }

            MaterialTheme(colorScheme = renkler) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    UygulamaEkrani(
                        activity = this,
                        otomatikGuncellemeKontrol = guncellemeIcinMi,
                        koyuModAktif = koyuModAktif,
                        onTemaDegistir = {
                            manuelKoyuMod = !koyuModAktif
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UygulamaEkrani(
    activity: MainActivity,
    otomatikGuncellemeKontrol: Boolean,
    koyuModAktif: Boolean,
    onTemaDegistir: () -> Unit
) {
    val context = activity
    val graph = context.graph

    val gunler by graph.menuRepository.sonrakiGunler(14).collectAsState(initial = emptyList())
    var guncellemeDurumu by remember { mutableStateOf<String?>(null) }
    var yeniSurum by remember { mutableStateOf<SurumBilgisi?>(null) }
    var indiriliyor by remember { mutableStateOf(false) }
    var indirmeYuzdesi by remember { mutableFloatStateOf(0f) }
    var guncellemeDiyaloguAcik by remember { mutableStateOf(false) }
    var hakkindaDiyaloguAcik by remember { mutableStateOf(false) }

    fun guncellemeleriDenetle() {
        guncellemeDurumu = "Kontrol ediliyor..."
        activity.lifecycleScope.launch {
            val surum = graph.guncellemeDeposu.sonSurum()
            if (surum != null && graph.guncellemeDeposu.yeniMi(surum)) {
                yeniSurum = surum
                guncellemeDurumu = null
                guncellemeDiyaloguAcik = true
            } else {
                guncellemeDurumu = "Uygulama güncel (v${BuildConfig.VERSION_NAME})"
                Toast.makeText(context, "Harika! En güncel sürümü kullanıyorsunuz.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(Unit) {
        if (otomatikGuncellemeKontrol) {
            guncellemeleriDenetle()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("YTÜ Yemekhane Menüsü", fontWeight = FontWeight.Bold) },
                actions = {
                    // Tema Değiştirme Butonu (Güneş / Ay)
                    IconButton(onClick = onTemaDegistir) {
                        Text(if (koyuModAktif) "☀️" else "🌙", fontSize = 18.sp)
                    }

                    // Hakkında Butonu
                    IconButton(onClick = { hakkindaDiyaloguAcik = true }) {
                        Text("ℹ️", fontSize = 16.sp)
                    }

                    // Yenile Butonu
                    TextButton(onClick = {
                        activity.lifecycleScope.launch {
                            Toast.makeText(context, "Menü senkronize ediliyor...", Toast.LENGTH_SHORT).show()
                            Zamanlayici.menuSenkronize(context, zorla = true)
                        }
                    }) {
                        Text("Yenile")
                    }
                }
            )
        }
    ) { icBosluk ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(icBosluk)
                .padding(horizontal = 16.dp)
        ) {
            // Sürüm ve Güncelleme Kartı
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Mevcut Sürüm: v${BuildConfig.VERSION_NAME}", fontSize = 13.sp)
                        if (guncellemeDurumu != null) {
                            Text(guncellemeDurumu!!, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Button(
                        onClick = { guncellemeleriDenetle() },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("Güncelleme Denetle", fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Gelecek Günlerin Menüsü Listesi
            if (gunler.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Kayıtlı menü bulunamadı. Lütfen 'Yenile' butonuna dokunun.")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(gunler, key = { it.tarih.toString() }) { gun ->
                        GunlukMenuKarti(gun)
                    }
                }
            }
        }
    }

    // Hakkında Diyaloğu
    if (hakkindaDiyaloguAcik) {
        AlertDialog(
            onDismissRequest = { hakkindaDiyaloguAcik = false },
            title = { Text("Hakkında") },
            text = {
                Column {
                    Text("🍽️ YTÜ Menü (v${BuildConfig.VERSION_NAME})", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Yıldız Teknik Üniversitesi Sağlık Kültür ve Spor Daire Başkanlığı (SKS) " +
                        "öğrenci yemek menülerini pratik bir şekilde takip etmeniz için geliştirilmiş One UI uyumlu yardımcı uygulama ve ana ekran widget'ı.",
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("✨ Özellikler:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    Text("• Öğle ve akşam saatine duyarlı otomatik geçiş", fontSize = 12.sp)
                    Text("• Tatil günlerinde sonraki açık günün menü önizlemesi", fontSize = 12.sp)
                    Text("• Günler arası ileri/geri gezinti tuşları", fontSize = 12.sp)
                    Text("• Normal ve vejetaryen menü alternatifleri", fontSize = 12.sp)
                    Text("• Açık/Koyu tema ve widget dinamik renk desteği", fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "🔗 GitHub Proje Sayfası:\nhttps://github.com/${BuildConfig.GITHUB_REPO}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { hakkindaDiyaloguAcik = false }) {
                    Text("Tamam")
                }
            }
        )
    }

    // Güncelleme İndirme Diyaloğu
    if (guncellemeDiyaloguAcik && yeniSurum != null) {
        val surum = yeniSurum!!
        AlertDialog(
            onDismissRequest = { if (!indiriliyor) guncellemeDiyaloguAcik = false },
            title = { Text("Yeni Sürüm Mevcut! (v${surum.surumAdi})") },
            text = {
                Column {
                    if (surum.degisiklikNotlari.isNotBlank()) {
                        Text(surum.degisiklikNotlari, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    if (indiriliyor) {
                        Text("APK indiriliyor... %${(indirmeYuzdesi * 100).toInt()}")
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { indirmeYuzdesi },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text("Boyut: ${surum.boyutBayt / (1024 * 1024)} MB")
                    }
                }
            },
            confirmButton = {
                if (!indiriliyor) {
                    Button(onClick = {
                        indiriliyor = true
                        activity.lifecycleScope.launch {
                            val apk = graph.guncellemeDeposu.indir(surum) { indirmeYuzdesi = it }
                            indiriliyor = false
                            guncellemeDiyaloguAcik = false
                            graph.guncellemeDeposu.yukle(apk)
                        }
                    }) {
                        Text("İndir ve Yükle")
                    }
                }
            },
            dismissButton = {
                if (!indiriliyor) {
                    TextButton(onClick = { guncellemeDiyaloguAcik = false }) {
                        Text("Daha Sonra")
                    }
                }
            }
        )
    }
}

@Composable
fun GunlukMenuKarti(gun: GunlukMenu) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = TurkceTarih.uzun(gun.tarih),
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                // Öğle Menüsü
                Column(modifier = Modifier.weight(1f)) {
                    Text("ÖĞLE", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    OgunIcerikMetin(gun.ogle)
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Akşam Menüsü
                Column(modifier = Modifier.weight(1f)) {
                    Text("AKŞAM", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    OgunIcerikMetin(gun.aksam)
                }
            }
        }
    }
}

@Composable
private fun OgunIcerikMetin(ogun: OgunMenusu?) {
    if (ogun == null || ogun.normal.isEmpty()) {
        Text("Servis yok", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    } else {
        ogun.normal.forEach { yemek ->
            Text(
                text = "• ${yemek.ad} ${yemek.kalori?.let { "($it kcal)" } ?: ""}",
                fontSize = 12.sp
            )
        }
        val vejFarki = ogun.vejetaryenFarki
        if (vejFarki.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "🌱 Vejetaryen: " + vejFarki.joinToString(", ") { it.ad },
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}
