package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "settings")
data class CatalogSettings(
    @PrimaryKey val id: Int = 1,
    val appName: String,
    val appSubtitle: String,
    val whatsappNumber: String,
    val hashedPin: String,
    val firebaseProjectId: String = "aistudio-shared-databases",
    val isConfigured: Boolean = false
)

