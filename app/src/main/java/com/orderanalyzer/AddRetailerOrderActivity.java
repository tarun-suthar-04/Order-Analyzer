package com.orderanalyzer;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.orderanalyzer.adapter.OrderEntryProductAdapter;
import com.orderanalyzer.db.DailyOrderRepository;
import com.orderanalyzer.model.DailyOrder;
import com.orderanalyzer.model.DailyOrderItem;
import com.orderanalyzer.model.DailyProduct;
import com.orderanalyzer.model.DailyRetailer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AddRetailerOrderActivity extends AppCompatActivity {

    private DailyOrderRepository repository;
    private String orderDate;
    private long editingOrderId = -1;
    private long preselectedRetailerId = -1;

    private Spinner spinnerRetailers;
    private TextView tvTotalUnits, tvScreenTitle, tvScreenSubtitle;
    private EditText etOrderNotes;
    private OrderEntryProductAdapter productAdapter;

    private List<DailyRetailer> activeRetailers = new ArrayList<>();
    private List<DailyProduct> allActiveProducts = new ArrayList<>();
    private DailyRetailer selectedRetailer = null;

    private final ActivityResultLauncher<Intent> addRetailerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    long newRetailerId = result.getData().getLongExtra("new_retailer_id", -1);
                    loadRetailers(newRetailerId);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_retailer_order);

        repository = DailyOrderRepository.getInstance(this);

        orderDate = getIntent().getStringExtra("order_date");
        if (orderDate == null) orderDate = DailyOrderRepository.getTodayDate();

        editingOrderId = getIntent().getLongExtra("order_id", -1);
        preselectedRetailerId = getIntent().getLongExtra("retailer_id", -1);

        initViews();
        loadData();
    }

    private void initViews() {
        ImageButton btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        tvScreenTitle = findViewById(R.id.tv_screen_title);
        tvScreenSubtitle = findViewById(R.id.tv_screen_subtitle);
        spinnerRetailers = findViewById(R.id.spinner_retailers);
        tvTotalUnits = findViewById(R.id.tv_total_units);
        etOrderNotes = findViewById(R.id.et_order_notes);

        // Add new retailer shortcut
        findViewById(R.id.btn_add_new_retailer).setOnClickListener(v -> {
            Intent intent = new Intent(this, AddRetailerActivity.class);
            addRetailerLauncher.launch(intent);
        });

        // Lock retailer selection in edit mode (must be AFTER the listener is attached)
        if (editingOrderId != -1) {
            tvScreenTitle.setText("Edit Retailer Order");
            tvScreenSubtitle.setText("Update order quantities for this retailer");
            // An order is uniquely identified by its date and retailer. Keeping the
            // retailer fixed while editing prevents this screen from accidentally
            // creating a second order and leaving the original one unchanged.
            spinnerRetailers.setEnabled(false);
            findViewById(R.id.btn_add_new_retailer).setVisibility(View.GONE);
        }

        RecyclerView rvProducts = findViewById(R.id.rv_entry_products);
        rvProducts.setLayoutManager(new LinearLayoutManager(this));
        productAdapter = new OrderEntryProductAdapter(this);
        productAdapter.setListener((quantities, totalUnits) -> tvTotalUnits.setText(String.valueOf(totalUnits)));
        rvProducts.setAdapter(productAdapter);

        findViewById(R.id.btn_save_order).setOnClickListener(v -> saveOrder());
    }

    private void loadData() {
        allActiveProducts = repository.getProducts(true);

        if (editingOrderId != -1) {
            // In edit mode: fetch the existing order first to get the correct retailer ID,
            // then call loadRetailers() exactly ONCE with the right selection.
            DailyOrder order = repository.getOrderById(editingOrderId);
            if (order != null) {
                if (!order.getOrderDate().equals(DailyOrderRepository.getTodayDate())) {
                    Toast.makeText(this, "Yesterday's orders cannot be edited", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }
                preselectedRetailerId = order.getRetailerId();
                if (order.getNotes() != null) {
                    etOrderNotes.setText(order.getNotes());
                }
            }
        }
        // Single call — covers both new-order and edit-order paths
        loadRetailers(preselectedRetailerId);
    }

    private void loadRetailers(long retailerToSelectId) {
        activeRetailers = repository.getRetailers(true);

        // Find which retailers are pending for orderDate so we can sort them first!
        List<DailyRetailer> pending = repository.getPendingRetailersForDate(orderDate);
        Set<Long> pendingIds = new HashSet<>();
        for (DailyRetailer pr : pending) {
            pendingIds.add(pr.getId());
        }

        // Sort: pending retailers first, then alphabetical by name
        Collections.sort(activeRetailers, (r1, r2) -> {
            boolean p1 = pendingIds.contains(r1.getId());
            boolean p2 = pendingIds.contains(r2.getId());
            if (p1 && !p2) return -1;
            if (!p1 && p2) return 1;
            return r1.getName().compareToIgnoreCase(r2.getName());
        });

        List<String> names = new ArrayList<>();
        int selectedIndex = 0;

        for (int i = 0; i < activeRetailers.size(); i++) {
            DailyRetailer r = activeRetailers.get(i);
            boolean isPending = pendingIds.contains(r.getId());
            names.add(r.getName() + (isPending ? " (Pending)" : " ✓"));
            if (r.getId() == retailerToSelectId) {
                selectedIndex = i;
            }
        }

        if (names.isEmpty()) {
            names.add("No active retailers (Add new)");
        }

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, names);
        spinnerRetailers.setAdapter(spinnerAdapter);

        if (!activeRetailers.isEmpty()) {
            spinnerRetailers.setSelection(selectedIndex);
            selectedRetailer = activeRetailers.get(selectedIndex);
            bindProductsForSelectedRetailer();
        }

        spinnerRetailers.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0 && position < activeRetailers.size()) {
                    selectedRetailer = activeRetailers.get(position);
                    bindProductsForSelectedRetailer();
                }
            }

            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void bindProductsForSelectedRetailer() {
        if (selectedRetailer == null) return;

        List<Long> prefIds = selectedRetailer.getPreferredProductIdList();
        // Sort products: Preferred products first, then normal order
        List<DailyProduct> sortedProducts = new ArrayList<>(allActiveProducts);
        Collections.sort(sortedProducts, (p1, p2) -> {
            boolean pref1 = prefIds.contains(p1.getId());
            boolean pref2 = prefIds.contains(p2.getId());
            if (pref1 && !pref2) return -1;
            if (!pref1 && pref2) return 1;
            return Integer.compare(p1.getSortOrder(), p2.getSortOrder());
        });

        // Load any existing quantities for this retailer on this date
        Map<Long, Integer> initialQuantities = new HashMap<>();
        DailyOrder existingOrder = repository.getOrderForRetailer(orderDate, selectedRetailer.getId());
        if (existingOrder != null) {
            if (etOrderNotes.getText().toString().trim().isEmpty() && existingOrder.getNotes() != null) {
                etOrderNotes.setText(existingOrder.getNotes());
            }
            for (DailyOrderItem item : existingOrder.getItems()) {
                initialQuantities.put(item.getProductId(), item.getQuantity());
            }
        }

        productAdapter.setData(sortedProducts, prefIds, initialQuantities);
    }

    private void saveOrder() {
        if (selectedRetailer == null) {
            Toast.makeText(this, "Please select a retailer", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<Long, Integer> quantities = productAdapter.getQuantities();
        int totalUnits = productAdapter.getTotalUnits();

        if (totalUnits == 0) {
            Toast.makeText(this, "Order cannot be 0 units", Toast.LENGTH_SHORT).show();
            return;
        }

        String notes = etOrderNotes.getText().toString().trim();
        repository.saveOrder(orderDate, selectedRetailer.getId(), notes, quantities);

        Toast.makeText(this, "Order saved successfully!", Toast.LENGTH_SHORT).show();
        finish();
    }
}
