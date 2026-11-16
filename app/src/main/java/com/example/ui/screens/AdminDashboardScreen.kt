package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AdminRole
import com.example.data.model.Announcement
import com.example.data.model.Banner
import com.example.data.model.Category
import com.example.data.model.OrderEnquiry
import com.example.data.model.Product
import com.example.data.model.Review
import com.example.data.model.UserProfile
import com.example.data.model.WebsiteSettings
import com.example.ui.components.BoutiqueImage
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.StatusPill
import com.example.ui.components.encodeUriToCompressedDataUri
import com.example.ui.components.formatInr
import com.example.ui.components.orderStatusColor
import com.example.ui.theme.BlushPink
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.CreamBackground
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.DeepRose
import com.example.ui.theme.EmeraldSage
import com.example.ui.theme.HairlineRose
import com.example.ui.theme.MutedTaupe
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SoftBlushSurface
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminDashboardScreen(
    products: List<Product>,
    categories: List<Category>,
    orders: List<OrderEnquiry>,
    users: List<UserProfile>,
    announcements: List<Announcement>,
    banners: List<Banner>,
    reviews: List<Review>,
    adminRoles: List<AdminRole>,
    settings: WebsiteSettings,
    onBack: () -> Unit,
    onSeedStorefront: () -> Unit,
    onSaveProduct: (Product) -> Unit,
    onDeleteProduct: (String) -> Unit,
    onSaveCategory: (Category) -> Unit,
    onSwapCategoryOrder: (Category, Category) -> Unit,
    onDeleteCategory: (String) -> Unit,
    onUpdateOrder: (orderId: String, status: String, channel: String, notes: String) -> Unit,
    onSaveAnnouncement: (Announcement) -> Unit,
    onDeleteAnnouncement: (String) -> Unit,
    onSaveBanner: (Banner) -> Unit,
    onDeleteBanner: (String) -> Unit,
    onSaveSettings: (WebsiteSettings) -> Unit,
    onToggleUserBlocked: (UserProfile) -> Unit,
    onToggleFeaturedReview: (Review) -> Unit,
    onDeleteReview: (String) -> Unit,
    onAddAdmin: (uid: String, email: String) -> Unit,
    onRemoveAdmin: (uid: String) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Overview", "Products", "Categories", "Orders", "Customize", "Users")

    var editingProduct by remember { mutableStateOf<Product?>(null) }
    var showProductDialog by remember { mutableStateOf(false) }

    var editingCategory by remember { mutableStateOf<Category?>(null) }
    var showCategoryDialog by remember { mutableStateOf(false) }

    var editingAnnouncement by remember { mutableStateOf<Announcement?>(null) }
    var showAnnouncementDialog by remember { mutableStateOf(false) }

    var editingBanner by remember { mutableStateOf<Banner?>(null) }
    var showBannerDialog by remember { mutableStateOf(false) }

    var editingOrder by remember { mutableStateOf<OrderEnquiry?>(null) }

    Scaffold(
        containerColor = CreamBackground,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CreamBackground)
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("admin_back_button")) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back to Store")
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("StitchByHer Admin Studio", style = MaterialTheme.typography.titleLarge)
                        Text("Live Storefront & Order Control", style = MaterialTheme.typography.labelSmall, color = DeepRose)
                    }
                }
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = CreamBackground,
                    contentColor = ObsidianBlack,
                    edgePadding = 12.dp
                ) {
                    tabs.forEachIndexed { idx, title ->
                        Tab(
                            selected = selectedTab == idx,
                            onClick = { selectedTab = idx },
                            text = { Text(title, style = MaterialTheme.typography.labelLarge) }
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (selectedTab in listOf(1, 2)) {
                FloatingActionButton(
                    onClick = {
                        when (selectedTab) {
                            1 -> {
                                editingProduct = null
                                showProductDialog = true
                            }
                            2 -> {
                                editingCategory = null
                                showCategoryDialog = true
                            }
                        }
                    },
                    containerColor = ObsidianBlack,
                    contentColor = BlushPink,
                    modifier = Modifier.navigationBarsPadding()
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Add")
                }
            }
        }
    ) { innerPadding ->
        when (selectedTab) {
            // 0. OVERVIEW
            0 -> {
                val enquiriesCount = orders.count { it.channel == "WhatsApp Enquiry" }
                val confirmedCount = orders.count { it.channel == "Confirmed Order" }
                val lowStockProducts = products.filter { it.stock <= 5L }
                val bestSellers = products.filter { it.isBestSeller }.take(4)

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            KpiCard("Products", products.size.toString(), Modifier.weight(1f))
                            KpiCard("Users", users.size.toString(), Modifier.weight(1f))
                        }
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            KpiCard("Categories", categories.size.toString(), Modifier.weight(1f))
                            KpiCard("Orders", orders.size.toString(), Modifier.weight(1f))
                        }
                    }
                    item {
                        LiquidGlassCard(cornerRadius = 20.dp, alpha = 0.88f) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("Order Channel Breakdown", style = MaterialTheme.typography.titleMedium)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("WhatsApp Enquiries: $enquiriesCount", style = MaterialTheme.typography.bodyMedium, color = DeepRose)
                                    Text("Confirmed Orders: $confirmedCount", style = MaterialTheme.typography.bodyMedium, color = EmeraldSage)
                                }
                                HorizontalDivider(color = HairlineRose)
                                OutlinedButton(
                                    onClick = onSeedStorefront,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Initialize Starter Categories & Pieces (If Empty)")
                                }
                            }
                        }
                    }
                    if (bestSellers.isNotEmpty()) {
                        item {
                            LiquidGlassCard(cornerRadius = 20.dp, alpha = 0.86f) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("Best-Selling Products", style = MaterialTheme.typography.titleMedium)
                                    bestSellers.forEach { p ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(p.name, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                                            Text("${formatInr(p.price)} • Stock ${p.stock}", style = MaterialTheme.typography.labelMedium, color = DeepRose)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    item {
                        LiquidGlassCard(cornerRadius = 20.dp, alpha = 0.86f) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("Inventory Overview (${lowStockProducts.size} low stock)", style = MaterialTheme.typography.titleMedium)
                                if (lowStockProducts.isEmpty()) {
                                    Text("All published creations have healthy stock.", style = MaterialTheme.typography.bodySmall, color = MutedTaupe)
                                } else {
                                    lowStockProducts.forEach { p ->
                                        Text("• ${p.name} (${p.stock} left)", style = MaterialTheme.typography.bodySmall, color = CrimsonError)
                                    }
                                }
                            }
                        }
                    }
                    item {
                        Text("Recent Order Activity", style = MaterialTheme.typography.titleLarge)
                    }
                    items(orders.take(5), key = { it.id }) { ord ->
                        LiquidGlassCard(cornerRadius = 16.dp, alpha = 0.86f, onClick = { editingOrder = ord }) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(ord.customerName, style = MaterialTheme.typography.titleSmall)
                                    Text("${ord.productName} × ${ord.quantity}", style = MaterialTheme.typography.bodySmall, color = MutedTaupe)
                                }
                                StatusPill(text = ord.status, color = orderStatusColor(ord.status))
                            }
                        }
                    }
                }
            }

            // 1. PRODUCTS
            1 -> {
                var productSearch by remember { mutableStateOf("") }
                val shownProducts = products.filter {
                    productSearch.isBlank() || it.name.contains(productSearch, ignoreCase = true)
                }
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        OutlinedTextField(
                            value = productSearch,
                            onValueChange = { productSearch = it },
                            label = { Text("Search products") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    items(shownProducts, key = { it.id }) { product ->
                        LiquidGlassCard(cornerRadius = 18.dp, alpha = 0.86f) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                BoutiqueImage(
                                    source = product.imageUrls.firstOrNull(),
                                    contentDescription = product.name,
                                    modifier = Modifier
                                        .size(62.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(product.name, style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        "${product.categoryName} • ${formatInr(product.price)} • Stock ${product.stock}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MutedTaupe
                                    )
                                    if (!product.isPublished) {
                                        StatusPill(text = "Hidden Draft", color = CrimsonError)
                                    }
                                }
                                IconButton(onClick = {
                                    editingProduct = product
                                    showProductDialog = true
                                }) {
                                    Icon(Icons.Rounded.Edit, contentDescription = "Edit product")
                                }
                                IconButton(onClick = { onDeleteProduct(product.id) }) {
                                    Icon(Icons.Rounded.Delete, contentDescription = "Delete product", tint = CrimsonError)
                                }
                            }
                        }
                    }
                }
            }

            // 2. CATEGORIES
            2 -> {
                val sortedCategories = categories.sortedBy { it.sortOrder }
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(sortedCategories, key = { _, c -> c.id }) { idx, cat ->
                        LiquidGlassCard(cornerRadius = 18.dp, alpha = 0.86f) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                BoutiqueImage(
                                    source = cat.imageUrl,
                                    contentDescription = cat.name,
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(cat.name, style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        if (cat.isVisible) "Visible" else "Hidden",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (cat.isVisible) EmeraldSage else CrimsonError
                                    )
                                }
                                if (idx > 0) {
                                    IconButton(onClick = { onSwapCategoryOrder(cat, sortedCategories[idx - 1]) }) {
                                        Icon(Icons.Rounded.ArrowUpward, contentDescription = "Move up")
                                    }
                                }
                                if (idx < sortedCategories.lastIndex) {
                                    IconButton(onClick = { onSwapCategoryOrder(cat, sortedCategories[idx + 1]) }) {
                                        Icon(Icons.Rounded.ArrowDownward, contentDescription = "Move down")
                                    }
                                }
                                IconButton(onClick = {
                                    editingCategory = cat
                                    showCategoryDialog = true
                                }) {
                                    Icon(Icons.Rounded.Edit, contentDescription = "Edit category")
                                }
                                IconButton(onClick = { onDeleteCategory(cat.id) }) {
                                    Icon(Icons.Rounded.Delete, contentDescription = "Delete category", tint = CrimsonError)
                                }
                            }
                        }
                    }
                }
            }

            // 3. ORDERS
            3 -> {
                var orderSearch by remember { mutableStateOf("") }
                var channelFilter by remember { mutableStateOf("All") }
                val filteredOrders = orders.filter { ord ->
                    val matchesChannel = channelFilter == "All" || ord.channel == channelFilter
                    val matchesSearch = orderSearch.isBlank() ||
                        ord.customerName.contains(orderSearch, ignoreCase = true) ||
                        ord.productName.contains(orderSearch, ignoreCase = true) ||
                        ord.customerPhone.contains(orderSearch, ignoreCase = true)
                    matchesChannel && matchesSearch
                }
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        OutlinedTextField(
                            value = orderSearch,
                            onValueChange = { orderSearch = it },
                            label = { Text("Search customer, phone, or product") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(listOf("All", "WhatsApp Enquiry", "Confirmed Order")) { filter ->
                                FilterChip(
                                    selected = channelFilter == filter,
                                    onClick = { channelFilter = filter },
                                    label = { Text(filter) }
                                )
                            }
                        }
                    }
                    items(filteredOrders, key = { it.id }) { ord ->
                        LiquidGlassCard(cornerRadius = 18.dp, alpha = 0.88f, onClick = { editingOrder = ord }) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    StatusPill(
                                        text = ord.channel,
                                        color = if (ord.channel == "Confirmed Order") EmeraldSage else DeepRose
                                    )
                                    StatusPill(text = ord.status, color = orderStatusColor(ord.status))
                                }
                                Text("${ord.productName} × ${ord.quantity} (${ord.selectedSize}, ${ord.selectedColor})", style = MaterialTheme.typography.titleSmall)
                                Text("${ord.customerName} • ${ord.customerPhone}", style = MaterialTheme.typography.bodySmall)
                                Text(ord.deliveryAddress, style = MaterialTheme.typography.bodySmall, color = MutedTaupe)
                            }
                        }
                    }
                }
            }

            // 4. COMPLETE WEBSITE CUSTOMIZATION (Brand, Colors, Hero, Sections, Announcements, Banners, Policies, Reviews)
            4 -> {
                val context = LocalContext.current
                val scope = rememberCoroutineScope()
                var draft by remember(settings) { mutableStateOf(settings) }
                val heroPicker = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.PickVisualMedia()
                ) { uri ->
                    if (uri != null) {
                        scope.launch {
                            encodeUriToCompressedDataUri(context, uri)?.let {
                                draft = draft.copy(heroImageUrl = it)
                            }
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Brand Identity & Colors
                    item {
                        LiquidGlassCard(cornerRadius = 20.dp, alpha = 0.88f) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text("Brand & Theme Customization", style = MaterialTheme.typography.titleLarge)
                                OutlinedTextField(value = draft.brandName, onValueChange = { draft = draft.copy(brandName = it) }, label = { Text("Brand Name") }, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(value = draft.tagline, onValueChange = { draft = draft.copy(tagline = it) }, label = { Text("Tagline") }, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(value = draft.whatsappNumber, onValueChange = { draft = draft.copy(whatsappNumber = it) }, label = { Text("WhatsApp Order Number") }, modifier = Modifier.fillMaxWidth())

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(value = draft.primaryColorHex, onValueChange = { draft = draft.copy(primaryColorHex = it) }, label = { Text("Primary Blush Hex") }, modifier = Modifier.weight(1f))
                                    OutlinedTextField(value = draft.accentColorHex, onValueChange = { draft = draft.copy(accentColorHex = it) }, label = { Text("Accent Rose Hex") }, modifier = Modifier.weight(1f))
                                }

                                // Hero Settings
                                OutlinedTextField(value = draft.heroTitle, onValueChange = { draft = draft.copy(heroTitle = it) }, label = { Text("Hero Heading") }, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(value = draft.heroSubtitle, onValueChange = { draft = draft.copy(heroSubtitle = it) }, label = { Text("Hero Description") }, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(value = draft.heroCtaText, onValueChange = { draft = draft.copy(heroCtaText = it) }, label = { Text("Hero Button Text") }, modifier = Modifier.fillMaxWidth())
                                OutlinedButton(onClick = {
                                    heroPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                }) {
                                    Text("Change Hero Banner Image")
                                }

                                // About, Contact, Footer & Policies
                                OutlinedTextField(value = draft.aboutTitle, onValueChange = { draft = draft.copy(aboutTitle = it) }, label = { Text("About Heading") }, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(value = draft.aboutDescription, onValueChange = { draft = draft.copy(aboutDescription = it) }, label = { Text("About Us Description") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(value = draft.contactPhone, onValueChange = { draft = draft.copy(contactPhone = it) }, label = { Text("Contact Phone") }, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(value = draft.contactEmail, onValueChange = { draft = draft.copy(contactEmail = it) }, label = { Text("Contact Email") }, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(value = draft.contactAddress, onValueChange = { draft = draft.copy(contactAddress = it) }, label = { Text("Boutique Address") }, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(value = draft.instagramHandle, onValueChange = { draft = draft.copy(instagramHandle = it) }, label = { Text("Instagram Handle") }, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(value = draft.footerText, onValueChange = { draft = draft.copy(footerText = it) }, label = { Text("Footer Copy") }, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(value = draft.shippingPolicy, onValueChange = { draft = draft.copy(shippingPolicy = it) }, label = { Text("Shipping Policy") }, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(value = draft.returnPolicy, onValueChange = { draft = draft.copy(returnPolicy = it) }, label = { Text("Return & Fit Policy") }, modifier = Modifier.fillMaxWidth())

                                AdminSwitchRow("Show Announcement Bar", draft.showAnnouncements) { draft = draft.copy(showAnnouncements = it) }
                                AdminSwitchRow("Show Patron Reviews", draft.showReviews) { draft = draft.copy(showReviews = it) }
                                AdminSwitchRow("Show About Section", draft.showAbout) { draft = draft.copy(showAbout = it) }
                                AdminSwitchRow("Show Contact & Footer", draft.showContact) { draft = draft.copy(showContact = it) }

                                Button(
                                    onClick = { onSaveSettings(draft) },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = ObsidianBlack, contentColor = BlushPink),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .testTag("save_website_settings_button")
                                ) {
                                    Text("Publish Storefront Customization")
                                }
                            }
                        }
                    }

                    // Show/Hide & Reorder Homepage Sections
                    item {
                        LiquidGlassCard(cornerRadius = 20.dp, alpha = 0.88f) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("Homepage Section Order & Visibility", style = MaterialTheme.typography.titleLarge)
                                val currentOrder = draft.sectionOrder.ifEmpty { WebsiteSettings.DEFAULT_SECTION_ORDER }
                                currentOrder.forEachIndexed { idx, key ->
                                    val isHidden = key in draft.hiddenSections
                                    Surface(
                                        color = PureWhite.copy(alpha = 0.7f),
                                        border = BorderStroke(0.8.dp, HairlineRose),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = key.replace("_", " ").replaceFirstChar { it.uppercase() },
                                                style = MaterialTheme.typography.bodyMedium,
                                                modifier = Modifier.weight(1f)
                                            )
                                            if (idx > 0) {
                                                IconButton(onClick = {
                                                    val mutable = currentOrder.toMutableList()
                                                    val tmp = mutable[idx - 1]
                                                    mutable[idx - 1] = mutable[idx]
                                                    mutable[idx] = tmp
                                                    draft = draft.copy(sectionOrder = mutable)
                                                    onSaveSettings(draft)
                                                }) {
                                                    Icon(Icons.Rounded.ArrowUpward, contentDescription = "Move up")
                                                }
                                            }
                                            if (idx < currentOrder.lastIndex) {
                                                IconButton(onClick = {
                                                    val mutable = currentOrder.toMutableList()
                                                    val tmp = mutable[idx + 1]
                                                    mutable[idx + 1] = mutable[idx]
                                                    mutable[idx] = tmp
                                                    draft = draft.copy(sectionOrder = mutable)
                                                    onSaveSettings(draft)
                                                }) {
                                                    Icon(Icons.Rounded.ArrowDownward, contentDescription = "Move down")
                                                }
                                            }
                                            Switch(
                                                checked = !isHidden,
                                                onCheckedChange = { visible ->
                                                    val nextHidden = if (visible) {
                                                        draft.hiddenSections - key
                                                    } else {
                                                        (draft.hiddenSections + key).distinct()
                                                    }
                                                    draft = draft.copy(hiddenSections = nextHidden)
                                                    onSaveSettings(draft)
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Announcements Management
                    item {
                        LiquidGlassCard(cornerRadius = 20.dp, alpha = 0.88f) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Announcements (${announcements.size})", style = MaterialTheme.typography.titleLarge)
                                    Button(
                                        onClick = {
                                            editingAnnouncement = null
                                            showAnnouncementDialog = true
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = ObsidianBlack)
                                    ) {
                                        Text("+ Add")
                                    }
                                }
                                announcements.sortedBy { it.sortOrder }.forEach { ann ->
                                    Surface(
                                        color = PureWhite.copy(alpha = 0.7f),
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text("${ann.title} (${if (ann.isActive) "Active" else "Paused"})", style = MaterialTheme.typography.titleSmall)
                                                Text(ann.message, style = MaterialTheme.typography.bodySmall, color = MutedTaupe)
                                            }
                                            IconButton(onClick = {
                                                editingAnnouncement = ann
                                                showAnnouncementDialog = true
                                            }) {
                                                Icon(Icons.Rounded.Edit, contentDescription = "Edit announcement")
                                            }
                                            IconButton(onClick = { onDeleteAnnouncement(ann.id) }) {
                                                Icon(Icons.Rounded.Delete, contentDescription = "Delete announcement", tint = CrimsonError)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Promo Banners Management
                    item {
                        LiquidGlassCard(cornerRadius = 20.dp, alpha = 0.88f) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Promo Banners (${banners.size})", style = MaterialTheme.typography.titleLarge)
                                    Button(
                                        onClick = {
                                            editingBanner = null
                                            showBannerDialog = true
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = ObsidianBlack)
                                    ) {
                                        Text("+ Add")
                                    }
                                }
                                banners.sortedBy { it.sortOrder }.forEach { banner ->
                                    Surface(
                                        color = PureWhite.copy(alpha = 0.7f),
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(banner.title, style = MaterialTheme.typography.titleSmall)
                                                Text(banner.subtitle, style = MaterialTheme.typography.bodySmall, color = MutedTaupe)
                                            }
                                            IconButton(onClick = {
                                                editingBanner = banner
                                                showBannerDialog = true
                                            }) {
                                                Icon(Icons.Rounded.Edit, contentDescription = "Edit banner")
                                            }
                                            IconButton(onClick = { onDeleteBanner(banner.id) }) {
                                                Icon(Icons.Rounded.Delete, contentDescription = "Delete banner", tint = CrimsonError)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Customer Reviews Moderation
                    if (reviews.isNotEmpty()) {
                        item {
                            LiquidGlassCard(cornerRadius = 20.dp, alpha = 0.88f) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("Customer Reviews (${reviews.size})", style = MaterialTheme.typography.titleLarge)
                                    reviews.forEach { rev ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text("${rev.userName} • ${rev.rating}★", style = MaterialTheme.typography.titleSmall)
                                                Text(rev.comment, style = MaterialTheme.typography.bodySmall, color = MutedTaupe)
                                            }
                                            IconButton(onClick = { onToggleFeaturedReview(rev) }) {
                                                Icon(
                                                    Icons.Rounded.Star,
                                                    contentDescription = "Feature review",
                                                    tint = if (rev.isFeaturedOnHome) ChampagneGold else HairlineRose
                                                )
                                            }
                                            IconButton(onClick = { onDeleteReview(rev.id) }) {
                                                Icon(Icons.Rounded.Delete, contentDescription = "Delete review", tint = CrimsonError)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 5. USERS & ADMIN ROLES
            else -> {
                var userSearch by remember { mutableStateOf("") }
                var newAdminUid by remember { mutableStateOf("") }
                var newAdminEmail by remember { mutableStateOf("") }
                val filteredUsers = users.filter {
                    userSearch.isBlank() ||
                        it.fullName.contains(userSearch, ignoreCase = true) ||
                        it.email.contains(userSearch, ignoreCase = true)
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        OutlinedTextField(
                            value = userSearch,
                            onValueChange = { userSearch = it },
                            label = { Text("Search registered patrons") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    items(filteredUsers, key = { it.userId }) { user ->
                        val dateText = user.createdAt?.toDate()?.let {
                            SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(it)
                        } ?: "Recent"
                        LiquidGlassCard(cornerRadius = 18.dp, alpha = 0.86f) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(user.fullName, style = MaterialTheme.typography.titleSmall)
                                    Text(user.email, style = MaterialTheme.typography.bodySmall, color = MutedTaupe)
                                    Text("Joined $dateText • ${user.phoneNumber.ifBlank { "No phone" }}", style = MaterialTheme.typography.labelSmall, color = DeepRose)
                                    if (user.defaultAddress.isNotBlank()) {
                                        Text(user.defaultAddress, style = MaterialTheme.typography.labelSmall, color = MutedTaupe)
                                    }
                                }
                                OutlinedButton(
                                    onClick = { onToggleUserBlocked(user) },
                                    border = BorderStroke(1.dp, if (user.isBlocked) EmeraldSage else CrimsonError),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = if (user.isBlocked) EmeraldSage else CrimsonError
                                    )
                                ) {
                                    Text(if (user.isBlocked) "Unblock" else "Block")
                                }
                            }
                        }
                    }

                    // Admin Roles Management
                    item {
                        LiquidGlassCard(cornerRadius = 20.dp, alpha = 0.88f) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text("Authorized Admins", style = MaterialTheme.typography.titleLarge)
                                Text("Primary owner (ahxantalib@gmail.com) is bootstrapped. Add additional verified admin UIDs below.", style = MaterialTheme.typography.bodySmall, color = MutedTaupe)
                                adminRoles.forEach { role ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(role.email, style = MaterialTheme.typography.titleSmall)
                                            Text("UID: ${role.uid}", style = MaterialTheme.typography.labelSmall, color = MutedTaupe)
                                        }
                                        IconButton(onClick = { onRemoveAdmin(role.uid) }) {
                                            Icon(Icons.Rounded.Delete, contentDescription = "Revoke admin", tint = CrimsonError)
                                        }
                                    }
                                }
                                OutlinedTextField(value = newAdminUid, onValueChange = { newAdminUid = it }, label = { Text("Firebase User UID") }, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(value = newAdminEmail, onValueChange = { newAdminEmail = it }, label = { Text("Admin Email") }, modifier = Modifier.fillMaxWidth())
                                Button(
                                    onClick = {
                                        if (newAdminUid.isNotBlank() && newAdminEmail.isNotBlank()) {
                                            onAddAdmin(newAdminUid, newAdminEmail)
                                            newAdminUid = ""
                                            newAdminEmail = ""
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = ObsidianBlack),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Grant Admin Access")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showProductDialog) {
        ProductEditorDialog(
            initial = editingProduct,
            categories = categories,
            onDismiss = { showProductDialog = false },
            onSave = onSaveProduct
        )
    }
    if (showCategoryDialog) {
        CategoryEditorDialog(
            initial = editingCategory,
            nextOrder = categories.size.toLong(),
            onDismiss = { showCategoryDialog = false },
            onSave = onSaveCategory
        )
    }
    if (showAnnouncementDialog) {
        AnnouncementEditorDialog(
            initial = editingAnnouncement,
            nextOrder = announcements.size.toLong(),
            onDismiss = { showAnnouncementDialog = false },
            onSave = onSaveAnnouncement
        )
    }
    if (showBannerDialog) {
        BannerEditorDialog(
            initial = editingBanner,
            categories = categories,
            nextOrder = banners.size.toLong(),
            onDismiss = { showBannerDialog = false },
            onSave = onSaveBanner
        )
    }
    val orderToEdit = editingOrder
    if (orderToEdit != null) {
        OrderAdminDialog(
            order = orderToEdit,
            onDismiss = { editingOrder = null },
            onSave = { st, ch, notes -> onUpdateOrder(orderToEdit.id, st, ch, notes) }
        )
    }
}

@Composable
private fun KpiCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    LiquidGlassCard(modifier = modifier, cornerRadius = 18.dp, alpha = 0.88f) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MutedTaupe)
            Text(value, style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold, color = ObsidianBlack)
        }
    }
}
