package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val fullName: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val photoUrl: String? = null,
    val savedAddresses: List<String> = emptyList(),
    val defaultAddress: String = "",
    val isBlocked: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class WishlistItem(
    @DocumentId val id: String = "",
    val productId: String = "",
    val productName: String = "",
    val price: Double = 0.0,
    val imageUrl: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class AdminRole(
    @DocumentId val id: String = "",
    val uid: String = "",
    val email: String = "",
    val addedBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Category(
    @DocumentId val id: String = "",
    val name: String = "",
    val description: String = "",
    val imageUrl: String? = null,
    val sortOrder: Long = 0L,
    val isVisible: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Product(
    @DocumentId val id: String = "",
    val name: String = "",
    val categoryId: String = "",
    val categoryName: String = "",
    val price: Double = 0.0,
    val originalPrice: Double = 0.0,
    val discountPercent: Long = 0L,
    val description: String = "",
    val fabricDetails: String = "",
    val specifications: String = "",
    val highlights: List<String> = emptyList(),
    val sizes: List<String> = emptyList(),
    val colors: List<String> = emptyList(),
    val imageUrls: List<String> = emptyList(),
    val stock: Long = 0L,
    val isFeatured: Boolean = false,
    val isBestSeller: Boolean = false,
    val isNewArrival: Boolean = false,
    val isTrending: Boolean = false,
    val isSpecialCollection: Boolean = false,
    val isPublished: Boolean = true,
    val ratingAvg: Double = 5.0,
    val reviewCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class OrderEnquiry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val customerName: String = "",
    val customerPhone: String = "",
    val deliveryAddress: String = "",
    val productId: String = "",
    val productName: String = "",
    val productImageUrl: String? = null,
    val selectedSize: String = "",
    val selectedColor: String = "",
    val quantity: Long = 1L,
    val unitPrice: Double = 0.0,
    val totalPrice: Double = 0.0,
    val status: String = "Pending",
    val channel: String = "WhatsApp Enquiry",
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Announcement(
    @DocumentId val id: String = "",
    val title: String = "",
    val message: String = "",
    val badgeText: String = "",
    val sortOrder: Long = 0L,
    val isActive: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Banner(
    @DocumentId val id: String = "",
    val title: String = "",
    val subtitle: String = "",
    val ctaText: String = "",
    val imageUrl: String? = null,
    val targetCategoryId: String = "",
    val sortOrder: Long = 0L,
    val isActive: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Review(
    @DocumentId val id: String = "",
    val productId: String = "",
    val userId: String = "",
    val userName: String = "",
    val rating: Long = 5L,
    val comment: String = "",
    val isFeaturedOnHome: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class WebsiteSettings(
    @DocumentId val id: String = "store",
    val brandName: String = "StitchByHer",
    val tagline: String = "Stitched With Passion, Made For You",
    val heroTitle: String = "Couture Crafted Around Your Grace",
    val heroSubtitle: String = "Hand-tailored silhouettes, delicate embroidery, and bespoke custom stitching in blush, cream & silk.",
    val heroCtaText: String = "Explore Collection",
    val heroImageUrl: String? = "preset://hero_boutique",
    val whatsappNumber: String = "9304560827",
    val aboutTitle: String = "About StitchByHer",
    val aboutDescription: String = "Born from a love of timeless femininity and artisan tailoring, StitchByHer crafts made-for-you dresses, embroidered kurtis, and bespoke couture pieces with delicate finishing.",
    val contactEmail: String = "atelier@stitchbyher.com",
    val contactPhone: String = "+91 9304560827",
    val contactAddress: String = "Atelier Studio, Rose Courtyard, India",
    val instagramHandle: String = "@stitchbyher.atelier",
    val footerText: String = "StitchByHer Atelier • Stitched With Passion, Made For You",
    val shippingPolicy: String = "Made-to-order tailoring dispatches in 4-7 business days with insured doorstep delivery.",
    val returnPolicy: String = "Complimentary fit alterations within 7 days of delivery; custom stitched orders eligible for atelier fit care.",
    val primaryColorHex: String = "#E8B7BE",
    val accentColorHex: String = "#D9A0AA",
    val sectionOrder: List<String> = DEFAULT_SECTION_ORDER,
    val hiddenSections: List<String> = emptyList(),
    val showAnnouncements: Boolean = true,
    val showReviews: Boolean = true,
    val showAbout: Boolean = true,
    val showContact: Boolean = true,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    companion object {
        val DEFAULT_SECTION_ORDER = listOf(
            "hero",
            "categories",
            "new_arrivals",
            "featured",
            "best_sellers",
            "trending",
            "special",
            "recent",
            "reviews",
            "about",
            "contact"
        )
    }
}
