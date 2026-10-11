# Order Analyzer

Order Analyzer is a native Android app for recording a shop's daily retailer orders and analyzing Amul order spreadsheets. Daily Product Orders are stored locally in SQLite and synchronized to the shop's Firestore account when signed in. Firestore's persistent cache keeps the daily workflow usable offline; changes can sync when connectivity returns. The Amul spreadsheet workflow remains separate and local.

## Features

- Maintain products and retailers, enter and edit retailer orders, and review product totals and order history.
- Keep only today's and yesterday's daily orders. Older orders are automatically removed; product and retailer master records are retained.
- Copy or share selected-date product totals as `Product Name - Quantity` with a date-aware heading. This text intentionally excludes prices, line amounts, and grand bill totals; in-app billing remains unchanged.
- Sign in with a shop Login ID and password, share the same shop account across devices, and view sync status from the profile screen.
- Analyze `.xlsx`, `.xls`, and `.csv` files in the Amul Milk Orders module; search/sort summaries and export a CSV.

## Requirements

- Android Studio with Android SDK Platform 34
- JDK 17 recommended for Android Gradle Plugin 8.3.2 (Java source/target compatibility is Java 8)
- Android device or emulator running API 26 or later
- A Firebase project configured for the application ID `com.orderanalyzer` to use authentication and cloud sync

## Build

Open this directory in Android Studio, allow Gradle sync, and run the `app` configuration. For a debug build from PowerShell:

```powershell
./gradlew.bat assembleDebug
./gradlew.bat testDebugUnitTest
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

## Firebase setup

1. Create or select a Firebase project. Register an Android app with the exact package/application ID `com.orderanalyzer`.
2. Download its real `google-services.json` and place it at `app/google-services.json`. This file is intentionally excluded from Git; do not commit credentials or substitute a fabricated configuration.
3. In Firebase Console, enable Authentication → Email/Password and create a Cloud Firestore database.
4. Review and deploy the rules in [`firestore.rules`](firestore.rules). They permit an authenticated UID to access only `shops/{uid}` and descendants. Never deploy open `allow read, write: if true` rules.
5. Build and test on devices using the configured Firebase project.

The app presents a Login ID rather than an email. Internally the authentication manager maps valid IDs (3–30 letters, numbers, dots, underscores, or hyphens) to a private Firebase email alias. Firebase Auth persists the session. Password recovery by email is not available for these aliases; the app explains the available recovery path. Do not share the alias as a user-facing address.

## Data and sync

Local SQLite is the app's operational store. The signed-in shop's cloud data is organized as:

```text
shops/{shopId}/profile/info
shops/{shopId}/products/{productId}
shops/{shopId}/retailers/{retailerId}
shops/{shopId}/dailyOrders/{yyyy-MM-dd}/retailers/{retailerId}
```

Here `shopId` is the Firebase Authentication UID. Products and retailers are persistent; daily order documents are retained for today and yesterday. Cloud rules are in `firestore.rules`. Firebase synchronization should be validated against the target Firebase project and on two real devices before relying on it operationally; this repository's automated tests do not prove live multi-device behavior.

## Modules

### Daily Product Orders

Shop sign-in leads to the daily order dashboard. Manage products and retailers, add or edit one order per retailer/day, review today's/yesterday's totals, and use copy/share for a quantity-only production list. Local billing and product prices remain part of the app and are not included in copied/shared totals. SQLite and Firestore support the offline-first daily workflow.

### Amul Milk Orders

Select a `.xlsx`, `.xls`, or `.csv` file using Android's document picker. The parser expects retailer name in column E (index 4), vehicle name in F (index 5), and numeric product quantities from H (index 7) onward. It computes vehicle and retailer summaries and can export a CSV. This module is separate from Firebase Daily Product Orders.

## Repository map

```text
app/src/main/java/com/orderanalyzer/
  firebase/     Firebase shop authentication, sync, and status
  db/           SQLite schema and daily-order repository
  model/        Products, retailers, orders, and spreadsheet rows
  parser/       CSV and Excel readers for the Amul module
  utils/        File helpers, aggregation, quantity-only copy/share formatting
  *Activity.java Android screens
app/src/main/res/ UI layouts, drawables, and theme resources
firestore.rules Firestore ownership rules (deploy explicitly)
```

## File format reference

See [`DATA_FORMAT.md`](DATA_FORMAT.md) for the Amul import columns, row handling, and export format. See [`DEVELOPMENT_GUIDE.md`](DEVELOPMENT_GUIDE.md) for local setup, Firebase setup, and test checklists.

## Tech stack

Java, AndroidX/AppCompat, Material Components, SQLite, Firebase Authentication, Cloud Firestore, Apache POI, OpenCSV, and Gradle. The module uses compile/target SDK 34 and min SDK 26. See `app/build.gradle` for dependency versions.

## Privacy and publishing

Do not commit `local.properties`, `app/google-services.json`, signing keys, passwords, generated build output, or the local `1773752191.csv` workbook (it may contain retailer/order data). `.gitignore` lists these local files. Use synthetic sample data for public testing.

This project is proprietary. All rights reserved.
