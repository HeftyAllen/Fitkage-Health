package com.example.fitkagehealth.food

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fitkagehealth.BuildConfig
import com.example.fitkagehealth.databinding.ActivityRecipeSearchBinding
import com.example.fitkagehealth.adapters.RecipeAdapter
import com.example.fitkagehealth.model.Recipe
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class RecipeSearchActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRecipeSearchBinding
    private lateinit var recipeAdapter: RecipeAdapter
    private lateinit var spoonacularService: com.example.fitkagehealth.api.SpoonacularService
    private var searchJob: Job? = null

    // Cached results used for instant local filtering
    private var cachedRecipes: List<Recipe> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecipeSearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRetrofit()
        setupUI()
        loadRandomRecipes()
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

        // Setup RecyclerView
        recipeAdapter = RecipeAdapter { recipe ->
            openRecipeDetail(recipe)
        }

        binding.recipesRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@RecipeSearchActivity)
            adapter = recipeAdapter
        }

        // Live search with local instant filtering + debounced network call
        binding.searchEditText.addTextChangedListener { editable ->
            searchJob?.cancel()
            val query = editable?.toString() ?: ""

            when {
                query.isEmpty() -> {
                    // empty => show random / cached full set
                    if (cachedRecipes.isNotEmpty()) {
                        recipeAdapter.submitList(cachedRecipes)
                        binding.emptyState.visibility = android.view.View.GONE
                    } else {
                        loadRandomRecipes()
                    }
                }
                query.length >= 3 -> {
                    // Immediate local filter for snappy UX
                    if (cachedRecipes.isNotEmpty()) {
                        val localFiltered = filterLocalRecipes(cachedRecipes, query)
                        if (localFiltered.isEmpty()) {
                            // show a loading placeholder while remote search runs
                            binding.emptyState.text = "Searching..."
                            binding.emptyState.visibility = android.view.View.VISIBLE
                            recipeAdapter.submitList(emptyList())
                        } else {
                            binding.emptyState.visibility = android.view.View.GONE
                            recipeAdapter.submitList(localFiltered)
                        }
                    } else {
                        // no cache yet: show loading UI
                        binding.emptyState.text = "Searching..."
                        binding.emptyState.visibility = android.view.View.VISIBLE
                        recipeAdapter.submitList(emptyList())
                    }

                    // Debounced server refresh (keeps UI snappy but updates with authoritative results)
                    searchJob = lifecycleScope.launch {
                        delay(500) // debounce
                        searchRecipes(query)
                    }
                }
                else -> {
                    // under 3 chars: treat as not searching
                    if (cachedRecipes.isNotEmpty()) {
                        recipeAdapter.submitList(cachedRecipes)
                        binding.emptyState.visibility = android.view.View.GONE
                    }
                }
            }
        }

        // Filter buttons
        binding.filterVegetarian.setOnClickListener { searchWithFilter("vegetarian") }
        binding.filterVegan.setOnClickListener { searchWithFilter("vegan") }
        binding.filterGlutenFree.setOnClickListener { searchWithFilter("glutenFree") }
        binding.filterDairyFree.setOnClickListener { searchWithFilter("dairyFree") }

        // Clear filters
        binding.clearFiltersButton.setOnClickListener {
            binding.searchEditText.text?.clear()
            loadRandomRecipes()
        }
    }

    private fun loadRandomRecipes() {
        lifecycleScope.launch {
            try {
                binding.progressBar.visibility = android.view.View.VISIBLE
                binding.emptyState.visibility = android.view.View.GONE

                val response = spoonacularService.getRandomRecipes(
                    number = 10,
                    apiKey = BuildConfig.SPOONACULAR_API_KEY
                )

                if (response.isSuccessful) {
                    val recipes = response.body()?.recipes ?: emptyList()
                    cachedRecipes = recipes // cache for local filtering
                    if (recipes.isEmpty()) {
                        showEmptyState("No recipes found")
                    } else {
                        recipeAdapter.submitList(recipes)
                        binding.emptyState.visibility = android.view.View.GONE
                    }
                } else {
                    showEmptyState("Failed to load recipes")
                    Toast.makeText(this@RecipeSearchActivity, "API Error: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                showEmptyState("Error: ${e.message}")
                Toast.makeText(this@RecipeSearchActivity, "Network error", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.visibility = android.view.View.GONE
            }
        }
    }

    private fun searchRecipes(query: String) {
        lifecycleScope.launch {
            try {
                binding.progressBar.visibility = android.view.View.VISIBLE
                binding.emptyState.visibility = android.view.View.GONE

                val response = spoonacularService.searchRecipes(
                    query = query,
                    apiKey = BuildConfig.SPOONACULAR_API_KEY,
                    number = 20,
                    addRecipeInformation = true // helpful to get images / summary if your API supports it
                )

                if (response.isSuccessful) {
                    val recipes = response.body()?.results ?: emptyList()
                    cachedRecipes = recipes // update cache with authoritative results
                    if (recipes.isEmpty()) {
                        showEmptyState("No recipes found for '$query'")
                    } else {
                        recipeAdapter.submitList(recipes)
                        binding.emptyState.visibility = android.view.View.GONE
                    }
                } else {
                    showEmptyState("Search failed")
                }
            } catch (e: Exception) {
                showEmptyState("Search error")
                Toast.makeText(this@RecipeSearchActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.visibility = android.view.View.GONE
            }
        }
    }

    private fun searchWithFilter(diet: String) {
        val query = binding.searchEditText.text.toString()
        lifecycleScope.launch {
            try {
                binding.progressBar.visibility = android.view.View.VISIBLE
                binding.emptyState.visibility = android.view.View.GONE

                val response = spoonacularService.searchRecipes(
                    query = if (query.isNotEmpty()) query else null,
                    diet = diet,
                    apiKey = BuildConfig.SPOONACULAR_API_KEY,
                    number = 20,
                    addRecipeInformation = true
                )

                if (response.isSuccessful) {
                    val recipes = response.body()?.results ?: emptyList()
                    cachedRecipes = recipes
                    if (recipes.isEmpty()) {
                        showEmptyState("No $diet recipes found")
                    } else {
                        recipeAdapter.submitList(recipes)
                        binding.emptyState.visibility = android.view.View.GONE
                    }
                }
            } catch (e: Exception) {
                showEmptyState("Filter error")
            } finally {
                binding.progressBar.visibility = android.view.View.GONE
            }
        }
    }

    private fun showEmptyState(message: String) {
        binding.emptyState.text = message
        binding.emptyState.visibility = android.view.View.VISIBLE
        recipeAdapter.submitList(emptyList())
    }

    private fun openRecipeDetail(recipe: Recipe) {
        val intent = Intent(this, RecipeDetailActivity::class.java).apply {
            putExtra("recipe", recipe)
        }
        startActivity(intent)
    }

    // -------------------------
    // Local filtering helpers
    // -------------------------
    private fun filterLocalRecipes(source: List<Recipe>, query: String): List<Recipe> {
        val tokens = query.trim().lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (tokens.isEmpty()) return emptyList()

        return source.filter { recipe ->
            val searchable = buildSearchableText(recipe)
            // require all tokens to be present (AND). Change to `any` if you want OR behavior.
            tokens.all { token -> searchable.contains(token) }
        }
    }

    private fun buildSearchableText(recipe: Recipe): String {
        val sb = StringBuilder()
        sb.append(recipe.title ?: "")
        recipe.summary?.let { sb.append(" ").append(stripHtml(it)) }

        recipe.extendedIngredients?.forEach { ing ->
            sb.append(" ").append(ing.name ?: "").append(" ").append(ing.original ?: "")
        }
        return sb.toString().lowercase()
    }

    private fun stripHtml(html: String): String {
        return html.replace(Regex("<.*?>"), " ")
    }
}
