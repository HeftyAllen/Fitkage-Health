package com.example.fitkagehealth

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.fitkagehealth.auth.Login
import com.example.fitkagehealth.component.GymComponentActivity
import com.example.fitkagehealth.auth.Personal_info
import com.example.fitkagehealth.relaxation.MeditationActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.io.ByteArrayOutputStream
import java.io.InputStream

class Setting : AppCompatActivity() {

    private lateinit var profileImageView: ImageView
    private lateinit var nameTextView: TextView
    private lateinit var numberTextView: TextView
    private lateinit var btnProfile: Button
    private lateinit var backBtn: ImageView
    private lateinit var ibHome: ImageButton
    private lateinit var ibGym: ImageButton
    private lateinit var ibMeditate: ImageButton
    private lateinit var ibProfile: ImageButton

    private val PICK_IMAGE_REQUEST = 1001
    private var imageUri: Uri? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(this, "Notifications enabled", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    private fun ensureNotificationPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_profile_page)

        // Handle window insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize views
        profileImageView = findViewById(R.id.profileid)
        nameTextView = findViewById(R.id.nameid)
        numberTextView = findViewById(R.id.number)
        btnProfile = findViewById(R.id.profilebtn)
        backBtn = findViewById(R.id.backBtn)
        ibProfile = findViewById(R.id.ibProfile)
        ibHome = findViewById(R.id.ibHome)
        ibGym = findViewById(R.id.ibGym)
        ibMeditate = findViewById(R.id.ibMeditate)

        val faqs = findViewById<RelativeLayout>(R.id.faqs)
        val about = findViewById<RelativeLayout>(R.id.abouts)
        val download = findViewById<RelativeLayout>(R.id.downs)
        val message = findViewById<RelativeLayout>(R.id.messagesss)
        val language = findViewById<RelativeLayout>(R.id.Language)
        val logout = findViewById<RelativeLayout>(R.id.loginin)

        faqs.setOnClickListener {
            startActivity(Intent(this, FAQsScreen::class.java))
        }

        about.setOnClickListener {
            startActivity(Intent(this, AboutUs::class.java))
        }

        download.setOnClickListener {
            startActivity(Intent(this, Download::class.java))
        }

        message.setOnClickListener {
            startActivity(Intent(this, Message::class.java))
        }

        language.setOnClickListener {
            startActivity(Intent(this, Languages::class.java))
        }

        logout.setOnClickListener {
            startActivity(Intent(this, Login::class.java))
            finish() // Prevents going back after logout
        }
        val notificationsSwitch: SwitchCompat = findViewById(R.id.notifcations)



        // Notification permission
        ensureNotificationPermission()

        val notificationHelper = NotificationHelper(this)
        notificationHelper.createNotificationChannel()

        // Load user data from Firebase
        loadUserData()

        // --- Click Listeners ---
        nameTextView.setOnClickListener {
            notificationHelper.showNotification("Hey!", "This is an immediate notification")
        }

        // Schedule daily notification
        scheduleDailyNotification(
            this,
            hour = 18,
            minute = 30,
            requestCode = 1001,
            title = "Daily Workout",
            message = "Time to move!"
        )

        notificationsSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                AlertDialog.Builder(this)
                    .setTitle("Enable Notifications?")
                    .setMessage("Are you sure you want notifications?")
                    .setPositiveButton("Yes") { _, _ ->
                        ensureNotificationPermission()
                        Toast.makeText(this, "Notifications Enabled", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("No") { _, _ ->
                        notificationsSwitch.isChecked = false
                    }
                    .setCancelable(false)
                    .show()
            } else {
                Toast.makeText(this, "Notifications Off", Toast.LENGTH_SHORT).show()
            }
        }
        // Profile image picker
        profileImageView.setOnClickListener { openImagePicker() }

        // Bottom navigation
        ibProfile.setOnClickListener { startActivity(Intent(this, Setting::class.java)) }
        ibHome.setOnClickListener { startActivity(Intent(this, MainActivity::class.java)) }
        ibGym.setOnClickListener { startActivity(Intent(this, GymComponentActivity::class.java)) }
        ibMeditate.setOnClickListener { startActivity(Intent(this, MeditationActivity::class.java)) }

        // Back button
        backBtn.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        // Open Personal_info to edit profile
        btnProfile.setOnClickListener {
            startActivity(Intent(this, Personal_info::class.java))
            finish()
        }

        // Settings buttons

    }

    // --- IMAGE PICKER ---
    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data?.data != null) {
            imageUri = data.data
            val inputStream: InputStream? = contentResolver.openInputStream(imageUri!!)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            profileImageView.setImageBitmap(bitmap)
            saveImageToFirebase(bitmap)
        }
    }

    // --- SAVE IMAGE AS BASE64 IN REALTIME DATABASE ---
    private fun saveImageToFirebase(bitmap: Bitmap) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val databaseRef = FirebaseDatabase.getInstance().getReference("users").child(userId)
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, baos)
        val imageBytes = baos.toByteArray()
        val imageString = Base64.encodeToString(imageBytes, Base64.DEFAULT)
        databaseRef.child("profileImageBase64").setValue(imageString)
            .addOnSuccessListener { Toast.makeText(this, "Profile image saved!", Toast.LENGTH_SHORT).show() }
            .addOnFailureListener { Toast.makeText(this, "Failed to save image", Toast.LENGTH_SHORT).show() }
    }

    // --- LOAD IMAGE, NAME, AND NUMBER FROM DATABASE ---
    private fun loadUserData() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val databaseRef = FirebaseDatabase.getInstance().getReference("users").child(userId)
        databaseRef.get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                val base64Image = snapshot.child("profileImageBase64").getValue(String::class.java)
                if (!base64Image.isNullOrEmpty()) {
                    val imageBytes = Base64.decode(base64Image, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                    profileImageView.setImageBitmap(bitmap)
                }
                val name = snapshot.child("name").getValue(String::class.java) ?: ""
                val surname = snapshot.child("surname").getValue(String::class.java) ?: ""
                val phone = snapshot.child("phone").getValue(String::class.java) ?: ""
                nameTextView.text = "$name $surname"
                numberTextView.text = phone
            } else {
                Toast.makeText(this, "No user data found. Complete your profile.", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, Personal_info::class.java))
                finish()
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Failed to load data", Toast.LENGTH_SHORT).show()
        }
    }
}