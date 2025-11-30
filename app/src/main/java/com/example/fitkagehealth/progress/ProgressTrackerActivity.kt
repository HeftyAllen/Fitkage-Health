package com.example.fitkagehealth.progress

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fitkagehealth.R
import com.example.fitkagehealth.adapters.ProgressAdapter
import com.example.fitkagehealth.databinding.ActivityProgressTrackerBinding
import com.example.fitkagehealth.databinding.DialogAddProgressBinding
import com.example.fitkagehealth.model.ProgressEntry
import com.example.fitkagehealth.repository.ProgressRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class ProgressTrackerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProgressTrackerBinding
    private val repo = ProgressRepository()
    private val auth = FirebaseAuth.getInstance()

    private val adapter by lazy { ProgressAdapter(::onEntryClicked, ::onEntryMenuClicked) }
    private val entries = mutableListOf<ProgressEntry>()

    // image picker
    private var imageUrisToUpload: List<Uri> = emptyList()
    private val pickImages = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        imageUrisToUpload = uris ?: emptyList()
        if (imageUrisToUpload.isNotEmpty()) {
            showToast("${imageUrisToUpload.size} photos selected")
        }
    }

    // Firebase listeners (kept so we can detach them)
    private var entriesListener: ValueEventListener? = null
    private var profileListener: ValueEventListener? = null
    private var profileRef: DatabaseReference? = null

    // cached profile weight (kg) if present
    private var profileWeightKg: Double? = null

    private var currentFilter = "all" // all, week, month, year

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProgressTrackerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        setupRecyclerView()

        // Listen to profile first (so fallback is ready)
        attachUserProfileListener()

        // Then attach entries listener
        attachRealtimeListener()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.progress_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_stats -> {
                showStatsDialog()
                true
            }
            R.id.action_export -> {
                exportProgressData()
                true
            }
            R.id.action_settings -> {
                showSettingsDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupUI() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.fabAddEntry.setOnClickListener {
            showAddEditDialog()
        }

        // Empty state button
        binding.emptyState.setOnClickListener {
            showAddEditDialog()
        }

        // Timeline filter
        binding.timelineFilter.setOnClickListener {
            showFilterMenu()
        }
    }

    private fun setupRecyclerView() {
        binding.progressRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.progressRecyclerView.adapter = adapter
        binding.progressRecyclerView.itemAnimator = androidx.recyclerview.widget.DefaultItemAnimator()
    }

    /**
     * Attach (or re-attach) the realtime listener for progress entries.
     * This method detaches any previously attached entriesListener before attaching the new one.
     */
    private fun attachRealtimeListener() {
        // detach previous listener if present
        entriesListener?.let {
            repo.detachEntriesListener(it)
            entriesListener = null
        }

        val uid = auth.currentUser?.uid ?: run {
            showToast("Please log in to track progress")
            finish()
            return
        }

        entriesListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<ProgressEntry>()

                snapshot.children.forEach { child ->
                    try {
                        val entry = ProgressEntry.fromSnapshot(child)
                        list.add(entry)
                    } catch (e: Exception) {
                        Log.w("ProgressTracker", "Failed parsing entry ${child.key}", e)
                    }
                }

                // Apply current filter and sort newest-first
                val filteredList = applyFilter(list, currentFilter).toMutableList()
                filteredList.sortByDescending { it.timestamp }

                entries.clear()
                entries.addAll(filteredList)
                adapter.submitList(ArrayList(entries))

                // Update header stats using the full unfiltered list
                updateHeaderStats(list)
                updateEmptyState()
            }

            override fun onCancelled(error: DatabaseError) {
                showToast("Failed to load progress data")
            }
        }

        // Use repository to attach listener (preserves your repo's path logic)
        repo.attachEntriesListener(entriesListener!!)
    }

    /**
     * Listen to the user profile node so we can get profile weight and auto-update header
     */
    private fun attachUserProfileListener() {
        // detach prior
        profileRef?.let { ref ->
            profileListener?.let { ref.removeEventListener(it) }
        }
        profileRef = null
        profileListener = null

        val uid = auth.currentUser?.uid ?: return
        val db = FirebaseDatabase.getInstance()
        profileRef = db.getReference("users").child(uid)

        fun anyToDoubleOrNull(value: Any?): Double? {
            return when (value) {
                null -> null
                is Double -> value
                is Float -> value.toDouble()
                is Long -> value.toDouble()
                is Int -> value.toDouble()
                is Number -> value.toDouble()
                is String -> value.toDoubleOrNull()
                else -> value.toString().toDoubleOrNull()
            }
        }

        profileListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val candidateKeys = listOf("weight_kg", "weightKg", "weight")
                var found: Double? = null

                for (key in candidateKeys) {
                    if (snapshot.hasChild(key)) {
                        val raw = snapshot.child(key).value
                        found = anyToDoubleOrNull(raw)
                        if (found != null) break
                    }
                }

                if (found == null) {
                    found = anyToDoubleOrNull(snapshot.child("weight_kg").value ?: snapshot.child("weightKg").value ?: snapshot.child("weight").value ?: snapshot.value)
                }

                profileWeightKg = found

                // If there are no progress entries currently shown, update header with profile weight
                if (entries.isEmpty()) {
                    if (profileWeightKg != null) {
                        binding.latestWeightText.text = "${"%.1f".format(profileWeightKg)} kg"
                        binding.progressSummary.text = "Weight from profile"
                        binding.weightDeltaText.visibility = View.GONE
                    } else {
                        binding.latestWeightText.text = "—"
                        binding.progressSummary.text = "Start your fitness journey with your first entry"
                        binding.weightDeltaText.visibility = View.GONE
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // silence or show a small toast if you want
            }
        }

        profileRef?.addValueEventListener(profileListener as ValueEventListener)
    }

    private fun applyFilter(entries: List<ProgressEntry>, filter: String): List<ProgressEntry> {
        val calendar = Calendar.getInstance()
        val now = Date()

        return when (filter) {
            "week" -> {
                calendar.time = now
                calendar.add(Calendar.WEEK_OF_YEAR, -1)
                val weekAgo = calendar.time
                entries.filter { entry -> Date(entry.timestamp) >= weekAgo }
            }
            "month" -> {
                calendar.time = now
                calendar.add(Calendar.MONTH, -1)
                val monthAgo = calendar.time
                entries.filter { entry -> Date(entry.timestamp) >= monthAgo }
            }
            "year" -> {
                calendar.time = now
                calendar.add(Calendar.YEAR, -1)
                val yearAgo = calendar.time
                entries.filter { entry -> Date(entry.timestamp) >= yearAgo }
            }
            else -> entries
        }
    }

    /**
     * Update the top header stats.
     * - If progress entries exist, use the latest entry
     * - If no progress entries, fallback to profileWeightKg (if present)
     */
    private fun updateHeaderStats(allEntries: List<ProgressEntry>) {
        if (allEntries.isEmpty()) {
            // No progress entries -> fallback to profile weight (if available)
            profileWeightKg?.let { weight ->
                binding.latestWeightText.text = "${"%.1f".format(weight)} kg"
                binding.progressSummary.text = "Weight from profile"
            } ?: run {
                binding.latestWeightText.text = "—"
                binding.progressSummary.text = "Start your fitness journey with your first entry"
            }

            binding.weightDeltaText.visibility = View.GONE
            binding.totalEntries.text = "0"
            binding.photoCount.text = "0"
            return
        }

        // There are progress entries -> use them (priority)
        val latest = allEntries.maxByOrNull { it.timestamp }
        latest?.let { entry ->
            binding.latestWeightText.text = "${"%.1f".format(entry.weightKg)} kg"
            binding.progressSummary.text = "Latest entry: ${formatDate(entry.timestamp)}"

            // Calculate weight change from previous entry
            if (allEntries.size > 1) {
                val sortedEntries = allEntries.sortedByDescending { it.timestamp }
                val previous = sortedEntries[1]
                val diff = entry.weightKg - previous.weightKg
                val sign = if (diff > 0) "+" else ""
                binding.weightDeltaText.visibility = View.VISIBLE
                binding.weightDeltaText.text = "$sign${"%.1f".format(diff)} kg"
                binding.weightDeltaText.setTextColor(
                    if (diff > 0) getColor(R.color.red) else getColor(R.color.green_light)
                )
            } else {
                binding.weightDeltaText.visibility = View.GONE
            }
        }

        // Update other stats
        binding.totalEntries.text = allEntries.size.toString()
        val totalPhotos = allEntries.sumOf { it.photos.size }
        binding.photoCount.text = totalPhotos.toString()
    }

    private fun updateEmptyState() {
        if (entries.isEmpty()) {
            binding.emptyState.visibility = View.VISIBLE
            binding.progressRecyclerView.visibility = View.GONE
        } else {
            binding.emptyState.visibility = View.GONE
            binding.progressRecyclerView.visibility = View.VISIBLE
        }
    }

    private fun showAddEditDialog(editEntry: ProgressEntry? = null) {
        val dialogBinding = DialogAddProgressBinding.inflate(layoutInflater)

        if (editEntry != null) {
            populateEditDialog(dialogBinding, editEntry)
        } else {
            setupNewEntryDialog(dialogBinding)
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle(if (editEntry == null) "Add Progress Entry" else "Edit Entry")
            .setView(dialogBinding.root)
            .setNegativeButton("Cancel", null)
            .setPositiveButton(if (editEntry == null) "Add" else "Save", null)
            .create()

        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                if (validateAndSaveEntry(dialogBinding, editEntry)) {
                    dialog.dismiss()
                }
            }
        }

        dialog.show()
    }

    private fun populateEditDialog(dialogBinding: DialogAddProgressBinding, entry: ProgressEntry) {
        dialogBinding.weightEdit.setText(entry.weightKg.toString())
        dialogBinding.notesEdit.setText(entry.notes)
        dialogBinding.photosCount.text = "${entry.photos.size} photos"
        dialogBinding.liftsEdit.setText(
            entry.lifts.entries.joinToString("\n") { liftEntry -> "${liftEntry.key}: ${liftEntry.value}" }
        )
    }

    private fun setupNewEntryDialog(dialogBinding: DialogAddProgressBinding) {
        dialogBinding.photosCount.text = "0 photos selected"
        dialogBinding.pickPhotosBtn.setOnClickListener {
            pickImages.launch("image/*")
        }
    }

    private fun validateAndSaveEntry(dialogBinding: DialogAddProgressBinding, editEntry: ProgressEntry?): Boolean {
        val weight = dialogBinding.weightEdit.text.toString().toDoubleOrNull()
        if (weight == null) {
            showToast("Please enter a valid weight")
            return false
        }

        val notes = dialogBinding.notesEdit.text.toString().trim()
        val liftsRaw = dialogBinding.liftsEdit.text.toString().trim()
        val liftsMap = parseLifts(liftsRaw)

        val entryId = editEntry?.id ?: UUID.randomUUID().toString()
        val existingPhotos = editEntry?.photos ?: emptyList()

        val newEntry = ProgressEntry(
            id = entryId,
            timestamp = System.currentTimeMillis(),
            weightKg = weight,
            photos = existingPhotos,
            notes = notes,
            lifts = liftsMap
        )

        saveEntryWithPhotos(newEntry)
        return true
    }

    private fun parseLifts(liftsRaw: String): Map<String, String> {
        val liftsMap = mutableMapOf<String, String>()
        if (liftsRaw.isNotEmpty()) {
            liftsRaw.lines().forEach { line ->
                val parts = line.split(":").map { it.trim() }
                if (parts.size >= 2) {
                    liftsMap[parts[0]] = parts.subList(1, parts.size).joinToString(":")
                }
            }
        }
        return liftsMap
    }

    private fun saveEntryWithPhotos(entry: ProgressEntry) {
        lifecycleScope.launch {
            try {
                showLoading(true)

                val finalEntry = if (imageUrisToUpload.isNotEmpty()) {
                    val uploaded = repo.uploadPhotos(entry.id, imageUrisToUpload, this@ProgressTrackerActivity)
                    entry.copy(photos = entry.photos + uploaded)
                } else {
                    entry
                }

                repo.saveEntry(finalEntry)
                showToast("Entry saved!")

                // Reset selection
                imageUrisToUpload = emptyList()
                showLoading(false)

                // after saving, refresh entries by re-attaching listener (safe)
                attachRealtimeListener()
            } catch (e: Exception) {
                showLoading(false)
                showToast("Failed to save: ${e.message}")
                Log.e("ProgressTracker", "saveEntryWithPhotos error", e)
            }
        }
    }

    private fun onEntryClicked(entry: ProgressEntry) {
        showEntryDetails(entry)
    }

    private fun onEntryMenuClicked(entry: ProgressEntry, view: View) {
        showEntryMenu(entry, view)
    }

    private fun showEntryDetails(entry: ProgressEntry) {
        val dialogBinding = DialogAddProgressBinding.inflate(layoutInflater)

        dialogBinding.weightEdit.setText(entry.weightKg.toString())
        dialogBinding.notesEdit.setText(entry.notes)
        dialogBinding.liftsEdit.setText(
            entry.lifts.entries.joinToString("\n") { liftEntry -> "${liftEntry.key}: ${liftEntry.value}" }
        )

        dialogBinding.weightEdit.isEnabled = false
        dialogBinding.notesEdit.isEnabled = false
        dialogBinding.liftsEdit.isEnabled = false
        dialogBinding.pickPhotosBtn.visibility = View.GONE
        dialogBinding.photosCount.text = "${entry.photos.size} photos"

        AlertDialog.Builder(this)
            .setTitle("${"%.1f".format(entry.weightKg)} kg - ${formatDate(entry.timestamp)}")
            .setView(dialogBinding.root)
            .setPositiveButton("Edit") { _, _ -> showAddEditDialog(entry) }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun showEntryMenu(entry: ProgressEntry, anchor: View) {
        val popup = PopupMenu(this, anchor)
        popup.menuInflater.inflate(R.menu.entry_menu, popup.menu)

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu_edit -> {
                    showAddEditDialog(entry)
                    true
                }
                R.id.menu_delete -> {
                    showDeleteConfirmation(entry)
                    true
                }
                R.id.menu_share -> {
                    shareEntry(entry)
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun showDeleteConfirmation(entry: ProgressEntry) {
        AlertDialog.Builder(this)
            .setTitle("Delete Entry?")
            .setMessage("This will permanently remove this progress entry.")
            .setPositiveButton("Delete") { _, _ ->
                repo.deleteEntry(entry.id)
                showToast("Entry deleted")
                // refresh
                attachRealtimeListener()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun shareEntry(entry: ProgressEntry) {
        val shareText = buildShareText(entry)
        val intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, shareText)
            type = "text/plain"
        }
        startActivity(Intent.createChooser(intent, "Share Progress Entry"))
    }

    private fun buildShareText(entry: ProgressEntry): String {
        return """
            🏋️ Progress Update 🎯
            
            Weight: ${"%.1f".format(entry.weightKg)} kg
            Date: ${formatDate(entry.timestamp)}
            ${if (entry.notes.isNotEmpty()) "Notes: ${entry.notes}" else ""}
            
            #FitKage #Progress #FitnessJourney
        """.trimIndent()
    }

    private fun showFilterMenu() {
        val filters = arrayOf("All Time", "This Week", "This Month", "This Year")
        AlertDialog.Builder(this)
            .setTitle("Filter Timeline")
            .setItems(filters) { _, which ->
                currentFilter = when (which) {
                    1 -> "week"
                    2 -> "month"
                    3 -> "year"
                    else -> "all"
                }
                binding.timelineFilter.text = filters[which]
                // re-attach entries listener safely so filter is applied server-side data -> local re-filter
                attachRealtimeListener()
            }
            .show()
    }

    private fun showStatsDialog() {
        showToast("Statistics feature coming soon!")
    }

    private fun exportProgressData() {
        showToast("Export feature coming soon!")
    }

    private fun showSettingsDialog() {
        showToast("Settings feature coming soon!")
    }

    private fun showLoading(show: Boolean) {
        binding.loadingOverlay.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun formatDate(timestamp: Long): String {
        return SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault()).format(Date(timestamp))
    }

    override fun onDestroy() {
        super.onDestroy()
        // detach entries listener
        entriesListener?.let { repo.detachEntriesListener(it) }
        entriesListener = null

        // detach profile listener
        profileRef?.let { ref ->
            profileListener?.let { ref.removeEventListener(it) }
        }
        profileListener = null
        profileRef = null
    }
}
