package com.example.fitkagehealth.food

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.fitkagehealth.BuildConfig
import com.example.fitkagehealth.databinding.ActivityNutritionAnalysisBinding
import com.example.fitkagehealth.api.SpoonacularService
import com.example.fitkagehealth.model.NutritionAnalysisResponse
import com.example.fitkagehealth.model.RecipeNutritionResponse
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class NutritionAnalysisActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNutritionAnalysisBinding
    private lateinit var spoonacularService: SpoonacularService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNutritionAnalysisBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRetrofit()

        // Pre-fill if mealTitle passed
        val mealTitle = intent.getStringExtra("mealTitle")
        if (!mealTitle.isNullOrBlank()) {
            binding.foodInputEditText.setText(mealTitle)
        }

        binding.analyzeButton.setOnClickListener {
            val text = binding.foodInputEditText.text.toString().trim()
            if (text.isEmpty()) {
                Toast.makeText(this, "Enter ingredients or recipe text to analyze", Toast.LENGTH_SHORT).show()
            } else {
                analyzeIngredients(text)
            }
        }

        // Quick analyze buttons (optional shortcuts)
        binding.quickAnalyzeBreakfast.setOnClickListener { binding.foodInputEditText.setText("2 eggs, 1 slice bread") ; binding.analyzeButton.performClick() }
        binding.quickAnalyzeLunch.setOnClickListener { binding.foodInputEditText.setText("150g chicken breast, 1 cup rice") ; binding.analyzeButton.performClick() }
        binding.quickAnalyzeDinner.setOnClickListener { binding.foodInputEditText.setText("200g salmon, mixed veggies") ; binding.analyzeButton.performClick() }
        binding.quickAnalyzeSnack.setOnClickListener { binding.foodInputEditText.setText("1 apple") ; binding.analyzeButton.performClick() }
    }

    private fun setupRetrofit() {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.spoonacular.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        spoonacularService = retrofit.create(SpoonacularService::class.java)
    }

    private fun analyzeIngredients(ingredientText: String) {
        lifecycleScope.launch {
            binding.progressBar.visibility = View.VISIBLE
            binding.analysisResultCard.visibility = View.GONE
            try {
                // Try the POST parseIngredients endpoint (returns List<RecipeNutritionResponse>)
                val resp = withContext(Dispatchers.IO) {
                    try {
                        spoonacularService.analyzeRecipe(ingr = ingredientText, servings = 1, apiKey = BuildConfig.SPOONACULAR_API_KEY)
                    } catch (e: Exception) {
                        null
                    }
                }

                if (resp != null && resp.isSuccessful && resp.body() != null) {
                    val parsed: List<RecipeNutritionResponse>? = resp.body()
                    displayParsedIngredientList(parsed)
                } else {
                    // Fallback: try the GET-style analyze that returns NutritionAnalysisResponse
                    try {
                        val g = withContext(Dispatchers.IO) {
                            spoonacularService.analyzeRecipe(ingredientText, BuildConfig.SPOONACULAR_API_KEY)
                        }
                        if (g.isSuccessful && g.body() != null) {
                            val body: NutritionAnalysisResponse = g.body()!!
                            displayNutritionAnalysisResponse(body)
                        } else {
                            Toast.makeText(this@NutritionAnalysisActivity, "Failed to retrieve nutrition analysis (code ${resp?.code() ?: "unknown"})", Toast.LENGTH_LONG).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(this@NutritionAnalysisActivity, "Nutrition analysis error: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(this@NutritionAnalysisActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun displayParsedIngredientList(parsed: List<RecipeNutritionResponse>?) {
        if (parsed == null || parsed.isEmpty()) {
            Toast.makeText(this, "No parsed ingredients returned", Toast.LENGTH_SHORT).show()
            return
        }

        // Build a readable summary and try to compute simple totals where possible
        val sb = StringBuilder()
        var totalCalories = 0.0
        var totalProtein = 0.0
        var totalCarbs = 0.0
        var totalFat = 0.0

        parsed.forEach { item: RecipeNutritionResponse ->
            val name = item.name ?: "Item"
            val amount = item.amount ?: 0.0
            val unit = item.unit ?: ""
            sb.append("$name — ${amount.toInt()} $unit")
            // If the item contains nutrition object with aggregated nutrients, try to sum
            item.nutrition?.nutrients?.forEach { n ->
                when (n.name.lowercase()) {
                    "calories" -> totalCalories += n.amount
                    "protein" -> totalProtein += n.amount
                    "carbohydrates", "carbs" -> totalCarbs += n.amount
                    "fat" -> totalFat += n.amount
                }
            }
            sb.append("\n")
        }

        // Update UI on main thread (we are already on Main because launched in lifecycleScope)
        binding.analyzedFoodText.text = sb.toString()
        binding.caloriesValue.text = if (totalCalories > 0) "${totalCalories.toInt()} cal" else "N/A"
        binding.proteinValue.text = if (totalProtein > 0) "${totalProtein.toInt()}g" else "N/A"
        binding.carbsValue.text = if (totalCarbs > 0) "${totalCarbs.toInt()}g" else "N/A"
        binding.fatValue.text = if (totalFat > 0) "${totalFat.toInt()}g" else "N/A"

        // progress bars: set to a percentage of a typical daily target (optional, guarded)
        try {
            val dailyCalories = 2000.0
            binding.caloriesProgress.progress = ((totalCalories / dailyCalories) * 100).toInt().coerceIn(0, 100)
            // protein/carbs/fat: assume simple 200g/300g/70g daily placeholders (just for visual)
            binding.proteinProgress.progress = ((totalProtein / 200.0) * 100).toInt().coerceIn(0, 100)
            binding.carbsProgress.progress = ((totalCarbs / 300.0) * 100).toInt().coerceIn(0, 100)
            binding.fatProgress.progress = ((totalFat / 70.0) * 100).toInt().coerceIn(0, 100)
        } catch (ignored: Exception) { /* ignore if progress bars missing */ }

        binding.fiberValue.text = "-" // not computed here
        binding.sugarValue.text = "-"

        binding.analysisResultCard.visibility = View.VISIBLE
    }

    private fun displayNutritionAnalysisResponse(body: NutritionAnalysisResponse) {
        // Safely extract values if present
        val calories = body.calories?.amount ?: 0.0
        val protein = body.protein?.amount ?: 0.0
        val carbs = body.carbs?.amount ?: 0.0
        val fat = body.fat?.amount ?: 0.0
        val fiber = body.fiber?.amount ?: 0.0
        val sugar = body.sugar?.amount ?: 0.0

        binding.caloriesValue.text = "${calories.toInt()} cal"
        binding.proteinValue.text = "${protein.toInt()}g"
        binding.carbsValue.text = "${carbs.toInt()}g"
        binding.fatValue.text = "${fat.toInt()}g"
        binding.fiberValue.text = if (fiber > 0) "${fiber.toInt()}g" else "N/A"
        binding.sugarValue.text = if (sugar > 0) "${sugar.toInt()}g" else "N/A"

        // provide a small human readable header listing the analyzed text
        binding.analyzedFoodText.text = binding.foodInputEditText.text.toString().trim()

        // update progress bars (same heuristics as above)
        try {
            val dailyCalories = 2000.0
            binding.caloriesProgress.progress = ((calories / dailyCalories) * 100).toInt().coerceIn(0, 100)
            binding.proteinProgress.progress = ((protein / 200.0) * 100).toInt().coerceIn(0, 100)
            binding.carbsProgress.progress = ((carbs / 300.0) * 100).toInt().coerceIn(0, 100)
            binding.fatProgress.progress = ((fat / 70.0) * 100).toInt().coerceIn(0, 100)
        } catch (ignored: Exception) { /* ignore */ }

        binding.analysisResultCard.visibility = View.VISIBLE
    }
}
