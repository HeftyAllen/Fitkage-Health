package com.example.fitkagehealth.model

data class WeatherData(
    val address: String,
    val updatedAt: String,
    val temperature: String,
    val feelsLike: String,
    val tempMin: String,
    val tempMax: String,
    val pressure: String,
    val humidity: String,
    val windSpeed: String,
    val weatherDescription: String,
    val weatherIcon: String,
    val sunrise: String,
    val sunset: String,
    val visibility: Int,
    val conditionId: Int
)