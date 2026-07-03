package com.example.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ExtractionDao {
    @Query("SELECT * FROM extraction_records ORDER BY timestamp DESC")
    fun getAllExtractions(): Flow<List<ExtractionRecord>>

    @Query("SELECT * FROM extraction_records WHERE workspaceId = :workspaceId ORDER BY timestamp DESC")
    fun getExtractionsByWorkspace(workspaceId: Int): Flow<List<ExtractionRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExtraction(record: ExtractionRecord)

    @Query("DELETE FROM extraction_records WHERE id = :id")
    suspend fun deleteExtractionById(id: Int)

    @Query("DELETE FROM extraction_records")
    suspend fun clearAllExtractions()
}
