package com.example.fitkagehealth.food

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fitkagehealth.BuildConfig
import com.example.fitkagehealth.R
import com.example.fitkagehealth.api.Meal
import com.example.fitkagehealth.api.MealPlan
import com.example.fitkagehealth.api.SpoonacularService
import com.example.fitkagehealth.api.WeeklyMealPlan
import com.example.fitkagehealth.databinding.ActivityMealPlanBinding
import com.example.fitkagehealth.databinding.DialogMealPlanSettingsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.*

class MealPlanActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMealPlanBinding
    private lateinit var spoonacularService: SpoonacularService
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()

    private var targetCalories = 2000
    private var dietPreference = ""
    private var currentWeekOffset = 0

    // Store fetched weekly recipes by day (keys: Monday..Sunday)
    private var weeklyMealPlan: Map<String, List<Meal>> = emptyMap()

    private val canonicalDays = listOf(
        "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMealPlanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRetrofit()
        setupUI()
        loadUserPreferences()
        generateMealPlan() // auto-generate on open
    }

    private fun setupRetrofit() {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.spoonacular.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        spoonacularService = retrofit.create(SpoonacularService::class.java)
    }

    private fun setupUI() {
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        binding.previousWeekButton.setOnClickListener { navigateWeek(-1) }
        binding.nextWeekButton.setOnClickListener { navigateWeek(1) }
        binding.generatePlanButton.setOnClickListener { showMealPlanSettingsDialog() }
        binding.savePlanButton.setOnClickListener { saveMealPlanToFirebase() }

        binding.dayMealsRecyclerView.layoutManager = LinearLayoutManager(this)

        // If your layout has a place to show day selector buttons (optional),
        // we create them dynamically so user can switch day quickly.
        // If you don't have a container, this block is safe because it's guarded.
        try {
            binding.daySelectorContainer.removeAllViews()
            canonicalDays.forEach { day ->
                val btn = layoutInflater.inflate(R.layout.item_day_button, binding.daySelectorContainer, false)
                btn.findViewById<android.widget.Button>(R.id.dayButton).text = day.take(3) // short label Mon, Tue...
                btn.setOnClickListener { showDayDetails(day) }
                binding.daySelectorContainer.addView(btn)
            }
        } catch (ignored: Exception) {
            // layout might not have daySelectorContainer: ok, continue — navigation still works via code
        }
    }

    private fun showMealPlanSettingsDialog() {
        val dialogBinding = DialogMealPlanSettingsBinding.inflate(layoutInflater)
        dialogBinding.caloriesEditText.setText(targetCalories.toString())
        dialogBinding.dietSpinner.setSelection(
            when (dietPreference) {
                "vegetarian" -> 1
                "vegan" -> 2
                "glutenFree" -> 3
                "ketogenic" -> 4
                else -> 0
            }
        )

        val dialog = AlertDialog.Builder(this)
            .setTitle("Generate Meal Plan")
            .setView(dialogBinding.root)
            .setPositiveButton("Generate") { _, _ ->
                targetCalories = dialogBinding.caloriesEditText.text.toString().toIntOrNull() ?: 2000
                dietPreference = when (dialogBinding.dietSpinner.selectedItemPosition) {
                    1 -> "vegetarian"
                    2 -> "vegan"
                    3 -> "glutenFree"
                    4 -> "ketogenic"
                    else -> ""
                }
                generateMealPlan()
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()
    }

    private fun generateMealPlan() {
        lifecycleScope.launch {
            try {
                binding.progressBar.visibility = View.VISIBLE

                val response = spoonacularService.generateMealPlan(
                    timeFrame = "week",
                    targetCalories = targetCalories,
                    diet = if (dietPreference.isNotEmpty()) dietPreference else null,
                    apiKey = BuildConfig.SPOONACULAR_API_KEY
                )

                if (response.isSuccessful && response.body() != null) {
                    val raw = response.body()!!
                    val gson = com.google.gson.Gson()
                    val json = gson.toJson(raw)

                    // Try to parse as weekly plan, otherwise try daily plan
                    val weeklyPlan = try {
                        gson.fromJson(json, WeeklyMealPlan::class.java)
                    } catch (e: Exception) {
                        null
                    }

                    if (weeklyPlan != null && weeklyPlan.week.isNotEmpty()) {
                        processWeeklyMealPlan(weeklyPlan)
                        Toast.makeText(this@MealPlanActivity, "Weekly meal plan generated!", Toast.LENGTH_SHORT).show()
                    } else {
                        val dailyPlan = try {
                            gson.fromJson(json, MealPlan::class.java)
                        } catch (e: Exception) {
                            null
                        }

                        if (dailyPlan != null && dailyPlan.meals.isNotEmpty()) {
                            processMealPlan(dailyPlan)
                            Toast.makeText(this@MealPlanActivity, "Daily meal plan generated!", Toast.LENGTH_SHORT).show()
                        } else {
                            // unknown shape
                            Toast.makeText(this@MealPlanActivity, "Unexpected plan format from API", Toast.LENGTH_LONG).show()
                            weeklyMealPlan = canonicalDays.associateWith { emptyList<Meal>() }
                        }
                    }

                    updateWeekDisplay()
                    saveUserPreferences()
                } else {
                    Toast.makeText(this@MealPlanActivity, "Failed to generate meal plan: ${response.code()}", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                Toast.makeText(this@MealPlanActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun processMealPlan(plan: MealPlan) {
        // Distribute daily meals across the week as evenly as possible
        val chunkSize = (plan.meals.size / 7).coerceAtLeast(1)
        val mealsPerDay = plan.meals.chunked(chunkSize)

        // ensure 7 keys and fill empty
        val map = canonicalDays.mapIndexed { index, day ->
            day to (mealsPerDay.getOrNull(index) ?: emptyList())
        }.toMap()

        weeklyMealPlan = normalizeWeeklyMap(map)
    }

    private fun processWeeklyMealPlan(plan: WeeklyMealPlan) {
        val mealsByDay = mutableMapOf<String, List<Meal>>()

        plan.week.forEach { (day, dailyPlan) ->
            // API days often come in lowercase, ensure Monday..Sunday capitalization
            val normalized = day.replaceFirstChar { it.uppercaseChar() }.replace("_", " ")
            mealsByDay[normalized] = dailyPlan.meals
        }

        // ensure all canonical days exist
        weeklyMealPlan = normalizeWeeklyMap(mealsByDay)
    }

    /**
     * Ensure final map has exact keys Monday..Sunday.
     * If API returned other day names/ordering, we fill canonical days and default to empty lists.
     */
    private fun normalizeWeeklyMap(input: Map<String, List<Meal>>): Map<String, List<Meal>> {
        val out = mutableMapOf<String, List<Meal>>()
        // try to match by canonical day names (case-insensitive)
        val lookup = input.mapKeys { it.key.lowercase(Locale.getDefault()).trim() }
        canonicalDays.forEach { day ->
            val v = lookup[day.lowercase(Locale.getDefault())] ?: emptyList()
            out[day] = v
        }
        return out
    }

    private fun showDayDetails(day: String) {
        val meals = weeklyMealPlan[day] ?: emptyList()

        // show a short message when there are no meals for the day (helps debug empty UI)
        if (meals.isEmpty()) {
            binding.noMealsText.visibility = View.VISIBLE
            binding.dayMealsRecyclerView.visibility = View.GONE
            binding.noMealsText.text = "No meals for $day"
            return
        } else {
            binding.noMealsText.visibility = View.GONE
            binding.dayMealsRecyclerView.visibility = View.VISIBLE
        }

        binding.dayMealsRecyclerView.adapter = object : androidx.recyclerview.widget.RecyclerView.Adapter<DayMealViewHolder>() {
            override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): DayMealViewHolder {
                val view = layoutInflater.inflate(R.layout.item_day_meal, parent, false)
                return DayMealViewHolder(view)
            }

            override fun onBindViewHolder(holder: DayMealViewHolder, position: Int) {
                val meal = meals[position]
                holder.bind(meal)
                holder.itemView.setOnClickListener {
                    val options = arrayOf("View Recipe Details", "Analyze Nutrition")

                    AlertDialog.Builder(this@MealPlanActivity)
                        .setTitle("Choose an action")
                        .setItems(options) { _, which ->
                            when (which) {
                                0 -> {
                                    val intent = Intent(this@MealPlanActivity, RecipeDetailActivity::class.java)
                                    intent.putExtra("recipeId", meal.id)
                                    intent.putExtra("recipeTitle", meal.title)
                                    startActivity(intent)
                                }
                                1 -> {
                                    val intent = Intent(this@MealPlanActivity, NutritionAnalysisActivity::class.java)
                                    intent.putExtra("mealTitle", meal.title)
                                    startActivity(intent)
                                }
                            }
                        }
                        .show()
                }
            }

            override fun getItemCount(): Int = meals.size
        }
    }

    private fun navigateWeek(offset: Int) {
        currentWeekOffset += offset
        updateWeekDisplay()
        Toast.makeText(this, "Viewing week offset $currentWeekOffset", Toast.LENGTH_SHORT).show()
    }

    private fun updateWeekDisplay() {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.WEEK_OF_YEAR, currentWeekOffset)
        val weekStart = SimpleDateFormat("MMM dd", Locale.getDefault()).format(calendar.time)
        val weekEndCalendar = Calendar.getInstance().apply {
            time = calendar.time
            add(Calendar.DAY_OF_WEEK, 6)
        }
        val weekEnd = SimpleDateFormat("MMM dd", Locale.getDefault()).format(weekEndCalendar.time)
        binding.weekRangeText.text = "$weekStart - $weekEnd"

        // default to Monday (or keep current selection if you later add state)
        showDayDetails("Monday")
    }

    private fun saveMealPlanToFirebase() {
        val userId = auth.currentUser?.uid ?: return
        val weekStart = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().time)
        val ref = database.getReference("meal_plans").child(userId).child(weekStart)
        ref.setValue(weeklyMealPlan)
            .addOnSuccessListener { Toast.makeText(this, "Meal plan saved!", Toast.LENGTH_SHORT).show() }
            .addOnFailureListener { Toast.makeText(this, "Failed to save plan", Toast.LENGTH_SHORT).show() }
    }

    private fun loadUserPreferences() {
        val userId = auth.currentUser?.uid ?: return
        val prefsRef = database.getReference("user_preferences").child(userId)
        prefsRef.get().addOnSuccessListener {
            targetCalories = it.child("targetCalories").getValue(Int::class.java) ?: 2000
            dietPreference = it.child("dietPreference").getValue(String::class.java) ?: ""
        }
    }

    private fun saveUserPreferences() {
        val userId = auth.currentUser?.uid ?: return
        val prefsRef = database.getReference("user_preferences").child(userId)
        prefsRef.setValue(mapOf("targetCalories" to targetCalories, "dietPreference" to dietPreference))
    }

    class DayMealViewHolder(view: android.view.View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(view) {
        private val title = view.findViewById<android.widget.TextView>(R.id.mealName)
        private val type = view.findViewById<android.widget.TextView>(R.id.mealType)
        private val calories = view.findViewById<android.widget.TextView>(R.id.mealCalories)

        fun bind(meal: Meal) {
            title.text = meal.title
            type.text = "Servings: ${meal.servings}"
            calories.text = "Ready in ${meal.readyInMinutes} min"
        }
    }
}
