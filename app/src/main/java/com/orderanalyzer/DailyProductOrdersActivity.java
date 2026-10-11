package com.orderanalyzer;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.orderanalyzer.db.DailyOrderRepository;
import com.orderanalyzer.model.DailyRetailer;
import com.orderanalyzer.model.DashboardMetrics;

import java.util.List;
import java.util.Locale;

public class DailyProductOrdersActivity extends AppCompatActivity {

    private DailyOrderRepository repository;
    private String todayDate;

    private TextView tvDashboardDate, tvDateChip, tvSyncStatusBadge;
    private TextView tvHeroTag, tvHeroTitle, tvHeroSubtitle;
    private TextView tvCountRetailerOrders, tvCountProductTotals, tvBillProductTotals;
    private TextView tvCountProducts, tvCountPendingOrders, tvCountRetailers;
    private TextView tvTakeOrdersSubtitle;

    private final com.orderanalyzer.firebase.FirebaseSyncManager.OnSyncStatusChangeListener syncListener = (status, lastSyncFormatted) -> {
        runOnUiThread(() -> {
            updateSyncBadge(status);
            loadDashboardData();
        });
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!com.orderanalyzer.firebase.FirebaseShopAuthManager.getInstance(this).isLoggedIn()) {
            Intent login = new Intent(this, LoginActivity.class);
            login.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(login);
            finish();
            return;
        }
        setContentView(R.layout.activity_daily_product_orders);

