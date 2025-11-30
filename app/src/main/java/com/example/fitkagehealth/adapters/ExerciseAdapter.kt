package com.example.fitkagehealth.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.fitkagehealth.databinding.ItemExerciseBinding
import com.example.fitkagehealth.model.Exercise

class ExerciseAdapter(
    private val onExerciseSelected: (Exercise, Boolean) -> Unit,
    private val onSettingsClick: (Exercise) -> Unit
) : ListAdapter<Exercise, ExerciseAdapter.ExerciseViewHolder>(ExerciseDiffCallback) {

    // Keep track of selected exercise IDs (persists across submitList calls as long as IDs match)
    private val selectedExercises = mutableSetOf<String>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExerciseViewHolder {
        val binding = ItemExerciseBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ExerciseViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ExerciseViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onViewRecycled(holder: ExerciseViewHolder) {
        super.onViewRecycled(holder)
        holder.unbind()
    }

    fun getSelectedExercises(): List<Exercise> {
        return currentList.filter { selectedExercises.contains(it.id) }
    }

    inner class ExerciseViewHolder(
        private val binding: ItemExerciseBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(exercise: Exercise) {
            binding.exerciseName.text = exercise.name
            binding.bodyPartText.text = exercise.bodyPart
            binding.targetText.text = exercise.target

            // Glide load (empty URL is allowed; Glide will handle it)
            Glide.with(binding.root.context)
                .load(exercise.gifUrl)
                .into(binding.exerciseGif)

            // SAFE checkbox handling: clear listener before changing checked state
            binding.checkbox.setOnCheckedChangeListener(null)
            val alreadySelected = selectedExercises.contains(exercise.id)
            binding.checkbox.isChecked = alreadySelected

            // Re-attach listener
            binding.checkbox.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) selectedExercises.add(exercise.id) else selectedExercises.remove(exercise.id)
                onExerciseSelected(exercise, isChecked)
            }

            // Tapping the row toggles the checkbox (improves UX)
            binding.root.setOnClickListener {
                // toggle safely: remove listener, set checked, reattach and call callback
                val newChecked = !binding.checkbox.isChecked
                binding.checkbox.setOnCheckedChangeListener(null)
                binding.checkbox.isChecked = newChecked
                binding.checkbox.setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) selectedExercises.add(exercise.id) else selectedExercises.remove(exercise.id)
                    onExerciseSelected(exercise, isChecked)
                }
                // also call the callback for immediate response
                onExerciseSelected(exercise, newChecked)
            }

            binding.settingsBtn.setOnClickListener {
                onSettingsClick(exercise)
            }
        }

        fun unbind() {
            // Clear listeners and free Glide image when view is recycled
            binding.checkbox.setOnCheckedChangeListener(null)
            binding.root.setOnClickListener(null)
            binding.settingsBtn.setOnClickListener(null)
            Glide.with(binding.root.context).clear(binding.exerciseGif)
        }
    }

    companion object {
        private val ExerciseDiffCallback = object : DiffUtil.ItemCallback<Exercise>() {
            override fun areItemsTheSame(oldItem: Exercise, newItem: Exercise): Boolean =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: Exercise, newItem: Exercise): Boolean =
                oldItem == newItem
        }
    }
}
