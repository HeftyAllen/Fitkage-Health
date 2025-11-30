package com.example.fitkagehealth.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import com.example.fitkagehealth.BaseActivity
import com.example.fitkagehealth.MainActivity
import com.example.fitkagehealth.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlin.math.roundToInt

class Personal_info : BaseActivity() {

    // Views (original IDs preserved)
    private lateinit var viewFlipper: ViewFlipper
    private lateinit var btnNextSave: Button
    private lateinit var btnBack: Button
    private lateinit var tvStep: TextView

    private lateinit var editTextName: EditText
    private lateinit var editTextSurname: EditText
    private lateinit var editTextEmail: EditText
    private lateinit var editTextPhone: EditText
    private lateinit var editTextSteps: EditText
    private lateinit var editTextAge: EditText
    private lateinit var editTextGender: EditText
    private lateinit var editTextHeight: EditText
    private lateinit var editTextWeight: EditText

    private lateinit var spnHeightUnit: Spinner
    private lateinit var spnWeightUnit: Spinner
    private lateinit var summaryText: TextView

    private var currentStep = 0 // 0..2
    private var heightUnit = "cm" // "cm" or "ft"
    private var weightUnit = "kg" // "kg" or "lb"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_personal_info)

        // bind views
        viewFlipper = findViewById(R.id.viewFlipper)
        btnNextSave = findViewById(R.id.idAdd)
        btnBack = findViewById(R.id.btnBack)
        tvStep = findViewById(R.id.tvStep)

        editTextName = findViewById(R.id.editTextName)
        editTextSurname = findViewById(R.id.editTextSurname)
        editTextEmail = findViewById(R.id.editTextEmail)
        editTextPhone = findViewById(R.id.editTextPhone)
        editTextSteps = findViewById(R.id.editTextSteps)
        editTextAge = findViewById(R.id.editTextAge)
        editTextGender = findViewById(R.id.editTextGender)
        editTextHeight = findViewById(R.id.editTextHeight)
        editTextWeight = findViewById(R.id.editTextWeight)

        spnHeightUnit = findViewById(R.id.spnHeightUnit)
        spnWeightUnit = findViewById(R.id.spnWeightUnit)
        summaryText = findViewById(R.id.summaryText)

        // autofill from FirebaseAuth if available
        val firebaseUser = FirebaseAuth.getInstance().currentUser
        firebaseUser?.let { user ->
            user.email?.let { editTextEmail.setText(it) }
            val displayName = user.displayName ?: user.email?.substringBefore("@")
            displayName?.let {
                val parts = it.split(" ")
                if (parts.isNotEmpty()) editTextName.setText(parts[0])
                if (parts.size > 1) editTextSurname.setText(
                    parts.subList(1, parts.size).joinToString(" ")
                )
            }
        }

        // set up spinners (dropdowns)
        val heightOptions = arrayOf("cm", "ft")
        val weightOptions = arrayOf("kg", "lb")

