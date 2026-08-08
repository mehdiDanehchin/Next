package com.example.next.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.next.R
import com.example.next.di.AppContainer
import com.example.next.models.CartItem
import com.example.next.models.ShippingMethod
import com.example.next.ui.theme.*
import com.example.next.viewmodel.CartViewModel
import com.example.next.viewmodel.CheckoutEvent
import com.example.next.viewmodel.CheckoutViewModel
import java.util.Locale

/**
 * Real checkout flow: order summary, shipping address form, shipping method
 * selection and a Place Order action that persists the order and clears the
 * cart. Bottom bar is hidden (it is a full-screen flow destination).
 */
@Composable
fun CheckoutScreen(
    container: AppContainer,
    cartViewModel: CartViewModel,
    onBack: () -> Unit,
    onOrderPlaced: () -> Unit
) {
    val context = LocalContext.current
    val customColors = LocalCustomColors.current
    val colorScheme = MaterialTheme.colorScheme

    val checkoutViewModel: CheckoutViewModel = viewModel(factory = container.checkoutViewModelFactory)
    val uiState by checkoutViewModel.uiState.collectAsStateWithLifecycle()

    var fullName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var shippingMethod by remember { mutableStateOf(ShippingMethod.STANDARD) }

    LaunchedEffect(Unit) {
        checkoutViewModel.events.collect { event ->
            when (event) {
                is CheckoutEvent.OrderPlaced -> {
                    android.widget.Toast.makeText(
                        context,
                        context.getString(R.string.order_placed, event.orderId),
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                    onOrderPlaced()
                }
                is CheckoutEvent.Error -> {
                    android.widget.Toast.makeText(
                        context,
                        context.getString(event.messageRes),
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
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
                text = context.getString(R.string.checkout_title),
                color = colorScheme.onSurface,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif,
                modifier = Modifier.weight(1f)
            )
        }

        if (uiState.items.isEmpty()) {
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
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Order summary
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        colors = CardDefaults.cardColors(containerColor = customColors.surfaceWhite)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = context.getString(R.string.checkout_your_items),
                                color = colorScheme.onSurface,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.SansSerif
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            uiState.items.forEach { item ->
                                CheckoutItemRow(item = item, colorScheme = colorScheme)
                                Spacer(modifier = Modifier.height(6.dp))
                            }
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 8.dp),
                                thickness = 0.5.dp,
                                color = customColors.divider
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = context.getString(R.string.order_subtotal),
                                    color = colorScheme.onSurfaceVariant,
                                    fontSize = 14.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "$${String.format(Locale.US, "%.2f", uiState.subtotal)}",
                                    color = colorScheme.onSurface,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.SansSerif
                                )
                            }
                        }
                    }
                }

                // Shipping address form
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        colors = CardDefaults.cardColors(containerColor = customColors.surfaceWhite)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = context.getString(R.string.checkout_address_title),
                                color = colorScheme.onSurface,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.SansSerif
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            CheckoutTextField(
                                value = fullName,
                                onValueChange = { fullName = it },
                                label = context.getString(R.string.checkout_full_name),
                                colorScheme = colorScheme
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            CheckoutTextField(
                                value = phone,
                                onValueChange = { phone = it },
                                label = context.getString(R.string.checkout_phone),
                                colorScheme = colorScheme,
                                keyboardType = KeyboardType.Phone
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            CheckoutTextField(
                                value = city,
                                onValueChange = { city = it },
                                label = context.getString(R.string.checkout_city),
                                colorScheme = colorScheme
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            CheckoutTextField(
                                value = address,
                                onValueChange = { address = it },
                                label = context.getString(R.string.checkout_address),
                                colorScheme = colorScheme
                            )
                        }
                    }
                }

                // Shipping method
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        colors = CardDefaults.cardColors(containerColor = customColors.surfaceWhite)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = context.getString(R.string.checkout_shipping_title),
                                color = colorScheme.onSurface,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.SansSerif
                            )
                            ShippingMethod.entries.forEach { method ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { shippingMethod = method }
                                        .padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = shippingMethod == method,
                                        onClick = { shippingMethod = method }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = context.getString(shippingMethodLabelRes(method)),
                                        color = colorScheme.onSurface,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = if (method.price == 0.0) context.getString(R.string.free)
                                        else "$${String.format(Locale.US, "%.2f", method.price)}",
                                        color = Primary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.SansSerif
                                    )
                                }
                            }
                        }
                    }
                }

                // Bottom spacer so content clears the total bar
                item { Spacer(modifier = Modifier.height(8.dp)) }
            }

            // Total + place order bar
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
                            text = context.getString(R.string.order_total),
                            color = colorScheme.onSurfaceVariant,
                            fontSize = 16.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "$${String.format(Locale.US, "%.2f", uiState.subtotal + shippingMethod.price)}",
                            color = colorScheme.onSurface,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif
                        )
                    }

                    Button(
                        onClick = {
                            checkoutViewModel.placeOrder(
                                fullName = fullName,
                                phone = phone,
                                city = city,
                                address = address,
                                shippingMethod = shippingMethod
                            )
                        },
                        enabled = !uiState.placing,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary)
                    ) {
                        if (uiState.placing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = customColors.textOnPrimary
                            )
                        } else {
                            Text(
                                text = context.getString(R.string.place_order),
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
}

@Composable
private fun CheckoutItemRow(item: CartItem, colorScheme: androidx.compose.material3.ColorScheme) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        ProductImage(
            imageName = item.imageUrl,
            modifier = Modifier
                .size(44.dp)
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
                text = "${item.quantity} × ${item.formattedPrice}",
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

@Composable
private fun CheckoutTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    colorScheme: androidx.compose.material3.ColorScheme,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Primary,
            unfocusedBorderColor = colorScheme.outlineVariant
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

private fun shippingMethodLabelRes(method: ShippingMethod): Int = when (method) {
    ShippingMethod.STANDARD -> R.string.shipping_standard
    ShippingMethod.EXPRESS -> R.string.shipping_express
    ShippingMethod.PICKUP -> R.string.shipping_pickup
}
