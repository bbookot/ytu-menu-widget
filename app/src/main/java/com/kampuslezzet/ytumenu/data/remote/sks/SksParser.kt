package com.kampuslezzet.ytumenu.data.remote.sks

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jsoup.Jsoup
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Serializable
data class SksGunDto(
    val date: String,
    @SerialName("lunch_menu") val ogleNormal: String? = null,
    @SerialName("lunch2_menu") val ogleVejetaryen: String? = null,
    @SerialName("dinner_menu") val aksamNormal: String? = null,
    @SerialName("dinner2_menu") val aksamVejetaryen: String? = null,
)

/** Bir günün ham (temizlenmiş satır listesi) menüsü. */
data class HamGunMenusu(
    val tarih: LocalDate,
    val ogleNormal: List<String>,
    val ogleVejetaryen: List<String>,
    val aksamNormal: List<String>,
    val aksamVejetaryen: List<String>,
) {
    val bosMu: Boolean
        get() = ogleNormal.isEmpty() && ogleVejetaryen.isEmpty() && aksamNormal.isEmpty() && aksamVejetaryen.isEmpty()
}

/**
 * sks.yildiz.edu.tr/api/food-menu yanıtını ayrıştırır.
 * Yanıt bir JSON dizisidir; menü alanları "<p>SATIR</p>" biçiminde HTML içerir.
 * Yayınlanmamış ay için boş dizi ("[]") döner.
 */
object SksParser {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val TARIH = DateTimeFormatter.ofPattern("dd-MM-yyyy")

    fun coz(govde: String): List<HamGunMenusu> {
        val metin = govde.trim()
        if (metin.isEmpty() || !metin.startsWith("[")) return emptyList()
        return json.decodeFromString<List<SksGunDto>>(metin)
            .mapNotNull { dto ->
                val tarih = runCatching { LocalDate.parse(dto.date.trim(), TARIH) }.getOrNull() ?: return@mapNotNull null
                HamGunMenusu(
                    tarih = tarih,
                    ogleNormal = satirlar(dto.ogleNormal),
                    ogleVejetaryen = satirlar(dto.ogleVejetaryen),
                    aksamNormal = satirlar(dto.aksamNormal),
                    aksamVejetaryen = satirlar(dto.aksamVejetaryen),
                ).takeUnless { it.bosMu }
            }
            .distinctBy { it.tarih }
    }

    /** HTML parçasını satırlara böler; <p>, <br> ve <li> ayraç kabul edilir. */
    fun satirlar(html: String?): List<String> {
        if (html.isNullOrBlank()) return emptyList()
        val belge = Jsoup.parseBodyFragment(html)
        belge.select("br").after("\n")
        belge.select("p, li, div").forEach { it.appendText("\n") }
        return belge.body().wholeText()
            .split('\n')
            .map { it.replace('\u00A0', ' ').replace(Regex("\\s+"), " ").trim() }
            .filter { it.isNotEmpty() }
    }
}
