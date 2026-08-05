package com.example.next.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.next.ProductDetailActivity
import com.example.next.R
import com.example.next.database.DatabaseHelper
import com.example.next.models.Product
import com.example.next.ui.theme.*
import com.example.next.viewmodel.StoreViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    contentScale: ContentScale = ContentScale.Crop
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(dbHelper: DatabaseHelper, storeViewModel: StoreViewModel) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val customColors = LocalCustomColors.current
    val colorScheme = MaterialTheme.colorScheme

    var featuredProducts by remember { mutableStateOf<List<Product>>(emptyList()) }
    var popularProducts by remember { mutableStateOf<List<Product>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var noResultsVisible by remember { mutableStateOf(false) }
    var isSearchActive by remember { mutableStateOf(false) }
    var activeFilterCategory by remember { mutableStateOf<String?>(null) }

    val isFilterActive = activeFilterCategory != null || isSearchActive

    val categories = remember {
        listOf(
            CategoryItem("Phones", R.string.category_phones, R.drawable.ic_phone, customColors.phonesBg),
            CategoryItem("Laptops", R.string.category_laptops, R.drawable.ic_laptop, customColors.laptopsBg),
            CategoryItem("Cameras", R.string.category_cameras, R.drawable.ic_camera, customColors.camerasBg),
            CategoryItem("Accessories", R.string.category_accessories, R.drawable.ic_accessories, customColors.accessoriesBg),
            CategoryItem("All", R.string.all_products, R.drawable.ic_home, customColors.primaryLight)
        )
    }

    LaunchedEffect(Unit) {
        featuredProducts = dbHelper.getFeaturedProducts()
        popularProducts = dbHelper.getPopularProducts()
    }

    fun restoreFullView() {
        isSearchActive = false
        activeFilterCategory = null
        noResultsVisible = false
        searchQuery = ""
        focusManager.clearFocus()
        scope.launch {
            featuredProducts = dbHelper.getFeaturedProducts()
            popularProducts = dbHelper.getPopularProducts()
        }
    }

    fun categoryDisplayName(category: String): String =
        categories.firstOrNull { it.key == category }?.let { context.getString(it.nameResId) } ?: category

    fun handleCategoryClick(category: String) {
        if (category == "All") {
            restoreFullView()
        } else {
            scope.launch {
                activeFilterCategory = category
                isSearchActive = false
                searchQuery = ""
                val filtered = dbHelper.getProductsByCategory(category)
                noResultsVisible = filtered.isEmpty()
                popularProducts = filtered
                featuredProducts = emptyList()
            }
        }
    }

    LaunchedEffect(searchQuery) {
        if (searchQuery.isEmpty()) {
            if (isSearchActive) {
                delay(200)
                isSearchActive = false
                noResultsVisible = false
                featuredProducts = dbHelper.getFeaturedProducts()
                popularProducts = dbHelper.getPopularProducts()
            }
            return@LaunchedEffect
        }

        delay(300)

        isSearchActive = true
        activeFilterCategory = null
        val results = dbHelper.searchProducts(searchQuery)
        noResultsVisible = results.isEmpty()
        popularProducts = results
        featuredProducts = emptyList()
    }

    fun openProductDetail(product: Product) {
        val intent = Intent(context, ProductDetailActivity::class.java)
        intent.putExtra("product_id", product.id)
        context.startActivity(intent)
    }

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
                        onClick = { restoreFullView() },
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
                        isSearchActive -> context.getString(R.string.search_results)
                        activeFilterCategory != null -> categoryDisplayName(activeFilterCategory!!)
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
                        onClick = { restoreFullView() },
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
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
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

        // Scrollable content
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            if (noResultsVisible) {
                item {
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
                item {
                    Text(
                        text = context.getString(R.string.categories),
                        color = colorScheme.onSurface,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif,
                        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp)
                    )
                }
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(categories) { cat ->
                            CategoryChip(cat, onClick = { handleCategoryClick(cat.key) })
                        }
                    }
                }

                if (featuredProducts.isNotEmpty()) {
                    item {
                        Text(
                            text = context.getString(R.string.featured_products),
                            color = colorScheme.onSurface,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif,
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp)
                        )
                    }
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(featuredProducts, key = { it.id }) { product ->
                                ProductCard(
                                    product = product,
                                    dbHelper = dbHelper,
                                    storeViewModel = storeViewModel,
                                    onClick = { openProductDetail(it) }
                                )
                            }
                        }
                    }
                }
            }

            if (popularProducts.isNotEmpty()) {
                item {
                    Text(
                        text = when {
                            isSearchActive -> ""
                            activeFilterCategory != null -> categoryDisplayName(activeFilterCategory!!)
                            else -> context.getString(R.string.popular_products)
                        },
                        color = colorScheme.onSurface,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp)
                    )
                }
                val rows = popularProducts.chunked(2)
                items(rows) { row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        for (product in row) {
                            ProductCard(
                                product = product,
                                dbHelper = dbHelper,
                                storeViewModel = storeViewModel,
                                onClick = { openProductDetail(it) }
                            )
                        }
                        if (row.size == 1) {
                            Spacer(modifier = Modifier.width(172.dp))
                        }
                    }
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

@Composable
fun ProductCard(
    product: Product,
    dbHelper: DatabaseHelper,
    storeViewModel: StoreViewModel,
    onClick: (Product) -> Unit
) {
    var isFav by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val customColors = LocalCustomColors.current
    val colorScheme = MaterialTheme.colorScheme
    val wishlistVersion by storeViewModel.wishlistVersion.collectAsStateWithLifecycle()

    LaunchedEffect(product.id, wishlistVersion) {
        isFav = dbHelper.isInWishlist(product.id)
    }

    Card(
        modifier = Modifier
            .width(160.dp)
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
                ProductImage(
                    imageName = product.imageUrl,
                    modifier = Modifier.fillMaxSize()
                )

                IconButton(
                    onClick = {
                        val newFavState = !isFav
                        isFav = newFavState
                        scope.launch {
                            try {
                                if (newFavState) {
                                    storeViewModel.addToWishlist(product)
                                    android.widget.Toast.makeText(context, context.getString(R.string.added_to_wishlist), android.widget.Toast.LENGTH_SHORT).show()
                                } else {
                                    storeViewModel.removeFromWishlist(product.id)
                                    android.widget.Toast.makeText(context, context.getString(R.string.removed_from_wishlist), android.widget.Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                isFav = !newFavState
                                android.widget.Toast.makeText(context, context.getString(R.string.operation_failed), android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
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
                        text = "(%.1f)".format(product.rating),
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
