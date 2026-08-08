package com.example.next.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.next.data.repository.CartRepository
import com.example.next.models.CartItem
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CartUiState(
    val items: List<CartItem> = emptyList(),
    val total: Double = 0.0,
    val count: Int = 0
)

/**
 * Cart state holder. Items/total/count are combined from Room flows into one
 * [CartUiState]; mutations go through the repository and the flows re-emit
 * automatically.
 */
class CartViewModel(private val cartRepository: CartRepository) : ViewModel() {

    val uiState: StateFlow<CartUiState> = combine(
        cartRepository.observeCartItems(),
        cartRepository.observeCartTotal(),
        cartRepository.observeCartCount()
    ) { items, total, count ->
        CartUiState(items = items, total = total, count = count)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CartUiState()
    )

    fun updateQuantity(cartId: Int, quantity: Int) {
        viewModelScope.launch { cartRepository.updateQuantity(cartId, quantity) }
    }

    fun removeItem(cartId: Int) {
        viewModelScope.launch { cartRepository.removeItem(cartId) }
    }

    fun clearCart() {
        viewModelScope.launch { cartRepository.clearCart() }
    }
}