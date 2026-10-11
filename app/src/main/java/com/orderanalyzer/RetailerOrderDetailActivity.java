package com.orderanalyzer;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.orderanalyzer.adapter.OrderDetailProductsAdapter;
import com.orderanalyzer.db.DailyOrderRepository;
import com.orderanalyzer.model.DailyOrder;
import com.orderanalyzer.model.DailyOrderItem;

import java.util.Locale;

public class RetailerOrderDetailActivity extends AppCompatActivity {

    private DailyOrderRepository repository;
    private long orderId = -1;
    private DailyOrder currentOrder;

    private TextView tvHeaderAvatar, tvHeaderName;
    private TextView tvDetailAvatar, tvDetailRetailerName, tvDetailPhone, tvDetailAddress;
    private TextView tvDetailDate, tvDetailItemsPill;
    private TextView tvDetailGrandTotal, tvDetailTotalBill, tvDetailProductFraction;
    private RecyclerView rvDetailProducts;
    private OrderDetailProductsAdapter productsAdapter;

    private Button btnShareOrder, btnCopyOrder, btnEditOrder;
    private ImageButton btnDeleteOrder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_retailer_order_detail);

        View root = findViewById(R.id.root_retailer_detail);
        View layoutBottomButtons = findViewById(R.id.layout_bottom_buttons);
        NestedScrollView scrollView = findViewById(R.id.scroll_order_detail);

        if (root != null) {
            ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
                Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(bars.left, bars.top, bars.right, 0);
                if (layoutBottomButtons != null) {
                    layoutBottomButtons.setPadding(
                            dpToPx(16),
                            dpToPx(12),
                            dpToPx(16),
                            dpToPx(12) + bars.bottom
                    );
                }
                if (scrollView != null) {
                    scrollView.setPadding(0, 0, 0, dpToPx(120) + bars.bottom);
                }
                return insets;
            });
        }

        repository = DailyOrderRepository.getInstance(this);
        orderId = getIntent().getLongExtra("order_id", -1);

        initViews();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadOrderDetail();
    }

    private void initViews() {
        ImageButton btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        btnDeleteOrder = findViewById(R.id.btn_delete_order);
        btnDeleteOrder.setOnClickListener(v -> confirmDelete());

        tvHeaderAvatar = findViewById(R.id.tv_header_avatar);
        tvHeaderName = findViewById(R.id.tv_header_name);

        tvDetailAvatar = findViewById(R.id.tv_detail_avatar);
        tvDetailRetailerName = findViewById(R.id.tv_detail_retailer_name);
        tvDetailPhone = findViewById(R.id.tv_detail_phone);
        tvDetailAddress = findViewById(R.id.tv_detail_address);

        tvDetailDate = findViewById(R.id.tv_detail_date);
        tvDetailItemsPill = findViewById(R.id.tv_detail_items_pill);
        tvDetailGrandTotal = findViewById(R.id.tv_detail_grand_total);
        tvDetailTotalBill = findViewById(R.id.tv_detail_total_bill);
        tvDetailProductFraction = findViewById(R.id.tv_detail_product_fraction);

        rvDetailProducts = findViewById(R.id.rv_detail_products);
        rvDetailProducts.setLayoutManager(new LinearLayoutManager(this));
        productsAdapter = new OrderDetailProductsAdapter();
        rvDetailProducts.setAdapter(productsAdapter);

        btnShareOrder = findViewById(R.id.btn_share_order);
        btnCopyOrder = findViewById(R.id.btn_copy_order);
        btnEditOrder = findViewById(R.id.btn_edit_order);

        btnShareOrder.setOnClickListener(v -> shareOrder());
        btnCopyOrder.setOnClickListener(v -> copyOrderToClipboard());
        btnEditOrder.setOnClickListener(v -> {
            if (currentOrder != null) {
                Intent intent = new Intent(this, AddRetailerOrderActivity.class);
                intent.putExtra("order_id", currentOrder.getId());
                intent.putExtra("order_date", currentOrder.getOrderDate());
                startActivity(intent);
            }
        });
    }

    private void loadOrderDetail() {
        currentOrder = repository.getOrderById(orderId);
        if (currentOrder == null) {
            finish();
            return;
        }

        String name = currentOrder.getRetailerName();
        String initial = (name != null && !name.trim().isEmpty()) ? name.trim().substring(0, 1).toUpperCase() : "R";

        tvHeaderAvatar.setText(initial);
        tvHeaderName.setText(name);

        tvDetailAvatar.setText(initial);
        tvDetailRetailerName.setText(name);
        tvDetailPhone.setText(currentOrder.getRetailerPhone() != null && !currentOrder.getRetailerPhone().isEmpty()
                ? currentOrder.getRetailerPhone() : "No phone");
        tvDetailAddress.setText(currentOrder.getRetailerAddress() != null && !currentOrder.getRetailerAddress().isEmpty()
                ? currentOrder.getRetailerAddress() : "No address");

        tvDetailDate.setText(DailyOrderRepository.formatDisplayDate(currentOrder.getOrderDate()));

        int totalUnits = currentOrder.getTotalUnits();
        String unitsLabel = totalUnits + (totalUnits == 1 ? " unit" : " units");
        tvDetailItemsPill.setText(unitsLabel);
        tvDetailGrandTotal.setText("Total Quantity: " + unitsLabel);

        double totalBill = 0;
        for (DailyOrderItem item : currentOrder.getItems()) {
            totalBill += item.getLineAmount();
        }
        tvDetailTotalBill.setText(String.format(Locale.US, "₹%.2f", totalBill));

        int orderedCount = currentOrder.getDistinctProductsCount();
        int totalProducts = repository.getProducts(true).size();
        tvDetailProductFraction.setText(orderedCount + " / " + totalProducts);

        boolean isToday = currentOrder.getOrderDate() != null
                && currentOrder.getOrderDate().equals(DailyOrderRepository.getTodayDate());

        // Delete order: only permitted for today's order
        btnDeleteOrder.setVisibility(isToday ? View.VISIBLE : View.GONE);

        // Bottom actions: Share and Copy are always available
        btnShareOrder.setVisibility(View.VISIBLE);
        btnCopyOrder.setVisibility(View.VISIBLE);

        if (isToday) {
            // Today's order: 3 equal-width buttons [ SHARE ] [ COPY ORDER ] [ EDIT ORDER ]
            btnEditOrder.setVisibility(View.VISIBLE);
            btnCopyOrder.setBackgroundResource(R.drawable.bg_btn_outline);
            btnCopyOrder.setTextColor(ContextCompat.getColor(this, R.color.brand_blue));

            LinearLayout.LayoutParams copyLp = (LinearLayout.LayoutParams) btnCopyOrder.getLayoutParams();
            copyLp.setMarginStart(dpToPx(4));
            copyLp.setMarginEnd(dpToPx(4));
            btnCopyOrder.setLayoutParams(copyLp);
        } else {
            // Historical order: 2 equal-width buttons [ SHARE ] [ COPY ORDER ], Edit hidden
            btnEditOrder.setVisibility(View.GONE);
            btnCopyOrder.setBackgroundResource(R.drawable.bg_btn_primary);
            btnCopyOrder.setTextColor(ContextCompat.getColor(this, R.color.white));

            LinearLayout.LayoutParams copyLp = (LinearLayout.LayoutParams) btnCopyOrder.getLayoutParams();
            copyLp.setMarginStart(dpToPx(4));
            copyLp.setMarginEnd(0);
            btnCopyOrder.setLayoutParams(copyLp);
        }

        productsAdapter.setData(currentOrder.getItems());
    }

    private void confirmDelete() {
        if (currentOrder == null) return;
        new AlertDialog.Builder(this)
                .setTitle("Delete Order")
                .setMessage("Delete order for " + currentOrder.getRetailerName() + "?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    repository.deleteOrder(currentOrder.getId());
                    Toast.makeText(this, "Order deleted", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private String buildOrderSummaryText() {
        if (currentOrder == null) return "";
        StringBuilder sb = new StringBuilder();
        sb.append(currentOrder.getRetailerName()).append("\n");
        sb.append(DailyOrderRepository.formatDisplayDate(currentOrder.getOrderDate())).append("\n\n");

        double totalBill = 0;
        for (DailyOrderItem item : currentOrder.getItems()) {
            if (item.getQuantity() > 0) {
                totalBill += item.getLineAmount();
                sb.append(item.getProductName()).append("\n");
                if (item.getUnitPrice() > 0) {
                    sb.append(item.getQuantity())
                            .append(" × ₹")
                            .append(String.format(Locale.US, "%.2f", item.getUnitPrice()))
                            .append("\n₹")
                            .append(String.format(Locale.US, "%.2f", item.getLineAmount()))
                            .append("\n\n");
                } else {
                    sb.append(item.getQuantity())
                            .append(item.getQuantity() == 1 ? " unit" : " units")
                            .append("\n\n");
                }
            }
        }

        int totalUnits = currentOrder.getTotalUnits();
        sb.append("Total Quantity\n").append(totalUnits).append(totalUnits == 1 ? " unit" : " units").append("\n\n");
        sb.append(String.format(Locale.US, "Grand Total\n₹%.2f", totalBill));

        return sb.toString().trim();
    }

    private void shareOrder() {
        if (currentOrder == null) return;
        String summary = buildOrderSummaryText();
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Order Details - " + currentOrder.getRetailerName());
        shareIntent.putExtra(Intent.EXTRA_TEXT, summary);
        startActivity(Intent.createChooser(shareIntent, "Share Order"));
    }

    private void copyOrderToClipboard() {
        if (currentOrder == null) return;
        String summary = buildOrderSummaryText();
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Retailer Order", summary);
        cm.setPrimaryClip(clip);
        Toast.makeText(this, "Order summary copied to clipboard", Toast.LENGTH_SHORT).show();
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}
