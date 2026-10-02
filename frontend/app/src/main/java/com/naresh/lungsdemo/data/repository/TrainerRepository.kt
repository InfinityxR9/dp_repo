package com.naresh.lungsdemo.data.repository

import com.naresh.lungsdemo.model.PlayRequest
import com.naresh.lungsdemo.model.VolumeRequest
import com.naresh.lungsdemo.network.SpeakerApiService

class TrainerRepository(
    private val api: SpeakerApiService
) {

    suspend fun playSound(soundId: String) =
        api.playSound(PlayRequest(soundId))

    suspend fun stopSound() =
        api.stopSound()

    suspend fun setVolume(volume: Float) =
        api.setVolume(VolumeRequest(volume))

    suspend fun getStatus() =
        api.getStatus()

    suspend fun health() =
        api.health()

    suspend fun selectLocation(location: Int) =
        api.selectLocation(location)

    suspend fun testSong() =
        api.testSong()
}