package com.orderanalyzer;

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

import com.orderanalyzer.adapter.TodayRetailersAdapter;
import com.orderanalyzer.db.DailyOrderRepository;
import com.orderanalyzer.model.DailyOrder;

import java.util.ArrayList;
import java.util.List;

public class TodayRetailersActivity extends AppCompatActivity {

    private DailyOrderRepository repository;
    private String selectedDate;

    private TextView tvRetailerCount, tvDatePill;
    private RecyclerView rvTodayRetailers;
    private TextView tvEmptyRetailers;
    private TodayRetailersAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_today_retailers);

        repository = DailyOrderRepository.getInstance(this);
        selectedDate = getIntent().getStringExtra("order_date");
        if (selectedDate == null) selectedDate = DailyOrderRepository.getTodayDate();

        initViews();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadOrders();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        String date = intent.getStringExtra("order_date");
        if (date != null) {
            selectedDate = date;
        }
        loadOrders();
    }

    private void initViews() {
        ImageButton btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        ImageButton btnAdd = findViewById(R.id.btn_add_order);
        btnAdd.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddRetailerOrderActivity.class);
            intent.putExtra("order_date", selectedDate);
            startActivity(intent);
        });

        tvRetailerCount = findViewById(R.id.tv_retailer_count);
        tvDatePill = findViewById(R.id.tv_date_pill);
        rvTodayRetailers = findViewById(R.id.rv_today_retailers);
        tvEmptyRetailers = findViewById(R.id.tv_empty_retailers);

        rvTodayRetailers.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TodayRetailersAdapter(new TodayRetailersAdapter.OnItemActionCallback() {
            @Override
            public void onItemClick(DailyOrder order) {
                Intent intent = new Intent(TodayRetailersActivity.this, RetailerOrderDetailActivity.class);
                intent.putExtra("order_id", order.getId());
                startActivity(intent);
            }

            @Override
            public void onEdit(DailyOrder order) {
                if (!order.getOrderDate().equals(DailyOrderRepository.getTodayDate())) {
                    Toast.makeText(TodayRetailersActivity.this, "Only today's orders can be edited", Toast.LENGTH_SHORT).show();
                    return;
                }
                Intent intent = new Intent(TodayRetailersActivity.this, AddRetailerOrderActivity.class);
                intent.putExtra("order_id", order.getId());
                intent.putExtra("order_date", order.getOrderDate());
                startActivity(intent);
            }

            @Override
            public void onDelete(DailyOrder order) {
                if (!order.getOrderDate().equals(DailyOrderRepository.getTodayDate())) {
                    Toast.makeText(TodayRetailersActivity.this, "Only today's orders can be deleted", Toast.LENGTH_SHORT).show();
                    return;
                }
                confirmDeleteOrder(order);
            }
        });
        rvTodayRetailers.setAdapter(adapter);

        View.OnClickListener dateClickListener = v -> showDateSelectionDialog();
        View cardDateSelect = findViewById(R.id.card_date_select);
        if (cardDateSelect != null) cardDateSelect.setOnClickListener(dateClickListener);
        tvDatePill.setOnClickListener(dateClickListener);
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
                    loadOrders();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void loadOrders() {
        boolean isToday = selectedDate.equals(DailyOrderRepository.getTodayDate());
        String relativeLabel = DailyOrderRepository.getRelativeLabel(selectedDate);
        tvDatePill.setText(relativeLabel + " ▼");

        findViewById(R.id.btn_add_order).setVisibility(isToday ? View.VISIBLE : View.GONE);

        List<DailyOrder> orders = repository.getOrdersForDate(selectedDate);
        tvRetailerCount.setText(orders.size() + " retailer" + (orders.size() == 1 ? "" : "s"));

        if (orders.isEmpty()) {
            rvTodayRetailers.setVisibility(View.GONE);
            tvEmptyRetailers.setVisibility(View.VISIBLE);
            tvEmptyRetailers.setText("No retailer orders recorded for " + relativeLabel + ".");
            adapter.setData(new ArrayList<>());
        } else {
            rvTodayRetailers.setVisibility(View.VISIBLE);
            tvEmptyRetailers.setVisibility(View.GONE);
            adapter.setData(orders);
        }
    }

    private void confirmDeleteOrder(DailyOrder order) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Order")
                .setMessage("Are you sure you want to delete the order for " + order.getRetailerName() + "?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    repository.deleteOrder(order.getId());
                    Toast.makeText(this, "Order deleted", Toast.LENGTH_SHORT).show();
                    loadOrders();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
