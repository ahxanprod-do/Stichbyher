package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.rounded.Checkroom
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.repository.StitchByHerRepository
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.signOut
import com.example.ui.components.LiquidGlassCard
import com.example.ui.screens.AccountScreen
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ProductDetailScreen
import com.example.ui.screens.ShopCatalogScreen
import com.example.ui.theme.BlushPink
import com.example.ui.theme.CreamBackground
import com.example.ui.theme.DeepRose
import com.example.ui.theme.MutedTaupe
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.PureWhite
import com.example.ui.theme.StitchByHerTheme
import com.example.ui.viewmodel.StitchByHerViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

private object Routes {
    const val HOME = "home"
    const val SHOP = "shop"
    const val WISHLIST = "wishlist"
    const val ACCOUNT = "account"
    const val PRODUCT_DETAIL = "product/{productId}"
    const val ADMIN = "admin"

    fun productDetail(productId: String) = "product/$productId"
}

private data class BottomNavTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val MAIN_TABS = listOf(
    BottomNavTab(Routes.HOME, "Home", Icons.Rounded.Home, Icons.Outlined.Home),
    BottomNavTab(Routes.SHOP, "Shop", Icons.Rounded.Checkroom, Icons.Outlined.Checkroom),
    BottomNavTab(Routes.WISHLIST, "Wishlist", Icons.Rounded.Favorite, Icons.Outlined.FavoriteBorder),
    BottomNavTab(Routes.ACCOUNT, "Account", Icons.Rounded.Person, Icons.Outlined.Person)
)

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StitchByHerTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation(auth: FirebaseAuth = Firebase.auth) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        AuthScreen(onAuthSuccess = {})
    } else {
        key(user.uid) {
            StitchByHerApp(
                currentUserId = user.uid,
                displayName = user.displayName.orEmpty(),
                email = user.email.orEmpty(),
                photoUrl = user.photoUrl?.toString(),
                isEmailVerified = user.isEmailVerified
            )
        }
    }
}

