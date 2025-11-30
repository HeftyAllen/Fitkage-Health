package com.example.fitkagehealth

import android.os.Bundle
import android.os.CountDownTimer
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.TextView

class TimerActivity : BaseActivity() {

    private lateinit var tvTimer: TextView
    private lateinit var btnStartTimer: Button
    private var timer: CountDownTimer? = null
    private var isRunning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_timer)

        tvTimer = findViewById(R.id.tvTimer)
        btnStartTimer = findViewById(R.id.btnStartTimer)

        btnStartTimer.setOnClickListener {
            if (!isRunning) startTimer()
        }
    }

    private fun startTimer() {
        isRunning = true
        btnStartTimer.isEnabled = false

        timer = object : CountDownTimer(60_000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val seconds = (millisUntilFinished / 1000) % 60
                val minutes = (millisUntilFinished / 1000) / 60
                tvTimer.text = String.format("%02d:%02d", minutes, seconds)
            }

            override fun onFinish() {
                tvTimer.text = "Done!"
                btnStartTimer.isEnabled = true
                isRunning = false
            }
        }.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        timer?.cancel()
    }
}
