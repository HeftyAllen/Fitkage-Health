package com.example.fitkagehealth

import android.app.Dialog
import android.os.Bundle
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.example.fitkagehealth.databinding.DialogExerciseSettingsBinding
import com.example.fitkagehealth.model.Exercise
import com.example.fitkagehealth.model.ExercisePlan
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class ExerciseSettingsDialog : DialogFragment() {

    private lateinit var binding: DialogExerciseSettingsBinding
    private var exercise: Exercise? = null
    private var onSettingsChanged: ((ExercisePlan) -> Unit)? = null

    companion object {
        private const val ARG_EXERCISE = "exercise"

        fun newInstance(
            exercise: Exercise,
            onSettingsChanged: (ExercisePlan) -> Unit
        ): ExerciseSettingsDialog {
            return ExerciseSettingsDialog().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_EXERCISE, exercise)
                }
                this.onSettingsChanged = onSettingsChanged
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = DialogExerciseSettingsBinding.inflate(layoutInflater)
        exercise = arguments?.getParcelable(ARG_EXERCISE)

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .create()

        setupUI()

        return dialog
    }

    private fun setupUI() {
        exercise?.let { exercise ->
            binding.exerciseName.text = exercise.name
            binding.exerciseTitle.text = exercise.name
            binding.exerciseDescription.text = exercise.muscleGroup ?: exercise.target


            // Optional: load exercise image if available
            // Glide.with(this).load(exercise.imageUrl).into(binding.exerciseImage)
        }

        // Set default values
        binding.setsEditText.setText("3")
        binding.repsEditText.setText("12")
        binding.restTimeEditText.setText("60")

        // Handle Save button click
        binding.saveButton.setOnClickListener {
            saveSettings()
        }

        // Handle Cancel button click
        binding.cancelButton.setOnClickListener {
            dismiss()
        }
    }

    private fun saveSettings() {
        val sets = binding.setsEditText.text.toString().toIntOrNull()
        val reps = binding.repsEditText.text.toString().toIntOrNull()
        val restTime = binding.restTimeEditText.text.toString().toIntOrNull()

        if (sets == null || reps == null || restTime == null) {
            Toast.makeText(requireContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        if (sets <= 0 || reps <= 0 || restTime < 0) {
            Toast.makeText(requireContext(), "Invalid values entered", Toast.LENGTH_SHORT).show()
            return
        }

        exercise?.let { exercise ->
            val exercisePlan = ExercisePlan(
                exercise = exercise,
                sets = sets,
                reps = reps,
                restSeconds = restTime
            )

            onSettingsChanged?.invoke(exercisePlan)
            dismiss()
        }
    }
}
