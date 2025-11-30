package com.example.fitkagehealth.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class RecipeHistory(
    val id: String = "",
    val userId: String = "",
    val recipeId: Int = 0,
    val recipeTitle: String = "",
    val imageUrl: String? = null,
    val calories: Int = 0,
    val protein: Int = 0,
    val carbs: Int = 0,
    val fat: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val rating: Int? = null,
    val notes: String = ""
) : Parcelable