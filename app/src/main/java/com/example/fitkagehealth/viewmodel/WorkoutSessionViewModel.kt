package com.example.fitkagehealth.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.fitkagehealth.manager.WorkoutSessionManager
import com.example.fitkagehealth.model.ExercisePlan
import com.example.fitkagehealth.model.WorkoutPlan
import com.example.fitkagehealth.model.WorkoutSession
import com.example.fitkagehealth.repository.WorkoutRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class WorkoutSessionViewModel(
    private val repository: WorkoutRepository
) : ViewModel() {

    private var sessionManager: WorkoutSessionManager? = null

    private val _currentExercise = MutableStateFlow<ExercisePlan?>(null)
    val currentExercise: StateFlow<ExercisePlan?> = _currentExercise

    private val _nextExercise = MutableStateFlow<ExercisePlan?>(null)
    val nextExercise: StateFlow<ExercisePlan?> = _nextExercise

    private val _workoutState = MutableStateFlow(WorkoutSessionManager.WorkoutState.IDLE)
    val workoutState: StateFlow<WorkoutSessionManager.WorkoutState> = _workoutState

    private val _timerValue = MutableStateFlow(0)
    val timerValue: StateFlow<Int> = _timerValue

    private val _workoutDuration = MutableStateFlow(0L)
    val workoutDuration: StateFlow<Long> = _workoutDuration

    private val _currentSet = MutableStateFlow(1)
    val currentSet: StateFlow<Int> = _currentSet

    private val _completedExercises = MutableStateFlow(0)
    val completedExercises: StateFlow<Int> = _completedExercises

    fun startWorkout(workoutPlan: WorkoutPlan) {
        sessionManager = WorkoutSessionManager(workoutPlan.exercises)
        sessionManager?.startWorkout(workoutPlan)
        observeSessionManager()
    }

    private fun observeSessionManager() {
        viewModelScope.launch {
            sessionManager?.currentExercise?.collect { exercise ->
                _currentExercise.value = exercise
            }
        }

        viewModelScope.launch {
            sessionManager?.nextExercise?.collect { next ->
                _nextExercise.value = next
            }
        }

        viewModelScope.launch {
            sessionManager?.workoutState?.collect { state ->
                _workoutState.value = state
            }
        }

        viewModelScope.launch {
            sessionManager?.timerValue?.collect { seconds ->
                _timerValue.value = seconds
            }
        }

        viewModelScope.launch {
            sessionManager?.workoutDuration?.collect { duration ->
                _workoutDuration.value = duration
            }
        }

        viewModelScope.launch {
            sessionManager?.currentSet?.collect { set ->
                _currentSet.value = set
            }
        }

        viewModelScope.launch {
            sessionManager?.completedExercises?.collect { completed ->
                _completedExercises.value = completed
            }
        }
    }

    fun togglePause() {
        when (workoutState.value) {
            WorkoutSessionManager.WorkoutState.PAUSED -> sessionManager?.resumeWorkout()
            WorkoutSessionManager.WorkoutState.ACTIVE -> sessionManager?.pauseWorkout()
            else -> {}
        }
    }

    fun completeSet() = sessionManager?.completeSet()
    fun skipRest() = sessionManager?.skipRest()
    fun addRestTime(seconds: Int) = sessionManager?.addRestTime(seconds)

    fun getCompletedExercises(): Int = sessionManager?.getCompletedExercises() ?: 0
    fun getTotalExercises(): Int = sessionManager?.getTotalExercises() ?: 0

    fun finishWorkout() {
        sessionManager?.finishWorkout()
        saveWorkoutSession()
    }

    fun getWorkoutSession(): WorkoutSession? {
        val manager = sessionManager ?: return null
        val workoutPlan = manager.getWorkoutPlan() ?: return null

        return WorkoutSession(
            workoutPlan = workoutPlan,
            totalDuration = manager.getTotalDuration(),
            completedExercises = manager.getCompletedExercises(),
            totalExercises = manager.getTotalExercises(),
            caloriesBurned = calculateCaloriesBurned(workoutPlan, manager.getTotalDuration())
        )
    }

    private fun calculateCaloriesBurned(workoutPlan: WorkoutPlan, duration: Long): Int {
        // Simple calorie calculation - adjust based on your needs
        val caloriesPerMinute = 8 // Average for strength training
        val minutes = duration / 60000
        return (caloriesPerMinute * minutes).toInt()
    }

    fun saveWorkoutSession() {
        viewModelScope.launch {
            getWorkoutSession()?.let { repository.saveWorkoutSession(it) }
        }
    }

    override fun onCleared() {
        super.onCleared()
        sessionManager?.finishWorkout()
    }
}

class WorkoutSessionViewModelFactory(
    private val repository: WorkoutRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WorkoutSessionViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WorkoutSessionViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}