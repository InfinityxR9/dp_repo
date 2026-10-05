package com.naresh.lungsdemo.model

data class LungSound(
    val id: String,
    val name: String,
    val location: AuscultationLocation? = null
)