        android.view.View root = findViewById(R.id.root_dashboard);
        if (root != null) {
            androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
                androidx.core.graphics.Insets bars = insets.getInsets(
                        androidx.core.view.WindowInsetsCompat.Type.systemBars());
                v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
                return insets;
            });
        }

        com.orderanalyzer.firebase.FirebaseSyncManager.getInstance(this).startSync();

        repository = DailyOrderRepository.getInstance(this);
        todayDate = DailyOrderRepository.getTodayDate();

        initViews();
        setupListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        com.orderanalyzer.firebase.FirebaseSyncManager.getInstance(this).addListener(syncListener);
        updateSyncBadge(com.orderanalyzer.firebase.FirebaseSyncManager.getInstance(this).getCurrentStatus());
        // Always refresh today's date on resume in case midnight passed
        todayDate = DailyOrderRepository.getTodayDate();
        updateDateUIAndLoadData();
    }

    @Override
    protected void onPause() {
        super.onPause();
        com.orderanalyzer.firebase.FirebaseSyncManager.getInstance(this).removeListener(syncListener);
    }

    private void updateSyncBadge(com.orderanalyzer.firebase.SyncStatus status) {
        if (tvSyncStatusBadge == null) return;
        tvSyncStatusBadge.setText("● " + status.getLabel());
        try {
            tvSyncStatusBadge.setTextColor(android.graphics.Color.parseColor(status.getTextColorHex()));
        } catch (Exception ignored) {}
    }

    private void initViews() {
        ImageButton btnBack = findViewById(R.id.btn_back);
        ImageButton btnHistory = findViewById(R.id.btn_history);
        ImageButton btnProfile = findViewById(R.id.btn_profile);
        btnBack.setOnClickListener(v -> finish());
        btnProfile.setOnClickListener(v -> startActivity(new Intent(this, ShopProfileActivity.class)));
        // Calendar button opens Order History & Reports screen (NOT a date-change feature)
        btnHistory.setOnClickListener(v -> startActivity(new Intent(this, OrderHistoryActivity.class)));

        tvSyncStatusBadge = findViewById(R.id.tv_sync_status_badge);
        if (tvSyncStatusBadge != null) {
            tvSyncStatusBadge.setOnClickListener(v -> startActivity(new Intent(this, ShopProfileActivity.class)));
        }

        tvDashboardDate = findViewById(R.id.tv_dashboard_date);
        tvDateChip = findViewById(R.id.tv_date_chip);

        tvHeroTag = findViewById(R.id.tv_hero_tag);
        tvHeroTitle = findViewById(R.id.tv_hero_title);
        tvHeroSubtitle = findViewById(R.id.tv_hero_subtitle);

        // 6 Functional Cards
        tvCountRetailerOrders = findViewById(R.id.tv_count_retailer_orders);
        tvCountProductTotals = findViewById(R.id.tv_count_product_totals);
        tvBillProductTotals = findViewById(R.id.tv_bill_product_totals);
        tvCountProducts = findViewById(R.id.tv_count_products);
        tvCountPendingOrders = findViewById(R.id.tv_count_pending_orders);
        tvCountRetailers = findViewById(R.id.tv_count_retailers);

        tvTakeOrdersSubtitle = findViewById(R.id.tv_take_orders_subtitle);
    }

    private void setupListeners() {
        // Card 1 — Retailer Orders (view today's taken orders)
        findViewById(R.id.card_qa_retailer_orders).setOnClickListener(v -> {
            Intent intent = new Intent(this, TodayRetailersActivity.class);
            intent.putExtra("order_date", todayDate);
            startActivity(intent);
        });

        // Card 2 — Total Orders (view today's product totals)
        findViewById(R.id.card_qa_product_totals).setOnClickListener(v -> {
            Intent intent = new Intent(this, ProductTotalsActivity.class);
            intent.putExtra("order_date", todayDate);
            startActivity(intent);
        });

        // Card 3 — Products
        findViewById(R.id.card_qa_products).setOnClickListener(v -> {
            startActivity(new Intent(this, ProductsManagementActivity.class));
        });

        // Card 4 — Pending Orders (strictly today's pending orders)
        findViewById(R.id.card_qa_pending_orders).setOnClickListener(v -> {
            Intent intent = new Intent(this, PendingOrdersActivity.class);
            startActivity(intent);
        });

        // Card 5 — Retailers
        findViewById(R.id.card_qa_retailers).setOnClickListener(v -> {
            startActivity(new Intent(this, RetailersManagementActivity.class));
        });

        // Card 6 — Reports / Useful Extra
        findViewById(R.id.card_qa_reports).setOnClickListener(v -> {
            startActivity(new Intent(this, OrderHistoryActivity.class));
        });

        // Prominent Full-Width "Take orders" Button Below 6 Cards
        findViewById(R.id.btn_take_orders).setOnClickListener(v -> {
            Intent intent = new Intent(this, AddRetailerOrderActivity.class);
            intent.putExtra("order_date", todayDate);
            // Prefer pending retailers: pass the first pending retailer ID if available
            List<DailyRetailer> pending = repository.getPendingRetailersForDate(todayDate);
            if (!pending.isEmpty()) {
                intent.putExtra("retailer_id", pending.get(0).getId());
            }
            startActivity(intent);
        });
    }

    private void updateDateUIAndLoadData() {
        tvDashboardDate.setText(DailyOrderRepository.formatDisplayDate(todayDate));
        tvDateChip.setText("Today");

        tvHeroTag.setText("TODAY");
        tvHeroTitle.setText("Let's take\ntoday's orders");

        tvTakeOrdersSubtitle.setText("Select a retailer and add today's orders");

        loadDashboardData();
    }

    private void loadDashboardData() {
        DashboardMetrics metrics = repository.getDashboardMetrics(todayDate);

        // Card 1: Retailer Orders count (orders taken today)
        tvCountRetailerOrders.setText(String.valueOf(metrics.getRetailersCount()));

        // Card 2: Total Orders count (total items ordered today) & Bill amount
        tvCountProductTotals.setText(String.valueOf(metrics.getTotalItems()));
        double bill = metrics.getTotalBillAmount();
        tvBillProductTotals.setText(String.format(Locale.US, "₹%.2f", bill));

        if (metrics.getTotalItems() > 0) {
            tvHeroSubtitle.setText(String.format(Locale.US, "Today: %d items • ₹%.2f total bill", metrics.getTotalItems(), bill));
        } else {
            tvHeroSubtitle.setText("Add retailer orders and track product totals");
        }

        // Card 3: Products count (total active products in catalog)
        int activeProductsCount = repository.getProducts(true).size();
        tvCountProducts.setText(String.valueOf(activeProductsCount));

        // Card 4: Pending Orders count (retailers with NO order for today)
        tvCountPendingOrders.setText(String.valueOf(metrics.getPendingRetailersCount()));

        // Card 5: Retailers count (total registered active retailers)
        int activeRetailersCount = repository.getRetailers(true).size();
        tvCountRetailers.setText(String.valueOf(activeRetailersCount));
    }
}
