package com.example.data

actual object DatabaseProvider {
    actual fun getDatabase(): AppDatabase {
        throw NotImplementedError("iOS database provider not fully implemented.")
    }
}
