package com.example.next.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.next.models.CartItem
import com.example.next.models.Product
import com.example.next.models.WishlistItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DatabaseHelper private constructor(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "next_store.db"
        private const val DATABASE_VERSION = 2

        // Table names
        private const val TABLE_PRODUCTS = "products"
        private const val TABLE_CART = "cart"
        private const val TABLE_WISHLIST = "wishlist"

        // Products columns
        private const val COL_PRODUCT_ID = "id"
        private const val COL_PRODUCT_NAME = "name"
        private const val COL_PRODUCT_DESC = "description"
        private const val COL_PRODUCT_CATEGORY = "category"
        private const val COL_PRODUCT_PRICE = "price"
        private const val COL_PRODUCT_IMAGE = "image_url"
        private const val COL_PRODUCT_SPECS = "specifications"
        private const val COL_PRODUCT_FEATURED = "is_featured"
        private const val COL_PRODUCT_POPULAR = "is_popular"
        private const val COL_PRODUCT_RATING = "rating"

        // Cart columns
        private const val COL_CART_ID = "id"
        private const val COL_CART_PRODUCT_ID = "product_id"
        private const val COL_CART_NAME = "product_name"
        private const val COL_CART_PRICE = "price"
        private const val COL_CART_IMAGE = "image_url"
        private const val COL_CART_QUANTITY = "quantity"

        // Wishlist columns
        private const val COL_WISHLIST_ID = "id"
        private const val COL_WISHLIST_PRODUCT_ID = "product_id"
        private const val COL_WISHLIST_NAME = "product_name"
        private const val COL_WISHLIST_PRICE = "price"
        private const val COL_WISHLIST_IMAGE = "image_url"

        @Volatile
        private var instance: DatabaseHelper? = null

        fun getInstance(context: Context): DatabaseHelper {
            return instance ?: synchronized(this) {
                instance ?: DatabaseHelper(context).also { instance = it }
            }
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        createTables(db)
        preloadProducts(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_CART")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_WISHLIST")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_PRODUCTS")
        onCreate(db)
    }

    private fun createTables(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE $TABLE_PRODUCTS (
                $COL_PRODUCT_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_PRODUCT_NAME TEXT NOT NULL,
                $COL_PRODUCT_DESC TEXT,
                $COL_PRODUCT_CATEGORY TEXT NOT NULL,
                $COL_PRODUCT_PRICE REAL NOT NULL,
                $COL_PRODUCT_IMAGE TEXT,
                $COL_PRODUCT_SPECS TEXT,
                $COL_PRODUCT_FEATURED INTEGER DEFAULT 0,
                $COL_PRODUCT_POPULAR INTEGER DEFAULT 0,
                $COL_PRODUCT_RATING REAL DEFAULT 0.0
            )"""
        )

        db.execSQL(
            """CREATE TABLE $TABLE_CART (
                $COL_CART_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_CART_PRODUCT_ID INTEGER NOT NULL,
                $COL_CART_NAME TEXT NOT NULL,
                $COL_CART_PRICE REAL NOT NULL,
                $COL_CART_IMAGE TEXT,
                $COL_CART_QUANTITY INTEGER NOT NULL DEFAULT 1
            )"""
        )

        db.execSQL(
            """CREATE TABLE $TABLE_WISHLIST (
                $COL_WISHLIST_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_WISHLIST_PRODUCT_ID INTEGER NOT NULL,
                $COL_WISHLIST_NAME TEXT NOT NULL,
                $COL_WISHLIST_PRICE REAL NOT NULL,
                $COL_WISHLIST_IMAGE TEXT
            )"""
        )
    }

    private fun preloadProducts(db: SQLiteDatabase) {
        val products = listOf(
            // Phones
            arrayOf(1, "iPhone 15 Pro Max", "Apple's most powerful iPhone with A17 Pro chip, titanium design, and advanced camera system.", "Phones", 1199.99, "iphone15", "Display: 6.7\" OLED\nChip: A17 Pro\nStorage: 256GB\nCamera: 48MP\nBattery: 4422mAh", 1, 1, 4.8),
            arrayOf(2, "Samsung Galaxy S24 Ultra", "Samsung flagship with Galaxy AI, S Pen, and titanium frame.", "Phones", 1299.99, "s24ultra", "Display: 6.8\" Dynamic AMOLED\nChip: Snapdragon 8 Gen 3\nStorage: 256GB\nCamera: 200MP\nBattery: 5000mAh", 1, 1, 4.7),
            arrayOf(3, "Google Pixel 9 Pro", "Google's best camera phone with Tensor G4 and AI photography features.", "Phones", 999.99, "pixel9", "Display: 6.3\" OLED\nChip: Tensor G4\nStorage: 128GB\nCamera: 50MP\nBattery: 4700mAh", 0, 1, 4.5),
            arrayOf(4, "OnePlus 12", "Flagship killer with Hasselblad cameras and blazing fast charging.", "Phones", 799.99, "oneplus", "Display: 6.82\" AMOLED\nChip: Snapdragon 8 Gen 3\nStorage: 256GB\nCamera: 50MP\nBattery: 5400mAh", 0, 1, 4.4),
            arrayOf(5, "Nothing Phone 2", "Unique transparent design with Glyph Interface LED lights.", "Phones", 599.99, "nothing2", "Display: 6.7\" OLED\nChip: Snapdragon 8+ Gen 1\nStorage: 128GB\nCamera: 50MP\nBattery: 4700mAh", 0, 0, 4.2),
            // Laptops
            arrayOf(6, "MacBook Pro 16\" M3 Max", "Apple's most powerful laptop with M3 Max chip and Liquid Retina XDR display.", "Laptops", 3499.99, "mac16", "Display: 16.2\" Liquid Retina XDR\nChip: M3 Max\nRAM: 36GB\nStorage: 1TB SSD\nBattery: 22 hours", 1, 1, 4.9),
            arrayOf(7, "Dell XPS 16", "Premium Windows laptop with stunning OLED display and Intel Core Ultra.", "Laptops", 2199.99, "dell", "Display: 16.3\" OLED\nChip: Intel Core Ultra 9\nRAM: 32GB\nStorage: 1TB SSD\nBattery: 14 hours", 1, 1, 4.6),
            arrayOf(8, "Lenovo ThinkPad X1 Carbon Gen 12", "Business laptop redefined with Intel Core Ultra and 14\" 2.8K OLED.", "Laptops", 1849.99, "lenovo", "Display: 14\" 2.8K OLED\nChip: Intel Core Ultra 7\nRAM: 16GB\nStorage: 512GB SSD\nBattery: 15 hours", 0, 1, 4.5),
            arrayOf(9, "ASUS ROG Zephyrus G16", "Gaming laptop with RTX 4070 and 240Hz display.", "Laptops", 1999.99, "rog", "Display: 16\" QHD 240Hz\nChip: Intel Core i9\nGPU: RTX 4070\nRAM: 32GB\nStorage: 1TB SSD", 0, 0, 4.3),
            arrayOf(10, "MacBook Air 15\" M3", "Thin and light laptop with M3 chip for everyday performance.", "Laptops", 1299.99, "macair", "Display: 15.3\" Liquid Retina\nChip: M3\nRAM: 8GB\nStorage: 256GB SSD\nBattery: 18 hours", 0, 1, 4.7),
            // Cameras
            arrayOf(11, "Sony Alpha A7 IV", "Full-frame mirrorless camera with 33MP sensor and 4K video.", "Cameras", 2499.99, "sony", "Sensor: 33MP Full-Frame\nVideo: 4K 60fps\nISO: 100-51200\nAF: 759 points\nStabilization: 5-axis IBIS", 1, 1, 4.8),
            arrayOf(12, "Canon EOS R6 Mark II", "Versatile full-frame mirrorless with 40fps burst shooting.", "Cameras", 2499.99, "canon", "Sensor: 24.2MP Full-Frame\nVideo: 4K 60fps\nISO: 100-102400\nAF: Dual Pixel CMOS AF II\nStabilization: 5-axis IBIS", 0, 1, 4.6),
            arrayOf(13, "Fujifilm X-T5", "Retro-styled APS-C camera with 40MP sensor and film simulations.", "Cameras", 1699.99, "fuji", "Sensor: 40MP APS-C\nVideo: 6.2K\nISO: 125-12800\nFilm Simulations: 19 modes\nStabilization: 5-axis IBIS", 0, 0, 4.4),
            arrayOf(14, "DJI Osmo Pocket 3", "Compact gimbal camera with 1-inch sensor and 4K 120fps.", "Cameras", 519.99, "dji", "Sensor: 1-inch CMOS\nVideo: 4K 120fps\nGimbal: 3-axis\nScreen: 2\" Rotatable\nMic: 3-mic array", 0, 0, 4.3),
            arrayOf(15, "GoPro Hero 12 Black", "Rugged action camera with 5.3K video and HyperSmooth 6.0.", "Cameras", 399.99, "go", "Video: 5.3K 60fps\nPhoto: 27MP\nStabilization: HyperSmooth 6.0\nWaterproof: 33ft\nBattery: 1720mAh", 0, 0, 4.2),
            // Accessories
            arrayOf(16, "Apple AirPods Pro 2", "Premium wireless earbuds with active noise cancellation.", "Accessories", 249.99, "airpod", "Type: True Wireless\nANC: Yes\nBattery: 6 + 30 hours\nChip: H2\nWater Resistance: IPX4", 1, 1, 4.7),
            arrayOf(17, "Samsung Galaxy Watch 6 Classic", "Premium smartwatch with rotating bezel and health tracking.", "Accessories", 399.99, "gwatch", "Display: 1.47\" Super AMOLED\nOS: Wear OS\nSensors: BioActive, Temp\nBattery: 425mAh\nWater: IP68", 0, 1, 4.3),
            arrayOf(18, "Anker 737 Power Bank", "High-capacity 24,000mAh power bank with 140W output.", "Accessories", 109.99, "anker", "Capacity: 24000mAh\nOutput: 140W USB-C\nPorts: 2x USB-C, 1x USB-A\nDisplay: Smart Digital\nCharge: 65W input", 0, 1, 4.6),
            arrayOf(19, "Logitech MX Master 3S", "Premium wireless mouse with 8K DPI and quiet clicks.", "Accessories", 99.99, "logi", "DPI: 8000\nConnectivity: Bluetooth, USB-C\nButtons: 7 programmable\nBattery: 70 days\nScroll: MagSpeed", 0, 0, 4.5),
            arrayOf(20, "Sony WH-1000XM5", "Industry-leading noise canceling headphones.", "Accessories", 349.99, "sonyh", "Type: Over-Ear\nANC: Yes\nBattery: 30 hours\nCodec: LDAC, AAC\nWeight: 250g", 0, 1, 4.8),
            arrayOf(21, "Apple iPad Air M2", "Versatile tablet with M2 chip for productivity and creativity.", "Accessories", 599.99, "ipad", "Display: 11\" Liquid Retina\nChip: M2\nStorage: 128GB\nCamera: 12MP\nAccessories: Apple Pencil", 1, 0, 4.6),
            arrayOf(22, "Samsung 990 Pro 2TB SSD", "Blazing fast PCIe 4.0 NVMe SSD for gaming and content creation.", "Accessories", 209.99, "sam", "Capacity: 2TB\nInterface: PCIe 4.0 NVMe\nRead: 7450 MB/s\nWrite: 6900 MB/s\nForm Factor: M.2 2280", 0, 0, 4.7),
            arrayOf(23, "Keychron Q1 Pro Mechanical Keyboard", "Premium wireless mechanical keyboard with aluminum body.", "Accessories", 199.99, "key", "Layout: 75%\nSwitches: Gateron Red\nConnectivity: Bluetooth, USB-C\nBody: Aluminum\nRGB: Per-key", 0, 0, 4.4),
            arrayOf(24, "Razer DeathAdder V3 Pro", "Ultra-lightweight wireless gaming mouse at 63g.", "Accessories", 149.99, "razer", "DPI: 30000\nWeight: 63g\nConnectivity: HyperSpeed\nBattery: 90 hours\nSensor: Focus Pro 30K", 0, 0, 4.5),
        )

        for (p in products) {
            val values = ContentValues().apply {
                put(COL_PRODUCT_ID, p[0] as Int)
                put(COL_PRODUCT_NAME, p[1] as String)
                put(COL_PRODUCT_DESC, p[2] as String)
                put(COL_PRODUCT_CATEGORY, p[3] as String)
                put(COL_PRODUCT_PRICE, p[4] as Double)
                put(COL_PRODUCT_IMAGE, p[5] as String)
                put(COL_PRODUCT_SPECS, p[6] as String)
                put(COL_PRODUCT_FEATURED, p[7] as Int)
                put(COL_PRODUCT_POPULAR, p[8] as Int)
                put(COL_PRODUCT_RATING, p[9] as Double)
            }
            db.insert(TABLE_PRODUCTS, null, values)
        }
    }

    // ==================== PRODUCT OPERATIONS ====================

    suspend fun getAllProducts(): List<Product> = withContext(Dispatchers.IO) {
        val products = mutableListOf<Product>()
        val cursor = readableDatabase.query(TABLE_PRODUCTS, null, null, null, null, null, "$COL_PRODUCT_NAME ASC")
        cursor.use {
            if (it.moveToFirst()) {
                do {
                    products.add(cursorToProduct(it))
                } while (it.moveToNext())
            }
        }
        products
    }

    suspend fun getProductById(id: Int): Product? = withContext(Dispatchers.IO) {
        val cursor = readableDatabase.query(
            TABLE_PRODUCTS, null,
            "$COL_PRODUCT_ID = ?", arrayOf(id.toString()),
            null, null, null
        )
        cursor.use {
            if (it.moveToFirst()) cursorToProduct(it) else null
        }
    }

    suspend fun getProductsByCategory(category: String): List<Product> = withContext(Dispatchers.IO) {
        val products = mutableListOf<Product>()
        val cursor = readableDatabase.query(
            TABLE_PRODUCTS, null,
            "$COL_PRODUCT_CATEGORY = ?", arrayOf(category),
            null, null, "$COL_PRODUCT_NAME ASC"
        )
        cursor.use {
            if (it.moveToFirst()) {
                do {
                    products.add(cursorToProduct(it))
                } while (it.moveToNext())
            }
        }
        products
    }

    suspend fun getFeaturedProducts(): List<Product> = withContext(Dispatchers.IO) {
        val products = mutableListOf<Product>()
        val cursor = readableDatabase.query(
            TABLE_PRODUCTS, null,
            "$COL_PRODUCT_FEATURED = 1", null,
            null, null, null
        )
        cursor.use {
            if (it.moveToFirst()) {
                do {
                    products.add(cursorToProduct(it))
                } while (it.moveToNext())
            }
        }
        products
    }

    suspend fun getPopularProducts(): List<Product> = withContext(Dispatchers.IO) {
        val products = mutableListOf<Product>()
        val cursor = readableDatabase.query(
            TABLE_PRODUCTS, null,
            "$COL_PRODUCT_POPULAR = 1", null,
            null, null, null
        )
        cursor.use {
            if (it.moveToFirst()) {
                do {
                    products.add(cursorToProduct(it))
                } while (it.moveToNext())
            }
        }
        products
    }

    suspend fun searchProducts(query: String): List<Product> = withContext(Dispatchers.IO) {
        val products = mutableListOf<Product>()
        val likeClause = "%$query%"
        val cursor = readableDatabase.query(
            TABLE_PRODUCTS, null,
            "$COL_PRODUCT_NAME LIKE ? OR $COL_PRODUCT_DESC LIKE ? OR $COL_PRODUCT_CATEGORY LIKE ? OR $COL_PRODUCT_SPECS LIKE ?",
            arrayOf(likeClause, likeClause, likeClause, likeClause),
            null, null, "$COL_PRODUCT_NAME ASC"
        )
        cursor.use {
            if (it.moveToFirst()) {
                do {
                    products.add(cursorToProduct(it))
                } while (it.moveToNext())
            }
        }
        products
    }

    private fun cursorToProduct(cursor: android.database.Cursor): Product = Product(
        id = cursor.getInt(cursor.getColumnIndexOrThrow(COL_PRODUCT_ID)),
        name = cursor.getString(cursor.getColumnIndexOrThrow(COL_PRODUCT_NAME)) ?: "",
        description = cursor.getString(cursor.getColumnIndexOrThrow(COL_PRODUCT_DESC)) ?: "",
        category = cursor.getString(cursor.getColumnIndexOrThrow(COL_PRODUCT_CATEGORY)) ?: "",
        price = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_PRODUCT_PRICE)),
        imageUrl = cursor.getString(cursor.getColumnIndexOrThrow(COL_PRODUCT_IMAGE)) ?: "",
        specifications = cursor.getString(cursor.getColumnIndexOrThrow(COL_PRODUCT_SPECS)) ?: "",
        isFeatured = cursor.getInt(cursor.getColumnIndexOrThrow(COL_PRODUCT_FEATURED)) == 1,
        isPopular = cursor.getInt(cursor.getColumnIndexOrThrow(COL_PRODUCT_POPULAR)) == 1,
        rating = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_PRODUCT_RATING)).toFloat()
    )

    // ==================== CART OPERATIONS ====================

    suspend fun addToCart(product: Product) = withContext(Dispatchers.IO) {
        val db = writableDatabase
        val cursor = db.query(
            TABLE_CART, null,
            "$COL_CART_PRODUCT_ID = ?", arrayOf(product.id.toString()),
            null, null, null
        )
        cursor.use {
            if (it.moveToFirst()) {
                val cartId = it.getInt(it.getColumnIndexOrThrow(COL_CART_ID))
                val quantity = it.getInt(it.getColumnIndexOrThrow(COL_CART_QUANTITY))
                val values = ContentValues().apply {
                    put(COL_CART_QUANTITY, quantity + 1)
                }
                db.update(TABLE_CART, values, "$COL_CART_ID = ?", arrayOf(cartId.toString()))
            } else {
                val values = ContentValues().apply {
                    put(COL_CART_PRODUCT_ID, product.id)
                    put(COL_CART_NAME, product.name)
                    put(COL_CART_PRICE, product.price)
                    put(COL_CART_IMAGE, product.imageUrl)
                    put(COL_CART_QUANTITY, 1)
                }
                db.insert(TABLE_CART, null, values)
            }
        }
    }

    suspend fun getAllCartItems(): List<CartItem> = withContext(Dispatchers.IO) {
        val items = mutableListOf<CartItem>()
        val cursor = readableDatabase.query(TABLE_CART, null, null, null, null, null, "$COL_CART_ID DESC")
        cursor.use {
            if (it.moveToFirst()) {
                do {
                    items.add(
                        CartItem(
                            id = it.getInt(it.getColumnIndexOrThrow(COL_CART_ID)),
                            productId = it.getInt(it.getColumnIndexOrThrow(COL_CART_PRODUCT_ID)),
                            productName = it.getString(it.getColumnIndexOrThrow(COL_CART_NAME)) ?: "",
                            price = it.getDouble(it.getColumnIndexOrThrow(COL_CART_PRICE)),
                            imageUrl = it.getString(it.getColumnIndexOrThrow(COL_CART_IMAGE)) ?: "",
                            quantity = it.getInt(it.getColumnIndexOrThrow(COL_CART_QUANTITY))
                        )
                    )
                } while (it.moveToNext())
            }
        }
        items
    }

    suspend fun updateCartQuantity(cartId: Int, quantity: Int) = withContext(Dispatchers.IO) {
        val db = writableDatabase
        if (quantity <= 0) {
            removeFromCart(cartId)
        } else {
            val values = ContentValues().apply {
                put(COL_CART_QUANTITY, quantity)
            }
            db.update(TABLE_CART, values, "$COL_CART_ID = ?", arrayOf(cartId.toString()))
        }
    }

    suspend fun removeFromCart(cartId: Int) = withContext(Dispatchers.IO) {
        writableDatabase.delete(TABLE_CART, "$COL_CART_ID = ?", arrayOf(cartId.toString()))
    }

    suspend fun clearCart() = withContext(Dispatchers.IO) {
        writableDatabase.delete(TABLE_CART, null, null)
    }

    suspend fun getCartTotal(): Double = withContext(Dispatchers.IO) {
        var total = 0.0
        val cursor = readableDatabase.rawQuery(
            "SELECT SUM($COL_CART_PRICE * $COL_CART_QUANTITY) FROM $TABLE_CART", null
        )
        cursor.use {
            if (it.moveToFirst()) total = it.getDouble(0)
        }
        total
    }

    suspend fun getCartItemCount(): Int = withContext(Dispatchers.IO) {
        var count = 0
        val cursor = readableDatabase.rawQuery(
            "SELECT SUM($COL_CART_QUANTITY) FROM $TABLE_CART", null
        )
        cursor.use {
            if (it.moveToFirst()) count = it.getInt(0)
        }
        count
    }

    // ==================== WISHLIST OPERATIONS ====================

    suspend fun addToWishlist(product: Product) = withContext(Dispatchers.IO) {
        val db = writableDatabase
        val cursor = db.query(
            TABLE_WISHLIST, null,
            "$COL_WISHLIST_PRODUCT_ID = ?", arrayOf(product.id.toString()),
            null, null, null
        )
        cursor.use {
            if (!it.moveToFirst()) {
                val values = ContentValues().apply {
                    put(COL_WISHLIST_PRODUCT_ID, product.id)
                    put(COL_WISHLIST_NAME, product.name)
                    put(COL_WISHLIST_PRICE, product.price)
                    put(COL_WISHLIST_IMAGE, product.imageUrl)
                }
                db.insert(TABLE_WISHLIST, null, values)
            }
        }
    }

    suspend fun removeFromWishlist(productId: Int) = withContext(Dispatchers.IO) {
        writableDatabase.delete(TABLE_WISHLIST, "$COL_WISHLIST_PRODUCT_ID = ?", arrayOf(productId.toString()))
    }

    suspend fun getAllWishlistItems(): List<WishlistItem> = withContext(Dispatchers.IO) {
        val items = mutableListOf<WishlistItem>()
        val cursor = readableDatabase.query(TABLE_WISHLIST, null, null, null, null, null, "$COL_WISHLIST_ID DESC")
        cursor.use {
            if (it.moveToFirst()) {
                do {
                    items.add(
                        WishlistItem(
                            id = it.getInt(it.getColumnIndexOrThrow(COL_WISHLIST_ID)),
                            productId = it.getInt(it.getColumnIndexOrThrow(COL_WISHLIST_PRODUCT_ID)),
                            productName = it.getString(it.getColumnIndexOrThrow(COL_WISHLIST_NAME)) ?: "",
                            price = it.getDouble(it.getColumnIndexOrThrow(COL_WISHLIST_PRICE)),
                            imageUrl = it.getString(it.getColumnIndexOrThrow(COL_WISHLIST_IMAGE)) ?: ""
                        )
                    )
                } while (it.moveToNext())
            }
        }
        items
    }

    suspend fun isInWishlist(productId: Int): Boolean = withContext(Dispatchers.IO) {
        val cursor = readableDatabase.query(
            TABLE_WISHLIST, null,
            "$COL_WISHLIST_PRODUCT_ID = ?", arrayOf(productId.toString()),
            null, null, null
        )
        cursor.use { it.moveToFirst() }
    }

    suspend fun getWishlistCount(): Int = withContext(Dispatchers.IO) {
        var count = 0
        val cursor = readableDatabase.rawQuery("SELECT COUNT(*) FROM $TABLE_WISHLIST", null)
        cursor.use {
            if (it.moveToFirst()) count = it.getInt(0)
        }
        count
    }
}
