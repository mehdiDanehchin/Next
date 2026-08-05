package com.example.next

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.next.database.DatabaseHelper
import com.example.next.ui.navigation.AppNavigation
import com.example.next.ui.theme.NextTheme
import com.example.next.ui.theme.ThemeMode
import com.example.next.ui.theme.ThemePreferences
import com.example.next.viewmodel.StoreViewModel

class MainActivity : ComponentActivity() {

    private val dbHelper by lazy { DatabaseHelper.getInstance(this) }
    private val storeViewModel: StoreViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val savedMode = ThemePreferences.load(this)

        setContent {
            var themeMode by remember { mutableStateOf(savedMode) }

            NextTheme(themeMode = themeMode) {
                AppNavigation(
                    dbHelper = dbHelper,
                    storeViewModel = storeViewModel,
                    themeMode = themeMode,
                    onThemeChanged = { newMode ->
                        themeMode = newMode
                        ThemePreferences.save(this@MainActivity, newMode)
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Re-read cart/wishlist from the DB whenever this activity comes back to the
        // foreground (e.g. after ProductDetailActivity added/removed items), so badges
        // and screens never show stale data.
        storeViewModel.refresh()
    }
}
