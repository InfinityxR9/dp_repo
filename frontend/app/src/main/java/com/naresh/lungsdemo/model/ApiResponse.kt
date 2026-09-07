package com.naresh.lungsdemo.model

data class ApiResponse(
    val success: Boolean,
    val message: String,
    val soundId: String? = null,
    val location: Int? = null
)