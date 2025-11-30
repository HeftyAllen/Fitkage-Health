package com.example.fitkagehealth.model

import com.google.firebase.database.DataSnapshot
import java.util.*

data class ProgressEntry(
    val id: String = "",
    val timestamp: Long = 0L,
    val weightKg: Double = 0.0,
    val photos: List<String> = emptyList(),           // storage URLs
    val notes: String = "",
    val lifts: Map<String, String> = emptyMap(),      // e.g. {"Bench":"80kg x 5", "Squat":"100kg x 3"}
    val visible: Boolean = true
) {
    companion object {
        private fun anyToDoubleOrNull(value: Any?): Double? {
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

        private fun anyToLongOrNull(value: Any?): Long? {
            return when (value) {
                null -> null
                is Long -> value
                is Int -> value.toLong()
                is Double -> value.toLong()
                is Float -> value.toLong()
                is Number -> value.toLong()
                is String -> value.toLongOrNull()
                else -> value.toString().toLongOrNull()
            }
        }

        /**
         * Create ProgressEntry from a DataSnapshot safely.
         * Handles weight stored as Double/Long/String and photos stored as list/map/string.
         */
        fun fromSnapshot(child: DataSnapshot): ProgressEntry {
            val id = child.child("id").value?.toString() ?: child.key ?: UUID.randomUUID().toString()

            val timestampAny = child.child("timestamp").value ?: child.child("time").value ?: child.child("ts").value
            val timestamp = anyToLongOrNull(timestampAny) ?: 0L

            val weightAny = when {
                child.hasChild("weightKg") -> child.child("weightKg").value
                child.hasChild("weight_kg") -> child.child("weight_kg").value
                child.hasChild("weight") -> child.child("weight").value
                else -> child.child("weightKg").value
            }
            val weightKg = anyToDoubleOrNull(weightAny) ?: 0.0

            val photosList = mutableListOf<String>()
            val photosSnap = child.child("photos")
            if (photosSnap.exists()) {
                photosSnap.children.forEach { p ->
                    p.value?.toString()?.let { photosList.add(it) }
                }
                // If `photos` stored as a single string or as a primitive
                if (photosList.isEmpty()) {
                    child.child("photos").value?.toString()?.let { photosList.add(it) }
                }
            }

            val notes = child.child("notes").value?.toString() ?: ""

            val liftsMap = mutableMapOf<String, String>()
            val liftsSnap = child.child("lifts")
            if (liftsSnap.exists()) {
                liftsSnap.children.forEach { liftChild ->
                    val key = liftChild.key ?: ""
                    val value = liftChild.value?.toString() ?: ""
                    if (key.isNotEmpty()) liftsMap[key] = value
                }
            }

            val visible = child.child("visible").value?.toString()?.toBoolean() ?: true

            return ProgressEntry(
                id = id,
                timestamp = timestamp,
                weightKg = weightKg,
                photos = photosList,
                notes = notes,
                lifts = liftsMap,
                visible = visible
            )
        }
    }
}
