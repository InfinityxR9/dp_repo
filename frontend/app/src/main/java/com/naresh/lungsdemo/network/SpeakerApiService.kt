package com.naresh.lungsdemo.network

import com.naresh.lungsdemo.model.ApiResponse
import com.naresh.lungsdemo.model.HealthResponse
import com.naresh.lungsdemo.model.PlayRequest
import com.naresh.lungsdemo.model.StatusResponse
import com.naresh.lungsdemo.model.VolumeRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface SpeakerApiService {

    @GET("api/health")
    suspend fun health(): Response<HealthResponse>


    @GET("api/state")
    suspend fun getStatus(): Response<StatusResponse>


    @POST("api/play")
    suspend fun playSound(
        @Body playRequest: PlayRequest
    ): Response<ApiResponse>


    @POST("api/stop")
    suspend fun stopSound(): Response<ApiResponse>

    @POST("api/volume")
    suspend fun setVolume(
        @Body volumeRequest: VolumeRequest
    ): Response<ApiResponse>

    @POST("api/select/{location}")
    suspend fun selectLocation(
        @Path("location") location: Int
    ): Response<ApiResponse>

    @POST("api/test/song")
    suspend fun testSong(): Response<ApiResponse>

}