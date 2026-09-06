package com.naresh.lungsdemo.network

import com.naresh.lungsdemo.model.ApiResponse
import com.naresh.lungsdemo.model.PlayRequest
import com.naresh.lungsdemo.model.StatusResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface SpeakerApiService {

    @POST("play")
    suspend fun playSound(
        @Body playRequest: PlayRequest
    ): Response<ApiResponse>

    @POST("stop")
    suspend fun stopSound(): Response<ApiResponse>

    @GET("status")
    suspend fun getStatus(): Response<StatusResponse>
}