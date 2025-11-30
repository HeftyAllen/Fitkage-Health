package com.example.fitkagehealth.workouts

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fitkagehealth.BaseActivity
import com.example.fitkagehealth.AppDependencies
import com.example.fitkagehealth.ExerciseSettingsDialog
import com.example.fitkagehealth.R
import com.example.fitkagehealth.adapters.ExerciseAdapter
import com.example.fitkagehealth.databinding.ActivityWorkoutListBinding
import com.example.fitkagehealth.model.Exercise
import com.example.fitkagehealth.model.WorkoutPlan
import com.example.fitkagehealth.viewmodel.WorkoutViewModel
import com.example.fitkagehealth.viewmodel.WorkoutViewModelFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class WorkoutListActivity : BaseActivity() {

    private lateinit var binding: ActivityWorkoutListBinding
    private lateinit var viewModel: WorkoutViewModel
    private lateinit var exerciseAdapter: ExerciseAdapter

    private var hasTriggeredRefresh = false
    private var workoutNameFromIntent: String = "Workout"
    private var bodyPart: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWorkoutListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val repo = AppDependencies.provideWorkoutRepository(this)
        val factory = WorkoutViewModelFactory(repo)
        viewModel = ViewModelProvider(this, factory).get(WorkoutViewModel::class.java)

        bodyPart = intent.getStringExtra("bodyPart") ?: ""
        workoutNameFromIntent = intent.getStringExtra("workoutName") ?: "Workout"

        setupUI(workoutNameFromIntent)
        setupRecyclerView()
        observeExercises()
        observeViewModelErrors()
    }

    private fun setupUI(workoutName: String) {
        binding.toolbar.title = workoutName
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        binding.workoutTitle.text = workoutName
        binding.workoutTitle.setOnClickListener {
            retryExerciseLoad()
        }

        binding.startWorkoutBtn.isEnabled = false
        binding.startWorkoutBtn.setOnClickListener {
            val selectedExercises = viewModel.getSelectedExercises()
            if (selectedExercises.isNotEmpty()) {
                viewModel.createWorkoutFromCurrentExercises(workoutNameFromIntent) { workoutPlan ->
                    startWorkoutSession(workoutPlan)
                }
            } else {
                Toast.makeText(this, "Please select at least one exercise", Toast.LENGTH_SHORT).show()
            }
        }

        binding.progressBar.visibility = View.VISIBLE
        binding.workoutTitle.visibility = View.VISIBLE
        binding.exerciseCount.visibility = View.VISIBLE
    }

    private fun setupRecyclerView() {
        exerciseAdapter = ExerciseAdapter(
            onExerciseSelected = { exercise, isSelected ->
                viewModel.toggleExerciseSelection(exercise)
                updateStartButton()
            },
            onSettingsClick = { exercise ->
                showExerciseSettingsDialog(exercise)
            }
        )

        binding.exercisesRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@WorkoutListActivity)
            adapter = exerciseAdapter
            setHasFixedSize(true)
        }
    }

    private fun observeViewModelErrors() {
        // Handle errors if needed
    }

    private fun observeExercises() {
        Log.d(TAG, "Observing exercises for bodyPart='$bodyPart'")

        viewModel.getExercisesForWorkoutPlanner(bodyPart).observe(this) { exercises ->
            Log.d(TAG, "Room emitted ${exercises.size} exercises for '$bodyPart'")

            if (exercises.isEmpty() || exercises.size < 3) {
                if (!hasTriggeredRefresh) {
                    hasTriggeredRefresh = true
                    Log.d(TAG, "Few exercises found -> triggering API refresh")
                    binding.progressBar.visibility = View.VISIBLE
                    binding.exerciseCount.text = "Loading exercises..."
                    viewModel.refreshExercises()

                    lifecycleScope.launch {
                        delay(3000)
                        // Check again after refresh
                        if (exerciseAdapter.currentList.size < 3) {
                            Log.d(TAG, "Still few exercises after refresh - performing enhanced search")
                            performEnhancedSearch()
                        }
                    }
                } else {
                    performEnhancedSearch()
                }
            } else {
                exerciseAdapter.submitList(exercises)
                binding.progressBar.visibility = View.GONE
                binding.workoutTitle.text = workoutNameFromIntent
                binding.exerciseCount.text = getString(R.string.exercise_count, exercises.size)
                Log.d(TAG, "Successfully loaded ${exercises.size} exercises")
            }
        }
    }

    private fun performEnhancedSearch() {
        Log.d(TAG, "Performing enhanced search for '$bodyPart'")
        binding.exerciseCount.text = "Searching for exercises..."

        viewModel.searchExercises(bodyPart)
        lifecycleScope.launch {
            viewModel.searchResults.collect { results ->
                if (results.isNotEmpty()) {
                    Log.d(TAG, "Enhanced search found ${results.size} exercises")
                    exerciseAdapter.submitList(results.take(12))
                    binding.progressBar.visibility = View.GONE
                    binding.exerciseCount.text = getString(R.string.exercise_count, results.size)
                    binding.workoutTitle.text = "$workoutNameFromIntent (${results.size} found)"
                } else {
                    // Show basic exercises as fallback
                    showBasicExercisesFallback()
                }
            }
        }
    }

    private fun showBasicExercisesFallback() {
        Log.d(TAG, "Showing basic exercises fallback for $bodyPart")

        val basicExercises = when (bodyPart.lowercase()) {
            "chest" -> listOf(
                "Push Up", "Bench Press", "Incline Bench Press", "Decline Bench Press",
                "Dumbbell Press", "Dumbbell Fly", "Chest Fly", "Cable Fly",
                "Pec Deck", "Chest Dips", "Incline Dumbbell Fly", "Svend Press", "Single-Arm Chest Press"
            )
            "arms" -> listOf(
                "Bicep Curl", "Hammer Curl", "Concentration Curl", "Preacher Curl",
                "Cable Curl", "Reverse Curl", "Zottman Curl", "Tricep Extension",
                "Skull Crusher", "Tricep Dip", "Tricep Pushdown", "Overhead Tricep Extension", "Close-Grip Bench Press"
            )
            "back" -> listOf(
                "Pull Up", "Chin Up", "Bent Over Row", "Seated Cable Row",
                "Lat Pulldown", "Single-Arm Dumbbell Row", "Inverted Row", "T-Bar Row",
                "Deadlift", "Hyperextension", "Face Pull", "Wide Grip Pulldown", "Meadows Row"
            )
            "legs" -> listOf(
                "Squat", "Front Squat", "Goblet Squat", "Lunge",
                "Walking Lunge", "Bulgarian Split Squat", "Leg Press", "Romanian Deadlift",
                "Deadlift", "Calf Raise", "Leg Extension", "Hamstring Curl", "Step Up"
            )
            "shoulders" -> listOf(
                "Shoulder Press", "Dumbbell Shoulder Press", "Arnold Press", "Lateral Raise",
                "Front Raise", "Rear Delt Fly", "Upright Row", "Military Press",
                "Dumbbell Shrug", "Face Pull", "Cable Lateral Raise", "Cuban Press"
            )
            "abs" -> listOf(
                "Crunches", "Leg Raises", "Plank", "Russian Twist",
                "Bicycle Crunch", "Hanging Knee Raise", "Mountain Climbers", "V-Up",
                "Toe Touches", "Flutter Kicks", "Dead Bug", "Side Plank"
            )
            "stretching", "stretch" -> listOf(
                "Hamstring Stretch", "Quad Stretch", "Calf Stretch", "Hip Flexor Stretch",
                "Child's Pose", "Seated Forward Fold", "Butterfly Stretch", "Figure Four Stretch",
                "Shoulder Stretch", "Triceps Stretch", "Chest Opener", "Cat-Cow", "Neck Stretch", "Standing Forward Bend"
            )
            "hiit" -> listOf(
                "Burpees", "Mountain Climbers", "High Knees", "Jump Squats",
                "Jump Lunges", "Skaters", "Tuck Jumps", "Sprint Intervals",
                "Jumping Jacks", "Plank Jacks", "Box Jumps", "Battle Ropes", "Push-Up Burpees"
            )
            "yoga" -> listOf(
                "Downward Dog", "Cobra Pose", "Warrior II", "Tree Pose",
                "Bridge Pose", "Child's Pose", "Cat-Cow", "Triangle Pose",
                "Seated Forward Fold", "Pigeon Pose", "Chair Pose", "Plank Pose"
            )
            "pilates" -> listOf(
                "Pilates Hundred", "Roll Up", "Leg Circles", "Single-Leg Stretch",
                "Double-Leg Stretch", "Teaser", "Spine Stretch", "Swan",
                "Side Kick", "Shoulder Bridge", "Saw", "Scissor"
            )
            "thighs" -> listOf(
                "Barbell Squat", "Bulgarian Split Squat", "Leg Press", "Front Squat",
                "Sumo Squat", "Walking Lunge", "Step Up", "Inner Thigh Adduction",
                "Curtsy Lunge", "Hack Squat", "Thigh Abduction", "Thigh Extension"
            )
            "calves" -> listOf(
                "Standing Calf Raise", "Seated Calf Raise", "Donkey Calf Raise", "Jump Rope",
                "Box Jumps", "Single-Leg Calf Raise", "Farmer's Walk on Toes", "Calf Press on Leg Press",
                "Tibialis Raise", "Heel Drop", "Calf Hop", "Sprinting Drills"
            )
            "glutes" -> listOf(
                "Glute Bridge", "Hip Thrust", "Bulgarian Split Squat", "Romanian Deadlift",
                "Step Up", "Cable Kickback", "Donkey Kick", "Fire Hydrant",
                "Sumo Deadlift", "Single-Leg Deadlift", "Glute Kickback", "Clamshell"
            )
            "obliques" -> listOf(
                "Side Plank", "Russian Twist", "Bicycle Crunch", "Wood Chop",
                "Side Bend", "Heel Touches", "Windshield Wipers", "Standing Oblique Crunch",
                "Side Plank Hip Lift", "Cable Woodchop", "Oblique V-Up", "Seated Side Twist"
            )
            "cardio" -> listOf(
                "Running", "Cycling", "Jump Rope", "Rowing",
                "Elliptical", "Stair Climber", "Swimming", "High-Intensity Intervals",
                "Aerobic Step", "Boxing", "Battle Ropes", "Dance Cardio"
            )
            "full body" -> listOf(
                "Burpee", "Thruster", "Kettlebell Swing", "Deadlift",
                "Clean and Press", "Man Makers", "Push-Up to Row", "Squat to Press",
                "Mountain Climbers", "Jumping Jacks", "Turkish Get-Up", "Renegade Row"
            )
            "balance" -> listOf(
                "Single-Leg Deadlift", "Single-Leg Squat", "Bosu Ball Squat", "Heel-to-Toe Walk",
                "Standing Knee Raise", "Lateral Reach", "Clock Reach", "Single-Leg Balance Reach",
                "Stability Ball Balance", "Tandem Stand", "Single-Leg Hop", "Single-Leg Calf Raise"
            )
            "neck" -> listOf(
                "Neck Flexion", "Neck Extension", "Lateral Neck Flexion", "Neck Rotation",
                "Shoulder Shrug", "Chin Tuck", "Isometric Neck Hold", "Scapular Retraction",
                "Upper Trap Stretch", "Levator Scapulae Stretch", "Neck Circles", "Prone Head Lift"
            )
            "endurance" -> listOf(
                "Long Run", "Tempo Run", "Interval Training", "Cycling Long Ride",
                "Rowing Endurance", "Stair Climb", "Circuit Training", "Swim Laps",
                "Trail Running", "Rucking", "Aerobic Step", "Cross-Country Skiing"
            )
            else -> listOf(
                "Push Up", "Squat", "Plank", "Pull Up", "Lunge", "Deadlift",
                "Burpee", "Mountain Climbers", "Jumping Jacks", "Glute Bridge",
                "Bicep Curl", "Tricep Dip"
            )
        }

        val fallbackExercises = basicExercises.map { name ->
            Exercise(
                id = "fallback_${name.replace(" ", "").lowercase()}",
                name = name,
                bodyPart = bodyPart,
                equipment = "body weight",
                target = bodyPart,
                instructions = listOf("Perform $name with proper form")
            )
        }

        exerciseAdapter.submitList(fallbackExercises)
        binding.progressBar.visibility = View.GONE
        binding.exerciseCount.text = "Basic ${bodyPart} exercises"
        binding.workoutTitle.text = "$workoutNameFromIntent (Basic)"

        Toast.makeText(
            this,
            "Using basic exercises. For full library, check your internet connection.",
            Toast.LENGTH_LONG
        ).show()
    }


    private fun retryExerciseLoad() {
        binding.progressBar.visibility = View.VISIBLE
        binding.exerciseCount.text = "Reloading exercises..."
        hasTriggeredRefresh = false
        viewModel.refreshExercises()
        observeExercises()
        Toast.makeText(this, "Reloading exercises...", Toast.LENGTH_SHORT).show()
    }

    @SuppressLint("StringFormatInvalid")
    private fun updateStartButton() {
        val selectedCount = viewModel.getSelectedExercises().size
        binding.startWorkoutBtn.isEnabled = selectedCount > 0
        binding.startWorkoutBtn.text = if (selectedCount > 0) {
            getString(R.string.start_workout_with_count, selectedCount)
        } else {
            getString(R.string.start_workout)
        }
    }

    private fun showExerciseSettingsDialog(exercise: Exercise) {
        val dialog = ExerciseSettingsDialog.newInstance(exercise) { updatedExercisePlan ->
            viewModel.updateExerciseSettings(updatedExercisePlan)
        }
        dialog.show(supportFragmentManager, "ExerciseSettingsDialog")
    }

    private fun startWorkoutSession(workoutPlan: WorkoutPlan) {
        val intent = Intent(this, WorkoutSessionActivity::class.java).apply {
            putExtra("workoutPlan", workoutPlan)
        }
        startActivity(intent)
    }

    companion object {
        private const val TAG = "WorkoutList"
    }
}