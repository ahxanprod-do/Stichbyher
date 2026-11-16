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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.VerifiedUser
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
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.OrderEnquiry
import com.example.data.model.Product
import com.example.data.model.UserProfile
import com.example.data.model.WebsiteSettings
import com.example.data.model.WishlistItem
import com.example.ui.components.BoutiqueImage
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.StatusPill
import com.example.ui.components.encodeUriToCompressedDataUri
import com.example.ui.components.formatInr
import com.example.ui.components.orderStatusColor
import com.example.ui.theme.BlushPink
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
    userProfile: UserProfile?,
    userEmail: String,
    isEmailVerified: Boolean,
    isAdmin: Boolean,
    orders: List<OrderEnquiry>,
    wishlist: List<WishlistItem>,
    products: List<Product>,
    settings: WebsiteSettings,
    initialTab: Int = 0,
    onUpdateProfile: (fullName: String, phone: String, addresses: List<String>, defaultAddress: String, photoUrl: String?) -> Unit,
    onCancelOrder: (String) -> Unit,
    onRemoveWishlist: (Product) -> Unit,
    onOpenProduct: (String) -> Unit,
    onOpenAdmin: () -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var activeTab by remember(initialTab) { mutableIntStateOf(initialTab) } // 0 = Orders, 1 = Wishlist, 2 = Addresses
    var showEditProfileSheet by remember { mutableStateOf(false) }
    var newAddressText by remember { mutableStateOf("") }

    val avatarPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null && userProfile != null) {
            scope.launch {
                val encoded = encodeUriToCompressedDataUri(context, uri)
                if (encoded != null) {
                    onUpdateProfile(
                        userProfile.fullName,
                        userProfile.phoneNumber,
                        userProfile.savedAddresses,
                        userProfile.defaultAddress,
                        encoded
                    )
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(CreamBackground),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 115.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (userProfile?.isBlocked == true) {
            item {
                Surface(
                    color = CrimsonError.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Account access restricted by StitchByHer admin. You may view history, but new orders are paused.",
                        style = MaterialTheme.typography.bodySmall,
                        color = CrimsonError,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        }

        // Profile Summary Glass Card
        item {
            LiquidGlassCard(cornerRadius = 24.dp, alpha = 0.88f) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(68.dp)
                                .clickable {
                                    avatarPicker.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                        ) {
                            BoutiqueImage(
                                source = userProfile?.photoUrl ?: "preset://logo",
                                contentDescription = "Profile photo",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                            )
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(ObsidianBlack),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CameraAlt,
                                    contentDescription = "Change photo",
                                    tint = BlushPink,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = userProfile?.fullName?.ifBlank { "Boutique Patron" } ?: "Loading...",
                                style = MaterialTheme.typography.headlineMedium,
                                color = ObsidianBlack
                            )
                            Text(
                                text = userProfile?.email?.ifBlank { userEmail } ?: userEmail,
                                style = MaterialTheme.typography.bodySmall,
                                color = MutedTaupe
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (isEmailVerified) {
                                    StatusPill(text = "Verified Google", color = EmeraldSage)
                                }
                                if (isAdmin) {
                                    StatusPill(text = "Store Admin", color = ObsidianBlack)
                                }
                            }
                        }

                        IconButton(
                            onClick = { showEditProfileSheet = true },
                            modifier = Modifier.testTag("edit_profile_button")
                        ) {
                            Icon(Icons.Rounded.Edit, contentDescription = "Edit profile", tint = DeepRose)
                        }
                    }

                    HorizontalDivider(color = HairlineRose)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Phone", style = MaterialTheme.typography.labelSmall, color = MutedTaupe)
                            Text(
                                text = userProfile?.phoneNumber?.ifBlank { "Tap edit to add phone" } ?: "—",
                                style = MaterialTheme.typography.bodyMedium,
                                color = ObsidianBlack
                            )
                        }
                        Column(modifier = Modifier.weight(1.3f)) {
                            Text("Default Delivery", style = MaterialTheme.typography.labelSmall, color = MutedTaupe)
                            Text(
                                text = userProfile?.defaultAddress?.ifBlank { "No saved address yet" } ?: "—",
                                style = MaterialTheme.typography.bodyMedium,
                                color = ObsidianBlack,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    if (isAdmin) {
                        Button(
                            onClick = onOpenAdmin,
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ObsidianBlack,
                                contentColor = BlushPink
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("account_admin_button")
                        ) {
                            Icon(Icons.Rounded.AdminPanelSettings, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Open StitchByHer Admin Studio")
                        }
                    }
                }
            }
        }

        // Tab Switcher: Orders / Wishlist / Addresses
        item {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val tabs = listOf("Orders (${orders.size})", "Wishlist (${wishlist.size})", "Addresses")
                tabs.forEachIndexed { idx, label ->
                    SegmentedButton(
                        selected = activeTab == idx,
                        onClick = { activeTab = idx },
                        shape = SegmentedButtonDefaults.itemShape(index = idx, count = tabs.size),
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = ObsidianBlack,
                            activeContentColor = PureWhite,
                            inactiveContainerColor = PureWhite.copy(alpha = 0.7f),
                            inactiveContentColor = ObsidianBlack
                        )
                    ) {
                        Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1)
                    }
                }
            }
        }

        when (activeTab) {
            0 -> {
                if (orders.isEmpty()) {
                    item {
                        EmptyAccountCard(
                            icon = Icons.Rounded.ShoppingBag,
                            title = "No orders yet",
                            subtitle = "Your WhatsApp enquiries and confirmed boutique orders will appear here."
                        )
                    }
                } else {
                    items(orders, key = { it.id }) { order ->
                        OrderHistoryCard(
                            order = order,
                            onCancel = { onCancelOrder(order.id) },
                            onOpenProduct = { onOpenProduct(order.productId) }
                        )
                    }
                }
            }

            1 -> {
                if (wishlist.isEmpty()) {
                    item {
                        EmptyAccountCard(
                            icon = Icons.Rounded.Favorite,
                            title = "Your wishlist is empty",
                            subtitle = "Tap the heart on any piece to curate your personal edit."
                        )
                    }
                } else {
                    items(wishlist, key = { it.productId }) { item ->
                        val matchedProduct = products.find { it.id == item.productId }
                        LiquidGlassCard(
                            cornerRadius = 18.dp,
                            alpha = 0.86f,
                            onClick = { onOpenProduct(item.productId) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                BoutiqueImage(
                                    source = item.imageUrl ?: matchedProduct?.imageUrls?.firstOrNull(),
                                    contentDescription = item.productName,
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = matchedProduct?.name ?: item.productName,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = ObsidianBlack
                                    )
                                    Text(
                                        text = formatInr(matchedProduct?.price ?: item.price),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = DeepRose
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        onRemoveWishlist(
                                            matchedProduct ?: Product(id = item.productId, name = item.productName, price = item.price)
                                        )
                                    }
                                ) {
                                    Icon(Icons.Rounded.Delete, contentDescription = "Remove", tint = CrimsonError)
                                }
                            }
                        }
                    }
                }
            }

            else -> {
                item {
                    LiquidGlassCard(cornerRadius = 20.dp, alpha = 0.86f) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("Saved Delivery Addresses", style = MaterialTheme.typography.titleLarge)
                            val addresses = userProfile?.savedAddresses.orEmpty()
                            if (addresses.isEmpty()) {
                                Text(
                                    text = "Add a delivery address below for faster WhatsApp ordering.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MutedTaupe
                                )
                            } else {
                                addresses.forEach { addr ->
                                    val isDefault = addr == userProfile?.defaultAddress
                                    Surface(
                                        color = if (isDefault) SoftBlushSurface else PureWhite.copy(alpha = 0.65f),
                                        border = BorderStroke(1.dp, if (isDefault) DeepRose else HairlineRose),
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = if (isDefault) Icons.Rounded.CheckCircle else Icons.Rounded.LocationOn,
                                                contentDescription = null,
                                                tint = if (isDefault) EmeraldSage else DeepRose,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = addr,
                                                style = MaterialTheme.typography.bodySmall,
                                                modifier = Modifier.weight(1f)
                                            )
                                            if (!isDefault && userProfile != null) {
                                                TextButton(
                                                    onClick = {
                                                        onUpdateProfile(
                                                            userProfile.fullName,
                                                            userProfile.phoneNumber,
                                                            addresses,
                                                            addr,
                                                            null
                                                        )
                                                    }
                                                ) {
                                                    Text("Default", style = MaterialTheme.typography.labelSmall)
                                                }
                                            }
                                            if (userProfile != null) {
                                                IconButton(
                                                    onClick = {
                                                        val updated = addresses.filterNot { it == addr }
                                                        val nextDefault = if (isDefault) updated.firstOrNull().orEmpty() else userProfile.defaultAddress
                                                        onUpdateProfile(
                                                            userProfile.fullName,
                                                            userProfile.phoneNumber,
                                                            updated,
                                                            nextDefault,
                                                            null
                                                        )
                                                    }
                                                ) {
                                                    Icon(Icons.Rounded.Delete, contentDescription = "Delete address", tint = CrimsonError)
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = newAddressText,
                                    onValueChange = { newAddressText = it },
                                    label = { Text("New delivery address") },
                                    modifier = Modifier.weight(1f)
                                )
                                Button(
                                    onClick = {
                                        val clean = newAddressText.trim()
                                        if (clean.isNotBlank() && userProfile != null) {
                                            val updated = (userProfile.savedAddresses + clean).distinct().take(10)
                                            val nextDefault = userProfile.defaultAddress.ifBlank { clean }
                                            onUpdateProfile(
                                                userProfile.fullName,
                                                userProfile.phoneNumber,
                                                updated,
                                                nextDefault,
                                                null
                                            )
                                            newAddressText = ""
                                        }
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = ObsidianBlack),
                                    modifier = Modifier.height(54.dp)
                                ) {
                                    Icon(Icons.Rounded.Add, contentDescription = "Add address")
                                }
                            }
                        }
                    }
                }
            }
        }

        // Policies & Sign Out
        item {
            LiquidGlassCard(cornerRadius = 20.dp, alpha = 0.84f) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.VerifiedUser, contentDescription = null, tint = DeepRose, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Atelier Policies", style = MaterialTheme.typography.titleMedium)
                    }
                    Text(settings.shippingPolicy, style = MaterialTheme.typography.bodySmall, color = MutedTaupe)
                    Text(settings.returnPolicy, style = MaterialTheme.typography.bodySmall, color = MutedTaupe)
                    HorizontalDivider(color = HairlineRose)
                    OutlinedButton(
                        onClick = onSignOut,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, CrimsonError.copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CrimsonError),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("sign_out_button")
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sign Out")
                    }
                }
            }
        }
    }

    if (showEditProfileSheet && userProfile != null) {
        ModalBottomSheet(
            onDismissRequest = { showEditProfileSheet = false },
            containerColor = CreamBackground
        ) {
            var fullName by remember(userProfile) { mutableStateOf(userProfile.fullName) }
            var phone by remember(userProfile) { mutableStateOf(userProfile.phoneNumber) }
            var defaultAddress by remember(userProfile) { mutableStateOf(userProfile.defaultAddress) }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Edit Profile", style = MaterialTheme.typography.headlineMedium)
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Full Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = defaultAddress,
                    onValueChange = { defaultAddress = it },
                    label = { Text("Primary Delivery Address") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {
                        val updatedAddresses = if (defaultAddress.isNotBlank()) {
                            (userProfile.savedAddresses + defaultAddress.trim()).distinct().take(10)
                        } else {
                            userProfile.savedAddresses
                        }
                        onUpdateProfile(fullName, phone, updatedAddresses, defaultAddress, null)
                        showEditProfileSheet = false
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ObsidianBlack, contentColor = PureWhite),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text("Save Changes")
                }
            }
        }
    }
}

