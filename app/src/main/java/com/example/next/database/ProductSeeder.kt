package com.example.next.database

import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.next.database.entity.ProductEntity

/**
 * Seeds the products catalog on first database creation. The legacy
 * SQLiteOpenHelper did this inside [android.database.sqlite.SQLiteOpenHelper.onCreate];
 * with Room it is triggered from a [androidx.room.RoomDatabase.Callback] (fresh installs)
 * and from the 1 -> 3 migration (in case a v1 database had no products yet).
 */
object ProductSeeder {

    private val products: List<ProductEntity> = listOf(
        // Phones
        ProductEntity(1, "iPhone 15 Pro Max", "Apple's most powerful iPhone with A17 Pro chip, titanium design, and advanced camera system.", "Phones", 1199.99, "iphone15", "Display: 6.7\" OLED\nChip: A17 Pro\nStorage: 256GB\nCamera: 48MP\nBattery: 4422mAh", isFeatured = true, isPopular = true, rating = 4.8f),
        ProductEntity(2, "Samsung Galaxy S24 Ultra", "Samsung flagship with Galaxy AI, S Pen, and titanium frame.", "Phones", 1299.99, "s24ultra", "Display: 6.8\" Dynamic AMOLED\nChip: Snapdragon 8 Gen 3\nStorage: 256GB\nCamera: 200MP\nBattery: 5000mAh", isFeatured = true, isPopular = true, rating = 4.7f),
        ProductEntity(3, "Google Pixel 9 Pro", "Google's best camera phone with Tensor G4 and AI photography features.", "Phones", 999.99, "pixel9", "Display: 6.3\" OLED\nChip: Tensor G4\nStorage: 128GB\nCamera: 50MP\nBattery: 4700mAh", isFeatured = false, isPopular = true, rating = 4.5f),
        ProductEntity(4, "OnePlus 12", "Flagship killer with Hasselblad cameras and blazing fast charging.", "Phones", 799.99, "oneplus", "Display: 6.82\" AMOLED\nChip: Snapdragon 8 Gen 3\nStorage: 256GB\nCamera: 50MP\nBattery: 5400mAh", isFeatured = false, isPopular = true, rating = 4.4f),
        ProductEntity(5, "Nothing Phone 2", "Unique transparent design with Glyph Interface LED lights.", "Phones", 599.99, "nothing2", "Display: 6.7\" OLED\nChip: Snapdragon 8+ Gen 1\nStorage: 128GB\nCamera: 50MP\nBattery: 4700mAh", isFeatured = false, isPopular = false, rating = 4.2f),
        // Laptops
        ProductEntity(6, "MacBook Pro 16\" M3 Max", "Apple's most powerful laptop with M3 Max chip and Liquid Retina XDR display.", "Laptops", 3499.99, "mac16", "Display: 16.2\" Liquid Retina XDR\nChip: M3 Max\nRAM: 36GB\nStorage: 1TB SSD\nBattery: 22 hours", isFeatured = true, isPopular = true, rating = 4.9f),
        ProductEntity(7, "Dell XPS 16", "Premium Windows laptop with stunning OLED display and Intel Core Ultra.", "Laptops", 2199.99, "dell", "Display: 16.3\" OLED\nChip: Intel Core Ultra 9\nRAM: 32GB\nStorage: 1TB SSD\nBattery: 14 hours", isFeatured = true, isPopular = true, rating = 4.6f),
        ProductEntity(8, "Lenovo ThinkPad X1 Carbon Gen 12", "Business laptop redefined with Intel Core Ultra and 14\" 2.8K OLED.", "Laptops", 1849.99, "lenovo", "Display: 14\" 2.8K OLED\nChip: Intel Core Ultra 7\nRAM: 16GB\nStorage: 512GB SSD\nBattery: 15 hours", isFeatured = false, isPopular = true, rating = 4.5f),
        ProductEntity(9, "ASUS ROG Zephyrus G16", "Gaming laptop with RTX 4070 and 240Hz display.", "Laptops", 1999.99, "rog", "Display: 16\" QHD 240Hz\nChip: Intel Core i9\nGPU: RTX 4070\nRAM: 32GB\nStorage: 1TB SSD", isFeatured = false, isPopular = false, rating = 4.3f),
        ProductEntity(10, "MacBook Air 15\" M3", "Thin and light laptop with M3 chip for everyday performance.", "Laptops", 1299.99, "macair", "Display: 15.3\" Liquid Retina\nChip: M3\nRAM: 8GB\nStorage: 256GB SSD\nBattery: 18 hours", isFeatured = false, isPopular = true, rating = 4.7f),
        // Cameras
        ProductEntity(11, "Sony Alpha A7 IV", "Full-frame mirrorless camera with 33MP sensor and 4K video.", "Cameras", 2499.99, "sony", "Sensor: 33MP Full-Frame\nVideo: 4K 60fps\nISO: 100-51200\nAF: 759 points\nStabilization: 5-axis IBIS", isFeatured = true, isPopular = true, rating = 4.8f),
        ProductEntity(12, "Canon EOS R6 Mark II", "Versatile full-frame mirrorless with 40fps burst shooting.", "Cameras", 2499.99, "canon", "Sensor: 24.2MP Full-Frame\nVideo: 4K 60fps\nISO: 100-102400\nAF: Dual Pixel CMOS AF II\nStabilization: 5-axis IBIS", isFeatured = false, isPopular = true, rating = 4.6f),
        ProductEntity(13, "Fujifilm X-T5", "Retro-styled APS-C camera with 40MP sensor and film simulations.", "Cameras", 1699.99, "fuji", "Sensor: 40MP APS-C\nVideo: 6.2K\nISO: 125-12800\nFilm Simulations: 19 modes\nStabilization: 5-axis IBIS", isFeatured = false, isPopular = false, rating = 4.4f),
        ProductEntity(14, "DJI Osmo Pocket 3", "Compact gimbal camera with 1-inch sensor and 4K 120fps.", "Cameras", 519.99, "dji", "Sensor: 1-inch CMOS\nVideo: 4K 120fps\nGimbal: 3-axis\nScreen: 2\" Rotatable\nMic: 3-mic array", isFeatured = false, isPopular = false, rating = 4.3f),
        ProductEntity(15, "GoPro Hero 12 Black", "Action camera with 5.3K video and HyperSmooth 6.0 stabilization.", "Cameras", 399.99, "go", "Video: 5.3K 60fps\nPhoto: 27MP\nStabilization: HyperSmooth 6.0\nWaterproof: 33ft\nBattery: 1720mAh", isFeatured = false, isPopular = false, rating = 4.2f),
        // Accessories
        ProductEntity(16, "Apple AirPods Pro 2", "Premium wireless earbuds with active noise cancellation.", "Accessories", 249.99, "airpod", "Type: True Wireless\nANC: Yes\nBattery: 6 + 30 hours\nChip: H2\nWater Resistance: IPX4", isFeatured = true, isPopular = true, rating = 4.7f),
        ProductEntity(17, "Samsung Galaxy Watch 6 Classic", "Premium smartwatch with rotating bezel and health tracking.", "Accessories", 399.99, "gwatch", "Display: 1.47\" Super AMOLED\nOS: Wear OS\nSensors: BioActive, Temp\nBattery: 425mAh\nWater: IP68", isFeatured = false, isPopular = true, rating = 4.3f),
        ProductEntity(18, "Anker 737 Power Bank", "High-capacity 24,000mAh power bank with 140W output.", "Accessories", 109.99, "anker", "Capacity: 24000mAh\nOutput: 140W USB-C\nPorts: 2x USB-C, 1x USB-A\nDisplay: Smart Digital\nCharge: 65W input", isFeatured = false, isPopular = true, rating = 4.6f),
        ProductEntity(19, "Logitech MX Master 3S", "Premium wireless mouse with 8K DPI and quiet clicks.", "Accessories", 99.99, "logi", "DPI: 8000\nConnectivity: Bluetooth, USB-C\nButtons: 7 programmable\nBattery: 70 days\nScroll: MagSpeed", isFeatured = false, isPopular = false, rating = 4.5f),
        ProductEntity(20, "Sony WH-1000XM5", "Industry-leading noise canceling headphones.", "Accessories", 349.99, "sonyh", "Type: Over-Ear\nANC: Yes\nBattery: 30 hours\nCodec: LDAC, AAC\nWeight: 250g", isFeatured = false, isPopular = true, rating = 4.8f),
        ProductEntity(21, "Apple iPad Air M2", "Versatile tablet with M2 chip for productivity and creativity.", "Accessories", 599.99, "ipad", "Display: 11\" Liquid Retina\nChip: M2\nStorage: 128GB\nCamera: 12MP\nPencil Support: Yes", isFeatured = true, isPopular = false, rating = 4.6f),
        ProductEntity(22, "Samsung 990 Pro 2TB SSD", "Blazing fast PCIe 4.0 NVMe SSD for gaming and content creation.", "Accessories", 209.99, "sam", "Capacity: 2TB\nInterface: PCIe 4.0 NVMe\nRead: 7450 MB/s\nWrite: 6900 MB/s\nForm Factor: M.2 2280", isFeatured = false, isPopular = false, rating = 4.7f),
        ProductEntity(23, "Keychron Q1 Pro Mechanical Keyboard", "Premium wireless mechanical keyboard with aluminum body.", "Accessories", 199.99, "key", "Layout: 75%\nSwitches: Gateron Red\nConnectivity: Bluetooth, USB-C\nFrame: Aluminum\nRGB: Per-key", isFeatured = false, isPopular = false, rating = 4.4f),
        ProductEntity(24, "Razer DeathAdder V3 Pro", "Ultra-lightweight wireless gaming mouse at 63g.", "Accessories", 149.99, "razer", "DPI: 30000\nWeight: 63g\nConnectivity: HyperSpeed\nBattery: 90 hours\nSensor: Focus Pro 30K", isFeatured = false, isPopular = false, rating = 4.5f)
    )

    /** Inserts the catalog only when the products table is empty (idempotent). */
    fun insertIfEmpty(db: SupportSQLiteDatabase) {
        val count = db.query("SELECT COUNT(*) FROM products").use { cursor ->
            if (cursor.moveToFirst()) cursor.getInt(0) else 0
        }
        if (count > 0) return

        val statement = db.compileStatement(
            "INSERT INTO products (id, name, description, category, price, image_url, specifications, is_featured, is_popular, rating) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
        )
        db.beginTransaction()
        try {
            for (p in products) {
                statement.clearBindings()
                statement.bindLong(1, p.id.toLong())
                statement.bindString(2, p.name)
                statement.bindString(3, p.description)
                statement.bindString(4, p.category)
                statement.bindDouble(5, p.price)
                statement.bindString(6, p.imageUrl)
                statement.bindString(7, p.specifications)
                statement.bindLong(8, if (p.isFeatured) 1 else 0)
                statement.bindLong(9, if (p.isPopular) 1 else 0)
                statement.bindDouble(10, p.rating.toDouble())
                statement.executeInsert()
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }
}