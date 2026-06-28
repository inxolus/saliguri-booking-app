package com.example.network

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

data class SyncRequest(
    val action: String,
    val sheet: String,
    val data: Map<String, String>? = null,
    val payload: Any? = null,
    val id: String? = null,
    val idColumn: String? = null
)

data class SyncResponse(
    val success: Boolean,
    val message: String? = null,
    val downloaded: Int = 0,
    val uploaded: Int = 0,
    val errors: Int = 0,
    val data: List<Map<String, String>>? = null
)

data class ApiResponse<T>(
    val success: Boolean,
    val data: T?,
    val error: String?
)

interface SaliguriApi {
    @POST("macros/s/dummy_endpoint/exec")
    suspend fun postData(@Body request: SyncRequest): Response<SyncResponse>

    @GET("macros/s/dummy_endpoint/exec")
    suspend fun getAll(
        @Query("apiKey") apiKey: String,
        @Query("action") action: String,
        @Query("sheet") sheet: String
    ): Response<ApiResponse<List<Map<String, String>>>>

    @GET("macros/s/dummy_endpoint/exec")
    suspend fun getAvailableRooms(
        @Query("apiKey") apiKey: String,
        @Query("action") action: String,
        @Query("checkIn") checkIn: String,
        @Query("checkOut") checkOut: String
    ): Response<ApiResponse<List<Map<String, String>>>>
}

object RetrofitClient {
    private const val BASE_URL = "https://script.google.com/"
    const val API_KEY = "saliguri_7x9k2m_p4qR8vLw"

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .addInterceptor(loggingInterceptor)
        .build()

    val api: SaliguriApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(SaliguriApi::class.java)
    }
}
