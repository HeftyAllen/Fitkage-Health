package com.example.fitkagehealth

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class Download : BaseActivity() {

    private lateinit var txtUserInfo: TextView
    private lateinit var btnDownload: Button
    private lateinit var backBtn: ImageView
    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_download)

        txtUserInfo = findViewById(R.id.txtUserInfo)
        btnDownload = findViewById(R.id.btnDownload)
        backBtn = findViewById(R.id.backBtn)

        backBtn.setOnClickListener { finish() }

        auth = FirebaseAuth.getInstance()
        val userId = auth.currentUser?.uid

        if (userId == null) {
            txtUserInfo.text = "No user logged in."
            return
        }

        database = FirebaseDatabase.getInstance().getReference("users").child(userId)

        // Load user data
        loadUserData()

        // Request storage permission (Android 9 and below)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    100
                )
            }
        }

        btnDownload.setOnClickListener {
            generatePDF(txtUserInfo.text.toString())
        }
        backBtn.setOnClickListener {
            startActivity(Intent(this, Setting::class.java))
        }
    }

    private fun loadUserData() {
        database.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {

                    // Retrieve all user data
                    val name = snapshot.child("name").getValue(String::class.java) ?: "N/A"
                    val surname = snapshot.child("surname").getValue(String::class.java) ?: "N/A"
                    val email = snapshot.child("email").getValue(String::class.java) ?: "N/A"
                    val phone = snapshot.child("phone").getValue(String::class.java) ?: "N/A"
                    val stepsGoal = snapshot.child("stepsGoal").getValue(String::class.java) ?: "N/A"
                    val age = snapshot.child("age").getValue(String::class.java) ?: "N/A"
                    val gender = snapshot.child("gender").getValue(String::class.java) ?: "N/A"
                    val height = snapshot.child("height").getValue(String::class.java) ?: "N/A"
                    val weight = snapshot.child("weight").getValue(String::class.java) ?: "N/A"

                    val userInfo = """
                        PERSONAL INFORMATION
                        
                        Name: $name
                        Surname: $surname
                        Email: $email
                        Phone: $phone
                        Steps Goal: $stepsGoal
                        Age: $age
                        Gender: $gender
                        Height: $height
                        Weight: $weight
                    """.trimIndent()

                    txtUserInfo.text = userInfo
                } else {
                    txtUserInfo.text = "No data found."
                }
            }

            override fun onCancelled(error: DatabaseError) {
                txtUserInfo.text = "Failed to load data: ${error.message}"
            }
        })
    }

    private fun generatePDF(content: String) {
        if (content.isBlank() || content == "No data found.") {
            Toast.makeText(this, "No data to export.", Toast.LENGTH_SHORT).show()
            return
        }

        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val paint = Paint().apply {
            textSize = 16f
            color = ContextCompat.getColor(this@Download, android.R.color.black)
        }

        val lines = content.split("\n")
        var y = 60f
        for (line in lines) {
            canvas.drawText(line, 40f, y, paint)
            y += 25f
        }

        pdfDocument.finishPage(page)

        // Save to downloads directory
        val downloadsDir = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        } else {
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        }

        val file = File(downloadsDir, "PersonalData.pdf")

        try {
            pdfDocument.writeTo(FileOutputStream(file))
            Toast.makeText(this, "PDF saved: ${file.absolutePath}", Toast.LENGTH_LONG).show()
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        } finally {
            pdfDocument.close()
        }
    }
}