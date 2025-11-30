package com.example.fitkagehealth.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ExercisePlan(
    val exercise: Exercise,
    var sets: Int = 3,
    var reps: Int = 12,
    var restTime: Int = 60,
    var restSeconds: Int = 60,
    var completed: Boolean = false,
    var actualReps: List<Int> = emptyList()
) : Parcelable
