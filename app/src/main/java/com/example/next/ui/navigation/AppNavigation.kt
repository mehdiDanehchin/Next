package com.example.next.ui.navigation

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
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.next.R
import com.example.next.database.DatabaseHelper
import com.example.next.ui.screens.CartScreen
import com.example.next.ui.screens.HomeScreen
import com.example.next.ui.screens.ProfileScreen
import com.example.next.ui.screens.WishlistScreen
import com.example.next.ui.theme.*
import com.example.next.viewmodel.StoreViewModel

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

@Composable
fun AppNavigation(
    dbHelper: DatabaseHelper,
    storeViewModel: StoreViewModel,
    themeMode: ThemeMode,
    onThemeChanged: (ThemeMode) -> Unit
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val customColors = LocalCustomColors.current

    val cartCount by storeViewModel.cartCount.collectAsStateWithLifecycle()
    val wishlistCount by storeViewModel.wishlistCount.collectAsStateWithLifecycle()

    fun badgeCountFor(item: BottomNavItem): Int = when (item) {
        BottomNavItem.Cart -> cartCount
        BottomNavItem.Wishlist -> wishlistCount
        else -> 0
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
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
                                        Badge {
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
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = BottomNavItem.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(BottomNavItem.Home.route) {
                HomeScreen(dbHelper = dbHelper, storeViewModel = storeViewModel)
            }
            composable(BottomNavItem.Wishlist.route) {
                WishlistScreen(storeViewModel = storeViewModel)
            }
            composable(BottomNavItem.Cart.route) {
                CartScreen(storeViewModel = storeViewModel)
            }
            composable(BottomNavItem.Profile.route) {
                ProfileScreen(
                    themeMode = themeMode,
                    onThemeChanged = onThemeChanged
                )
            }
        }
    }
}
