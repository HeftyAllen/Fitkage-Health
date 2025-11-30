package com.example.fitkagehealth.db

import androidx.room.*
import com.example.fitkagehealth.model.Exercise
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(exercises: List<Exercise>)

    @Query("SELECT * FROM exercises WHERE LOWER(bodyPart) LIKE '%' || LOWER(:bodyPart) || '%' LIMIT 20")
    fun getExercisesByBodyPart(bodyPart: String): Flow<List<Exercise>>

    @Query("""
        SELECT * FROM exercises 
        WHERE LOWER(name) LIKE '%' || LOWER(:query) || '%' 
           OR LOWER(bodyPart) LIKE '%' || LOWER(:query) || '%' 
           OR LOWER(target) LIKE '%' || LOWER(:query) || '%'
        LIMIT 50
    """)
    fun searchExercises(query: String): Flow<List<Exercise>>

    @Query("SELECT DISTINCT bodyPart FROM exercises WHERE bodyPart IS NOT NULL")
    fun getAllBodyParts(): Flow<List<String>>

    @Query("SELECT * FROM exercises WHERE id = :id")
    suspend fun getExerciseById(id: String): Exercise?

    @Query("SELECT * FROM exercises LIMIT 50")
    fun getAllExercises(): Flow<List<Exercise>>

    // New method for workout planner with better fallback
    @Query("""
        SELECT * FROM exercises 
        WHERE LOWER(bodyPart) LIKE '%' || LOWER(:bodyPart) || '%' 
           OR LOWER(target) LIKE '%' || LOWER(:bodyPart) || '%'
           OR LOWER(name) LIKE '%' || LOWER(:bodyPart) || '%'
        LIMIT 50
    """)
    fun getExercisesForWorkoutPlanner(bodyPart: String): Flow<List<Exercise>>
}