// MealPrepMealAdapter.kt
package com.example.fitkagehealth.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.fitkagehealth.R
import com.example.fitkagehealth.food.MealPrepActivity

class MealPrepMealAdapter(private val meals: List<MealPrepActivity.MealPrepMeal>) :
    RecyclerView.Adapter<MealPrepMealAdapter.MealViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MealViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_meal_prep_meal, parent, false)
        return MealViewHolder(view)
    }

    override fun onBindViewHolder(holder: MealViewHolder, position: Int) {
        val meal = meals[position]
        holder.bind(meal)
    }

    override fun getItemCount(): Int = meals.size

    class MealViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val mealName: TextView = itemView.findViewById(R.id.mealName)
        private val mealType: TextView = itemView.findViewById(R.id.mealType)
        private val calories: TextView = itemView.findViewById(R.id.calories)
        private val nutritionInfo: TextView = itemView.findViewById(R.id.nutritionInfo)

        fun bind(meal: MealPrepActivity.MealPrepMeal) {
            mealName.text = meal.name
            mealType.text = meal.mealType
            calories.text = "${meal.calories} cal"
            nutritionInfo.text = "P: ${meal.protein}g • C: ${meal.carbs}g • F: ${meal.fat}g"
        }
    }
}