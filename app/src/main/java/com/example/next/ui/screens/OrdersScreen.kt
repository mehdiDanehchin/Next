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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.next.R
import com.example.next.di.AppContainer
import com.example.next.models.Order
import com.example.next.models.OrderItem
import com.example.next.models.OrderStatus
import com.example.next.ui.theme.*
import com.example.next.viewmodel.OrdersViewModel
import java.util.Locale

/** Order history: list of placed orders with status, address and line items. */
@Composable
fun OrdersScreen(
    container: AppContainer,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val customColors = LocalCustomColors.current
    val colorScheme = MaterialTheme.colorScheme

    val ordersViewModel: OrdersViewModel = viewModel(factory = container.ordersViewModelFactory)
    val uiState by ordersViewModel.uiState.collectAsStateWithLifecycle()
    val itemsMap by ordersViewModel.items.collectAsStateWithLifecycle()
    var expandedOrderId by remember { mutableStateOf<Long?>(null) }

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
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                Icon(
                    painter = painterResource(R.drawable.ic_chevron_down),
                    contentDescription = context.getString(R.string.cd_back),
                    modifier = Modifier
                        .size(24.dp)
                        .graphicsLayer { rotationZ = -90f },
                    tint = colorScheme.onSurface
                )
            }
            Text(
                text = context.getString(R.string.orders_title),
                color = colorScheme.onSurface,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif,
                modifier = Modifier.weight(1f)
            )
        }

        if (uiState.orders.isEmpty()) {
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
                    text = context.getString(R.string.orders_empty),
                    color = colorScheme.onSurfaceVariant,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = context.getString(R.string.orders_empty_subtitle),
                    color = customColors.textHint,
                    fontSize = 14.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.orders, key = { it.id }) { order ->
                    OrderCard(
                        order = order,
                        items = itemsMap[order.id].orEmpty(),
                        expanded = expandedOrderId == order.id,
                        onToggle = {
                            expandedOrderId = if (expandedOrderId == order.id) null else order.id
                            ordersViewModel.loadItems(order.id)
                        },
                        onCancel = {
                            ordersViewModel.cancelOrder(order.id)
                            android.widget.Toast.makeText(
                                context,
                                context.getString(R.string.order_cancelled),
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun OrderCard(
    order: Order,
    items: List<OrderItem>,
    expanded: Boolean,
    onToggle: () -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val customColors = LocalCustomColors.current
    val colorScheme = MaterialTheme.colorScheme
    val status = order.statusEnum

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = customColors.surfaceWhite)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header row: order number + status badge (clickable to expand)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle)
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = context.getString(R.string.order_number, order.id),
                    color = colorScheme.onSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif,
                    modifier = Modifier.weight(1f)
                )
                StatusBadge(status = status)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = order.orderDate,
                    color = colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "$${String.format(Locale.US, "%.2f", order.total)}",
                    color = Primary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif
                )
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(thickness = 0.5.dp, color = customColors.divider)
                Spacer(modifier = Modifier.height(8.dp))

                // Line items
                items.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ProductImage(
                            imageName = item.imageUrl,
                            modifier = Modifier
                                .size(40.dp)
                                .background(colorScheme.background, RoundedCornerShape(4.dp))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.productName,
                                color = colorScheme.onSurface,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${item.quantity} × $${String.format(Locale.US, "%.2f", item.price)}",
                                color = colorScheme.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                        }
                        Text(
                            text = item.formattedTotal,
                            color = colorScheme.onSurface,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                HorizontalDivider(thickness = 0.5.dp, color = customColors.divider)
                Spacer(modifier = Modifier.height(8.dp))

                // Shipping address
                Text(
                    text = context.getString(R.string.checkout_address_title),
                    color = colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${order.fullName} — ${order.phone}",
                    color = colorScheme.onSurface,
                    fontSize = 13.sp
                )
                Text(
                    text = "${order.address}، ${order.city}",
                    color = colorScheme.onSurface,
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Totals
                PriceRow(
                    label = context.getString(R.string.order_subtotal),
                    value = "$${String.format(Locale.US, "%.2f", order.subtotal)}",
                    colorScheme = colorScheme
                )
                PriceRow(
                    label = context.getString(R.string.order_shipping),
                    value = if (order.shippingPrice == 0.0) context.getString(R.string.free)
                    else "$${String.format(Locale.US, "%.2f", order.shippingPrice)}",
                    colorScheme = colorScheme
                )
                PriceRow(
                    label = context.getString(R.string.order_total),
                    value = "$${String.format(Locale.US, "%.2f", order.total)}",
                    colorScheme = colorScheme,
                    emphasize = true
                )

                // Cancel while still pending
                if (status == OrderStatus.PENDING) {
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = onCancel,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = context.getString(R.string.order_cancel),
                            color = AccentRed,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(status: OrderStatus) {
    val color = statusColor(status)
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Text(
            text = stringResource(statusLabelRes(status)),
            color = color,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun PriceRow(
    label: String,
    value: String,
    colorScheme: androidx.compose.material3.ColorScheme,
    emphasize: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            color = if (emphasize) colorScheme.onSurface else colorScheme.onSurfaceVariant,
            fontSize = if (emphasize) 15.sp else 13.sp,
            fontWeight = if (emphasize) FontWeight.Bold else FontWeight.Normal,
            fontFamily = FontFamily.SansSerif
        )
    }
}

private fun statusColor(status: OrderStatus): Color = when (status) {
    OrderStatus.PENDING -> Color(0xFFD97706)
    OrderStatus.PROCESSING -> Primary
    OrderStatus.SHIPPED -> AccentOrange
    OrderStatus.DELIVERED -> AccentGreen
    OrderStatus.CANCELLED -> AccentRed
}

private fun statusLabelRes(status: OrderStatus): Int = when (status) {
    OrderStatus.PENDING -> R.string.status_pending
    OrderStatus.PROCESSING -> R.string.status_processing
    OrderStatus.SHIPPED -> R.string.status_shipped
    OrderStatus.DELIVERED -> R.string.status_delivered
    OrderStatus.CANCELLED -> R.string.status_cancelled
}
