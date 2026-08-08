package com.example.next.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.next.data.repository.OrderRepository
import com.example.next.models.Order
import com.example.next.models.OrderItem
import com.example.next.models.OrderStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class OrdersUiState(
    val orders: List<Order> = emptyList(),
    val loading: Boolean = true
)

/**
 * Order history state holder. Orders stream from Room (newest first);
 * line items are fetched lazily per expanded order card and cached here.
 */
class OrdersViewModel(private val orderRepository: OrderRepository) : ViewModel() {

    val uiState: StateFlow<OrdersUiState> = orderRepository.observeOrders()
        .map { orders -> OrdersUiState(orders = orders, loading = false) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = OrdersUiState()
        )

    private val _items = MutableStateFlow<Map<Long, List<OrderItem>>>(emptyMap())

    /** orderId -> line items, cached once loaded so toggling an expanded card doesn't re-query. */
    val items: StateFlow<Map<Long, List<OrderItem>>> = _items

    fun loadItems(orderId: Long) {
        if (_items.value.containsKey(orderId)) return
        viewModelScope.launch {
            _items.value = _items.value + (orderId to orderRepository.getItemsForOrder(orderId))
        }
    }

    fun cancelOrder(orderId: Long) {
        viewModelScope.launch { orderRepository.updateStatus(orderId, OrderStatus.CANCELLED) }
    }
}
