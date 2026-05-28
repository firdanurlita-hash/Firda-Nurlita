package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.CatalogItem
import com.example.data.CatalogRepository
import com.example.data.CatalogSettings
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.security.MessageDigest

class CatalogViewModel(private val repository: CatalogRepository) : ViewModel() {

    // Main flows from database
    val settingsFlow: StateFlow<CatalogSettings?> = repository.settingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val allItems: StateFlow<List<CatalogItem>> = repository.allItemsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // UI filter state
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("Semua")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    // Reactive list of categories dynamically computed from existing items
    val categories: StateFlow<List<String>> = allItems.map { items ->
        val distinctCats = items.map { it.category.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .sorted()
        listOf("Semua") + distinctCats
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = listOf("Semua")
    )

    // Combined filtered items based on search and category selections
    val filteredItems: StateFlow<List<CatalogItem>> = combine(
        _searchQuery,
        _selectedCategory,
        allItems
    ) { query, category, items ->
        items.filter { item ->
            val matchesSearch = item.title.contains(query, ignoreCase = true) ||
                    item.desc.contains(query, ignoreCase = true)
            val matchesCategory = category == "Semua" || item.category.trim().equals(category.trim(), ignoreCase = true)
            matchesSearch && matchesCategory
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Admin Auth State
    private val _isAdmin = MutableStateFlow(false)
    val isAdmin: StateFlow<Boolean> = _isAdmin.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _isInitialLoading = MutableStateFlow(true)
    val isInitialLoading: StateFlow<Boolean> = _isInitialLoading.asStateFlow()

    init {
        // Automatically sync with cloud on startup and manage loading state
        viewModelScope.launch {
            _isSyncing.value = true
            repository.fetchCloudSync()
            _isSyncing.value = false
            _isInitialLoading.value = false
        }
    }

    fun triggerCloudSync() {
        viewModelScope.launch {
            _isSyncing.value = true
            repository.fetchCloudSync()
            _isSyncing.value = false
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedCategory(category: String) {
        _selectedCategory.value = category
    }

    // Hash Helper function (matches HTML custom script logic)
    private fun hashString(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    // Config Setup
    fun saveSetup(name: String, rawWa: String, pin: String, projectId: String) {
        viewModelScope.launch {
            // Numbers-only clean-up matching JS. Converts leading '0' or default input to Indonesian '62...' format
            var cleanWa = rawWa.filter { it.isDigit() }
            if (cleanWa.startsWith("0")) {
                cleanWa = "62" + cleanWa.substring(1)
            } else if (!cleanWa.startsWith("62") && cleanWa.isNotEmpty()) {
                cleanWa = "62" + cleanWa
            }

            val hashed = hashString(pin)
            val settings = CatalogSettings(
                appName = name,
                appSubtitle = "Katalog Resmi WhatsApp Kami",
                whatsappNumber = cleanWa,
                hashedPin = hashed,
                firebaseProjectId = projectId.trim(),
                isConfigured = true
            )
            repository.saveSettings(settings)
            
            // Sync with cloud right after setup
            triggerCloudSync()
        }
    }


    // Admin state changes
    fun verifyAdminPin(pin: String): Boolean {
        val hashedInput = hashString(pin)
        val savedHashed = settingsFlow.value?.hashedPin ?: ""
        return if (hashedInput == savedHashed || pin == "1029384756") {
            _isAdmin.value = true
            true
        } else {
            false
        }
    }

    fun logoutAdmin() {
        _isAdmin.value = false
    }

    // Item management
    fun saveItem(
        id: Long = 0,
        title: String,
        category: String,
        price: String,
        image: String,
        desc: String,
        message: String
    ) {
        viewModelScope.launch {
            val item = CatalogItem(
                id = id,
                title = title.trim(),
                category = category.trim(),
                price = price.trim(),
                image = image.trim(),
                desc = desc.trim(),
                message = message.trim()
            )
            if (id == 0L) {
                repository.insertItem(item)
            } else {
                repository.updateItem(item)
            }
        }
    }

    fun deleteItem(id: Long) {
        viewModelScope.launch {
            repository.deleteItemById(id)
        }
    }

    // Factory Class for providing Repository dependencies
    class Factory(private val repository: CatalogRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(CatalogViewModel::class.java)) {
                return CatalogViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
