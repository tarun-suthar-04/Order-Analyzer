package com.orderanalyzer;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.orderanalyzer.adapter.RetailerAdapter;
import com.orderanalyzer.adapter.VehicleAdapter;
import com.orderanalyzer.model.OrderRow;
import com.orderanalyzer.parser.CsvParser;
import com.orderanalyzer.parser.ExcelParser;
import com.orderanalyzer.utils.DataProcessor;
import com.orderanalyzer.utils.FileUtils;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * AmulMilkOrdersActivity — Amul Milk Orders analysis screen.
 *
 * <p>Handles Excel/CSV upload, parsing, and summary display for
 * milk order data.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Launch SAF file picker for .xlsx / .csv files</li>
 *   <li>Parse selected file on a background thread (ExecutorService)</li>
 *   <li>Display Vehicle Summary, Retailer Summary, and Raw Data preview</li>
 *   <li>Support real-time search and sort-toggle for the retailer list</li>
 *   <li>Export summary as CSV to Downloads folder</li>
 * </ul>
 */
public class AmulMilkOrdersActivity extends AppCompatActivity {

    // ── UI references ─────────────────────────────────────────────────────

    private View                       rootLayout;
    private Toolbar                    toolbar;
    private ProgressBar                progressBar;
    private CardView                   cardVehicle, cardRetailer, cardRaw;
    private RecyclerView               rvVehicle, rvRetailer;
    private TextView                   tvStats, tvRawData;
    private TextView                   tvVehicleCount, tvRetailerCount;
    private EditText                   etSearch;
    private ExtendedFloatingActionButton fabUpload;
    private AppCompatButton            btnEmptyUpload;
    private MaterialButton             btnSort, btnExport;
    private View                       layoutControls;  // wraps search + sort + export
    private View                       layoutEmpty;

    // ── Adapters & state ─────────────────────────────────────────────────

    private VehicleAdapter   vehicleAdapter;
    private RetailerAdapter  retailerAdapter;
    private DataProcessor    dataProcessor;

    /** Full sorted list; kept so sort toggle can re-sort without re-parsing */
    private List<Map.Entry<String, Double>> retailerEntries = new ArrayList<>();
    private boolean sortAscending = false;

    // ── Background threading ─────────────────────────────────────────────

    private final ExecutorService executor    = Executors.newSingleThreadExecutor();
    private final Handler         mainHandler = new Handler(Looper.getMainLooper());

    // ── File picker ───────────────────────────────────────────────────────

