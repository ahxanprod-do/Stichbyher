package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.Checkroom
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.Category
import com.example.data.model.Product
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.ProductBoutiqueCard
import com.example.ui.theme.BlushPink
import com.example.ui.theme.CreamBackground
import com.example.ui.theme.DeepRose
import com.example.ui.theme.HairlineRose
import com.example.ui.theme.MutedTaupe
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SoftBlushSurface
import com.example.ui.viewmodel.ProductSortOption

@Composable
fun ShopCatalogScreen(
    products: List<Product>,
    allPublishedProducts: List<Product>,
    categories: List<Category>,
    wishlistIds: Set<String>,
    searchQuery: String,
    selectedCategoryId: String?,
    selectedSizeFilter: String?,
    selectedColorFilter: String?,
    maxPriceFilter: Double?,
    sortOption: ProductSortOption,
    onSearchChange: (String) -> Unit,
    onCategorySelect: (String?) -> Unit,
    onSizeSelect: (String?) -> Unit,
    onColorSelect: (String?) -> Unit,
    onMaxPriceSelect: (Double?) -> Unit,
    onSortSelect: (ProductSortOption) -> Unit,
    onClearFilters: () -> Unit,
    onProductClick: (String) -> Unit,
    onToggleWishlist: (Product) -> Unit
) {
    var showFilterPanel by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }

    val availableSizes = remember(allPublishedProducts) {
        allPublishedProducts.flatMap { it.sizes }.distinct().take(10)
    }
    val availableColors = remember(allPublishedProducts) {
        allPublishedProducts.flatMap { it.colors }.distinct().take(10)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CreamBackground)
            .padding(top = 10.dp)
    ) {
        // Search + Sort/Filter Header
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
                Column {
                    Text(
                        text = "Boutique Catalogue",
                        style = MaterialTheme.typography.headlineLarge,
                        color = ObsidianBlack
                    )
                    Text(
                        text = "${products.size} handcrafted creations",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedTaupe
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = { showFilterPanel = !showFilterPanel },
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FilterList,
                            contentDescription = "Filters",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Filter", style = MaterialTheme.typography.labelMedium)
                    }

                    androidx.compose.foundation.layout.Box {
                        OutlinedButton(
                            onClick = { showSortMenu = true },
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Sort,
                                contentDescription = "Sort",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sort", style = MaterialTheme.typography.labelMedium)
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            ProductSortOption.entries.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option.label) },
                                    onClick = {
                                        onSortSelect(option)
                                        showSortMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("Search gowns, kurtis, fabrics...") },
                leadingIcon = {
                    Icon(Icons.Rounded.Search, contentDescription = null, tint = DeepRose)
                },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = PureWhite.copy(alpha = 0.85f),
                    unfocusedContainerColor = PureWhite.copy(alpha = 0.72f),
                    focusedBorderColor = DeepRose,
                    unfocusedBorderColor = HairlineRose
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("catalog_search_input")
            )
        }

        // Category Filter Chips
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = selectedCategoryId == null,
                    onClick = { onCategorySelect(null) },
                    label = { Text("All") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ObsidianBlack,
                        selectedLabelColor = PureWhite
                    )
                )
            }
            items(categories.filter { it.isVisible }.sortedBy { it.sortOrder }, key = { it.id }) { category ->
                FilterChip(
                    selected = selectedCategoryId == category.id,
                    onClick = {
                        onCategorySelect(if (selectedCategoryId == category.id) null else category.id)
                    },
                    label = { Text(category.name) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ObsidianBlack,
                        selectedLabelColor = PureWhite
                    )
                )
            }
        }

        // Expandable Size, Color, and Price Filter Panel
        if (showFilterPanel) {
            LiquidGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                cornerRadius = 18.dp,
                alpha = 0.88f
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (availableSizes.isNotEmpty()) {
                        Text("Size", style = MaterialTheme.typography.labelMedium, color = MutedTaupe)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(availableSizes) { size ->
                                FilterChip(
                                    selected = selectedSizeFilter == size,
                                    onClick = { onSizeSelect(if (selectedSizeFilter == size) null else size) },
                                    label = { Text(size) }
                                )
                            }
                        }
                    }

                    if (availableColors.isNotEmpty()) {
                        Text("Color", style = MaterialTheme.typography.labelMedium, color = MutedTaupe)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(availableColors) { color ->
                                FilterChip(
                                    selected = selectedColorFilter == color,
                                    onClick = { onColorSelect(if (selectedColorFilter == color) null else color) },
                                    label = { Text(color) }
                                )
                            }
                        }
                    }

                    Text("Max Budget", style = MaterialTheme.typography.labelMedium, color = MutedTaupe)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val priceOptions = listOf(
                            null to "Any",
                            2500.0 to "Under ₹2,500",
                            5000.0 to "Under ₹5,000",
                            10000.0 to "Under ₹10,000"
                        )
                        items(priceOptions) { (limit, label) ->
                            FilterChip(
                                selected = maxPriceFilter == limit,
                                onClick = { onMaxPriceSelect(limit) },
                                label = { Text(label) }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onClearFilters) {
                            Text("Reset All", color = DeepRose)
                        }
                    }
                }
            }
        }

        if (products.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Checkroom,
                    contentDescription = null,
                    tint = BlushPink,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "No pieces match your filters",
                    style = MaterialTheme.typography.titleLarge,
                    color = ObsidianBlack
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Try clearing your search or selecting another collection.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedTaupe
                )
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedButton(onClick = onClearFilters, shape = RoundedCornerShape(14.dp)) {
                    Text("Reset Filters")
                }
            }
        } else {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val columns = when {
                    maxWidth >= 840.dp -> 4
                    maxWidth >= 600.dp -> 3
                    else -> 2
                }
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 110.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(products, key = { it.id }) { product ->
                        ProductBoutiqueCard(
                            product = product,
                            isWishlisted = product.id in wishlistIds,
                            onClick = { onProductClick(product.id) },
                            onToggleWishlist = { onToggleWishlist(product) }
                        )
                    }
                }
            }
        }
    }
}
