// Models.kt
package com.example.fitkagehealth.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Recipe(
    val id: Int = 0,
    val title: String = "",
    val image: String? = null,
    val imageType: String? = null,
    val servings: Int = 0,
    val readyInMinutes: Int = 0,
    val sourceUrl: String? = null,
    val spoonacularSourceUrl: String? = null,
    val healthScore: Double = 0.0,
    val pricePerServing: Double = 0.0,
    val dairyFree: Boolean = false,
    val glutenFree: Boolean = false,
    val vegan: Boolean = false,
    val vegetarian: Boolean = false,
    val veryHealthy: Boolean = false,
    val cheap: Boolean = false,
    val extendedIngredients: List<ExtendedIngredient>? = null,
    val summary: String? = null,
    val nutrition: Nutrition? = null,
    val analyzedInstructions: List<AnalyzedInstruction>? = null
) : Parcelable

@Parcelize
data class ExtendedIngredient(
    val id: Int = 0,
    val aisle: String? = null,
    val image: String? = null,
    val consistency: String? = null,
    val name: String = "",
    val nameClean: String? = null,
    val original: String = "",
    val originalName: String? = null,
    val amount: Double = 0.0,
    val unit: String? = null,
    val meta: List<String>? = null,
    val measures: Measures? = null
) : Parcelable

@Parcelize
data class Measures(
    val us: MeasureDetail? = null,
    val metric: MeasureDetail? = null
) : Parcelable

@Parcelize
data class MeasureDetail(
    val amount: Double = 0.0,
    val unitShort: String? = null,
    val unitLong: String? = null
) : Parcelable

@Parcelize
data class Nutrition(
    val nutrients: List<Nutrient>? = null,
    val properties: List<Property>? = null,
    val flavonoids: List<Flavonoid>? = null,
    val ingredients: List<Ingredient>? = null,
    val caloricBreakdown: CaloricBreakdown? = null,
    val weightPerServing: WeightPerServing? = null
) : Parcelable

@Parcelize
data class Nutrient(
    val name: String = "",
    val amount: Double = 0.0,
    val unit: String? = null,
    val percentOfDailyNeeds: Double? = null
) : Parcelable

@Parcelize
data class Property(
    val name: String = "",
    val amount: Double = 0.0,
    val unit: String? = null
) : Parcelable

@Parcelize
data class Flavonoid(
    val name: String = "",
    val amount: Double = 0.0,
    val unit: String? = null
) : Parcelable

@Parcelize
data class CaloricBreakdown(
    val percentProtein: Double? = null,
    val percentFat: Double? = null,
    val percentCarbs: Double? = null
) : Parcelable

@Parcelize
data class WeightPerServing(
    val amount: Double = 0.0,
    val unit: String? = null
) : Parcelable

@Parcelize
data class AnalyzedInstruction(
    val name: String? = null,
    val steps: List<InstructionStep>? = null
) : Parcelable

@Parcelize
data class InstructionStep(
    val number: Int = 0,
    val step: String = "",
    val ingredients: List<Ingredient>? = null,
    val equipment: List<Equipment>? = null
) : Parcelable

@Parcelize
data class Ingredient(
    val id: Int = 0,
    val name: String = "",
    val image: String? = null
) : Parcelable

@Parcelize
data class Equipment(
    val id: Int = 0,
    val name: String = "",
    val image: String? = null
) : Parcelable

@Parcelize
data class FoodEntry(
    val id: String = "",
    val name: String = "",
    val calories: Int = 0,
    val protein: Int = 0,
    val carbs: Int = 0,
    val fat: Int = 0,
    val mealType: String = "",
    val timestamp: Long = 0L
) : Parcelable

/**
 * RecipeNutrition - extended nutrition widget model (Spoonacular / nutritionWidget.json).
 * Many endpoints return string values like "350 calories" or "25g" — keep these as strings
 * so the raw API response maps naturally. Make fields nullable in case API omits them.
 *
 * If you need numeric values, parse the strings (e.g. "25g" -> 25) in the calling code.
 */
@Parcelize
data class RecipeNutrition(
    val calories: String? = null,
    val carbohydrates: String? = null,
    val fat: String? = null,
    val protein: String? = null,
    val fiber: String? = null,
    val sugar: String? = null,
    val cholesterol: String? = null,
    val sodium: String? = null,
    val calcium: String? = null,
    val iron: String? = null,
    val vitaminA: String? = null,
    val vitaminC: String? = null,
    val vitaminD: String? = null,
    val magnesium: String? = null,
    val potassium: String? = null,
    val zinc: String? = null,
    val servingSize: String? = null
) : Parcelable

/* ---------------------------
   Non-parcelable network wrapper models (responses)
   --------------------------- */

data class RecipeNutritionResponse(
    val name: String?,
    val amount: Double?,
    val unit: String?,
    val nutrition: Nutrition?
)
data class RandomRecipeResponse(
    val recipes: List<Recipe> = emptyList()
)

data class RecipeSearchResponse(
    val results: List<Recipe> = emptyList(),
    val offset: Int = 0,
    val number: Int = 0,
    val totalResults: Int = 0
)

data class NutritionAnalysisResponse(
    val calories: NutrientInfo?,
    val protein: NutrientInfo?,
    val carbs: NutrientInfo?,
    val fat: NutrientInfo?,
    val fiber: NutrientInfo?,
    val sugar: NutrientInfo?
)

data class NutrientInfo(
    val name: String,
    val amount: Double,
    val unit: String
)
