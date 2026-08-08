package com.example.next.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.next.R
import com.example.next.models.CartItem
import com.example.next.ui.theme.*
import com.example.next.viewmodel.CartViewModel
import java.util.Locale

@Composable
fun CartScreen(
    cartViewModel: CartViewModel,
    onCheckout: () -> Unit
) {
    val context = LocalContext.current
    val customColors = LocalCustomColors.current
    val colorScheme = MaterialTheme.colorScheme

    val uiState by cartViewModel.uiState.collectAsStateWithLifecycle()
    val cartItems = uiState.items
    val cartTotal = uiState.total
    val cartItemCount = uiState.count

    fun handleQuantityChanged(item: CartItem, newQuantity: Int) {
        cartViewModel.updateQuantity(item.id, newQuantity)
    }

    fun handleRemoveItem(item: CartItem) {
        cartViewModel.removeItem(item.id)
        android.widget.Toast.makeText(
            context,
            context.getString(R.string.removed_from_cart, item.productName),
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(customColors.surfaceWhite)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = context.getString(R.string.nav_cart),
                color = colorScheme.onSurface,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif,
                modifier = Modifier.weight(1f)
            )
            if (cartItems.isNotEmpty()) {
                Text(
                    text = context.resources.getQuantityString(
                        R.plurals.cart_item_count, cartItemCount, cartItemCount
                    ),
                    color = colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
            }
        }

        if (cartItems.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colorScheme.background),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_cart),
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = context.getString(R.string.cart_empty),
                    color = colorScheme.onSurfaceVariant,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = context.getString(R.string.cart_start_shopping),
                    color = customColors.textHint,
                    fontSize = 14.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(4.dp)
            ) {
                items(cartItems, key = { it.id }) { item ->
                    CartItemCard(
                        item = item,
                        onQuantityChanged = { handleQuantityChanged(item, it) },
                        onRemove = { handleRemoveItem(item) }
                    )
                }
            }

            // Bottom checkout card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                shadowElevation = 4.dp,
                color = customColors.surfaceWhite
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = context.getString(R.string.cart_total),
                            color = colorScheme.onSurfaceVariant,
                            fontSize = 16.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "$${String.format(Locale.US, "%.2f", cartTotal)}",
                            color = colorScheme.onSurface,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif
                        )
                    }

                    Button(
                        onClick = onCheckout,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary)
                    ) {
                        Text(
                            text = context.getString(R.string.checkout),
                            color = customColors.textOnPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CartItemCard(
    item: CartItem,
    onQuantityChanged: (Int) -> Unit,
    onRemove: () -> Unit
) {
    val customColors = LocalCustomColors.current
    val colorScheme = MaterialTheme.colorScheme

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
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
                    .size(70.dp)
                    .background(colorScheme.background, RoundedCornerShape(4.dp))
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.productName,
                    color = colorScheme.onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.formattedTotal,
                    color = Primary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(colorScheme.background)
                            .clickable { onQuantityChanged(item.quantity - 1) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_minus),
                            contentDescription = stringResource(R.string.cd_decrease),
                            modifier = Modifier.size(16.dp),
                            tint = colorScheme.onSurface
                        )
                    }

                    Text(
                        text = "${item.quantity}",
                        color = colorScheme.onSurface,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.SansSerif,
                        modifier = Modifier.width(40.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(customColors.primaryLight)
                            .clickable { onQuantityChanged(item.quantity + 1) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_plus),
                            contentDescription = stringResource(R.string.cd_increase),
                            modifier = Modifier.size(16.dp),
                            tint = Primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_delete),
                    contentDescription = stringResource(R.string.cd_remove),
                    modifier = Modifier.size(24.dp),
                    tint = AccentRed
                )
            }
        }
    }
}
