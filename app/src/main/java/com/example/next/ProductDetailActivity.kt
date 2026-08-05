package com.example.next

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.next.database.DatabaseHelper
import com.example.next.ui.screens.ProductDetailScreen
import com.example.next.ui.theme.NextTheme
import com.example.next.ui.theme.ThemePreferences

class ProductDetailActivity : ComponentActivity() {

    private val dbHelper by lazy { DatabaseHelper.getInstance(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val productId = intent.getIntExtra("product_id", -1)
        if (productId == -1) {
            finish()
            return
        }

        val themeMode = ThemePreferences.load(this)

        setContent {
            NextTheme(themeMode = themeMode) {
                ProductDetailScreen(
                    productId = productId,
                    dbHelper = dbHelper,
                    onBack = { finish() }
                )
            }
        }
    }
}
