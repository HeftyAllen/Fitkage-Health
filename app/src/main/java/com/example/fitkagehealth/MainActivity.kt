package com.example.fitkagehealth

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.ui.platform.ComposeView
import com.example.fitkagehealth.component.GymComponentActivity
import com.example.fitkagehealth.databinding.ActivityMainBinding
import com.example.fitkagehealth.food.FoodDashboardActivity
import com.example.fitkagehealth.progress.ProgressTrackerActivity
import com.example.fitkagehealth.relaxation.MeditationActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MainActivity : BaseActivity(), SensorEventListener {

    private lateinit var binding: ActivityMainBinding

    // sensors
    private lateinit var sensorManager: SensorManager
    private var stepSensor: Sensor? = null
    private var totalSteps = 0
    private var previousTotalSteps = 0
    private var isBaselineSet = false

    // step / distance
    private val strideLength = 0.8
    private var startTime = 0L
    private var elapsedTime = 0L
    private var stepGoal = 0

    // water
    private var currentWater = 0f

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(this, "Notifications permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    private fun ensureNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val sharedPref = getSharedPreferences("ThemePrefs", MODE_PRIVATE)
        val isLightMode = sharedPref.getBoolean("light_mode", false)

        AppCompatDelegate.setDefaultNightMode(
            if (isLightMode) AppCompatDelegate.MODE_NIGHT_NO
            else AppCompatDelegate.MODE_NIGHT_YES
        )

        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ensureNotificationPermission()
        setupUI()
        setupSensors()

        // load persisted state
        loadWaterData()
        loadData()
        loadGoal()
    }

    private fun setupUI() {
        // toolbar actions
        binding.ibSettings.setOnClickListener {
            startActivity(Intent(this, Setting::class.java))
        }
        binding.ibNotifications.setOnClickListener {
            startActivity(Intent(this, MeditationActivity::class.java))
        }

        // activity quick tiles
        binding.walk.setOnClickListener { startActivity(Intent(this, WeatherActivity::class.java)) }
        binding.run.setOnClickListener { startActivity(Intent(this, WeatherActivity::class.java)) }
        binding.bike.setOnClickListener { startActivity(Intent(this, WeatherActivity::class.java)) }
        binding.hiking.setOnClickListener { startActivity(Intent(this, WeatherActivity::class.java)) }
        binding.meditation.setOnClickListener { startActivity(Intent(this, MeditationActivity::class.java)) }
        binding.weather.setOnClickListener { startActivity(Intent(this, WeatherActivity::class.java)) }
        // water intake button (update displayed text + optional custom view via reflection)
        binding.drinkButton.setOnClickListener {
            val waterIncrement = 250f
            currentWater += waterIncrement
            binding.waterText.text = "${currentWater.toInt()}ml"
            // if a custom waterView exists in layout, attempt to call setWaterLevel(f: Float)
            attemptSetWaterViewLevel(currentWater / 2000f)
            saveWaterData()
        }

        // food dashboard (fixed)
        binding.foodButton.setOnClickListener {
            startActivity(Intent(this, FoodDashboardActivity::class.java))
        }

        // chat bot (compose)
        binding.chatBotButton.setOnClickListener {
            binding.composeChatContainer.visibility = View.VISIBLE
            val composeView = ComposeView(this).apply {
                setContent {
                    FirebaseAiLogicChatScreen(
                        onClose = { binding.composeChatContainer.visibility = View.GONE }
                    )
                }
            }
            binding.composeChatContainer.removeAllViews()
            binding.composeChatContainer.addView(composeView)
        }

        // bottom navigation — use the menu ids present in your menu resource.
        val bottomNav: BottomNavigationView? = try {
            binding.bottomNavigation
        } catch (e: Exception) {
            findViewById(R.id.bottomNavigation)
        }

        bottomNav?.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    // already on home
                    true
                }

                R.id.nav_stats -> {
                    // replace with your Stats activity if you have one
                   startActivity(Intent(this, ProgressTrackerActivity::class.java))
                    true
                }
                R.id.nav_meditation -> {
                    startActivity(Intent(this, MeditationActivity::class.java))
                    true
                }

                R.id.nav_workouts, R.id.nav_workouts -> {
                    startActivity(Intent(this, GymComponentActivity::class.java))
                    true
                }

                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    true
                }

                else -> false
            }
        }

        // reset steps UX
        binding.txtSteps.setOnClickListener {
            Toast.makeText(this, "Long press to reset steps", Toast.LENGTH_SHORT).show()
        }
        binding.txtSteps.setOnLongClickListener {
            resetAllValues()
            saveData()
            true
        }
    }

    private fun setupSensors() {
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    }

    override fun onResume() {
        super.onResume()
        stepSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        } ?: run {
            Toast.makeText(this, "This device has no Step Counter Sensor", Toast.LENGTH_SHORT)
                .show()
        }
    }

    override fun onPause() {
        super.onPause()
        try {
            sensorManager.unregisterListener(this)
        } catch (_: Exception) {
        }
        saveData()
        saveWaterData()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return
        if (event.sensor.type != Sensor.TYPE_STEP_COUNTER) return

        val sensorValue = event.values.getOrNull(0)?.toInt() ?: return

        if (!isBaselineSet) {
            previousTotalSteps = sensorValue
            isBaselineSet = true
            startTime = SystemClock.elapsedRealtime()
            saveData()
        }

        if (sensorValue < previousTotalSteps) {
            // sensor reset
            previousTotalSteps = sensorValue
            isBaselineSet = true
            startTime = SystemClock.elapsedRealtime()
            saveData()
        }

        totalSteps = sensorValue
        val currentSteps = (totalSteps - previousTotalSteps).coerceAtLeast(0)
        val distance = currentSteps * strideLength

        if (startTime == 0L) startTime = SystemClock.elapsedRealtime()
        elapsedTime = SystemClock.elapsedRealtime() - startTime

        // update UI fields that exist in your layout
        binding.txtSteps.text = currentSteps.toString()
        binding.txtDistance.text = String.format("%.2f meters", distance)

        // optional: update txtTime if present in layout (guarded)
        val txtTimeId = resources.getIdentifier("txtTime", "id", packageName)
        if (txtTimeId != 0) {
            try {
                val v = findViewById<View?>(txtTimeId)
                val timeInSeconds = elapsedTime / 1000
                val minutes = timeInSeconds / 60
                val seconds = timeInSeconds % 60
                (v as? android.widget.TextView)?.text = String.format("%02d:%02d", minutes, seconds)
            } catch (_: Exception) {
            }
        }

        // optional: update progressBar if present
        val pbId = resources.getIdentifier("progressBar", "id", packageName)
        if (pbId != 0) {
            try {
                val pb = findViewById<android.widget.ProgressBar?>(pbId)
                if (pb != null) {
                    if (pb.max < currentSteps) pb.max = currentSteps
                    pb.progress = currentSteps
                }
            } catch (_: Exception) {
            }
        }

        if (currentSteps >= stepGoal && stepGoal > 0) {
            Toast.makeText(this, "You've reached your goal!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun resetAllValues() {
        if (totalSteps > 0) {
            previousTotalSteps = totalSteps
            isBaselineSet = true
        } else {
            totalSteps = 0
            previousTotalSteps = 0
            isBaselineSet = false
        }

        elapsedTime = 0
        startTime = SystemClock.elapsedRealtime()

        binding.txtSteps.text = "0"
        binding.txtDistance.text = "0.00 meters"

        // optional txtTime/progressBar reset if present
        val txtTimeId = resources.getIdentifier("txtTime", "id", packageName)
        if (txtTimeId != 0) (findViewById<View>(txtTimeId) as? android.widget.TextView)?.text =
            "00:00"
        val pbId = resources.getIdentifier("progressBar", "id", packageName)
        if (pbId != 0) (findViewById<View>(pbId) as? android.widget.ProgressBar)?.progress = 0

        val sharedPreferences = getSharedPreferences("myPref", Context.MODE_PRIVATE)
        sharedPreferences.edit()
            .putInt("baseline", previousTotalSteps)
            .putInt("key1", 0)
            .apply()
    }

    /** Save Water Intake */
    private fun saveWaterData() {
        val sharedPreferences = getSharedPreferences("waterPref", Context.MODE_PRIVATE)
        sharedPreferences.edit()
            .putFloat("currentWater", currentWater)
            .apply()
    }

    /** Load Water Intake */
    private fun loadWaterData() {
        val sharedPreferences = getSharedPreferences("waterPref", Context.MODE_PRIVATE)
        currentWater = sharedPreferences.getFloat("currentWater", 0f)
        binding.waterText.text = "${currentWater.toInt()}ml"
        attemptSetWaterViewLevel(currentWater / 2000f)
    }

    private fun attemptSetWaterViewLevel(level: Float) {
        // If you later add a custom view with id "waterView" and a method setWaterLevel(Float),
        // this will call it without crashing when the view isn't present.
        val waterViewId = resources.getIdentifier("waterView", "id", packageName)
        if (waterViewId == 0) return
        val wv = findViewById<View?>(waterViewId) ?: return
        try {
            val m =
                wv.javaClass.methods.firstOrNull { it.name == "setWaterLevel" && it.parameterTypes.size == 1 }
            m?.invoke(wv, level)
        } catch (_: Exception) { /* ignore reflection errors */
        }
    }

    private fun saveData() {
        val sharedPreferences = getSharedPreferences("myPref", Context.MODE_PRIVATE)
        sharedPreferences.edit()
            .putInt("baseline", previousTotalSteps)
            .putInt("last_total_steps", totalSteps)
            .putInt("key1", totalSteps)
            .apply()
    }

    private fun loadData() {
        val sharedPreferences = getSharedPreferences("myPref", Context.MODE_PRIVATE)
        previousTotalSteps = sharedPreferences.getInt("baseline", 0)
        val savedTotal = sharedPreferences.getInt("last_total_steps", 0)
        isBaselineSet = previousTotalSteps != 0
        totalSteps = savedTotal
        startTime = SystemClock.elapsedRealtime()
        val currentSteps = (totalSteps - previousTotalSteps).coerceAtLeast(0)
        binding.txtSteps.text = currentSteps.toString()
        val distance = currentSteps * strideLength
        binding.txtDistance.text = String.format("%.2f meters", distance)

        // optional: progressBar/text time initialization if present
        val pbId = resources.getIdentifier("progressBar", "id", packageName)
        if (pbId != 0) (findViewById<View>(pbId) as? android.widget.ProgressBar)?.progress =
            currentSteps
        val txtTimeId = resources.getIdentifier("txtTime", "id", packageName)
        if (txtTimeId != 0) (findViewById<View>(txtTimeId) as? android.widget.TextView)?.text =
            "00:00"
    }

    private fun loadGoal() {
        val firebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        val userId = firebaseUser?.uid ?: run {
            android.util.Log.d("MainActivity", "No logged in user; skipping loadGoal")
            stepGoal = 0
            return
        }

        val database = FirebaseDatabase.getInstance()
        val ref = database.getReference("step_goals").child(userId)

        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    android.util.Log.d(
                        "MainActivity",
                        "loadGoal snapshot.value = ${snapshot.value}"
                    )

                    // Prefer primitive numeric values
                    val raw = snapshot.value
                    val computedGoal = when (raw) {
                        null -> 0
                        is Number -> raw.toInt()
                        is String -> raw.trim().replace(",", "").toIntOrNull() ?: 0
                        is Map<*, *> -> {
                            // common keys to check
                            val candidateKeys =
                                listOf("goal", "stepsGoal", "value", "target", "stepGoal")
                            var found: Int? = null
                            for (k in candidateKeys) {
                                if (raw.containsKey(k)) {
                                    found = anyToInt(raw[k])
                                    if (found != null) break
                                }
                            }
                            if (found == null) {
                                // fallback to first numeric child value
                                val firstVal = raw.values.firstOrNull()
                                found = anyToInt(firstVal)
                            }
                            found ?: 0
                        }

                        else -> {
                            // final fallback: try snapshot children
                            if (snapshot.hasChildren()) {
                                val candidateKeys =
                                    listOf("goal", "stepsGoal", "value", "target", "stepGoal")
                                var found: Int? = null
                                for (k in candidateKeys) {
                                    if (snapshot.hasChild(k)) {
                                        found = anyToInt(snapshot.child(k).value)
                                        if (found != null) break
                                    }
                                }
                                if (found == null) {
                                    val firstChild = snapshot.children.firstOrNull()
                                    found = anyToInt(firstChild?.value)
                                }
                                found ?: 0
                            } else 0
                        }
                    }

                    stepGoal = computedGoal
                    if (stepGoal > 0) {
                        Toast.makeText(
                            this@MainActivity,
                            "Step goal: $stepGoal",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        android.util.Log.d("MainActivity", "No valid step goal found.")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("MainActivity", "Error parsing step goal snapshot", e)
                    stepGoal = 0
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@MainActivity, "Failed to load step goal", Toast.LENGTH_SHORT)
                    .show()
            }
        })
    }

    /** Utility: convert unknown any to Int if possible (reusable) */
    private fun anyToInt(value: Any?): Int? {
        try {
            return when (value) {
                null -> null
                is Int -> value
                is Long -> value.toInt()
                is Double -> value.toInt()
                is Float -> value.toInt()
                is Number -> value.toInt()
                is String -> {
                    val cleaned = value.trim().replace(",", "")
                    cleaned.toDoubleOrNull()?.toInt() ?: cleaned.toIntOrNull()
                }

                else -> null
            }
        } catch (_: Exception) {
            return null
        }
    }
}