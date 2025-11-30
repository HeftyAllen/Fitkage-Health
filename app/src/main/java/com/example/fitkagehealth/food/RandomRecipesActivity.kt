// RandomRecipesActivity.kt
package com.example.fitkagehealth.food

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fitkagehealth.BuildConfig
import com.example.fitkagehealth.databinding.ActivityRandomRecipesBinding
import com.example.fitkagehealth.adapters.RecipeAdapter
import com.example.fitkagehealth.model.Recipe
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class RandomRecipesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRandomRecipesBinding
    private lateinit var recipeAdapter: RecipeAdapter
    private lateinit var spoonacularService: com.example.fitkagehealth.api.SpoonacularService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRandomRecipesBinding.inflate(layoutInflater)
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

        recipeAdapter = RecipeAdapter { recipe ->
            val intent = Intent(this, RecipeDetailActivity::class.java).apply {
                putExtra("recipe", recipe)
            }
            startActivity(intent)
        }

        binding.recipesRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@RandomRecipesActivity)
            adapter = recipeAdapter
        }

        binding.swipeRefresh.setOnRefreshListener {
            loadRandomRecipes()
        }
    }

    private fun loadRandomRecipes() {
        lifecycleScope.launch {
            try {
                binding.progressBar.visibility = android.view.View.VISIBLE

                val response = spoonacularService.getRandomRecipes(
                    number = 15,
                    apiKey = BuildConfig.SPOONACULAR_API_KEY
                )

                if (response.isSuccessful) {
                    val recipes = response.body()?.recipes ?: emptyList()
                    recipeAdapter.submitList(recipes as List<Recipe?>?)
                }
            } catch (e: Exception) {
                // Handle error
            } finally {
                binding.progressBar.visibility = android.view.View.GONE
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }
}