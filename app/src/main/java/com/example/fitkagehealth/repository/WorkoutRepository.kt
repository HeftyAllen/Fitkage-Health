package com.example.fitkagehealth.repository

import android.util.Log
import com.example.fitkagehealth.api.ExerciseApi
import com.example.fitkagehealth.db.ExerciseDao
import com.example.fitkagehealth.db.WorkoutPlanDao
import com.example.fitkagehealth.db.WorkoutSessionDao
import com.example.fitkagehealth.model.Exercise
import com.example.fitkagehealth.model.WorkoutPlan
import com.example.fitkagehealth.model.WorkoutSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class WorkoutRepository(
    private val api: ExerciseApi,
    private val exerciseDao: ExerciseDao,
    private val workoutPlanDao: WorkoutPlanDao,
    private val workoutSessionDao: WorkoutSessionDao
) {

    companion object {
        private const val TAG = "WorkoutRepository"
    }

    // Enhanced body part mapping with better coverage
    private val bodyPartMapping: Map<String, List<String>> = mapOf(
        "chest" to listOf("chest", "pectorals", "upper chest", "lower chest", "pecs"),
        "arms" to listOf("biceps", "triceps", "forearms", "arm", "bicep", "tricep"),
        "shoulders" to listOf("shoulders", "delts", "deltoid", "shoulder"),
        "back" to listOf("back", "lats", "rhomboids", "traps", "upper back", "lower back", "latissimus", "trapezius"),
        "legs" to listOf("quads", "hamstrings", "glutes", "calves", "thighs", "quadriceps", "hamstring", "calf"),
        "thighs" to listOf("quads", "hamstrings", "thighs", "quadriceps", "hamstring"),
        "calves" to listOf("calves", "calf"),
        "glutes" to listOf("glutes", "glute"),
        "abs" to listOf("abs", "abdominals", "core", "abdominal", "abdomen"),
        "obliques" to listOf("obliques", "oblique"),
        "cardio" to listOf("cardio", "cardiovascular"),
        "hiit" to listOf("hiit", "high intensity interval training"),
        "full body" to listOf("full body", "full-body", "full body workout"),
        "yoga" to listOf("yoga"),
        "pilates" to listOf("pilates"),
        "stretching" to listOf("stretching", "stretch", "flexibility"),
        "balance" to listOf("balance", "balancing"),
        "neck" to listOf("neck", "traps", "trapezius"),
        "endurance" to listOf("endurance", "stamina")
    )

    fun getExercisesByBodyPart(bodyPart: String): Flow<List<Exercise>> {
        val key = bodyPart.trim().lowercase()
        val searchTerms = bodyPartMapping[key] ?: listOf(key)

        Log.d(TAG, "Searching exercises for bodyPart: $key, terms: $searchTerms")

        return exerciseDao.getAllExercises().map { allExercises ->
            val filtered = allExercises.filter { exercise ->
                val exerciseBodyPart = exercise.bodyPart?.trim()?.lowercase() ?: ""
                val exerciseTarget = exercise.target?.trim()?.lowercase() ?: ""
                val exerciseName = exercise.name?.trim()?.lowercase() ?: ""

                searchTerms.any { term ->
                    exerciseBodyPart.contains(term) ||
                            exerciseTarget.contains(term) ||
                            exerciseName.contains(term)
                }
            }
            Log.d(TAG, "Filtered ${filtered.size} exercises for $key")
            filtered
        }
    }

    fun searchExercises(query: String): Flow<List<Exercise>> {
        val searchQuery = query.trim().lowercase()
        Log.d(TAG, "Searching exercises with query: $searchQuery")

        return if (searchQuery.isEmpty()) {
            exerciseDao.getAllExercises().map { it.take(20) }
        } else {
            exerciseDao.searchExercises(searchQuery)
        }
    }

    fun getAllExercises(): Flow<List<Exercise>> = exerciseDao.getAllExercises()

    suspend fun insertExercises(exercises: List<Exercise>) {
        withContext(Dispatchers.IO) {
            exerciseDao.insertAll(exercises)
        }
    }

    fun getAllWorkoutPlans(): Flow<List<WorkoutPlan>> = workoutPlanDao.getAllWorkoutPlans()

    suspend fun refreshExercisesFromApi() {
        try {
            Log.d(TAG, "refreshExercisesFromApi: calling API")
            val response = api.getExercises(1000)
            Log.d(TAG, "refreshExercisesFromApi: code=${response.code()} message=${response.message()}")
            if (response.isSuccessful) {
                response.body()?.let { exercises ->
                    if (exercises.isEmpty()) {
                        Log.w(TAG, "WorkoutRepository: API returned 0 exercises")
                    } else {
                        Log.d(TAG, "WorkoutRepository: API returned ${exercises.size} exercises")
                        withContext(Dispatchers.IO) {
                            try {
                                exerciseDao.insertAll(exercises)
                                Log.d(TAG, "WorkoutRepository: inserted ${exercises.size} exercises into DB")
                            } catch (t: Throwable) {
                                Log.e(TAG, "WorkoutRepository: failed to insert exercises", t)
                            }
                        }
                    }
                }
            } else {
                Log.e(TAG, "WorkoutRepository: API call failed: ${response.code()} ${response.message()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "WorkoutRepository: API exception: ${e.message}", e)
        }
    }

    fun getCustomWorkoutPlans(): Flow<List<WorkoutPlan>> = workoutPlanDao.getCustomWorkoutPlans()

    suspend fun saveWorkoutPlan(workoutPlan: WorkoutPlan) = workoutPlanDao.insert(workoutPlan)

    suspend fun updateWorkoutPlan(workoutPlan: WorkoutPlan) = workoutPlanDao.update(workoutPlan)

    suspend fun deleteWorkoutPlan(workoutPlan: WorkoutPlan) = workoutPlanDao.delete(workoutPlan)

    suspend fun saveWorkoutSession(session: WorkoutSession) = workoutSessionDao.insert(session)

    fun getAllWorkoutSessions(): Flow<List<WorkoutSession>> = workoutSessionDao.getAllSessions()

    // New method for workout planner with better search
    fun getExercisesForWorkoutPlanner(bodyPart: String): Flow<List<Exercise>> {
        return exerciseDao.getExercisesForWorkoutPlanner(bodyPart)
    }
}