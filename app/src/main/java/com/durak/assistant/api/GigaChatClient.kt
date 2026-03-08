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

    private fun getUnsafeOkHttpClient(): OkHttpClient {
        try {
            val trustAllCerts = arrayOf<javax.net.ssl.TrustManager>(object : javax.net.ssl.X509TrustManager {
                override fun checkClientTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {}
                override fun checkServerTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {}
                override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> = arrayOf()
            })

            val sslContext = javax.net.ssl.SSLContext.getInstance("SSL")
            sslContext.init(null, trustAllCerts, java.security.SecureRandom())
            val sslSocketFactory = sslContext.socketFactory

            val builder = OkHttpClient.Builder()
            builder.sslSocketFactory(sslSocketFactory, trustAllCerts[0] as javax.net.ssl.X509TrustManager)
            builder.hostnameVerifier { _, _ -> true }
            builder.addInterceptor(logging)
            builder.connectTimeout(30, TimeUnit.SECONDS)
            builder.readTimeout(30, TimeUnit.SECONDS)
            return builder.build()
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    private val okHttpClient = getUnsafeOkHttpClient()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .client(okHttpClient)
        .build()

    val api: GigaChatApi = retrofit.create(GigaChatApi::class.java)

    fun getAuthToken(keyFromSettings: String): String {
        // Если ключ пустой, используем заглушку (для предотвращения краша на пустом Bearer)
        val token = if (keyFromSettings.isEmpty()) "EMPTY_KEY" else keyFromSettings
        return "Bearer $token"
    }
}
