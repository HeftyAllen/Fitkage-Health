// adapters/FoodLogAdapter.kt
package com.example.fitkagehealth.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.fitkagehealth.R
import com.example.fitkagehealth.model.FoodEntry

class FoodLogAdapter(private val onFoodEntryClick: (FoodEntry) -> Unit) :
    ListAdapter<FoodEntry, FoodLogAdapter.FoodEntryViewHolder>(FoodEntryDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FoodEntryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_food_entry, parent, false)
        return FoodEntryViewHolder(view)
    }

    override fun onBindViewHolder(holder: FoodEntryViewHolder, position: Int) {
        val foodEntry = getItem(position)
        holder.bind(foodEntry)
        holder.itemView.setOnClickListener { onFoodEntryClick(foodEntry) }
    }

    class FoodEntryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val foodName: TextView = itemView.findViewById(R.id.foodName)
        private val foodCalories: TextView = itemView.findViewById(R.id.foodCalories)
        private val mealType: TextView = itemView.findViewById(R.id.mealType)
        private val nutritionInfo: TextView = itemView.findViewById(R.id.nutritionInfo)

        fun bind(foodEntry: FoodEntry) {
            foodName.text = foodEntry.name
            foodCalories.text = "${foodEntry.calories} cal"
            mealType.text = foodEntry.mealType
            nutritionInfo.text = "P: ${foodEntry.protein}g • C: ${foodEntry.carbs}g • F: ${foodEntry.fat}g"
        }
    }

    object FoodEntryDiffCallback : DiffUtil.ItemCallback<FoodEntry>() {
        override fun areItemsTheSame(oldItem: FoodEntry, newItem: FoodEntry): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: FoodEntry, newItem: FoodEntry): Boolean {
            return oldItem == newItem
        }
    }
}