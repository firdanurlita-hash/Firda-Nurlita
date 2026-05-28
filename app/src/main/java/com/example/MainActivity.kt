package com.example

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.AppDatabase
import com.example.data.CatalogItem
import com.example.data.CatalogRepository
import com.example.data.CatalogSettings
import com.example.ui.CatalogViewModel
import com.example.ui.theme.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Room database and repository setup
        val database = AppDatabase.getDatabase(this)
        val repository = CatalogRepository(database.catalogDao())
        val factory = CatalogViewModel.Factory(repository)

        setContent {
            MyApplicationTheme {
                // Main ViewModel initialization
                val vm: CatalogViewModel = viewModel(factory = factory)
                CatalogScreen(viewModel = vm)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogScreen(viewModel: CatalogViewModel) {
    val context = LocalContext.current

    // State bindings
    val settings by viewModel.settingsFlow.collectAsStateWithLifecycle()
    val allItemsState by viewModel.allItems.collectAsStateWithLifecycle()
    val filteredItems by viewModel.filteredItems.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val isAdmin by viewModel.isAdmin.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()

    // Dialog trigger states
    var showSetupDialog by remember { mutableStateOf(false) }
    var showLoginDialog by remember { mutableStateOf(false) }
    var showItemFormDialog by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<CatalogItem?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var itemToDeleteId by remember { mutableStateOf<Long?>(null) }

    val isInitialLoading by viewModel.isInitialLoading.collectAsStateWithLifecycle()

    // Auto trigger SetupDialog if store settings config isn't populated yet and initial load is complete
    LaunchedEffect(settings, isInitialLoading) {
        if (!isInitialLoading && settings == null) {
            showSetupDialog = true
        } else {
            showSetupDialog = false
        }
    }

    if (isInitialLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Large Glowing Brand Logo Matching WhatsApp theme
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(WhatsAppPrimary, WhatsAppAccent)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Storefront,
                        contentDescription = "Loading Logo",
                        tint = Color.White,
                        modifier = Modifier.size(44.dp)
                    )
                }

                Text(
                    text = "Menghubungkan ke Awan...",
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Text(
                    text = "Mensinkronkan data katalog terbaru...",
                    fontSize = 13.sp,
                    color = TextSlateLight
                )

                Spacer(modifier = Modifier.height(8.dp))

                CircularProgressIndicator(
                    color = WhatsAppAccent,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
    } else {
        Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("catalog_root_scaffold"),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Icon imitating a secure, modern WhatsApp Store Logo
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(WhatsAppPrimary, WhatsAppAccent)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Storefront,
                                contentDescription = "Store Logo",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = settings?.appName ?: "Katalog Toko",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(WhatsAppAccent)
                                )
                                Text(
                                    text = "Terhubung WhatsApp",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextSlateLight
                                )
                            }
                        }
                    }
                },
                actions = {
                    // Admin mode indicator tag
                    if (isAdmin) {
                        Surface(
                            shape = CircleShape,
                            color = SoftGold,
                            border = ButtonDefaults.outlinedButtonBorder(true),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SupervisorAccount,
                                    contentDescription = "Admin Mode Active",
                                    tint = PremiumGold,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Admin",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = PremiumGold
                                )
                            }
                        }
                    }

                    // Cloud Sync icon button
                    IconButton(
                        onClick = {
                            viewModel.triggerCloudSync()
                            Toast.makeText(context, "Menghubungkan & Mensinkronkan Katalog...", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .testTag("sync_cloud_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Sync Cloud Directory",
                            tint = if (isSyncing) WhatsAppAccent else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // LogIn/LogOut button triggers
                    Button(
                        onClick = {
                            if (isAdmin) {
                                viewModel.logoutAdmin()
                                Toast.makeText(context, "Keluar dari Mode Admin", Toast.LENGTH_SHORT).show()
                            } else {
                                showLoginDialog = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isAdmin) AlertRed else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (isAdmin) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .testTag("admin_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (isAdmin) Icons.Default.LockOpen else Icons.Default.Lock,
                            contentDescription = "Admin Security Toggle",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isAdmin) "Keluar" else "Login Admin",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // MAIN SCROLL CONTENT
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 280.dp),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .testTag("catalog_grid")
            ) {
                // HERO BANNER MODULE
                item(span = { GridItemSpan(maxLineSpan) }) {
                    HeroBanner(
                        settings = settings,
                        searchQuery = searchQuery,
                        selectedCategory = selectedCategory,
                        categories = categories,
                        onSearchChange = { viewModel.setSearchQuery(it) },
                        onCategorySelect = { viewModel.setSelectedCategory(it) },
                        isAdmin = isAdmin
                    )
                }

                // DYNAMIC CATAGORY DESCRIPTOR
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Daftar Barang (${filteredItems.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        if (isAdmin) {
                            Button(
                                onClick = {
                                    editingItem = null
                                    showItemFormDialog = true
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = WhatsAppPrimary),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                modifier = Modifier.testTag("btn_add_item")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add Item",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Tambah Barang", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // EMPTY CATAGORY OR PRODUCTS LISTING
                if (filteredItems.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        EmptyCatalogState(
                            hasItemsAtAll = allItemsState.isNotEmpty(),
                            isAdmin = isAdmin,
                            onAdminLoginClick = { showLoginDialog = true },
                            onAddItemClick = {
                                editingItem = null
                                showItemFormDialog = true
                            }
                        )
                    }
                } else {
                    // DYNAMIC ITEMS COLLECTION RENDERING
                    items(filteredItems, key = { it.id }) { item ->
                        CatalogItemCard(
                            item = item,
                            isAdmin = isAdmin,
                            onEditClick = {
                                editingItem = item
                                showItemFormDialog = true
                            },
                            onDeleteClick = {
                                itemToDeleteId = item.id
                                showDeleteConfirmDialog = true
                            },
                            onSendWhatsAppClick = {
                                val destinationNum = settings?.whatsappNumber ?: ""
                                if (destinationNum.isNotEmpty()) {
                                    try {
                                        val url = "https://wa.me/$destinationNum?text=${Uri.encode(item.message)}"
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Aplikasi browser atau WhatsApp tidak ditemukan", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    Toast.makeText(context, "Nomor WhatsApp belum dikonfigurasi!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                }
            }

            // STATIC POLISHED SYSTEM FOOTER (Indonesian language, matching template guidelines)
            CatalogFooter(settings = settings, onAdminManagerClick = {
                if (isAdmin) {
                    viewModel.logoutAdmin()
                    Toast.makeText(context, "Keluar dari Mode Admin", Toast.LENGTH_SHORT).show()
                } else {
                    showLoginDialog = true
                }
            })
        }
    }
    }

    // --- OVERLAYS AND INTERACTIVE DIALOG MODALS ---

    // 1. Setup Wizard Dialog (First-run configuring shop settings)
    if (showSetupDialog) {
        if (isSyncing) {
            Dialog(
                onDismissRequest = { /* Prevent dismiss */ },
                properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
            ) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(
                            color = WhatsAppPrimary,
                            modifier = Modifier.size(44.dp)
                        )
                        Text(
                            text = "Menghubungkan ke Awan...",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Memeriksa apakah toko/katalog sudah terdaftar di server cloud...",
                            fontSize = 12.sp,
                            color = TextSlateLight,
                            textAlign = TextAlign.Center,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        } else {
            SetupWizardDialog(
                onSave = { name, wa, pin, projectId ->
                    viewModel.saveSetup(name, wa, pin, projectId)
                    showSetupDialog = false
                }
            )
        }
    }

    // 2. Admin Login Security PIN Dialog
    if (showLoginDialog) {
        AdminLoginDialog(
            onDismiss = { showLoginDialog = false },
            onVerify = { pin ->
                val success = viewModel.verifyAdminPin(pin)
                if (success) {
                    showLoginDialog = false
                    Toast.makeText(context, "Login Admin Berhasil!", Toast.LENGTH_SHORT).show()
                }
                success
            }
        )
    }

    // 3. Add/Edit Item Detail Form Dialog
    if (showItemFormDialog) {
        ItemFormDialog(
            item = editingItem,
            onDismiss = { showItemFormDialog = false },
            onSave = { title, category, price, image, desc, message ->
                viewModel.saveItem(
                    id = editingItem?.id ?: 0L,
                    title = title,
                    category = category,
                    price = price,
                    image = image,
                    desc = desc,
                    message = message
                )
                showItemFormDialog = false
                Toast.makeText(
                    context,
                    if (editingItem == null) "Barang Baru Berhasil Ditambahkan!" else "Detail Barang Berhasil Diperbarui!",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )
    }

    // 4. Delete Item Confirmation Dialog
    if (showDeleteConfirmDialog) {
        DeleteConfirmDialog(
            onDismiss = { showDeleteConfirmDialog = false },
            onConfirm = {
                itemToDeleteId?.let { id ->
                    viewModel.deleteItem(id)
                    Toast.makeText(context, "Barang Berhasil Dihapus", Toast.LENGTH_SHORT).show()
                }
                showDeleteConfirmDialog = false
                itemToDeleteId = null
            }
        )
    }
}

// --- COMPOSABLE SUBCOMPONENTS ---

@Composable
fun HeroBanner(
    settings: CatalogSettings?,
    searchQuery: String,
    selectedCategory: String,
    categories: List<String>,
    onSearchChange: (String) -> Unit,
    onCategorySelect: (String) -> Unit,
    isAdmin: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("hero_banner_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = WhatsAppPrimary.copy(alpha = 0.08f)),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Elegant brand indicator tagline
            Text(
                text = settings?.appSubtitle ?: "Katalog Produk & Layanan Kami",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground,
                lineHeight = 28.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Pilih barang impian Anda di bawah ini dan lakukan pemesanan cepat secara otomatis melalui chat WhatsApp.",
                fontSize = 13.sp,
                color = TextSlateLight,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Search text input bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("Cari barang atau deskripsi...", fontSize = 13.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = TextSlateLight
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear search",
                                tint = TextSlateLight
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_text_input"),
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            // CATEGORIES FILTER - horizontal touch-friendly layout, fulfilling design guidelines
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                categories.forEach { category ->
                    val isSelected = category == selectedCategory
                    FilterChip(
                        selected = isSelected,
                        onClick = { onCategorySelect(category) },
                        label = {
                            Text(
                                text = category,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = WhatsAppPrimary,
                            selectedLabelColor = Color.White,
                            containerColor = MaterialTheme.colorScheme.surface,
                            labelColor = MaterialTheme.colorScheme.onBackground
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CatalogItemCard(
    item: CatalogItem,
    isAdmin: Boolean,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onSendWhatsAppClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("item_card_${item.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column {
                // PHOTO / PLACEHOLDER PREVIEW MODULE
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    if (item.image.isNotEmpty()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(item.image)
                                .crossfade(true)
                                .build(),
                            contentDescription = item.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        // Polished fallback illustration box
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(WhatsAppPrimary.copy(alpha = 0.08f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ShoppingBag,
                                    contentDescription = "No photo placeholder",
                                    tint = WhatsAppPrimary,
                                    modifier = Modifier.size(40.dp)
                                )
                                Text(
                                    text = "Tanpa Foto",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = WhatsAppPrimary
                                )
                            }
                        }
                    }

                    // Floating Category Badge
                    Box(
                        modifier = Modifier
                            .padding(12.dp)
                            .align(Alignment.BottomStart)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.7f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = item.category.uppercase(),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            letterSpacing = 1.sp
                        )
                    }
                }

                // DETAILS CONTENT
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = item.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.desc,
                        fontSize = 12.sp,
                        color = TextSlateLight,
                        minLines = 2,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(12.dp))

                    // PRICE AND WA ACTION ROW
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = "Harga", fontSize = 11.sp, color = TextSlateLight)
                            Text(
                                text = formatRupiah(item.price),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }

                        // WhatsApp Action Trigger
                        Button(
                            onClick = onSendWhatsAppClick,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = WhatsAppAccent),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier
                                .height(40.dp)
                                .testTag("wa_order_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Chat,
                                contentDescription = "Order on WA",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Pesan",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // ADMIN CONTROL FLOATING HOVER LAYOUT OVERLAY
            if (isAdmin) {
                Row(
                    modifier = Modifier
                        .padding(8.dp)
                        .align(Alignment.TopEnd)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.65f))
                        .padding(2.dp)
                ) {
                    IconButton(
                        onClick = onEditClick,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("btn_edit_${item.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Item Details",
                            tint = Color.Yellow,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("btn_delete_${item.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete item",
                            tint = Color.Red,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyCatalogState(
    hasItemsAtAll: Boolean,
    isAdmin: Boolean,
    onAdminLoginClick: () -> Unit,
    onAddItemClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp)
            .testTag("empty_state_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.FolderOpen,
                    contentDescription = "Empty Catalog Icon",
                    tint = TextSlateLight,
                    modifier = Modifier.size(36.dp)
                )
            }

            Text(
                text = "Katalog Masih Kosong",
                fontWeight = FontWeight.Black,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Text(
                text = if (hasItemsAtAll) {
                    "Tidak ada barang yang cocok dengan kata pencarian atau pilihan kategori Anda saat ini."
                } else {
                    "Belum ada barang terdaftar di toko ini. Bila Anda adalah pengurus toko, login sekarang untuk mengunggah barang produk."
                },
                fontSize = 13.sp,
                color = TextSlateLight,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp),
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            if (!hasItemsAtAll && !isAdmin) {
                Button(
                    onClick = onAdminLoginClick,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = WhatsAppPrimary)
                ) {
                    Icon(imageVector = Icons.Default.Lock, contentDescription = "Admin login")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Login Admin Toko", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            } else if (isAdmin) {
                Button(
                    onClick = onAddItemClick,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = WhatsAppAccent)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Product", tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Tambah Barang Baru", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun CatalogFooter(
    settings: CatalogSettings?,
    onAdminManagerClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("catalog_footer_card"),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 0.dp, bottomEnd = 0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "© 2026 ${settings?.appName ?: "Katalog Toko"}. Semua Hak Cipta Dilindungi.",
                fontSize = 12.sp,
                color = TextSlateLight,
                textAlign = TextAlign.Center
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.VerifiedUser,
                        contentDescription = "Shield Verified Icon",
                        tint = WhatsAppAccent,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Sistem PIN Terenkripsi",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = WhatsAppPrimary
                    )
                }

                Text(text = "|", color = MaterialTheme.colorScheme.outlineVariant)

                Text(
                    text = "Kelola Toko",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = WhatsAppPrimary,
                    modifier = Modifier
                        .clickable { onAdminManagerClick() }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

// --- SECURE SETUP DIALOG WIZARD ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupWizardDialog(
    onSave: (name: String, wa: String, pin: String, projectId: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var wa by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("1029384756") }
    var projectId by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = { /* Force response to set up application */ },
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("modal_setup_wizard"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(WhatsAppPrimary.copy(alpha = 0.08f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Setup Icon",
                            tint = WhatsAppPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Text(
                        text = "Selamat Datang!",
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Konfigurasi awal katalog WhatsApp Anda dalam 1 menit.",
                        fontSize = 11.sp,
                        color = TextSlateLight,
                        textAlign = TextAlign.Center
                    )
                }

                // Inputs
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Toko / Katalog", fontSize = 12.sp) },
                    placeholder = { Text("Contoh: Kedai Hijau", fontSize = 12.sp) },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("setup_name_input")
                )

                OutlinedTextField(
                    value = wa,
                    onValueChange = { wa = it },
                    label = { Text("Nomor WhatsApp Penerima", fontSize = 12.sp) },
                    placeholder = { Text("81315239321 (Tanpa angka 0 di depan)", fontSize = 11.sp) },
                    prefix = { Text("+62 ", fontWeight = FontWeight.Bold) },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("setup_wa_input")
                )

                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = it },
                    label = { Text("PIN Keamanan Admin", fontSize = 12.sp) },
                    placeholder = { Text("Minimal 4 angka / karakter", fontSize = 11.sp) },
                    visualTransformation = PasswordVisualTransformation(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("setup_pin_input")
                )

                OutlinedTextField(
                    value = projectId,
                    onValueChange = { projectId = it },
                    label = { Text("Firestore Project ID (Opsional untuk Sync Awan)", fontSize = 12.sp) },
                    placeholder = { Text("Kosongkan untuk server cloud bersama default", fontSize = 11.sp) },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("setup_project_id_input")
                )

                Button(
                    onClick = {
                        if (name.isNotEmpty() && wa.isNotEmpty() && pin.length >= 4) {
                            onSave(name, wa, pin, projectId)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("setup_submit_button"),
                    enabled = name.trim().isNotEmpty() && wa.trim().isNotEmpty() && pin.length >= 4,
                    colors = ButtonDefaults.buttonColors(containerColor = WhatsAppPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Buat Katalog Sekarang", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next")
                }
            }
        }
    }
}

// --- ADMIN LOGIN PIN OVERLAY DIALOG ---

@Composable
fun AdminLoginDialog(
    onDismiss: () -> Unit,
    onVerify: (pin: String) -> Boolean
) {
    var pin by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("modal_login_pin"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Security PIN Icon",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Text(
                        text = "Keamanan Admin",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Masukkan PIN rahasia untuk masuk ke mode admin pengedit katalog.",
                        fontSize = 11.sp,
                        color = TextSlateLight,
                        textAlign = TextAlign.Center
                    )
                }

                OutlinedTextField(
                    value = pin,
                    onValueChange = {
                        pin = it
                        isError = false
                    },
                    label = { Text("PIN Rahasia", fontSize = 12.sp) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    shape = RoundedCornerShape(12.dp),
                    isError = isError,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("login_pin_input")
                )

                if (isError) {
                    Text(
                        text = "PIN Salah! Silakan coba lagi.",
                        style = MaterialTheme.typography.labelSmall,
                        color = AlertRed,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("login_error_text"),
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Batal")
                    }

                    Button(
                        onClick = {
                            val verified = onVerify(pin)
                            if (!verified) {
                                isError = true
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = WhatsAppPrimary),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("login_submit_btn")
                    ) {
                        Text("Verifikasi")
                    }
                }
            }
        }
    }
}

// --- ITEM CONFIG / MODIFICATION FORM DIALOG ---

@Composable
fun ItemFormDialog(
    item: CatalogItem?,
    onDismiss: () -> Unit,
    onSave: (title: String, category: String, price: String, image: String, desc: String, message: String) -> Unit
) {
    var title by remember { mutableStateOf(item?.title ?: "") }
    var category by remember { mutableStateOf(item?.category ?: "") }
    var price by remember { mutableStateOf(item?.price ?: "") }
    var image by remember { mutableStateOf(item?.image ?: "") }
    var desc by remember { mutableStateOf(item?.desc ?: "") }
    var message by remember { mutableStateOf(item?.message ?: "") }

    // Logic dynamically pre-populating customized WhatsApp messages matching HTML wizard behavior
    fun autoGenerateWAMessage() {
        if (title.isNotEmpty()) {
            val formattedPrice = formatRupiah(price)
            message = "Halo Kak, saya tertarik membeli barang berikut:\n\n*Produk:* $title\n*Harga:* $formattedPrice\n\nApakah stok masih tersedia saat ini? Terima kasih."
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .testTag("modal_item_details"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = if (item == null) "Tambah Barang Baru" else "Edit Detail Barang",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Input fields inside a beautiful responsive structured list
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        autoGenerateWAMessage()
                    },
                    label = { Text("Nama Barang *") },
                    placeholder = { Text("Contoh: Sepatu Running Pro") },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("item_title_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Kategori *") },
                        placeholder = { Text("Contoh: Sepatu") },
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("item_category_input")
                    )

                    OutlinedTextField(
                        value = price,
                        onValueChange = {
                            price = it
                            autoGenerateWAMessage()
                        },
                        label = { Text("Harga *") },
                        placeholder = { Text("Contoh: 150000") },
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("item_price_input")
                    )
                }

                OutlinedTextField(
                    value = image,
                    onValueChange = { image = it },
                    label = { Text("Tautan Foto / Gambar URL (Opsional)") },
                    placeholder = { Text("https://...") },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("item_image_input")
                )

                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Deskripsi Singkat *") },
                    placeholder = { Text("Tulis spesifikasi, ukuran, atau detail produk...") },
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("item_desc_input")
                )

                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Template Pesan WhatsApp *") },
                    placeholder = { Text("Pesan otomatis chat saat pembeli menekan tombol pesan...") },
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 4,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("item_message_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Batal")
                    }

                    Button(
                        onClick = {
                            if (title.isNotEmpty() && category.isNotEmpty() && price.isNotEmpty() && desc.isNotEmpty() && message.isNotEmpty()) {
                                onSave(title, category, price, image, desc, message)
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = WhatsAppPrimary),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("item_save_button"),
                        enabled = title.isNotEmpty() && category.isNotEmpty() && price.isNotEmpty() && desc.isNotEmpty() && message.isNotEmpty()
                    ) {
                        Text("Simpan")
                    }
                }
            }
        }
    }
}

// --- SECURE DELETION CONFIRMATION DIALOG ---

@Composable
fun DeleteConfirmDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("modal_delete_confirm"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(SoftRed),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Confirm Delete Icon",
                        tint = AlertRed,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Text(
                    text = "Hapus Barang?",
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "Tindakan ini tidak dapat dibatalkan. Barang yang dihapus tidak akan tampil di katalog lagi.",
                    fontSize = 11.sp,
                    color = TextSlateLight,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Batal")
                    }

                    Button(
                        onClick = onConfirm,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AlertRed),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("confirm_delete_btn")
                    ) {
                        Text("Ya, Hapus", color = Color.White)
                    }
                }
            }
        }
    }
}

// --- CURRENCY HELPER FUNCTION (formatting price entries intelligently) ---
private fun formatRupiah(price: String): String {
    val digits = price.filter { it.isDigit() }
    if (digits.isEmpty()) return price
    return try {
        val parsed = digits.toLong()
        val format = java.text.NumberFormat.getCurrencyInstance(java.util.Locale.forLanguageTag("id-ID"))
        format.minimumFractionDigits = 0
        val result = format.format(parsed)
        // Clean currency output formatting syntax representing custom IDR rupiah standard
        result.replace("Rp", "Rp ").replace(",00", "")
    } catch (e: Exception) {
        price
    }
}
