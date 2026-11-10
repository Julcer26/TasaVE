package com.julcer26.tasave.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "conversion_history")
data class ConversionHistory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val amountFrom: Double,
    val currencyFrom: String,
    val amountTo: Double,
    val currencyTo: String,
    val rate: Double,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface ConversionHistoryDao {
    @Query("SELECT * FROM conversion_history ORDER BY timestamp DESC LIMIT 30")
    fun getHistory(): Flow<List<ConversionHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(history: ConversionHistory)

    @Query("DELETE FROM conversion_history")
    suspend fun clearAll()
}
