package com.example.next.di

import android.content.Context
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.next.NextApplication
import com.example.next.database.AppDatabase
import com.example.next.data.repository.CartRepository
import com.example.next.data.repository.OrderRepository
import com.example.next.data.repository.ProductRepository
import com.example.next.data.repository.SettingsRepository
import com.example.next.data.repository.WishlistRepository
import com.example.next.viewmodel.CartViewModel
import com.example.next.viewmodel.CheckoutViewModel
import com.example.next.viewmodel.HomeViewModel
import com.example.next.viewmodel.OrdersViewModel
import com.example.next.viewmodel.ProductDetailViewModel
import com.example.next.viewmodel.WishlistViewModel

/**
 * Manual dependency-injection container (the "AppContainer" pattern — no Hilt
 * needed for a project this size). Owns the long-lived singletons (Room DB,
 * repositories) and builds ViewModel factories wired to them.
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    // ---------- Data layer ----------

    val database: AppDatabase = AppDatabase.build(appContext)

    val productRepository = ProductRepository(database.productDao())

    val cartRepository = CartRepository(database.cartDao())

    val wishlistRepository = WishlistRepository(database.wishlistDao())

    val orderRepository = OrderRepository(database, database.orderDao(), database.cartDao())

    val settingsRepository = SettingsRepository(appContext)

    // ---------- ViewModel factories ----------

    val homeViewModelFactory = viewModelFactory {
        initializer {
            HomeViewModel(
                productRepository = productRepository,
                wishlistRepository = wishlistRepository
            )
        }
    }

    val productDetailViewModelFactory = viewModelFactory {
        initializer {
            ProductDetailViewModel(
                savedStateHandle = createSavedStateHandle(),
                productRepository = productRepository,
                wishlistRepository = wishlistRepository,
                cartRepository = cartRepository
            )
        }
    }

    val cartViewModelFactory = viewModelFactory {
        initializer {
            CartViewModel(cartRepository = cartRepository)
        }
    }

    val wishlistViewModelFactory = viewModelFactory {
        initializer {
            WishlistViewModel(wishlistRepository = wishlistRepository)
        }
    }

    val checkoutViewModelFactory = viewModelFactory {
        initializer {
            CheckoutViewModel(
                cartRepository = cartRepository,
                orderRepository = orderRepository
            )
        }
    }

    val ordersViewModelFactory = viewModelFactory {
        initializer {
            OrdersViewModel(orderRepository = orderRepository)
        }
    }
}

/** Access the container from any [CreationExtras] (e.g. inside a custom factory). */
fun CreationExtras.appContainer(): AppContainer {
    val app = this[APPLICATION_KEY] as? NextApplication
        ?: error("Expected NextApplication in CreationExtras, got: ${this[APPLICATION_KEY]}")
    return app.container
}