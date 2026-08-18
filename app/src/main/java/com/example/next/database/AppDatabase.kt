package com.example.next.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.next.database.dao.CartDao
import com.example.next.database.dao.OrderDao
import com.example.next.database.dao.PendingOpsDao
import com.example.next.database.dao.ProductDao
import com.example.next.database.dao.WishlistDao
import com.example.next.database.entity.CartItemEntity
import com.example.next.database.entity.OrderEntity
import com.example.next.database.entity.OrderItemEntity
import com.example.next.database.entity.PendingOpEntity
import com.example.next.database.entity.ProductEntity
import com.example.next.database.entity.WishlistItemEntity

/**
 * Next store database. Version 3 is the first Room-managed version:
 * versions 1 and 2 were handled by a raw [android.database.sqlite.SQLiteOpenHelper]
 * (see git history). [MIGRATION_2_3] preserves all existing data by moving the
 * legacy tables aside, creating the Room schema, and copying rows across with
 * COALESCE normalization for columns that used to be nullable.
 * Version 4 adds the checkout feature: `orders` and `order_items` tables.
 * Version 5 adds user-ownership: `owner` on wishlist/cart/orders, the
 * client-generated `cloud_id` on orders, and the offline `pending_ops` outbox.
 * Pre-v5 rows get owner/cloud_id backfilled in app code at startup (the guest
 * id is runtime-generated); nothing is deleted.
 */
