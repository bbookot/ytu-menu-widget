package com.kampuslezzet.ytumenu.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll
import com.kampuslezzet.ytumenu.graph
import com.kampuslezzet.ytumenu.work.Zamanlayici

object WidgetYenileyici {
    suspend fun yenile(context: Context) {
        // Tercihlerdeki sayaç değerini artırarak glance widget'ın tetiklenmesini sağla
        context.graph.tercihler.widgetSurumunuArtir()
        MenuWidget().updateAll(context)
    }
}
