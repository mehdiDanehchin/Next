package com.example.next.ui.screens

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.next.R
import com.example.next.di.AppContainer
import com.example.next.models.Product
import com.example.next.ui.theme.*
import com.example.next.viewmodel.HomeViewModel
import com.example.next.viewmodel.PriceRangeFilter
import com.example.next.viewmodel.ProductSortOption
import com.example.next.viewmodel.RatingFilter
import java.util.Locale

data class CategoryItem(
    val key: String,
    val nameResId: Int,
    val iconResId: Int,
    val bgColor: Color
)

fun resolveImageRes(context: android.content.Context, imageName: String): Int {
    if (imageName.isEmpty()) return 0
    return context.resources.getIdentifier(imageName, "drawable", context.packageName)
}

@Composable
fun ProductImage(
    imageName: String,
    modifier: Modifier = Modifier,
    // Fit (never crop): every product image keeps its aspect ratio, is fully
    // visible and centered, with empty space around it when ratios differ.
    contentScale: ContentScale = ContentScale.Fit
) {
    val context = LocalContext.current
    val imageRes = remember(imageName) { resolveImageRes(context, imageName) }

    if (imageRes != 0) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(imageRes)
                .crossfade(true)
                .build(),
            contentDescription = null,
            modifier = modifier,
            contentScale = contentScale,
            error = painterResource(R.drawable.ic_image_placeholder),
            placeholder = painterResource(R.drawable.ic_image_placeholder)
        )
    } else {
        androidx.compose.foundation.Image(
            painter = painterResource(R.drawable.ic_image_placeholder),
            contentDescription = null,
            modifier = modifier,
            contentScale = contentScale
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun HomeScreen(
    container: AppContainer,
    onProductClick: (Product, Boolean) -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedContentScope
) {
    val homeViewModel: HomeViewModel = viewModel(factory = container.homeViewModelFactory)
    val uiState by homeViewModel.uiState.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val customColors = LocalCustomColors.current
    val colorScheme = MaterialTheme.colorScheme

    val isFilterActive = uiState.activeCategory != null || uiState.isSearchActive

    val categories = remember {
        listOf(
            CategoryItem("Phones", R.string.category_phones, R.drawable.ic_phone, customColors.phonesBg),
            CategoryItem("Laptops", R.string.category_laptops, R.drawable.ic_laptop, customColors.laptopsBg),
            CategoryItem("Cameras", R.string.category_cameras, R.drawable.ic_camera, customColors.camerasBg),
            CategoryItem("Accessories", R.string.category_accessories, R.drawable.ic_accessories, customColors.accessoriesBg),
            CategoryItem("All", R.string.all_products, R.drawable.ic_home, customColors.primaryLight)
        )
    }

    fun categoryDisplayName(category: String): String =
        categories.firstOrNull { it.key == category }?.let { context.getString(it.nameResId) } ?: category

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
    ) {
        // Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(customColors.surfaceWhite)
                .padding(start = 4.dp, end = 16.dp, top = 16.dp, bottom = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isFilterActive) {
                    IconButton(
                        onClick = { homeViewModel.restoreFullView() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_chevron_down),
                            contentDescription = stringResource(R.string.cd_back_home),
                            modifier = Modifier.size(24.dp),
                            tint = Color.Unspecified
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.size(20.dp))
                }

                Text(
                    text = when {
                        uiState.isSearchActive -> context.getString(R.string.search_results)
                        uiState.activeCategory != null -> categoryDisplayName(uiState.activeCategory!!)
                        else -> context.getString(R.string.app_name)
                    },
                    color = if (isFilterActive) colorScheme.onSurface else Primary,
                    fontSize = if (isFilterActive) 20.sp else 28.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif,
                    modifier = Modifier.weight(1f)
                )

                if (isFilterActive) {
                    IconButton(
                        onClick = { homeViewModel.restoreFullView() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_close),
                            contentDescription = stringResource(R.string.cd_clear_filter),
                            modifier = Modifier.size(24.dp),
                            tint = colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.size(36.dp))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Search Bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = colorScheme.background
            ) {
                Row(
                    modifier = Modifier
                        .height(48.dp)
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_search),
                        contentDescription = stringResource(R.string.cd_search),
                        modifier = Modifier.size(20.dp),
                        tint = Color.Unspecified
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    TextField(
                        value = uiState.searchQuery,
                        onValueChange = { homeViewModel.onSearchQueryChange(it) },
                        placeholder = {
                            Text(
                                context.getString(R.string.search_hint),
                                color = customColors.textHint,
                                fontSize = 14.sp
                            )
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = Primary
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = { focusManager.clearFocus() }
                        )
                    )
                }
            }
        }

        // Scrollable content: 2-column product grid; headers/rows span the full width.
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 10.dp, end = 10.dp, bottom = 24.dp)
        ) {
            if (uiState.noResults) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        text = context.getString(R.string.no_results),
                        color = colorScheme.onSurfaceVariant,
                        fontSize = 16.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }

            if (!isFilterActive) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        text = context.getString(R.string.categories),
                        color = colorScheme.onSurface,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif,
                        modifier = Modifier.padding(start = 6.dp, top = 16.dp, bottom = 4.dp)
                    )
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(categories) { cat ->
                            CategoryChip(cat, onClick = { homeViewModel.selectCategory(cat.key) })
                        }
                    }
                }

                if (uiState.featuredProducts.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            text = context.getString(R.string.featured_products),
                            color = colorScheme.onSurface,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif,
                            modifier = Modifier.padding(start = 6.dp, end = 6.dp, top = 12.dp)
                        )
                    }
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 0.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(uiState.featuredProducts, key = { it.id }) { product ->
                                ProductCard(
                                    product = product,
                                    homeViewModel = homeViewModel,
                                    onClick = { onProductClick(it, true) },
                                    // Per-section shared-element key: the same product
                                    // also lives in the grid below, and keys must be
                                    // unique within one SharedTransitionScope.
                                    sharedElementKey = "featured_product_image_${product.id}",
                                    sharedTransitionScope = sharedTransitionScope,
                                    animatedVisibilityScope = animatedVisibilityScope
                                )
                            }
                        }
                    }
                }
            }

            if (uiState.visibleProducts.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        text = when {
                            uiState.isSearchActive -> ""
                            uiState.activeCategory != null -> categoryDisplayName(uiState.activeCategory!!)
                            else -> context.getString(R.string.popular_products)
                        },
                        color = colorScheme.onSurface,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif,
                        modifier = Modifier.padding(start = 6.dp, end = 6.dp, top = 8.dp)
                    )
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    SortFilterBar(
                        sortOption = uiState.sortOption,
                        priceFilter = uiState.priceFilter,
                        ratingFilter = uiState.ratingFilter,
                        onSortChange = homeViewModel::setSortOption,
                        onPriceChange = homeViewModel::setPriceFilter,
                        onRatingChange = homeViewModel::setRatingFilter
                    )
                }
                gridItems(uiState.visibleProducts, key = { it.id }) { product ->
                    ProductCard(
                        product = product,
                        homeViewModel = homeViewModel,
                        onClick = { onProductClick(it, false) },
                        fixedWidth = false,
                        // Unique per-section key (see featured row above).
                        sharedElementKey = "grid_product_image_${product.id}",
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryChip(item: CategoryItem, onClick: () -> Unit) {
    val customColors = LocalCustomColors.current
    Surface(
        modifier = Modifier
            .padding(4.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = customColors.surfaceWhite
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(item.bgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(item.iconResId),
                    contentDescription = stringResource(item.nameResId),
                    modifier = Modifier.size(28.dp),
                    tint = Primary
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(item.nameResId),
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 12.sp,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ProductCard(
    product: Product,
    homeViewModel: HomeViewModel,
    onClick: (Product) -> Unit,
    fixedWidth: Boolean = true,
    sharedElementKey: String? = null,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedContentScope? = null
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val customColors = LocalCustomColors.current
    val colorScheme = MaterialTheme.colorScheme

    // Reactive favorite state: updates instantly when the wishlist changes anywhere.
    val isFav by homeViewModel.isInWishlist(product.id).collectAsStateWithLifecycle(initialValue = false)

    Card(
        modifier = Modifier
            // Fixed width inside horizontal rows, fill the cell inside the grid.
            .then(if (fixedWidth) Modifier.width(160.dp) else Modifier.fillMaxWidth())
            .padding(6.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { onClick(product) }
            ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(containerColor = customColors.surfaceWhite)
    ) {
        Column {
            Box(modifier = Modifier.height(140.dp)) {
                val imageModifier = if (sharedTransitionScope != null &&
                    animatedVisibilityScope != null && sharedElementKey != null
                ) {
                    // sharedElement is a member extension of SharedTransitionScope.
                    with(sharedTransitionScope) {
                        Modifier
                            .fillMaxSize()
                            .sharedElement(
                                rememberSharedContentState(key = sharedElementKey),
                                animatedVisibilityScope
                            )
                    }
                } else {
                    Modifier.fillMaxSize()
                }
                ProductImage(
                    imageName = product.imageUrl,
                    modifier = imageModifier
                )

                IconButton(
                    onClick = {
                        homeViewModel.toggleWishlist(product)
                        val message = if (isFav) {
                            context.getString(R.string.removed_from_wishlist)
                        } else {
                            context.getString(R.string.added_to_wishlist)
                        }
                        android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(32.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xCCFFFFFF))
                ) {
                    Icon(
                        painter = painterResource(
                            if (isFav) R.drawable.ic_heart_filled else R.drawable.ic_heart
                        ),
                        contentDescription = stringResource(R.string.cd_favorite),
                        modifier = Modifier.size(20.dp),
                        tint = Color.Unspecified
                    )
                }
            }

            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = product.name,
                    color = colorScheme.onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    StarRating(rating = product.rating, starSize = 12.dp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "(%.1f)".format(Locale.US, product.rating),
                        color = colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = product.description,
                    color = colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = product.formattedPrice,
                    color = Primary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif
                )
            }
        }
    }
}

@Composable
fun StarRating(rating: Float, starSize: androidx.compose.ui.unit.Dp) {
    val fullStars = rating.toInt()
    val hasHalf = (rating - fullStars) >= 0.25f
    val customColors = LocalCustomColors.current

    Row {
        for (i in 0 until 5) {
            val resId = when {
                i < fullStars -> R.drawable.ic_star_filled
                i == fullStars && hasHalf -> R.drawable.ic_star_half
                else -> R.drawable.ic_star_outline
            }
            if (i > 0) Spacer(modifier = Modifier.width(2.dp))
            Icon(
                painter = painterResource(resId),
                contentDescription = null,
                modifier = Modifier.size(starSize),
                tint = Color.Unspecified
            )
        }
    }
}

/** Sort dropdown + price/rating filter chips, shown above the product grid. */
@Composable
private fun SortFilterBar(
    sortOption: ProductSortOption,
    priceFilter: PriceRangeFilter,
    ratingFilter: RatingFilter,
    onSortChange: (ProductSortOption) -> Unit,
    onPriceChange: (PriceRangeFilter) -> Unit,
    onRatingChange: (RatingFilter) -> Unit
) {
    val context = LocalContext.current
    val customColors = LocalCustomColors.current
    var sortMenuOpen by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp, vertical = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Sort dropdown
            Box {
                Surface(
                    onClick = { sortMenuOpen = true },
                    shape = RoundedCornerShape(10.dp),
                    color = customColors.primaryLight
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = context.getString(sortOptionLabelRes(sortOption)),
                            color = Primary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            painter = painterResource(R.drawable.ic_chevron_down),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Primary
                        )
                    }
                }
                DropdownMenu(
                    expanded = sortMenuOpen,
                    onDismissRequest = { sortMenuOpen = false }
                ) {
                    ProductSortOption.entries.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(context.getString(sortOptionLabelRes(option))) },
                            onClick = {
                                sortMenuOpen = false
                                onSortChange(option)
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Rating filter chips
            LazyRow(modifier = Modifier.weight(1f)) {
                RatingFilter.entries.forEach { filter ->
                    item {
                        FilterChip(
                            selected = ratingFilter == filter,
                            onClick = { onRatingChange(filter) },
                            label = { Text(context.getString(ratingFilterLabelRes(filter)), fontSize = 12.sp) },
                            modifier = Modifier.padding(end = 6.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Price filter chips
        LazyRow(modifier = Modifier.fillMaxWidth()) {
            PriceRangeFilter.entries.forEach { filter ->
                item {
                    FilterChip(
                        selected = priceFilter == filter,
                        onClick = { onPriceChange(filter) },
                        label = { Text(context.getString(priceFilterLabelRes(filter)), fontSize = 12.sp) },
                        modifier = Modifier.padding(end = 6.dp)
                    )
                }
            }
        }
    }
}

private fun sortOptionLabelRes(option: ProductSortOption): Int = when (option) {
    ProductSortOption.RECOMMENDED -> R.string.sort_recommended
    ProductSortOption.PRICE_ASC -> R.string.sort_price_asc
    ProductSortOption.PRICE_DESC -> R.string.sort_price_desc
    ProductSortOption.RATING_DESC -> R.string.sort_rating
}

private fun priceFilterLabelRes(filter: PriceRangeFilter): Int = when (filter) {
    PriceRangeFilter.ALL -> R.string.filter_all_prices
    PriceRangeFilter.UNDER_500 -> R.string.filter_under_500
    PriceRangeFilter.RANGE_500_1500 -> R.string.filter_500_1500
    PriceRangeFilter.RANGE_1500_2500 -> R.string.filter_1500_2500
    PriceRangeFilter.OVER_2500 -> R.string.filter_over_2500
}

private fun ratingFilterLabelRes(filter: RatingFilter): Int = when (filter) {
    RatingFilter.ALL -> R.string.filter_all_ratings
    RatingFilter.AT_LEAST_4 -> R.string.filter_rating_4
    RatingFilter.AT_LEAST_4_5 -> R.string.filter_rating_4_5
}