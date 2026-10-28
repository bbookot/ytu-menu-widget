package com.kampuslezzet.ytumenu.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import com.kampuslezzet.ytumenu.core.date.TurkiyeSaati
import com.kampuslezzet.ytumenu.graph
import com.kampuslezzet.ytumenu.work.Zamanlayici

class RefreshAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val simdi = TurkiyeSaati.epochMilis(TurkiyeSaati.simdi())
        context.graph.tercihler.yenilemeBasladi(simdi)
        // Widget'ı "Yenileniyor..." durumuna geçir
        WidgetYenileyici.yenile(context)
        // Arka planda zorla senkronizasyon başlat
        Zamanlayici.menuSenkronize(context, zorla = true)
    }
}
