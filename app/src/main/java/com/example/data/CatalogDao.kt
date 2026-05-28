package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CatalogDao {
    // Settings operations
    @Query("SELECT * FROM settings WHERE id = 1 LIMIT 1")
    fun getSettingsFlow(): Flow<CatalogSettings?>

    @Query("SELECT * FROM settings WHERE id = 1 LIMIT 1")
    suspend fun getSettingsDirect(): CatalogSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettings(settings: CatalogSettings)

    @Update
    suspend fun updateSettings(settings: CatalogSettings)

    // Catalog items operations
    @Query("SELECT * FROM items ORDER BY createdAt DESC")
    fun getAllItemsFlow(): Flow<List<CatalogItem>>

    @Query("SELECT * FROM items")
    suspend fun getAllItemsDirect(): List<CatalogItem>


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: CatalogItem): Long

    @Update
    suspend fun updateItem(item: CatalogItem)

    @Delete
    suspend fun deleteItem(item: CatalogItem)

    @Query("DELETE FROM items WHERE id = :id")
    suspend fun deleteItemById(id: Long)
}
