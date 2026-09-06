package com.naresh.lungsdemo.ui

data class Question(
    val question: String,
    val options: List<String>,
    val correctAnswer: String
)