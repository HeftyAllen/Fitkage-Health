package com.example.fitkagehealth

import android.content.Context
import android.util.Log

import com.example.fitkagehealth.api.ExerciseApi
import com.example.fitkagehealth.api.RetrofitClient
import com.example.fitkagehealth.db.AppDatabase
import com.example.fitkagehealth.db.ExerciseDao
import com.example.fitkagehealth.db.WorkoutPlanDao
import com.example.fitkagehealth.db.WorkoutSessionDao
import com.example.fitkagehealth.repository.ExerciseRepository
import com.example.fitkagehealth.repository.WorkoutRepository

object AppDependencies {

    private var exerciseApiInstance: ExerciseApi? = null
    private var appDatabaseInstance: AppDatabase? = null
    private var exerciseRepositoryInstance: ExerciseRepository? = null
    private var workoutRepositoryInstance: WorkoutRepository? = null

    private fun provideExerciseApi(): ExerciseApi {
        return exerciseApiInstance ?: RetrofitClient.exerciseApi.also {
            exerciseApiInstance = it
            Log.d("AppDependencies", "ExerciseApi instance created")
        }
    }

    private fun provideAppDatabase(context: Context): AppDatabase =
        appDatabaseInstance ?: AppDatabase.getInstance(context).also {
            appDatabaseInstance = it
            Log.d("AppDependencies", "AppDatabase instance created")
        }

    private fun provideExerciseDao(context: Context): ExerciseDao =
        provideAppDatabase(context).exerciseDao()

    private fun provideWorkoutPlanDao(context: Context): WorkoutPlanDao =
        provideAppDatabase(context).workoutPlanDao()

    private fun provideWorkoutSessionDao(context: Context): WorkoutSessionDao =
        provideAppDatabase(context).workoutSessionDao()

    fun provideExerciseRepository(context: Context): ExerciseRepository {
        return exerciseRepositoryInstance ?: run {
            val repo = ExerciseRepository(provideExerciseApi(), provideExerciseDao(context))
            exerciseRepositoryInstance = repo
            Log.d("AppDependencies", "ExerciseRepository instance created")
            repo
        }
    }

    fun provideWorkoutRepository(context: Context): WorkoutRepository {
        return workoutRepositoryInstance ?: run {
            val repo = WorkoutRepository(
                provideExerciseApi(),
                provideExerciseDao(context),
                provideWorkoutPlanDao(context),
                provideWorkoutSessionDao(context)
            )
            workoutRepositoryInstance = repo
            Log.d("AppDependencies", "WorkoutRepository instance created")
            repo
        }
    }
}
