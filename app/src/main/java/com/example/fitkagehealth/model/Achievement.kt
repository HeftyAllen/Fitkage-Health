package com.example.fitkagehealth.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Achievement(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val iconRes: Int = 0,
    val unlocked: Boolean = false,
    val unlockedDate: Long? = null,
    val progress: Int = 0,
    val target: Int = 0
) : Parcelable