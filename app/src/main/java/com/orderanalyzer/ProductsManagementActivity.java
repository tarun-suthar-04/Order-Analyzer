package com.orderanalyzer;

import android.os.Bundle;
import android.text.InputType;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.orderanalyzer.adapter.ManageProductsAdapter;
import com.orderanalyzer.db.DailyOrderRepository;
import com.orderanalyzer.model.DailyProduct;

import java.util.List;
import java.util.Locale;

public class ProductsManagementActivity extends AppCompatActivity {

    private DailyOrderRepository repository;
    private TextView tvProductCountBadge;
    private ManageProductsAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_products_management);

        repository = DailyOrderRepository.getInstance(this);

        initViews();
        loadProducts();
    }

    private void initViews() {
        ImageButton btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        tvProductCountBadge = findViewById(R.id.tv_product_count_badge);

        findViewById(R.id.btn_add_product).setOnClickListener(v -> showAddProductDialog());

        RecyclerView rvProducts = findViewById(R.id.rv_manage_products);
        rvProducts.setLayoutManager(new LinearLayoutManager(this));

        adapter = new ManageProductsAdapter(new ManageProductsAdapter.OnProductActionCallback() {
            @Override
            public void onToggleActive(DailyProduct product, boolean active) {
                repository.setProductActive(product.getId(), active);
                loadProducts();
            }

            @Override
            public void onEdit(DailyProduct product) {
                showEditProductDialog(product);
            }

            @Override
            public void onDelete(DailyProduct product) {
                confirmDeleteProduct(product);
            }
        });
        rvProducts.setAdapter(adapter);
    }

    private void loadProducts() {
        List<DailyProduct> products = repository.getProducts(false);
        tvProductCountBadge.setText(products.size() + " products");
        adapter.setData(products);
    }

    private void confirmDeleteProduct(DailyProduct product) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Product")
                .setMessage("Are you sure you want to delete \"" + product.getName() + "\"? This will permanently remove the product.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    repository.deleteProduct(product.getId());
                    Toast.makeText(this, "Product deleted", Toast.LENGTH_SHORT).show();
                    loadProducts();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showAddProductDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20);

        TextView tvNameLabel = new TextView(this);
        tvNameLabel.setText("Product Name");
        tvNameLabel.setTextSize(12);
        tvNameLabel.setTextColor(getResources().getColor(R.color.text_secondary));
        layout.addView(tvNameLabel);

        final EditText etName = new EditText(this);
        etName.setHint("e.g. Maruti 500 Gram");
        layout.addView(etName);

        TextView tvPriceLabel = new TextView(this);
        tvPriceLabel.setText("Price (₹ per unit)");
        tvPriceLabel.setTextSize(12);
        tvPriceLabel.setTextColor(getResources().getColor(R.color.text_secondary));
        tvPriceLabel.setPadding(0, 24, 0, 0);
        layout.addView(tvPriceLabel);

        final EditText etPrice = new EditText(this);
        etPrice.setHint("0.00");
        etPrice.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        layout.addView(etPrice);

        new AlertDialog.Builder(this)
                .setTitle("Add New Product")
                .setView(layout)
                .setPositiveButton("Add", (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    if (!name.isEmpty()) {
                        long newId = repository.addProduct(name);
                        String priceStr = etPrice.getText().toString().trim();
                        if (!priceStr.isEmpty()) {
                            try {
                                double price = Double.parseDouble(priceStr);
                                repository.updateProductPrice(newId, price);
                            } catch (NumberFormatException ignored) {}
                        }
                        Toast.makeText(this, "Product added", Toast.LENGTH_SHORT).show();
                        loadProducts();
                    } else {
                        Toast.makeText(this, "Product name cannot be empty", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showEditProductDialog(DailyProduct product) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20);

        TextView tvNameLabel = new TextView(this);
        tvNameLabel.setText("Product Name");
        tvNameLabel.setTextSize(12);
        tvNameLabel.setTextColor(getResources().getColor(R.color.text_secondary));
        layout.addView(tvNameLabel);

        final EditText etName = new EditText(this);
        etName.setText(product.getName());
        layout.addView(etName);

        TextView tvPriceLabel = new TextView(this);
        tvPriceLabel.setText("Price (₹ per unit)");
        tvPriceLabel.setTextSize(12);
        tvPriceLabel.setTextColor(getResources().getColor(R.color.text_secondary));
        tvPriceLabel.setPadding(0, 24, 0, 0);
        layout.addView(tvPriceLabel);

        final EditText etPrice = new EditText(this);
        if (product.getPrice() > 0) {
            etPrice.setText(String.format(Locale.US, "%.2f", product.getPrice()));
        } else {
            etPrice.setHint("0.00");
        }
        etPrice.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        layout.addView(etPrice);

        new AlertDialog.Builder(this)
                .setTitle("Edit Product")
                .setView(layout)
                .setPositiveButton("Save", (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    if (!name.isEmpty()) {
                        repository.updateProductName(product.getId(), name);
                        String priceStr = etPrice.getText().toString().trim();
                        double price = 0.0;
                        if (!priceStr.isEmpty()) {
                            try {
                                price = Double.parseDouble(priceStr);
                            } catch (NumberFormatException ignored) {}
                        }
                        repository.updateProductPrice(product.getId(), price);
                        Toast.makeText(this, "Product updated", Toast.LENGTH_SHORT).show();
                        loadProducts();
                    } else {
                        Toast.makeText(this, "Product name cannot be empty", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
