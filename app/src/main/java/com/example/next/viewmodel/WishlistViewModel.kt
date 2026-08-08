package com.example.next.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.next.data.repository.WishlistRepository
import com.example.next.models.WishlistItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class WishlistUiState(
    val items: List<WishlistItem> = emptyList(),
    val count: Int = 0
)

/**
 * Wishlist state. Driven by Room flows, so removing an item (from this screen
 * or from any product card) is reflected here immediately.
 */
class WishlistViewModel(private val wishlistRepository: WishlistRepository) : ViewModel() {

    val uiState: StateFlow<WishlistUiState> = combine(
        wishlistRepository.observeWishlistItems(),
        wishlistRepository.observeWishlistCount()
    ) { items, count ->
        WishlistUiState(items = items, count = count)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = WishlistUiState()
    )

    fun removeItem(productId: Int) {
        viewModelScope.launch { wishlistRepository.removeFromWishlist(productId) }
    }

    fun isInWishlist(productId: Int): Flow<Boolean> = wishlistRepository.isInWishlist(productId)
}