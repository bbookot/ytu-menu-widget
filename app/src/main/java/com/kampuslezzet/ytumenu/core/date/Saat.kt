package com.kampuslezzet.ytumenu.core.date

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId

/** Türkiye saatine sabitlenmiş saat. Testlerde sahte saat verilebilir. */
interface Saat {
    fun simdi(): LocalDateTime
    fun bugun(): LocalDate = simdi().toLocalDate()
}

object TurkiyeSaati : Saat {
    val BOLGE: ZoneId = ZoneId.of("Europe/Istanbul")
    override fun simdi(): LocalDateTime = LocalDateTime.now(BOLGE)
    fun epochMilis(zaman: LocalDateTime): Long = zaman.atZone(BOLGE).toInstant().toEpochMilli()
    fun tarih(epochMilis: Long): LocalDate = Instant.ofEpochMilli(epochMilis).atZone(BOLGE).toLocalDate()
    fun zaman(epochMilis: Long): LocalDateTime = Instant.ofEpochMilli(epochMilis).atZone(BOLGE).toLocalDateTime()
}

/** Türkçe tarih biçimleri (cihaz dilinden bağımsız, sabit). */
object TurkceTarih {
    private val AYLAR = listOf(
        "Ocak", "Şubat", "Mart", "Nisan", "Mayıs", "Haziran",
        "Temmuz", "Ağustos", "Eylül", "Ekim", "Kasım", "Aralık",
    )
    private val GUNLER = listOf("Pazartesi", "Salı", "Çarşamba", "Perşembe", "Cuma", "Cumartesi", "Pazar")

    fun ayAdi(ay: YearMonth): String = AYLAR[ay.monthValue - 1]
    fun gunAdi(tarih: LocalDate): String = GUNLER[tarih.dayOfWeek.value - 1]

    /** "28 Ekim Çarşamba" */
    fun uzun(tarih: LocalDate): String = "${tarih.dayOfMonth} ${AYLAR[tarih.monthValue - 1]} ${gunAdi(tarih)}"

    /** "Pazartesi, 2 Kasım" */
    fun gunVeTarih(tarih: LocalDate): String = "${gunAdi(tarih)}, ${tarih.dayOfMonth} ${AYLAR[tarih.monthValue - 1]}"
}

fun LocalDate.haftaSonuMu(): Boolean = dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY
