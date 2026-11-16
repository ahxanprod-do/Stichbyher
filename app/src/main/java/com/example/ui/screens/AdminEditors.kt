package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.Announcement
import com.example.data.model.Banner
import com.example.data.model.Category
import com.example.data.model.OrderEnquiry
import com.example.data.model.Product
import com.example.ui.components.BoutiqueImage
import com.example.ui.components.encodeUriToCompressedDataUri
import com.example.ui.theme.BlushPink
import com.example.ui.theme.CreamBackground
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.DeepRose
import com.example.ui.theme.HairlineRose
import com.example.ui.theme.MutedTaupe
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.PureWhite
import kotlinx.coroutines.launch

private val STUDIO_PRESETS = listOf(
    "preset://dress_collection" to "Rose Gown",
    "preset://kurti_ethnic" to "Chikankari",
    "preset://custom_stitching" to "Tailoring",
    "preset://hero_boutique" to "Atelier Rack"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProductEditorDialog(
    initial: Product?,
    categories: List<Category>,
    onDismiss: () -> Unit,
    onSave: (Product, (Result<String>) -> Unit) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var selectedCategory by remember {
        mutableStateOf(
            categories.find { it.id == initial?.categoryId } ?: categories.firstOrNull()
        )
    }
    LaunchedEffect(categories) {
        if (selectedCategory == null && categories.isNotEmpty()) {
            selectedCategory = categories.find { it.id == initial?.categoryId } ?: categories.firstOrNull()
        }
    }
    var priceText by remember { mutableStateOf(initial?.price?.takeIf { it > 0 }?.toInt()?.toString().orEmpty()) }
    var originalPriceText by remember { mutableStateOf(initial?.originalPrice?.takeIf { it > 0 }?.toInt()?.toString().orEmpty()) }
    var stockText by remember { mutableStateOf((initial?.stock ?: 10L).toString()) }
    var description by remember { mutableStateOf(initial?.description.orEmpty()) }
    var fabricDetails by remember { mutableStateOf(initial?.fabricDetails.orEmpty()) }
    var specifications by remember { mutableStateOf(initial?.specifications.orEmpty()) }
    var sizesText by remember { mutableStateOf(initial?.sizes?.joinToString(", ") ?: "XS, S, M, L, XL, Custom") }
    var colorsText by remember { mutableStateOf(initial?.colors?.joinToString(", ") ?: "Blush Pink, Cream") }
    var highlightsText by remember { mutableStateOf(initial?.highlights?.joinToString(", ") ?: "Hand-finished, Custom fit") }
    val imageUrls = remember {
        mutableStateListOf<String>().apply {
            addAll(initial?.imageUrls ?: listOf("preset://dress_collection"))
        }
    }
    var customImageUrl by remember { mutableStateOf("") }
    var isFeatured by remember { mutableStateOf(initial?.isFeatured ?: true) }
    var isBestSeller by remember { mutableStateOf(initial?.isBestSeller ?: false) }
    var isNewArrival by remember { mutableStateOf(initial?.isNewArrival ?: true) }
    var isTrending by remember { mutableStateOf(initial?.isTrending ?: false) }
    var isSpecialCollection by remember { mutableStateOf(initial?.isSpecialCollection ?: false) }
    var isPublished by remember { mutableStateOf(initial?.isPublished ?: true) }
    var isProcessingImages by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 5)
    ) { uris ->
        if (uris.isNotEmpty()) {
            scope.launch {
                isProcessingImages = true
                errorText = null
                try {
                    uris.forEach { uri ->
                        if (imageUrls.size < 8) {
                            val dataUri = encodeUriToCompressedDataUri(context, uri)
                            if (dataUri != null) {
                                imageUrls.add(dataUri)
                            }
                        }
                    }
                } finally {
                    isProcessingImages = false
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = CreamBackground,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (initial == null) "Add Boutique Creation" else "Edit Product",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close")
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Product Name *") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Category", style = MaterialTheme.typography.labelMedium, color = MutedTaupe)
                if (categories.isEmpty()) {
                    Text(
                        text = "Using default category: Women's Clothing (create custom categories in Categories tab).",
                        style = MaterialTheme.typography.bodySmall,
                        color = DeepRose
                    )
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(categories, key = { it.id }) { cat ->
                            FilterChip(
                                selected = selectedCategory?.id == cat.id,
                                onClick = { selectedCategory = cat },
                                label = { Text(cat.name) }
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = priceText,
                        onValueChange = { priceText = it.filter { c -> c.isDigit() } },
                        label = { Text("Price (₹) *") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = originalPriceText,
                        onValueChange = { originalPriceText = it.filter { c -> c.isDigit() } },
                        label = { Text("MRP (₹)") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = stockText,
                        onValueChange = { stockText = it.filter { c -> c.isDigit() } },
                        label = { Text("Stock") },
                        modifier = Modifier.weight(0.8f)
                    )
                }

                // Multi-Image Gallery Management
                Text("Product Images (${imageUrls.size}/8)", style = MaterialTheme.typography.labelMedium, color = MutedTaupe)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(imageUrls.indices.toList()) { idx ->
                        Box(modifier = Modifier.size(84.dp)) {
                            BoutiqueImage(
                                source = imageUrls[idx],
                                contentDescription = "Product image ${idx + 1}",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(14.dp))
                            )
                            IconButton(
                                onClick = { imageUrls.removeAt(idx) },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(ObsidianBlack.copy(alpha = 0.75f))
                            ) {
                                Icon(Icons.Rounded.Close, contentDescription = "Remove image", tint = PureWhite, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            imagePicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        enabled = !isProcessingImages && !isSaving,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isProcessingImages) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Optimizing...")
                        } else {
                            Icon(Icons.Rounded.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Pick Photos")
                        }
                    }
                }

                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    STUDIO_PRESETS.forEach { (uri, label) ->
                        FilterChip(
                            selected = uri in imageUrls,
                            onClick = {
                                if (uri !in imageUrls && imageUrls.size < 8) imageUrls.add(uri)
                            },
                            label = { Text("+ $label") }
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = customImageUrl,
                        onValueChange = { customImageUrl = it },
                        label = { Text("Or paste HTTPS image URL") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            val clean = customImageUrl.trim()
                            if (clean.isNotBlank() && imageUrls.size < 8) {
                                imageUrls.add(clean)
                                customImageUrl = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ObsidianBlack)
                    ) {
                        Text("Add")
                    }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = fabricDetails,
                    onValueChange = { fabricDetails = it },
                    label = { Text("Fabric Details") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = specifications,
                    onValueChange = { specifications = it },
                    label = { Text("Specifications") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = sizesText,
                    onValueChange = { sizesText = it },
                    label = { Text("Sizes (comma-separated)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = colorsText,
                    onValueChange = { colorsText = it },
                    label = { Text("Colors (comma-separated)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = highlightsText,
                    onValueChange = { highlightsText = it },
                    label = { Text("Highlights (comma-separated)") },
                    modifier = Modifier.fillMaxWidth()
                )

                AdminSwitchRow("Published (Visible to Shoppers)", isPublished) { isPublished = it }
                AdminSwitchRow("Featured Creation", isFeatured) { isFeatured = it }
                AdminSwitchRow("New Arrival", isNewArrival) { isNewArrival = it }
                AdminSwitchRow("Best Seller", isBestSeller) { isBestSeller = it }
                AdminSwitchRow("Trending Collection", isTrending) { isTrending = it }
                AdminSwitchRow("Special Collection", isSpecialCollection) { isSpecialCollection = it }

                if (!errorText.isNullOrBlank()) {
                    Surface(
                        color = CrimsonError.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorText.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = CrimsonError,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                Button(
                    onClick = {
                        val cleanName = name.trim()
                        val parsedPrice = priceText.toDoubleOrNull()
                        if (cleanName.isBlank()) {
                            errorText = "Please enter a Product Name."
                            return@Button
                        }
                        if (parsedPrice == null || parsedPrice < 0.0) {
                            errorText = "Please enter a valid Price (₹)."
                            return@Button
                        }
                        val parsedOriginal = (originalPriceText.toDoubleOrNull() ?: parsedPrice).coerceAtLeast(parsedPrice)
                        val parsedStock = stockText.toLongOrNull() ?: 10L
                        val resolvedCatId = selectedCategory?.id?.takeIf { it.isNotBlank() }
                            ?: initial?.categoryId?.takeIf { it.isNotBlank() }
                            ?: "cat_general"
                        val resolvedCatName = selectedCategory?.name?.takeIf { it.isNotBlank() }
                            ?: initial?.categoryName?.takeIf { it.isNotBlank() }
                            ?: "Women's Clothing"

                        errorText = null
                        isSaving = true
                        onSave(
                            (initial ?: Product()).copy(
                                name = cleanName,
                                categoryId = resolvedCatId,
                                categoryName = resolvedCatName,
                                price = parsedPrice,
                                originalPrice = parsedOriginal,
                                stock = parsedStock,
                                description = description.trim(),
                                fabricDetails = fabricDetails.trim(),
                                specifications = specifications.trim(),
                                sizes = sizesText.split(",").map { it.trim() }.filter { it.isNotBlank() },
                                colors = colorsText.split(",").map { it.trim() }.filter { it.isNotBlank() },
                                highlights = highlightsText.split(",").map { it.trim() }.filter { it.isNotBlank() },
                                imageUrls = imageUrls.toList(),
                                isFeatured = isFeatured,
                                isBestSeller = isBestSeller,
                                isNewArrival = isNewArrival,
                                isTrending = isTrending,
                                isSpecialCollection = isSpecialCollection,
                                isPublished = isPublished
                            )
                        ) { result ->
                            isSaving = false
                            result.onSuccess {
                                onDismiss()
                            }.onFailure { err ->
                                errorText = err.localizedMessage ?: "Failed to save product. Please verify internet and admin access."
                            }
                        }
                    },
                    enabled = !isSaving && !isProcessingImages,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ObsidianBlack, contentColor = BlushPink),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = BlushPink,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Saving to Live Store...")
                    } else {
                        Text("Save Product to Live Store")
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryEditorDialog(
    initial: Category?,
    nextOrder: Long,
    onDismiss: () -> Unit,
    onSave: (Category, (Result<String>) -> Unit) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var description by remember { mutableStateOf(initial?.description.orEmpty()) }
    var imageUrl by remember { mutableStateOf(initial?.imageUrl ?: "preset://dress_collection") }
    var isVisible by remember { mutableStateOf(initial?.isVisible ?: true) }
    var isSaving by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                encodeUriToCompressedDataUri(context, uri)?.let { imageUrl = it }
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(color = CreamBackground, shape = RoundedCornerShape(22.dp)) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (initial == null) "New Category" else "Edit Category",
                    style = MaterialTheme.typography.headlineMedium
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Category Name *") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    BoutiqueImage(
                        source = imageUrl,
                        contentDescription = name,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                    OutlinedButton(onClick = {
                        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }) {
                        Text("Pick Photo")
                    }
                }
                OutlinedTextField(
                    value = imageUrl,
                    onValueChange = { imageUrl = it },
                    label = { Text("Image URL or Preset") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                AdminSwitchRow("Visible on Storefront", isVisible) { isVisible = it }
                if (!errorText.isNullOrBlank()) {
                    Text(
                        text = errorText.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = CrimsonError
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss, enabled = !isSaving) { Text("Cancel") }
                    Button(
                        onClick = {
                            if (name.isBlank()) {
                                errorText = "Please enter a Category Name."
                                return@Button
                            }
                            errorText = null
                            isSaving = true
                            onSave(
                                (initial ?: Category(sortOrder = nextOrder)).copy(
                                    name = name.trim(),
                                    description = description.trim(),
                                    imageUrl = imageUrl.trim().ifBlank { null },
                                    isVisible = isVisible
                                )
                            ) { result ->
                                isSaving = false
                                result.onSuccess { onDismiss() }
                                    .onFailure { errorText = it.localizedMessage ?: "Failed to save category." }
                            }
                        },
                        enabled = !isSaving,
                        colors = ButtonDefaults.buttonColors(containerColor = ObsidianBlack)
                    ) {
                        Text(if (isSaving) "Saving..." else "Save")
                    }
                }
            }
        }
    }
}

@Composable
fun AnnouncementEditorDialog(
    initial: Announcement?,
    nextOrder: Long,
    onDismiss: () -> Unit,
    onSave: (Announcement) -> Unit
) {
    var title by remember { mutableStateOf(initial?.title.orEmpty()) }
    var message by remember { mutableStateOf(initial?.message.orEmpty()) }
    var badge by remember { mutableStateOf(initial?.badgeText ?: "NEW") }
    var isActive by remember { mutableStateOf(initial?.isActive ?: true) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(color = CreamBackground, shape = RoundedCornerShape(22.dp)) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(if (initial == null) "New Announcement" else "Edit Announcement", style = MaterialTheme.typography.headlineMedium)
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title *") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = message, onValueChange = { message = it }, label = { Text("Message *") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = badge, onValueChange = { badge = it }, label = { Text("Badge Label") }, modifier = Modifier.fillMaxWidth())
                AdminSwitchRow("Active on Storefront", isActive) { isActive = it }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Button(
                        onClick = {
                            if (title.isNotBlank() && message.isNotBlank()) {
                                onSave(
                                    (initial ?: Announcement(sortOrder = nextOrder)).copy(
                                        title = title.trim(),
                                        message = message.trim(),
                                        badgeText = badge.trim(),
                                        isActive = isActive
                                    )
                                )
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ObsidianBlack)
                    ) { Text("Save") }
                }
            }
        }
    }
}

@Composable
fun BannerEditorDialog(
    initial: Banner?,
    categories: List<Category>,
    nextOrder: Long,
    onDismiss: () -> Unit,
    onSave: (Banner) -> Unit
) {
    var title by remember { mutableStateOf(initial?.title.orEmpty()) }
    var subtitle by remember { mutableStateOf(initial?.subtitle.orEmpty()) }
    var ctaText by remember { mutableStateOf(initial?.ctaText ?: "Shop Now") }
    var imageUrl by remember { mutableStateOf(initial?.imageUrl ?: "preset://dress_collection") }
    var targetCategoryId by remember { mutableStateOf(initial?.targetCategoryId.orEmpty()) }
    var isActive by remember { mutableStateOf(initial?.isActive ?: true) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(color = CreamBackground, shape = RoundedCornerShape(22.dp)) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(if (initial == null) "New Promo Banner" else "Edit Banner", style = MaterialTheme.typography.headlineMedium)
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Banner Title *") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = subtitle, onValueChange = { subtitle = it }, label = { Text("Subtitle") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = ctaText, onValueChange = { ctaText = it }, label = { Text("CTA Text") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = imageUrl, onValueChange = { imageUrl = it }, label = { Text("Image URL or Preset") }, modifier = Modifier.fillMaxWidth())
                Text("Target Category", style = MaterialTheme.typography.labelMedium, color = MutedTaupe)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(categories) { cat ->
                        FilterChip(
                            selected = targetCategoryId == cat.id,
                            onClick = { targetCategoryId = if (targetCategoryId == cat.id) "" else cat.id },
                            label = { Text(cat.name) }
                        )
                    }
                }
                AdminSwitchRow("Active Banner", isActive) { isActive = it }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Button(
                        onClick = {
                            if (title.isNotBlank()) {
                                onSave(
                                    (initial ?: Banner(sortOrder = nextOrder)).copy(
                                        title = title.trim(),
                                        subtitle = subtitle.trim(),
                                        ctaText = ctaText.trim(),
                                        imageUrl = imageUrl.trim().ifBlank { null },
                                        targetCategoryId = targetCategoryId,
                                        isActive = isActive
                                    )
                                )
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ObsidianBlack)
                    ) { Text("Save") }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OrderAdminDialog(
    order: OrderEnquiry,
    onDismiss: () -> Unit,
    onSave: (status: String, channel: String, notes: String) -> Unit
) {
    var status by remember { mutableStateOf(order.status) }
    var channel by remember { mutableStateOf(order.channel) }
    var notes by remember { mutableStateOf(order.notes) }

    val statuses = listOf("Pending", "Confirmed", "Processing", "Shipped", "Delivered", "Cancelled")
    val channels = listOf("WhatsApp Enquiry", "Confirmed Order")

    Dialog(onDismissRequest = onDismiss) {
        Surface(color = CreamBackground, shape = RoundedCornerShape(22.dp)) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Manage Order", style = MaterialTheme.typography.headlineMedium)
                Text("${order.productName} • Qty ${order.quantity} • ₹${order.totalPrice.toInt()}", style = MaterialTheme.typography.titleSmall, color = DeepRose)
                Text("Customer: ${order.customerName} (${order.customerPhone})", style = MaterialTheme.typography.bodySmall)
                Text("Address: ${order.deliveryAddress}", style = MaterialTheme.typography.bodySmall, color = MutedTaupe)

                Text("Order Type", style = MaterialTheme.typography.labelMedium, color = MutedTaupe)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    channels.forEach { ch ->
                        FilterChip(selected = channel == ch, onClick = { channel = ch }, label = { Text(ch) })
                    }
                }

                Text("Fulfillment Status", style = MaterialTheme.typography.labelMedium, color = MutedTaupe)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    statuses.forEach { st ->
                        FilterChip(
                            selected = status == st,
                            onClick = {
                                status = st
                                if (st in listOf("Confirmed", "Processing", "Shipped", "Delivered")) {
                                    channel = "Confirmed Order"
                                }
                            },
                            label = { Text(st) }
                        )
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Atelier Notes") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Close") }
                    Button(
                        onClick = {
                            onSave(status, channel, notes)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ObsidianBlack)
                    ) { Text("Update Order") }
                }
            }
        }
    }
}

@Composable
fun AdminSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        color = PureWhite.copy(alpha = 0.7f),
        border = BorderStroke(0.8.dp, HairlineRose),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = ObsidianBlack)
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}
