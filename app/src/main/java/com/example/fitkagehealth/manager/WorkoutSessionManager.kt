package com.example.fitkagehealth.manager

import android.os.CountDownTimer
import com.example.fitkagehealth.model.ExercisePlan
import com.example.fitkagehealth.model.WorkoutPlan
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class WorkoutSessionManager(
    private val exercises: List<ExercisePlan>
) {
    private var _workoutState = MutableStateFlow(WorkoutState.IDLE)
    val workoutState: StateFlow<WorkoutState> = _workoutState.asStateFlow()

    private var _currentExercise = MutableStateFlow<ExercisePlan?>(null)
    val currentExercise: StateFlow<ExercisePlan?> = _currentExercise.asStateFlow()

    private var _nextExercise = MutableStateFlow<ExercisePlan?>(null)
    val nextExercise: StateFlow<ExercisePlan?> = _nextExercise.asStateFlow()

    private var _currentSet = MutableStateFlow(1)
    val currentSet: StateFlow<Int> = _currentSet.asStateFlow()

    private var _timerValue = MutableStateFlow(0)
    val timerValue: StateFlow<Int> = _timerValue.asStateFlow()

    private var _workoutDuration = MutableStateFlow(0L)
    val workoutDuration: StateFlow<Long> = _workoutDuration.asStateFlow()

    private var _completedExercises = MutableStateFlow(0)
    val completedExercises: StateFlow<Int> = _completedExercises.asStateFlow()

    private var currentWorkoutPlan: WorkoutPlan? = null
    private var currentExerciseIndex = 0
    private var restTimer: CountDownTimer? = null
    private var workoutTimer: CountDownTimer? = null
    private var workoutStartTime: Long = 0

    private val completedExerciseIds = mutableSetOf<String>()

    fun startWorkout(workoutPlan: WorkoutPlan) {
        currentWorkoutPlan = workoutPlan.copy(
            exercises = workoutPlan.exercises.map { it.copy(completed = false) }
        )
        currentExerciseIndex = 0
        _currentSet.value = 1
        _workoutState.value = WorkoutState.ACTIVE
        _completedExercises.value = 0
        completedExerciseIds.clear()
        workoutStartTime = System.currentTimeMillis()
        startWorkoutTimer()
        loadCurrentExercise()
    }

    fun completeSet() {
        val current = _currentExercise.value ?: return

        val updatedReps = current.actualReps.toMutableList().apply {
            if (size < _currentSet.value) {
                add(current.reps)
            }
        }

        _currentExercise.value = current.copy(actualReps = updatedReps)

        if (_currentSet.value < current.sets) {
            _currentSet.value = _currentSet.value + 1
            startRestTimer(current.restTime)
        } else {
            // Mark exercise as completed
            val completedExercise = current.copy(completed = true)
            _currentExercise.value = completedExercise

            // Update completed count
            if (!completedExerciseIds.contains(current.exercise.id)) {
                completedExerciseIds.add(current.exercise.id)
                _completedExercises.value = completedExerciseIds.size
            }

            moveToNextExercise()
        }
    }

    fun skipRest() {
        restTimer?.cancel()
        _workoutState.value = WorkoutState.ACTIVE
        _timerValue.value = 0
    }

    fun pauseWorkout() {
        workoutTimer?.cancel()
        restTimer?.cancel()
        _workoutState.value = WorkoutState.PAUSED
    }

    fun resumeWorkout() {
        if (_workoutState.value == WorkoutState.PAUSED) {
            _workoutState.value = WorkoutState.ACTIVE
            startWorkoutTimer()
        }
    }

    fun finishWorkout() {
        workoutTimer?.cancel()
        restTimer?.cancel()
        _workoutState.value = WorkoutState.COMPLETED
        _workoutDuration.value = System.currentTimeMillis() - workoutStartTime
    }

    fun addRestTime(seconds: Int) {
        val currentTime = _timerValue.value
        _timerValue.value = currentTime + seconds
    }

    private fun loadCurrentExercise() {
        val exercises = currentWorkoutPlan?.exercises ?: return

        if (currentExerciseIndex < exercises.size) {
            _currentExercise.value = exercises[currentExerciseIndex].copy(completed = false)

            if (currentExerciseIndex + 1 < exercises.size) {
                _nextExercise.value = exercises[currentExerciseIndex + 1]
            } else {
                _nextExercise.value = null
            }

            _currentSet.value = 1
            _workoutState.value = WorkoutState.ACTIVE
        } else {
            finishWorkout()
        }
    }

    private fun moveToNextExercise() {
        currentExerciseIndex++
        if (currentExerciseIndex < (currentWorkoutPlan?.exercises?.size ?: 0)) {
            loadCurrentExercise()
        } else {
            finishWorkout()
        }
    }

    private fun startRestTimer(seconds: Int) {
        _workoutState.value = WorkoutState.RESTING
        _timerValue.value = seconds

        restTimer = object : CountDownTimer(seconds * 1000L, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                _timerValue.value = (millisUntilFinished / 1000).toInt()
            }

            override fun onFinish() {
                _workoutState.value = WorkoutState.ACTIVE
                _timerValue.value = 0
            }
        }.start()
    }

    private fun startWorkoutTimer() {
        workoutTimer = object : CountDownTimer(Long.MAX_VALUE, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                _workoutDuration.value = System.currentTimeMillis() - workoutStartTime
            }

            override fun onFinish() {}
        }.start()
    }

    fun getCompletedExercises(): Int = _completedExercises.value

    fun getTotalExercises(): Int = currentWorkoutPlan?.exercises?.size ?: 0

    fun getWorkoutPlan(): WorkoutPlan? = currentWorkoutPlan

    fun getTotalDuration(): Long = _workoutDuration.value

    enum class WorkoutState {
        IDLE, ACTIVE, RESTING, PAUSED, COMPLETED
    }
}