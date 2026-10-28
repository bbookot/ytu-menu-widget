package com.kampuslezzet.ytumenu.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kampuslezzet.ytumenu.work.Zamanlayici
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class OgunGecisReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        CoroutineScope(Dispatchers.Default).launch {
            WidgetYenileyici.yenile(context)
        }
    }
}

class SistemOlayReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Cihaz açıldığında veya saat/zaman dilimi değiştiğinde periyodik işleri ve alarmları yeniden ayarla
        Zamanlayici.periyodikIsleriPlanla(context)
        CoroutineScope(Dispatchers.Default).launch {
            WidgetYenileyici.yenile(context)
        }
    }
}
