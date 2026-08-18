# Firebase setup — Next

This app integrates Firebase Authentication (Google provider), Cloud Firestore
and Credential Manager. Everything on the code side is already wired; the steps
below are the MANUAL, one-time console steps that require your Google account.

## 1. Create the Firebase project & register the app

1. Go to https://console.firebase.google.com → **Add project** (name it e.g. `next-app`).
2. In the project → **Add app → Android**.
3. Package name (exact): `com.example.next`
4. App nickname: `Next`
5. **SHA-1** of the debug keystore (required for Google Sign-In). On this machine:
   `keytool -list -v -keystore %USERPROFILE%\.android\debug.keystore -storepass android -alias androiddebugkey`
   (SHA-256 is optional unless you plan to use Play App Signing / fingerprint-based features.)
6. Register → download **google-services.json**.

## 2. Install the config file

1. Replace the placeholder file at `app/google-services.json` with the real one.
   (The repo's copy is a build-enabling placeholder; it is gitignored on purpose.)
2. The web OAuth client (`oauth_client` entry with `client_type: 3`) must be
   present in the real file — the app reads it as `default_web_client_id` to
   build the Credential Manager request. The console-generated file includes it.

## 3. Enable Authentication with the Google provider

1. Firebase Console → **Build → Authentication → Get started → Sign-in method**.
2. Enable **Google**.
3. In **Authorized domains** keep the default ones.
4. Note: the Google provider requires SHA-1 registration (step 1.5) — a wrong/
   missing SHA-1 fails at runtime with a network error, not a build error.

## 4. Create Cloud Firestore

1. Firebase Console → **Build → Firestore Database → Create database**.
2. Start in **production mode** (lock mode). Do NOT pick test mode.
3. Choose a location close to your users (e.g. `europe-west1` / `asia-south1`).

## 5. Deploy the security rules

The rules live in `firebase/firestore.rules`. Two options:

- **Firebase CLI** (recommended, repeatable):
  ```
  npm i -g firebase-tools
  firebase login
  firebase init firestore   # use existing firestore.rules; project: the one above
  firebase deploy --only firestore:rules
  ```
- **Console**: Firestore → **Rules** tab → paste the content of
  `firebase/firestore.rules` → **Publish**.

The rules are fail-closed. Each authenticated user may read/write ONLY
`users/{uid}` and its subcollections; unauthenticated (guest) access and any
other user's documents are denied. Per-dataset constraints:

- `users/{uid}` profile: `uid` and `createdAt` immutable.
- `wishlist/{productId}`: owner-only set/upsert/delete.
- `cart/{productId}`: owner-only; `quantity` int 1..99; snapshot fields
  (productName/price/imageUrl) immutable after create (LWW on quantity+updatedAt).
- `orders/{orderId}`: create only with `status == 'PENDING'`; the only update
  allowed is `PENDING -> CANCELLED`; delete forbidden.
- `orders/{orderId}/items/{productId}`: create only (immutable after).
- `settings/settings`: allowlisted keys (`themeMode`, `updatedAt`).

## 6. (Optional) Check Firestore data

After a successful sign-in a document appears automatically at
`users/{uid}` with: `uid`, `displayName`, `email`, `photoUrl`, `createdAt`,
`lastLoginAt` (plus a `preferences` map, and `guestMergedAt` once the
guest->account merge ran). Signed-in user data then syncs under:

```
users/{uid}/wishlist/{productId}       productId, productName, price, imageUrl, addedAt
users/{uid}/cart/{productId}           productId, productName, price, imageUrl, quantity, updatedAt
users/{uid}/orders/{orderId}           orderId, orderDate, status, fullName, phone, address, city,
                                       shippingMethod, shippingPrice, subtotal, total, createdAt, updatedAt
users/{uid}/orders/{orderId}/items/{productId}   productId, productName, price, imageUrl, quantity
users/{uid}/settings/settings          themeMode, updatedAt
```

Order ids are client-generated: `g_<hash>` for guest-origin orders (deterministic,
so a merge retry can never duplicate a document) and `u_<uuid>` for
signed-in orders.

Guest sessions are fully local (Room `next_store.db`, v5): they never create
Firestore documents. On the first successful login per install, the guest data
is merged into the account (wishlist = union, cart = quantity sum capped at
99, orders = append), then the local rows are re-owned to the account.

## Verify

- Build: `gradlew.bat :app:assembleDebug`
- Unit tests: `gradlew.bat :app:testDebugUnitTest`
- Signed-in state survives app restarts (Firebase persists the session).
- Sign-out clears the Firebase session *and* the Credential Manager state,
  so the next sign-in shows the account picker again.
- Account switching is leak-free: signing out purges the previous account's
  local cache; the guest session always starts empty.