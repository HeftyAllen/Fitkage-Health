package com.example.fitkagehealth.model

data class WeatherActivity(
    val name: String,
    val icon: Int,
    val score: Int,
    val recommendation: String,
    val isRecommended: Boolean
)