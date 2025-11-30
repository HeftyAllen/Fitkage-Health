package com.example.fitkagehealth.food

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.fitkagehealth.adapters.MealPrepMealAdapter

import com.example.fitkagehealth.databinding.ActivityMealPrepBinding

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.*

class MealPrepActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMealPrepBinding
    private lateinit var spoonacularService: com.example.fitkagehealth.api.SpoonacularService
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()

    private var dailyCalories = 2000
    private var mealsPerDay = 3
    private var dietPreference = ""
    private val weeklyPlan = mutableListOf<MealPlanDay>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMealPrepBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRetrofit()
        setupUI()
        loadUserPreferences()
    }

    private fun setupRetrofit() {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.spoonacular.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        spoonacularService = retrofit.create(com.example.fitkagehealth.api.SpoonacularService::class.java)
    }

    private fun setupUI() {
        binding.toolbar.setNavigationOnClickListener { onBackPressed() }

        binding.generatePlanButton.setOnClickListener {
            showMealPrepSettingsDialog()
        }

        binding.savePlanButton.setOnClickListener {
            saveMealPrepPlan()
        }

        binding.weekCalendar.setOnDateChangeListener { view, year, month, dayOfMonth ->
            val selectedDate = Calendar.getInstance().apply {
                set(year, month, dayOfMonth)
            }
            showDayMeals(selectedDate)
        }

        setupWeekCalendar()
    }

    private fun setupWeekCalendar() {
        val calendar = Calendar.getInstance()
        binding.weekCalendar.minDate = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_YEAR, 7)
        binding.weekCalendar.maxDate = calendar.timeInMillis
    }

    private fun showMealPrepSettingsDialog() {
        val dialogBinding = com.example.fitkagehealth.databinding.DialogMealPrepSettingsBinding.inflate(layoutInflater)
        dialogBinding.caloriesEditText.setText(dailyCalories.toString())
        dialogBinding.mealsPerDaySpinner.setSelection(mealsPerDay - 1)
        dialogBinding.dietSpinner.setSelection(
            when (dietPreference) {
                "vegetarian" -> 1
                "vegan" -> 2
                "glutenFree" -> 3
                "ketogenic" -> 4
                else -> 0
            }
        )

        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Meal Prep Settings")
            .setView(dialogBinding.root)
            .setPositiveButton("Generate Plan") { _, _ ->
                dailyCalories = dialogBinding.caloriesEditText.text.toString().toIntOrNull() ?: 2000
                mealsPerDay = dialogBinding.mealsPerDaySpinner.selectedItemPosition + 1
                dietPreference = when (dialogBinding.dietSpinner.selectedItemPosition) {
                    1 -> "vegetarian"
                    2 -> "vegan"
                    3 -> "glutenFree"
                    4 -> "ketogenic"
                    else -> ""
                }
                generateMealPrepPlan()
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()
    }

    private fun generateMealPrepPlan() {
        lifecycleScope.launch {
            try {
                binding.progressBar.visibility = android.view.View.VISIBLE
                weeklyPlan.clear()

                // Generate meal plan for each day of the week
                for (day in 0..6) {
                    val dayPlan = generateDayPlan(day)
                    weeklyPlan.add(dayPlan)
                }

                displayWeeklyPlan()
                saveUserPreferences()

            } catch (e: Exception) {
                Toast.makeText(this@MealPrepActivity, "Failed to generate meal prep plan: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.visibility = android.view.View.GONE
            }
        }
    }

    private suspend fun generateDayPlan(dayOffset: Int): MealPlanDay {
        val caloriesPerMeal = dailyCalories / mealsPerDay

        // In a real app, you'd call Spoonacular API to get actual recipes
        // For now, we'll create mock meals based on the day and meal type
        val meals = mutableListOf<MealPrepMeal>()

        for (mealIndex in 0 until mealsPerDay) {
            val mealType = when (mealIndex) {
                0 -> "Breakfast"
                1 -> "Lunch"
                2 -> "Dinner"
                else -> "Snack"
            }

            val meal = generateMockMeal(mealType, caloriesPerMeal, dayOffset)
            meals.add(meal)
        }

        return MealPlanDay(
            date = getDateForDay(dayOffset),
            meals = meals,
            totalCalories = meals.sumOf { it.calories }
        )
    }

    private fun generateMockMeal(mealType: String, targetCalories: Int, dayOffset: Int): MealPrepMeal {
        val mealNames = when (mealType) {
            "Breakfast" -> listOf(
                "Oatmeal with Berries",
                "Greek Yogurt Parfait",
                "Avocado Toast",
                "Protein Smoothie",
                "Egg Scramble"
            )
            "Lunch" -> listOf(
                "Chicken Salad",
                "Quinoa Bowl",
                "Turkey Wrap",
                "Vegetable Stir Fry",
                "Lentil Soup"
            )
            "Dinner" -> listOf(
                "Grilled Salmon",
                "Chicken Breast with Veggies",
                "Pasta with Tomato Sauce",
                "Beef Stir Fry",
                "Vegetable Curry"
            )
            else -> listOf(
                "Apple with Almonds",
                "Protein Bar",
                "Greek Yogurt",
                "Rice Cakes",
                "Vegetable Sticks"
            )
        }

        val mealName = mealNames[(dayOffset + mealType.hashCode()) % mealNames.size]

        return MealPrepMeal(
            name = mealName,
            mealType = mealType,
            calories = targetCalories,
            protein = (targetCalories * 0.3 / 4).toInt(), // 30% calories from protein
            carbs = (targetCalories * 0.4 / 4).toInt(),   // 40% calories from carbs
            fat = (targetCalories * 0.3 / 9).toInt()      // 30% calories from fat
        )
    }

    private fun displayWeeklyPlan() {
        binding.weeklySummaryCard.visibility = android.view.View.VISIBLE

        val totalWeeklyCalories = weeklyPlan.sumOf { it.totalCalories }
        val avgDailyCalories = totalWeeklyCalories / 7

        binding.weeklyCaloriesText.text = "Weekly Total: $totalWeeklyCalories cal"
        binding.avgDailyCaloriesText.text = "Average Daily: $avgDailyCalories cal"

        // Show today's meals by default
        showDayMeals(Calendar.getInstance())
    }

    private fun showDayMeals(date: Calendar) {
        val dateString = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date.time)
        val dayPlan = weeklyPlan.find { it.date == dateString }

        if (dayPlan != null) {
            binding.selectedDateText.text = SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(date.time)
            displayDayMeals(dayPlan)
        }
    }

    private fun displayDayMeals(dayPlan: MealPlanDay) {
        binding.dayMealsRecyclerView.visibility = android.view.View.VISIBLE
        binding.noMealsText.visibility = android.view.View.GONE

        // Create and set adapter for day meals
        val adapter = MealPrepMealAdapter(dayPlan.meals)
        binding.dayMealsRecyclerView.adapter = adapter
        binding.dayMealsRecyclerView.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)

        binding.dayTotalCalories.text = "Day Total: ${dayPlan.totalCalories} cal"
    }

    private fun saveMealPrepPlan() {
        val userId = auth.currentUser?.uid ?: return

        val planData = weeklyPlan.associate { it.date to it }
        database.getReference("meal_prep_plans")
            .child(userId)
            .setValue(planData)
            .addOnSuccessListener {
                Toast.makeText(this, "Meal prep plan saved!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to save plan", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadUserPreferences() {
        val userId = auth.currentUser?.uid ?: return
        val prefsRef = database.getReference("user_preferences").child(userId)

        prefsRef.addListenerForSingleValueEvent(object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                dailyCalories = snapshot.child("dailyCalories").getValue(Int::class.java) ?: 2000
                mealsPerDay = snapshot.child("mealsPerDay").getValue(Int::class.java) ?: 3
                dietPreference = snapshot.child("dietPreference").getValue(String::class.java) ?: ""
            }

            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                // Use defaults
            }
        })
    }

    private fun saveUserPreferences() {
        val userId = auth.currentUser?.uid ?: return
        val prefsRef = database.getReference("user_preferences").child(userId)

        val preferences = mapOf(
            "dailyCalories" to dailyCalories,
            "mealsPerDay" to mealsPerDay,
            "dietPreference" to dietPreference
        )

        prefsRef.setValue(preferences)
    }

    private fun getDateForDay(dayOffset: Int): String {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, dayOffset)
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
    }

    data class MealPrepMeal(
        val name: String,
        val mealType: String,
        val calories: Int,
        val protein: Int,
        val carbs: Int,
        val fat: Int
    )
}

// Add this to your models package
data class MealPlanDay(
    val date: String,
    val meals: List<MealPrepActivity.MealPrepMeal>,
    val totalCalories: Int
)