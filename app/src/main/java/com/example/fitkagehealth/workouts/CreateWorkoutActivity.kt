package com.example.fitkagehealth.workouts

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.inputmethod.EditorInfo
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fitkagehealth.BaseActivity
import com.example.fitkagehealth.AppDependencies
import com.example.fitkagehealth.R
import com.example.fitkagehealth.adapters.ExerciseSearchAdapter
import com.example.fitkagehealth.adapters.SelectedExercisesAdapter
import com.example.fitkagehealth.databinding.ActivityCreateWorkoutBinding
import com.example.fitkagehealth.model.Exercise
import com.example.fitkagehealth.model.ExercisePlan
import com.example.fitkagehealth.model.WorkoutPlan
import com.example.fitkagehealth.viewmodel.WorkoutViewModel
import com.example.fitkagehealth.viewmodel.WorkoutViewModelFactory
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

class CreateWorkoutActivity : BaseActivity() {

    private lateinit var binding: ActivityCreateWorkoutBinding
    private lateinit var viewModel: WorkoutViewModel

    private lateinit var searchAdapter: ExerciseSearchAdapter
    private lateinit var selectedAdapter: SelectedExercisesAdapter

    private val selectedExercises = mutableListOf<ExercisePlan>()
    private var isSearching = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateWorkoutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val repo = AppDependencies.provideWorkoutRepository(this)
        val factory = WorkoutViewModelFactory(repo)
        viewModel = ViewModelProvider(this, factory).get(WorkoutViewModel::class.java)

        setupUI()
        setupRecyclerViews()
        setupObservers()

