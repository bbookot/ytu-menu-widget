package com.kampuslezzet.ytumenu.work

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.kampuslezzet.ytumenu.core.date.TurkiyeSaati
import com.kampuslezzet.ytumenu.widget.OgunGecisReceiver
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

object Zamanlayici {

    private const val MENU_PERIYODIK = "menu_periyodik"
    private const val MENU_TEK = "menu_tek"
    private const val GUNCELLEME_PERIYODIK = "guncelleme_periyodik"

    private val agGerekli = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

    /** Günde 1 menü kontrolü (gece 04:00 civarı) ve günde 1 güncelleme kontrolü. */
    fun periyodikIsleriPlanla(context: Context) {
        val wm = WorkManager.getInstance(context)
        val simdi = TurkiyeSaati.simdi()
        var hedef = simdi.toLocalDate().atTime(LocalTime.of(4, 0))
        if (!hedef.isAfter(simdi)) hedef = hedef.plusDays(1)
        val gecikme = Duration.between(simdi, hedef).toMinutes()

        wm.enqueueUniquePeriodicWork(
            MENU_PERIYODIK,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<MenuSyncWorker>(24, TimeUnit.HOURS)
                .setConstraints(agGerekli)
                .setInitialDelay(gecikme, TimeUnit.MINUTES)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
                .build(),
        )
        wm.enqueueUniquePeriodicWork(
            GUNCELLEME_PERIYODIK,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<UpdateCheckWorker>(24, TimeUnit.HOURS)
                .setConstraints(agGerekli)
                .setInitialDelay(gecikme + 30, TimeUnit.MINUTES)
                .build(),
        )
    }

    /**
     * Tek seferlik menü senkronizasyonu.
     * @param zorla Yenile butonu: sınırları yok sayar, ağ koşulu beklemeden hemen dener.
     */
    fun menuSenkronize(context: Context, zorla: Boolean) {
        val istek = OneTimeWorkRequestBuilder<MenuSyncWorker>()
            .setInputData(workDataOf(MenuSyncWorker.ZORLA to zorla))
            .apply {
                if (zorla) setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                else setConstraints(agGerekli).setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
            }
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            MENU_TEK,
            if (zorla) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP,
            istek,
        )
    }

    /**
     * Bir sonraki öğün geçişinde (11:30, 14:30, 17:00, 19:15, 00:00) widget'ı yeniden çizmek için alarm.
     * setWindow tam zamanlı alarm izni gerektirmez; en fazla ~1 dk gecikebilir.
     */
    fun ogunGecisiPlanla(context: Context, zaman: LocalDateTime) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        val pi = PendingIntent.getBroadcast(
            context, 0,
            Intent(context, OgunGecisReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val ms = TurkiyeSaati.epochMilis(zaman.plusSeconds(5))
        am.setWindow(AlarmManager.RTC, ms, 60_000L, pi)
    }
}
