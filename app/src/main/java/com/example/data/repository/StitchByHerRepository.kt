package com.example.data.repository

import android.content.Context
import com.example.R
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
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class StitchByHerRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        Firebase.auth
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in before accessing StitchByHer data.")
    }

    private fun <T> Flow<T>.logFirestoreErrors(
        operationType: OperationType,
        path: String,
        expectedUid: String? = null
    ): Flow<T> = catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && (expectedUid == null || auth.currentUser?.uid == expectedUid)) {
            handleFirestoreError(exceptionToLog, operationType, path, auth)
        }
        throw error
    }

    // --- ADMIN VERIFICATION & MANAGEMENT ---

    suspend fun checkIsAdmin(uid: String, email: String?, isEmailVerified: Boolean): Boolean {
        if (isEmailVerified && email?.equals(BOOTSTRAPPED_ADMIN_EMAIL, ignoreCase = true) == true) {
            return true
        }
        val path = "admins/$uid"
        return try {
            val snap = db.collection("admins").document(uid).get().await()
            snap.exists()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path, auth)
            false
        }
    }

    fun observeAdmins(): Flow<List<AdminRole>> =
        db.collection("admins")
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull {
                    it.toObject(AdminRole::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
            }
            .logFirestoreErrors(OperationType.LIST, "admins")

    suspend fun addAdmin(uid: String, email: String): Result<Unit> = runCatching {
        val adminUid = requireUserId()
        val cleanUid = uid.trim()
        require(cleanUid.isNotBlank()) { "Admin UID cannot be empty" }
        val docRef = db.collection("admins").document(cleanUid)
        val payload = mapOf(
            "uid" to cleanUid,
            "email" to email.trim(),
            "addedBy" to adminUid,
            "createdAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path, auth)
            throw e
        }
    }

    suspend fun removeAdmin(uid: String): Result<Unit> = runCatching {
        requireUserId()
        val docRef = db.collection("admins").document(uid)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path, auth)
            throw e
        }
    }

    // --- USER PROFILE & ACCOUNTS ---

    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        val uid = requireUserId()
        require(uid == userId) { "User ID mismatch" }
        val path = "users/$userId"
        emitAll(
            db.collection("users").document(userId)
                .snapshots()
                .map { snap ->
                    snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .logFirestoreErrors(OperationType.GET, path, userId)
        )
    }

    suspend fun ensureUserProfile(
        userId: String,
        displayName: String,
        email: String,
        photoUrl: String?
    ): Result<UserProfile> = runCatching {
        val uid = requireUserId()
        require(uid == userId) { "Cannot initialize profile for another user" }
        val docRef = db.collection("users").document(userId)
        try {
            val existing = docRef.get().await()
            if (existing.exists()) {
                val parsed = existing.toObject(
                    UserProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
                if (parsed != null) return@runCatching parsed
            }
            val safeName = displayName.trim().ifBlank { email.substringBefore("@").ifBlank { "Boutique Guest" } }.take(100)
            val payload = mutableMapOf<String, Any>(
                "userId" to userId,
                "fullName" to safeName,
                "email" to email.trim().take(150),
                "phoneNumber" to "",
                "savedAddresses" to emptyList<String>(),
                "defaultAddress" to "",
                "isBlocked" to false,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (!photoUrl.isNullOrBlank()) {
                payload["photoUrl"] = photoUrl.take(500000)
            }
            docRef.set(payload).await()
            val created = docRef.get().await().toObject(
                UserProfile::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
            created ?: UserProfile(
                id = userId,
                userId = userId,
                fullName = safeName,
                email = email,
                photoUrl = photoUrl
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path, auth)
            throw e
        }
    }

    suspend fun updateUserProfile(
        fullName: String,
        phoneNumber: String,
        savedAddresses: List<String>,
        defaultAddress: String,
        photoUrl: String? = null
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        val cleanName = fullName.trim().ifBlank { "Boutique Customer" }.take(100)
        val cleanPhone = phoneNumber.trim().take(30)
        val cleanAddresses = savedAddresses.map { it.trim().take(380) }.filter { it.isNotBlank() }.distinct().take(10)
        val cleanDefault = defaultAddress.trim().take(380)
        val updates = mutableMapOf<String, Any>(
            "fullName" to cleanName,
            "phoneNumber" to cleanPhone,
            "savedAddresses" to cleanAddresses,
            "defaultAddress" to cleanDefault,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (!photoUrl.isNullOrBlank()) {
            updates["photoUrl"] = photoUrl.take(500000)
        }
        try {
            docRef.update(updates).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path, auth)
            throw e
        }
    }

    fun observeAllUsers(): Flow<List<UserProfile>> =
        db.collection("users")
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull {
                    it.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }.sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .logFirestoreErrors(OperationType.LIST, "users")

    suspend fun setUserBlocked(userId: String, isBlocked: Boolean): Result<Unit> = runCatching {
        requireUserId()
        val docRef = db.collection("users").document(userId)
        try {
            docRef.update(
                mapOf(
                    "isBlocked" to isBlocked,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path, auth)
            throw e
        }
    }

    // --- WISHLIST ---

    fun observeWishlist(userId: String): Flow<List<WishlistItem>> = flow {
        val uid = requireUserId()
        require(uid == userId) { "Wishlist userId mismatch" }
        val path = "users/$userId/wishlist"
        emitAll(
            db.collection("users").document(userId).collection("wishlist")
                .snapshots()
                .map { snapshot ->
                    snapshot.documents.mapNotNull {
                        it.toObject(WishlistItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    }.sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .logFirestoreErrors(OperationType.LIST, path, userId)
        )
    }

    suspend fun toggleWishlist(product: Product, currentlySaved: Boolean): Result<Boolean> = runCatching {
        val uid = requireUserId()
        require(product.id.isNotBlank()) { "Product ID required for wishlist" }
        val docRef = db.collection("users").document(uid).collection("wishlist").document(product.id)
        try {
            if (currentlySaved) {
                docRef.delete().await()
                false
            } else {
                val payload = mutableMapOf<String, Any>(
                    "productId" to product.id,
                    "productName" to product.name.take(160),
                    "price" to product.price,
                    "createdAt" to FieldValue.serverTimestamp()
                )
                val firstImg = product.imageUrls.firstOrNull()
                if (!firstImg.isNullOrBlank()) {
                    payload["imageUrl"] = firstImg.take(500000)
                }
                docRef.set(payload).await()
                true
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path, auth)
            throw e
        }
    }

    // --- CATEGORIES ---

    fun observeCategories(): Flow<List<Category>> =
        db.collection("categories")
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull {
                    it.toObject(Category::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }.sortedBy { it.sortOrder }
            }
            .logFirestoreErrors(OperationType.LIST, "categories")

    suspend fun saveCategory(category: Category): Result<String> = runCatching {
        requireUserId()
        val isNew = category.id.isBlank()
        val docId = if (isNew) "cat_${UUID.randomUUID().toString().take(8)}" else category.id
        val docRef = db.collection("categories").document(docId)
        val payload = mutableMapOf<String, Any>(
            "name" to category.name.trim().ifBlank { "Collection" }.take(80),
            "description" to category.description.trim().take(300),
            "sortOrder" to category.sortOrder.coerceIn(0L, 10000L),
            "isVisible" to category.isVisible,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (!category.imageUrl.isNullOrBlank()) {
            payload["imageUrl"] = category.imageUrl.take(500000)
        }
        try {
            if (isNew) {
                payload["createdAt"] = FieldValue.serverTimestamp()
                docRef.set(payload).await()
            } else {
                docRef.update(payload).await()
            }
            docId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, if (isNew) OperationType.CREATE else OperationType.UPDATE, docRef.path, auth)
            throw e
        }
    }

    suspend fun deleteCategory(categoryId: String): Result<Unit> = runCatching {
        requireUserId()
        val docRef = db.collection("categories").document(categoryId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path, auth)
            throw e
        }
    }

    // --- PRODUCTS ---

    fun observeProducts(): Flow<List<Product>> =
        db.collection("products")
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull {
                    it.toObject(Product::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }.sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .logFirestoreErrors(OperationType.LIST, "products")

    suspend fun saveProduct(product: Product): Result<String> = runCatching {
        requireUserId()
        val isNew = product.id.isBlank()
        val docId = if (isNew) "prod_${UUID.randomUUID().toString().take(8)}" else product.id
        val docRef = db.collection("products").document(docId)
        val discount = if (product.originalPrice > product.price && product.originalPrice > 0.0) {
            (((product.originalPrice - product.price) / product.originalPrice) * 100).toLong().coerceIn(0L, 99L)
        } else {
            product.discountPercent.coerceIn(0L, 99L)
        }
        val payload = mutableMapOf<String, Any>(
            "name" to product.name.trim().ifBlank { "Boutique Creation" }.take(160),
            "categoryId" to product.categoryId.trim().ifBlank { "cat_general" }.take(128),
            "categoryName" to product.categoryName.trim().ifBlank { "Women's Clothing" }.take(80),
            "price" to product.price.coerceAtLeast(0.0),
            "originalPrice" to product.originalPrice.coerceAtLeast(product.price),
            "discountPercent" to discount,
            "description" to product.description.trim().take(2000),
            "fabricDetails" to product.fabricDetails.trim().take(500),
            "specifications" to product.specifications.trim().take(1000),
            "highlights" to product.highlights.map { it.trim().take(300) }.filter { it.isNotBlank() }.take(15),
            "sizes" to product.sizes.map { it.trim().take(40) }.filter { it.isNotBlank() }.take(15),
            "colors" to product.colors.map { it.trim().take(40) }.filter { it.isNotBlank() }.take(15),
            "imageUrls" to product.imageUrls.map { it.trim().take(500000) }.filter { it.isNotBlank() }.take(8),
            "stock" to product.stock.coerceIn(0L, 100000L),
            "isFeatured" to product.isFeatured,
            "isBestSeller" to product.isBestSeller,
            "isNewArrival" to product.isNewArrival,
            "isTrending" to product.isTrending,
            "isSpecialCollection" to product.isSpecialCollection,
            "isPublished" to product.isPublished,
            "ratingAvg" to product.ratingAvg.coerceIn(0.0, 5.0),
            "reviewCount" to product.reviewCount.coerceAtLeast(0L),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            if (isNew) {
                payload["createdAt"] = FieldValue.serverTimestamp()
                docRef.set(payload).await()
            } else {
                docRef.update(payload).await()
            }
            docId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, if (isNew) OperationType.CREATE else OperationType.UPDATE, docRef.path, auth)
            throw e
        }
    }

    suspend fun deleteProduct(productId: String): Result<Unit> = runCatching {
        requireUserId()
        val docRef = db.collection("products").document(productId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path, auth)
            throw e
        }
    }

    // --- ORDERS (WHATSAPP ENQUIRIES & FULFILLMENT) ---

    fun observeUserOrders(userId: String): Flow<List<OrderEnquiry>> = flow {
        val uid = requireUserId()
        require(uid == userId) { "Order query userId mismatch" }
        emitAll(
            db.collection("orders")
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.documents.mapNotNull {
                        it.toObject(OrderEnquiry::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    }.sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .logFirestoreErrors(OperationType.LIST, "orders", userId)
        )
    }

    fun observeAllOrders(): Flow<List<OrderEnquiry>> =
        db.collection("orders")
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull {
                    it.toObject(OrderEnquiry::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }.sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .logFirestoreErrors(OperationType.LIST, "orders")

    suspend fun createOrderEnquiry(
        product: Product,
        customerName: String,
        customerPhone: String,
        deliveryAddress: String,
        selectedSize: String,
        selectedColor: String,
        quantity: Int,
        notes: String = ""
    ): Result<OrderEnquiry> = runCatching {
        val uid = requireUserId()
        val orderId = "ord_${UUID.randomUUID().toString().take(8)}"
        val docRef = db.collection("orders").document(orderId)
        val qty = quantity.coerceIn(1, 100)
        val total = product.price * qty
        val firstImage = product.imageUrls.firstOrNull()
        val payload = mutableMapOf<String, Any>(
            "userId" to uid,
            "customerName" to customerName.trim().take(120),
            "customerPhone" to customerPhone.trim().take(30),
            "deliveryAddress" to deliveryAddress.trim().take(400),
            "productId" to product.id,
            "productName" to product.name.take(160),
            "selectedSize" to selectedSize.ifBlank { "Custom" }.take(40),
            "selectedColor" to selectedColor.ifBlank { "Signature" }.take(40),
            "quantity" to qty.toLong(),
            "unitPrice" to product.price,
            "totalPrice" to total,
            "status" to "Pending",
            "channel" to "WhatsApp Enquiry",
            "notes" to notes.trim().take(500),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (!firstImage.isNullOrBlank()) {
            payload["productImageUrl"] = firstImage.take(500000)
        }
        try {
            docRef.set(payload).await()
            OrderEnquiry(
                id = orderId,
                userId = uid,
                customerName = customerName.trim(),
                customerPhone = customerPhone.trim(),
                deliveryAddress = deliveryAddress.trim(),
                productId = product.id,
                productName = product.name,
                productImageUrl = firstImage,
                selectedSize = selectedSize.ifBlank { "Custom" },
                selectedColor = selectedColor.ifBlank { "Signature" },
                quantity = qty.toLong(),
                unitPrice = product.price,
                totalPrice = total,
                status = "Pending",
                channel = "WhatsApp Enquiry",
                notes = notes.trim()
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path, auth)
            throw e
        }
    }

    suspend fun updateOrderByAdmin(
        orderId: String,
        status: String,
        channel: String,
        notes: String
    ): Result<Unit> = runCatching {
        requireUserId()
        val docRef = db.collection("orders").document(orderId)
        try {
            docRef.update(
                mapOf(
                    "status" to status,
                    "channel" to channel,
                    "notes" to notes.trim().take(500),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path, auth)
            throw e
        }
    }

    suspend fun cancelOrderByCustomer(orderId: String): Result<Unit> = runCatching {
        requireUserId()
        val docRef = db.collection("orders").document(orderId)
        try {
            docRef.update(
                mapOf(
                    "status" to "Cancelled",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path, auth)
            throw e
        }
    }

    // --- ANNOUNCEMENTS ---

    fun observeAnnouncements(): Flow<List<Announcement>> =
        db.collection("announcements")
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull {
                    it.toObject(Announcement::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }.sortedBy { it.sortOrder }
            }
            .logFirestoreErrors(OperationType.LIST, "announcements")

    suspend fun saveAnnouncement(announcement: Announcement): Result<String> = runCatching {
        requireUserId()
        val isNew = announcement.id.isBlank()
        val docId = if (isNew) "ann_${UUID.randomUUID().toString().take(8)}" else announcement.id
        val docRef = db.collection("announcements").document(docId)
        val payload = mutableMapOf<String, Any>(
            "title" to announcement.title.trim().ifBlank { "Boutique Note" }.take(100),
            "message" to announcement.message.trim().ifBlank { "New collection arriving soon." }.take(240),
            "badgeText" to announcement.badgeText.trim().take(30),
            "sortOrder" to announcement.sortOrder.coerceIn(0L, 10000L),
            "isActive" to announcement.isActive,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            if (isNew) {
                payload["createdAt"] = FieldValue.serverTimestamp()
                docRef.set(payload).await()
            } else {
                docRef.update(payload).await()
            }
            docId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, if (isNew) OperationType.CREATE else OperationType.UPDATE, docRef.path, auth)
            throw e
        }
    }

    suspend fun deleteAnnouncement(id: String): Result<Unit> = runCatching {
        requireUserId()
        val docRef = db.collection("announcements").document(id)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path, auth)
            throw e
        }
    }

    // --- BANNERS ---

    fun observeBanners(): Flow<List<Banner>> =
        db.collection("banners")
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull {
                    it.toObject(Banner::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }.sortedBy { it.sortOrder }
            }
            .logFirestoreErrors(OperationType.LIST, "banners")

    suspend fun saveBanner(banner: Banner): Result<String> = runCatching {
        requireUserId()
        val isNew = banner.id.isBlank()
        val docId = if (isNew) "ban_${UUID.randomUUID().toString().take(8)}" else banner.id
        val docRef = db.collection("banners").document(docId)
        val payload = mutableMapOf<String, Any>(
            "title" to banner.title.trim().ifBlank { "Seasonal Atelier" }.take(120),
            "subtitle" to banner.subtitle.trim().take(240),
            "ctaText" to banner.ctaText.trim().take(40),
            "targetCategoryId" to banner.targetCategoryId.trim().take(128),
            "sortOrder" to banner.sortOrder.coerceIn(0L, 10000L),
            "isActive" to banner.isActive,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (!banner.imageUrl.isNullOrBlank()) {
            payload["imageUrl"] = banner.imageUrl.take(500000)
        }
        try {
            if (isNew) {
                payload["createdAt"] = FieldValue.serverTimestamp()
                docRef.set(payload).await()
            } else {
                docRef.update(payload).await()
            }
            docId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, if (isNew) OperationType.CREATE else OperationType.UPDATE, docRef.path, auth)
            throw e
        }
    }

    suspend fun deleteBanner(id: String): Result<Unit> = runCatching {
        requireUserId()
        val docRef = db.collection("banners").document(id)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path, auth)
            throw e
        }
    }

    // --- REVIEWS ---

    fun observeReviews(): Flow<List<Review>> =
        db.collection("reviews")
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull {
                    it.toObject(Review::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }.sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .logFirestoreErrors(OperationType.LIST, "reviews")

    suspend fun submitReview(
        productId: String,
        userName: String,
        rating: Int,
        comment: String
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val reviewId = "rev_${UUID.randomUUID().toString().take(8)}"
        val docRef = db.collection("reviews").document(reviewId)
        val payload = mapOf(
            "productId" to productId,
            "userId" to uid,
            "userName" to userName.trim().ifBlank { "Boutique Patron" }.take(100),
            "rating" to rating.coerceIn(1, 5).toLong(),
            "comment" to comment.trim().take(1000),
            "isFeaturedOnHome" to false,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            reviewId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path, auth)
            throw e
        }
    }

    suspend fun toggleFeaturedReview(reviewId: String, featured: Boolean): Result<Unit> = runCatching {
        requireUserId()
        val docRef = db.collection("reviews").document(reviewId)
        try {
            docRef.update(
                mapOf(
                    "isFeaturedOnHome" to featured,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path, auth)
            throw e
        }
    }

    suspend fun deleteReview(reviewId: String): Result<Unit> = runCatching {
        requireUserId()
        val docRef = db.collection("reviews").document(reviewId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path, auth)
            throw e
        }
    }

    // --- WEBSITE SETTINGS ---

    fun observeWebsiteSettings(): Flow<WebsiteSettings> =
        db.collection("websiteSettings").document("store")
            .snapshots()
            .map { snap ->
                snap.toObject(WebsiteSettings::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    ?: WebsiteSettings()
            }
            .logFirestoreErrors(OperationType.GET, "websiteSettings/store")

    suspend fun saveWebsiteSettings(settings: WebsiteSettings): Result<Unit> = runCatching {
        requireUserId()
        val docRef = db.collection("websiteSettings").document("store")
        val payload = mutableMapOf<String, Any>(
            "brandName" to settings.brandName.trim().ifBlank { "StitchByHer" }.take(60),
            "tagline" to settings.tagline.trim().take(120),
            "heroTitle" to settings.heroTitle.trim().ifBlank { "Couture Crafted Around Your Grace" }.take(140),
            "heroSubtitle" to settings.heroSubtitle.trim().take(300),
            "heroCtaText" to settings.heroCtaText.trim().ifBlank { "Explore Collection" }.take(40),
            "whatsappNumber" to settings.whatsappNumber.filter { it.isDigit() || it == '+' }.ifBlank { "9304560827" }.take(20),
            "aboutTitle" to settings.aboutTitle.trim().take(100),
            "aboutDescription" to settings.aboutDescription.trim().take(1000),
            "contactEmail" to settings.contactEmail.trim().take(120),
            "contactPhone" to settings.contactPhone.trim().take(30),
            "contactAddress" to settings.contactAddress.trim().take(250),
            "instagramHandle" to settings.instagramHandle.trim().take(80),
            "footerText" to settings.footerText.trim().take(250),
            "shippingPolicy" to settings.shippingPolicy.trim().take(600),
            "returnPolicy" to settings.returnPolicy.trim().take(600),
            "primaryColorHex" to settings.primaryColorHex.trim().ifBlank { "#E8B7BE" }.take(9),
            "accentColorHex" to settings.accentColorHex.trim().ifBlank { "#D9A0AA" }.take(9),
            "sectionOrder" to settings.sectionOrder.take(12),
            "hiddenSections" to settings.hiddenSections.take(12),
            "showAnnouncements" to settings.showAnnouncements,
            "showReviews" to settings.showReviews,
            "showAbout" to settings.showAbout,
            "showContact" to settings.showContact,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (!settings.heroImageUrl.isNullOrBlank()) {
            payload["heroImageUrl"] = settings.heroImageUrl.take(500000)
        }
        try {
            docRef.set(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path, auth)
            throw e
        }
    }

    // --- INITIAL ADMIN STOREFRONT SEEDING ---

    suspend fun seedInitialStorefrontIfEmpty(): Result<Boolean> = runCatching {
        requireUserId()
        val settingsSnap = db.collection("websiteSettings").document("store").get().await()
        val categoriesSnap = db.collection("categories").limit(1).get().await()
        if (settingsSnap.exists() && !categoriesSnap.isEmpty) {
            return@runCatching false
        }

        val batch = db.batch()
        val now = FieldValue.serverTimestamp()

        if (!settingsSnap.exists()) {
            val defaults = WebsiteSettings()
            batch.set(
                db.collection("websiteSettings").document("store"),
                mapOf(
                    "brandName" to defaults.brandName,
                    "tagline" to defaults.tagline,
                    "heroTitle" to defaults.heroTitle,
                    "heroSubtitle" to defaults.heroSubtitle,
                    "heroCtaText" to defaults.heroCtaText,
                    "heroImageUrl" to (defaults.heroImageUrl ?: "preset://hero_boutique"),
                    "whatsappNumber" to defaults.whatsappNumber,
                    "aboutTitle" to defaults.aboutTitle,
                    "aboutDescription" to defaults.aboutDescription,
                    "contactEmail" to defaults.contactEmail,
                    "contactPhone" to defaults.contactPhone,
                    "contactAddress" to defaults.contactAddress,
                    "instagramHandle" to defaults.instagramHandle,
                    "footerText" to defaults.footerText,
                    "shippingPolicy" to defaults.shippingPolicy,
                    "returnPolicy" to defaults.returnPolicy,
                    "primaryColorHex" to defaults.primaryColorHex,
                    "accentColorHex" to defaults.accentColorHex,
                    "sectionOrder" to defaults.sectionOrder,
                    "hiddenSections" to defaults.hiddenSections,
                    "showAnnouncements" to defaults.showAnnouncements,
                    "showReviews" to defaults.showReviews,
                    "showAbout" to defaults.showAbout,
                    "showContact" to defaults.showContact,
                    "updatedAt" to now
                )
            )
        }

        if (categoriesSnap.isEmpty) {
            val defaultCategories = listOf(
                Triple("cat_womens", "Women's Clothing", "preset://dress_collection"),
                Triple("cat_dresses", "Dresses", "preset://dress_collection"),
                Triple("cat_kurtis", "Kurtis", "preset://kurti_ethnic"),
                Triple("cat_tops", "Tops", "preset://hero_boutique"),
                Triple("cat_custom", "Custom Stitching", "preset://custom_stitching"),
                Triple("cat_traditional", "Traditional Wear", "preset://kurti_ethnic"),
                Triple("cat_accessories", "Accessories", "preset://custom_stitching"),
                Triple("cat_new", "New Arrivals", "preset://dress_collection"),
                Triple("cat_bestsellers", "Best Sellers", "preset://kurti_ethnic")
            )
            defaultCategories.forEachIndexed { idx, (id, name, img) ->
                batch.set(
                    db.collection("categories").document(id),
                    mapOf(
                        "name" to name,
                        "description" to "Curated $name handcrafted by StitchByHer atelier.",
                        "imageUrl" to img,
                        "sortOrder" to idx.toLong(),
                        "isVisible" to true,
                        "createdAt" to now,
                        "updatedAt" to now
                    )
                )
            }

            batch.set(
                db.collection("announcements").document("ann_welcome"),
                mapOf(
                    "title" to "Bespoke Tailoring Open",
                    "message" to "Complimentary custom fit adjustments on all Dresses & Kurtis this season.",
                    "badgeText" to "ATELIER",
                    "sortOrder" to 0L,
                    "isActive" to true,
                    "createdAt" to now,
                    "updatedAt" to now
                )
            )
            batch.set(
                db.collection("announcements").document("ann_whatsapp"),
                mapOf(
                    "title" to "Direct Stylist Orders",
                    "message" to "Order directly via WhatsApp for personalized fabric & measurement guidance.",
                    "badgeText" to "CONCIERGE",
                    "sortOrder" to 1L,
                    "isActive" to true,
                    "createdAt" to now,
                    "updatedAt" to now
                )
            )

            batch.set(
                db.collection("banners").document("ban_couture"),
                mapOf(
                    "title" to "The Rose Silk Edit",
                    "subtitle" to "Fluid mulberry silk gowns & custom-fitted evening silhouettes.",
                    "ctaText" to "Shop Dresses",
                    "imageUrl" to "preset://dress_collection",
                    "targetCategoryId" to "cat_dresses",
                    "sortOrder" to 0L,
                    "isActive" to true,
                    "createdAt" to now,
                    "updatedAt" to now
                )
            )

            val starterProducts = listOf(
                mapOf(
                    "id" to "prod_rose_gown",
                    "name" to "Aurelia Blush Silk Gown",
                    "categoryId" to "cat_dresses",
                    "categoryName" to "Dresses",
                    "price" to 4899.0,
                    "originalPrice" to 6299.0,
                    "discountPercent" to 22L,
                    "description" to "Draped bias-cut blush pink silk satin evening gown tailored with an internal bodice lining and graceful flared hem.",
                    "fabricDetails" to "100% Pure Mulberry Silk Satin with soft breathable cotton-voile lining",
                    "specifications" to "Floor-length drape • Concealed side zipper • Custom bust & waist tailoring available",
                    "highlights" to listOf("Hand-finished French seams", "Custom sizing on request", "Includes silk waist tie"),
                    "sizes" to listOf("XS", "S", "M", "L", "XL", "Custom"),
                    "colors" to listOf("Blush Pink", "Ivory Cream", "Obsidian"),
                    "imageUrls" to listOf("preset://dress_collection", "preset://hero_boutique"),
                    "stock" to 14L,
                    "isFeatured" to true,
                    "isBestSeller" to true,
                    "isNewArrival" to true,
                    "isTrending" to true,
                    "isSpecialCollection" to true,
                    "isPublished" to true,
                    "ratingAvg" to 4.9,
                    "reviewCount" to 12L
                ),
                mapOf(
                    "id" to "prod_chikankari_kurti",
                    "name" to "Noor Rose Chikankari Kurti Set",
                    "categoryId" to "cat_kurtis",
                    "categoryName" to "Kurtis",
                    "price" to 3299.0,
                    "originalPrice" to 4199.0,
                    "discountPercent" to 21L,
                    "description" to "Intricate pastel rose hand-embroidered Chikankari straight kurti paired with scalloped organza dupatta and tailored pants.",
                    "fabricDetails" to "Handloom Chanderi Silk-Cotton & Sheer Silk Organza Dupatta",
                    "specifications" to "3-Piece Set (Kurti, Trouser, Dupatta) • Delicate floral resham threadwork",
                    "highlights" to listOf("Authentic hand embroidery", "Breathable all-day lining", "Pearl button placket"),
                    "sizes" to listOf("XS", "S", "M", "L", "XL", "Custom"),
                    "colors" to listOf("Pastel Rose", "Cream Ivory"),
                    "imageUrls" to listOf("preset://kurti_ethnic", "preset://custom_stitching"),
                    "stock" to 18L,
                    "isFeatured" to true,
                    "isBestSeller" to true,
                    "isNewArrival" to true,
                    "isTrending" to false,
                    "isSpecialCollection" to true,
                    "isPublished" to true,
                    "ratingAvg" to 4.8,
                    "reviewCount" to 9L
                ),
                mapOf(
                    "id" to "prod_bespoke_stitching",
                    "name" to "Bespoke Couture Custom Stitching",
                    "categoryId" to "cat_custom",
                    "categoryName" to "Custom Stitching",
                    "price" to 1999.0,
                    "originalPrice" to 2499.0,
                    "discountPercent" to 20L,
                    "description" to "Made-to-measure boutique stitching service crafted to your exact measurements, neckline preference, and sleeve silhouette.",
                    "fabricDetails" to "Complimentary lining & luxury pearl/piping finishing included",
                    "specifications" to "1-on-1 WhatsApp measurement consultation • 5-day atelier turnaround",
                    "highlights" to listOf("Precision bespoke pattern", "Complimentary fit refinement", "Designer neckline catalog"),
                    "sizes" to listOf("Custom"),
                    "colors" to listOf("Blush Pink", "Cream", "Custom Fabric"),
                    "imageUrls" to listOf("preset://custom_stitching", "preset://hero_boutique"),
                    "stock" to 50L,
                    "isFeatured" to true,
                    "isBestSeller" to false,
                    "isNewArrival" to false,
                    "isTrending" to true,
                    "isSpecialCollection" to true,
                    "isPublished" to true,
                    "ratingAvg" to 5.0,
                    "reviewCount" to 24L
                ),
                mapOf(
                    "id" to "prod_pearl_organza_top",
                    "name" to "Camellia Organza Puff-Sleeve Top",
                    "categoryId" to "cat_tops",
                    "categoryName" to "Tops",
                    "price" to 1799.0,
                    "originalPrice" to 2299.0,
                    "discountPercent" to 22L,
                    "description" to "Airy cream silk-organza blouse featuring softly gathered bishop sleeves and delicate blush piping along the sweetheart neckline.",
                    "fabricDetails" to "100% Pure Silk Organza with Satin Camisole Slip",
                    "specifications" to "Includes inner satin slip • Elasticated cuffs • Dry clean recommended",
                    "highlights" to listOf("Detachable satin camisole", "Romantic bishop sleeves", "Zero-sheer layering"),
                    "sizes" to listOf("XS", "S", "M", "L", "XL"),
                    "colors" to listOf("Warm Cream", "Soft Rose"),
                    "imageUrls" to listOf("preset://hero_boutique", "preset://dress_collection"),
                    "stock" to 22L,
                    "isFeatured" to false,
                    "isBestSeller" to true,
                    "isNewArrival" to true,
                    "isTrending" to true,
                    "isSpecialCollection" to false,
                    "isPublished" to true,
                    "ratingAvg" to 4.7,
                    "reviewCount" to 6L
                )
            )

            starterProducts.forEach { prod ->
                val prodId = prod["id"] as String
                val data = prod.toMutableMap().apply {
                    remove("id")
                    put("createdAt", now)
                    put("updatedAt", now)
                }
                batch.set(db.collection("products").document(prodId), data)
            }
        }

        try {
            batch.commit().await()
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "batch_seed", auth)
            throw e
        }
    }

    companion object {
        const val BOOTSTRAPPED_ADMIN_EMAIL = "ahxantalib@gmail.com"
    }
}