        spnHeightUnit.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, heightOptions)
        spnWeightUnit.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, weightOptions)

        // defaults
        spnHeightUnit.setSelection(0) // cm
        spnWeightUnit.setSelection(0) // kg

        spnHeightUnit.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val newUnit = heightOptions[position]
                if (newUnit != heightUnit) {
                    convertHeightTo(newUnit)
                    heightUnit = newUnit
                    updateHints()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        spnWeightUnit.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val newUnit = weightOptions[position]
                if (newUnit != weightUnit) {
                    convertWeightTo(newUnit)
                    weightUnit = newUnit
                    updateHints()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // button wiring
        btnNextSave.setOnClickListener {
            when (currentStep) {
                0 -> {
                    if (!validateStep1()) return@setOnClickListener
                    goToStep(1)
                }

                1 -> {
                    if (!validateStep2()) return@setOnClickListener
                    populateSummary()
                    goToStep(2)
                }

                2 -> {
                    saveData()
                }
            }
        }

        btnBack.setOnClickListener {
            when (currentStep) {
                1 -> goToStep(0)
                2 -> goToStep(1)
            }
        }

        // initialize UI
        goToStep(0)
    }

    private fun updateHints() {
        editTextHeight.hint = if (heightUnit == "cm") "Height (cm)" else "Height (ft, e.g. 5.9)"
        editTextWeight.hint = if (weightUnit == "kg") "Weight (kg)" else "Weight (lb)"
    }

    private fun convertHeightTo(newUnit: String) {
        val raw = editTextHeight.text.toString().trim().toDoubleOrNull() ?: return
        if (newUnit == "cm" && heightUnit == "ft") {
            // ft -> cm
            val cm = raw * 30.48
            editTextHeight.setText(String.format("%.0f", cm))
        } else if (newUnit == "ft" && heightUnit == "cm") {
            // cm -> ft
            val ft = raw / 30.48
            editTextHeight.setText(String.format("%.2f", ft))
        }
    }

    private fun convertWeightTo(newUnit: String) {
        val raw = editTextWeight.text.toString().trim().toDoubleOrNull() ?: return
        if (newUnit == "kg" && weightUnit == "lb") {
            val kg = raw * 0.45359237
            editTextWeight.setText(String.format("%.1f", kg))
        } else if (newUnit == "lb" && weightUnit == "kg") {
            val lb = raw / 0.45359237
            editTextWeight.setText(String.format("%.1f", lb))
        }
    }

    private fun goToStep(step: Int) {
        currentStep = step
        viewFlipper.displayedChild = step
        btnBack.visibility = if (step == 0) View.GONE else View.VISIBLE
        when (step) {
            0 -> {
                btnNextSave.text = "Next"
                tvStep.text = "Step 1 of 3 — Basic"
            }

            1 -> {
                btnNextSave.text = "Next"
                tvStep.text = "Step 2 of 3 — Goals"
            }

            2 -> {
                btnNextSave.text = "Save"
                tvStep.text = "Step 3 of 3 — Confirm"
            }
        }
        updateHints()
    }

    private fun validateStep1(): Boolean {
        val name = editTextName.text.toString().trim()
        val surname = editTextSurname.text.toString().trim()
        val email = editTextEmail.text.toString().trim()
        val phone = editTextPhone.text.toString().trim()
        if (name.isEmpty() || surname.isEmpty() || email.isEmpty() || phone.isEmpty()) {
            Toast.makeText(this, "Please fill name, surname, email and phone", Toast.LENGTH_SHORT)
                .show()
            return false
        }
        return true
    }

    private fun validateStep2(): Boolean {
        val steps = editTextSteps.text.toString().trim()
        val age = editTextAge.text.toString().trim()
        val height = editTextHeight.text.toString().trim()
        val weight = editTextWeight.text.toString().trim()
        if (steps.isEmpty() || age.isEmpty() || height.isEmpty() || weight.isEmpty()) {
            Toast.makeText(this, "Please fill steps, age, height and weight", Toast.LENGTH_SHORT)
                .show()
            return false
        }
        return true
    }

    private fun populateSummary() {
        val sb = StringBuilder()
        sb.append("Name: ${editTextName.text}\n")
        sb.append("Surname: ${editTextSurname.text}\n")
        sb.append("Email: ${editTextEmail.text}\n")
        sb.append("Phone: ${editTextPhone.text}\n\n")
        sb.append("Steps goal: ${editTextSteps.text}\n")
        sb.append("Age: ${editTextAge.text}\n")
        sb.append("Gender: ${editTextGender.text}\n")
        sb.append("Height: ${editTextHeight.text} ${heightUnit}\n")
        sb.append("Weight: ${editTextWeight.text} ${weightUnit}\n")
        summaryText.text = sb.toString()
    }

    private fun saveData() {
        // Save metric-normalized values to DB (store cm and kg)
        val name = editTextName.text.toString().trim()
        val surname = editTextSurname.text.toString().trim()
        val email = editTextEmail.text.toString().trim()
        val phone = editTextPhone.text.toString().trim()
        val stepsGoalStr = editTextSteps.text.toString().trim()
        val ageStr = editTextAge.text.toString().trim()
        val gender = editTextGender.text.toString().trim()

        val rawHeight = editTextHeight.text.toString().trim().toDoubleOrNull() ?: 0.0
        val rawWeight = editTextWeight.text.toString().trim().toDoubleOrNull() ?: 0.0

        val heightCm = if (heightUnit == "cm") rawHeight else rawHeight * 30.48
        val weightKg = if (weightUnit == "kg") rawWeight else rawWeight * 0.45359237

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        // Parse numeric values defensively
        val stepsGoalInt = stepsGoalStr.replace(",", "").toIntOrNull() ?: 0
        val ageInt = ageStr.toIntOrNull() ?: 0

        // Build user map: numbers stored as numbers, strings remain strings
        val userData = mapOf<String, Any>(
            "name" to name,
            "surname" to surname,
            "email" to email,
            "phone" to phone,
            "stepsGoal" to stepsGoalInt,          // Int (numeric)
            "age" to ageInt,                      // Int (numeric)
            "gender" to gender,
            "height_cm" to heightCm.roundToInt(), // Int (numeric)
            "weight_kg" to String.format(
                "%.1f",
                weightKg
            ), // keep as string if you prefer decimal formatting
            "height_unit_entered" to heightUnit,
            "weight_unit_entered" to weightUnit
        )

        val database = FirebaseDatabase.getInstance()
        val userRef = database.getReference("users").child(userId)

        // write user profile
        userRef.setValue(userData)
            .addOnSuccessListener {
                // Also write step goal to a dedicated node expected by MainActivity
                // store numeric under step_goals/<uid> = stepsGoalInt
                database.getReference("step_goals").child(userId).setValue(stepsGoalInt)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this, "Profile & goal saved", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(
                                this,
                                "Profile saved but failed to save goal",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    }
            }
            .addOnFailureListener { ex ->
                Toast.makeText(this, "Failed to save: ${ex.message}", Toast.LENGTH_LONG).show()
                ex.printStackTrace()
            }
    }
}