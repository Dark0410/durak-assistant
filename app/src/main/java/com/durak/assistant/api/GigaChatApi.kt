package com.durak.assistant.api

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface GigaChatApi {
    @POST("v1/chat/completions")
    fun getChatCompletion(
        @Header("Authorization") token: String,
        @Body request: GigaChatRequest
    ): Call<GigaChatResponse>
}

data class GigaChatRequest(
    val model: String = "GigaChat",
    val messages: List<Message>
)

data class Message(val role: String, val content: String)

data class GigaChatResponse(
    val choices: List<Choice>
)

data class Choice(val message: Message)
