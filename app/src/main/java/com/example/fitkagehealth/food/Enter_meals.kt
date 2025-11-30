package com.example.fitkagehealth.food

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.fitkagehealth.BaseActivity
import com.example.fitkagehealth.MainActivity
import com.example.fitkagehealth.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class Enter_meals : BaseActivity() {

    private lateinit var inputMeals: EditText
    private lateinit var inputCalories: EditText
    private lateinit var btnSubmit: Button
    private lateinit var backBtn: ImageView
    private lateinit var titleText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_enter_meals)

        // Handle window insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize Firebase auth
        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        val uid = currentUser.uid

        // Initialize views
        inputMeals = findViewById(R.id.inputMeals)
        inputCalories = findViewById(R.id.inputCalories)
        btnSubmit = findViewById(R.id.btnSubmit)
        backBtn = findViewById(R.id.backBtn)
        titleText = findViewById(R.id.titleText)

        titleText.text = "Enter Meals"

        // Firebase reference: /Meals/<UID>/
        val databaseRef = FirebaseDatabase.getInstance("https://fitkagehealth-default-rtdb.firebaseio.com/")
            .getReference("Meals")
            .child(uid)

        btnSubmit.setOnClickListener {
            val mealName = inputMeals.text.toString().trim()
            val calories = inputCalories.text.toString().trim()

            // Validation
            if (mealName.isEmpty() || calories.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Only saving inputMeals and inputCalories
            val mealData = mapOf(
                "mealName" to mealName,
                "calories" to calories
            )

            databaseRef.push().setValue(mealData)
                .addOnSuccessListener {
                    Toast.makeText(this, "Meal saved!", Toast.LENGTH_SHORT).show()
                    inputMeals.text.clear()
                    inputCalories.text.clear()
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(this, "Failed to save meal: ${exception.message}", Toast.LENGTH_SHORT).show()
                    exception.printStackTrace()
                }
        }

        backBtn.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }
    }
}