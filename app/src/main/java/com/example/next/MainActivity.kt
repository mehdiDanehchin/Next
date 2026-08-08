package com.example.next

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.next.di.AppContainer
import com.example.next.ui.navigation.AppNavigation
import com.example.next.ui.theme.NextTheme
import com.example.next.models.ThemeMode
import com.example.next.viewmodel.CartViewModel
import com.example.next.viewmodel.WishlistViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val container: AppContainer by lazy { (application as NextApplication).container }

    // Activity-scoped: shared by the bottom-nav badges and the Cart/Wishlist screens.
    private val cartViewModel: CartViewModel by viewModels { container.cartViewModelFactory }
    private val wishlistViewModel: WishlistViewModel by viewModels { container.wishlistViewModelFactory }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Show the pure-black splash theme until the first frame is ready.
        // Must run before super.onCreate() per the AndroidX SplashScreen API.
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            // Theme is now reactive: DataStore emits the persisted mode and the
            // whole tree recomposes. No manual save/load in the Activity anymore.
            val themeMode by container.settingsRepository.themeMode
                .collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)
            val scope = rememberCoroutineScope()

            NextTheme(themeMode = themeMode) {
                AppNavigation(
                    container = container,
                    cartViewModel = cartViewModel,
                    wishlistViewModel = wishlistViewModel,
                    themeMode = themeMode,
                    onThemeChanged = { newMode ->
                        scope.launch { container.settingsRepository.setThemeMode(newMode) }
                    }
                )
            }
        }
    }
}