package com.example.next.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.next.R
import com.example.next.data.repository.CartRepository
import com.example.next.data.repository.OrderRepository
import com.example.next.models.CartItem
import com.example.next.models.Order
import com.example.next.models.OrderItem
import com.example.next.models.ShippingMethod
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CheckoutUiState(
    val items: List<CartItem> = emptyList(),
    val subtotal: Double = 0.0,
    val count: Int = 0,
    val placing: Boolean = false
)

/** One-shot checkout outcomes; the screen maps them to UI feedback + navigation. */
sealed interface CheckoutEvent {
    data class OrderPlaced(val orderId: Long) : CheckoutEvent
    data class Error(val messageRes: Int) : CheckoutEvent
}

/**
 * Checkout state holder: cart summary (live from Room) + placing an order.
 * [placeOrder] validates the address fields, snapshots the cart into an
 * [Order] with line items, and clears the cart — all in one DB transaction.
 */
class CheckoutViewModel(
    private val cartRepository: CartRepository,
    private val orderRepository: OrderRepository
) : ViewModel() {

    private val _placing = MutableStateFlow(false)
    private val _events = MutableSharedFlow<CheckoutEvent>()

    val uiState: StateFlow<CheckoutUiState> = combine(
        cartRepository.observeCartItems(),
        cartRepository.observeCartTotal(),
        cartRepository.observeCartCount(),
        _placing
    ) { items, total, count, placing ->
        CheckoutUiState(items = items, subtotal = total, count = count, placing = placing)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CheckoutUiState()
    )

    val events: SharedFlow<CheckoutEvent> = _events

    fun placeOrder(
        fullName: String,
        phone: String,
        city: String,
        address: String,
        shippingMethod: ShippingMethod
    ) {
        val trimmed = listOf(fullName, phone, city, address).map { it.trim() }
        if (trimmed.any { it.isEmpty() }) {
            viewModelScope.launch { _events.emit(CheckoutEvent.Error(R.string.checkout_validation)) }
            return
        }
        if (uiState.value.items.isEmpty()) return

        viewModelScope.launch {
            _placing.value = true
            try {
                val cart = uiState.value
                val orderId = orderRepository.placeOrder(
                    order = Order(
                        orderDate = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date()),
                        fullName = trimmed[0],
                        phone = trimmed[1],
                        city = trimmed[2],
                        address = trimmed[3],
                        shippingMethod = shippingMethod.code,
                        shippingPrice = shippingMethod.price,
                        subtotal = cart.subtotal,
                        total = cart.subtotal + shippingMethod.price
                    ),
                    items = cart.items.map {
                        OrderItem(
                            productId = it.productId,
                            productName = it.productName,
                            price = it.price,
                            imageUrl = it.imageUrl,
                            quantity = it.quantity
                        )
                    }
                )
                _events.emit(CheckoutEvent.OrderPlaced(orderId))
            } catch (e: Exception) {
                _events.emit(CheckoutEvent.Error(R.string.checkout_failed))
            } finally {
                _placing.value = false
            }
        }
    }
}
