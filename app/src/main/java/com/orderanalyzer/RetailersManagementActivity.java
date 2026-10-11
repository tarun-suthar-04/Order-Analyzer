package com.orderanalyzer;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.orderanalyzer.adapter.ManageRetailersAdapter;
import com.orderanalyzer.db.DailyOrderRepository;
import com.orderanalyzer.model.DailyRetailer;

import java.util.List;

public class RetailersManagementActivity extends AppCompatActivity {

    private DailyOrderRepository repository;
    private TextView tvRetailersCountBadge;
    private ManageRetailersAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_retailers_management);

        repository = DailyOrderRepository.getInstance(this);

        initViews();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadRetailers();
    }

    private void initViews() {
        ImageButton btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        tvRetailersCountBadge = findViewById(R.id.tv_retailers_count_badge);

        findViewById(R.id.btn_add_retailer).setOnClickListener(v -> {
            startActivity(new Intent(this, AddRetailerActivity.class));
        });

        RecyclerView rvRetailers = findViewById(R.id.rv_manage_retailers);
        rvRetailers.setLayoutManager(new LinearLayoutManager(this));

        adapter = new ManageRetailersAdapter(new ManageRetailersAdapter.OnRetailerActionCallback() {
            @Override
            public void onToggleActive(DailyRetailer retailer, boolean active) {
                repository.setRetailerActive(retailer.getId(), active);
                loadRetailers();
            }

            @Override
            public void onEdit(DailyRetailer retailer) {
                Intent intent = new Intent(RetailersManagementActivity.this, AddRetailerActivity.class);
                intent.putExtra("retailer_id", retailer.getId());
                startActivity(intent);
            }

            @Override
            public void onDelete(DailyRetailer retailer) {
                confirmDeleteRetailer(retailer);
            }
        });
        rvRetailers.setAdapter(adapter);
    }

    private void loadRetailers() {
        List<DailyRetailer> retailers = repository.getRetailers(false);
        tvRetailersCountBadge.setText(retailers.size() + " retailers");
        adapter.setData(retailers);
    }

    private void confirmDeleteRetailer(DailyRetailer retailer) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Retailer")
                .setMessage("Are you sure you want to delete \"" + retailer.getName() + "\"? Any orders for this retailer will also be deleted.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    repository.deleteRetailer(retailer.getId());
                    Toast.makeText(this, "Retailer deleted", Toast.LENGTH_SHORT).show();
                    loadRetailers();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
