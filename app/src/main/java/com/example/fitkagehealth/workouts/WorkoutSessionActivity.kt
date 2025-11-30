package com.example.fitkagehealth.workouts

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.addCallback
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.fitkagehealth.BaseActivity
import com.example.fitkagehealth.AppDependencies
import com.example.fitkagehealth.FinishWorkoutDialog
import com.example.fitkagehealth.databinding.ActivityWorkoutSessionBinding
import com.example.fitkagehealth.model.ExercisePlan
import com.example.fitkagehealth.model.WorkoutPlan
import com.example.fitkagehealth.viewmodel.WorkoutSessionViewModel
import com.example.fitkagehealth.viewmodel.WorkoutSessionViewModelFactory
import kotlinx.coroutines.launch

class WorkoutSessionActivity : BaseActivity() {

    private lateinit var binding: ActivityWorkoutSessionBinding
    private lateinit var viewModel: WorkoutSessionViewModel
    private lateinit var workoutPlan: WorkoutPlan

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWorkoutSessionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val wp = intent.getParcelableExtra<WorkoutPlan>("workoutPlan")
        if (wp == null) {
            finish()
            return
        }
        workoutPlan = wp

        val repo = AppDependencies.provideWorkoutRepository(this)
        val factory = WorkoutSessionViewModelFactory(repo)
        viewModel = ViewModelProvider(this, factory).get(WorkoutSessionViewModel::class.java)

        onBackPressedDispatcher.addCallback(this) {
            showFinishWorkoutDialog()
        }

        setupUI()
        setupObservers()
        startWorkout()
    }

    private fun setupUI() {
        binding.pauseBtn.setOnClickListener {
            viewModel.togglePause()
        }

        binding.completeSetBtn.setOnClickListener {
            viewModel.completeSet()
        }

        binding.skipRestBtn.setOnClickListener {
            viewModel.skipRest()
        }

        binding.finishWorkoutBtn.setOnClickListener {
            showFinishWorkoutDialog()
        }

        binding.addRestTimeBtn.setOnClickListener {
            viewModel.addRestTime(15)
        }
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            viewModel.currentExercise.collect { exercisePlan ->
                exercisePlan?.let { updateExerciseUI(it) }
            }
        }

        lifecycleScope.launch {
            viewModel.nextExercise.collect { next ->
                next?.let { updateNextExercisePreview(it) }
            }
        }

        lifecycleScope.launch {
            viewModel.workoutState.collect { state ->
                updateUIForState(state)
            }
        }

        lifecycleScope.launch {
            viewModel.timerValue.collect { seconds ->
                updateTimerUI(seconds)
            }
        }

        lifecycleScope.launch {
            viewModel.workoutDuration.collect { duration ->
                updateWorkoutDuration(duration)
            }
        }

        lifecycleScope.launch {
            viewModel.currentSet.collect { set ->
                val setsCount = getCurrentExerciseSets()
                binding.currentSetText.text = "Set $set of $setsCount"
            }
        }

        // NEW: Observe completed exercises count
        lifecycleScope.launch {
            viewModel.completedExercises.collect { completed ->
                updateProgressUI(completed)
            }
        }
    }

    private fun startWorkout() {
        viewModel.startWorkout(workoutPlan)
    }

    private fun updateExerciseUI(exercisePlan: ExercisePlan) {
        binding.exerciseName.text = exercisePlan.exercise.name
        binding.setsText.text = "Sets: ${exercisePlan.sets}"
        binding.repsText.text = "Reps: ${exercisePlan.reps}"
        binding.restTimeText.text = "Rest: ${exercisePlan.restTime}s"

        Glide.with(this)
            .load(exercisePlan.exercise.gifUrl)
            .into(binding.exerciseGif)

        // Use the observed completed exercises count
        val completed = viewModel.completedExercises.value
        val total = viewModel.getTotalExercises()
        binding.progressText.text = "$completed/$total exercises completed"
    }

    private fun updateProgressUI(completed: Int) {
        val total = viewModel.getTotalExercises()
        binding.progressText.text = "$completed/$total exercises completed"

        // Update progress bar if you have one
        // binding.progressBar.max = total
        // binding.progressBar.progress = completed
    }

    private fun updateNextExercisePreview(nextExercise: ExercisePlan) {
        binding.nextExerciseName.text = "Next: ${nextExercise.exercise.name}"
        Glide.with(this)
            .load(nextExercise.exercise.gifUrl)
            .into(binding.nextExerciseGif)
    }

    private fun updateUIForState(state: com.example.fitkagehealth.manager.WorkoutSessionManager.WorkoutState) {
        when (state) {
            com.example.fitkagehealth.manager.WorkoutSessionManager.WorkoutState.ACTIVE -> {
                binding.statusText.text = "ACTIVE"
                binding.completeSetBtn.isEnabled = true
                binding.skipRestBtn.isEnabled = false
                binding.restTimerCard.visibility = android.view.View.GONE
                binding.pauseBtn.text = "Pause"
            }

            com.example.fitkagehealth.manager.WorkoutSessionManager.WorkoutState.RESTING -> {
                binding.statusText.text = "RESTING"
                binding.completeSetBtn.isEnabled = false
                binding.skipRestBtn.isEnabled = true
                binding.restTimerCard.visibility = android.view.View.VISIBLE
            }

            com.example.fitkagehealth.manager.WorkoutSessionManager.WorkoutState.PAUSED -> {
                binding.statusText.text = "PAUSED"
                binding.pauseBtn.text = "Resume"
            }

            com.example.fitkagehealth.manager.WorkoutSessionManager.WorkoutState.COMPLETED -> {
                navigateToCompletion()
            }

            com.example.fitkagehealth.manager.WorkoutSessionManager.WorkoutState.IDLE -> {
                binding.statusText.text = "READY"
                binding.completeSetBtn.isEnabled = false
                binding.skipRestBtn.isEnabled = false
                binding.restTimerCard.visibility = android.view.View.GONE
            }
        }
    }

    private fun updateTimerUI(seconds: Int) {
        binding.restTimerText.text = "$seconds s"
    }

    private fun updateWorkoutDuration(duration: Long) {
        val minutes = duration / 60000
        val seconds = (duration % 60000) / 1000
        binding.workoutTimerText.text = String.format("%02d:%02d", minutes, seconds)
    }

    private fun showFinishWorkoutDialog() {
        val dialog = FinishWorkoutDialog(this) {
            viewModel.finishWorkout()
            navigateToCompletion()
        }
        dialog.show()
    }

    private fun navigateToCompletion() {
        val intent = Intent(this, WorkoutCompletionActivity::class.java)
        val session = viewModel.getWorkoutSession()
        if (session is android.os.Parcelable) {
            intent.putExtra("workoutSession", session)
        }
        startActivity(intent)
        finish()
    }

    private fun getCurrentExerciseSets(): Int {
        return viewModel.currentExercise.value?.sets ?: 0
    }
}