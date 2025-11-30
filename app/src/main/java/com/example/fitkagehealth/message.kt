package com.example.fitkagehealth

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class Message : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_message)
        findViewById<ImageView>(R.id.ibinst).setOnClickListener {
            val uri = Uri.parse("https://www.instagram.com")
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        }

// WhatsApp
        findViewById<ImageView>(R.id.ibwhatsapp).setOnClickListener {
            val uri = Uri.parse("https://wa.me/27614467227")
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        }

// LinkedIn
        findViewById<ImageView>(R.id.iblinklin).setOnClickListener {
            val uri = Uri.parse("https://www.linkedin.com")
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        }

// YouTube
        findViewById<ImageView>(R.id.youtube).setOnClickListener {
            val uri = Uri.parse("https://youtube.com")
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        }

// Facebook
        findViewById<ImageView>(R.id.facebook).setOnClickListener {
            val uri = Uri.parse("https://www.facebook.com")
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        }
    }

}