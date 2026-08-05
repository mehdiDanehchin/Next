package com.example.next.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.next.database.DatabaseHelper
import com.example.next.models.CartItem
import com.example.next.models.Product
import com.example.next.models.WishlistItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Shared ViewModel for the whole activity: single source of truth for the cart and
 * wishlist. Screens observe these flows instead of holding their own snapshot state,
 * and the bottom navigation badges read the same counts. Refreshed from
 * MainActivity.onResume() so returning from ProductDetailActivity always shows fresh data.
 */
class StoreViewModel(application: Application) : AndroidViewModel(application) {

    private val dbHelper: DatabaseHelper
        get() = DatabaseHelper.getInstance(getApplication())

    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems.asStateFlow()

    private val _cartTotal = MutableStateFlow(0.0)
    val cartTotal: StateFlow<Double> = _cartTotal.asStateFlow()

    private val _cartCount = MutableStateFlow(0)
    val cartCount: StateFlow<Int> = _cartCount.asStateFlow()

    private val _wishlistItems = MutableStateFlow<List<WishlistItem>>(emptyList())
    val wishlistItems: StateFlow<List<WishlistItem>> = _wishlistItems.asStateFlow()

    private val _wishlistCount = MutableStateFlow(0)
    val wishlistCount: StateFlow<Int> = _wishlistCount.asStateFlow()

    /** Bumped on every wishlist change; home product cards re-check favorites when it changes. */
    private val _wishlistVersion = MutableStateFlow(0)
    val wishlistVersion: StateFlow<Int> = _wishlistVersion.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _cartItems.value = dbHelper.getAllCartItems()
            _cartTotal.value = dbHelper.getCartTotal()
            _cartCount.value = dbHelper.getCartItemCount()
            _wishlistItems.value = dbHelper.getAllWishlistItems()
            _wishlistCount.value = dbHelper.getWishlistCount()
        }
    }

    // ==================== PRODUCT OPERATIONS ====================

    suspend fun getProductById(id: Int): Product? = dbHelper.getProductById(id)

    // ==================== CART ====================

    fun addToCart(product: Product) {
        viewModelScope.launch {
            dbHelper.addToCart(product)
            refresh()
        }
    }

    fun updateCartQuantity(cartId: Int, quantity: Int) {
        viewModelScope.launch {
            dbHelper.updateCartQuantity(cartId, quantity)
            refresh()
        }
    }

    fun removeFromCart(cartId: Int) {
        viewModelScope.launch {
            dbHelper.removeFromCart(cartId)
            refresh()
        }
    }

    fun clearCart() {
        viewModelScope.launch {
            dbHelper.clearCart()
            refresh()
        }
    }

    // ==================== WISHLIST ====================

    fun addToWishlist(product: Product) {
        viewModelScope.launch {
            dbHelper.addToWishlist(product)
            refresh()
            _wishlistVersion.value++
        }
    }

    fun removeFromWishlist(productId: Int) {
        viewModelScope.launch {
            dbHelper.removeFromWishlist(productId)
            refresh()
            _wishlistVersion.value++
        }
    }
}
