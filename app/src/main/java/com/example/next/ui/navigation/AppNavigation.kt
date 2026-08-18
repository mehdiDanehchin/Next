package com.example.next.ui.navigation

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.next.R
import com.example.next.di.AppContainer
import com.example.next.models.ThemeMode
import com.example.next.ui.screens.CartScreen
import com.example.next.ui.screens.CheckoutScreen
import com.example.next.ui.screens.HomeScreen
import com.example.next.ui.screens.OrdersScreen
import com.example.next.ui.screens.ProductDetailScreen
import com.example.next.ui.screens.ProfileScreen
import com.example.next.ui.screens.WishlistScreen
import com.example.next.ui.theme.*
import com.example.next.viewmodel.CartViewModel
import com.example.next.viewmodel.ProfileViewModel
import com.example.next.viewmodel.WishlistViewModel

sealed class BottomNavItem(
    val route: String,
    val titleResId: Int,
    val iconResId: Int
) {
    data object Home : BottomNavItem("home", R.string.nav_home, R.drawable.ic_home)
    data object Wishlist : BottomNavItem("wishlist", R.string.nav_wishlist, R.drawable.ic_heart)
    data object Cart : BottomNavItem("cart", R.string.nav_cart, R.drawable.ic_cart)
    data object Profile : BottomNavItem("profile", R.string.nav_profile, R.drawable.ic_profile)
}

val bottomNavItems = listOf(
    BottomNavItem.Home,
    BottomNavItem.Wishlist,
    BottomNavItem.Cart,
    BottomNavItem.Profile
)

const val PRODUCT_DETAIL_ROUTE = "product_detail/{productId}?fromFeatured={fromFeatured}"

fun productDetailRoute(productId: Int, fromFeatured: Boolean = false) =
    "product_detail/$productId?fromFeatured=$fromFeatured"

const val CHECKOUT_ROUTE = "checkout"

const val ORDERS_ROUTE = "orders"

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun AppNavigation(
    container: AppContainer,
    cartViewModel: CartViewModel,
    wishlistViewModel: WishlistViewModel,
    themeMode: ThemeMode,
    onThemeChanged: (ThemeMode) -> Unit
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val customColors = LocalCustomColors.current

    val cartUiState by cartViewModel.uiState.collectAsStateWithLifecycle()
    val wishlistUiState by wishlistViewModel.uiState.collectAsStateWithLifecycle()

    fun badgeCountFor(item: BottomNavItem): Int = when (item) {
        BottomNavItem.Cart -> cartUiState.count
        BottomNavItem.Wishlist -> wishlistUiState.count
        else -> 0
    }

    // Full-screen destinations: product detail, checkout and orders hide the bottom bar.
    val isFullScreenRoute = currentRoute == PRODUCT_DETAIL_ROUTE ||
        currentRoute == CHECKOUT_ROUTE ||
        currentRoute == ORDERS_ROUTE

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (!isFullScreenRoute) {
                NavigationBar(
                    containerColor = customColors.surfaceWhite,
                    tonalElevation = 0.dp,
                    contentColor = customColors.surfaceWhite,
                    modifier = Modifier
                        .shadow(8.dp)
                ) {
                    bottomNavItems.forEach { item ->
                        val isSelected = currentRoute == item.route
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                val badgeCount = badgeCountFor(item)
                                if (badgeCount > 0) {
                                    BadgedBox(
                                        badge = {
                                            Badge(modifier = Modifier.animateContentSize()) {
                                                Text(
                                                    text = if (badgeCount > 99) "99+" else badgeCount.toString(),
                                                    fontSize = 10.sp
                                                )
                                            }
                                        }
                                    ) {
                                        Icon(
                                            painter = painterResource(item.iconResId),
                                            contentDescription = null,
                                            tint = if (isSelected) Primary else customColors.iconInactive
                                        )
                                    }
                                } else {
                                    Icon(
                                        painter = painterResource(item.iconResId),
                                        contentDescription = null,
                                        tint = if (isSelected) Primary else customColors.iconInactive
                                    )
                                }
                            },
                            label = {
                                Text(
                                    text = navController.context.getString(item.titleResId),
                                    color = if (isSelected) Primary else customColors.iconInactive
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = Color.Transparent
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        // SharedTransitionLayout enables the shared-element (morphing) transition
        // between the home grid thumbnails and the product-detail image.
        SharedTransitionLayout {
            val sharedTransitionScope = this
            NavHost(
                navController = navController,
                startDestination = BottomNavItem.Home.route,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(BottomNavItem.Home.route) {
                    HomeScreen(
                        container = container,
                        onProductClick = { product, fromFeatured ->
                            navController.navigate(productDetailRoute(product.id, fromFeatured))
                        },
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = this
                    )
                }
            composable(BottomNavItem.Wishlist.route) {
                WishlistScreen(
                    wishlistViewModel = wishlistViewModel,
                    onProductClick = { productId -> navController.navigate(productDetailRoute(productId)) }
                )
            }
            composable(BottomNavItem.Cart.route) {
                CartScreen(
                    cartViewModel = cartViewModel,
                    onCheckout = { navController.navigate(CHECKOUT_ROUTE) }
                )
            }
            composable(BottomNavItem.Profile.route) {
                val profileViewModel: ProfileViewModel =
                    viewModel(factory = container.profileViewModelFactory)
                ProfileScreen(
                    profileViewModel = profileViewModel,
                    themeMode = themeMode,
                    onThemeChanged = onThemeChanged,
                    onMyOrders = { navController.navigate(ORDERS_ROUTE) }
                )
            }
            composable(CHECKOUT_ROUTE) {
                CheckoutScreen(
                    container = container,
                    cartViewModel = cartViewModel,
                    onBack = { navController.popBackStack() },
                    onOrderPlaced = {
                        navController.navigate(ORDERS_ROUTE) {
                            popUpTo(CHECKOUT_ROUTE) { inclusive = true }
                        }
                    }
                )
            }
            composable(ORDERS_ROUTE) {
                OrdersScreen(
                    container = container,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                route = PRODUCT_DETAIL_ROUTE,
                arguments = listOf(
                    navArgument("productId") { type = NavType.IntType },
                    navArgument("fromFeatured") {
                        type = NavType.BoolType
                        defaultValue = false
                    }
                )
            ) {
                ProductDetailScreen(
                    container = container,
                    onBack = { navController.popBackStack() },
                    fromFeatured = it.arguments?.getBoolean("fromFeatured") ?: false,
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = this
                )
            }
            }
            }
    }
}