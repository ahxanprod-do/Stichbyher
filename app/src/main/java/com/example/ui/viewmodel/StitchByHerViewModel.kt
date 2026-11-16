package com.example.ui.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AdminRole
import com.example.data.model.Announcement
import com.example.data.model.Banner
import com.example.data.model.Category
import com.example.data.model.OrderEnquiry
import com.example.data.model.Product
import com.example.data.model.Review
import com.example.data.model.UserProfile
import com.example.data.model.WebsiteSettings
import com.example.data.model.WishlistItem
import com.example.data.repository.StitchByHerRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ProductSortOption(val label: String) {
    LATEST("Latest"),
    PRICE_LOW_HIGH("Price: Low to High"),
    PRICE_HIGH_LOW("Price: High to Low"),
    POPULARITY("Popularity")
}

@OptIn(ExperimentalCoroutinesApi::class)
class StitchByHerViewModel(
    private val repository: StitchByHerRepository,
    val currentUserId: String,
    val userDisplayName: String,
    val userEmail: String,
    val userPhotoUrl: String?,
    val isEmailVerified: Boolean
) : ViewModel() {

    private val _toastMessage = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val toastMessage: SharedFlow<String> = _toastMessage.asSharedFlow()

    private val _isAdmin = MutableStateFlow(false)
    val isAdmin: StateFlow<Boolean> = _isAdmin.asStateFlow()

    // Search & filter state
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategoryId = MutableStateFlow<String?>(null)
    val selectedCategoryId: StateFlow<String?> = _selectedCategoryId.asStateFlow()

    private val _selectedSizeFilter = MutableStateFlow<String?>(null)
    val selectedSizeFilter: StateFlow<String?> = _selectedSizeFilter.asStateFlow()

    private val _selectedColorFilter = MutableStateFlow<String?>(null)
    val selectedColorFilter: StateFlow<String?> = _selectedColorFilter.asStateFlow()

    private val _maxPriceFilter = MutableStateFlow<Double?>(null)
    val maxPriceFilter: StateFlow<Double?> = _maxPriceFilter.asStateFlow()

    private val _sortOption = MutableStateFlow(ProductSortOption.LATEST)
    val sortOption: StateFlow<ProductSortOption> = _sortOption.asStateFlow()

    private fun <T> Flow<T>.safeState(initial: T, tag: String): StateFlow<T> =
        this.catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            Log.w(TAG, "Stream error ($tag): ${(firestoreError ?: error).message}")
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = initial
        )

    val userProfile: StateFlow<UserProfile?> = repository.observeUserProfile(currentUserId)
        .safeState(null, "userProfile")

    val settings: StateFlow<WebsiteSettings> = repository.observeWebsiteSettings()
        .safeState(WebsiteSettings(), "websiteSettings")

    val categories: StateFlow<List<Category>> = repository.observeCategories()
        .safeState(emptyList(), "categories")

    val products: StateFlow<List<Product>> = repository.observeProducts()
        .safeState(emptyList(), "products")

    val announcements: StateFlow<List<Announcement>> = repository.observeAnnouncements()
        .safeState(emptyList(), "announcements")

    val banners: StateFlow<List<Banner>> = repository.observeBanners()
        .safeState(emptyList(), "banners")

    val reviews: StateFlow<List<Review>> = repository.observeReviews()
        .safeState(emptyList(), "reviews")

    val wishlist: StateFlow<List<WishlistItem>> = repository.observeWishlist(currentUserId)
        .safeState(emptyList(), "wishlist")

    val userOrders: StateFlow<List<OrderEnquiry>> = repository.observeUserOrders(currentUserId)
        .safeState(emptyList(), "userOrders")

    val allOrders: StateFlow<List<OrderEnquiry>> = _isAdmin
        .flatMapLatest { admin ->
            if (admin) repository.observeAllOrders() else flowOf(emptyList())
        }
        .safeState(emptyList(), "allOrders")

    val allUsers: StateFlow<List<UserProfile>> = _isAdmin
        .flatMapLatest { admin ->
            if (admin) repository.observeAllUsers() else flowOf(emptyList())
        }
        .safeState(emptyList(), "allUsers")

    val adminRoles: StateFlow<List<AdminRole>> = _isAdmin
        .flatMapLatest { admin ->
            if (admin) repository.observeAdmins() else flowOf(emptyList())
        }
        .safeState(emptyList(), "adminRoles")

    val filteredProducts: StateFlow<List<Product>> = combine(
        products,
        _searchQuery,
        _selectedCategoryId,
        _selectedSizeFilter,
        _selectedColorFilter,
        _maxPriceFilter,
        _sortOption
    ) { array ->
        @Suppress("UNCHECKED_CAST")
        val allProducts = array[0] as List<Product>
        val query = (array[1] as String).trim()
        val catId = array[2] as String?
        val sizeFilter = array[3] as String?
        val colorFilter = array[4] as String?
        val maxPrice = array[5] as Double?
        val sort = array[6] as ProductSortOption

        val filtered = allProducts
            .filter { it.isPublished }
            .filter { prod ->
                query.isBlank() ||
                    prod.name.contains(query, ignoreCase = true) ||
                    prod.categoryName.contains(query, ignoreCase = true) ||
                    prod.description.contains(query, ignoreCase = true)
            }
            .filter { prod -> catId == null || prod.categoryId == catId }
            .filter { prod -> sizeFilter == null || prod.sizes.any { it.equals(sizeFilter, ignoreCase = true) } }
            .filter { prod -> colorFilter == null || prod.colors.any { it.equals(colorFilter, ignoreCase = true) } }
            .filter { prod -> maxPrice == null || prod.price <= maxPrice }

        when (sort) {
            ProductSortOption.LATEST -> filtered.sortedByDescending { it.createdAt?.seconds ?: 0L }
            ProductSortOption.PRICE_LOW_HIGH -> filtered.sortedBy { it.price }
            ProductSortOption.PRICE_HIGH_LOW -> filtered.sortedByDescending { it.price }
            ProductSortOption.POPULARITY -> filtered.sortedByDescending {
                (if (it.isBestSeller) 100 else 0) + (it.ratingAvg * 10) + it.reviewCount
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptyList()
    )

    init {
        viewModelScope.launch {
            repository.ensureUserProfile(
                userId = currentUserId,
                displayName = userDisplayName,
                email = userEmail,
                photoUrl = userPhotoUrl
            )
            val admin = repository.checkIsAdmin(currentUserId, userEmail, isEmailVerified)
            _isAdmin.value = admin
            if (admin) {
                repository.seedInitialStorefrontIfEmpty()
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectCategory(categoryId: String?) {
        _selectedCategoryId.value = categoryId
    }

    fun selectSizeFilter(size: String?) {
        _selectedSizeFilter.value = size
    }

    fun selectColorFilter(color: String?) {
        _selectedColorFilter.value = color
    }

    fun setMaxPriceFilter(maxPrice: Double?) {
        _maxPriceFilter.value = maxPrice
    }

    fun setSortOption(option: ProductSortOption) {
        _sortOption.value = option
    }

    fun clearFilters() {
        _searchQuery.value = ""
        _selectedCategoryId.value = null
        _selectedSizeFilter.value = null
        _selectedColorFilter.value = null
        _maxPriceFilter.value = null
        _sortOption.value = ProductSortOption.LATEST
    }

    fun toggleWishlist(product: Product) {
        val currentlySaved = wishlist.value.any { it.productId == product.id }
        viewModelScope.launch {
            repository.toggleWishlist(product, currentlySaved)
                .onSuccess { nowSaved ->
                    _toastMessage.tryEmit(
                        if (nowSaved) "Saved to your Wishlist" else "Removed from Wishlist"
                    )
                }
                .onFailure {
                    _toastMessage.tryEmit(it.localizedMessage ?: "Failed to update wishlist")
                }
        }
    }

    fun updateProfile(
        fullName: String,
        phoneNumber: String,
        savedAddresses: List<String>,
        defaultAddress: String,
        photoUrl: String? = null
    ) {
        viewModelScope.launch {
            repository.updateUserProfile(
                fullName = fullName,
                phoneNumber = phoneNumber,
                savedAddresses = savedAddresses,
                defaultAddress = defaultAddress,
                photoUrl = photoUrl
            ).onSuccess {
                _toastMessage.tryEmit("Profile saved")
            }.onFailure {
                _toastMessage.tryEmit(it.localizedMessage ?: "Failed to save profile")
            }
        }
    }

    fun placeWhatsAppOrder(
        product: Product,
        selectedSize: String,
        selectedColor: String,
        quantity: Int,
        customerName: String,
        customerPhone: String,
        deliveryAddress: String,
        notes: String,
        onLaunchWhatsApp: (String) -> Unit
    ) {
        if (userProfile.value?.isBlocked == true) {
            _toastMessage.tryEmit("Your account is currently restricted from placing orders.")
            return
        }
        viewModelScope.launch {
            // Also persist the customer's phone & address onto their profile for convenience
            val currentProf = userProfile.value
            if (currentProf != null) {
                val mergedAddresses = (currentProf.savedAddresses + deliveryAddress.trim())
                    .filter { it.isNotBlank() }
                    .distinct()
                    .take(10)
                repository.updateUserProfile(
                    fullName = customerName.ifBlank { currentProf.fullName },
                    phoneNumber = customerPhone.ifBlank { currentProf.phoneNumber },
                    savedAddresses = mergedAddresses,
                    defaultAddress = deliveryAddress.trim()
                )
            }

            repository.createOrderEnquiry(
                product = product,
                customerName = customerName,
                customerPhone = customerPhone,
                deliveryAddress = deliveryAddress,
                selectedSize = selectedSize,
                selectedColor = selectedColor,
                quantity = quantity,
                notes = notes
            ).onSuccess {
                val rawPhone = settings.value.whatsappNumber.filter { it.isDigit() }
                val waDigits = when {
                    rawPhone.length == 10 -> "91$rawPhone"
                    rawPhone.isNotBlank() -> rawPhone
                    else -> "919304560827"
                }
                val unitPriceText = "₹${product.price.toInt()}"
                val message = buildString {
                    appendLine("Hello ${settings.value.brandName}!")
                    appendLine()
                    appendLine("I want to place an order.")
                    appendLine()
                    appendLine("Product: ${product.name}")
                    appendLine("Product ID: ${product.id}")
                    appendLine("Price: $unitPriceText")
                    appendLine("Size: ${selectedSize.ifBlank { "Standard" }}")
                    appendLine("Color: ${selectedColor.ifBlank { "Signature" }}")
                    appendLine("Quantity: $quantity")
                    appendLine("Customer Name: ${customerName.trim()}")
                    appendLine("Phone Number: ${customerPhone.trim()}")
                    appendLine("Delivery Address: ${deliveryAddress.trim()}")
                    if (notes.isNotBlank()) {
                        appendLine("Tailoring Notes: ${notes.trim()}")
                    }
                    appendLine()
                    append("Please confirm my order.")
                }
                val url = "https://wa.me/$waDigits?text=${Uri.encode(message)}"
                onLaunchWhatsApp(url)
            }.onFailure {
                _toastMessage.tryEmit(it.localizedMessage ?: "Unable to log WhatsApp enquiry")
            }
        }
    }

    fun cancelOrder(orderId: String) {
        viewModelScope.launch {
            repository.cancelOrderByCustomer(orderId)
                .onSuccess { _toastMessage.tryEmit("Order enquiry cancelled") }
                .onFailure { _toastMessage.tryEmit(it.localizedMessage ?: "Unable to cancel order") }
        }
    }

    fun submitReview(productId: String, rating: Int, comment: String) {
        val reviewerName = userProfile.value?.fullName?.ifBlank { userDisplayName } ?: userDisplayName
        viewModelScope.launch {
            repository.submitReview(productId, reviewerName, rating, comment)
                .onSuccess { _toastMessage.tryEmit("Thank you for your review!") }
                .onFailure { _toastMessage.tryEmit(it.localizedMessage ?: "Could not post review") }
        }
    }

    // --- ADMIN ACTIONS ---

    fun seedStorefront() {
        viewModelScope.launch {
            repository.seedInitialStorefrontIfEmpty()
                .onSuccess { seeded ->
                    _toastMessage.tryEmit(
                        if (seeded) "Boutique storefront initialized" else "Storefront already populated"
                    )
                }
                .onFailure { _toastMessage.tryEmit(it.localizedMessage ?: "Failed to initialize store") }
        }
    }

    fun saveProduct(product: Product) {
        viewModelScope.launch {
            repository.saveProduct(product)
                .onSuccess { _toastMessage.tryEmit("Product saved") }
                .onFailure { _toastMessage.tryEmit(it.localizedMessage ?: "Failed to save product") }
        }
    }

    fun deleteProduct(productId: String) {
        viewModelScope.launch {
            repository.deleteProduct(productId)
                .onSuccess { _toastMessage.tryEmit("Product deleted") }
                .onFailure { _toastMessage.tryEmit(it.localizedMessage ?: "Failed to delete product") }
        }
    }

    fun saveCategory(category: Category) {
        viewModelScope.launch {
            repository.saveCategory(category)
                .onSuccess { _toastMessage.tryEmit("Category saved") }
                .onFailure { _toastMessage.tryEmit(it.localizedMessage ?: "Failed to save category") }
        }
    }

    fun swapCategoryOrder(first: Category, second: Category) {
        viewModelScope.launch {
            repository.saveCategory(first.copy(sortOrder = second.sortOrder))
            repository.saveCategory(second.copy(sortOrder = first.sortOrder))
        }
    }

    fun deleteCategory(categoryId: String) {
        viewModelScope.launch {
            repository.deleteCategory(categoryId)
                .onSuccess { _toastMessage.tryEmit("Category deleted") }
                .onFailure { _toastMessage.tryEmit(it.localizedMessage ?: "Failed to delete category") }
        }
    }

    fun saveAnnouncement(announcement: Announcement) {
        viewModelScope.launch {
            repository.saveAnnouncement(announcement)
                .onSuccess { _toastMessage.tryEmit("Announcement updated") }
                .onFailure { _toastMessage.tryEmit(it.localizedMessage ?: "Failed to save announcement") }
        }
    }

    fun deleteAnnouncement(id: String) {
        viewModelScope.launch {
            repository.deleteAnnouncement(id)
                .onSuccess { _toastMessage.tryEmit("Announcement removed") }
                .onFailure { _toastMessage.tryEmit(it.localizedMessage ?: "Failed to delete announcement") }
        }
    }

    fun saveBanner(banner: Banner) {
        viewModelScope.launch {
            repository.saveBanner(banner)
                .onSuccess { _toastMessage.tryEmit("Banner updated") }
                .onFailure { _toastMessage.tryEmit(it.localizedMessage ?: "Failed to save banner") }
        }
    }

    fun deleteBanner(id: String) {
        viewModelScope.launch {
            repository.deleteBanner(id)
                .onSuccess { _toastMessage.tryEmit("Banner removed") }
                .onFailure { _toastMessage.tryEmit(it.localizedMessage ?: "Failed to delete banner") }
        }
    }

    fun saveWebsiteSettings(newSettings: WebsiteSettings) {
        viewModelScope.launch {
            repository.saveWebsiteSettings(newSettings)
                .onSuccess { _toastMessage.tryEmit("Storefront updated live") }
                .onFailure { _toastMessage.tryEmit(it.localizedMessage ?: "Failed to save store settings") }
        }
    }

    fun updateOrderStatus(orderId: String, status: String, channel: String, notes: String) {
        viewModelScope.launch {
            repository.updateOrderByAdmin(orderId, status, channel, notes)
                .onSuccess { _toastMessage.tryEmit("Order status updated to $status") }
                .onFailure { _toastMessage.tryEmit(it.localizedMessage ?: "Failed to update order") }
        }
    }

    fun toggleUserBlocked(user: UserProfile) {
        viewModelScope.launch {
            repository.setUserBlocked(user.userId, !user.isBlocked)
                .onSuccess {
                    _toastMessage.tryEmit(if (!user.isBlocked) "Customer blocked" else "Customer unblocked")
                }
                .onFailure { _toastMessage.tryEmit(it.localizedMessage ?: "Failed to update user access") }
        }
    }

    fun toggleFeaturedReview(review: Review) {
        viewModelScope.launch {
            repository.toggleFeaturedReview(review.id, !review.isFeaturedOnHome)
                .onSuccess { _toastMessage.tryEmit("Review highlight updated") }
                .onFailure { _toastMessage.tryEmit(it.localizedMessage ?: "Failed to update review") }
        }
    }

    fun deleteReview(reviewId: String) {
        viewModelScope.launch {
            repository.deleteReview(reviewId)
                .onSuccess { _toastMessage.tryEmit("Review removed") }
                .onFailure { _toastMessage.tryEmit(it.localizedMessage ?: "Failed to remove review") }
        }
    }

    fun addAdminRole(uid: String, email: String) {
        viewModelScope.launch {
            repository.addAdmin(uid, email)
                .onSuccess { _toastMessage.tryEmit("Admin access granted") }
                .onFailure { _toastMessage.tryEmit(it.localizedMessage ?: "Failed to add admin") }
        }
    }

    fun removeAdminRole(uid: String) {
        viewModelScope.launch {
            repository.removeAdmin(uid)
                .onSuccess { _toastMessage.tryEmit("Admin access revoked") }
                .onFailure { _toastMessage.tryEmit(it.localizedMessage ?: "Failed to remove admin") }
        }
    }

    private companion object {
        const val TAG = "StitchByHerVM"
    }
}
