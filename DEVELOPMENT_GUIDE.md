# Development Guide — Order Analyzer

## Requirements and local setup

- Android Studio and Android SDK Platform 34
- JDK 17 recommended (Android source and target compatibility are Java 8)
- Gradle wrapper included; min supported Android version is API 26

Open the repository root in Android Studio, allow Gradle sync, then run the `app` configuration. From PowerShell:

```powershell
./gradlew.bat assembleDebug
./gradlew.bat testDebugUnitTest
adb install app/build/outputs/apk/debug/app-debug.apk
```

The Android application ID and namespace are both `com.orderanalyzer`. Main build configuration is in root `build.gradle`, `settings.gradle`, and `app/build.gradle`.

## Firebase development setup

1. Register an Android app in the intended Firebase project using exactly `com.orderanalyzer`.
2. Download that app's real `google-services.json` into `app/google-services.json`. The file is ignored by Git and must be provisioned separately on each developer/build machine.
3. Enable Firebase Authentication's Email/Password provider and create Cloud Firestore.
4. Review and deploy `firestore.rules` before using real accounts. Never use open read/write rules.
5. Build and verify Login ID/password sign-in, account creation, session restore, logout, and the shop profile.

The app maps a validated Login ID to a private synthetic email alias for Firebase Auth; users do not enter or see that alias. Email password-reset links are not supported for the synthetic address. The current recovery guidance requires changing the password from an authenticated device or administering it through Firebase Console.

Firestore paths use the Firebase UID as the shop ID:

```text
shops/{uid}/profile/info
shops/{uid}/products/{productId}
shops/{uid}/retailers/{retailerId}
shops/{uid}/dailyOrders/{yyyy-MM-dd}/retailers/{retailerId}
```

SQLite remains the local operational store. Firestore persistent cache supports offline access and queues writes for later delivery. Test two installations with the same account; also test concurrent edits, delete propagation, first-install migration, and old-order retention. A passing unit test does not establish live Firebase correctness.

## Source map

| Location | Responsibility |
|---|---|
| `firebase/FirebaseShopAuthManager.java` | Login ID mapping, Firebase Auth, shop profile, logout |
| `firebase/FirebaseSyncManager.java` | Firestore listeners/writes, offline setup, status and retention |
| `db/DailyOrderDbHelper.java` | SQLite schema, seed data and migrations |
| `db/DailyOrderRepository.java` | Local product/retailer/order data access |
| `utils/ProductTotalsTextFormatter.java` | Quantity-only selected-date copy/share text |
| `parser/CsvParser.java`, `parser/ExcelParser.java` | Amul CSV/Excel input |
| `utils/DataProcessor.java` | Amul totals aggregation and export |
| `res/layout/`, `res/drawable/`, `res/values/` | Screen UI, illustrations/icons, and theme |

## Data invariants to preserve

- Retain exactly today's and yesterday's daily orders. Do not delete permanent products or retailers when pruning old orders.
- Keep Amul spreadsheet analysis independent of Firebase Daily Product Orders.
- Maintain one order per retailer per date.
- Copy/share output must contain only product name and selected-date quantity plus the total item quantity. Do not add prices or billing amounts there; the rest of the billing system is unchanged.
- Firebase rules and all data paths must remain shop-UID scoped.
- Keep local config, Firebase JSON, keystores, passwords, and generated files out of source control.

## Schema changes

`DailyOrderDbHelper` currently uses schema version 3. When changing SQLite schema:

1. Update `onCreate()` and increment `DATABASE_VERSION`.
2. Implement a safe, incremental `onUpgrade()` migration for existing installs; never assume users have an empty database.
3. Update model and repository read/write paths and any cloud serialization/import paths that use the changed field.
4. Add or update tests, including migration/retention coverage where appropriate.

## Testing

### Automated unit tests

```powershell
./gradlew.bat testDebugUnitTest
```

Current local tests cover spreadsheet parser behavior, order retention boundaries, and product-totals text formatting. Formatter tests should verify today, yesterday, older date, empty data, preserved ordering, and absence of `₹`/prices/amounts. Tests do not connect to Firebase.

### Daily Product Orders manual checks

- Create/sign into a shop account; relaunch and verify session restore; log out and verify return to Login.
- Add/edit an order and confirm the retailer/day order is updated, not duplicated.
- Verify prices and all existing screen/bill calculations still work.
- Check today/yesterday selectors and confirm older records are pruned while product/retailer masters remain.
- Copy and share totals for today, yesterday, and an empty date; confirm heading/date and quantity-only rows/total.
- With two devices on the same account, test product, retailer, and daily order sync; repeat offline then online.
- Exercise simultaneous edits and deletes and inspect sync status transitions.

### Amul manual checks

- Upload valid `.xlsx`, `.xls`, and `.csv`; verify column positions and summaries.
- Upload a workbook whose filename ends in `.csv`; confirm content detection.
- Search, clear search, toggle sort, and export.
- Try empty, malformed, and blank-row files and verify a recoverable error.
- Confirm imported data does not appear in Daily Product Orders or Firestore.

## Common troubleshooting

| Symptom | Checks |
|---|---|
| Google Services task/config error | Correct `app/google-services.json` exists locally and matches `com.orderanalyzer`; never commit it |
| Auth unavailable | Verify Firebase project, Email/Password provider, network, and configuration |
| Firestore permission denied | Deploy `firestore.rules`; check Firebase UID path and authenticated state |
| Sync remains offline | Check device connectivity and app network permission; later writes may queue until connectivity returns |
| Amul says no valid rows | Confirm retailer in E, vehicle in F, product quantities from H, and header row first |
| Export failure on API 28 or below | Check storage permission declaration/request; API 29+ uses MediaStore |
| Duplicate META-INF build error | Preserve Apache POI packaging exclusions in `app/build.gradle` |

## Dependencies

See `app/build.gradle` for exact versions. Important groups are AndroidX/Material UI, Firebase BoM/Auth/Firestore, Apache POI with XMLBeans, OpenCSV, and JUnit 4.
