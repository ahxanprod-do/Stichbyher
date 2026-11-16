package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.Product
import com.example.data.model.Review
import com.example.data.model.UserProfile
import com.example.ui.components.BoutiqueImage
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.ProductBoutiqueCard
import com.example.ui.components.StatusPill
import com.example.ui.components.formatInr
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
import com.example.ui.theme.WhatsAppGreen
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    product: Product,
    relatedProducts: List<Product>,
    reviews: List<Review>,
    userProfile: UserProfile?,
    isWishlisted: Boolean,
    wishlistIds: Set<String>,
    onBack: () -> Unit,
    onToggleWishlist: (Product) -> Unit,
    onProductClick: (String) -> Unit,
    onSubmitReview: (Int, String) -> Unit,
    onConfirmWhatsAppOrder: (
        size: String,
        color: String,
        quantity: Int,
        customerName: String,
        customerPhone: String,
        deliveryAddress: String,
        notes: String
    ) -> Unit
) {
    val images = remember(product.imageUrls) {
        product.imageUrls.ifEmpty { listOf("preset://dress_collection") }
    }
    val pagerState = rememberPagerState(pageCount = { images.size })
    var selectedSize by remember(product.id) {
        mutableStateOf(product.sizes.firstOrNull() ?: "Standard")
    }
    var selectedColor by remember(product.id) {
        mutableStateOf(product.colors.firstOrNull() ?: "Signature")
    }
    var quantity by remember(product.id) { mutableIntStateOf(1) }
    var fullScreenViewerIndex by remember { mutableStateOf<Int?>(null) }
    var showWhatsAppOrderSheet by remember { mutableStateOf(false) }
    var showReviewSheet by remember { mutableStateOf(false) }

    val productReviews = remember(reviews, product.id) {
        reviews.filter { it.productId == product.id }
    }

    Scaffold(
        containerColor = CreamBackground,
        bottomBar = {
            LiquidGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                cornerRadius = 24.dp,
                alpha = 0.94f
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { onToggleWishlist(product) },
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.2.dp, if (isWishlisted) DeepRose else HairlineRose),
                        modifier = Modifier
                            .height(50.dp)
                            .testTag("wishlist_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (isWishlisted) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            contentDescription = "Wishlist",
                            tint = if (isWishlisted) DeepRose else ObsidianBlack
                        )
                    }

                    Button(
                        onClick = { showWhatsAppOrderSheet = true },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = WhatsAppGreen,
                            contentColor = PureWhite
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("order_now_button")
                    ) {
                        Text(
                            text = "Order Now • ${formatInr(product.price * quantity)}",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding()),
            contentPadding = PaddingValues(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Swipeable Image Gallery Header
            item(key = "gallery") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(410.dp)
                ) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        BoutiqueImage(
                            source = images[page],
                            contentDescription = "${product.name} photo ${page + 1}",
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable { fullScreenViewerIndex = page }
                        )
                    }

                    // Top Navigation Overlay
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .clip(CircleShape)
                                .background(PureWhite.copy(alpha = 0.88f))
                                .testTag("product_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "Back",
                                tint = ObsidianBlack
                            )
                        }

                        IconButton(
                            onClick = { fullScreenViewerIndex = pagerState.currentPage },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .clip(CircleShape)
                                .background(PureWhite.copy(alpha = 0.88f))
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Fullscreen,
                                contentDescription = "Full-screen viewer",
                                tint = ObsidianBlack
                            )
                        }
                    }

                    // Pager Dots
                    if (images.size > 1) {
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 14.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(ObsidianBlack.copy(alpha = 0.45f))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            repeat(images.size) { idx ->
                                Box(
                                    modifier = Modifier
                                        .size(if (pagerState.currentPage == idx) 8.dp else 6.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (pagerState.currentPage == idx) BlushPink else PureWhite.copy(alpha = 0.55f)
                                        )
                                )
                            }
                        }
                    }
                }
            }

            // Title, Pricing & Stock
            item(key = "info") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatusPill(text = product.categoryName, color = DeepRose)
                        StatusPill(
                            text = if (product.stock > 0L) "In Stock (${product.stock} ready)" else "Made to Order",
                            color = if (product.stock > 0L) EmeraldSage else ChampagneGold
                        )
                    }

                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.headlineLarge,
                        color = ObsidianBlack
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = formatInr(product.price),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = ObsidianBlack
                        )
                        if (product.originalPrice > product.price) {
                            Text(
                                text = formatInr(product.originalPrice),
                                style = MaterialTheme.typography.titleMedium,
                                color = MutedTaupe,
                                textDecoration = TextDecoration.LineThrough
                            )
                        }
                        if (product.discountPercent > 0L) {
                            StatusPill(text = "${product.discountPercent}% OFF", color = EmeraldSage)
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Star,
                            contentDescription = null,
                            tint = ChampagneGold,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${String.format(Locale.US, "%.1f", product.ratingAvg)} (${productReviews.size.coerceAtLeast(product.reviewCount.toInt())} reviews)",
                            style = MaterialTheme.typography.labelLarge,
                            color = ObsidianBlack
                        )
                    }

                    Text(
                        text = product.description,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MutedTaupe
                    )
                }
            }

            // Size, Color & Quantity Selectors
            item(key = "variants") {
                LiquidGlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    cornerRadius = 22.dp,
                    alpha = 0.88f
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        if (product.sizes.isNotEmpty()) {
                            Text("Select Size", style = MaterialTheme.typography.titleSmall, color = ObsidianBlack)
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                product.sizes.forEach { size ->
                                    FilterChip(
                                        selected = selectedSize == size,
                                        onClick = { selectedSize = size },
                                        label = { Text(size) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = ObsidianBlack,
                                            selectedLabelColor = PureWhite
                                        )
                                    )
                                }
                            }
                        }

                        if (product.colors.isNotEmpty()) {
                            Text("Select Shade", style = MaterialTheme.typography.titleSmall, color = ObsidianBlack)
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                product.colors.forEach { color ->
                                    FilterChip(
                                        selected = selectedColor == color,
                                        onClick = { selectedColor = color },
                                        label = { Text(color) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = BlushPink,
                                            selectedLabelColor = ObsidianBlack
                                        )
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Quantity", style = MaterialTheme.typography.titleSmall, color = ObsidianBlack)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                IconButton(
                                    onClick = { if (quantity > 1) quantity-- },
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .clip(CircleShape)
                                        .background(SoftBlushSurface)
                                ) {
                                    Icon(Icons.Rounded.Remove, contentDescription = "Decrease quantity")
                                }
                                Text(
                                    text = quantity.toString(),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = ObsidianBlack
                                )
                                IconButton(
                                    onClick = { if (quantity < 20) quantity++ },
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .clip(CircleShape)
                                        .background(SoftBlushSurface)
                                ) {
                                    Icon(Icons.Rounded.Add, contentDescription = "Increase quantity")
                                }
                            }
                        }
                    }
                }
            }

            // Fabric, Specifications & Highlights
            item(key = "specs") {
                LiquidGlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    cornerRadius = 22.dp,
                    alpha = 0.85f
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Fabric & Craftsmanship",
                            style = MaterialTheme.typography.titleLarge,
                            color = ObsidianBlack
                        )
                        if (product.fabricDetails.isNotBlank()) {
                            Text(
                                text = "Fabric: ${product.fabricDetails}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = ObsidianBlack
                            )
                        }
                        if (product.specifications.isNotBlank()) {
                            Text(
                                text = "Specifications: ${product.specifications}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MutedTaupe
                            )
                        }
                        if (product.highlights.isNotEmpty()) {
                            HorizontalDivider(color = HairlineRose)
                            product.highlights.forEach { highlight ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.CheckCircle,
                                        contentDescription = null,
                                        tint = DeepRose,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = highlight,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = ObsidianBlack
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Customer Reviews
            item(key = "reviews") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Customer Reviews (${productReviews.size})",
                            style = MaterialTheme.typography.titleLarge,
                            color = ObsidianBlack
                        )
                        TextButton(onClick = { showReviewSheet = true }) {
                            Text("Write a Review", color = DeepRose)
                        }
                    }

                    if (productReviews.isEmpty()) {
                        Surface(
                            color = SoftBlushSurface,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Be the first patron to review ${product.name}.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MutedTaupe,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    } else {
                        productReviews.forEach { review ->
                            LiquidGlassCard(cornerRadius = 18.dp, alpha = 0.85f) {
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
                                        Text(review.userName, style = MaterialTheme.typography.titleSmall)
                                        Row {
                                            repeat(5) { idx ->
                                                Icon(
                                                    imageVector = Icons.Rounded.Star,
                                                    contentDescription = null,
                                                    tint = if (idx < review.rating) ChampagneGold else HairlineRose,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    }
                                    Text(review.comment, style = MaterialTheme.typography.bodyMedium, color = MutedTaupe)
                                }
                            }
                        }
                    }
                }
            }

            // Related Products
            if (relatedProducts.isNotEmpty()) {
                item(key = "related") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "You May Also Adore",
                            style = MaterialTheme.typography.headlineMedium,
                            color = ObsidianBlack,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(relatedProducts, key = { it.id }) { related ->
                                ProductBoutiqueCard(
                                    product = related,
                                    isWishlisted = related.id in wishlistIds,
                                    onClick = { onProductClick(related.id) },
                                    onToggleWishlist = { onToggleWishlist(related) },
                                    modifier = Modifier.width(172.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Full-Screen Image Viewer
    val viewerPage = fullScreenViewerIndex
    if (viewerPage != null) {
        Dialog(
            onDismissRequest = { fullScreenViewerIndex = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            val viewerPager = rememberPagerState(initialPage = viewerPage, pageCount = { images.size })
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ObsidianBlack)
            ) {
                HorizontalPager(
                    state = viewerPager,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    var scale by remember { mutableFloatStateOf(1f) }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTransformGestures { _, _, zoom, _ ->
                                    scale = (scale * zoom).coerceIn(1f, 3f)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        BoutiqueImage(
                            source = images[page],
                            contentDescription = "Zoomable product view",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer(scaleX = scale, scaleY = scale)
                        )
                    }
                }
                IconButton(
                    onClick = { fullScreenViewerIndex = null },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(16.dp)
                        .clip(CircleShape)
                        .background(PureWhite.copy(alpha = 0.2f))
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close viewer", tint = PureWhite)
                }
            }
        }
    }

    // WhatsApp Order Confirmation Bottom Sheet
    if (showWhatsAppOrderSheet) {
        ModalBottomSheet(
            onDismissRequest = { showWhatsAppOrderSheet = false },
            containerColor = CreamBackground
        ) {
            var customerName by remember(userProfile) {
                mutableStateOf(userProfile?.fullName.orEmpty())
            }
            var customerPhone by remember(userProfile) {
                mutableStateOf(userProfile?.phoneNumber.orEmpty())
            }
            var deliveryAddress by remember(userProfile) {
                mutableStateOf(userProfile?.defaultAddress.orEmpty())
            }
            var notes by remember { mutableStateOf("") }
            var validationError by remember { mutableStateOf<String?>(null) }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Confirm WhatsApp Enquiry",
                    style = MaterialTheme.typography.headlineMedium,
                    color = ObsidianBlack
                )
                Text(
                    text = "${product.name} • Size: $selectedSize • Color: $selectedColor • Qty: $quantity",
                    style = MaterialTheme.typography.bodyMedium,
                    color = DeepRose
                )

                OutlinedTextField(
                    value = customerName,
                    onValueChange = { customerName = it },
                    label = { Text("Full Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = customerPhone,
                    onValueChange = { customerPhone = it },
                    label = { Text("Phone Number *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                val savedAddresses = userProfile?.savedAddresses.orEmpty()
                if (savedAddresses.isNotEmpty()) {
                    Text("Saved Addresses", style = MaterialTheme.typography.labelMedium, color = MutedTaupe)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(savedAddresses) { addr ->
                            FilterChip(
                                selected = deliveryAddress == addr,
                                onClick = { deliveryAddress = addr },
                                label = { Text(addr.take(28)) }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = deliveryAddress,
                    onValueChange = { deliveryAddress = it },
                    label = { Text("Delivery Address *") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Custom Stitching / Fit Notes (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )

                if (validationError != null) {
                    Text(
                        text = validationError.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = CrimsonError
                    )
                }

                Surface(
                    color = SoftBlushSurface,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "You must press Send inside WhatsApp to deliver your order enquiry. Orders are confirmed only after the atelier replies.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedTaupe,
                        modifier = Modifier.padding(12.dp)
                    )
                }

                Button(
                    onClick = {
                        when {
                            customerName.trim().length < 2 -> validationError = "Please enter your full name."
                            customerPhone.trim().length < 7 -> validationError = "Please enter a valid phone number."
                            deliveryAddress.trim().length < 5 -> validationError = "Please enter your delivery address."
                            else -> {
                                validationError = null
                                showWhatsAppOrderSheet = false
                                onConfirmWhatsAppOrder(
                                    selectedSize,
                                    selectedColor,
                                    quantity,
                                    customerName,
                                    customerPhone,
                                    deliveryAddress,
                                    notes
                                )
                            }
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WhatsAppGreen,
                        contentColor = PureWhite
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("confirm_whatsapp_order_button")
                ) {
                    Text("Open WhatsApp & Send Enquiry", style = MaterialTheme.typography.labelLarge)
                }
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }

    // Write Review Modal
    if (showReviewSheet) {
        ModalBottomSheet(
            onDismissRequest = { showReviewSheet = false },
            containerColor = CreamBackground
        ) {
            var rating by remember { mutableIntStateOf(5) }
            var comment by remember { mutableStateOf("") }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Review ${product.name}", style = MaterialTheme.typography.headlineMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    (1..5).forEach { star ->
                        IconButton(onClick = { rating = star }) {
                            Icon(
                                imageVector = if (star <= rating) Icons.Rounded.Star else Icons.Rounded.StarOutline,
                                contentDescription = "$star stars",
                                tint = ChampagneGold,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("Share your thoughts on fabric, fit & finish") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {
                        if (comment.trim().isNotBlank()) {
                            onSubmitReview(rating, comment.trim())
                            showReviewSheet = false
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ObsidianBlack, contentColor = PureWhite),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text("Submit Review")
                }
            }
        }
    }
}
