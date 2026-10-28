package com.kampuslezzet.ytumenu.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/**
 * Bir günün menüsü. Öğle/akşam × normal/vejetaryen listeleri satır satır ("\n") saklanır.
 * @property tarih ISO tarih ("2026-10-28")
 * @property yilAy "2026-10"
 */
@Entity(tableName = "menu")
data class MenuEntity(
    @PrimaryKey val tarih: String,
    val yilAy: String,
    val ogleNormal: String,
    val ogleVejetaryen: String,
    val aksamNormal: String,
    val aksamVejetaryen: String,
    val cekilmeZamani: Long,
)

@Dao
interface MenuDao {
    @Query("SELECT * FROM menu WHERE tarih = :tarih")
    suspend fun gun(tarih: String): MenuEntity?

    @Query("SELECT COUNT(*) FROM menu WHERE yilAy = :yilAy")
    suspend fun aydakiGunSayisi(yilAy: String): Int

    @Query("SELECT * FROM menu WHERE tarih >= :baslangic ORDER BY tarih LIMIT :adet")
    fun sonrakiGunler(baslangic: String, adet: Int): Flow<List<MenuEntity>>

    @Query("DELETE FROM menu WHERE yilAy = :yilAy")
    suspend fun aySil(yilAy: String)

    @Query("DELETE FROM menu WHERE tarih < :tarih")
    suspend fun eskileriSil(tarih: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun ekle(liste: List<MenuEntity>)

    @Transaction
    suspend fun ayiDegistir(yilAy: String, liste: List<MenuEntity>) {
        aySil(yilAy)
        ekle(liste)
    }
}

@Database(entities = [MenuEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun menuDao(): MenuDao

    companion object {
        fun olustur(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "ytumenu.db")
                .fallbackToDestructiveMigration(true)
                .build()
    }
}
