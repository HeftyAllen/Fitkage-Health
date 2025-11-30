package com.example.fitkagehealth.db

import androidx.room.*
import com.example.fitkagehealth.model.UserWorkout
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkout(workout: UserWorkout): Long

    @Query("SELECT * FROM user_workouts ORDER BY id DESC")
    fun getAllWorkouts(): Flow<List<UserWorkout>>

    @Query("SELECT * FROM user_workouts WHERE id = :id")
    suspend fun getWorkoutById(id: Int): UserWorkout?

    @Delete
    suspend fun deleteWorkout(workout: UserWorkout): Int
}
