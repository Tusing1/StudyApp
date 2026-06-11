package com.example.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        UserProfile::class,
        DiscussionMessage::class,
        QuizQuestion::class,
        Bookmark::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userProfileDao(): UserProfileDao
    abstract fun discussionMessageDao(): DiscussionMessageDao
    abstract fun quizQuestionDao(): QuizQuestionDao
    abstract fun bookmarkDao(): BookmarkDao
}
