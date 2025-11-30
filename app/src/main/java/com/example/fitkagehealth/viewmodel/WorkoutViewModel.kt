package com.example.fitkagehealth.viewmodel

import androidx.lifecycle.*
import com.example.fitkagehealth.model.Exercise
import com.example.fitkagehealth.model.ExercisePlan
import com.example.fitkagehealth.model.WorkoutPlan
import com.example.fitkagehealth.repository.WorkoutRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class WorkoutViewModel(
    private val repository: WorkoutRepository
) : ViewModel() {

    private val _searchResultsLiveData = MutableLiveData<List<Exercise>>()
    val searchResultsLiveData: LiveData<List<Exercise>> get() = _searchResultsLiveData
    // Selected exercises (ExercisePlan)
    private val _selectedExercises = MutableStateFlow<List<ExercisePlan>>(emptyList())
    val selectedExercises: StateFlow<List<ExercisePlan>> = _selectedExercises

    // Custom workout plans
    private val _customWorkoutPlans = MutableStateFlow<List<WorkoutPlan>>(emptyList())
    val customWorkoutPlans: StateFlow<List<WorkoutPlan>> = _customWorkoutPlans

    // Search results - ONLY StateFlow, no LiveData
    private val _searchResultsFlow = MutableStateFlow<List<Exercise>>(emptyList())
    val searchResults: StateFlow<List<Exercise>> = _searchResultsFlow

    // internal job so repeated searches cancel previous work
    private var searchJob: Job? = null

    // --- Exercise fetching helpers ---

    fun getExercisesByBodyPart(bodyPart: String): LiveData<List<Exercise>> {
        viewModelScope.launch { repository.refreshExercisesFromApi() }
        return repository.getExercisesByBodyPart(bodyPart)
            .map { it.take(12) }
            .asLiveData()
    }

    fun getExercisesForWorkoutPlanner(bodyPart: String): LiveData<List<Exercise>> =
        repository.getExercisesByBodyPart(bodyPart).map { it.take(12) }.asLiveData()

    fun getAllExercises(): LiveData<List<Exercise>> = repository.getAllExercises().asLiveData()
    fun getAllExercisesForSearch(): LiveData<List<Exercise>> = repository.getAllExercises().asLiveData()

    fun refreshExercises() {
        viewModelScope.launch { repository.refreshExercisesFromApi() }
    }

    fun searchExercises(query: String) {
        val q = query.trim()
        if (q.isEmpty()) {
            _searchResultsFlow.value = emptyList()
            return
        }

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            try {
                repository.searchExercises(q).collectLatest { results ->
                    _searchResultsFlow.value = results
                }
            } catch (t: Throwable) {
                t.printStackTrace()
                _searchResultsFlow.value = emptyList()
            }
        }
    }

    fun clearSearchResults() {
        searchJob?.cancel()
        _searchResultsFlow.value = emptyList()
    }

    // --- Selection management (ExercisePlan) ---

    fun addExerciseToWorkout(exercise: Exercise) {
        val current = _selectedExercises.value.toMutableList()
        val plan = ExercisePlan(
            exercise = exercise,
            sets = 3,
            reps = 12,
            restTime = 60
        )
        if (current.none { it.exercise.id == exercise.id }) {
            current.add(plan)
            _selectedExercises.value = current
        }
    }

    fun toggleExerciseSelection(exercise: Exercise) {
        val exists = _selectedExercises.value.any { it.exercise.id == exercise.id }
        if (exists) removeExerciseFromWorkout(exercise) else addExerciseToWorkout(exercise)
    }

    fun removeExerciseFromWorkout(exercise: Exercise) {
        val current = _selectedExercises.value.toMutableList()
        current.removeAll { it.exercise.id == exercise.id }
        _selectedExercises.value = current
    }

    fun updateExerciseSettings(updatedPlan: ExercisePlan) {
        val current = _selectedExercises.value.toMutableList()
        val idx = current.indexOfFirst { it.exercise.id == updatedPlan.exercise.id }
        if (idx != -1) {
            current[idx] = updatedPlan
            _selectedExercises.value = current
        }
    }

    fun getSelectedExercises(): List<ExercisePlan> = _selectedExercises.value
    fun clearSelectedExercises() { _selectedExercises.value = emptyList() }
    fun getExerciseCount(): Int = _selectedExercises.value.size

    // --- Workout saving / creation ---

    fun createWorkoutFromCurrentExercises(name: String, callback: (WorkoutPlan) -> Unit) {
        val plan = WorkoutPlan(name = name, exercises = _selectedExercises.value, isCustom = true)
        callback(plan)
    }

    fun saveCustomWorkout(name: String, callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val plan = WorkoutPlan(name = name, exercises = _selectedExercises.value, isCustom = true)
                repository.saveWorkoutPlan(plan)
                callback(true)
            } catch (e: Exception) {
                e.printStackTrace()
                callback(false)
            }
        }
    }

    fun saveWorkoutPlan(workoutPlan: WorkoutPlan) {
        viewModelScope.launch {
            try { repository.saveWorkoutPlan(workoutPlan) } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun deleteWorkoutPlan(workoutPlan: WorkoutPlan, callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                repository.deleteWorkoutPlan(workoutPlan)
                callback(true)
            } catch (e: Exception) {
                e.printStackTrace()
                callback(false)
            }
        }
    }

    fun loadCustomWorkoutPlans() {
        viewModelScope.launch {
            try {
                repository.getCustomWorkoutPlans().collect {
                    _customWorkoutPlans.value = it
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // --- Validation & helpers ---

    fun validateWorkout(workoutName: String, exercises: List<ExercisePlan>): Pair<Boolean, String> = when {
        workoutName.isBlank() -> false to "Workout name cannot be empty"
        exercises.isEmpty() -> false to "Please add at least one exercise"
        exercises.any { it.sets <= 0 } -> false to "All exercises must have at least 1 set"
        exercises.any { it.reps <= 0 } -> false to "All exercises must have at least 1 rep"
        exercises.any { it.restTime < 0 } -> false to "Rest time cannot be negative"
        else -> true to "Valid workout"
    }

    fun estimateWorkoutDuration(exercises: List<ExercisePlan>): String {
        val totalTime = exercises.sumOf { e -> (e.sets * e.reps * 4) + ((e.sets - 1) * e.restTime) }
        val minutes = totalTime / 60
        return if (minutes < 60) "$minutes minutes" else "${minutes / 60} h ${minutes % 60} m"
    }

    override fun onCleared() {
        super.onCleared()
        searchJob?.cancel()
    }
}

class WorkoutViewModelFactory(
    private val repository: WorkoutRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WorkoutViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WorkoutViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}