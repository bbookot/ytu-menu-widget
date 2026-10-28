package com.kampuslezzet.ytumenu.work

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.kampuslezzet.ytumenu.R
import com.kampuslezzet.ytumenu.YtuMenuApp
import com.kampuslezzet.ytumenu.graph
import com.kampuslezzet.ytumenu.widget.WidgetYenileyici

/** SKS'den menüyü çeker, önbelleğe yazar ve widget'ı günceller. */
class MenuSyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val zorla = inputData.getBoolean(ZORLA, false)
        val graph = applicationContext.graph
        val basarili = try {
            graph.menuRepository.senkronize(zorla)
        } finally {
            graph.tercihler.yenilemeBasladi(null)
            WidgetYenileyici.yenile(applicationContext)
        }
        return when {
            basarili || zorla -> Result.success()
            runAttemptCount < 5 -> Result.retry()
            else -> Result.failure()
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val bildirim = NotificationCompat.Builder(applicationContext, YtuMenuApp.KANAL_ARKAPLAN)
            .setSmallIcon(R.drawable.ic_yenile)
            .setContentTitle("Menü güncelleniyor")
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
        // Yalnızca Android 12 öncesinde "expedited" iş için kullanılır (S25'te çağrılmaz).
        return ForegroundInfo(1001, bildirim)
    }

    companion object {
        const val ZORLA = "zorla"
    }
}
