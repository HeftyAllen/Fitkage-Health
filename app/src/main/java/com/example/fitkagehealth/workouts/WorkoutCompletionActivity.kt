package com.example.fitkagehealth.workouts

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fitkagehealth.BaseActivity
import com.example.fitkagehealth.AppDependencies
import com.example.fitkagehealth.adapters.WorkoutSummaryAdapter
import com.example.fitkagehealth.databinding.ActivityWorkoutCompletionBinding
import com.example.fitkagehealth.model.WorkoutSession
import com.example.fitkagehealth.viewmodel.WorkoutCompletionViewModel
import com.example.fitkagehealth.viewmodel.WorkoutCompletionViewModelFactory

class WorkoutCompletionActivity : BaseActivity() {

    private lateinit var binding: ActivityWorkoutCompletionBinding
    private lateinit var viewModel: WorkoutCompletionViewModel
    private lateinit var session: WorkoutSession

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWorkoutCompletionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val repo = AppDependencies.provideWorkoutRepository(this)
        val factory = WorkoutCompletionViewModelFactory(repo)
        viewModel = ViewModelProvider(this, factory).get(WorkoutCompletionViewModel::class.java)

        session = intent.getParcelableExtra("workoutSession")!!
        setupUI()
        setupRecyclerView()
        populateData()
    }

    private fun setupUI() {
        binding.toolbar.setNavigationOnClickListener { onBackPressed() }

        binding.ratingBar.setOnRatingBarChangeListener { _, rating, _ ->
            session = session.copy(rating = rating.toInt())
        }

        binding.notesEditText.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                session = session.copy(notes = binding.notesEditText.text.toString())
            }
        }

        binding.saveSessionBtn.setOnClickListener {
            saveWorkoutSession()
        }

        binding.shareWorkoutBtn.setOnClickListener {
            shareWorkoutResults()
        }
    }

    private fun setupRecyclerView() {
        val workoutAdapter = WorkoutSummaryAdapter(session.workoutPlan.exercises)
        binding.exercisesRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@WorkoutCompletionActivity)
            adapter = workoutAdapter
        }
    }

    private fun populateData() {
        binding.workoutNameText.text = session.workoutPlan.name
        binding.durationText.text = formatDuration(session.totalDuration)
        binding.caloriesText.text = "${session.caloriesBurned} cal"
        binding.completedExercisesText.text = "${session.completedExercises}/${session.workoutPlan.exercises.size}"

        session.rating?.let { binding.ratingBar.rating = it.toFloat() }
        binding.notesEditText.setText(session.notes)
    }

    private fun formatDuration(millis: Long): String {
        val minutes = millis / 60000
        val seconds = (millis % 60000) / 1000
        return String.format("%02d:%02d", minutes, seconds)
    }

    private fun saveWorkoutSession() {
        viewModel.saveWorkoutSession(session) { success ->
            if (success) {
                setResult(RESULT_OK)
                finish()
            }
        }
    }

    private fun shareWorkoutResults() {
        val shareText = buildShareText()
        val intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, shareText)
            type = "text/plain"
        }
        startActivity(Intent.createChooser(intent, "Share Workout Results"))
    }

    private fun buildShareText(): String {
        return """
            🏋️ Workout Completed! 🎉
            
            ${session.workoutPlan.name}
            Duration: ${formatDuration(session.totalDuration)}
            Exercises: ${session.completedExercises}/${session.workoutPlan.exercises.size}
            Calories: ${session.caloriesBurned}
            Rating: ${session.rating}/5
            
            #FitKage #Workout #Fitness
        """.trimIndent()
    }
}
