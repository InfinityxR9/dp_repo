package com.naresh.lungsdemo.model

data class StatusResponse(
    val running: Boolean,
    val activeSensor: Int?,
    val pressure: Int,
    val soundId: String?,
    val audioVolume: Float,
    val masterMultiplier: Float
)