# Features — Order Analyzer

This document describes user-facing behavior in the current Android app. The two order workflows remain separate: Firebase-backed shared shop data is used by Daily Product Orders; Amul spreadsheet analysis is local and independent.

## Shop access and profile

- Sign in with Login ID and password; Firebase email aliases are internal and not shown.
- Create a shop account with shop name, Login ID, and password (minimum six characters).
- Firebase Auth retains the session. The profile shows shop/account and sync details and provides logout.
- The current Forgot Password flow explains that a standard email reset is unavailable for the private Login ID alias. Password changes require a signed-in device or Firebase Console account administration.
- The same account can be used on multiple phones; no role or active-device management is implemented.

## Daily Product Orders

### Product and retailer records

- Add, edit, activate/deactivate, and delete product and retailer records.
- Products carry names and prices for in-app billing; retailer records can include contact/address details and preferred products.
- Local SQLite remains the operational store. Signed-in data is synchronized under the shop's Firestore UID. Firestore offline persistence queues/serves data where available.

### Orders and history

- Record quantities for products per retailer and date, with optional notes.
- There is at most one order per retailer per day; saving again edits that order.
- Review today's and yesterday's orders and pending retailers, and edit/delete orders.
- Keep only today and yesterday's daily orders. Older orders are cleaned up automatically. Product and retailer master records are not expired with orders.

### Product totals and billing

- View selected-date product quantities in the app and retain existing prices, per-product amounts, and grand bill calculations on applicable screens.
- Copy Totals and Share produce a quantity-only message: date-aware heading, selected date, each product name with its total quantity, and total items. No currency symbol, unit price, line amount, or grand bill is included in that generated text.
- Product ordering follows the existing totals ordering (ordered products first, then quantity descending and configured sort order).
- When selected date has no quantities, output a suitable empty-state message instead of fabricated rows.

## Sync status

The profile exposes sync state such as Synced, Syncing, Offline — will sync when online, and Sync error, plus last-sync time. Sync depends on Firebase configuration and deployed security rules. Validate online/offline behavior and same-account multi-device changes against the target Firebase project.

## Amul Milk Orders

- Choose `.xlsx`, `.xls`, or `.csv` through Android's file picker.
- Parse expected retailer, vehicle, and product quantity columns; display vehicle and retailer summaries.
- Search retailers, toggle sorting, inspect available raw-data preview, and export a summary CSV.
- Processing occurs on a background executor; spreadsheet content is not added to the Firebase daily-order data model.

## Platform and styling

The app uses native Java activities, AndroidX/Material widgets, the warm off-white/navy/blue visual system, and responsive scrollable content. Current Android configuration is min SDK 26 and target/compile SDK 34.

## Not currently included

- User-facing Firebase email addresses, roles, or active-device management
- Email-based password reset for the synthetic Login ID alias
- Amul spreadsheet data sync/import into Daily Product Orders
- Order retention beyond today and yesterday
- Guaranteed conflict-free simultaneous editing across devices
