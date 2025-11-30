package com.example.fitkagehealth.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.fitkagehealth.model.WorkoutSession
import com.example.fitkagehealth.repository.WorkoutRepository
import kotlinx.coroutines.launch

class WorkoutCompletionViewModel(
    private val repository: WorkoutRepository
) : ViewModel() {

    fun saveWorkoutSession(session: WorkoutSession, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                repository.saveWorkoutSession(session)
                onComplete(true)
            } catch (e: Exception) {
                onComplete(false)
            }
        }
    }
}

class WorkoutCompletionViewModelFactory(
    private val repository: WorkoutRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WorkoutCompletionViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WorkoutCompletionViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
