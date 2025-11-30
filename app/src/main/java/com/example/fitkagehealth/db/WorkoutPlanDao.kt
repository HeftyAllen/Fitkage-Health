package com.example.fitkagehealth.db

import androidx.room.*
import com.example.fitkagehealth.model.WorkoutPlan
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutPlanDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(workoutPlan: WorkoutPlan)

    @Update
    suspend fun update(workoutPlan: WorkoutPlan)

    @Delete
    suspend fun delete(workoutPlan: WorkoutPlan)

    @Query("SELECT * FROM workout_plans")
    fun getAllWorkoutPlans(): Flow<List<WorkoutPlan>>

    @Query("SELECT * FROM workout_plans WHERE id = :id")
    suspend fun getWorkoutPlanById(id: String): WorkoutPlan?

    @Query("SELECT * FROM workout_plans WHERE isCustom = 1")
    fun getCustomWorkoutPlans(): Flow<List<WorkoutPlan>>
}
