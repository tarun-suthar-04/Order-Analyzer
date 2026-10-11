package com.orderanalyzer.utils;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import com.orderanalyzer.model.OrderRow;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * DataProcessor aggregates OrderRow data into vehicle and retailer totals,
 * and provides a CSV export feature.
 */
public class DataProcessor {

    private final List<OrderRow>           rows           = new ArrayList<>();
    private final HashMap<String, Double>  vehicleTotals  = new HashMap<>();
    private final HashMap<String, Double>  retailerTotals = new HashMap<>();

    // ── Aggregation ────────────────────────────────────────────────────────

    /**
     * Processes a list of rows and fills {@link #vehicleTotals} and
     * {@link #retailerTotals}.
     *
     * <p>Algorithm:
     * <pre>
     *   for each row:
     *       vehicleTotals[vehicle]   += rowTotal
     *       retailerTotals[retailer] += rowTotal
     * </pre>
     */
    public void processRows(List<OrderRow> orderRows) {
        rows.clear();
        vehicleTotals.clear();
        retailerTotals.clear();

        rows.addAll(orderRows);

        for (OrderRow row : orderRows) {
            String vehicle  = nullSafe(row.getVehicleName(),  "Unknown Vehicle");
            String retailer = nullSafe(row.getRetailerName(), "Unknown Retailer");
            double total    = row.getRowTotal();

            vehicleTotals.put(vehicle,  vehicleTotals.getOrDefault(vehicle,  0.0) + total);
            retailerTotals.put(retailer, retailerTotals.getOrDefault(retailer, 0.0) + total);
        }
    }

    // ── Getters ────────────────────────────────────────────────────────────

    public List<OrderRow>          getRows()           { return rows; }
    public HashMap<String, Double> getVehicleTotals()  { return vehicleTotals; }
    public HashMap<String, Double> getRetailerTotals() { return retailerTotals; }

    // ── CSV Export ─────────────────────────────────────────────────────────

    /**
     * Exports a summary CSV to the Downloads folder.
     *
     * <ul>
     *   <li>API 29+ : uses {@link MediaStore} (no permission needed)</li>
     *   <li>API 24-28: writes directly to public Downloads directory</li>
     * </ul>
     *
     * @return {@code true} on success, {@code false} on any failure
     */
    public boolean exportSummaryToCsv(Context context) {
        String ts       = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String fileName = "OrderSummary_" + ts + ".csv";

        try {
            OutputStream outputStream;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Downloads.DISPLAY_NAME,  fileName);
                values.put(MediaStore.Downloads.MIME_TYPE,     "text/csv");
                values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
                Uri uri = context.getContentResolver()
                        .insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                if (uri == null) return false;
                outputStream = context.getContentResolver().openOutputStream(uri);
            } else {
                File dir  = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                File file = new File(dir, fileName);
                outputStream = new FileOutputStream(file);
            }

            if (outputStream == null) return false;

            try (OutputStreamWriter w = new OutputStreamWriter(outputStream)) {
                // Vehicle section
                w.write("=== VEHICLE SUMMARY ===\n");
                w.write("Vehicle,Total Order\n");
                for (Map.Entry<String, Double> e : vehicleTotals.entrySet()) {
                    w.write(csv(e.getKey()) + "," + fmt(e.getValue()) + "\n");
                }

                w.write("\n=== RETAILER SUMMARY ===\n");
                w.write("Retailer,Total Order\n");
                for (Map.Entry<String, Double> e : retailerTotals.entrySet()) {
                    w.write(csv(e.getKey()) + "," + fmt(e.getValue()) + "\n");
                }
            }
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private static String nullSafe(String value, String fallback) {
        return (value == null || value.trim().isEmpty()) ? fallback : value.trim();
    }

    /** Format number with 2 decimal places */
    private static String fmt(double v) {
        return String.format(Locale.getDefault(), "%.2f", v);
    }

    /** Wrap value in quotes if it contains a comma */
    private static String csv(String v) {
        return v.contains(",") ? "\"" + v + "\"" : v;
    }
}
