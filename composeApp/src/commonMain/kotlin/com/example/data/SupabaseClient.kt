package com.example.data

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.SessionManager
import io.github.jan.supabase.auth.user.UserSession
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.functions.Functions
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
class DatabaseSessionManager : SessionManager {
    override suspend fun saveSession(session: UserSession) {
        try {
            val json = Json.encodeToString(UserSession.serializer(), session)
            val db = DatabaseProvider.getDatabase()
            val dao = db.userProfileDao()
            val profile = dao.getUserProfileDirect()
            if (profile == null) {
                dao.insertProfile(
                    UserProfile(
                        id = "local_user",
                        username = session.user?.email?.substringBefore("@") ?: "User",
                        nursingField = "General",
                        avatarColor = 0xFF8B5CF6.toInt(),
                        supabaseSessionJson = json
                    )
                )
            } else {
                dao.updateSupabaseSessionJson(json)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override suspend fun loadSession(): UserSession? {
        return try {
            val db = DatabaseProvider.getDatabase()
            val profile = db.userProfileDao().getUserProfileDirect()
            val json = profile?.supabaseSessionJson ?: ""
            if (json.isNotEmpty()) {
                Json.decodeFromString(UserSession.serializer(), json)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override suspend fun deleteSession() {
        try {
            val db = DatabaseProvider.getDatabase()
            db.userProfileDao().updateSupabaseSessionJson("")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

object SupabaseApi {
    val client: SupabaseClient = createSupabaseClient(
        supabaseUrl = Constants.SUPABASE_URL,
        supabaseKey = Constants.SUPABASE_ANON_KEY
    ) {
        install(Auth) {
            sessionManager = DatabaseSessionManager()
        }
        install(Postgrest)
        install(Realtime)
        install(Storage)
        install(Functions)
    }
}
