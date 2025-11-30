package com.example.fitkagehealth.component

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fitkagehealth.BaseActivity
import com.example.fitkagehealth.AppDependencies
import com.example.fitkagehealth.adapters.CustomWorkoutsAdapter
import com.example.fitkagehealth.databinding.ActivityGymComponentBinding
import com.example.fitkagehealth.workouts.CreateWorkoutActivity
import com.example.fitkagehealth.workouts.WorkoutListActivity
import com.example.fitkagehealth.workouts.WorkoutSessionActivity
import com.example.fitkagehealth.model.WorkoutPlan
import com.example.fitkagehealth.viewmodel.WorkoutViewModel
import com.example.fitkagehealth.viewmodel.WorkoutViewModelFactory
import kotlinx.coroutines.launch

class GymComponentActivity : BaseActivity() {

    private lateinit var binding: ActivityGymComponentBinding
    private lateinit var viewModel: WorkoutViewModel
    private lateinit var customWorkoutsAdapter: CustomWorkoutsAdapter

    private val bodyPartMap = mapOf(
        "chest" to "Chest Strength",
        "arms" to "Arm Builder",
        "shoulders" to "Shoulder Shaper",
        "back" to "Back Power",
        "legs" to "Leg Day",
        "thighs" to "Thighs",
        "calves" to "Calves",
        "glutes" to "Glute Gains",
        "abs" to "Core Crusher",
        "obliques" to "Oblique Burn",
        "cardio" to "Cardio Burn",
        "hiit" to "HIIT Extreme",
        "full body" to "Full Body Blast",
        "yoga" to "Yoga Flow",
        "pilates" to "Pilates Core",
        "stretching" to "Stretch and Recover",
        "balance" to "Balance and Stability",
        "neck" to "Neck and Traps",
        "endurance" to "Endurance and Flexibility"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGymComponentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val repo = AppDependencies.provideWorkoutRepository(this)
        val factory = WorkoutViewModelFactory(repo)
        viewModel = ViewModelProvider(this, factory).get(WorkoutViewModel::class.java)

        setupClickListeners()
        setupCustomWorkoutsRecyclerView()
        observeWorkoutPlans()
        loadCustomWorkouts()
    }

    private fun setupClickListeners() {
        binding.chestCard.setOnClickListener { navigateToWorkoutList("chest") }
        binding.armsCard.setOnClickListener { navigateToWorkoutList("arms") }
        binding.shouldersCard.setOnClickListener { navigateToWorkoutList("shoulders") }
        binding.backCard.setOnClickListener { navigateToWorkoutList("back") }
        binding.legsCard.setOnClickListener { navigateToWorkoutList("legs") }
        binding.thighsCard.setOnClickListener { navigateToWorkoutList("thighs") }
        binding.calvesCard.setOnClickListener { navigateToWorkoutList("calves") }
        binding.glutesCard.setOnClickListener { navigateToWorkoutList("glutes") }
        binding.absCard.setOnClickListener { navigateToWorkoutList("abs") }
        binding.obliquesCard.setOnClickListener { navigateToWorkoutList("obliques") }
        binding.cardioCard.setOnClickListener { navigateToWorkoutList("cardio") }
        binding.hiitCard.setOnClickListener { navigateToWorkoutList("hiit") }
        binding.fullBodyCard.setOnClickListener { navigateToWorkoutList("full body") }
        binding.yogaCard.setOnClickListener { navigateToWorkoutList("yoga") }
        binding.pilatesCard.setOnClickListener { navigateToWorkoutList("pilates") }
        binding.stretchCard.setOnClickListener { navigateToWorkoutList("stretching") }
        binding.balanceCard.setOnClickListener { navigateToWorkoutList("balance") }
        binding.neckTrapsCard.setOnClickListener { navigateToWorkoutList("neck") }
        binding.enduranceFlexCard.setOnClickListener { navigateToWorkoutList("endurance") }

        binding.customWorkoutCard.setOnClickListener {
            startActivity(Intent(this, CreateWorkoutActivity::class.java))
        }
    }

    private fun setupCustomWorkoutsRecyclerView() {
        customWorkoutsAdapter = CustomWorkoutsAdapter(
            onWorkoutClick = { workoutPlan ->
                startWorkoutSession(workoutPlan)
            },
            onWorkoutDelete = { workoutPlan ->
                deleteCustomWorkout(workoutPlan)
            }
        )

        binding.customWorkoutsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@GymComponentActivity)
            adapter = customWorkoutsAdapter
        }
    }

    private fun observeWorkoutPlans() {
        lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                viewModel.customWorkoutPlans.collect { customPlans ->
                    customWorkoutsAdapter.submitList(customPlans)
                    binding.customWorkoutsLabel.visibility =
                        if (customPlans.isEmpty()) android.view.View.GONE else android.view.View.VISIBLE
                    binding.customWorkoutsRecyclerView.visibility =
                        if (customPlans.isEmpty()) android.view.View.GONE else android.view.View.VISIBLE
                }
            }
        }
    }

    private fun loadCustomWorkouts() {
        viewModel.loadCustomWorkoutPlans()
    }

    private fun navigateToWorkoutList(bodyPart: String) {
        val intent = Intent(this, WorkoutListActivity::class.java).apply {
            putExtra("bodyPart", bodyPart)
            putExtra("workoutName", bodyPartMap[bodyPart] ?: "Workout")
        }
        startActivity(intent)
    }

    private fun startWorkoutSession(workoutPlan: WorkoutPlan) {
        val intent = Intent(this, WorkoutSessionActivity::class.java).apply {
            putExtra("workoutPlan", workoutPlan)
        }
        startActivity(intent)
    }

    private fun deleteCustomWorkout(workoutPlan: WorkoutPlan) {
        viewModel.deleteWorkoutPlan(workoutPlan) { success ->
            if (success) {
                android.widget.Toast.makeText(this, "Workout deleted", android.widget.Toast.LENGTH_SHORT).show()
            } else {
                android.widget.Toast.makeText(this, "Failed to delete workout", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }
}