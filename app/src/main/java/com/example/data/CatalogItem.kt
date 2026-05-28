package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "items")
data class CatalogItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val category: String,
    val price: String,
    val image: String,
    val desc: String,
    val message: String,
    val createdAt: Long = System.currentTimeMillis()
)
