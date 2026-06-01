package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profile WHERE id = 'local_user' LIMIT 1")
    fun getUserProfile(): Flow<UserProfile?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: UserProfile)

    @Query("DELETE FROM user_profile WHERE id = 'local_user'")
    suspend fun deleteProfile()
}

@Dao
interface DiscussionMessageDao {
    @Query("SELECT * FROM discussion_messages WHERE channelId = :channelId ORDER BY timestamp ASC")
    fun getMessagesForChannel(channelId: String): Flow<List<DiscussionMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: DiscussionMessage)

    @Query("DELETE FROM discussion_messages WHERE id = :id")
    suspend fun deleteMessageById(id: Int)

    @Query("UPDATE discussion_messages SET isBookmarked = :isBookmarked WHERE id = :id")
    suspend fun updateMessageBookmarkStatus(id: Int, isBookmarked: Boolean)
}

@Dao
interface QuizQuestionDao {
    @Query("SELECT * FROM quiz_questions WHERE subject = :subject")
    fun getQuestionsForSubject(subject: String): Flow<List<QuizQuestion>>

    @Query("SELECT * FROM quiz_questions")
    fun getAllQuestions(): Flow<List<QuizQuestion>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestions(questions: List<QuizQuestion>)

    @Query("SELECT * FROM quiz_questions WHERE id = :id LIMIT 1")
    suspend fun getQuestionById(id: Int): QuizQuestion?
}

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks ORDER BY timestamp DESC")
    fun getAllBookmarks(): Flow<List<Bookmark>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: Bookmark)

    @Query("DELETE FROM bookmarks WHERE referenceId = :refId AND type = :type")
    suspend fun deleteBookmarkByRef(refId: String, type: String)

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarks WHERE referenceId = :refId AND type = :type)")
    fun isBookmarked(refId: String, type: String): Flow<Boolean>
}
