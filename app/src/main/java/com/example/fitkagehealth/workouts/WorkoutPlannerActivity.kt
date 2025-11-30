package com.example.fitkagehealth.workouts

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fitkagehealth.BaseActivity
import com.example.fitkagehealth.R
import com.example.fitkagehealth.adapters.PlannerAdapter
import com.example.fitkagehealth.db.AppDatabase
import com.example.fitkagehealth.model.ExercisePlan
import com.example.fitkagehealth.model.UserWorkout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WorkoutPlannerActivity : BaseActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var saveBtn: Button
    private lateinit var searchEditText: EditText
    private lateinit var clearSearchBtn: ImageButton
    private lateinit var exerciseCountText: TextView
    private lateinit var plannerAdapter: PlannerAdapter

    private val exercisePlans = mutableListOf<ExercisePlan>()
    private val appDb by lazy { AppDatabase.getInstance(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_workout_planner)

        initializeViews()
        setupRecyclerView()
        setupSearch()
        loadInitialExercises()

        saveBtn.setOnClickListener {
            saveWorkout()
        }
    }

    private fun initializeViews() {
        recyclerView = findViewById(R.id.recyclerViewExercises)
        saveBtn = findViewById(R.id.saveWorkoutBtn)
        searchEditText = findViewById(R.id.search_view)
        clearSearchBtn = findViewById(R.id.clear_search_btn)
        exerciseCountText = findViewById(R.id.exercise_count_text)
    }

    private fun setupRecyclerView() {
        plannerAdapter = PlannerAdapter(exercisePlans) { exercisePlan ->
            // Handle exercise selection if needed
            updateExerciseCount()
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = plannerAdapter
    }

    private fun setupSearch() {
        // Text watcher for real-time search
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().trim()
                updateClearButtonVisibility(query)

                if (query.isNotEmpty()) {
                    searchExercises(query)
                } else {
                    loadInitialExercises()
                }
            }
        })

        // Clear search button
        clearSearchBtn.setOnClickListener {
            searchEditText.text.clear()
            loadInitialExercises()
        }

        // Handle search action from keyboard
        searchEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                val query = searchEditText.text.toString().trim()
                if (query.isNotEmpty()) {
                    searchExercises(query)
                }
                true
            } else {
                false
            }
        }
    }

    private fun updateClearButtonVisibility(query: String) {
        clearSearchBtn.visibility = if (query.isNotEmpty()) View.VISIBLE else View.GONE
    }

    private fun loadInitialExercises() {
        lifecycleScope.launch {
            showLoading(true)

            val exercises = withContext(Dispatchers.IO) {
                try {
                    appDb.exerciseDao().getAllExercises().firstOrNull()
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            } ?: emptyList()

            if (exercises.isNotEmpty()) {
                updateExerciseList(exercises.take(15))
                exerciseCountText.text = "${exercises.size} exercises available"
            } else {
                Toast.makeText(
                    this@WorkoutPlannerActivity,
                    "No exercises found. Please check your connection and try again.",
                    Toast.LENGTH_LONG
                ).show()
                exerciseCountText.text = "No exercises available"
            }

            showLoading(false)
        }
    }

    private fun searchExercises(query: String) {
        if (query.length < 2) {
            // Don't search for very short queries
            if (query.isEmpty()) {
                loadInitialExercises()
            }
            return
        }

        lifecycleScope.launch {
            showLoading(true)

            val exercises = withContext(Dispatchers.IO) {
                try {
                    appDb.exerciseDao().searchExercises("%$query%").firstOrNull()
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            } ?: emptyList()

            if (exercises.isNotEmpty()) {
                updateExerciseList(exercises)
                exerciseCountText.text = "Found ${exercises.size} exercises for \"$query\""
                Toast.makeText(
                    this@WorkoutPlannerActivity,
                    "Found ${exercises.size} exercises",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                exerciseCountText.text = "No exercises found for \"$query\""
                Toast.makeText(
                    this@WorkoutPlannerActivity,
                    "No exercises found for \"$query\"",
                    Toast.LENGTH_SHORT
                ).show()
            }

            showLoading(false)
        }
    }

    private fun updateExerciseList(exercises: List<com.example.fitkagehealth.model.Exercise>) {
        exercisePlans.clear()
        exercisePlans.addAll(exercises.map { ex ->
            ExercisePlan(
                exercise = ex,
                sets = 3,
                reps = 12,
                restTime = 60
            )
        })
        plannerAdapter.updateExercises(exercisePlans)
        updateExerciseCount()
    }

    private fun updateExerciseCount() {
        val selectedCount = exercisePlans.count { it.sets > 0 }
        saveBtn.text = if (selectedCount > 0) {
            "Save Workout ($selectedCount exercises)"
        } else {
            "Save Workout"
        }
        saveBtn.isEnabled = selectedCount > 0
    }

    private fun saveWorkout() {
        val selectedExercises = exercisePlans.filter { it.sets > 0 }

        if (selectedExercises.isEmpty()) {
            Toast.makeText(this, "Please select at least one exercise", Toast.LENGTH_SHORT).show()
            return
        }

        val workout = UserWorkout(
            name = "My Custom Workout - ${System.currentTimeMillis()}",
            exercises = selectedExercises
        )

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                appDb.workoutDao().insertWorkout(workout)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@WorkoutPlannerActivity, "Workout Saved Successfully!", Toast.LENGTH_SHORT).show()
                    // Optionally clear selection or navigate away
                    exercisePlans.forEach { it.sets = 0 }
                    plannerAdapter.updateExercises(exercisePlans)
                    updateExerciseCount()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@WorkoutPlannerActivity,
                        "Failed to save workout: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun showLoading(loading: Boolean) {
        // You can add a progress bar to your layout if needed
        if (loading) {
            // Show loading state
            saveBtn.text = "Loading..."
            saveBtn.isEnabled = false
        } else {
            updateExerciseCount()
        }
    }
}