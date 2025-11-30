package com.example.fitkagehealth.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.fitkagehealth.model.Exercise
import com.example.fitkagehealth.model.UserWorkout
import com.example.fitkagehealth.model.WorkoutPlan
import com.example.fitkagehealth.model.WorkoutSession

@Database(
    entities = [Exercise::class, WorkoutPlan::class, WorkoutSession::class, UserWorkout::class],
    version = 5, // Increased version
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun workoutPlanDao(): WorkoutPlanDao
    abstract fun workoutSessionDao(): WorkoutSessionDao
    abstract fun workoutDao(): WorkoutDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "fitkage_workout_db"
                )
                    .fallbackToDestructiveMigration() // This will clear DB on version change
                    .build()
                    .also { INSTANCE = it }
            }
    }
}