package com.example.fitkagehealth

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate

open class BaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // load saved theme BEFORE super.onCreate or setContentView
        val sharedPref = getSharedPreferences("ThemePrefs", MODE_PRIVATE)
        val isLightMode = sharedPref.getBoolean("light_mode", false)

        AppCompatDelegate.setDefaultNightMode(
            if (isLightMode) AppCompatDelegate.MODE_NIGHT_NO
            else AppCompatDelegate.MODE_NIGHT_YES
        )

        super.onCreate(savedInstanceState)
    }
}