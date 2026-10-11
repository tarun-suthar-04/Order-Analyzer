# Architecture — Order Analyzer

## System overview

The app is a single Android application (`:app`) written in Java. It has two distinct workflows:

- **Daily Product Orders:** local SQLite is the UI's operational data store. `DailyOrderRepository` owns product, retailer, and order reads/writes. `FirebaseSyncManager` mirrors shop-scoped product, retailer, and recent daily-order data to Firestore when authenticated. Firestore's persistent cache supports offline reads/writes and later delivery.
- **Amul Milk Orders:** user-selected spreadsheets are parsed off the main thread and summarized in memory. This workflow is not part of Firebase sync.

The Firebase implementation is present in the app, but live synchronization between devices depends on correct Firebase Console configuration and should be validated against the intended project. Automated unit tests do not simulate Firestore rules, network failures, or concurrent multi-device edits.

## Application entry and navigation

`LoginActivity` handles shop login and account creation. Firebase Auth session persistence determines whether the user proceeds into the app. `HomeActivity` presents the two modules. `ShopProfileActivity` exposes shop/account and sync information and logout.

Daily workflow screens include `DailyProductOrdersActivity`, `AddRetailerOrderActivity`, `TodayRetailersActivity`, `ProductTotalsActivity`, `RetailerOrderDetailActivity`, `ProductsManagementActivity`, `RetailersManagementActivity`, `AddRetailerActivity`, `OrderHistoryActivity`, and `PendingOrdersActivity`.

`AmulMilkOrdersActivity` handles document selection, parsing, summary display, search/sort, and export.

## Local persistence

`DailyOrderDbHelper` owns SQLite schema version 3 and seeds default product/retailer data for a fresh install. `DailyOrderRepository` provides the application data-access operations and triggers old-order cleanup. Product and retailer records are permanent unless explicitly managed by the user. Only daily order records older than yesterday are eligible for cleanup; the current date and previous date are retained.

Core entities:

```text
products(id, cloud_id, name, price, is_active, sort_order, icon_key, color_key)
retailers(id, cloud_id, name, phone, address, preferred_product_ids, is_active)
daily_orders(id, order_date, retailer_id, timestamp, notes)
daily_order_items(order_id, product_id, quantity)
```

Orders are unique by `(order_date, retailer_id)`. Items are unique by `(order_id, product_id)` and are removed with their order. `preferred_product_ids` is a comma-separated list of local product IDs. Cloud IDs provide stable product and retailer document identities across app installs; verify their behavior carefully if changing the ID-generation strategy.

## Firebase account and document model

`FirebaseShopAuthManager` wraps Firebase Authentication. The user-facing Login ID is validated and mapped to an internal pseudo-email alias; that alias is implementation detail and must not be presented as a user's email. The Firebase UID is the shop ID. Session persistence is provided by Firebase Auth; local preferences cache display details such as Login ID and shop name.

```text
shops/{uid}/profile/info
shops/{uid}/products/{productCloudId}
shops/{uid}/retailers/{retailerCloudId}
shops/{uid}/dailyOrders/{yyyy-MM-dd}/retailers/{retailerCloudId}
```

`firestore.rules` restricts reads/writes to authenticated users whose UID matches the `shops/{shopId}` path. Rules must be deployed in Firebase Console/CLI; merely storing the file does not activate it. Firestore persistent local cache is configured in `FirebaseSyncManager`. The app reports Synced, Syncing, Offline, or error states through `SyncStatus` and listeners.

Daily order retention applies to order documents, not products or retailers. The implementation prunes dates older than yesterday locally and in its cloud-retention path. Keep this intentional two-day behavior when changing sync or history code.

## Sync boundaries and cautions

The repository persists a local mutation, then invokes the sync manager. Firestore listeners import cloud changes into SQLite while guarding against echoing imported writes. Orders are keyed by date and retailer cloud ID, which provides one cloud order per retailer/day. Timestamps are used to avoid replacing a newer local order with an older incoming copy.

Offline Firestore writes are queued by the SDK, but application-level reconciliation and conflicts still require device testing. In particular, product/retailer matching and concurrent edits should be tested with two installations sharing the same account. Do not describe this as a transactional distributed database: local SQLite and Firestore are separate stores.

The Authentication UI's Forgot Password action cannot send a standard reset email for the synthetic alias address. The manager currently directs the user to change the password while signed in or use Firebase Console account administration. Do not promise email reset unless a real recovery channel is implemented.

## Copy/share totals

`ProductTotalsTextFormatter` formats only the selected date's product names and total quantities. It chooses a Today/Yesterday heading or a date-neutral older-date heading, preserves the repository's ordering, and omits price, per-product amount, and grand bill total. This is only a text-export format: pricing, screen totals, and billing calculations remain in their existing paths.

## Amul spreadsheet pipeline

```text
Activity Result document picker
  → FileUtils (URI metadata / temporary Excel copy)
  → CsvParser or ExcelParser (single-thread ExecutorService)
  → OrderRow list
  → DataProcessor aggregation
  → main-thread adapters and summary UI
  → optional CSV export using MediaStore (API 29+) or legacy file access
```

Column mapping is fixed: index 4 retailer, index 5 vehicle, index 7 onward numeric quantities. Parsing/file access stays off the main thread; view updates are posted to the main thread. This module does not write imported data into the daily order database.

## Key invariants

- Retain exactly today and yesterday's daily orders; don't remove permanent product/retailer masters as part of retention.
- Keep Amul import/analysis independent of Firebase daily-order synchronization.
- A retailer has at most one order on each date.
- The quantity-only copy/share output must not contain prices or bill amounts; billing elsewhere is unchanged.
- Every Firestore path is scoped to the authenticated UID and protected by deployed rules.
- Never commit local Firebase configuration, SDK paths, signing keys, or generated output.
