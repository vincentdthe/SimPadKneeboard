package com.simpad.kneeboard.data.repository

import android.content.Context
import android.util.Log
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.simpad.kneeboard.data.api.SimPadApiService
import com.simpad.kneeboard.data.models.KneeboardPage
import com.simpad.kneeboard.data.models.KneeboardTab
import com.simpad.kneeboard.data.models.Profile
import com.simpad.kneeboard.data.models.ServerStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class KneeboardRepository(private val context: Context) {
    private val tag = "KneeboardRepository"
    private var currentHost: String = "192.168.1.100"
    private var currentPort: Int = 8090
    private var apiService: SimPadApiService? = null

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val docCacheDir: File by lazy {
        File(context.cacheDir, "kneeboard_docs").apply {
            if (!exists()) mkdirs()
        }
    }

    fun configureServer(host: String, port: Int = 8090) {
        currentHost = host
        currentPort = port

        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("http://$host:$port")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

        apiService = retrofit.create(SimPadApiService::class.java)
    }

    suspend fun getServerStatus(): Result<ServerStatus> = withContext(Dispatchers.IO) {
        try {
            val service = apiService ?: return@withContext Result.failure(IllegalStateException("Server not configured"))
            val response = service.getServerStatus()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("HTTP Error ${response.code()}: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTabs(profileId: String? = null): Result<List<KneeboardTab>> = withContext(Dispatchers.IO) {
        try {
            val service = apiService ?: return@withContext Result.failure(IllegalStateException("Server not configured"))
            val response = service.getKneeboardTabs(profileId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("HTTP Error ${response.code()}: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getProfiles(): Result<List<Profile>> = withContext(Dispatchers.IO) {
        try {
            val service = apiService ?: return@withContext Result.failure(IllegalStateException("Server not configured"))
            val response = service.getProfiles()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("HTTP Error ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun setActiveProfile(profileId: String): Result<Profile> = withContext(Dispatchers.IO) {
        try {
            val service = apiService ?: return@withContext Result.failure(IllegalStateException("Server not configured"))
            val response = service.setActiveProfile(profileId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("HTTP Error ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchAndCachePage(page: KneeboardPage): Result<File> = withContext(Dispatchers.IO) {
        try {
            val cacheKey = "${page.id}_p${page.pdfPageNumber}"
            val cachedFile = File(docCacheDir, "$cacheKey.bin")

            if (cachedFile.exists() && cachedFile.length() > 0) {
                return@withContext Result.success(cachedFile)
            }

            val service = apiService ?: return@withContext Result.failure(IllegalStateException("Server not configured"))
            val response = service.getPageContent(page.filePath, page.pdfPageNumber)

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                body.byteStream().use { input ->
                    FileOutputStream(cachedFile).use { output ->
                        input.copyTo(output)
                    }
                }
                Result.success(cachedFile)
            } else {
                Result.failure(Exception("Failed to download page: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e(tag, "Error fetching page content: ${e.message}")
            Result.failure(e)
        }
    }

    fun getPageContentUrl(page: KneeboardPage): String {
        return "http://$currentHost:$currentPort/api/kneeboard/pages/content?path=${page.filePath}&page=${page.pdfPageNumber}"
    }

    fun getDirectUrl(relativeOrAbsolute: String): String {
        return if (relativeOrAbsolute.startsWith("http://") || relativeOrAbsolute.startsWith("https://")) {
            relativeOrAbsolute
        } else {
            "http://$currentHost:$currentPort$relativeOrAbsolute"
        }
    }
}
