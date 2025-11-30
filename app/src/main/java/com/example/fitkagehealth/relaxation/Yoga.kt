package com.example.fitkagehealth.relaxation

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import com.example.fitkagehealth.BaseActivity
import com.example.fitkagehealth.R
import com.example.fitkagehealth.TimerActivity

class Yoga : BaseActivity(){
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_yoga)

        val startWorkoutBtn = findViewById<Button>(R.id.startWorkoutBtn)

        startWorkoutBtn.setOnClickListener {
            val intent = Intent(this, TimerActivity::class.java)
            startActivity(intent)
        }
    }
}