    private final ActivityResultLauncher<Intent> filePickerLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            Uri fileUri = result.getData().getData();
                            if (fileUri != null) processFile(fileUri);
                        }
                    });

    // ── Permission launcher (for API < 29 CSV export) ─────────────────────

    private final ActivityResultLauncher<String> permissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(),
                    granted -> {
                        if (granted) exportData();
                        else showSnackbar("Storage permission denied — cannot export.");
                    });

    // ═════════════════════════════════════════════════════════════════════
    // Lifecycle
    // ═════════════════════════════════════════════════════════════════════

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_amul_milk_orders);

        initViews();
        setupToolbar();
        setupRecyclerViews();
        setupSearchBar();
        setupButtons();

        dataProcessor = new DataProcessor();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();    // clean up the background thread pool
    }

    // ═════════════════════════════════════════════════════════════════════
    // Initialisation helpers
    // ═════════════════════════════════════════════════════════════════════

    private void initViews() {
        rootLayout      = findViewById(R.id.root_layout);
        progressBar     = findViewById(R.id.progress_bar);
        cardVehicle     = findViewById(R.id.card_vehicle);
        cardRetailer    = findViewById(R.id.card_retailer);
        cardRaw         = findViewById(R.id.card_raw);
        rvVehicle       = findViewById(R.id.rv_vehicle);
        rvRetailer      = findViewById(R.id.rv_retailer);
        tvStats         = findViewById(R.id.tv_stats);
        tvRawData       = findViewById(R.id.tv_raw_data);
        tvVehicleCount  = findViewById(R.id.tv_vehicle_count);
        tvRetailerCount = findViewById(R.id.tv_retailer_count);
        etSearch        = findViewById(R.id.et_search);
        fabUpload       = findViewById(R.id.fab_upload);
        btnEmptyUpload  = findViewById(R.id.btn_empty_upload);
        btnSort         = findViewById(R.id.btn_sort);
        btnExport       = findViewById(R.id.btn_export);
        layoutControls  = findViewById(R.id.layout_controls);
        layoutEmpty     = findViewById(R.id.layout_empty);
    }

    private void setupToolbar() {
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerViews() {
        vehicleAdapter = new VehicleAdapter(new ArrayList<>());
        rvVehicle.setLayoutManager(new LinearLayoutManager(this));
        rvVehicle.setAdapter(vehicleAdapter);
        rvVehicle.setNestedScrollingEnabled(false);

        retailerAdapter = new RetailerAdapter(new ArrayList<>());
        rvRetailer.setLayoutManager(new LinearLayoutManager(this));
        rvRetailer.setAdapter(retailerAdapter);
        rvRetailer.setNestedScrollingEnabled(false);
    }

    private void setupSearchBar() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                retailerAdapter.getFilter().filter(s);
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void setupButtons() {
        fabUpload.setOnClickListener(v -> openFilePicker());
        btnEmptyUpload.setOnClickListener(v -> openFilePicker());

        btnSort.setOnClickListener(v -> {
            sortAscending = !sortAscending;
            sortAndUpdateRetailers();
            btnSort.setText(sortAscending ? "Sort ↑" : "Sort ↓");
        });

        btnExport.setOnClickListener(v -> requestExportPermissionIfNeeded());
    }

    // ═════════════════════════════════════════════════════════════════════
    // File picking & processing
    // ═════════════════════════════════════════════════════════════════════

    /** Launches the SAF file picker constrained to Excel and CSV MIME types. */
    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "application/vnd.ms-excel",
                "text/csv",
                "text/comma-separated-values",
                "application/csv"
        });
        filePickerLauncher.launch(intent);
    }

    /**
     * Validates the file extension, then dispatches parsing to a background thread.
     * The UI thread is only touched via {@link #mainHandler}.
     */
    private void processFile(Uri fileUri) {
        showLoading(true);
        hideResults();
        etSearch.setText("");       // clear any previous search

        executor.execute(() -> {
            try {
                String extension = FileUtils.getFileExtension(AmulMilkOrdersActivity.this, fileUri);
                List<OrderRow> rows;

                // Check magic bytes to see if it's secretly a ZIP (Excel) file disguised as CSV
                boolean isZip = false;
                try (InputStream is = getContentResolver().openInputStream(fileUri)) {
                    if (is != null) {
                        byte[] magic = new byte[2];
                        if (is.read(magic) == 2) {
                            if (magic[0] == 0x50 && magic[1] == 0x4B) { // 'PK'
                                isZip = true;
                            }
                        }
                    }
                } catch (Exception ignored) {}

                // Override extension if magic bytes say it's an Excel spreadsheet
                if (isZip) {
                    extension = "xlsx";
                }

                if ("xlsx".equals(extension) || "xls".equals(extension)) {
                    // Apache POI needs a real File, not a Uri stream
                    File tempFile = FileUtils.copyUriToTempFile(AmulMilkOrdersActivity.this, fileUri, extension);
                    rows = ExcelParser.parse(tempFile);
                    //noinspection ResultOfMethodCallIgnored
                    tempFile.delete();          // remove temp file immediately after parsing

                } else if ("csv".equals(extension)) {
                    try (InputStream inputStream = getContentResolver().openInputStream(fileUri)) {
                        rows = CsvParser.parse(inputStream);
                    }
                } else {
                    throw new Exception(
                            "Invalid file format (" + extension + "). Please upload a .xlsx, .xls, or .csv file.");
                }

                dataProcessor.processRows(rows);

                // Switch back to the main thread to update UI
                mainHandler.post(() -> {
                    showLoading(false);
                    displayResults();
                });

            } catch (Exception e) {
                mainHandler.post(() -> {
                    showLoading(false);
                    showSnackbar("❌ " + e.getMessage());
                });
            }
        });
    }

    // ═════════════════════════════════════════════════════════════════════
    // Display results
    // ═════════════════════════════════════════════════════════════════════

    /** Populates all three result sections after successful parsing. */
    private void displayResults() {
        toolbar.setSubtitle("Analysis Summary");

        // ── Section A: Vehicle Summary ─────────────────────────────────
        List<Map.Entry<String, Double>> vehicleEntries =
                new ArrayList<>(dataProcessor.getVehicleTotals().entrySet());
        // Default: highest first
        vehicleEntries.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        vehicleAdapter.updateData(vehicleEntries);
        tvVehicleCount.setText(vehicleEntries.size() + " vehicle(s)");

        // ── Section B: Retailer Summary ───────────────────────────────
        retailerEntries = new ArrayList<>(dataProcessor.getRetailerTotals().entrySet());
        sortAscending = false;
        sortAndUpdateRetailers();
        tvRetailerCount.setText(retailerEntries.size() + " retailer(s)");

        // ── Stats bar ─────────────────────────────────────────────────
        int totalRows = dataProcessor.getRows().size();
        double grandTotal = retailerEntries.stream()
                .mapToDouble(Map.Entry::getValue).sum();
        tvStats.setText(String.format(
                "📊  %d rows  |  Grand Total: %.0f units", totalRows, grandTotal));

        // ── Section C: Raw Data Preview (max 50 rows for performance) ──
        buildRawDataPreview();

        // Make everything visible
        tvStats.setVisibility(View.VISIBLE);
        cardVehicle.setVisibility(View.VISIBLE);
        cardRetailer.setVisibility(View.VISIBLE);
        cardRaw.setVisibility(View.VISIBLE);
        layoutControls.setVisibility(View.VISIBLE);
        layoutEmpty.setVisibility(View.GONE);
        fabUpload.setVisibility(View.VISIBLE);
    }

    private void buildRawDataPreview() {
        List<OrderRow> rows        = dataProcessor.getRows();
        int            displayMax  = Math.min(rows.size(), 50);
        StringBuilder  sb          = new StringBuilder();

        // Simple fixed-width header
        sb.append(String.format("%-26s %-14s %s%n", "RETAILER", "VEHICLE", "TOTAL"));
        sb.append("─".repeat(54)).append("\n");

        for (int i = 0; i < displayMax; i++) {
            OrderRow row = rows.get(i);
            sb.append(String.format("%-26s %-14s %.0f%n",
                    truncate(row.getRetailerName(), 25),
                    truncate(row.getVehicleName(),  13),
                    row.getRowTotal()));
        }
        if (rows.size() > 50) {
            sb.append("\n… and ").append(rows.size() - 50).append(" more rows");
        }
        tvRawData.setText(sb.toString());
    }

    // ═════════════════════════════════════════════════════════════════════
    // Sort & Search
    // ═════════════════════════════════════════════════════════════════════

    private void sortAndUpdateRetailers() {
        List<Map.Entry<String, Double>> sorted = new ArrayList<>(retailerEntries);
        if (sortAscending) {
            Collections.sort(sorted, (a, b) -> Double.compare(a.getValue(), b.getValue()));
        } else {
            Collections.sort(sorted, (a, b) -> Double.compare(b.getValue(), a.getValue()));
        }
        retailerEntries = sorted;
        retailerAdapter.updateData(sorted);
    }

    // ═════════════════════════════════════════════════════════════════════
    // Export
    // ═════════════════════════════════════════════════════════════════════

    private void requestExportPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            // API 24-28: need WRITE_EXTERNAL_STORAGE
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                return;
            }
        }
        exportData();
    }

    private void exportData() {
        boolean success = dataProcessor.exportSummaryToCsv(this);
        showSnackbar(success
                ? "✅ Summary exported to Downloads folder"
                : "❌ Export failed. Please try again.");
    }

    // ═════════════════════════════════════════════════════════════════════
    // UI helpers
    // ═════════════════════════════════════════════════════════════════════

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        fabUpload.setEnabled(!show);
        btnEmptyUpload.setEnabled(!show);
    }

    private void hideResults() {
        toolbar.setSubtitle("Upload your order file to get started");
        cardVehicle.setVisibility(View.GONE);
        cardRetailer.setVisibility(View.GONE);
        cardRaw.setVisibility(View.GONE);
        layoutControls.setVisibility(View.GONE);
        tvStats.setVisibility(View.GONE);
        fabUpload.setVisibility(View.GONE);
    }

    private void showSnackbar(String message) {
        Snackbar.make(rootLayout, message, Snackbar.LENGTH_LONG).show();
    }

    /** Truncates a string and appends "…" if it exceeds maxLen. */
    private static String truncate(String text, int maxLen) {
        if (text == null || text.isEmpty()) return "";
        return text.length() > maxLen ? text.substring(0, maxLen - 1) + "…" : text;
    }
}
