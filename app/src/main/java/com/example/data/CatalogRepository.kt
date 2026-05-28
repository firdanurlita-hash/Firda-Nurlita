package com.example.data

import android.util.Log
import kotlinx.coroutines.flow.Flow

class CatalogRepository(private val catalogDao: CatalogDao) {

    companion object {
        const val APP_ID = "42000fc5-7c5a-440f-9570-9c01a8a5244d"
        private const val TAG = "CatalogRepository"
    }

    val settingsFlow: Flow<CatalogSettings?> = catalogDao.getSettingsFlow()
    val allItemsFlow: Flow<List<CatalogItem>> = catalogDao.getAllItemsFlow()

    suspend fun getSettingsDirect(): CatalogSettings? {
        return catalogDao.getSettingsDirect()
    }

    suspend fun saveSettings(settings: CatalogSettings) {
        val cleanSettings = settings.copy(id = 1)
        catalogDao.insertSettings(cleanSettings)
        
        // Asynchronously sync settings to cloud
        try {
            val projectId = cleanSettings.firebaseProjectId.ifBlank { "aistudio-shared-databases" }
            FirestoreApiClient.service.saveSettings(projectId, APP_ID, cleanSettings.toFirestoreDocument())
            Log.d(TAG, "Successfully synced settings to Firebase!")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync settings to Firebase: ${e.message}", e)
        }
    }

    suspend fun updateSettings(settings: CatalogSettings) {
        saveSettings(settings)
    }

    suspend fun insertItem(item: CatalogItem) {
        val localId = catalogDao.insertItem(item)
        val insertedItem = item.copy(id = localId)
        
        // Sync new item to cloud using the local SQL primary key auto-generated ID
        val settings = getSettingsDirect()
        if (settings != null) {
            try {
                val projectId = settings.firebaseProjectId.ifBlank { "aistudio-shared-databases" }
                FirestoreApiClient.service.saveItem(projectId, APP_ID, localId.toString(), insertedItem.toFirestoreDocument())
                Log.d(TAG, "Successfully added new item to Firebase: $localId")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to sync new item to Firebase: ${e.message}", e)
            }
        }
    }

    suspend fun updateItem(item: CatalogItem) {
        catalogDao.updateItem(item)
        
        // Sync updated item to cloud
        val settings = getSettingsDirect()
        if (settings != null) {
            try {
                val projectId = settings.firebaseProjectId.ifBlank { "aistudio-shared-databases" }
                FirestoreApiClient.service.saveItem(projectId, APP_ID, item.id.toString(), item.toFirestoreDocument())
                Log.d(TAG, "Successfully updated item on Firebase: ${item.id}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to sync update to Firebase: ${e.message}", e)
            }
        }
    }

    suspend fun deleteItemById(id: Long) {
        catalogDao.deleteItemById(id)
        
        // Sync deletion to cloud
        val settings = getSettingsDirect()
        if (settings != null) {
            try {
                val projectId = settings.firebaseProjectId.ifBlank { "aistudio-shared-databases" }
                FirestoreApiClient.service.deleteItem(projectId, APP_ID, id.toString())
                Log.d(TAG, "Successfully deleted item from Firebase: $id")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to sync deletion to Firebase: ${e.message}", e)
            }
        }
    }

    // Main cloud synchronization reconciliation fetch
    suspend fun fetchCloudSync(): Boolean {
        var settings = getSettingsDirect()
        val projectId = settings?.firebaseProjectId?.ifBlank { "aistudio-shared-databases" } ?: "aistudio-shared-databases"
        return try {
            Log.d(TAG, "Starting Cloud sync reconciliation from Firestore (Project: $projectId, App: $APP_ID)...")
            
            // 1. Fetch remote Settings
            var hasRemoteSettings = false
            try {
                val remoteSettingsDoc = FirestoreApiClient.service.getSettings(projectId, APP_ID)
                val remoteSettings = remoteSettingsDoc.toCatalogSettings(projectId)
                
                // If local settings are null, or if remote settings have been updated, merge to local Room
                if (settings == null || 
                    remoteSettings.appName != settings.appName || 
                    remoteSettings.whatsappNumber != settings.whatsappNumber ||
                    remoteSettings.hashedPin != settings.hashedPin) {
                    catalogDao.insertSettings(remoteSettings.copy(id = 1))
                    Log.d(TAG, "Successfully saved remote settings to local Room.")
                    settings = remoteSettings.copy(id = 1)
                }
                hasRemoteSettings = true
            } catch (e: Exception) {
                Log.w(TAG, "Remote settings not found or inaccessible in cloud: ${e.message}")
                if (settings != null) {
                    try {
                        Log.d(TAG, "Syncing local settings context to cloud...")
                        FirestoreApiClient.service.saveSettings(projectId, APP_ID, settings.toFirestoreDocument())
                        hasRemoteSettings = true
                    } catch (saveEx: Exception) {
                        Log.e(TAG, "Failed to write local settings to cloud: ${saveEx.message}", saveEx)
                    }
                }
            }

            // 2. Fetch remote products (only if we have/had settings, or if remote settings were found)
            if (settings != null || hasRemoteSettings) {
                val response = FirestoreApiClient.service.getItems(projectId, APP_ID)
                val cloudItems = response.documents?.map { doc ->
                    val docId = doc.name?.substringAfterLast("/") ?: ""
                    doc.toCatalogItem(docId)
                } ?: emptyList()

                // 3. Reconcile database listings:
                // Since cloud is the source of truth for synced multiple accounts/phones:
                // Insert or update changed items, and delete local items that are not in cloud
                val localItems = catalogDao.getAllItemsDirect()
                val cloudIds = cloudItems.map { it.id }.toSet()

                // Delete local items no longer in Cloud
                localItems.forEach { local ->
                    if (!cloudIds.contains(local.id)) {
                        catalogDao.deleteItemById(local.id)
                    }
                }

                // Insert/Update from Cloud to local
                cloudItems.forEach { cloudItem ->
                    catalogDao.insertItem(cloudItem)
                }

                Log.d(TAG, "Cloud sync complete. Synchronized items count: ${cloudItems.size}")
                true
            } else {
                Log.d(TAG, "No local or remote settings found. Setup required first.")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed Cloud sync reconciliation: ${e.message}", e)
            false
        }
    }
}

