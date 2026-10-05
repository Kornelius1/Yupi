package com.example.yupi.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface WordLogDao {

    @Query(
        "SELECT * FROM daily_word_logs " +
                "WHERE date = :date LIMIT 1"
    )
    suspend fun getLogByDate(
        date: String
    ): WordLogEntity?

    @Insert(
        onConflict = OnConflictStrategy.REPLACE
    )
    suspend fun insertOrUpdate(
        log: WordLogEntity
    )

    @Query(
        "UPDATE daily_word_logs " +
                "SET isSynced = :isSynced " +
                "WHERE date = :date"
    )
    suspend fun updateSyncStatus(
        date: String,
        isSynced: Boolean
    )
}