@Composable
private fun OrderHistoryCard(
    order: OrderEnquiry,
    onCancel: () -> Unit,
    onOpenProduct: () -> Unit
) {
    val dateStr = remember(order.createdAt) {
        order.createdAt?.toDate()?.let {
            SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(it)
        } ?: "Just now"
    }
    LiquidGlassCard(cornerRadius = 20.dp, alpha = 0.88f, onClick = onOpenProduct) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusPill(
                    text = order.channel,
                    color = if (order.channel == "Confirmed Order") EmeraldSage else DeepRose
                )
                StatusPill(text = order.status, color = orderStatusColor(order.status))
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                BoutiqueImage(
                    source = order.productImageUrl,
                    contentDescription = order.productName,
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(14.dp))
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(order.productName, style = MaterialTheme.typography.titleSmall, color = ObsidianBlack)
                    Text(
                        text = "Size ${order.selectedSize} • ${order.selectedColor} • Qty ${order.quantity}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedTaupe
                    )
                    Text(
                        text = formatInr(order.totalPrice),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = ObsidianBlack
                    )
                }
            }

            Text(
                text = "Deliver to: ${order.deliveryAddress}",
                style = MaterialTheme.typography.bodySmall,
                color = MutedTaupe
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(dateStr, style = MaterialTheme.typography.labelSmall, color = MutedTaupe)
                if (order.status == "Pending") {
                    TextButton(onClick = onCancel) {
                        Text("Cancel Enquiry", color = CrimsonError, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyAccountCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    LiquidGlassCard(cornerRadius = 20.dp, alpha = 0.82f) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, contentDescription = null, tint = BlushPink, modifier = Modifier.size(48.dp))
            Text(title, style = MaterialTheme.typography.titleLarge, color = ObsidianBlack)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MutedTaupe)
        }
    }
}
