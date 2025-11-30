package com.example.fitkagehealth.model

import androidx.room.Entity
import androidx.room.PrimaryKey
@Entity(tableName = "user_workouts")
data class UserWorkout(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val exercises: List<ExercisePlan> = emptyList(),
    val date: Long = System.currentTimeMillis(),
    val rating: Int? = null
)

