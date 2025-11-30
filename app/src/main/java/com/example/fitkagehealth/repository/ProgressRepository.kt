package com.example.fitkagehealth.repository

import android.content.Context
import android.net.Uri
import com.example.fitkagehealth.model.ProgressEntry
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.*

class ProgressRepository {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseDatabase.getInstance().reference
    private val storage = FirebaseStorage.getInstance().reference

    private fun uid(): String = auth.currentUser?.uid ?: throw IllegalStateException("Not logged in")

    private fun entriesRef() = db.child("progress_entries").child(uid())

    /**
     * Upload multiple image URIs to Firebase Storage under entryId and return list of public URLs.
     */
    suspend fun uploadPhotos(entryId: String, uris: List<Uri>, context: Context): List<String> =
        withContext(Dispatchers.IO) {
            val results = mutableListOf<String>()
            uris.forEach { uri ->
                val fileName = UUID.randomUUID().toString() + "_" + (uri.lastPathSegment ?: "img")
                val ref = storage.child("progress_photos").child(uid()).child(entryId).child(fileName)
                ref.putFile(uri).await()
                val url = ref.downloadUrl.await().toString()
                results.add(url)
            }
            results
        }

    /**
     * Save or update an entry. Writes a typed Map to ensure numeric fields are stored as numbers.
     */
    fun saveEntry(entry: ProgressEntry) {
        val key = if (entry.id.isBlank()) entriesRef().push().key ?: throw IllegalStateException("no key") else entry.id
        val modelMap: Map<String, Any?> = mapOf(
            "id" to key,
            "timestamp" to entry.timestamp,
            // ensure weight is a numeric type
            "weightKg" to entry.weightKg,
            "photos" to entry.photos,
            "notes" to entry.notes,
            "lifts" to entry.lifts,
            "visible" to entry.visible
        )
        entriesRef().child(key).setValue(modelMap)
    }

    /**
     * Delete an entry (this doesn't delete photos in storage — you can extend to delete them)
     */
    fun deleteEntry(entryId: String) {
        entriesRef().child(entryId).removeValue()
    }

    /**
     * Attach realtime listener to entries. Caller provides ValueEventListener.
     */
    fun attachEntriesListener(listener: ValueEventListener) {
        entriesRef().orderByChild("timestamp").addValueEventListener(listener)
    }

    fun detachEntriesListener(listener: ValueEventListener) {
        entriesRef().removeEventListener(listener)
    }
}
