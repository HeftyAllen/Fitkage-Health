package com.example.fitkagehealth.api

import com.example.fitkagehealth.model.Exercise
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface ExerciseApi {
    @GET("exercises")
    suspend fun getExercises(@Query("limit") limit: Int = 1000): Response<List<Exercise>>
}
