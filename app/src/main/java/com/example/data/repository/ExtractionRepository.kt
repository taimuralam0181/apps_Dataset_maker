package com.example.data.repository

import com.example.data.db.ExtractionDao
import com.example.data.db.ExtractionRecord
import kotlinx.coroutines.flow.Flow

class ExtractionRepository(private val dao: ExtractionDao) {
    val allExtractions: Flow<List<ExtractionRecord>> = dao.getAllExtractions()

    fun getExtractionsForWorkspace(workspaceId: Int): Flow<List<ExtractionRecord>> =
        dao.getExtractionsByWorkspace(workspaceId)

    suspend fun insert(record: ExtractionRecord) {
        dao.insertExtraction(record)
    }

    suspend fun delete(id: Int) {
        dao.deleteExtractionById(id)
    }

    suspend fun clearAll() {
        dao.clearAllExtractions()
    }
}
