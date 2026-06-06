package com.example.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.parameter
import io.ktor.http.ContentType
import io.ktor.http.contentType

class GeminiApiService(private val client: HttpClient) {
    suspend fun generateContent(
        model: String,
        apiKey: String,
        request: GenerateContentRequest
    ): GenerateContentResponse {
        return client.post("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent") {
            parameter("key", apiKey)
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }
}
