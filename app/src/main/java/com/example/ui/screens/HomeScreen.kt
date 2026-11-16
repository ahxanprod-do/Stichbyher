package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.Announcement
import com.example.data.model.Banner
import com.example.data.model.Category
import com.example.data.model.Product
import com.example.data.model.Review
import com.example.data.model.WebsiteSettings
import com.example.ui.components.AnnouncementGlassBanner
import com.example.ui.components.BoutiqueImage
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.ProductBoutiqueCard
import com.example.ui.theme.BlushPink
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.CreamBackground
import com.example.ui.theme.DeepRose
import com.example.ui.theme.HairlineRose
import com.example.ui.theme.MutedTaupe
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SoftBlushSurface
import com.example.ui.theme.WhatsAppGreen

@Composable
fun HomeScreen(
    settings: WebsiteSettings,
    categories: List<Category>,
    products: List<Product>,
    announcements: List<Announcement>,
    banners: List<Banner>,
    reviews: List<Review>,
    wishlistProductIds: Set<String>,
    isAdmin: Boolean,
    onOpenSearch: () -> Unit,
    onSelectCategory: (String) -> Unit,
    onProductClick: (String) -> Unit,
    onToggleWishlist: (Product) -> Unit,
    onOpenAdmin: () -> Unit,
    onOpenWhatsAppConcierge: () -> Unit
) {
    val publishedProducts = remember(products) { products.filter { it.isPublished } }
    val visibleCategories = remember(categories) {
        categories.filter { it.isVisible }.sortedBy { it.sortOrder }
    }
    val activeBanners = remember(banners) {
        banners.filter { it.isActive }.sortedBy { it.sortOrder }
    }
    val featuredReviews = remember(reviews) {
        val highlighted = reviews.filter { it.isFeaturedOnHome }
        if (highlighted.isNotEmpty()) highlighted else reviews.take(5)
    }

    val sectionList = remember(settings.sectionOrder, settings.hiddenSections) {
        val baseOrder = settings.sectionOrder.ifEmpty { WebsiteSettings.DEFAULT_SECTION_ORDER }
        baseOrder.filterNot { it in settings.hiddenSections }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(CreamBackground),
        contentPadding = PaddingValues(bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // 1. Elegant Liquid Glass Header with Square Logo, Brand Name, Search & Admin button
        item(key = "header") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                LiquidGlassCard(
                    cornerRadius = 22.dp,
                    alpha = 0.88f
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_stitchbyher_logo),
                            contentDescription = "${settings.brandName} Logo",
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, HairlineRose, RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = settings.brandName,
                                style = MaterialTheme.typography.titleLarge,
                                color = ObsidianBlack,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = settings.tagline,
                                style = MaterialTheme.typography.labelSmall,
                                color = DeepRose,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        IconButton(
                            onClick = onOpenSearch,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .clip(CircleShape)
                                .background(SoftBlushSurface)
                                .testTag("home_search_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Search,
                                contentDescription = "Search Boutique",
                                tint = ObsidianBlack
                            )
                        }

                        if (isAdmin) {
                            Spacer(modifier = Modifier.width(6.dp))
                            IconButton(
                                onClick = onOpenAdmin,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .clip(CircleShape)
                                    .background(ObsidianBlack)
                                    .testTag("open_admin_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AdminPanelSettings,
                                    contentDescription = "Admin Studio",
                                    tint = BlushPink
                                )
                            }
                        }
                    }
                }

                // Announcement Banner right below logo & brand-name line
                if (settings.showAnnouncements) {
                    AnnouncementGlassBanner(announcements = announcements)
                }
            }
        }

        // Dynamic Admin-Ordered Sections
        items(sectionList, key = { it }) { sectionKey ->
            when (sectionKey) {
                "hero" -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        HeroEditorialBanner(
                            settings = settings,
                            onExploreClick = onOpenSearch
                        )
                        if (activeBanners.isNotEmpty()) {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(activeBanners, key = { it.id }) { banner ->
                                    PromoBannerCard(
                                        banner = banner,
                                        onClick = {
                                            if (banner.targetCategoryId.isNotBlank()) {
                                                onSelectCategory(banner.targetCategoryId)
                                            } else {
                                                onOpenSearch()
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                "categories" -> {
                    if (visibleCategories.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            SectionHeader(
                                title = "Shop by Category",
                                subtitle = "Curated silhouettes & bespoke tailoring",
                                onSeeAll = onOpenSearch
                            )
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(visibleCategories, key = { it.id }) { category ->
                                    CategoryPillCard(
                                        category = category,
                                        onClick = { onSelectCategory(category.id) }
                                    )
                                }
                            }
                        }
                    }
                }

                "new_arrivals" -> {
                    val list = publishedProducts.filter { it.isNewArrival }
                    if (list.isNotEmpty()) {
                        ProductCarouselSection(
                            title = "New Arrivals",
                            subtitle = "Fresh from the atelier",
                            products = list,
                            wishlistIds = wishlistProductIds,
                            onSeeAll = onOpenSearch,
                            onProductClick = onProductClick,
                            onToggleWishlist = onToggleWishlist
                        )
                    }
                }

                "featured" -> {
                    val list = publishedProducts.filter { it.isFeatured }
                    if (list.isNotEmpty()) {
                        ProductCarouselSection(
                            title = "Featured Creations",
                            subtitle = "Signature hand-finished pieces",
                            products = list,
                            wishlistIds = wishlistProductIds,
                            onSeeAll = onOpenSearch,
                            onProductClick = onProductClick,
                            onToggleWishlist = onToggleWishlist
                        )
                    }
                }

                "best_sellers" -> {
                    val list = publishedProducts.filter { it.isBestSeller }
                    if (list.isNotEmpty()) {
                        ProductCarouselSection(
                            title = "Best Sellers",
                            subtitle = "Most adored by StitchByHer patrons",
                            products = list,
                            wishlistIds = wishlistProductIds,
                            onSeeAll = onOpenSearch,
                            onProductClick = onProductClick,
                            onToggleWishlist = onToggleWishlist
                        )
                    }
                }

                "trending" -> {
                    val list = publishedProducts.filter { it.isTrending }
                    if (list.isNotEmpty()) {
                        ProductCarouselSection(
                            title = "Trending Collection",
                            subtitle = "In-season drapes & statement sleeves",
                            products = list,
                            wishlistIds = wishlistProductIds,
                            onSeeAll = onOpenSearch,
                            onProductClick = onProductClick,
                            onToggleWishlist = onToggleWishlist
                        )
                    }
                }

                "special" -> {
                    val list = publishedProducts.filter { it.isSpecialCollection }
                    if (list.isNotEmpty()) {
                        ProductCarouselSection(
                            title = "Special Collection",
                            subtitle = "Limited festive & bridal craftsmanship",
                            products = list,
                            wishlistIds = wishlistProductIds,
                            onSeeAll = onOpenSearch,
                            onProductClick = onProductClick,
                            onToggleWishlist = onToggleWishlist
                        )
                    }
                }

                "recent" -> {
                    if (publishedProducts.isNotEmpty()) {
                        ProductCarouselSection(
                            title = "Recently Added",
                            subtitle = "Explore our full boutique edit",
                            products = publishedProducts.take(8),
                            wishlistIds = wishlistProductIds,
                            onSeeAll = onOpenSearch,
                            onProductClick = onProductClick,
                            onToggleWishlist = onToggleWishlist
                        )
                    }
                }

                "reviews" -> {
                    if (settings.showReviews && featuredReviews.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            SectionHeader(
                                title = "Patron Love",
                                subtitle = "Verified boutique reviews",
                                onSeeAll = null
                            )
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(featuredReviews, key = { it.id }) { review ->
                                    ReviewGlassCard(review = review)
                                }
                            }
                        }
                    }
                }

                "about" -> {
                    if (settings.showAbout) {
                        AboutAtelierSection(settings = settings)
                    }
                }

                "contact" -> {
                    if (settings.showContact) {
                        ContactAndFooterSection(
                            settings = settings,
                            onOpenWhatsAppConcierge = onOpenWhatsAppConcierge
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroEditorialBanner(
    settings: WebsiteSettings,
    onExploreClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(260.dp)
            .clip(RoundedCornerShape(28.dp))
            .clickable(onClick = onExploreClick)
    ) {
        BoutiqueImage(
            source = settings.heroImageUrl,
            contentDescription = settings.heroTitle,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            ObsidianBlack.copy(alpha = 0.72f)
                        )
                    )
                )
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = settings.heroTitle,
                style = MaterialTheme.typography.headlineLarge,
                color = PureWhite,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (settings.heroSubtitle.isNotBlank()) {
                Text(
                    text = settings.heroSubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = PureWhite.copy(alpha = 0.9f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Button(
                onClick = onExploreClick,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BlushPink,
                    contentColor = ObsidianBlack
                ),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
                modifier = Modifier.testTag("hero_cta_button")
            ) {
                Text(
                    text = settings.heroCtaText,
                    style = MaterialTheme.typography.labelLarge
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun PromoBannerCard(
    banner: Banner,
    onClick: () -> Unit
) {
    LiquidGlassCard(
        modifier = Modifier.width(280.dp),
        cornerRadius = 20.dp,
        alpha = 0.85f,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BoutiqueImage(
                source = banner.imageUrl,
                contentDescription = banner.title,
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(14.dp))
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = banner.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = ObsidianBlack,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (banner.subtitle.isNotBlank()) {
                    Text(
                        text = banner.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedTaupe,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (banner.ctaText.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${banner.ctaText} →",
                        style = MaterialTheme.typography.labelMedium,
                        color = DeepRose
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryPillCard(
    category: Category,
    onClick: () -> Unit
) {
    LiquidGlassCard(
        modifier = Modifier
            .width(114.dp)
            .testTag("category_card_${category.id}"),
        cornerRadius = 20.dp,
        alpha = 0.85f,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            BoutiqueImage(
                source = category.imageUrl,
                contentDescription = category.name,
                modifier = Modifier
                    .size(86.dp)
                    .clip(RoundedCornerShape(15.dp))
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = category.name,
                style = MaterialTheme.typography.labelMedium,
                color = ObsidianBlack,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    subtitle: String,
    onSeeAll: (() -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = ObsidianBlack
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MutedTaupe
            )
        }
        if (onSeeAll != null) {
            TextButton(onClick = onSeeAll) {
                Text(
                    text = "See All",
                    style = MaterialTheme.typography.labelLarge,
                    color = DeepRose
                )
            }
        }
    }
}

@Composable
private fun ProductCarouselSection(
    title: String,
    subtitle: String,
    products: List<Product>,
    wishlistIds: Set<String>,
    onSeeAll: () -> Unit,
    onProductClick: (String) -> Unit,
    onToggleWishlist: (Product) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader(
            title = title,
            subtitle = subtitle,
            onSeeAll = onSeeAll
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(products, key = { it.id }) { product ->
                ProductBoutiqueCard(
                    product = product,
                    isWishlisted = product.id in wishlistIds,
                    onClick = { onProductClick(product.id) },
                    onToggleWishlist = { onToggleWishlist(product) },
                    modifier = Modifier.width(178.dp)
                )
            }
        }
    }
}

@Composable
private fun ReviewGlassCard(review: Review) {
    LiquidGlassCard(
        modifier = Modifier.width(260.dp),
        cornerRadius = 20.dp,
        alpha = 0.84f
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                repeat(5) { idx ->
                    Icon(
                        imageVector = Icons.Rounded.Star,
                        contentDescription = null,
                        tint = if (idx < review.rating) ChampagneGold else HairlineRose,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Text(
                text = "\"${review.comment}\"",
                style = MaterialTheme.typography.bodyMedium,
                color = ObsidianBlack,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "— ${review.userName}",
                style = MaterialTheme.typography.labelMedium,
                color = DeepRose
            )
        }
    }
}

@Composable
private fun AboutAtelierSection(settings: WebsiteSettings) {
    LiquidGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        cornerRadius = 24.dp,
        alpha = 0.84f
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = settings.aboutTitle,
                style = MaterialTheme.typography.headlineMedium,
                color = ObsidianBlack
            )
            Text(
                text = settings.aboutDescription,
                style = MaterialTheme.typography.bodyMedium,
                color = MutedTaupe
            )
            HorizontalDivider(color = HairlineRose)
            if (settings.shippingPolicy.isNotBlank()) {
                Text(
                    text = "Shipping: ${settings.shippingPolicy}",
                    style = MaterialTheme.typography.bodySmall,
                    color = ObsidianBlack
                )
            }
            if (settings.returnPolicy.isNotBlank()) {
                Text(
                    text = "Fit & Returns: ${settings.returnPolicy}",
                    style = MaterialTheme.typography.bodySmall,
                    color = ObsidianBlack
                )
            }
        }
    }
}

@Composable
private fun ContactAndFooterSection(
    settings: WebsiteSettings,
    onOpenWhatsAppConcierge: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        LiquidGlassCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 24.dp,
            alpha = 0.86f
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Boutique Concierge",
                    style = MaterialTheme.typography.headlineMedium,
                    color = ObsidianBlack
                )
                ContactRow(Icons.Rounded.Phone, settings.contactPhone)
                ContactRow(Icons.Rounded.Email, settings.contactEmail)
                ContactRow(Icons.Rounded.LocationOn, settings.contactAddress)
                if (settings.instagramHandle.isNotBlank()) {
                    ContactRow(Icons.Rounded.ChatBubbleOutline, "Instagram ${settings.instagramHandle}")
                }

                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = onOpenWhatsAppConcierge,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WhatsAppGreen,
                        contentColor = PureWhite
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text(
                        text = "Chat with StitchByHer on WhatsApp",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }

        if (settings.footerText.isNotBlank()) {
            Surface(
                color = SoftBlushSurface,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = settings.footerText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedTaupe,
                    modifier = Modifier.padding(14.dp)
                )
            }
        }
    }
}

@Composable
private fun ContactRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    if (text.isBlank()) return
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = DeepRose,
            modifier = Modifier.size(17.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = ObsidianBlack
        )
    }
}
