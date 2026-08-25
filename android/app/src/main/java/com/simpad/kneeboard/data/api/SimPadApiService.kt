package com.simpad.kneeboard.data.api

import com.simpad.kneeboard.data.models.KneeboardTab
import com.simpad.kneeboard.data.models.Profile
import com.simpad.kneeboard.data.models.ServerStatus
import com.simpad.kneeboard.data.models.TelemetryData
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming

interface SimPadApiService {

    @GET("/api/status")
    suspend fun getServerStatus(): Response<ServerStatus>

    @GET("/api/telemetry")
    suspend fun getCurrentTelemetry(): Response<TelemetryData>

    @GET("/api/profiles")
    suspend fun getProfiles(): Response<List<Profile>>

    @GET("/api/profiles/active")
    suspend fun getActiveProfile(): Response<Profile>

    @POST("/api/profiles/active/{id}")
    suspend fun setActiveProfile(@Path("id") profileId: String): Response<Profile>

    @GET("/api/kneeboard/tabs")
    suspend fun getKneeboardTabs(@Query("profileId") profileId: String? = null): Response<List<KneeboardTab>>

    @Streaming
    @GET("/api/kneeboard/pages/content")
    suspend fun getPageContent(
        @Query("path") path: String,
        @Query("page") pageNumber: Int = 1
    ): Response<ResponseBody>
}
