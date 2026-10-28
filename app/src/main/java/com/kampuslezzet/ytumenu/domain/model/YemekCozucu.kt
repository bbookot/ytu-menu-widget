package com.kampuslezzet.ytumenu.domain.model

/**
 * SKS'den gelen ham satırları ("TARHANA ÇORBA (120 kcal)") yemek kalemlerine çevirir.
 */
object YemekCozucu {

    private val KALORI = Regex("""\(\s*(\d+)\s*kcal\s*\)\s*$""", RegexOption.IGNORE_CASE)

    private val CORBA = listOf("ÇORBA")
    private val ICECEK = listOf("AYRAN", "MEYVE SUYU", "ŞALGAM", "LİMONATA", "KOLA", "SODA", "KOMPOSTO", "ŞERBET")
    private val YAN = listOf(
        "PİLAV", "MAKARNA", "SPAGETTİ", "ERİŞTE", "BULGUR", "PÜRE", "PATATES", "MANTI", "KUSKUS", "NOODLE",
    )
    private val SALATA = listOf("SALATA", "BÜFE", "CACIK", "TARATOR", "PİYAZ", "TURŞU", "YOĞURT", "ACILI EZME", "ÇİĞ KÖFTE")
    private val TATLI = listOf(
        "TATLI", "SÜTLAÇ", "REVANİ", "MEYVE", "KADAYIF", "BAKLAVA", "PUDİNG", "HELVA", "KEK", "MUHALLEBİ",
        "KAZANDİBİ", "TULUMBA", "LOKMA", "ŞEKERPARE", "AŞURE", "PASTA", "DONDURMA", "KEŞKÜL", "TRİLEÇE", "GÜLLAÇ",
    )

    /** Şablon satırları (örn. vejetaryen listesinin başındaki "ÖĞLE") elenir. */
    private val GURULTU = Regex("""^(ÖĞLE|AKŞAM|OGLE|AKSAM|NORMAL MENÜ|VEJETARYEN MENÜ|-+|\.+)$""", RegexOption.IGNORE_CASE)

    fun gurultuMu(satir: String): Boolean = satir.isBlank() || GURULTU.matches(satir.trim())

    fun coz(satirlar: List<String>): List<YemekKalemi> {
        var anaYemekBulundu = false
        return satirlar.filterNot(::gurultuMu).map { satir ->
            val eslesme = KALORI.find(satir)
            val kalori = eslesme?.groupValues?.get(1)?.toIntOrNull()
            var hamAd = (if (eslesme != null) satir.removeRange(eslesme.range) else satir).trim()
            
            // SKS vejetaryen menülerindeki "V." veya "V " öneklerini temizle
            hamAd = hamAd.replace(Regex("""^[vV]\.\s*|^[vV]\s+"""), "").trim()

            val buyuk = hamAd.uppercase(TR)
            val kategori = when {
                CORBA.any(buyuk::contains) -> Kategori.CORBA
                ICECEK.any(buyuk::contains) -> Kategori.ICECEK
                !anaYemekBulundu -> Kategori.ANA_YEMEK.also { anaYemekBulundu = true }
                YAN.any(buyuk::contains) -> Kategori.YAN_YEMEK
                SALATA.any(buyuk::contains) -> Kategori.SALATA
                TATLI.any(buyuk::contains) -> Kategori.TATLI_MEYVE
                else -> Kategori.DIGER
            }
            YemekKalemi(baslikHarfi(hamAd), kalori, kategori)
        }
    }

    /** "SPAGETTİ (soslu)" -> "Spagetti (soslu)"; Türkçe büyük/küçük harf kurallarıyla. */
    fun baslikHarfi(metin: String): String =
        metin.lowercase(TR)
            .split(' ')
            .joinToString(" ") { kelime ->
                if (kelime.isEmpty() || kelime.startsWith("(")) kelime
                else kelime.replaceFirstChar { it.titlecase(TR) }
            }
            .replace(Regex("""/(\p{L})""")) { "/" + it.groupValues[1].uppercase(TR) }
}
