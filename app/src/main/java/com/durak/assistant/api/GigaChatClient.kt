package com.durak.assistant.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object GigaChatClient {
    private const val BASE_URL = "https://gigachat.devices.sberbank.ru/"
    
    // ВНИМАНИЕ: Пользователь должен заменить это на свой Client ID / Client Secret или готовый токен
    private const val AUTH_KEY = "YOUR_GIGACHAT_AUTH_KEY"

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(logging)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .client(okHttpClient)
        .build()

    val api: GigaChatApi = retrofit.create(GigaChatApi::class.java)

    fun getAuthToken(): String {
        // В идеале здесь должен быть запрос на получение токена через OAuth
        // Для упрощения возвращаем заготовленный ключ
        return "Bearer $AUTH_KEY"
    }
}
