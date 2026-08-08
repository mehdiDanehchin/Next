# Next

**Next** is a modern e-commerce application for digital goods (phones, laptops, cameras and accessories), built with **Kotlin** and **Jetpack Compose**. It combines a clean Material 3 interface with a fully offline-first data layer powered by Room.

## Features

- **Home storefront** — horizontal *Featured Products* carousel plus a *Popular Products* grid, with category chips and instant debounced search
- **Sorting & filtering** — sort by price/rating and filter by price band and minimum rating
- **Product detail** — full specifications, star rating, wishlist toggle, add-to-cart and a shared-element (morphing) image transition
- **Wishlist** — persistent favorites with live badge count in the bottom navigation
- **Cart & Checkout** — quantity management, address form, shipping methods and order placement
- **Orders** — order history with status tracking (pending → processing → shipped → delivered) and cancel support
- **Theming** — light / dark / system theme persisted with DataStore
- **Localization** — English + Persian (RTL) with full string coverage
- **Splash screen** — AndroidX SplashScreen API (black background with the NEXT logo), correctly sized adaptive launcher icon

## Tech Stack

- **Kotlin** + **Jetpack Compose** (Material 3, Compose BOM 2024.12)
- **Room** (KSP) — local SQLite database with a seeded 24-product catalog
- **DataStore Preferences** — persisted theme mode
- **Coil** — image loading in Compose
- **Navigation Compose** — single-activity navigation with shared-element transitions
- **AndroidX SplashScreen** — startup experience on Android 12+
- **Gradle** with AGP 9, version catalog (`libs.versions.toml`); `minSdk 24`, `targetSdk 35`

## Architecture

Clean layering with **UI → ViewModel → Repository → DataSource (Room)**, wired manually through an `AppContainer` (no DI framework):

```
ui/screens (Compose)  →  viewmodel (StateFlow + UiState)  →  data/repository  →  database (Room DAOs / entities)
```

- Single-activity, single navigation graph; screens collect `StateFlow` with `collectAsStateWithLifecycle`
- Room `Flow` queries keep cart/wishlist/orders state reactive across screens (shared badges)
- `SharedTransitionLayout` powers the product-image morph between Home/Detail

## Screens

| Screen | Route | Purpose |
| --- | --- | --- |
| Home | `home` | Featured carousel, categories, search, popular grid with sort/filter |
| Product Detail | `product_detail/{productId}` | Full product info, wishlist toggle, add to cart |
| Wishlist | `wishlist` | Saved products, navigates to detail |
| Cart | `cart` | Line items, quantities, total, checkout entry |
| Checkout | `checkout` | Address form, shipping methods, place order |
| Orders | `orders` | Order list with status and line items |
| Profile | `profile` | Theme options and About |

## Getting Started

1. **Requirements** — JDK 17+, Android Studio (latest stable), Android SDK Platform 35
2. **Clone / open** — `File ▸ Open` and select the project root; Android Studio generates `local.properties` automatically
3. **Sync** — let Gradle sync (AGP 9.x; the Gradle version catalog pins all dependencies — no manual setup)
4. **Run** — select the `app` configuration and press Run, or use the Gradle tasks below

## Configuration

The app requires **no API keys or external services** — all data (products, orders, wishlist, cart) lives in a local Room database seeded on first launch. Nothing to configure.

## Build

```bash
# Debug APK (installable, debug-signed)
./gradlew :app:assembleDebug

# Release APK (unsigned — add your own signing config in app/build.gradle to distribute)
./gradlew :app:assembleRelease

# Lint + unit tests
./gradlew :app:lintDebug :app:testDebugUnitTest

# Install on a connected device
./gradlew :app:installDebug
```

Debug and release outputs land in `app/build/outputs/apk/`.

## Developer

Created by **Mehdi Danehchin**

## License

No license has been specified yet. © 2026 — all rights reserved by the author until a license is added.