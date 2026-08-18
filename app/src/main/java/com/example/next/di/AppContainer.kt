package com.example.next.di

import android.content.Context
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.next.NextApplication
import com.example.next.database.AppDatabase
import com.example.next.data.repository.AuthRepository
import com.example.next.data.repository.CartRepository
import com.example.next.data.repository.OrderRepository
import com.example.next.data.repository.ProductRepository
import com.example.next.data.repository.SettingsRepository
import com.example.next.data.repository.UserRepository
import com.example.next.data.repository.WishlistRepository
import com.example.next.data.session.OwnershipBackfill
import com.example.next.data.session.SessionManager
import com.example.next.data.sync.AccountSwitchHandler
import com.example.next.data.sync.GuestMerger
import com.example.next.data.sync.SyncCoordinator
import com.example.next.data.sync.SyncEngine
import com.example.next.viewmodel.CartViewModel
import com.example.next.viewmodel.CheckoutViewModel
import com.example.next.viewmodel.HomeViewModel
import com.example.next.viewmodel.OrdersViewModel
import com.example.next.viewmodel.ProductDetailViewModel
import com.example.next.viewmodel.ProfileViewModel
import com.example.next.viewmodel.WishlistViewModel
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Manual dependency-injection container (the "AppContainer" pattern — no Hilt
 * needed for a project this size). Owns the long-lived singletons (Room DB,
 * repositories) and builds ViewModel factories wired to them.
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    // ---------- Data layer ----------

    val database: AppDatabase = AppDatabase.build(appContext)

    // ---------- Account system (Firebase) ----------
    // Guarded init: without a valid google-services.json the app must still
    // start, with the auth feature degrading to AuthState.Error instead of
    // crashing at launch.
    val authRepository = AuthRepository(appContext)

    // App-wide background scope for session/sync work that must outlive
    // individual ViewModels (backfill, pending-op flush, account-switch purge).
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Identity: active owner (uid:<uid> / guest:<uuid>) derived reactively
    // from the Firebase auth state. Guest id is preloaded so the owner flow
    // can map synchronously from the very first emission.
    val sessionManager = SessionManager(appContext, authRepository)

    private val firestore: FirebaseFirestore? =
        runCatching { FirebaseFirestore.getInstance() }.getOrNull()

    val userRepository = UserRepository(firestore)

    // Firestore write-through + offline outbox for authenticated owners.
    val syncEngine: SyncEngine = SyncEngine(firestore, database.pendingOpsDao())

    val productRepository = ProductRepository(database.productDao())

    val cartRepository = CartRepository(database.cartDao(), sessionManager, syncEngine)

    val wishlistRepository = WishlistRepository(database.wishlistDao(), sessionManager, syncEngine)

    val orderRepository = OrderRepository(
        database,
        database.orderDao(),
        database.cartDao(),
        sessionManager,
        syncEngine
    )

    val settingsRepository = SettingsRepository(appContext, sessionManager, syncEngine)

    init {
        appScope.launch { sessionManager.guestId() }
        // One-time (idempotent) re-owning of pre-v5 rows to this install's guest.
        appScope.launch { OwnershipBackfill(database, sessionManager).run() }
        // No-leak guarantee: purge the previous owner's cache on logout/switch,
        // after a best-effort outbox flush.
        val guestMerger = GuestMerger(
            firestore = firestore,
            sessionManager = sessionManager,
            wishlistDao = database.wishlistDao(),
            cartDao = database.cartDao(),
            orderDao = database.orderDao(),
            syncEngine = syncEngine
        )
        AccountSwitchHandler(
            sessionManager = sessionManager,
            database = database,
            scope = appScope,
            flushHook = { owner -> syncEngine.flushPendingOps(owner) },
            onGuestToUser = { uid -> guestMerger.mergeIfNeeded(uid) }
        ).start()
        // Cloud -> local sync while an authenticated owner is active.
        SyncCoordinator(
            firestore = firestore,
            sessionManager = sessionManager,
            wishlistDao = database.wishlistDao(),
            cartDao = database.cartDao(),
            orderDao = database.orderDao(),
            pendingOpsDao = database.pendingOpsDao(),
            settingsRepository = settingsRepository,
            syncEngine = syncEngine,
            scope = appScope
        ).start()
    }

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

    val profileViewModelFactory = viewModelFactory {
        initializer {
            ProfileViewModel(
                authRepository = authRepository,
                userRepository = userRepository
            )
        }
    }
}

/** Access the container from any [CreationExtras] (e.g. inside a custom factory). */
fun CreationExtras.appContainer(): AppContainer {
    val app = this[APPLICATION_KEY] as? NextApplication
        ?: error("Expected NextApplication in CreationExtras, got: ${this[APPLICATION_KEY]}")
    return app.container
}