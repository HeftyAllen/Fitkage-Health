package com.example.fitkagehealth

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.*

class FAQsScreen : BaseActivity() {

    private lateinit var recyclerView: RecyclerView
    private var mList = ArrayList<LanguageData>()
    private lateinit var adapter: LanguageAdapter
    private lateinit var backBtn: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_faqs_screen)

        recyclerView = findViewById(R.id.recyclerView)
        backBtn = findViewById(R.id.backBtn)
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // load FAQ data
        addDataToList()

        adapter = LanguageAdapter(mList)
        recyclerView.adapter = adapter
    }


    private fun filterList(query: String?) {
        if (query != null) {
            val filteredList = ArrayList<LanguageData>()
            for (i in mList) {
                if (i.title.lowercase(Locale.ROOT).contains(query.lowercase(Locale.ROOT))) {
                    filteredList.add(i)
                }
            }

            if (filteredList.isEmpty()) {
                Toast.makeText(this, "No FAQ found", Toast.LENGTH_SHORT).show()
            } else {
                adapter.setFilteredList(filteredList)
            }
        }
    }

    private fun addDataToList() {
        mList.add(
            LanguageData(
                "Is it free?",
                R.drawable.some, // your icon here
                "Yes, FitKage has a free version with essential health tracking. Some premium features may require a subscription."
            )
        )
        mList.add(
            LanguageData(
                "What is FitKage?",
                R.drawable.muscular,
                "FitKage is a health and wellness app that helps users track steps, calories, workouts, and monitor overall progress towards fitness goals."
            )
        )
        mList.add(
            LanguageData(
                "Does it collect information?",
                R.drawable.folder,
                "FitKage only collects data you choose to share. Your health information is encrypted and kept secure."
            )
        )
        mList.add(
            LanguageData(
                "Where can I find the latest FitKage health news?",
                R.drawable.megaphone,
                "You can find the latest updates in the News section of the app or on FitKage’s official website and social media channels."
            )
        )
        mList.add(
            LanguageData(
                "Can I sync FitKage with other apps or devices?",
                R.drawable.tick,
                "Yes, FitKage supports syncing with popular fitness trackers and health apps to centralize your progress."
            )
        )
        mList.add(
            LanguageData(
                "Is FitKage available offline?",
                R.drawable.offline,
                "Core features like step tracking and workout logging work offline. Internet is needed for backups and insights."
            )
        )
        mList.add(
            LanguageData(
                "Who can use FitKage?",
                R.drawable.users,
                "FitKage is designed for anyone looking to improve their health—from beginners to professional athletes."
            )
        )
        mList.add(
            LanguageData(
                "Does FitKage offer personalized plans?",
                R.drawable.plan,
                "Yes, FitKage provides personalized workout and nutrition recommendations based on your fitness goals."
            )
        )
        backBtn.setOnClickListener {
            startActivity(Intent(this, Setting::class.java))
        }
    }

}