# Project Context — Order Analyzer

Order Analyzer helps a small shop or bakery manage daily retailer orders and analyze incoming Amul order spreadsheets from an Android phone.

## Who it serves

Shop owners and staff can maintain a shared shop catalog, enter retailer orders, and prepare a product-quantity requirement for dispatch. A shop Login ID/password can be used on multiple phones; there are no owner/labour roles. The local order workflow remains available offline, and Firestore can synchronize authenticated shop data when connectivity returns.

## Two deliberately separate workflows

### Daily Product Orders

- Product and retailer master lists plus retailer/day orders are stored in local SQLite.
- Add/edit orders, review pending retailers, history, product totals, and bill calculations.
- Preserve only today and yesterday's daily orders. Old order cleanup must not delete products or retailers.
- Copy/share creates a production list in `Product Name - Quantity` format for the selected date. It contains no unit prices, line amounts, or grand total. On-screen product pricing and billing continue to work normally.
- Firebase Auth and shop-scoped Firestore sync are integrated for products, retailers, and daily orders. Persistent cache supports offline operation. Configure the intended Firebase project and test two-device synchronization before production use.

### Amul Milk Orders

- Select a CSV or Excel workbook and view vehicle/retailer summaries, with search, sort, and CSV export.
- Spreadsheet data is processed separately and is not migrated to or synchronized through Firebase.

## Typical workflow

1. Sign in to the shop account on the device. Create an account once with a shop name, Login ID, and password if needed.
2. Maintain the product and retailer lists.
3. Enter each retailer's quantities for today; edit the existing retailer/day order rather than creating a duplicate.
4. Check product totals and billing in the app. Copy/share when a quantity-only dispatch message is needed.
5. Use yesterday's view when checking the retained prior-day records.
6. Separately, open Amul Milk Orders to analyze a supplier spreadsheet.

## Technology

Native Android Java; Gradle/Android Gradle Plugin; AndroidX and Material Components; SQLite; Firebase Authentication and Cloud Firestore; Apache POI for Excel; OpenCSV for CSV; Storage Access Framework for file selection. Application ID: `com.orderanalyzer`; min SDK 26; compile/target SDK 34.

## Operational cautions

- Firebase setup is project-specific: `app/google-services.json` is not included in source control. Enable Email/Password and Firestore, register the exact app ID, and deploy `firestore.rules`.
- Login ID is mapped internally to a Firebase email alias. That alias is not a user email or recovery channel; use the app's stated password management path.
- Do not claim a successful cross-device test based on local unit tests. Test real configured accounts, online/offline transitions, simultaneous order edits, and retention.
- The local `1773752191.csv` workbook is excluded from source control because it may contain customer/order data.

See [README.md](README.md) for setup, [Architecture.md](Architecture.md) for data flow and invariants, [DATA_FORMAT.md](DATA_FORMAT.md) for spreadsheet requirements, and [DEVELOPMENT_GUIDE.md](DEVELOPMENT_GUIDE.md) for development/testing.
