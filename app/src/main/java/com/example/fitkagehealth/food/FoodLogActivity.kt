package com.example.fitkagehealth.food

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fitkagehealth.BuildConfig
import com.example.fitkagehealth.R
import com.example.fitkagehealth.adapters.FoodLogAdapter
import com.example.fitkagehealth.databinding.ActivityFoodLogBinding
import com.example.fitkagehealth.databinding.DialogAddFoodBinding
import com.example.fitkagehealth.model.FoodEntry
import com.example.fitkagehealth.api.SpoonacularService
import com.example.fitkagehealth.api.IngredientSearchResponse
import com.example.fitkagehealth.api.IngredientInformation
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.*

class FoodLogActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFoodLogBinding
    private lateinit var foodLogAdapter: FoodLogAdapter
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()

    private val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    private val meals = listOf("Breakfast", "Lunch", "Dinner", "Snacks")

    private lateinit var spoonacularService: SpoonacularService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFoodLogBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRetrofit()
        setupUI()
        loadFoodLog()
    }

    private fun setupRetrofit() {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.spoonacular.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        spoonacularService = retrofit.create(SpoonacularService::class.java)
    }

    private fun setupUI() {
        binding.toolbar.setNavigationOnClickListener { onBackPressed() }

        // Setup RecyclerView
        foodLogAdapter = FoodLogAdapter { foodEntry ->
            showEditFoodDialog(foodEntry)
        }

        binding.foodLogRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@FoodLogActivity)
            adapter = foodLogAdapter
        }

        // Add food button
        binding.addFoodButton.setOnClickListener {
            showAddFoodDialog()
        }

        binding.currentDateText.text = formatDateForDisplay(currentDate)
    }

    private fun showAddFoodDialog() {
        val dialogBinding = DialogAddFoodBinding.inflate(layoutInflater)
        val builder = AlertDialog.Builder(this)
            .setTitle("Add Food Entry")
            .setView(dialogBinding.root)
            .setPositiveButton("Add", null) // we'll override to control validation
            .setNeutralButton("Search Spoonacular", null) // will set a click listener after showing
            .setNegativeButton("Cancel", null)

        val dialog = builder.create()
        dialog.setOnShowListener {
            val positive = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            val neutral = dialog.getButton(AlertDialog.BUTTON_NEUTRAL)

            positive.setOnClickListener {
                val foodName = dialogBinding.foodNameEditText.text.toString().trim()
                val calories = dialogBinding.caloriesEditText.text.toString().toIntOrNull() ?: 0
                val protein = dialogBinding.proteinEditText.text.toString().toIntOrNull() ?: 0
                val carbs = dialogBinding.carbsEditText.text.toString().toIntOrNull() ?: 0
                val fat = dialogBinding.fatEditText.text.toString().toIntOrNull() ?: 0
                val mealType = when (dialogBinding.mealTypeGroup.checkedRadioButtonId) {
                    R.id.breakfastRadio -> "Breakfast"
                    R.id.lunchRadio -> "Lunch"
                    R.id.dinnerRadio -> "Dinner"
                    else -> "Snacks"
                }

                if (foodName.isNotEmpty() && calories > 0) {
                    addFoodEntry(foodName, calories, protein, carbs, fat, mealType)
                    dialog.dismiss()
                } else {
                    Toast.makeText(this, "Please enter valid food details (or search Spoonacular)", Toast.LENGTH_SHORT).show()
                }
            }

            neutral.setOnClickListener {
                // Search Spoonacular for ingredient matches using the typed name
                val query = dialogBinding.foodNameEditText.text.toString().trim()
                if (query.isEmpty()) {
                    Toast.makeText(this, "Type a food name to search (e.g. 'chicken')", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                // perform network search
                lifecycleScope.launch {
                    try {
                        binding.progressBar.visibility = android.view.View.VISIBLE
                        val resp = spoonacularService.searchIngredients(query = query, number = 8, apiKey = BuildConfig.SPOONACULAR_API_KEY)
                        if (resp.isSuccessful && resp.body() != null) {
                            val results = resp.body()!!.results
                            if (results.isEmpty()) {
                                Toast.makeText(this@FoodLogActivity, "No ingredients found for \"$query\"", Toast.LENGTH_SHORT).show()
                            } else {
                                // show selectable list
                                val names = results.map { it.name }.toTypedArray()
                                AlertDialog.Builder(this@FoodLogActivity)
                                    .setTitle("Pick an ingredient")
                                    .setItems(names) { _, which ->
                                        val picked = results[which]
                                        // fetch ingredient info (100 g)
                                        fetchIngredientInfoAndPopulate(picked.id, dialogBinding)
                                    }
                                    .show()
                            }
                        } else {
                            Toast.makeText(this@FoodLogActivity, "Ingredient search failed: ${resp.code()}", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(this@FoodLogActivity, "Search error: ${e.message}", Toast.LENGTH_SHORT).show()
                    } finally {
                        binding.progressBar.visibility = android.view.View.GONE
                    }
                }
            }
        }

        dialog.show()
    }

    private suspend fun fetchIngredientInfo(ingredientId: Int): IngredientInformation? = withContext(Dispatchers.IO) {
        try {
            val resp = spoonacularService.getIngredientInformation(id = ingredientId, amount = 100.0, unit = "g", apiKey = BuildConfig.SPOONACULAR_API_KEY)
            if (resp.isSuccessful) {
                resp.body()
            } else null
        } catch (e: Exception) {
            null
        }
    }

    private fun fetchIngredientInfoAndPopulate(ingredientId: Int, dialogBinding: DialogAddFoodBinding) {
        lifecycleScope.launch {
            try {
                binding.progressBar.visibility = android.view.View.VISIBLE
                val info = fetchIngredientInfo(ingredientId)
                if (info == null) {
                    Toast.makeText(this@FoodLogActivity, "Failed to load ingredient info", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                // parse nutrition list for primary macros
                var calories = 0
                var protein = 0
                var carbs = 0
                var fat = 0

                // Some Spoonacular responses include nutrition.nutrients list
                val nutrients = info.nutrition?.nutrients ?: emptyList()
                nutrients.forEach { n ->
                    when (n.name.lowercase(Locale.getDefault())) {
                        "calories" -> calories = n.amount.toInt()
                        "protein" -> protein = n.amount.toInt()
                        "carbohydrates", "carbs" -> carbs = n.amount.toInt()
                        "fat" -> fat = n.amount.toInt()
                    }
                }

                // populate dialog fields
                dialogBinding.foodNameEditText.setText(info.name)
                if (calories > 0) dialogBinding.caloriesEditText.setText(calories.toString())
                if (protein > 0) dialogBinding.proteinEditText.setText(protein.toString())
                if (carbs > 0) dialogBinding.carbsEditText.setText(carbs.toString())
                if (fat > 0) dialogBinding.fatEditText.setText(fat.toString())

                Toast.makeText(this@FoodLogActivity, "Populated nutrition for ${info.name}", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@FoodLogActivity, "Error populating ingredient: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.visibility = android.view.View.GONE
            }
        }
    }

    private fun addFoodEntry(foodName: String, calories: Int, protein: Int, carbs: Int, fat: Int, mealType: String) {
        val userId = auth.currentUser?.uid ?: return
        val foodEntry = FoodEntry(
            id = System.currentTimeMillis().toString(),
            name = foodName,
            calories = calories,
            protein = protein,
            carbs = carbs,
            fat = fat,
            mealType = mealType,
            timestamp = System.currentTimeMillis()
        )

        val foodRef = database.getReference("food_logs")
            .child(userId)
            .child(currentDate)
            .child(mealType)
            .child(foodEntry.id)

        foodRef.setValue(foodEntry)
            .addOnSuccessListener {
                Toast.makeText(this, "Food added successfully", Toast.LENGTH_SHORT).show()
                loadFoodLog()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to add food", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadFoodLog() {
        val userId = auth.currentUser?.uid ?: return
        val foodLogRef = database.getReference("food_logs").child(userId).child(currentDate)

        foodLogRef.addListenerForSingleValueEvent(object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                val foodEntries = mutableListOf<FoodEntry>()
                var totalCalories = 0
                var totalProtein = 0
                var totalCarbs = 0
                var totalFat = 0

                meals.forEach { mealType ->
                    snapshot.child(mealType).children.forEach { foodSnapshot ->
                        val foodEntry = foodSnapshot.getValue(FoodEntry::class.java)
                        foodEntry?.let {
                            foodEntries.add(it)
                            totalCalories += it.calories
                            totalProtein += it.protein
                            totalCarbs += it.carbs
                            totalFat += it.fat
                        }
                    }
                }

                // Sort by timestamp (most recent first)
                foodEntries.sortByDescending { it.timestamp }
                foodLogAdapter.submitList(foodEntries)

                // Update totals
                updateNutritionTotals(totalCalories, totalProtein, totalCarbs, totalFat)
            }

            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                Toast.makeText(this@FoodLogActivity, "Failed to load food log", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun updateNutritionTotals(calories: Int, protein: Int, carbs: Int, fat: Int) {
        binding.totalCaloriesText.text = "$calories cal"
        binding.totalProteinText.text = "${protein}g"
        binding.totalCarbsText.text = "${carbs}g"
        binding.totalFatText.text = "${fat}g"
    }

    private fun formatDateForDisplay(date: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val outputFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            val parsedDate = inputFormat.parse(date)
            outputFormat.format(parsedDate ?: Date())
        } catch (e: Exception) {
            date
        }
    }

    private fun showEditFoodDialog(foodEntry: FoodEntry) {
        val dialogBinding = DialogAddFoodBinding.inflate(layoutInflater)
        dialogBinding.foodNameEditText.setText(foodEntry.name)
        dialogBinding.caloriesEditText.setText(foodEntry.calories.toString())
        dialogBinding.proteinEditText.setText(foodEntry.protein.toString())
        dialogBinding.carbsEditText.setText(foodEntry.carbs.toString())
        dialogBinding.fatEditText.setText(foodEntry.fat.toString())

        // Set the radio button based on meal type
        when (foodEntry.mealType) {
            "Breakfast" -> dialogBinding.breakfastRadio.isChecked = true
            "Lunch" -> dialogBinding.lunchRadio.isChecked = true
            "Dinner" -> dialogBinding.dinnerRadio.isChecked = true
            else -> dialogBinding.snacksRadio.isChecked = true
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle("Edit Food Entry")
            .setView(dialogBinding.root)
            .setPositiveButton("Update") { _, _ ->
                val foodName = dialogBinding.foodNameEditText.text.toString().trim()
                val calories = dialogBinding.caloriesEditText.text.toString().toIntOrNull() ?: 0
                val protein = dialogBinding.proteinEditText.text.toString().toIntOrNull() ?: 0
                val carbs = dialogBinding.carbsEditText.text.toString().toIntOrNull() ?: 0
                val fat = dialogBinding.fatEditText.text.toString().toIntOrNull() ?: 0
                val mealType = when (dialogBinding.mealTypeGroup.checkedRadioButtonId) {
                    R.id.breakfastRadio -> "Breakfast"
                    R.id.lunchRadio -> "Lunch"
                    R.id.dinnerRadio -> "Dinner"
                    else -> "Snacks"
                }

                if (foodName.isNotEmpty() && calories > 0) {
                    updateFoodEntry(foodEntry, foodName, calories, protein, carbs, fat, mealType)
                }
            }
            .setNeutralButton("Delete") { _, _ ->
                deleteFoodEntry(foodEntry)
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()
    }

    private fun updateFoodEntry(oldEntry: FoodEntry, name: String, calories: Int, protein: Int, carbs: Int, fat: Int, mealType: String) {
        val userId = auth.currentUser?.uid ?: return

        // If meal type changed, we need to remove from old location and add to new
        if (oldEntry.mealType != mealType) {
            // Remove from old location
            database.getReference("food_logs")
                .child(userId)
                .child(currentDate)
                .child(oldEntry.mealType)
                .child(oldEntry.id)
                .removeValue()

            // Add to new location
            val newEntry = oldEntry.copy(
                name = name,
                calories = calories,
                protein = protein,
                carbs = carbs,
                fat = fat,
                mealType = mealType
            )

            database.getReference("food_logs")
                .child(userId)
                .child(currentDate)
                .child(mealType)
                .child(newEntry.id)
                .setValue(newEntry)
        } else {
            // Just update the existing entry
            val updatedEntry = oldEntry.copy(
                name = name,
                calories = calories,
                protein = protein,
                carbs = carbs,
                fat = fat
            )

            database.getReference("food_logs")
                .child(userId)
                .child(currentDate)
                .child(mealType)
                .child(updatedEntry.id)
                .setValue(updatedEntry)
        }

        loadFoodLog()
    }

    private fun deleteFoodEntry(foodEntry: FoodEntry) {
        val userId = auth.currentUser?.uid ?: return
        val foodRef = database.getReference("food_logs")
            .child(userId)
            .child(currentDate)
            .child(foodEntry.mealType)
            .child(foodEntry.id)

        foodRef.removeValue()
            .addOnSuccessListener {
                Toast.makeText(this, "Food deleted", Toast.LENGTH_SHORT).show()
                loadFoodLog()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to delete food", Toast.LENGTH_SHORT).show()
            }
    }
}
