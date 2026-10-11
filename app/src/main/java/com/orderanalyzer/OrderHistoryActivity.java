package com.orderanalyzer;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.orderanalyzer.adapter.OrderHistoryAdapter;
import com.orderanalyzer.db.DailyOrderRepository;
import com.orderanalyzer.model.DashboardMetrics;

import java.util.ArrayList;
import java.util.List;

public class OrderHistoryActivity extends AppCompatActivity {

    private DailyOrderRepository repository;
    private RecyclerView rvHistoryDays;
    private OrderHistoryAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_history);

        repository = DailyOrderRepository.getInstance(this);

        initViews();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadHistoryData();
    }

    private void initViews() {
        ImageButton btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        rvHistoryDays = findViewById(R.id.rv_history_days);
        rvHistoryDays.setLayoutManager(new LinearLayoutManager(this));

        adapter = new OrderHistoryAdapter(new OrderHistoryAdapter.OnHistoryActionListener() {
            @Override
            public void onViewOrders(String date) {
                Intent intent = new Intent(OrderHistoryActivity.this, TodayRetailersActivity.class);
                intent.putExtra("order_date", date);
                startActivity(intent);
            }

            @Override
            public void onViewTotals(String date) {
                Intent intent = new Intent(OrderHistoryActivity.this, ProductTotalsActivity.class);
                intent.putExtra("order_date", date);
                startActivity(intent);
            }
        });
        rvHistoryDays.setAdapter(adapter);
    }

    private void loadHistoryData() {
        List<String> dates = DailyOrderRepository.getLastNDates(DailyOrderRepository.ORDER_RETENTION_DAYS);
        List<OrderHistoryAdapter.DayItem> items = new ArrayList<>();

        for (String d : dates) {
            String relLabel = DailyOrderRepository.getRelativeLabel(d);
            String displayDate = DailyOrderRepository.formatDisplayDate(d);
            DashboardMetrics metrics = repository.getDashboardMetrics(d);

            items.add(new OrderHistoryAdapter.DayItem(
                    d,
                    relLabel,
                    displayDate,
                    metrics.getRetailersCount(),
                    metrics.getTotalItems(),
                    metrics.getTotalBillAmount()
            ));
        }

        adapter.setData(items);
    }
}
