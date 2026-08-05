package com.example.next.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.next.R
import com.example.next.database.DatabaseHelper
import com.example.next.models.Product
import com.example.next.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun ProductDetailScreen(
    productId: Int,
    dbHelper: DatabaseHelper,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val customColors = LocalCustomColors.current
    val colorScheme = MaterialTheme.colorScheme

    var product by remember { mutableStateOf<Product?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var isFavorite by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    LaunchedEffect(productId) {
        isLoading = true
        loadError = null
        try {
            val loadedProduct = dbHelper.getProductById(productId)
            if (loadedProduct == null) {
                loadError = context.getString(R.string.product_not_found)
            } else {
                product = loadedProduct
                try {
                    isFavorite = dbHelper.isInWishlist(loadedProduct.id)
                } catch (_: Exception) {
                }
            }
        } catch (e: Exception) {
            loadError = e.message ?: context.getString(R.string.load_error)
        } finally {
            isLoading = false
        }
    }

    if (isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = Primary)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = context.getString(R.string.loading_product),
                    color = colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
            }
        }
        return
    }

    if (loadError != null || product == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(32.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_image_placeholder),
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = customColors.textHint
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = loadError ?: context.getString(R.string.product_not_found),
                    color = colorScheme.onSurfaceVariant,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onBack,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) {
                    Text(
                        text = context.getString(R.string.go_back),
                        color = customColors.textOnPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif
                    )
                }
            }
        }
        return
    }

    val p = product!!

    fun toggleFavorite() {
        scope.launch {
            try {
                if (isFavorite) {
                    dbHelper.removeFromWishlist(p.id)
                    Toast.makeText(context, context.getString(R.string.removed_from_wishlist), Toast.LENGTH_SHORT).show()
                } else {
                    dbHelper.addToWishlist(p)
                    Toast.makeText(context, context.getString(R.string.added_to_wishlist), Toast.LENGTH_SHORT).show()
                }
                isFavorite = !isFavorite
            } catch (e: Exception) {
                Toast.makeText(context, context.getString(R.string.operation_failed), Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun addToCart() {
        scope.launch {
            try {
                dbHelper.addToCart(p)
                Toast.makeText(context, context.getString(R.string.added_to_cart, p.name), Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, context.getString(R.string.operation_failed), Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(customColors.surfaceWhite)
                .padding(start = 8.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                Icon(
                    painter = painterResource(R.drawable.ic_chevron_down),
                    contentDescription = context.getString(R.string.cd_back),
                    modifier = Modifier.size(24.dp),
                    tint = Color.Unspecified
                )
            }
            Text(
                text = context.getString(R.string.app_name),
                color = colorScheme.onSurface,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { toggleFavorite() }, modifier = Modifier.size(40.dp)) {
                Icon(
                    painter = painterResource(
                        if (isFavorite) R.drawable.ic_heart_filled else R.drawable.ic_heart
                    ),
                    contentDescription = context.getString(R.string.cd_favorite),
                    modifier = Modifier.size(28.dp),
                    tint = Color.Unspecified
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
        ) {
            ProductImage(
                imageName = p.imageUrl,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .background(customColors.surfaceWhite),
                contentScale = ContentScale.Crop
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = (-16).dp)
                    .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(containerColor = customColors.surfaceWhite)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = p.name,
                        color = colorScheme.onSurface,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = p.formattedPrice,
                        color = Primary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = p.category,
                        color = colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = context.getString(R.string.description),
                        color = colorScheme.onSurface,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = p.description.ifEmpty { context.getString(R.string.no_description) },
                        color = colorScheme.onSurfaceVariant,
                        fontSize = 14.sp,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = context.getString(R.string.specifications),
                        color = colorScheme.onSurface,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        colors = CardDefaults.cardColors(containerColor = colorScheme.background)
                    ) {
                        Text(
                            text = p.specifications.ifEmpty { context.getString(R.string.no_specifications) },
                            color = colorScheme.onSurfaceVariant,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 19.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shadowElevation = 8.dp,
            color = customColors.surfaceWhite
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { toggleFavorite() },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = customColors.primaryLight)
                ) {
                    Text(
                        text = if (isFavorite) context.getString(R.string.in_wishlist) else context.getString(R.string.add_to_wishlist),
                        color = customColors.textOnPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Button(
                    onClick = { addToCart() },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) {
                    Text(
                        text = context.getString(R.string.add_to_cart),
                        color = customColors.textOnPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif
                    )
                }
            }
        }
    }
}
