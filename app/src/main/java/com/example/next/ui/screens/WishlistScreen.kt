package com.example.next.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.next.ProductDetailActivity
import com.example.next.R
import com.example.next.models.WishlistItem
import com.example.next.ui.theme.*
import com.example.next.viewmodel.StoreViewModel
import kotlinx.coroutines.launch

@Composable
fun WishlistScreen(storeViewModel: StoreViewModel) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val customColors = LocalCustomColors.current
    val colorScheme = MaterialTheme.colorScheme

    val wishlistItems by storeViewModel.wishlistItems.collectAsStateWithLifecycle()
    val wishlistCount by storeViewModel.wishlistCount.collectAsStateWithLifecycle()

    fun handleRemove(item: WishlistItem) {
        storeViewModel.removeFromWishlist(item.productId)
        android.widget.Toast.makeText(
            context,
            context.getString(R.string.removed_from_wishlist),
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }

    fun handleItemClick(item: WishlistItem) {
        scope.launch {
            val product = storeViewModel.getProductById(item.productId)
            if (product != null) {
                val intent = Intent(context, ProductDetailActivity::class.java)
                intent.putExtra("product_id", product.id)
                context.startActivity(intent)
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
                .background(customColors.surfaceWhite)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = context.getString(R.string.nav_wishlist),
                color = colorScheme.onSurface,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif,
                modifier = Modifier.weight(1f)
            )
            if (wishlistItems.isNotEmpty()) {
                Text(
                    text = context.resources.getQuantityString(
                        R.plurals.wishlist_item_count, wishlistCount, wishlistCount
                    ),
                    color = colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
            }
        }

        if (wishlistItems.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colorScheme.background),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_heart),
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = context.getString(R.string.wishlist_empty),
                    color = colorScheme.onSurfaceVariant,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = context.getString(R.string.wishlist_empty_subtitle),
                    color = customColors.textHint,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(horizontal = 32.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(4.dp)) {
                itemsIndexed(wishlistItems, key = { _, item -> item.id }) { index, item ->
                    WishlistItemCard(
                        item = item,
                        onClick = { handleItemClick(item) },
                        onRemove = { handleRemove(item) }
                    )
                }
            }
        }
    }
}

@Composable
fun WishlistItemCard(
    item: WishlistItem,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    val customColors = LocalCustomColors.current
    val colorScheme = MaterialTheme.colorScheme

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = customColors.surfaceWhite)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProductImage(
                imageName = item.imageUrl,
                modifier = Modifier
                    .size(80.dp)
                    .background(colorScheme.background, RoundedCornerShape(4.dp))
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.productName,
                    color = colorScheme.onSurface,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = item.formattedPrice,
                    color = Primary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_close),
                    contentDescription = stringResource(R.string.cd_remove),
                    modifier = Modifier.size(24.dp),
                    tint = colorScheme.onSurfaceVariant
                )
            }
        }
    }
}