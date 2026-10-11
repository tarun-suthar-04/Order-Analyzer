# Amul Milk Orders — Data Format

This document applies only to the Amul Milk Orders spreadsheet analyzer. It does not describe the Firestore data model or Daily Product Orders. Imported rows are analyzed in memory and are not synced to the shop account.

## Supported files

| Format | Extension | Parser |
|---|---|---|
| Excel Open XML | `.xlsx` | Apache POI |
| Legacy Excel | `.xls` | Apache POI |
| Comma-separated values | `.csv` | OpenCSV |

The parser checks file content in addition to extension so a ZIP-based workbook named `.csv` can be recognized as Excel.

## Required column positions

Column mapping is fixed; header names are not used for detection.

| Zero-based index | Spreadsheet column | Meaning | Handling |
|---:|---|---|---|
| 0–3 | A–D | Unused | Ignored |
| 4 | E | Retailer name | Required; missing-name rows are skipped |
| 5 | F | Vehicle name | Optional; empty value uses `Unknown Vehicle` |
| 6 | G | Unused | Ignored |
| 7 onward | H onward | Product quantities | Numeric values contribute to row total |

The first row (index 0) is treated as the header and skipped. Empty rows are skipped. Non-numeric quantity cells do not contribute; negative numeric values are included as-is.

## Aggregation

For each accepted row:

```text
rowTotal = sum(numeric cells from column index 7 through the last column)
```

The application then groups row totals by vehicle and retailer. It displays these summaries and an available raw-data preview. This import summary is separate from the Daily Product Orders product-price/billing calculations.

## Export

The Amul module exports `OrderSummary_<yyyyMMdd_HHmmss>.csv` to Downloads. Android 10+ uses MediaStore; Android 8–9 uses the legacy external-storage path/permission. The export contains vehicle and retailer total sections. Values are formatted to two decimal places and CSV fields containing commas are quoted.

Illustrative shape:

```csv
=== VEHICLE SUMMARY ===
Vehicle,Total Order
Vehicle 1,58.00

=== RETAILER SUMMARY ===
Retailer,Total Order
ABC Store,35.00
```

## Example input

```csv
OrderDate,OrderID,Zone,SalesRep,RetailerName,VehicleName,Notes,Product1,Product2,Product3
2026-10-05,ORD001,North,John,ABC Store,Vehicle 1,,10,20,5
2026-10-05,ORD002,North,John,XYZ Mart,Vehicle 1,,0,15,8
2026-10-05,ORD003,South,Jane,DEF Shop,Vehicle 2,,12,0,3
```

The resulting row totals are 35, 23, and 15; vehicle totals are Vehicle 1 = 58 and Vehicle 2 = 15; retailer totals are ABC Store = 35, XYZ Mart = 23, DEF Shop = 15.

## Sample file

The workspace may contain a local sample workbook named `1773752191.csv`; it is excluded from source control because it may contain retailer/order data. Use a synthetic test file when sharing or testing publicly.
