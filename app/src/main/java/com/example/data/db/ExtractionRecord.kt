package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "extraction_records")
data class ExtractionRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val workspaceId: Int? = null,
    val workspaceName: String? = null,
    val fileName: String,
    val extractedJson: String, // Stringified JSON representation of the extraction
    val timestamp: Long = System.currentTimeMillis()
)
