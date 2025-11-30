package com.example.fitkagehealth

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.fitkagehealth.R
import com.example.fitkagehealth.adapters.RecipeHistoryAdapter
import com.example.fitkagehealth.adapters.WorkoutHistoryAdapter
import com.example.fitkagehealth.databinding.ActivityProfileBinding
import com.example.fitkagehealth.databinding.StatItemBinding
import com.example.fitkagehealth.model.Achievement
import com.example.fitkagehealth.model.ProgressEntry
import com.example.fitkagehealth.model.RecipeHistory
import com.example.fitkagehealth.model.WorkoutSession
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private lateinit var workoutHistoryAdapter: WorkoutHistoryAdapter
    private lateinit var recipeHistoryAdapter: RecipeHistoryAdapter

    private var selectedFilter = "week" // week, month, year, all
    private var selectedStartDate: Date? = null
    private var selectedEndDate: Date? = null

    // Profile image picker
    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { uploadProfileImage(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        setupAdapters()
        loadUserData()
        setupFilterListeners()
        loadStatistics()
        loadWorkoutHistory()
        loadRecipeHistory()
        loadProgressData()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.profile_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_export -> {
                exportData()
                true
            }
            R.id.action_goals -> {
                setupGoals()
                true
            }
            R.id.action_achievements -> {
                showAchievements()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupUI() {
        // Toolbar setup
        binding.toolbar.setNavigationOnClickListener { onBackPressed() }
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Profile image click
        binding.profileImage.setOnClickListener {
            pickImage.launch("image/*")
        }

        // Filter setup
        val filterOptions = arrayOf("This Week", "This Month", "This Year", "All Time")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, filterOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.filterSpinner.adapter = adapter

        // Date range selectors
        binding.selectStartDate.setOnClickListener { showDatePicker(true) }
        binding.selectEndDate.setOnClickListener { showDatePicker(false) }
        binding.applyDateRange.setOnClickListener { applyDateRangeFilter() }
        binding.clearDateRange.setOnClickListener { clearDateRangeFilter() }

        // Tab selection
        binding.statsTab.setOnClickListener { showStatsTab() }
        binding.workoutsTab.setOnClickListener { showWorkoutsTab() }
        binding.recipesTab.setOnClickListener { showRecipesTab() }
        binding.progressTab.setOnClickListener { showProgressTab() }

        // Initialize with stats tab
        showStatsTab()
    }

    private fun setupAdapters() {
        workoutHistoryAdapter = WorkoutHistoryAdapter { session ->
            showWorkoutDetails(session)
        }

        recipeHistoryAdapter = RecipeHistoryAdapter { recipe ->
            showRecipeDetails(recipe)
        }

        binding.workoutHistoryRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@ProfileActivity)
            adapter = workoutHistoryAdapter
        }

        binding.recipeHistoryRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@ProfileActivity)
            adapter = recipeHistoryAdapter
        }
    }

    private fun setupFilterListeners() {
        binding.filterSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedFilter = when (position) {
                    0 -> "week"
                    1 -> "month"
                    2 -> "year"
                    else -> "all"
                }
                loadStatistics()
                loadWorkoutHistory()
                loadRecipeHistory()
                loadProgressData()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun loadUserData() {
        val userId = auth.currentUser?.uid ?: return
        val userRef = database.getReference("users").child(userId)

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val name = snapshot.child("name").getValue(String::class.java) ?: ""
                val surname = snapshot.child("surname").getValue(String::class.java) ?: ""
                val email = snapshot.child("email").getValue(String::class.java) ?: ""
                val weight = snapshot.child("weight_kg").getValue(String::class.java) ?: "0.0"

                binding.userName.text = "$name $surname"
                binding.userEmail.text = email

                // update top-level current weight TextView (ensure your XML has this id)
                binding.currentWeight.text = "$weight kg"

                // Load profile image
                val imageUrl = snapshot.child("profileImageUrl").getValue(String::class.java)
                if (!imageUrl.isNullOrEmpty()) {
                    Glide.with(this@ProfileActivity)
                        .load(imageUrl)
                        .circleCrop()
                        .into(binding.profileImage)
                } else {
                    // Load from base64 if URL not available
                    val base64Image = snapshot.child("profileImageBase64").getValue(String::class.java)
                    if (!base64Image.isNullOrEmpty()) {
                        try {
                            val imageBytes = android.util.Base64.decode(base64Image, android.util.Base64.DEFAULT)
                            val bitmap = android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                            binding.profileImage.setImageBitmap(bitmap)
                        } catch (e: Exception) {
                            // Use default if decoding fails
                            binding.profileImage.setImageResource(R.drawable.ic_default_profile)
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error
            }
        })
    }

    private fun uploadProfileImage(imageUri: Uri) {
        val userId = auth.currentUser?.uid ?: return
        val storageRef = storage.reference.child("profile_images/$userId.jpg")

        lifecycleScope.launch {
            try {
                binding.profileProgress.visibility = View.VISIBLE

                // Upload image
                storageRef.putFile(imageUri).await()

                // Get download URL
                val downloadUrl = storageRef.downloadUrl.await()

                // Save URL to database
                database.getReference("users")
                    .child(userId)
                    .child("profileImageUrl")
                    .setValue(downloadUrl.toString())
                    .await()

                // Update image view
                Glide.with(this@ProfileActivity)
                    .load(downloadUrl)
                    .circleCrop()
                    .into(binding.profileImage)

                binding.profileProgress.visibility = View.GONE

            } catch (e: Exception) {
                binding.profileProgress.visibility = View.GONE
                // Handle error
            }
        }
    }

    private fun loadStatistics() {
        val userId = auth.currentUser?.uid ?: return
        val (startDate, endDate) = getDateRange(selectedFilter)

        // Load workout statistics
        database.getReference("workout_sessions").child(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var totalWorkouts = 0
                    var totalCalories = 0
                    var totalDuration = 0L
                    var completedExercises = 0

                    snapshot.children.forEach { sessionSnapshot ->
                        val session = sessionSnapshot.getValue(WorkoutSession::class.java)
                        session?.let {
                            val sessionDate = Date(it.startTime)
                            if (isDateInRange(sessionDate, startDate, endDate)) {
                                totalWorkouts++
                                totalCalories += it.caloriesBurned
                                totalDuration += it.totalDuration
                                completedExercises += it.completedExercises
                            }
                        }
                    }

                    updateStatItem(binding.totalWorkoutsItem, totalWorkouts.toString(), "Workouts")
                    updateStatItem(binding.caloriesItem, "$totalCalories", "Calories")
                    updateStatItem(binding.timeItem, formatDuration(totalDuration), "Exercise Time")
                    updateStatItem(binding.exercisesItem, completedExercises.toString(), "Exercises")
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle error
                }
            })

        // Load recipe statistics
        database.getReference("recipe_history").child(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var recipesCooked = 0
                    var totalRecipeCalories = 0

                    snapshot.children.forEach { recipeSnapshot ->
                        val recipe = recipeSnapshot.getValue(RecipeHistory::class.java)
                        recipe?.let {
                            val recipeDate = Date(it.timestamp)
                            if (isDateInRange(recipeDate, startDate, endDate)) {
                                recipesCooked++
                                totalRecipeCalories += it.calories
                            }
                        }
                    }

                    // If you have included stat items for recipe stats, update them similarly:
                    // e.g. updateStatItem(binding.recipesCookedItem, recipesCooked.toString(), "Recipes Cooked")
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle error
                }
            })
    }

    // Updated to accept StatItemBinding (generated from stat_item.xml include)
    private fun updateStatItem(itemBinding: StatItemBinding, value: String, label: String) {
        itemBinding.statValue.text = value
        itemBinding.statLabel.text = label
    }

    private fun loadWorkoutHistory() {
        val userId = auth.currentUser?.uid ?: return
        val (startDate, endDate) = getDateRange(selectedFilter)

        database.getReference("workout_sessions").child(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val sessions = mutableListOf<WorkoutSession>()

                    snapshot.children.forEach { sessionSnapshot ->
                        val session = sessionSnapshot.getValue(WorkoutSession::class.java)
                        session?.let {
                            val sessionDate = Date(it.startTime)
                            if (isDateInRange(sessionDate, startDate, endDate)) {
                                sessions.add(it)
                            }
                        }
                    }

                    // Sort by timestamp (newest first)
                    sessions.sortByDescending { it.startTime }
                    workoutHistoryAdapter.submitList(sessions)

                    binding.emptyWorkouts.visibility = if (sessions.isEmpty()) View.VISIBLE else View.GONE
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle error
                }
            })
    }

    private fun loadRecipeHistory() {
        val userId = auth.currentUser?.uid ?: return
        val (startDate, endDate) = getDateRange(selectedFilter)

        database.getReference("recipe_history").child(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val recipes = mutableListOf<RecipeHistory>()

                    snapshot.children.forEach { recipeSnapshot ->
                        val recipe = recipeSnapshot.getValue(RecipeHistory::class.java)
                        recipe?.let {
                            val recipeDate = Date(it.timestamp)
                            if (isDateInRange(recipeDate, startDate, endDate)) {
                                recipes.add(it)
                            }
                        }
                    }

                    // Sort by timestamp (newest first)
                    recipes.sortByDescending { it.timestamp }
                    recipeHistoryAdapter.submitList(recipes)

                    binding.emptyRecipes.visibility = if (recipes.isEmpty()) View.VISIBLE else View.GONE
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle error
                }
            })
    }

    private fun loadProgressData() {
        val userId = auth.currentUser?.uid ?: return
        val (startDate, endDate) = getDateRange(selectedFilter)

        database.getReference("progress_entries").child(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val progressEntries = mutableListOf<ProgressEntry>()
                    var weightChange = 0.0
                    var initialWeight: Double? = null
                    var currentWeight: Double? = null

                    snapshot.children.forEach { entrySnapshot ->
                        val entry = entrySnapshot.getValue(ProgressEntry::class.java)
                        entry?.let {
                            val entryDate = Date(it.timestamp)
                            if (isDateInRange(entryDate, startDate, endDate)) {
                                progressEntries.add(it)

                                // Track weight change
                                if (initialWeight == null) {
                                    initialWeight = it.weightKg
                                }
                                currentWeight = it.weightKg
                            }
                        }
                    }

                    // Calculate weight change
                    if (initialWeight != null && currentWeight != null) {
                        weightChange = currentWeight - initialWeight
                    }

                    // Update progress UI
                    binding.weightChange.text = String.format(Locale.getDefault(), "%.1f kg", weightChange)
                    binding.weightChange.setTextColor(
                        if (weightChange >= 0)
                            getColor(R.color.red)
                        else
                            getColor(R.color.green)
                    )

                    // Load progress photos (most recent)
                    progressEntries.sortedByDescending { it.timestamp }
                        .firstOrNull()?.photos?.take(3)?.forEachIndexed { index, photoUrl ->
                            when (index) {
                                0 -> loadProgressPhoto(photoUrl, binding.progressPhoto1)
                                1 -> loadProgressPhoto(photoUrl, binding.progressPhoto2)
                                2 -> loadProgressPhoto(photoUrl, binding.progressPhoto3)
                            }
                        }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle error
                }
            })
    }

    private fun loadProgressPhoto(photoUrl: String, imageView: android.widget.ImageView) {
        Glide.with(this)
            .load(photoUrl)
            .centerCrop()
            .placeholder(R.drawable.ic_default_progress)
            .into(imageView)
    }

    private fun isDateInRange(date: Date, startDate: Date, endDate: Date): Boolean {
        return date.time >= startDate.time && date.time <= endDate.time
    }

    private fun getDateRange(filter: String): Pair<Date, Date> {
        val calendar = Calendar.getInstance()
        val endDate = calendar.time

        calendar.time = endDate

        when (filter) {
            "week" -> calendar.add(Calendar.WEEK_OF_YEAR, -1)
            "month" -> calendar.add(Calendar.MONTH, -1)
            "year" -> calendar.add(Calendar.YEAR, -1)
            else -> calendar.time = Date(0) // All time
        }

        val startDate = calendar.time
        return Pair(startDate, endDate)
    }

    private fun formatDuration(millis: Long): String {
        val minutes = millis / 60000
        val seconds = (millis % 60000) / 1000
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }

    private fun showDatePicker(isStartDate: Boolean) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            this,
            { _, year, month, day ->
                val selectedDate = Calendar.getInstance().apply {
                    set(year, month, day)
                }.time

                if (isStartDate) {
                    selectedStartDate = selectedDate
                    binding.startDateText.text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(selectedDate)
                } else {
                    selectedEndDate = selectedDate
                    binding.endDateText.text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(selectedDate)
                }
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun applyDateRangeFilter() {
        if (selectedStartDate != null && selectedEndDate != null) {
            // Use custom date range
            loadStatistics()
            loadWorkoutHistory()
            loadRecipeHistory()
            loadProgressData()
        }
    }

    private fun clearDateRangeFilter() {
        selectedStartDate = null
        selectedEndDate = null
        binding.startDateText.text = "Start Date"
        binding.endDateText.text = "End Date"
        binding.filterSpinner.setSelection(0) // Reset to week
    }

    // Tab navigation methods
    private fun showStatsTab() {
        setActiveTab(binding.statsTab)
        binding.statsContent.visibility = View.VISIBLE
        binding.workoutsContent.visibility = View.GONE
        binding.recipesContent.visibility = View.GONE
        binding.progressContent.visibility = View.GONE
    }

    private fun showWorkoutsTab() {
        setActiveTab(binding.workoutsTab)
        binding.statsContent.visibility = View.GONE
        binding.workoutsContent.visibility = View.VISIBLE
        binding.recipesContent.visibility = View.GONE
        binding.progressContent.visibility = View.GONE
    }

    private fun showRecipesTab() {
        setActiveTab(binding.recipesTab)
        binding.statsContent.visibility = View.GONE
        binding.workoutsContent.visibility = View.GONE
        binding.recipesContent.visibility = View.VISIBLE
        binding.progressContent.visibility = View.GONE
    }

    private fun showProgressTab() {
        setActiveTab(binding.progressTab)
        binding.statsContent.visibility = View.GONE
        binding.workoutsContent.visibility = View.GONE
        binding.recipesContent.visibility = View.GONE
        binding.progressContent.visibility = View.VISIBLE
    }

    private fun setActiveTab(activeTab: View) {
        val tabs = listOf(binding.statsTab, binding.workoutsTab, binding.recipesTab, binding.progressTab)
        tabs.forEach { tab ->
            tab.isSelected = tab == activeTab
        }
    }

    private fun showWorkoutDetails(session: WorkoutSession) {
        // Implement workout details dialog
        android.app.AlertDialog.Builder(this)
            .setTitle(session.workoutPlan.name)
            .setMessage(
                "Date: ${SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault()).format(Date(session.startTime))}\n" +
                        "Duration: ${formatDuration(session.totalDuration)}\n" +
                        "Calories: ${session.caloriesBurned} cal\n" +
                        "Exercises: ${session.completedExercises}/${session.totalExercises}\n" +
                        "Rating: ${session.rating}/5\n" +
                        "Notes: ${session.notes}"
            )
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showRecipeDetails(recipe: RecipeHistory) {
        // Implement recipe details dialog
        android.app.AlertDialog.Builder(this)
            .setTitle(recipe.recipeTitle)
            .setMessage(
                "Date: ${SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(recipe.timestamp))}\n" +
                        "Calories: ${recipe.calories} cal\n" +
                        "Protein: ${recipe.protein}g\n" +
                        "Carbs: ${recipe.carbs}g\n" +
                        "Fat: ${recipe.fat}g\n" +
                        "Rating: ${recipe.rating ?: "Not rated"}/5"
            )
            .setPositiveButton("OK", null)
            .show()
    }

    // Feature 1: Export Data
    private fun exportData() {
        lifecycleScope.launch {
            try {
                // Collect all data
                val workoutData = getWorkoutDataForExport()
                val recipeData = getRecipeDataForExport()
                val progressData = getProgressDataForExport()

                // Create CSV content
                val csvContent = buildCsvContent(workoutData, recipeData, progressData)

                // Save to file and share
                saveAndShareCsv(csvContent, "fitkage_data_export.csv")

            } catch (e: Exception) {
                android.widget.Toast.makeText(this@ProfileActivity, "Export failed: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun getWorkoutDataForExport(): List<WorkoutSession> {
        val userId = auth.currentUser?.uid ?: return emptyList()
        return database.getReference("workout_sessions").child(userId).get().await()
            .children.mapNotNull { it.getValue(WorkoutSession::class.java) }
    }

    private suspend fun getRecipeDataForExport(): List<RecipeHistory> {
        val userId = auth.currentUser?.uid ?: return emptyList()
        return database.getReference("recipe_history").child(userId).get().await()
            .children.mapNotNull { it.getValue(RecipeHistory::class.java) }
    }

    private suspend fun getProgressDataForExport(): List<ProgressEntry> {
        val userId = auth.currentUser?.uid ?: return emptyList()
        return database.getReference("progress_entries").child(userId).get().await()
            .children.mapNotNull { it.getValue(ProgressEntry::class.java) }
    }

    private fun buildCsvContent(
        workouts: List<WorkoutSession>,
        recipes: List<RecipeHistory>,
        progress: List<ProgressEntry>
    ): String {
        val sb = StringBuilder()

        // Workout data
        sb.append("Workout History\n")
        sb.append("Date,Workout Name,Duration,Calories,Exercises Completed,Rating\n")
        workouts.forEach { session ->
            sb.append("${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(session.startTime))},")
            sb.append("${session.workoutPlan.name.replace(",", ";")},")
            sb.append("${formatDuration(session.totalDuration)},")
            sb.append("${session.caloriesBurned},")
            sb.append("${session.completedExercises},")
            sb.append("${session.rating}\n")
        }

        sb.append("\nRecipe History\n")
        sb.append("Date,Recipe Name,Calories,Protein,Carbs,Fat,Rating\n")
        recipes.forEach { recipe ->
            sb.append("${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(recipe.timestamp))},")
            sb.append("${recipe.recipeTitle.replace(",", ";")},")
            sb.append("${recipe.calories},")
            sb.append("${recipe.protein},")
            sb.append("${recipe.carbs},")
            sb.append("${recipe.fat},")
            sb.append("${recipe.rating ?: ""}\n")
        }

        return sb.toString()
    }

    private fun saveAndShareCsv(content: String, fileName: String) {
        try {
            val file = java.io.File(getExternalFilesDir(null), fileName)
            file.writeText(content)

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, androidx.core.content.FileProvider.getUriForFile(
                    this@ProfileActivity,
                    "${packageName}.provider",
                    file
                ))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            startActivity(Intent.createChooser(shareIntent, "Export Data"))
        } catch (e: Exception) {
            android.widget.Toast.makeText(this, "Failed to export data", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    // Feature 2: Set Fitness Goals
    private fun setupGoals() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_fitness_goals, null)

        // Load current goals
        val userId = auth.currentUser?.uid
        if (userId != null) {
            database.getReference("user_goals").child(userId).get()
                .addOnSuccessListener { snapshot ->
                    snapshot.child("targetWeight").getValue(Double::class.java)?.let {
                        dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.targetWeight).setText(it.toString())
                    }
                    snapshot.child("dailyCalorieGoal").getValue(Int::class.java)?.let {
                        dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.dailyCalorieGoal).setText(it.toString())
                    }
                    snapshot.child("weeklyWorkoutsGoal").getValue(Int::class.java)?.let {
                        dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.weeklyWorkoutsGoal).setText(it.toString())
                    }
                    snapshot.child("dailyStepsGoal").getValue(Int::class.java)?.let {
                        dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.dailyStepsGoal).setText(it.toString())
                    }
                }
        }

        android.app.AlertDialog.Builder(this)
            .setTitle("Set Fitness Goals")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                saveFitnessGoals(dialogView)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveFitnessGoals(dialogView: View) {
        val targetWeight = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.targetWeight).text.toString().toDoubleOrNull()
        val dailyCalorieGoal = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.dailyCalorieGoal).text.toString().toIntOrNull()
        val weeklyWorkoutsGoal = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.weeklyWorkoutsGoal).text.toString().toIntOrNull()
        val dailyStepsGoal = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.dailyStepsGoal).text.toString().toIntOrNull()

        val userId = auth.currentUser?.uid ?: return

        val goals = mapOf(
            "targetWeight" to targetWeight,
            "dailyCalorieGoal" to dailyCalorieGoal,
            "weeklyWorkoutsGoal" to weeklyWorkoutsGoal,
            "dailyStepsGoal" to dailyStepsGoal
        )

        database.getReference("user_goals").child(userId).setValue(goals)
            .addOnSuccessListener {
                android.widget.Toast.makeText(this, "Goals saved successfully!", android.widget.Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                android.widget.Toast.makeText(this, "Failed to save goals", android.widget.Toast.LENGTH_SHORT).show()
            }
    }

    // Feature 3: Achievement System
    private fun showAchievements() {
        loadAchievements()
    }

    private fun loadAchievements() {
        val userId = auth.currentUser?.uid ?: return

        database.getReference("user_achievements").child(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val achievements = mutableListOf<Achievement>()
                    snapshot.children.forEach { achievementSnapshot ->
                        val achievement = achievementSnapshot.getValue(Achievement::class.java)
                        achievement?.let { achievements.add(it) }
                    }

                    // If no achievements exist, initialize default ones
                    if (achievements.isEmpty()) {
                        initializeDefaultAchievements(userId)
                    } else {
                        displayAchievements(achievements)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle error
                }
            })
    }

    private fun initializeDefaultAchievements(userId: String) {
        val defaultAchievements = listOf(
            Achievement(
                id = "first_workout",
                title = "First Workout",
                description = "Complete your first workout",
                iconRes = R.drawable.ic_achievement,
                unlocked = false,
                progress = 0,
                target = 1
            ),
            Achievement(
                id = "workout_streak_7",
                title = "Weekly Warrior",
                description = "Complete workouts for 7 consecutive days",
                iconRes = R.drawable.ic_achievement,
                unlocked = false,
                progress = 0,
                target = 7
            ),
            Achievement(
                id = "calories_10000",
                title = "Calorie Crusher",
                description = "Burn 10,000 total calories",
                iconRes = R.drawable.ic_achievement,
                unlocked = false,
                progress = 0,
                target = 10000
            ),
            Achievement(
                id = "recipes_10",
                title = "Master Chef",
                description = "Cook 10 different recipes",
                iconRes = R.drawable.ic_achievement,
                unlocked = false,
                progress = 0,
                target = 10
            )
        )

        val achievementsMap = defaultAchievements.associateBy { it.id }
        database.getReference("user_achievements").child(userId).setValue(achievementsMap)
            .addOnSuccessListener {
                displayAchievements(defaultAchievements)
            }
    }

    private fun displayAchievements(achievements: List<Achievement>) {
        val achievementItems = achievements.map { achievement ->
            "${if (achievement.unlocked) "✓" else "○"} ${achievement.title} - ${achievement.progress}/${achievement.target}"
        }.toTypedArray()

        android.app.AlertDialog.Builder(this)
            .setTitle("Your Achievements")
            .setItems(achievementItems) { _, which ->
                val achievement = achievements[which]
                showAchievementDetails(achievement)
            }
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showAchievementDetails(achievement: Achievement) {
        android.app.AlertDialog.Builder(this)
            .setTitle(achievement.title)
            .setMessage(
                "${achievement.description}\n\n" +
                        "Progress: ${achievement.progress}/${achievement.target}\n" +
                        "Status: ${if (achievement.unlocked) "Unlocked" else "Locked"}\n" +
                        if (achievement.unlocked && achievement.unlockedDate != null) {
                            "Unlocked on: ${SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(achievement.unlockedDate))}"
                        } else {
                            ""
                        }
            )
            .setPositiveButton("OK", null)
            .show()
    }
}
