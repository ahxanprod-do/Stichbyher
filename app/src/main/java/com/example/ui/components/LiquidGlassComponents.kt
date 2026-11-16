package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.Announcement
import com.example.data.model.Product
import com.example.ui.theme.BlushPink
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.CreamBackground
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.DeepRose
import com.example.ui.theme.EmeraldSage
import com.example.ui.theme.MutedTaupe
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SoftBlushSurface
import com.example.ui.theme.SoftRose
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.text.NumberFormat
import java.util.Locale

fun formatInr(amount: Double): String {
    val formatter = NumberFormat.getCurrencyInstance(
        Locale.Builder().setLanguage("en").setRegion("IN").build()
    )
    formatter.maximumFractionDigits = 0
    return formatter.format(amount)
}

@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    alpha: Float = 0.80f,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    val baseModifier = modifier
        .shadow(
            elevation = 12.dp,
            shape = shape,
            ambientColor = SoftRose.copy(alpha = 0.22f),
            spotColor = SoftRose.copy(alpha = 0.22f)
        )
        .clip(shape)
        .background(
            Brush.verticalGradient(
                colors = listOf(
                    PureWhite.copy(alpha = alpha + 0.08f),
                    CreamBackground.copy(alpha = alpha)
                )
            )
        )
        .border(
            width = 1.dp,
            brush = Brush.linearGradient(
                colors = listOf(
                    PureWhite.copy(alpha = 0.95f),
                    SoftRose.copy(alpha = 0.35f)
                )
            ),
            shape = shape
        )

    Box(
        modifier = if (onClick != null) {
            baseModifier.clickable(onClick = onClick)
        } else {
            baseModifier
        },
        content = content
    )
}

@Composable
fun BoutiqueImage(
    source: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    when {
        source.isNullOrBlank() || source == "preset://dress_collection" -> {
            Image(
                painter = painterResource(id = R.drawable.img_dress_collection),
                contentDescription = contentDescription,
                modifier = modifier,
                contentScale = contentScale
            )
        }
        source == "preset://hero_boutique" -> {
            Image(
                painter = painterResource(id = R.drawable.img_hero_boutique),
                contentDescription = contentDescription,
                modifier = modifier,
                contentScale = contentScale
            )
        }
        source == "preset://kurti_ethnic" -> {
            Image(
                painter = painterResource(id = R.drawable.img_kurti_ethnic),
                contentDescription = contentDescription,
                modifier = modifier,
                contentScale = contentScale
            )
        }
        source == "preset://custom_stitching" -> {
            Image(
                painter = painterResource(id = R.drawable.img_custom_stitching),
                contentDescription = contentDescription,
                modifier = modifier,
                contentScale = contentScale
            )
        }
        source == "preset://logo" -> {
            Image(
                painter = painterResource(id = R.drawable.img_stitchbyher_logo),
                contentDescription = contentDescription,
                modifier = modifier,
                contentScale = contentScale
            )
        }
        source.startsWith("data:image") -> {
            val bitmap = remember(source) {
                try {
                    val base64Part = source.substringAfter("base64,", "")
                    if (base64Part.isNotBlank()) {
                        val bytes = Base64.decode(base64Part, Base64.DEFAULT)
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                    } else null
                } catch (_: Exception) {
                    null
                }
            }
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = contentDescription,
                    modifier = modifier,
                    contentScale = contentScale
                )
            } else {
                Image(
                    painter = painterResource(id = R.drawable.img_dress_collection),
                    contentDescription = contentDescription,
                    modifier = modifier,
                    contentScale = contentScale
                )
            }
        }
        else -> {
            AsyncImage(
                model = source,
                contentDescription = contentDescription,
                placeholder = painterResource(id = R.drawable.img_hero_boutique),
                error = painterResource(id = R.drawable.img_dress_collection),
                modifier = modifier,
                contentScale = contentScale
            )
        }
    }
}

suspend fun encodeUriToCompressedDataUri(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
    try {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            val original = BitmapFactory.decodeStream(stream) ?: return@withContext null
            val maxDim = 640
            val scale = minOf(maxDim.toFloat() / original.width, maxDim.toFloat() / original.height, 1f)
            val targetW = (original.width * scale).toInt().coerceAtLeast(1)
            val targetH = (original.height * scale).toInt().coerceAtLeast(1)
            val resized = if (scale < 1f) {
                Bitmap.createScaledBitmap(original, targetW, targetH, true)
            } else {
                original
            }
            val out = ByteArrayOutputStream()
            resized.compress(Bitmap.CompressFormat.JPEG, 76, out)
            val base64 = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
            "data:image/jpeg;base64,$base64"
        }
    } catch (_: Exception) {
        null
    }
}

