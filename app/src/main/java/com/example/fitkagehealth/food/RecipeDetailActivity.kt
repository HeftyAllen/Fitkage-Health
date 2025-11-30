// RecipeDetailActivity.kt
package com.example.fitkagehealth.food

import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.fitkagehealth.BuildConfig
import com.example.fitkagehealth.api.SpoonacularService
import com.example.fitkagehealth.databinding.ActivityRecipeDetailBinding
import com.example.fitkagehealth.model.Recipe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class RecipeDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRecipeDetailBinding
    private lateinit var spoonacularService: SpoonacularService

    // Make nullable to avoid lateinit crash; populate before use
    private var recipe: Recipe? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecipeDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRetrofit()
        setupToolbar()

        // Try to get full parcelable recipe first (supports pre/post TIRAMISU)
        val parcelRecipe = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("recipe", Recipe::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra<Recipe>("recipe")
        }

        val recipeId = intent.getIntExtra("recipeId", -1)
        val recipeTitle = intent.getStringExtra("recipeTitle")

        when {
            parcelRecipe != null -> {
                recipe = parcelRecipe
                populateUiAndLoadDetailsIfNeeded()
            }
            recipeId != -1 -> {
                // show placeholder UI while fetching details
                binding.recipeTitle.text = recipeTitle ?: "Recipe"
                binding.progressBar.visibility = View.VISIBLE
                fetchRecipeById(recipeId)
            }
            !recipeTitle.isNullOrBlank() -> {
                // only a title was passed; show minimal info
                binding.recipeTitle.text = recipeTitle
                binding.readyInMinutes.text = ""
                binding.servings.text = ""
                binding.summaryText.text = ""
                binding.ingredientsText.text = ""
                binding.instructionsText.text = ""
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, "Showing basic info (no recipe details provided)", Toast.LENGTH_SHORT).show()
            }
            else -> {
                Toast.makeText(this, "No recipe data provided", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun setupRetrofit() {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.spoonacular.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        spoonacularService = retrofit.create(SpoonacularService::class.java)
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun populateUiAndLoadDetailsIfNeeded() {
        // Called when recipe (possibly partial) is available
        recipe?.let { r ->
            binding.toolbar.title = r.title
            binding.recipeTitle.text = r.title
            binding.readyInMinutes.text = if (r.readyInMinutes > 0) "${r.readyInMinutes} min" else ""
            binding.servings.text = if (r.servings > 0) "${r.servings} servings" else ""

            if (!r.image.isNullOrBlank()) {
                Glide.with(this)
                    .load(r.image)
                    .into(binding.recipeImage)
            }

            // placeholders while details are loaded (if needed)
            binding.caloriesText.text = "Loading..."
            binding.proteinText.text = "Loading..."
            binding.carbsText.text = "Loading..."
            binding.fatText.text = "Loading..."

            // If the recipe already includes ingredients/instructions/nutrition, update right away
            // otherwise call loadRecipeDetails() to fetch full info
            val hasDetails = (r.extendedIngredients?.isNotEmpty() == true)
                    || (r.analyzedInstructions?.isNotEmpty() == true)
                    || (r.nutrition?.nutrients?.isNotEmpty() == true)

            if (hasDetails) {
                // If the passed Recipe already contains details, use it
                updateRecipeDetails(r)
                binding.progressBar.visibility = View.GONE
            } else {
                // try to get more details using the recipe id (if available)
                if (r.id != 0) {
                    loadRecipeDetails()
                } else {
                    binding.progressBar.visibility = View.GONE
                }
            }
        }
    }

    private fun fetchRecipeById(id: Int) {
        lifecycleScope.launch {
            try {
                val resp = withContext(Dispatchers.IO) {
                    spoonacularService.getRecipeInformation(id = id, apiKey = BuildConfig.SPOONACULAR_API_KEY, includeNutrition = true)
                }

                if (resp.isSuccessful) {
                    // API returns model.Recipe type in your interface, so map directly
                    val detailed = resp.body()
                    if (detailed != null) {
                        recipe = detailed
                        populateUiAndLoadDetailsIfNeeded()
                    } else {
                        Toast.makeText(this@RecipeDetailActivity, "No recipe details returned", Toast.LENGTH_SHORT).show()
                        binding.progressBar.visibility = View.GONE
                    }
                } else {
                    Toast.makeText(this@RecipeDetailActivity, "Failed to load recipe details (code ${resp.code()})", Toast.LENGTH_LONG).show()
                    binding.progressBar.visibility = View.GONE
                }
            } catch (e: Exception) {
                Toast.makeText(this@RecipeDetailActivity, "Error fetching recipe: ${e.message}", Toast.LENGTH_LONG).show()
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun loadRecipeDetails() {
        val id = recipe?.id ?: run {
            binding.progressBar.visibility = View.GONE
            return
        }

        lifecycleScope.launch {
            try {
                binding.progressBar.visibility = View.VISIBLE
                val response = withContext(Dispatchers.IO) {
                    spoonacularService.getRecipeInformation(id = id, apiKey = BuildConfig.SPOONACULAR_API_KEY, includeNutrition = true)
                }

                if (response.isSuccessful) {
                    val detailedRecipe = response.body()
                    if (detailedRecipe != null) {
                        recipe = detailedRecipe
                        updateRecipeDetails(detailedRecipe)
                    } else {
                        Toast.makeText(this@RecipeDetailActivity, "No details returned", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@RecipeDetailActivity, "Failed to load recipe details (code ${response.code()})", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@RecipeDetailActivity, "Error loading details: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    // NOTE: use model.Recipe (parcelable) as you originally intended.
    private fun updateRecipeDetails(detailedRecipe: Recipe) {
        // Update toolbar/title
        binding.toolbar.title = detailedRecipe.title ?: binding.toolbar.title

        // Nutrition information (model.Nutrition -> nutrients)
        detailedRecipe.nutrition?.nutrients?.let { nutrients ->
            val calories = nutrients.find { it.name.equals("Calories", ignoreCase = true) }?.amount?.toInt() ?: 0
            val protein = nutrients.find { it.name.equals("Protein", ignoreCase = true) }?.amount?.toInt() ?: 0
            val carbs = nutrients.find { it.name.equals("Carbohydrates", ignoreCase = true) }?.amount?.toInt() ?: 0
            val fat = nutrients.find { it.name.equals("Fat", ignoreCase = true) }?.amount?.toInt() ?: 0

            binding.caloriesText.text = "$calories"
            binding.proteinText.text = "${protein}g"
            binding.carbsText.text = "${carbs}g"
            binding.fatText.text = "${fat}g"
        } ?: run {
            binding.caloriesText.text = "N/A"
            binding.proteinText.text = "N/A"
            binding.carbsText.text = "N/A"
            binding.fatText.text = "N/A"
        }

        // Ingredients
        detailedRecipe.extendedIngredients?.let { ingredients ->
            val ingredientsText = ingredients.joinToString("\n") { ing ->
                "• ${ing.original}"
            }
            binding.ingredientsText.text = ingredientsText
        } ?: run {
            binding.ingredientsText.text = "No ingredients found"
        }

        // Instructions (analyzed instructions preferred, fallback to summary or instructions string)
        val analyzedSteps = detailedRecipe.analyzedInstructions
            ?.firstOrNull()
            ?.steps

        if (!analyzedSteps.isNullOrEmpty()) {
            val instructionsText = analyzedSteps.joinToString("\n\n") { step ->
                "${step.number}. ${step.step}"
            }
            binding.instructionsText.text = instructionsText
        } else {
            // fallback to summary if no instructions property
            binding.instructionsText.text = detailedRecipe.summary?.replace(Regex("<.*?>"), "") ?: "No instructions found"
        }

        // Summary (remove HTML if present)
        detailedRecipe.summary?.let { summary ->
            val cleanSummary = summary.replace(Regex("<.*?>"), "")
            binding.summaryText.text = cleanSummary
        }

        // Image (ensure binding.recipeImage exists)
        detailedRecipe.image?.let { imageUrl ->
            Glide.with(this)
                .load(imageUrl)
                .into(binding.recipeImage)
        }
    }
}
