package com.example.data

import android.content.Context
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers

actual object DatabaseProvider {
    private var db: AppDatabase? = null
    
    fun init(context: Context) {
        if (db == null) {
            val dbFile = context.getDatabasePath("studygram_database.db")
            db = Room.databaseBuilder<AppDatabase>(
                context = context.applicationContext,
                name = dbFile.absolutePath
            )
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .fallbackToDestructiveMigration(true)
            .build()
        }
    }
    
    actual fun getDatabase(): AppDatabase {
        return db ?: throw IllegalStateException("DatabaseProvider must be initialized first")
    }
}