@Composable
private fun StitchByHerApp(
    currentUserId: String,
    displayName: String,
    email: String,
    photoUrl: String?,
    isEmailVerified: Boolean
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: StitchByHerViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                StitchByHerViewModel(
                    repository = StitchByHerRepository(db, Firebase.auth),
                    currentUserId = currentUserId,
                    userDisplayName = displayName,
                    userEmail = email,
                    userPhotoUrl = photoUrl,
                    isEmailVerified = isEmailVerified
                )
            }
        }
    )

    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val products by viewModel.products.collectAsStateWithLifecycle()
    val filteredProducts by viewModel.filteredProducts.collectAsStateWithLifecycle()
    val announcements by viewModel.announcements.collectAsStateWithLifecycle()
    val banners by viewModel.banners.collectAsStateWithLifecycle()
    val reviews by viewModel.reviews.collectAsStateWithLifecycle()
    val wishlist by viewModel.wishlist.collectAsStateWithLifecycle()
    val userOrders by viewModel.userOrders.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val isAdmin by viewModel.isAdmin.collectAsStateWithLifecycle()

    val allOrders by viewModel.allOrders.collectAsStateWithLifecycle()
    val allUsers by viewModel.allUsers.collectAsStateWithLifecycle()
    val adminRoles by viewModel.adminRoles.collectAsStateWithLifecycle()

    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategoryId by viewModel.selectedCategoryId.collectAsStateWithLifecycle()
    val selectedSizeFilter by viewModel.selectedSizeFilter.collectAsStateWithLifecycle()
    val selectedColorFilter by viewModel.selectedColorFilter.collectAsStateWithLifecycle()
    val maxPriceFilter by viewModel.maxPriceFilter.collectAsStateWithLifecycle()
    val sortOption by viewModel.sortOption.collectAsStateWithLifecycle()

    val wishlistIds = remember(wishlist) { wishlist.map { it.productId }.toSet() }
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showMainNav = currentRoute in MAIN_TABS.map { it.route }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        viewModel.toastMessage.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    fun launchWhatsAppUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "Unable to open WhatsApp on this device", Toast.LENGTH_SHORT).show()
        }
    }

    StitchByHerTheme(
        primaryHex = settings.primaryColorHex,
        accentHex = settings.accentColorHex
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(CreamBackground)
        ) {
            val isWideScreen = maxWidth >= 680.dp

            Scaffold(
                containerColor = CreamBackground,
                contentWindowInsets = WindowInsets.safeDrawing,
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                bottomBar = {
                    if (showMainNav && !isWideScreen) {
                        LiquidGlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            cornerRadius = 26.dp,
                            alpha = 0.92f
                        ) {
                            NavigationBar(
                                containerColor = Color.Transparent,
                                tonalElevation = 0.dp
                            ) {
                                MAIN_TABS.forEach { tab ->
                                    val selected = currentRoute == tab.route
                                    NavigationBarItem(
                                        selected = selected,
                                        onClick = {
                                            navController.navigate(tab.route) {
                                                popUpTo(Routes.HOME) { saveState = true }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        },
                                        icon = {
                                            Icon(
                                                imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                                contentDescription = tab.label
                                            )
                                        },
                                        label = {
                                            Text(tab.label, style = MaterialTheme.typography.labelSmall)
                                        },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = ObsidianBlack,
                                            selectedTextColor = ObsidianBlack,
                                            indicatorColor = BlushPink.copy(alpha = 0.45f),
                                            unselectedIconColor = MutedTaupe,
                                            unselectedTextColor = MutedTaupe
                                        ),
                                        modifier = Modifier.testTag("nav_tab_${tab.route}")
                                    )
                                }
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = innerPadding.calculateTopPadding())
                ) {
                    if (showMainNav && isWideScreen) {
                        NavigationRail(
                            containerColor = PureWhite.copy(alpha = 0.72f),
                            modifier = Modifier
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp))
                        ) {
                            MAIN_TABS.forEach { tab ->
                                val selected = currentRoute == tab.route
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = {
                                        navController.navigate(tab.route) {
                                            popUpTo(Routes.HOME) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tab.label
                                        )
                                    },
                                    label = { Text(tab.label) },
                                    colors = NavigationRailItemDefaults.colors(
                                        selectedIconColor = ObsidianBlack,
                                        indicatorColor = BlushPink.copy(alpha = 0.5f),
                                        unselectedIconColor = MutedTaupe
                                    )
                                )
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        NavHost(
                            navController = navController,
                            startDestination = Routes.HOME
                        ) {
                            composable(Routes.HOME) {
                                HomeScreen(
                                    settings = settings,
                                    categories = categories,
                                    products = products,
                                    announcements = announcements,
                                    banners = banners,
                                    reviews = reviews,
                                    wishlistProductIds = wishlistIds,
                                    isAdmin = isAdmin,
                                    onOpenSearch = {
                                        navController.navigate(Routes.SHOP) {
                                            popUpTo(Routes.HOME) { saveState = true }
                                            launchSingleTop = true
                                        }
                                    },
                                    onSelectCategory = { catId ->
                                        viewModel.selectCategory(catId)
                                        navController.navigate(Routes.SHOP) {
                                            popUpTo(Routes.HOME) { saveState = true }
                                            launchSingleTop = true
                                        }
                                    },
                                    onProductClick = { productId ->
                                        navController.navigate(Routes.productDetail(productId))
                                    },
                                    onToggleWishlist = { viewModel.toggleWishlist(it) },
                                    onOpenAdmin = {
                                        if (isAdmin) navController.navigate(Routes.ADMIN)
                                    },
                                    onOpenWhatsAppConcierge = {
                                        val digits = settings.whatsappNumber.filter { it.isDigit() }
                                        val waDigits = if (digits.length == 10) "91$digits" else digits.ifBlank { "919304560827" }
                                        val url = "https://wa.me/$waDigits?text=${Uri.encode("Hello ${settings.brandName}! I would like styling & custom tailoring assistance.")}"
                                        launchWhatsAppUrl(url)
                                    }
                                )
                            }

                            composable(Routes.SHOP) {
                                ShopCatalogScreen(
                                    products = filteredProducts,
                                    allPublishedProducts = products.filter { it.isPublished },
                                    categories = categories,
                                    wishlistIds = wishlistIds,
                                    searchQuery = searchQuery,
                                    selectedCategoryId = selectedCategoryId,
                                    selectedSizeFilter = selectedSizeFilter,
                                    selectedColorFilter = selectedColorFilter,
                                    maxPriceFilter = maxPriceFilter,
                                    sortOption = sortOption,
                                    onSearchChange = viewModel::setSearchQuery,
                                    onCategorySelect = viewModel::selectCategory,
                                    onSizeSelect = viewModel::selectSizeFilter,
                                    onColorSelect = viewModel::selectColorFilter,
                                    onMaxPriceSelect = viewModel::setMaxPriceFilter,
                                    onSortSelect = viewModel::setSortOption,
                                    onClearFilters = viewModel::clearFilters,
                                    onProductClick = { productId ->
                                        navController.navigate(Routes.productDetail(productId))
                                    },
                                    onToggleWishlist = viewModel::toggleWishlist
                                )
                            }

                            composable(Routes.WISHLIST) {
                                AccountScreen(
                                    userProfile = userProfile,
                                    userEmail = email,
                                    isEmailVerified = isEmailVerified,
                                    isAdmin = isAdmin,
                                    orders = userOrders,
                                    wishlist = wishlist,
                                    products = products,
                                    settings = settings,
                                    initialTab = 1,
                                    onUpdateProfile = viewModel::updateProfile,
                                    onCancelOrder = viewModel::cancelOrder,
                                    onRemoveWishlist = viewModel::toggleWishlist,
                                    onOpenProduct = { productId ->
                                        navController.navigate(Routes.productDetail(productId))
                                    },
                                    onOpenAdmin = {
                                        if (isAdmin) navController.navigate(Routes.ADMIN)
                                    },
                                    onSignOut = {
                                        signOut(credentialManager, scope)
                                    }
                                )
                            }

                            composable(Routes.ACCOUNT) {
                                AccountScreen(
                                    userProfile = userProfile,
                                    userEmail = email,
                                    isEmailVerified = isEmailVerified,
                                    isAdmin = isAdmin,
                                    orders = userOrders,
                                    wishlist = wishlist,
                                    products = products,
                                    settings = settings,
                                    initialTab = 0,
                                    onUpdateProfile = viewModel::updateProfile,
                                    onCancelOrder = viewModel::cancelOrder,
                                    onRemoveWishlist = viewModel::toggleWishlist,
                                    onOpenProduct = { productId ->
                                        navController.navigate(Routes.productDetail(productId))
                                    },
                                    onOpenAdmin = {
                                        if (isAdmin) navController.navigate(Routes.ADMIN)
                                    },
                                    onSignOut = {
                                        signOut(credentialManager, scope)
                                    }
                                )
                            }

                            composable(
                                route = Routes.PRODUCT_DETAIL,
                                arguments = listOf(navArgument("productId") { type = NavType.StringType })
                            ) { entry ->
                                val productId = entry.arguments?.getString("productId").orEmpty()
                                val product = products.find { it.id == productId }
                                if (product == null) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .statusBarsPadding(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("Piece unavailable or loading...", style = MaterialTheme.typography.bodyLarge, color = DeepRose)
                                    }
                                } else {
                                    val related = products.filter {
                                        it.isPublished && it.id != product.id && it.categoryId == product.categoryId
                                    }.take(6)

                                    ProductDetailScreen(
                                        product = product,
                                        relatedProducts = related,
                                        reviews = reviews,
                                        userProfile = userProfile,
                                        isWishlisted = product.id in wishlistIds,
                                        wishlistIds = wishlistIds,
                                        onBack = { navController.popBackStack() },
                                        onToggleWishlist = viewModel::toggleWishlist,
                                        onProductClick = { nextId ->
                                            navController.navigate(Routes.productDetail(nextId))
                                        },
                                        onSubmitReview = { rating, comment ->
                                            viewModel.submitReview(product.id, rating, comment)
                                        },
                                        onConfirmWhatsAppOrder = { size, color, qty, name, phone, address, notes ->
                                            viewModel.placeWhatsAppOrder(
                                                product = product,
                                                selectedSize = size,
                                                selectedColor = color,
                                                quantity = qty,
                                                customerName = name,
                                                customerPhone = phone,
                                                deliveryAddress = address,
                                                notes = notes,
                                                onLaunchWhatsApp = ::launchWhatsAppUrl
                                            )
                                        }
                                    )
                                }
                            }

                            composable(Routes.ADMIN) {
                                if (isAdmin) {
                                    AdminDashboardScreen(
                                        products = products,
                                        categories = categories,
                                        orders = allOrders,
                                        users = allUsers,
                                        announcements = announcements,
                                        banners = banners,
                                        reviews = reviews,
                                        adminRoles = adminRoles,
                                        settings = settings,
                                        onBack = { navController.popBackStack() },
                                        onSeedStorefront = viewModel::seedStorefront,
                                        onSaveProduct = viewModel::saveProduct,
                                        onDeleteProduct = viewModel::deleteProduct,
                                        onSaveCategory = viewModel::saveCategory,
                                        onSwapCategoryOrder = viewModel::swapCategoryOrder,
                                        onDeleteCategory = viewModel::deleteCategory,
                                        onUpdateOrder = viewModel::updateOrderStatus,
                                        onSaveAnnouncement = viewModel::saveAnnouncement,
                                        onDeleteAnnouncement = viewModel::deleteAnnouncement,
                                        onSaveBanner = viewModel::saveBanner,
                                        onDeleteBanner = viewModel::deleteBanner,
                                        onSaveSettings = viewModel::saveWebsiteSettings,
                                        onToggleUserBlocked = viewModel::toggleUserBlocked,
                                        onToggleFeaturedReview = viewModel::toggleFeaturedReview,
                                        onDeleteReview = viewModel::deleteReview,
                                        onAddAdmin = viewModel::addAdminRole,
                                        onRemoveAdmin = viewModel::removeAdminRole
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("Authorized administrators only.", style = MaterialTheme.typography.titleLarge)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
