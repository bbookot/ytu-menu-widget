package com.kampuslezzet.ytumenu.data.remote.sks

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.time.YearMonth

/** YTÜ SKS yemek menüsü API istemcisi (sitenin kendi sayfasının kullandığı uç nokta). */
class SksApi(private val client: OkHttpClient) {

    suspend fun ayMenusu(ay: YearMonth): String = withContext(Dispatchers.IO) {
        val url = "https://sks.yildiz.edu.tr/api/food-menu".toHttpUrl().newBuilder()
            .addQueryParameter("type", "student_food_menu")
            .addQueryParameter("list_date", "%04d%02d".format(ay.year, ay.monthValue))
            .addQueryParameter("sort_by", "date_asc")
            .build()
        val istek = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .build()
        client.newCall(istek).execute().use { yanit ->
            if (!yanit.isSuccessful) throw IOException("SKS yanıtı: HTTP ${yanit.code}")
            yanit.body?.string() ?: throw IOException("SKS yanıtı boş")
        }
    }
}
