package com.example.fitkagehealth.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.fitkagehealth.R
import com.example.fitkagehealth.model.WorkoutSession
import java.text.SimpleDateFormat
import java.util.*

class WorkoutHistoryAdapter(
    private val onItemClick: (WorkoutSession) -> Unit
) : ListAdapter<WorkoutSession, WorkoutHistoryAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_workout_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val session = getItem(position)
        holder.bind(session)
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val workoutName: TextView = itemView.findViewById(R.id.workoutName)
        private val workoutDate: TextView = itemView.findViewById(R.id.workoutDate)
        private val workoutDuration: TextView = itemView.findViewById(R.id.workoutDuration)
        private val workoutCalories: TextView = itemView.findViewById(R.id.workoutCalories)
        private val completedExercises: TextView = itemView.findViewById(R.id.completedExercises)

        fun bind(session: WorkoutSession) {
            workoutName.text = session.workoutPlan.name
            workoutDate.text = SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault())
                .format(Date(session.startTime))
            workoutDuration.text = formatDuration(session.totalDuration)
            workoutCalories.text = "${session.caloriesBurned} cal"
            completedExercises.text = "${session.completedExercises}/${session.totalExercises} exercises"

            itemView.setOnClickListener { onItemClick(session) }
        }

        private fun formatDuration(millis: Long): String {
            val minutes = millis / 60000
            val seconds = (millis % 60000) / 1000
            return String.format("%02d:%02d", minutes, seconds)
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<WorkoutSession>() {
        override fun areItemsTheSame(oldItem: WorkoutSession, newItem: WorkoutSession): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: WorkoutSession, newItem: WorkoutSession): Boolean {
            return oldItem == newItem
        }
    }
}