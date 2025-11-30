package com.example.fitkagehealth

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth

class forgot_password : BaseActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var edtEmailForgot: EditText
    private lateinit var btnResetPassword: Button
    private lateinit var tvBackToLogin: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_forgot_password)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // UI references
        edtEmailForgot = findViewById(R.id.edtEmailForgot)
        btnResetPassword = findViewById(R.id.btnResetPassword)
        tvBackToLogin = findViewById(R.id.tvBackToLogin)

        btnResetPassword.setOnClickListener {
            val email = edtEmailForgot.text.toString().trim()

            if (email.isEmpty()) {
                edtEmailForgot.error = "Email required"
                edtEmailForgot.requestFocus()
                return@setOnClickListener
            }

            auth.sendPasswordResetEmail(email)
                .addOnSuccessListener {
                    Toast.makeText(
                        this,
                        "Reset link sent to your email!",
                        Toast.LENGTH_LONG
                    ).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(
                        this,
                        "${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
        }

        // Optional: Back to login navigation
        tvBackToLogin.setOnClickListener {
            finish() // or navigate to login activity using Intent
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}