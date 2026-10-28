package com.kampuslezzet.ytumenu.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.appwidget.action.ActionCallback
import com.kampuslezzet.ytumenu.graph

class GunDegistirAction : ActionCallback {

    companion object {
        val YON_KEY = ActionParameters.Key<Int>("yon")
    }

    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val yon = parameters[YON_KEY] ?: 0
        val graph = context.graph
        val mevcutOfset = graph.tercihler.widgetGunOfseti()
        val yeniOfset = (mevcutOfset + yon).coerceIn(0, 7) // 0: bugün, en fazla 7 gün ileri
        graph.tercihler.widgetGunOfsetiAyarla(yeniOfset)
        WidgetYenileyici.yenile(context)
    }
}
