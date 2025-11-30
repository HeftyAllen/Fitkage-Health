package com.example.fitkagehealth.api

import com.example.fitkagehealth.model.AnalyzedInstruction
import com.example.fitkagehealth.model.NutritionAnalysisResponse
import com.example.fitkagehealth.model.RandomRecipeResponse
import com.example.fitkagehealth.model.Recipe
import com.example.fitkagehealth.model.RecipeNutrition
import com.example.fitkagehealth.model.RecipeNutritionResponse
import com.example.fitkagehealth.model.RecipeSearchResponse
import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Extended SpoonacularService with extra endpoints for richer images & ingredient info.
 *
 * Notes:
 * - complexSearch: set addRecipeInformation=true to return recipe image and extra fields for each result.
 * - food/images/search: useful when you want generic food photos for UI placeholders.
 * - ingredients endpoints: helpful to get image or metadata for ingredients.
 */
interface SpoonacularService {

    @GET("recipes/complexSearch")
    suspend fun searchRecipes(
        @Query("query") query: String? = null,
        @Query("diet") diet: String? = null,
        @Query("cuisine") cuisine: String? = null,
        @Query("type") type: String? = null,
        @Query("maxReadyTime") maxReadyTime: Int? = null,
        @Query("minCalories") minCalories: Int? = null,
        @Query("maxCalories") maxCalories: Int? = null,
        @Query("number") number: Int = 20,
        @Query("addRecipeInformation") addRecipeInformation: Boolean = false, // set true to include images, summary, nutrition snippet
        @Query("apiKey") apiKey: String
    ): Response<RecipeSearchResponse>

    @GET("recipes/random")
    suspend fun getRandomRecipes(
        @Query("number") number: Int = 10,
        @Query("apiKey") apiKey: String,
        @Query("tags") tags: String? = null
    ): Response<RandomRecipeResponse>

    @GET("recipes/{id}/information")
    suspend fun getRecipeInformation(
        @Path("id") id: Int,
        @Query("apiKey") apiKey: String,
        @Query("includeNutrition") includeNutrition: Boolean = true
    ): Response<Recipe>

    @GET("recipes/{id}/nutritionWidget.json")
    suspend fun getRecipeNutrition(
        @Path("id") id: Int,
        @Query("apiKey") apiKey: String
    ): Response<RecipeNutrition>

    @GET("recipes/{id}/analyzedInstructions")
    suspend fun getRecipeInstructions(
        @Path("id") id: Int,
        @Query("apiKey") apiKey: String
    ): Response<List<AnalyzedInstruction>>

    // 1️⃣ Analyze nutrition of free-text ingredients
    @FormUrlEncoded
    @POST("recipes/parseIngredients")
    suspend fun analyzeRecipe(
        @Field("ingredientList") ingr: String,
        @Field("servings") servings: Int = 1,
        @Query("apiKey") apiKey: String
    ): Response<List<RecipeNutritionResponse>>

    @GET("mealplanner/generate")
    suspend fun generateMealPlan(
        @Query("timeFrame") timeFrame: String, // "day" or "week"
        @Query("targetCalories") targetCalories: Int,
        @Query("diet") diet: String? = null,
        @Query("exclude") exclude: String? = null,
        @Query("apiKey") apiKey: String
    ): Response<Any>  // flexible response type


    // -----------------------
    // Extra endpoints for images & ingredients
    // -----------------------

    /**
     * Search for stock food images by keyword.
     * Example: GET https://api.spoonacular.com/food/images/search?query=avocado&number=10&apiKey=...
     */
    @GET("food/images/search")
    suspend fun searchFoodImages(
        @Query("query") query: String,
        @Query("number") number: Int = 10,
        @Query("apiKey") apiKey: String
    ): Response<FoodImageSearchResponse>

    /**
     * Search ingredients by name (returns ingredient results with IDs).
     * Useful to then call /ingredients/{id}/information to get the ingredient's image.
     */
    @GET("food/ingredients/search")
    suspend fun searchIngredients(
        @Query("query") query: String,
        @Query("number") number: Int = 10,
        @Query("apiKey") apiKey: String
    ): Response<IngredientSearchResponse>


    @GET("recipes/parseIngredients")
    suspend fun analyzeRecipe(
        @Query("ingredientList") ingr: String,
        @Query("apiKey") apiKey: String
    ): Response<NutritionAnalysisResponse>


    /**
     * Fetch ingredient information (includes image field if present).
     * Example: /food/ingredients/{id}/information
     */
    @GET("food/ingredients/{id}/information")
    suspend fun getIngredientInformation(
        @Path("id") id: Int,
        @Query("amount") amount: Double? = null,
        @Query("unit") unit: String? = null,
        @Query("apiKey") apiKey: String
    ): Response<IngredientInformation>
}


/* -----------------------
   Lightweight response models for extra endpoints
   ----------------------- */

/**
 * Food image entry returned by /food/images/search
 */
data class FoodImage(
    val id: Int?,
    val width: Int?,
    val height: Int?,
    val url: String?,      // direct image url (if provided)
    val license: String? = null,
    val attribution: String? = null
)

data class FoodImageSearchResponse(
    val images: List<FoodImage> = emptyList(),
    val totalResults: Int = 0
)

/**
 * Ingredient search results (food/ingredients/search)
 */
data class IngredientSearchResult(
    val id: Int,
    val name: String,
    val image: String? // image filename (append to Spoonacular image base if needed)
)

data class WeeklyMealPlan(
    val week: Map<String, DailyMealPlan>
)

data class DailyMealPlan(
    val meals: List<Meal>,
    val nutrients: NutrientsSummary
)

data class IngredientSearchResponse(
    val results: List<IngredientSearchResult> = emptyList(),
    val offset: Int = 0,
    val number: Int = 0,
    val totalResults: Int = 0
)

/**
 * Ingredient information model (food/ingredients/{id}/information)
 * Contains e.g. image (file name), possible nutrition, and meta.
 */
data class IngredientInformation(
    val id: Int,
    val name: String,
    val image: String? = null, // file name, e.g. "apple.jpg". To build URL, use Spoonacular image base.
    val possibleUnits: List<String>? = null,
    val nutrition: IngredientNutrition? = null
)

data class IngredientNutrition(
    val nutrients: List<Nutrient> = emptyList()
)



/**
 * Slim Nutrient model for mealplan & ingredient nutrition summaries.
 * If you have an existing Nutrient data class elsewhere, remove duplicates.
 */
data class Nutrient(
    val name: String,
    val amount: Double,
    val unit: String,
    val percentOfDailyNeeds: Double?
)

data class MealPlan(
    val meals: List<Meal>,
    val nutrients: NutrientsSummary
)

data class Meal(
    val id: Int,
    val imageType: String,
    val title: String,
    val readyInMinutes: Int,
    val servings: Int,
    val sourceUrl: String
)

data class NutrientsSummary(
    val calories: Double,
    val protein: Double,
    val fat: Double,
    val carbohydrates: Double
)
