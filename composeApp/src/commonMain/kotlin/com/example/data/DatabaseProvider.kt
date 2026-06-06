package com.example.data

expect object DatabaseProvider {
    fun getDatabase(): AppDatabase
}
