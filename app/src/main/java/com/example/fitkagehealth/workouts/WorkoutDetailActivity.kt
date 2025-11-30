package com.example.fitkagehealth.workouts

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.fitkagehealth.BaseActivity
import com.example.fitkagehealth.R
import com.example.fitkagehealth.db.AppDatabase
import com.example.fitkagehealth.model.ExercisePlan
import com.example.fitkagehealth.model.UserWorkout
import com.example.fitkagehealth.model.WorkoutPlan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WorkoutDetailActivity : BaseActivity() {

    private lateinit var exerciseName: TextView
    private lateinit var exerciseGif: ImageView
    private lateinit var setsText: TextView
    private lateinit var repsText: TextView
    private lateinit var restTimerText: TextView
    private lateinit var add15SecondsBtn: Button
    private lateinit var nextExerciseBtn: Button

    private var currentExercisePlan: ExercisePlan? = null
    private var workoutPlan: WorkoutPlan? = null
    private var currentExerciseIndex: Int = 0
    private var currentRestTime: Int = 0

    private var restTimer: CountDownTimer? = null
    private val appDb by lazy { AppDatabase.getInstance(this) }
    private var userWorkoutName: String = "Completed Workout"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_workout_detail)

        initializeViews()
        getDataFromIntent()
        setupClickListeners()
        displayCurrentExercise()

        // Handle back gestures and back button
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                showExitConfirmation()
            }
        })
    }

    private fun initializeViews() {
        exerciseName = findViewById(R.id.exerciseName)
        exerciseGif = findViewById(R.id.exerciseGif)
        setsText = findViewById(R.id.setsText)
        repsText = findViewById(R.id.repsText)
        restTimerText = findViewById(R.id.restTimerText)
        add15SecondsBtn = findViewById(R.id.add15SecondsBtn)
        nextExerciseBtn = findViewById(R.id.nextExerciseBtn)
    }

    private fun getDataFromIntent() {
        workoutPlan = intent.getParcelableExtra("workout_plan")
        currentExerciseIndex = intent.getIntExtra("current_exercise_index", 0)

        workoutPlan?.let { plan ->
            if (currentExerciseIndex < plan.exercises.size) {
                currentExercisePlan = plan.exercises[currentExerciseIndex]
                currentRestTime = currentExercisePlan?.restTime ?: 60
            }
        }
    }

    private fun setupClickListeners() {
        add15SecondsBtn.setOnClickListener {
            currentRestTime += 15
            currentExercisePlan?.let { it.restTime = currentRestTime }
            updateRestTimeText()
        }

        nextExerciseBtn.setOnClickListener {
            restTimer?.cancel()
            navigateToNextExercise()
        }
    }

    private fun displayCurrentExercise() {
        currentExercisePlan?.let { exercisePlan ->
            exerciseName.text = exercisePlan.exercise.name
            setsText.text = "Sets: ${exercisePlan.sets}"
            repsText.text = "Reps: ${exercisePlan.reps}"

            currentRestTime = exercisePlan.restTime
            updateRestTimeText()
            val gifUrl = exercisePlan.exercise.gifUrl
            if (!gifUrl.isNullOrEmpty()) {
                Glide.with(this)
                    .load(gifUrl)
                    .placeholder(R.drawable.abs)
                    .error(R.drawable.bicep)
                    .into(exerciseGif)
            } else {
                // Fallback if GIF URL is null
                exerciseGif.setImageResource(R.drawable.chest)
            }


            updateNextButtonText()
            startRestTimer(currentRestTime)
        } ?: onWorkoutComplete()
    }

    private fun updateRestTimeText() {
        restTimerText.text = "Rest: ${currentRestTime}s"
    }

    private fun updateNextButtonText() {
        workoutPlan?.let { plan ->
            nextExerciseBtn.text =
                if (currentExerciseIndex >= plan.exercises.size - 1) "Finish Workout"
                else "Next Exercise"
        }
    }

    private fun startRestTimer(seconds: Int) {
        restTimer?.cancel()
        restTimerText.text = "Rest: $seconds"

        restTimer = object : CountDownTimer(seconds * 1000L, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                restTimerText.text = "Rest: ${millisUntilFinished / 1000}s"
            }

            override fun onFinish() {
                navigateToNextExercise()
            }
        }.start()
    }

    private fun navigateToNextExercise() {
        workoutPlan?.let { plan ->
            if (currentExerciseIndex < plan.exercises.size - 1) {
                val nextIntent = Intent(this, WorkoutDetailActivity::class.java).apply {
                    putExtra("workout_plan", plan)
                    putExtra("current_exercise_index", currentExerciseIndex + 1)
                }
                startActivity(nextIntent)
                finish()
            } else {
                onWorkoutComplete()
            }
        }
    }

    private fun onWorkoutComplete() {
        Toast.makeText(this, "Workout Complete!", Toast.LENGTH_SHORT).show()
        saveWorkoutToDb()
        finish()
    }

    private fun saveWorkoutToDb() {
        val exercises = workoutPlan?.exercises ?: listOf(currentExercisePlan ?: return)
        val workout = UserWorkout(
            name = userWorkoutName,
            exercises = exercises
        )
        lifecycleScope.launch(Dispatchers.IO) {
            appDb.workoutDao().insertWorkout(workout)
            withContext(Dispatchers.Main) {
                Toast.makeText(this@WorkoutDetailActivity, "Workout Saved!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showExitConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Exit Workout?")
            .setMessage("Are you sure you want to exit the workout? Your progress will be saved.")
            .setPositiveButton("Exit") { _, _ ->
                saveWorkoutToDb()
                finish()
            }
            .setNegativeButton("Continue", null)
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        restTimer?.cancel()
    }
}
