package com.example.fitkagehealth.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import java.util.UUID

@Entity(tableName = "workout_plans")
@Parcelize
data class WorkoutPlan(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String = "",
    val exercises: List<ExercisePlan> = emptyList(),
    val totalDuration: Long = 0,
    val difficulty: String = "Medium",
    val createdAt: Long = System.currentTimeMillis(),
    val isCustom: Boolean = false
) : Parcelable
