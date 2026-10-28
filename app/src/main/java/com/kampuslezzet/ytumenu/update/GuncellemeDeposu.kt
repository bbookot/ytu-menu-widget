package com.kampuslezzet.ytumenu.update

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.kampuslezzet.ytumenu.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

@Serializable
data class GitHubAsset(
    val name: String,
    @SerialName("browser_download_url") val indirmeUrl: String,
    val size: Long,
)

@Serializable
data class GitHubRelease(
    @SerialName("tag_name") val etiket: String,
    val name: String? = null,
    val body: String? = null,
    val prerelease: Boolean = false,
    val draft: Boolean = false,
    val assets: List<GitHubAsset> = emptyList(),
)

data class SurumBilgisi(
    val surumAdi: String,
    val surumKodu: Int,
    val indirmeUrl: String,
    val boyutBayt: Long,
    val degisiklikNotlari: String,
)

class GuncellemeDeposu(
    private val context: Context,
    private val client: OkHttpClient,
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    /** GitHub Releases API'den en son release'i çeker (public repo, token gerektirmez). */
    suspend fun sonSurum(): SurumBilgisi? = withContext(Dispatchers.IO) {
        val repo = BuildConfig.GITHUB_REPO
        val url = "https://api.github.com/repos/$repo/releases/latest"
        val istek = Request.Builder()
            .url(url)
            .header("Accept", "application/vnd.github+json")
            .build()

        val yanitGövdesi = try {
            client.newCall(istek).execute().use { yanit ->
                if (!yanit.isSuccessful) return@withContext null
                yanit.body?.string() ?: return@withContext null
            }
        } catch (e: Exception) {
            return@withContext null
        }

        val release = runCatching { json.decodeFromString<GitHubRelease>(yanitGövdesi) }.getOrNull()
            ?: return@withContext null

        val apkAsset = release.assets.find { it.name.endsWith(".apk", ignoreCase = true) }
            ?: return@withContext null

        val surumKodu = parseVersionCode(release.etiket)

        SurumBilgisi(
            surumAdi = release.etiket.removePrefix("v"),
            surumKodu = surumKodu,
            indirmeUrl = apkAsset.indirmeUrl,
            boyutBayt = apkAsset.size,
            degisiklikNotlari = release.body ?: "",
        )
    }

    fun yeniMi(surum: SurumBilgisi): Boolean {
        return surum.surumKodu > BuildConfig.VERSION_CODE
    }

    /** APK dosyasını cacheDir/updates klasörüne indirir. */
    suspend fun indir(surum: SurumBilgisi, ilerleme: (Float) -> Unit): File = withContext(Dispatchers.IO) {
        val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
        val hedefDosya = File(updatesDir, "ytumenu-${surum.surumAdi}.apk")

        if (hedefDosya.exists() && hedefDosya.length() == surum.boyutBayt) {
            ilerleme(1.0f)
            return@withContext hedefDosya
        }

        val istek = Request.Builder().url(surum.indirmeUrl).build()
        client.newCall(istek).execute().use { yanit ->
            if (!yanit.isSuccessful) throw IOException("APK indirilemedi: HTTP ${yanit.code}")
            val body = yanit.body ?: throw IOException("Boş yanıt gövdesi")
            val toplamBoyut = if (surum.boyutBayt > 0) surum.boyutBayt else body.contentLength()

            body.byteStream().use { giris ->
                FileOutputStream(hedefDosya).use { cikis ->
                    val buffer = ByteArray(8192)
                    var okunan: Int
                    var toplamOkunan = 0L

                    while (giris.read(buffer).also { okunan = it } != -1) {
                        cikis.write(buffer, 0, okunan)
                        toplamOkunan += okunan
                        if (toplamBoyut > 0) {
                            ilerleme(toplamOkunan.toFloat() / toplamBoyut)
                        }
                    }
                    cikis.flush()
                }
            }
        }
        ilerleme(1.0f)
        hedefDosya
    }

    /** İndirilen APK için sistem paket yükleyicisini tetikler. */
    fun yukle(apkDosyasi: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkDosyasi
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    private fun parseVersionCode(tag: String): Int {
        // v1.0.3 veya v1.0.3+12 veya 12
        val artiBolunmus = tag.substringAfterLast('+', "")
        if (artiBolunmus.isNotEmpty() && artiBolunmus.all { it.isDigit() }) {
            return artiBolunmus.toInt()
        }
        val temiz = tag.removePrefix("v").replace(Regex("[^0-9.]"), "")
        val parcalar = temiz.split('.').mapNotNull { it.toIntOrNull() }
        return when (parcalar.size) {
            1 -> parcalar[0]
            2 -> parcalar[0] * 1000 + parcalar[1]
            3 -> parcalar[0] * 10000 + parcalar[1] * 100 + parcalar[2]
            else -> 1
        }
    }
}
