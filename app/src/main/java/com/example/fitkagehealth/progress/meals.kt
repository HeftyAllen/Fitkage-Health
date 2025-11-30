package com.example.fitkagehealth.progress

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.fitkagehealth.BaseActivity
import com.example.fitkagehealth.MainActivity
import com.example.fitkagehealth.R
import com.example.fitkagehealth.food.Enter_meals

class meals : BaseActivity() {

    private lateinit var backBtn: ImageView
    private lateinit var breakfastBtn: Button
    private lateinit var lunchBtn: Button
    private lateinit var dinnerBtn: Button
    private lateinit var breakfastSnackBtn: Button
    private lateinit var lunchSnackBtn: Button
    private lateinit var dinnerSnackBtn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_meals)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize buttons
        backBtn = findViewById(R.id.backBtn)
        breakfastBtn = findViewById(R.id.buttonBreakfast)
        lunchBtn = findViewById(R.id.buttonLunch)
        dinnerBtn = findViewById(R.id.buttonDinner)
        breakfastSnackBtn = findViewById(R.id.buttonBreakfastSnack)
        lunchSnackBtn = findViewById(R.id.buttonLunchSnack)
        dinnerSnackBtn = findViewById(R.id.buttonDinnerSnack)

        // Go back to main screen
        backBtn.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        // Each button opens Enter_meals screen with its meal type
        breakfastBtn.setOnClickListener { openEnterMeals("Breakfast") }
        lunchBtn.setOnClickListener { openEnterMeals("Lunch") }
        dinnerBtn.setOnClickListener { openEnterMeals("Dinner") }
        breakfastSnackBtn.setOnClickListener { openEnterMeals("Breakfast Snack") }
        lunchSnackBtn.setOnClickListener { openEnterMeals("Lunch Snack") }
        dinnerSnackBtn.setOnClickListener { openEnterMeals("Dinner Snack") }
    }

    private fun openEnterMeals(mealType: String) {
        val intent = Intent(this, Enter_meals::class.java)
        intent.putExtra("mealType", mealType) // pass meal type to next screen
        startActivity(intent)
    }
}