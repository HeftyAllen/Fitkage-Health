package com.example.fitkagehealth.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.fitkagehealth.databinding.ItemExerciseSearchBinding
import com.example.fitkagehealth.model.Exercise

class ExerciseSearchAdapter(
    private val onExerciseAdded: (Exercise) -> Unit
) : ListAdapter<Exercise, ExerciseSearchAdapter.ExerciseSearchViewHolder>(ExerciseDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExerciseSearchViewHolder {
        val binding = ItemExerciseSearchBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ExerciseSearchViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ExerciseSearchViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ExerciseSearchViewHolder(
        private val binding: ItemExerciseSearchBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(exercise: Exercise) {
            binding.exerciseName.text = exercise.name
            binding.bodyPartText.text = "${exercise.bodyPart} • ${exercise.target}"

            Glide.with(binding.root.context)
                .load(exercise.gifUrl)
                .into(binding.exerciseGif)

            binding.addButton.setOnClickListener {
                onExerciseAdded(exercise)
            }
        }
    }
}