@Database(
    entities = [
        ProductEntity::class,
        CartItemEntity::class,
        WishlistItemEntity::class,
        OrderEntity::class,
        OrderItemEntity::class,
        PendingOpEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun productDao(): ProductDao

    abstract fun cartDao(): CartDao

    abstract fun wishlistDao(): WishlistDao

    abstract fun orderDao(): OrderDao

    abstract fun pendingOpsDao(): PendingOpsDao

    companion object {

        const val DATABASE_NAME = "next_store.db"

        private const val CREATE_PENDING_OPS =
            "CREATE TABLE IF NOT EXISTS `pending_ops` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`owner` TEXT NOT NULL, " +
                "`table_name` TEXT NOT NULL, " +
                "`row_id` TEXT NOT NULL, " +
                "`op` TEXT NOT NULL, " +
                "`payload` TEXT NOT NULL, " +
                "`created_at` INTEGER NOT NULL, " +
                "`attempt` INTEGER NOT NULL)"

        private const val CREATE_PRODUCTS =
            "CREATE TABLE IF NOT EXISTS `products` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`name` TEXT NOT NULL, " +
                "`description` TEXT NOT NULL, " +
                "`category` TEXT NOT NULL, " +
                "`price` REAL NOT NULL, " +
                "`image_url` TEXT NOT NULL, " +
                "`specifications` TEXT NOT NULL, " +
                "`is_featured` INTEGER NOT NULL, " +
                "`is_popular` INTEGER NOT NULL, " +
                "`rating` REAL NOT NULL)"

        private const val CREATE_CART =
            "CREATE TABLE IF NOT EXISTS `cart` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`product_id` INTEGER NOT NULL, " +
                "`product_name` TEXT NOT NULL, " +
                "`price` REAL NOT NULL, " +
                "`image_url` TEXT NOT NULL, " +
                "`quantity` INTEGER NOT NULL)"

        private const val CREATE_WISHLIST =
            "CREATE TABLE IF NOT EXISTS `wishlist` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`product_id` INTEGER NOT NULL, " +
                "`product_name` TEXT NOT NULL, " +
                "`price` REAL NOT NULL, " +
                "`image_url` TEXT NOT NULL)"

        private const val CREATE_ORDERS =
            "CREATE TABLE IF NOT EXISTS `orders` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`order_date` TEXT NOT NULL, " +
                "`status` TEXT NOT NULL, " +
                "`full_name` TEXT NOT NULL, " +
                "`phone` TEXT NOT NULL, " +
                "`address` TEXT NOT NULL, " +
                "`city` TEXT NOT NULL, " +
                "`shipping_method` TEXT NOT NULL, " +
                "`shipping_price` REAL NOT NULL, " +
                "`subtotal` REAL NOT NULL, " +
                "`total` REAL NOT NULL)"

        private const val CREATE_ORDER_ITEMS =
            "CREATE TABLE IF NOT EXISTS `order_items` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`order_id` INTEGER NOT NULL, " +
                "`product_id` INTEGER NOT NULL, " +
                "`product_name` TEXT NOT NULL, " +
                "`price` REAL NOT NULL, " +
                "`image_url` TEXT NOT NULL, " +
                "`quantity` INTEGER NOT NULL)"

        /** Copy legacy rows into the Room schema, filling NULLs with sensible defaults. */
        private const val COPY_PRODUCTS =
            "INSERT INTO products (id, name, description, category, price, image_url, specifications, is_featured, is_popular, rating) " +
                "SELECT id, name, COALESCE(description, ''), category, price, COALESCE(image_url, ''), COALESCE(specifications, ''), " +
                "COALESCE(is_featured, 0), COALESCE(is_popular, 0), COALESCE(rating, 0.0) FROM products_legacy"

        private const val COPY_CART =
            "INSERT INTO cart (id, product_id, product_name, price, image_url, quantity) " +
                "SELECT id, product_id, product_name, price, COALESCE(image_url, ''), quantity FROM cart_legacy"

        private const val COPY_WISHLIST =
            "INSERT INTO wishlist (id, product_id, product_name, price, image_url) " +
                "SELECT id, product_id, product_name, price, COALESCE(image_url, '') FROM wishlist_legacy"

        /**
         * 2 -> 3: migrate the raw-SQLite schema (DatabaseHelper, version 2) to Room,
         * preserving cart/wishlist/product data.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE products RENAME TO products_legacy")
                db.execSQL("ALTER TABLE cart RENAME TO cart_legacy")
                db.execSQL("ALTER TABLE wishlist RENAME TO wishlist_legacy")

                db.execSQL(CREATE_PRODUCTS)
                db.execSQL(CREATE_CART)
                db.execSQL(CREATE_WISHLIST)

                db.execSQL(COPY_PRODUCTS)
                db.execSQL(COPY_CART)
                db.execSQL(COPY_WISHLIST)

                db.execSQL("DROP TABLE products_legacy")
                db.execSQL("DROP TABLE cart_legacy")
                db.execSQL("DROP TABLE wishlist_legacy")
            }
        }

        /**
         * 3 -> 4: add the checkout schema (orders + order_items). Pure additive
         * migration; existing products/cart/wishlist rows are untouched.
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(CREATE_ORDERS)
                db.execSQL(CREATE_ORDER_ITEMS)
            }
        }

        /**
         * 4 -> 5: add user-ownership. Pure additive migration — NO data is
         * deleted or transformed. The DEFAULT '' placeholder becomes the real
         * owner (`guest:<uuid>`) in app code at startup, because the guest id
         * is runtime-generated and cannot be known inside the Migration.
         */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `wishlist` ADD COLUMN `owner` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `cart` ADD COLUMN `owner` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `orders` ADD COLUMN `owner` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `orders` ADD COLUMN `cloud_id` TEXT NOT NULL DEFAULT ''")
                db.execSQL(CREATE_PENDING_OPS)
            }
        }

        /**
         * Seeds the catalog on a fresh database. Also hooked into onOpen so the
         * catalog self-heals after any destructive fallback. Idempotent (only
         * inserts when the products table is empty).
         */
        val seedCallback = object : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                ProductSeeder.insertIfEmpty(db)
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                ProductSeeder.insertIfEmpty(db)
            }
        }

        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, DATABASE_NAME)
                .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                // Safety net for any database older than v2; user data from v1 was
                // already wiped by the legacy app's own onUpgrade logic.
                .fallbackToDestructiveMigration(dropAllTables = true)
                .addCallback(seedCallback)
                .build()
    }
}