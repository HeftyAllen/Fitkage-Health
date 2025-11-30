package com.example.fitkagehealth.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.fitkagehealth.R
import com.example.fitkagehealth.model.WeatherActivity

class WeatherActivityAdapter(
    private val activities: List<WeatherActivity>
) : RecyclerView.Adapter<WeatherActivityAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_weather_activity, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val activity = activities[position]
        holder.bind(activity)
    }

    override fun getItemCount(): Int = activities.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val icon: ImageView = itemView.findViewById(R.id.activityIcon)
        private val name: TextView = itemView.findViewById(R.id.activityName)
        private val score: TextView = itemView.findViewById(R.id.activityScore)
        private val recommendation: TextView = itemView.findViewById(R.id.activityRecommendation)
        private val recommendationIndicator: View = itemView.findViewById(R.id.recommendationIndicator)

        fun bind(activity: WeatherActivity) {
            icon.setImageResource(activity.icon)
            name.text = activity.name
            score.text = "${activity.score}/10"
            recommendation.text = activity.recommendation

            // Set recommendation indicator color
            val colorRes = if (activity.isRecommended) {
                R.color.good_condition
            } else {
                R.color.poor_condition
            }
            recommendationIndicator.setBackgroundColor(
                ContextCompat.getColor(itemView.context, colorRes)
            )

            // Set score text color based on recommendation
            val scoreColorRes = when {
                activity.score >= 8 -> R.color.excellent_score
                activity.score >= 6 -> R.color.good_score
                activity.score >= 4 -> R.color.fair_score
                else -> R.color.poor_score
            }
            score.setTextColor(ContextCompat.getColor(itemView.context, scoreColorRes))
        }
    }
}