package edu.singaporetech.inf2007quiz01.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpressionHistoryDao {
    @Insert
    suspend fun insert(history: ExpressionHistory)

    @Query("SELECT * FROM expression_history WHERE calBotId = :calBotId ORDER BY id DESC")
    fun getHistoryByCalBot(calBotId: Int): Flow<List<ExpressionHistory>>

    @Query("DELETE FROM expression_history WHERE id = (SELECT id FROM expression_history WHERE calBotId = :calBotId ORDER BY id ASC LIMIT 1)")
    suspend fun deleteOldestForCalBot(calBotId: Int)

    @Query("SELECT COUNT(*) FROM expression_history WHERE calBotId = :calBotId")
    suspend fun countForCalBot(calBotId: Int): Int
}
