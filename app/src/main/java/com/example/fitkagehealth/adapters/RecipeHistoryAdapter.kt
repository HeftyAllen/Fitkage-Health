package com.example.fitkagehealth.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.fitkagehealth.R
import com.example.fitkagehealth.model.RecipeHistory
import java.text.SimpleDateFormat
import java.util.*

class RecipeHistoryAdapter(
    private val onItemClick: (RecipeHistory) -> Unit
) : ListAdapter<RecipeHistory, RecipeHistoryAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recipe_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val recipe = getItem(position)
        holder.bind(recipe)
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val recipeImage: ImageView = itemView.findViewById(R.id.recipeImage)
        private val recipeTitle: TextView = itemView.findViewById(R.id.recipeTitle)
        private val recipeDate: TextView = itemView.findViewById(R.id.recipeDate)
        private val recipeCalories: TextView = itemView.findViewById(R.id.recipeCalories)
        private val recipeMacros: TextView = itemView.findViewById(R.id.recipeMacros)

        fun bind(recipe: RecipeHistory) {
            recipeTitle.text = recipe.recipeTitle
            recipeDate.text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                .format(Date(recipe.timestamp))
            recipeCalories.text = "${recipe.calories} cal"
            recipeMacros.text = "P:${recipe.protein}g C:${recipe.carbs}g F:${recipe.fat}g"

            // Load image
            recipe.imageUrl?.let { url ->
                Glide.with(itemView.context)
                    .load(url)
                    .placeholder(R.drawable.placeholder_food)
                    .into(recipeImage)
            } ?: run {
                recipeImage.setImageResource(R.drawable.placeholder_food)
            }

            itemView.setOnClickListener { onItemClick(recipe) }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<RecipeHistory>() {
        override fun areItemsTheSame(oldItem: RecipeHistory, newItem: RecipeHistory): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: RecipeHistory, newItem: RecipeHistory): Boolean {
            return oldItem == newItem
        }
    }
}