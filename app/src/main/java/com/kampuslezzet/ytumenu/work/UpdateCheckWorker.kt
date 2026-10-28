package com.kampuslezzet.ytumenu.work

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kampuslezzet.ytumenu.R
import com.kampuslezzet.ytumenu.YtuMenuApp
import com.kampuslezzet.ytumenu.graph
import com.kampuslezzet.ytumenu.ui.MainActivity

/** Günde bir kez GitHub'daki son sürümü kontrol eder; yeniyse APK'yı indirip bildirim gösterir. */
class UpdateCheckWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val depo = applicationContext.graph.guncellemeDeposu
        return try {
            val surum = depo.sonSurum() ?: return Result.success()
            if (!depo.yeniMi(surum)) return Result.success()
            depo.indir(surum) { }
            bildirimGoster(surum.surumAdi)
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    private fun bildirimGoster(surumAdi: String) {
        val izinVar = ContextCompat.checkSelfPermission(
            applicationContext, Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!izinVar) return

        val intent = Intent(applicationContext, MainActivity::class.java)
            .putExtra(MainActivity.EKSTRA_GUNCELLE, true)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pi = PendingIntent.getActivity(
            applicationContext, 2, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val bildirim = NotificationCompat.Builder(applicationContext, YtuMenuApp.KANAL_GUNCELLEME)
            .setSmallIcon(R.drawable.ic_yenile)
            .setContentTitle("YTÜ Menü $surumAdi hazır")
            .setContentText("Yüklemek için dokun.")
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(applicationContext).notify(2001, bildirim)
    }
}
