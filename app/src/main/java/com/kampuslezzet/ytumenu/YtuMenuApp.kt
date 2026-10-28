package com.kampuslezzet.ytumenu

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import com.kampuslezzet.ytumenu.core.date.TurkiyeSaati
import com.kampuslezzet.ytumenu.data.local.AppDatabase
import com.kampuslezzet.ytumenu.data.local.Tercihler
import com.kampuslezzet.ytumenu.data.remote.sks.SksApi
import com.kampuslezzet.ytumenu.data.repository.MenuRepository
import com.kampuslezzet.ytumenu.domain.WidgetIcerikOlusturucu
import com.kampuslezzet.ytumenu.update.GuncellemeDeposu
import com.kampuslezzet.ytumenu.work.Zamanlayici
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/** Basit bağımlılık grafiği (DI kütüphanesi olmadan). */
class AppGraph(context: Context) {
    val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            chain.proceed(
                chain.request().newBuilder()
                    .header("User-Agent", "YTUMenu/${BuildConfig.VERSION_NAME} (Android)")
                    .build()
            )
        }
        .build()

    val tercihler = Tercihler(context)
    private val db = AppDatabase.olustur(context)
    val menuRepository = MenuRepository(db.menuDao(), SksApi(httpClient), tercihler, TurkiyeSaati)
    val widgetIcerikOlusturucu = WidgetIcerikOlusturucu(menuRepository, tercihler, TurkiyeSaati)
    val guncellemeDeposu = GuncellemeDeposu(context, httpClient)
}

class YtuMenuApp : Application() {

    lateinit var graph: AppGraph
        private set

    override fun onCreate() {
        super.onCreate()
        graph = AppGraph(this)
        bildirimKanallariniOlustur()
        Zamanlayici.periyodikIsleriPlanla(this)
    }

    private fun bildirimKanallariniOlustur() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(KANAL_GUNCELLEME, "Uygulama güncellemeleri", NotificationManager.IMPORTANCE_DEFAULT)
        )
        nm.createNotificationChannel(
            NotificationChannel(KANAL_ARKAPLAN, "Arka plan işlemleri", NotificationManager.IMPORTANCE_MIN)
        )
    }

    companion object {
        const val KANAL_GUNCELLEME = "guncelleme"
        const val KANAL_ARKAPLAN = "arkaplan"
    }
}

val Context.graph: AppGraph get() = (applicationContext as YtuMenuApp).graph
