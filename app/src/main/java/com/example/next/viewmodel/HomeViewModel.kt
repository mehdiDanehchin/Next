package com.example.next.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.next.data.repository.ProductRepository
import com.example.next.data.repository.WishlistRepository
import com.example.next.models.Product
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** How the visible product list is ordered. */
enum class ProductSortOption { RECOMMENDED, PRICE_ASC, PRICE_DESC, RATING_DESC }

/** Price-band filter applied to the visible product list. */
enum class PriceRangeFilter { ALL, UNDER_500, RANGE_500_1500, RANGE_1500_2500, OVER_2500 }

/** Minimum-rating filter applied to the visible product list. */
enum class RatingFilter { ALL, AT_LEAST_4, AT_LEAST_4_5 }

data class HomeUiState(
    val featuredProducts: List<Product> = emptyList(),
    /** Currently displayed product grid: popular items, category filter results, or search results. */
    val visibleProducts: List<Product> = emptyList(),
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
    val activeCategory: String? = null,
    val sortOption: ProductSortOption = ProductSortOption.RECOMMENDED,
    val priceFilter: PriceRangeFilter = PriceRangeFilter.ALL,
    val ratingFilter: RatingFilter = RatingFilter.ALL,
    val noResults: Boolean = false,
    val isLoading: Boolean = false
)

/**
 * Home screen state holder: catalog sections, category filtering, debounced
 * search, and sort/filter of the visible list — all driven through a single
 * [HomeUiState] StateFlow. The raw dataset for the current view lives in
 * [baseProducts]; sort/filter options are applied on top of it.
 */
class HomeViewModel(
    private val productRepository: ProductRepository,
    private val wishlistRepository: WishlistRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState(isLoading = true))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    /** Raw dataset for the current view (popular / category / search), before sort+filter. */
    private var baseProducts: List<Product> = emptyList()

    private var searchJob: Job? = null

    init {
        loadInitial()
    }

    fun loadInitial() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val featured = productRepository.getFeaturedProducts()
            val popular = productRepository.getPopularProducts()
            baseProducts = popular
            _uiState.update {
                it.copy(
                    featuredProducts = featured,
                    isLoading = false,
                    searchQuery = "",
                    isSearchActive = false,
                    activeCategory = null
                )
            }
            applySortFilter()
        }
    }

    /** Reactive favourite state for a single product (used by product cards). */
    fun isInWishlist(productId: Int): Flow<Boolean> = wishlistRepository.isInWishlist(productId)

    fun toggleWishlist(product: Product) {
        viewModelScope.launch {
            if (wishlistRepository.isInWishlistNow(product.id)) {
                wishlistRepository.removeFromWishlist(product.id)
            } else {
                wishlistRepository.addToWishlist(product)
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        searchJob?.cancel()
        _uiState.update { it.copy(searchQuery = query) }

        if (query.isBlank()) {
            if (_uiState.value.isSearchActive) {
                // Give the TextField a moment to settle before restoring the grid.
                searchJob = viewModelScope.launch {
                    delay(200)
                    loadInitial()
                    _uiState.update { it.copy(noResults = false) }
                }
            }
            return
        }

        searchJob = viewModelScope.launch {
            delay(300)
            val results = productRepository.searchProducts(query.trim())
            baseProducts = results
            _uiState.update {
                it.copy(
                    isSearchActive = true,
                    activeCategory = null
                )
            }
            applySortFilter()
        }
    }

    fun selectCategory(category: String) {
        if (category == "All") {
            restoreFullView()
            return
        }
        searchJob?.cancel()
        viewModelScope.launch {
            val filtered = productRepository.getProductsByCategory(category)
            baseProducts = filtered
            _uiState.update {
                it.copy(
                    activeCategory = category,
                    isSearchActive = false,
                    searchQuery = "",
                    featuredProducts = emptyList()
                )
            }
            applySortFilter()
        }
    }

    fun restoreFullView() {
        searchJob?.cancel()
        viewModelScope.launch {
            val featured = productRepository.getFeaturedProducts()
            val popular = productRepository.getPopularProducts()
            baseProducts = popular
            _uiState.update {
                it.copy(
                    featuredProducts = featured,
                    searchQuery = "",
                    isSearchActive = false,
                    activeCategory = null
                )
            }
            applySortFilter()
        }
    }

    fun setSortOption(option: ProductSortOption) {
        _uiState.update { it.copy(sortOption = option) }
        applySortFilter()
    }

    fun setPriceFilter(filter: PriceRangeFilter) {
        _uiState.update { it.copy(priceFilter = filter) }
        applySortFilter()
    }

    fun setRatingFilter(filter: RatingFilter) {
        _uiState.update { it.copy(ratingFilter = filter) }
        applySortFilter()
    }

    /** Re-derives the visible list from the base dataset using the current sort/filter options. */
    private fun applySortFilter() {
        val state = _uiState.value
        var result = baseProducts

        result = when (state.priceFilter) {
            PriceRangeFilter.ALL -> result
            PriceRangeFilter.UNDER_500 -> result.filter { it.price < 500.0 }
            PriceRangeFilter.RANGE_500_1500 -> result.filter { it.price >= 500.0 && it.price < 1500.0 }
            PriceRangeFilter.RANGE_1500_2500 -> result.filter { it.price >= 1500.0 && it.price < 2500.0 }
            PriceRangeFilter.OVER_2500 -> result.filter { it.price >= 2500.0 }
        }

        result = when (state.ratingFilter) {
            RatingFilter.ALL -> result
            RatingFilter.AT_LEAST_4 -> result.filter { it.rating >= 4.0f }
            RatingFilter.AT_LEAST_4_5 -> result.filter { it.rating >= 4.5f }
        }

        result = when (state.sortOption) {
            ProductSortOption.RECOMMENDED -> result
            ProductSortOption.PRICE_ASC -> result.sortedBy { it.price }
            ProductSortOption.PRICE_DESC -> result.sortedByDescending { it.price }
            ProductSortOption.RATING_DESC -> result.sortedByDescending { it.rating }
        }

        _uiState.update {
            it.copy(visibleProducts = result, noResults = result.isEmpty() && baseProducts.isNotEmpty())
        }
    }
}
