package com.example.fitkagehealth.repository

import android.util.Log
import com.example.fitkagehealth.api.ExerciseApi
import com.example.fitkagehealth.db.ExerciseDao
import com.example.fitkagehealth.model.Exercise
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

class ExerciseRepository(
    private val api: ExerciseApi,
    private val dao: ExerciseDao
) {

    companion object {
        private const val TAG = "ExerciseRepository"
    }

    /**
     * Fetch remote, cache to DB, and emit DB -> remote result.
     * Consumers can collect to get cached + refreshed data.
     */
    fun getAllExercises(): Flow<List<Exercise>> = flow {
        // emit cached first
        val cached = try {
            dao.getAllExercises().first()
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to read cached exercises", t)
            emptyList<Exercise>()
        }
        emit(cached)

        try {
            Log.d(TAG, "Requesting exercises from API...")
            val response = api.getExercises()
            Log.d(TAG, "API response code=${response.code()} message=${response.message()}")
            if (response.isSuccessful) {
                val body = response.body() ?: emptyList()
                Log.d(TAG, "API returned ${body.size} exercises")
                if (body.isNotEmpty()) {
                    // cache and emit fresh
                    withContext(Dispatchers.IO) {
                        try {
                            dao.insertAll(body)
                            Log.d(TAG, "Inserted ${body.size} exercises into DB")
                        } catch (t: Throwable) {
                            Log.e(TAG, "Failed to insert exercises into DB", t)
                        }
                    }
                    emit(body)
                } else {
                    Log.w(TAG, "API returned empty list; keeping cached items")
                    emit(cached)
                }
            } else {
                Log.e(TAG, "API call failed: ${response.code()} ${response.message()}")
                emit(cached)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception calling API", e)
            emit(cached)
        }
    }

    /**
     * Explicit background fetch + cache. Useful for pull-to-refresh / explicit attempts.
     */
    suspend fun fetchAndCacheExercises() = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "fetchAndCacheExercises: calling API")
            val response = api.getExercises()
            Log.d(TAG, "fetchAndCacheExercises: code=${response.code()} message=${response.message()}")
            if (response.isSuccessful) {
                val body = response.body() ?: emptyList()
                Log.d(TAG, "fetchAndCacheExercises: API returned ${body.size} exercises")
                if (body.isNotEmpty()) {
                    dao.insertAll(body)
                    Log.d(TAG, "fetchAndCacheExercises: inserted ${body.size} exercises into DB")
                } else {
                    Log.w(TAG, "fetchAndCacheExercises: API returned 0 exercises")
                }
            } else {
                Log.e(TAG, "fetchAndCacheExercises: API call failed: ${response.code()} ${response.message()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "fetchAndCacheExercises: exception", e)
        }
    }

    fun getExercisesByBodyPart(bodyPart: String): Flow<List<Exercise>> =
        dao.getExercisesByBodyPart(bodyPart)

    fun searchExercises(query: String): Flow<List<Exercise>> =
        dao.searchExercises("%$query%")
}
