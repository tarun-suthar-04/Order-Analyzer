package com.orderanalyzer.parser;

import com.opencsv.CSVReader;
import com.orderanalyzer.model.OrderRow;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses incoming CSV files logic with fixed indexes, 
 * safe numeric parsing, and empty/invalid row skipping.
 */
public class CsvParser {

    /**
     * Parses the CSV input stream using fixed indexes:
     * - Retailer Name: index 4 (Column E)
     * - Vehicle: index 5 (Column F)
     * - Products: index 7 to end (Column H to BI)
     */
    public static List<OrderRow> parse(InputStream inputStream) throws Exception {
        List<String[]> allRows;
        try (CSVReader reader = new CSVReader(new InputStreamReader(inputStream))) {
            allRows = reader.readAll();
        }

        if (allRows == null || allRows.isEmpty()) {
            throw new Exception("CSV file is empty.");
        }

        List<OrderRow> rows = new ArrayList<>();

        // Start from index 1 to skip header row
        for (int r = 1; r < allRows.size(); r++) {
            String[] cols = allRows.get(r);
            
            // Skip if entire row is empty
            if (cols == null || isBlankRow(cols)) {
                continue;
            }

            // Fixed Indexes
            String retailer = safeGet(cols, 4).trim();
            String vehicle = safeGet(cols, 5).trim();

            // Skip row if Retailer name is empty
            if (retailer.isEmpty()) {
                continue;
            }

            if (vehicle.isEmpty()) {
                vehicle = "Unknown Vehicle";
            }

            OrderRow orderRow = new OrderRow();
            orderRow.setRetailerName(retailer);
            orderRow.setVehicleName(vehicle);

            // Row total calculation: SUM of columns from index 7 to last column
            for (int i = 7; i < cols.length; i++) {
                String cell = safeGet(cols, i).trim();
                if (!cell.isEmpty()) {
                    try {
                        double qty = Double.parseDouble(cell);
                        // Using column index as a placeholder name, since exact names may vary and aren't requested
                        orderRow.addProduct("Product " + i, qty);
                    } catch (Exception e) {
                        // ignore invalid values / non-numeric values
                    }
                }
            }

            // Only add if there were any quantities (or if required even without quantities, add it anyway)
            rows.add(orderRow);
        }

        if (rows.isEmpty()) {
            throw new Exception("No valid data rows found.");
        }
        
        return rows;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Gets a cell value from a row array, or "" if the index is out of bounds. */
    private static String safeGet(String[] row, int index) {
        return (index >= 0 && index < row.length && row[index] != null) ? row[index] : "";
    }

    /** Returns true if all cells in the row are null or blank. */
    private static boolean isBlankRow(String[] row) {
        for (String cell : row) {
            if (cell != null && !cell.trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }
}
