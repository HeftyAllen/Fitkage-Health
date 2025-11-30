package com.example.fitkagehealth.db

import android.util.Log
import androidx.room.TypeConverter
import com.example.fitkagehealth.model.*
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

object Converters {
    private val gson = Gson()
    private const val TAG = "Converters"

    private inline fun <reified T> fromJsonSafe(json: String?): T? {
        if (json.isNullOrEmpty()) return null
        return try {
            val type: Type = object : TypeToken<T>() {}.type
            gson.fromJson<T>(json, type)
        } catch (e: JsonSyntaxException) {
            Log.w(TAG, "JSON parse failed for ${T::class.java.simpleName}: $json", e)
            null
        } catch (e: Exception) {
            Log.w(TAG, "Unexpected parse error for ${T::class.java.simpleName}", e)
            null
        }
    }

    private fun toJsonSafe(any: Any?): String? {
        if (any == null) return null
        return try {
            gson.toJson(any)
        } catch (e: Exception) {
            Log.w(TAG, "JSON serialization failed for ${any::class.java.simpleName}", e)
            null
        }
    }

    // ---------------------
    // List<String>
    // ---------------------
    @TypeConverter
    @JvmStatic
    fun fromStringList(value: List<String>?): String = toJsonSafe(value) ?: "[]"

    @TypeConverter
    @JvmStatic
    fun toStringList(value: String?): List<String> =
        fromJsonSafe<List<String>>(value) ?: emptyList()

    // ---------------------
    // List<Int>
    // ---------------------
    @TypeConverter
    @JvmStatic
    fun fromIntList(value: List<Int>?): String = toJsonSafe(value) ?: "[]"

    @TypeConverter
    @JvmStatic
    fun toIntList(value: String?): List<Int> =
        fromJsonSafe<List<Int>>(value) ?: emptyList()

    // ---------------------
    // List<Long>
    // ---------------------
    @TypeConverter
    @JvmStatic
    fun fromLongList(value: List<Long>?): String = toJsonSafe(value) ?: "[]"

    @TypeConverter
    @JvmStatic
    fun toLongList(value: String?): List<Long> =
        fromJsonSafe<List<Long>>(value) ?: emptyList()

    // ---------------------
    // List<ExercisePlan>
    // ---------------------
    @TypeConverter
    @JvmStatic
    fun fromExercisePlanList(value: List<ExercisePlan>?): String = toJsonSafe(value) ?: "[]"

    @TypeConverter
    @JvmStatic
    fun toExercisePlanList(value: String?): List<ExercisePlan> =
        fromJsonSafe<List<ExercisePlan>>(value) ?: emptyList()

    // ---------------------
    // WorkoutPlan object
    // ---------------------
    @TypeConverter
    @JvmStatic
    fun fromWorkoutPlan(value: WorkoutPlan?): String? = toJsonSafe(value)

    @TypeConverter
    @JvmStatic
    fun toWorkoutPlan(value: String?): WorkoutPlan? =
        fromJsonSafe<WorkoutPlan>(value)

    // ---------------------
    // ExtendedIngredient (commonly appears in Recipe/food models)
    // ---------------------
    @TypeConverter
    @JvmStatic
    fun extendedIngredientsToJson(list: List<ExtendedIngredient>?): String = toJsonSafe(list) ?: "[]"

    @TypeConverter
    @JvmStatic
    fun jsonToExtendedIngredients(json: String?): List<ExtendedIngredient> =
        fromJsonSafe<List<ExtendedIngredient>>(json) ?: emptyList()

    // ---------------------
    // InstructionStep
    // ---------------------
    @TypeConverter
    @JvmStatic
    fun instructionStepsToJson(list: List<InstructionStep>?): String = toJsonSafe(list) ?: "[]"

    @TypeConverter
    @JvmStatic
    fun jsonToInstructionSteps(json: String?): List<InstructionStep> =
        fromJsonSafe<List<InstructionStep>>(json) ?: emptyList()

    // ---------------------
    // Nutrient (used inside Nutrition)
    // ---------------------
    @TypeConverter
    @JvmStatic
    fun nutrientsToJson(list: List<Nutrient>?): String = toJsonSafe(list) ?: "[]"

    @TypeConverter
    @JvmStatic
    fun jsonToNutrients(json: String?): List<Nutrient> =
        fromJsonSafe<List<Nutrient>>(json) ?: emptyList()
}
