package com.example.fitkagehealth.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "exercises")
data class Exercise(
    @PrimaryKey val id: String,
    val name: String,
    val bodyPart: String,
    val equipment: String? = null,
    val gifUrl: String? = null,
    val target: String? = null,
    val muscleGroup: String? = null,
    val instructions: List<String> = emptyList()
) : Parcelable
