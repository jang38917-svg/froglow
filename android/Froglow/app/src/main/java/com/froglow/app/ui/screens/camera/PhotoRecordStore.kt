package com.froglow.app.ui.screens.camera

data class PhotoRecord(
    val date: String,
    val photoUris: List<String>
)

object PhotoRecordStore {
    private val records = mutableMapOf<String, PhotoRecord>()

    fun save(record: PhotoRecord) {
        records[record.date] = record
    }

    fun get(date: String): PhotoRecord? = records[date]

    fun getAll(): List<PhotoRecord> = records.values.toList()
}