        // Load initial popular exercises
        loadPopularExercises()
    }

    private fun setupUI() {
        binding.toolbar.setNavigationOnClickListener { onBackPressed() }

        // Search button click
        binding.searchButton.setOnClickListener {
            performSearch()
        }

        // Keyboard search action
        binding.searchEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch()
                true
            } else {
                false
            }
        }

        // Real-time search as user types (with debounce)
        binding.searchEditText.addTextChangedListener(object : TextWatcher {
            private var searchRunnable: Runnable? = null

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                searchRunnable?.let { binding.searchEditText.removeCallbacks(it) }

                searchRunnable = Runnable {
                    val query = s.toString().trim()
                    if (query.length >= 2) { // Only search if query is 2+ characters
                        performSearch()
                    } else if (query.isEmpty()) {
                        // Show popular exercises when search is cleared
                        loadPopularExercises()
                    }
                }

                searchRunnable?.let {
                    binding.searchEditText.postDelayed(it, 500) // 500ms debounce
                }
            }
        })

        binding.saveWorkoutBtn.setOnClickListener {
            saveCustomWorkout()
        }

        updateUIState()
    }

    private fun setupRecyclerViews() {
        searchAdapter = ExerciseSearchAdapter { exercise ->
            addExerciseToWorkout(exercise)
        }

        selectedAdapter = SelectedExercisesAdapter(
            onRemoveClick = { exercisePlan ->
                removeExerciseFromWorkout(exercisePlan)
            },
            onSettingsClick = { exercisePlan ->
                showExerciseSettingsDialog(exercisePlan)
            }
        )

        binding.searchResultsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@CreateWorkoutActivity)
            adapter = searchAdapter
        }

        binding.selectedExercisesRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@CreateWorkoutActivity)
            adapter = selectedAdapter
        }
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.searchResults.collect { exercises ->
                    searchAdapter.submitList(exercises)
                    updateSearchResultsUI(exercises)
                }
            }
        }
    }

    private fun loadPopularExercises() {
        // Show popular exercises when no search is active
        viewModel.getAllExercisesForSearch().observe(this) { exercises ->
            if (!isSearching && binding.searchEditText.text.isNullOrBlank()) {
                val popularExercises = exercises.take(15) // Show first 15 as popular
                searchAdapter.submitList(popularExercises)
                updateSearchResultsUI(popularExercises)

                // Show the search results card
                binding.searchResultsRecyclerView.parent?.parent?.let {
                    (it as? android.view.View)?.isVisible = popularExercises.isNotEmpty()
                }
            }
        }
    }

    private fun performSearch() {
        val query = binding.searchEditText.text.toString().trim()

        if (query.isEmpty()) {
            loadPopularExercises()
            return
        }

        isSearching = true
        showSearchLoading(true)

        // Show search results card
        binding.searchResultsRecyclerView.parent?.parent?.let {
            (it as? android.view.View)?.isVisible = true
        }

        viewModel.searchExercises(query)

        // Hide loading after a delay (the search should complete quickly)
        lifecycleScope.launch {
            delay(1000)
            showSearchLoading(false)
        }
    }

    private fun showSearchLoading(loading: Boolean) {
        binding.searchProgressBar.isVisible = loading
        binding.searchResultsRecyclerView.isVisible = !loading
    }

    private fun updateSearchResultsUI(exercises: List<Exercise>) {
        binding.searchResultsCount.text = "${exercises.size} exercises"
        binding.emptySearchText.isVisible = exercises.isEmpty()

        if (exercises.isNotEmpty()) {
            binding.searchResultsRecyclerView.isVisible = true
            binding.emptySearchText.isVisible = false
        } else {
            binding.searchResultsRecyclerView.isVisible = false
            binding.emptySearchText.isVisible = true
        }
    }

    private fun addExerciseToWorkout(exercise: Exercise) {
        if (selectedExercises.any { it.exercise.id == exercise.id }) {
            Toast.makeText(this, "Exercise already added", Toast.LENGTH_SHORT).show()
            return
        }

        val exercisePlan = ExercisePlan(
            exercise = exercise,
            sets = 3,
            reps = 12,
            restTime = 60
        )

        selectedExercises.add(exercisePlan)
        selectedAdapter.submitList(selectedExercises.toList())
        updateUIState()

        // Clear search and show success
        binding.searchEditText.text?.clear()
        isSearching = false
        loadPopularExercises()

        Toast.makeText(this, "${exercise.name} added to workout", Toast.LENGTH_SHORT).show()
    }

    private fun removeExerciseFromWorkout(exercisePlan: ExercisePlan) {
        selectedExercises.removeAll { it.exercise.id == exercisePlan.exercise.id }
        selectedAdapter.submitList(selectedExercises.toList())
        updateUIState()
        Toast.makeText(this, "${exercisePlan.exercise.name} removed", Toast.LENGTH_SHORT).show()
    }

    private fun showExerciseSettingsDialog(exercisePlan: ExercisePlan) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_exercise_settings, null)

        val setsInput = dialogView.findViewById<android.widget.EditText>(R.id.setsEditText)
        val repsInput = dialogView.findViewById<android.widget.EditText>(R.id.repsEditText)
        val restInput = dialogView.findViewById<android.widget.EditText>(R.id.restTimeEditText)

        setsInput.setText(exercisePlan.sets.toString())
        repsInput.setText(exercisePlan.reps.toString())
        restInput.setText(exercisePlan.restTime.toString())

        AlertDialog.Builder(this)
            .setTitle("Edit ${exercisePlan.exercise.name}")
            .setView(dialogView)
            .setPositiveButton("Save") { dialog, _ ->
                val sets = setsInput.text.toString().toIntOrNull() ?: exercisePlan.sets
                val reps = repsInput.text.toString().toIntOrNull() ?: exercisePlan.reps
                val rest = restInput.text.toString().toIntOrNull() ?: exercisePlan.restTime

                // Validate inputs
                if (sets <= 0 || reps <= 0 || rest < 0) {
                    Toast.makeText(this, "Please enter valid numbers", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                exercisePlan.sets = sets
                exercisePlan.reps = reps
                exercisePlan.restTime = rest

                selectedAdapter.submitList(selectedExercises.toList())
                updateUIState()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun updateUIState() {
        val exerciseCount = selectedExercises.size

        // Update save button
        binding.saveWorkoutBtn.isEnabled = exerciseCount > 0
        binding.workoutNameInput.isEnabled = exerciseCount > 0

        // Update workout name hint
        if (exerciseCount > 0) {
            binding.workoutNameInput.hint = "My Custom Workout ($exerciseCount exercises)"
        } else {
            binding.workoutNameInput.hint = "Enter workout name"
        }

        // Update selected exercises count
        binding.selectedExercisesCount.text = "$exerciseCount exercises"

        // Show/hide empty state for selected exercises
        binding.emptySelectedText.isVisible = exerciseCount == 0
        binding.selectedExercisesRecyclerView.isVisible = exerciseCount > 0
    }

    private fun saveCustomWorkout() {
        val workoutName = binding.workoutNameInput.text.toString().trim()

        if (workoutName.isEmpty()) {
            binding.workoutNameInput.error = "Please enter a workout name"
            return
        }

        if (selectedExercises.isEmpty()) {
            Toast.makeText(this, "Please add at least one exercise", Toast.LENGTH_SHORT).show()
            return
        }

        // Validate all exercises have valid sets/reps
        val invalidExercises = selectedExercises.filter {
            it.sets <= 0 || it.reps <= 0 || it.restTime < 0
        }

        if (invalidExercises.isNotEmpty()) {
            Toast.makeText(this, "Please check exercise settings (sets, reps, rest time)", Toast.LENGTH_LONG).show()
            return
        }

        val workoutPlan = WorkoutPlan(
            name = workoutName,
            exercises = selectedExercises,
            isCustom = true
        )

        viewModel.saveWorkoutPlan(workoutPlan)

        Toast.makeText(this, "Workout '$workoutName' saved successfully!", Toast.LENGTH_SHORT).show()
        finish()
    }
}