package com.orderanalyzer;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.orderanalyzer.adapter.PreferredProductSelectAdapter;
import com.orderanalyzer.db.DailyOrderRepository;
import com.orderanalyzer.model.DailyProduct;
import com.orderanalyzer.model.DailyRetailer;

import java.util.List;

public class AddRetailerActivity extends AppCompatActivity {

    private DailyOrderRepository repository;
    private long editingRetailerId = -1;

    private EditText etRetailerName, etRetailerPhone, etRetailerAddress;
    private PreferredProductSelectAdapter productSelectAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_retailer);

        repository = DailyOrderRepository.getInstance(this);
        editingRetailerId = getIntent().getLongExtra("retailer_id", -1);

        initViews();
        loadData();
    }

    private void initViews() {
        ImageButton btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        TextView tvTitle = findViewById(R.id.tv_title);
        if (editingRetailerId != -1) {
            tvTitle.setText("Edit Retailer");
        }

        etRetailerName = findViewById(R.id.et_retailer_name);
        etRetailerPhone = findViewById(R.id.et_retailer_phone);
        etRetailerAddress = findViewById(R.id.et_retailer_address);

        RecyclerView rvPreferred = findViewById(R.id.rv_preferred_products);
        rvPreferred.setLayoutManager(new LinearLayoutManager(this));
        productSelectAdapter = new PreferredProductSelectAdapter();
        rvPreferred.setAdapter(productSelectAdapter);



        findViewById(R.id.btn_cancel).setOnClickListener(v -> finish());
        findViewById(R.id.btn_save_retailer).setOnClickListener(v -> saveRetailer());
    }

    private void loadData() {
        List<DailyProduct> products = repository.getProducts(true);
        List<Long> selectedIds = null;

        if (editingRetailerId != -1) {
            DailyRetailer r = repository.getRetailerById(editingRetailerId);
            if (r != null) {
                etRetailerName.setText(r.getName());
                etRetailerPhone.setText(r.getPhone());
                etRetailerAddress.setText(r.getAddress());
                selectedIds = r.getPreferredProductIdList();
            }
        }
        productSelectAdapter.setData(products, selectedIds);
    }

    private void saveRetailer() {
        String name = etRetailerName.getText().toString().trim();
        if (name.isEmpty()) {
            etRetailerName.setError("Retailer name is required");
            etRetailerName.requestFocus();
            return;
        }

        String phone = etRetailerPhone.getText().toString().trim();
        String address = etRetailerAddress.getText().toString().trim();

        List<Long> selected = productSelectAdapter.getSelectedIds();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < selected.size(); i++) {
            sb.append(selected.get(i));
            if (i < selected.size() - 1) sb.append(",");
        }
        String preferredIds = sb.toString();

        long retailerId;
        if (editingRetailerId != -1) {
            repository.updateRetailer(editingRetailerId, name, phone, address, preferredIds, true);
            retailerId = editingRetailerId;
            Toast.makeText(this, "Retailer updated", Toast.LENGTH_SHORT).show();
        } else {
            retailerId = repository.addRetailer(name, phone, address, preferredIds);
            Toast.makeText(this, "Retailer added", Toast.LENGTH_SHORT).show();
        }

        Intent resultData = new Intent();
        resultData.putExtra("new_retailer_id", retailerId);
        setResult(RESULT_OK, resultData);
        finish();
    }
}
