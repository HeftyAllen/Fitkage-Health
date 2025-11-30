package com.example.fitkagehealth.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import java.util.UUID

@Parcelize
@Entity(tableName = "workout_sessions")
data class WorkoutSession(
    @PrimaryKey val id: String = UUID.randomUUID().toString(), // <-- default ID
    val workoutPlan: WorkoutPlan,
    val startTime: Long = System.currentTimeMillis(),
    var endTime: Long = 0,
    var totalDuration: Long = 0,
    var completedExercises: Int = 0,
    var rating: Int = 0,
    var notes: String = "",
    var totalExercises: Int = 0,  // Add this
    var caloriesBurned: Int = 0
) : Parcelable
