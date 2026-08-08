package com.example.next.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.next.R
import com.example.next.data.repository.CartRepository
import com.example.next.data.repository.ProductRepository
import com.example.next.data.repository.WishlistRepository
import com.example.next.models.Product
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProductDetailUiState(
    val product: Product? = null,
    val isLoading: Boolean = true,
    val errorRes: Int? = null,
    val isFavorite: Boolean = false
)

/** One-shot UI feedback (toasts) produced by the ViewModel. */
sealed interface ProductDetailEvent {
    data class Toast(val messageRes: Int, val formatArg: String? = null) : ProductDetailEvent
}

/**
 * Product detail state holder. Reads the product id from the navigation
 * arguments via [SavedStateHandle] (single-activity navigation).
 */
class ProductDetailViewModel(
    savedStateHandle: SavedStateHandle,
    private val productRepository: ProductRepository,
    private val wishlistRepository: WishlistRepository,
    private val cartRepository: CartRepository
) : ViewModel() {

    private val productId: Int = savedStateHandle.get<Int>("productId") ?: -1

    private val _uiState = MutableStateFlow(ProductDetailUiState())
    val uiState: StateFlow<ProductDetailUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ProductDetailEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<ProductDetailEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            try {
                val product = productRepository.getProductById(productId)
                _uiState.update {
                    if (product == null) {
                        it.copy(isLoading = false, errorRes = R.string.product_not_found)
                    } else {
                        it.copy(isLoading = false, product = product)
                    }
                }
            } catch (_: Exception) {
                _uiState.update { it.copy(isLoading = false, errorRes = R.string.load_error) }
            }
        }

        // Favorite state is reactive: it updates automatically when the wishlist changes.
        viewModelScope.launch {
            wishlistRepository.isInWishlist(productId).collect { favorite ->
                _uiState.update { it.copy(isFavorite = favorite) }
            }
        }
    }

    fun toggleFavorite() {
        val product = _uiState.value.product ?: return
        viewModelScope.launch {
            try {
                if (_uiState.value.isFavorite) {
                    wishlistRepository.removeFromWishlist(product.id)
                    _events.emit(ProductDetailEvent.Toast(R.string.removed_from_wishlist))
                } else {
                    wishlistRepository.addToWishlist(product)
                    _events.emit(ProductDetailEvent.Toast(R.string.added_to_wishlist))
                }
            } catch (_: Exception) {
                _events.emit(ProductDetailEvent.Toast(R.string.operation_failed))
            }
        }
    }

    fun addToCart() {
        val product = _uiState.value.product ?: return
        viewModelScope.launch {
            try {
                cartRepository.addToCart(product)
                _events.emit(ProductDetailEvent.Toast(R.string.added_to_cart, product.name))
            } catch (_: Exception) {
                _events.emit(ProductDetailEvent.Toast(R.string.operation_failed))
            }
        }
    }
}