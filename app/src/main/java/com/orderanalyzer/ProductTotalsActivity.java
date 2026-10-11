package com.orderanalyzer;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.orderanalyzer.adapter.ProductTotalsAdapter;
import com.orderanalyzer.db.DailyOrderRepository;
import com.orderanalyzer.model.DashboardMetrics;
import com.orderanalyzer.model.ProductTotalItem;
import com.orderanalyzer.utils.ProductTotalsTextFormatter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ProductTotalsActivity extends AppCompatActivity {

    private DailyOrderRepository repository;
    private String selectedDate;

    private TextView tvTotalsDate, tvDatePill;
    private TextView tvStatTotalItems, tvStatProductsCount, tvStatRetailersCount;
    private TextView tvGrandTotalBill, tvStatItemsSummary;
    private RecyclerView rvProductTotals;
    private TextView tvEmptyTotals;
    private ProductTotalsAdapter totalsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_totals);

        repository = DailyOrderRepository.getInstance(this);
        selectedDate = getIntent().getStringExtra("order_date");
        if (selectedDate == null) selectedDate = DailyOrderRepository.getTodayDate();

        initViews();
        loadTotals();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadTotals();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        String date = intent.getStringExtra("order_date");
        if (date != null) {
            selectedDate = date;
        }
        loadTotals();
    }

    private void initViews() {
        ImageButton btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        tvTotalsDate = findViewById(R.id.tv_totals_date);
        tvDatePill = findViewById(R.id.tv_date_pill);
        tvStatTotalItems = findViewById(R.id.tv_stat_total_items);
        tvStatProductsCount = findViewById(R.id.tv_stat_products_count);
        tvStatRetailersCount = findViewById(R.id.tv_stat_retailers_count);
        tvGrandTotalBill = findViewById(R.id.tv_grand_total_bill);
        tvStatItemsSummary = findViewById(R.id.tv_stat_items_summary);

        rvProductTotals = findViewById(R.id.rv_product_totals);
        tvEmptyTotals = findViewById(R.id.tv_empty_totals);

        rvProductTotals.setLayoutManager(new LinearLayoutManager(this));
        totalsAdapter = new ProductTotalsAdapter();
        rvProductTotals.setAdapter(totalsAdapter);

        // Click on Date Card, Date Text, or Date Pill opens the retained 7-day window.
        View.OnClickListener dateClickListener = v -> showDateSelectionDialog();
        findViewById(R.id.card_date_select).setOnClickListener(dateClickListener);
        tvTotalsDate.setOnClickListener(dateClickListener);
        tvDatePill.setOnClickListener(dateClickListener);

        findViewById(R.id.btn_copy_totals).setOnClickListener(v -> copyTotalsToClipboard());
        findViewById(R.id.btn_share_totals).setOnClickListener(v -> shareTotals());
    }

    private void showDateSelectionDialog() {
        List<String> lastDates = DailyOrderRepository.getLastNDates(DailyOrderRepository.ORDER_RETENTION_DAYS);
        String[] options = new String[lastDates.size()];

        for (int i = 0; i < lastDates.size(); i++) {
            String d = lastDates.get(i);
            String rel = DailyOrderRepository.getRelativeLabel(d);
            String formatted = DailyOrderRepository.formatDisplayDate(d);
            String mark = d.equals(selectedDate) ? "✓  " : "    ";
            options[i] = mark + rel + " (" + formatted + ")";
        }

        new AlertDialog.Builder(this)
                .setTitle("Select Date (Last 7 Days)")
                .setItems(options, (dialog, which) -> {
                    selectedDate = lastDates.get(which);
                    dialog.dismiss();
                    loadTotals();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void loadTotals() {
        tvTotalsDate.setText(DailyOrderRepository.formatDisplayDate(selectedDate));
        String relativeLabel = DailyOrderRepository.getRelativeLabel(selectedDate);
        tvDatePill.setText(relativeLabel + " ▼");

        DashboardMetrics metrics = repository.getDashboardMetrics(selectedDate);
        tvStatTotalItems.setText(String.valueOf(metrics.getTotalItems()));
        tvStatProductsCount.setText(String.valueOf(metrics.getProductsOrderedCount()));
        tvStatRetailersCount.setText(String.valueOf(metrics.getRetailersCount()));

        tvGrandTotalBill.setText(String.format(Locale.US, "₹%.2f", metrics.getTotalBillAmount()));
        tvStatItemsSummary.setText(metrics.getTotalItems() + " Units Total");

        List<ProductTotalItem> allTotals = repository.getProductTotalsForDate(selectedDate);
        // Only show products that have actual ordered quantity (> 0) on the selected date
        List<ProductTotalItem> orderedOnly = new ArrayList<>();
        for (ProductTotalItem item : allTotals) {
            if (item.getTotalQuantity() > 0) {
                orderedOnly.add(item);
            }
        }

        if (orderedOnly.isEmpty()) {
            rvProductTotals.setVisibility(View.GONE);
            tvEmptyTotals.setVisibility(View.VISIBLE);
            tvEmptyTotals.setText("No orders recorded for " + relativeLabel + ".");
            totalsAdapter.setData(new ArrayList<>());
        } else {
            rvProductTotals.setVisibility(View.VISIBLE);
            tvEmptyTotals.setVisibility(View.GONE);
            totalsAdapter.setData(orderedOnly);
        }
    }

    private String buildTotalsSummaryText() {
        return ProductTotalsTextFormatter.format(
                selectedDate,
                DailyOrderRepository.getTodayDate(),
                DailyOrderRepository.getYesterdayDate(),
                DailyOrderRepository.formatDisplayDate(selectedDate),
                totalsAdapter.getItems());
    }

    private void copyTotalsToClipboard() {
        String text = buildTotalsSummaryText();
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Total Orders", text);
        cm.setPrimaryClip(clip);
        Toast.makeText(this, "Totals copied to clipboard", Toast.LENGTH_SHORT).show();
    }

    private void shareTotals() {
        String text = buildTotalsSummaryText();
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, "Total Orders - " + DailyOrderRepository.formatDisplayDate(selectedDate));
        intent.putExtra(Intent.EXTRA_TEXT, text);
        startActivity(Intent.createChooser(intent, "Share Total Orders"));
    }
}
