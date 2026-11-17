package com.azxnism.waron.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.azxnism.waron.data.model.Finding
import com.azxnism.waron.data.model.ScanRecord

@Database(
    entities = [ScanRecord::class, Finding::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun scanDao(): ScanDao
}
