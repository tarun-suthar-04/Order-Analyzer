package com.orderanalyzer;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.orderanalyzer.adapter.PendingRetailersAdapter;
import com.orderanalyzer.db.DailyOrderRepository;
import com.orderanalyzer.model.DailyRetailer;

import java.util.ArrayList;
import java.util.List;

public class PendingOrdersActivity extends AppCompatActivity {

    private DailyOrderRepository repository;
    private String todayDate;

    private TextView tvPendingSubtitle, tvDatePill, tvRetailerCount, tvEmptyPending;
    private RecyclerView rvPendingRetailers;
    private PendingRetailersAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pending_orders);

        repository = DailyOrderRepository.getInstance(this);
        todayDate = DailyOrderRepository.getTodayDate();

        initViews();
    }

    @Override
    protected void onResume() {
        super.onResume();
        todayDate = DailyOrderRepository.getTodayDate();
        loadPendingRetailers();
    }

    private void initViews() {
        ImageButton btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        tvPendingSubtitle = findViewById(R.id.tv_pending_subtitle);
        tvDatePill = findViewById(R.id.tv_date_pill);
        tvRetailerCount = findViewById(R.id.tv_retailer_count);
        tvEmptyPending = findViewById(R.id.tv_empty_pending);

        tvDatePill.setText("Today");
        tvPendingSubtitle.setText("Retailers with no order today");

        rvPendingRetailers = findViewById(R.id.rv_pending_retailers);
        rvPendingRetailers.setLayoutManager(new LinearLayoutManager(this));

        adapter = new PendingRetailersAdapter(retailer -> {
            Intent intent = new Intent(this, AddRetailerOrderActivity.class);
            intent.putExtra("order_date", todayDate);
            intent.putExtra("retailer_id", retailer.getId());
            startActivity(intent);
        });
        rvPendingRetailers.setAdapter(adapter);
    }

    private void loadPendingRetailers() {
        tvDatePill.setText("Today (" + DailyOrderRepository.formatDisplayDate(todayDate) + ")");

        List<DailyRetailer> pending = repository.getPendingRetailersForDate(todayDate);
        tvRetailerCount.setText(pending.size() + " pending");

        if (pending.isEmpty()) {
            rvPendingRetailers.setVisibility(View.GONE);
            tvEmptyPending.setVisibility(View.VISIBLE);
            tvEmptyPending.setText("All retailers have placed orders today! 🎉");
            adapter.setData(new ArrayList<>());
        } else {
            rvPendingRetailers.setVisibility(View.VISIBLE);
            tvEmptyPending.setVisibility(View.GONE);
            adapter.setData(pending);
        }
    }
}
