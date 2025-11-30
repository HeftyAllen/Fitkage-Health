package com.example.fitkagehealth.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.fitkagehealth.R
import com.example.fitkagehealth.databinding.ItemProgressEntryBinding
import com.example.fitkagehealth.model.ProgressEntry
import java.text.SimpleDateFormat
import java.util.*

class ProgressAdapter(
    private val onClick: (ProgressEntry) -> Unit,
    private val onMenuClick: (ProgressEntry, View) -> Unit
) : ListAdapter<ProgressEntry, ProgressAdapter.ProgressViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProgressViewHolder {
        val binding = ItemProgressEntryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ProgressViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProgressViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    inner class ProgressViewHolder(
        private val binding: ItemProgressEntryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ProgressEntry) {
            // Bind basic data
            binding.weightText.text = "${"%.1f".format(item.weightKg)} kg"

            binding.dateText.text = if (item.timestamp > 0L) {
                SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(item.timestamp))
            } else {
                "—"
            }

            // Handle notes
            binding.notesPreview.text = if (item.notes.isNotEmpty()) {
                if (item.notes.length > 80) item.notes.substring(0, 80) + "…" else item.notes
            } else {
                "No notes"
            }

            // Reset photo views visibility first
            binding.photosPreview.visibility = View.GONE
            binding.photo1.visibility = View.GONE
            binding.photo2.visibility = View.GONE
            binding.photo3.visibility = View.GONE

            // Handle photos
            if (item.photos.isNotEmpty()) {
                binding.photosPreview.visibility = View.VISIBLE
                // Load first 3 photos as preview
                item.photos.take(3).forEachIndexed { index, photoUrl ->
                    when (index) {
                        0 -> {
                            binding.photo1.visibility = View.VISIBLE
                            Glide.with(binding.photo1)
                                .load(photoUrl)
                                .centerCrop()
                                .placeholder(R.drawable.placeholder_photo)
                                .into(binding.photo1)
                        }
                        1 -> {
                            binding.photo2.visibility = View.VISIBLE
                            Glide.with(binding.photo2)
                                .load(photoUrl)
                                .centerCrop()
                                .placeholder(R.drawable.placeholder_photo)
                                .into(binding.photo2)
                        }
                        2 -> {
                            binding.photo3.visibility = View.VISIBLE
                            Glide.with(binding.photo3)
                                .load(photoUrl)
                                .centerCrop()
                                .placeholder(R.drawable.placeholder_photo)
                                .into(binding.photo3)
                        }
                    }
                }
            }

            // Update badges
            binding.photosBadge.text = "${item.photos.size} ${if (item.photos.size == 1) "photo" else "photos"}"
            binding.liftsBadge.text = "${item.lifts.size} ${if (item.lifts.size == 1) "lift" else "lifts"}"

            // Click listeners
            binding.root.setOnClickListener {
                onClick(item)
            }

            binding.menuButton.setOnClickListener { view ->
                onMenuClick(item, view)
            }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<ProgressEntry>() {
            override fun areItemsTheSame(oldItem: ProgressEntry, newItem: ProgressEntry): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: ProgressEntry, newItem: ProgressEntry): Boolean {
                return oldItem == newItem
            }
        }
    }
}