@Composable
fun AnnouncementGlassBanner(
    announcements: List<Announcement>,
    modifier: Modifier = Modifier
) {
    val active = remember(announcements) {
        announcements.filter { it.isActive }.sortedBy { it.sortOrder }
    }
    if (active.isEmpty()) return

    var currentIndex by remember(active.size) { mutableIntStateOf(0) }
    LaunchedEffect(active) {
        if (active.size > 1) {
            while (true) {
                delay(4200L)
                currentIndex = (currentIndex + 1) % active.size
            }
        }
    }

    val current = active[currentIndex % active.size]
    LiquidGlassCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("announcement_banner"),
        cornerRadius = 18.dp,
        alpha = 0.78f
    ) {
        AnimatedContent(
            targetState = current,
            transitionSpec = {
                (slideInVertically { it / 2 } + fadeIn()) togetherWith
                    (slideOutVertically { -it / 2 } + fadeOut())
            },
            label = "announcement_cycle"
        ) { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                BlushPink.copy(alpha = 0.24f),
                                PureWhite.copy(alpha = 0.35f),
                                SoftRose.copy(alpha = 0.18f)
                            )
                        )
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = DeepRose,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                if (item.badgeText.isNotBlank()) {
                    Surface(
                        color = ObsidianBlack,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = item.badgeText.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = PureWhite,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.labelLarge,
                        color = ObsidianBlack,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = item.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedTaupe,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun ProductBoutiqueCard(
    product: Product,
    isWishlisted: Boolean,
    onClick: () -> Unit,
    onToggleWishlist: () -> Unit,
    modifier: Modifier = Modifier
) {
    val heartScale by animateFloatAsState(
        targetValue = if (isWishlisted) 1.15f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "wishlist_scale"
    )

    LiquidGlassCard(
        modifier = modifier.testTag("product_card_${product.id}"),
        cornerRadius = 22.dp,
        alpha = 0.86f,
        onClick = onClick
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.82f)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(17.dp))
            ) {
                BoutiqueImage(
                    source = product.imageUrls.firstOrNull(),
                    contentDescription = product.name,
                    modifier = Modifier.fillMaxSize()
                )

                // Top-left discount or badge
                if (product.discountPercent > 0L) {
                    Surface(
                        color = ObsidianBlack.copy(alpha = 0.84f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "-${product.discountPercent}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = BlushPink,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                } else if (product.isNewArrival) {
                    Surface(
                        color = PureWhite.copy(alpha = 0.90f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "NEW",
                            style = MaterialTheme.typography.labelSmall,
                            color = ObsidianBlack,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                // Top-right wishlist toggle
                IconButton(
                    onClick = onToggleWishlist,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .minimumInteractiveComponentSize()
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(PureWhite.copy(alpha = 0.85f))
                        .testTag("wishlist_btn_${product.id}")
                ) {
                    Icon(
                        imageVector = if (isWishlisted) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = if (isWishlisted) "Remove from wishlist" else "Add to wishlist",
                        tint = if (isWishlisted) DeepRose else ObsidianBlack,
                        modifier = Modifier
                            .size(19.dp)
                            .scale(heartScale)
                    )
                }

                // Bottom-left stock indicator
                if (product.stock <= 0L) {
                    Surface(
                        color = CrimsonError.copy(alpha = 0.9f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "Made to Order",
                            style = MaterialTheme.typography.labelSmall,
                            color = PureWhite,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = product.categoryName.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = DeepRose,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Star,
                            contentDescription = null,
                            tint = ChampagneGold,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = String.format(Locale.US, "%.1f", product.ratingAvg),
                            style = MaterialTheme.typography.labelSmall,
                            color = ObsidianBlack
                        )
                    }
                }

                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = ObsidianBlack,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = formatInr(product.price),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ObsidianBlack
                    )
                    if (product.originalPrice > product.price) {
                        Text(
                            text = formatInr(product.originalPrice),
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedTaupe,
                            textDecoration = TextDecoration.LineThrough
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatusPill(
    text: String,
    color: Color = DeepRose
) {
    Surface(
        color = color.copy(alpha = 0.14f),
        border = BorderStroke(0.8.dp, color.copy(alpha = 0.45f)),
        shape = RoundedCornerShape(10.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
        )
    }
}

fun orderStatusColor(status: String): Color = when (status) {
    "Confirmed", "Delivered" -> EmeraldSage
    "Processing", "Shipped" -> ChampagneGold
    "Cancelled" -> CrimsonError
    else -> DeepRose
}
