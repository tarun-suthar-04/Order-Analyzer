package com.orderanalyzer.adapter;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.orderanalyzer.R;
import com.orderanalyzer.model.DailyProduct;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class OrderEntryProductAdapter extends RecyclerView.Adapter<OrderEntryProductAdapter.ViewHolder>
        implements Filterable {

    public interface OnQuantityChangeListener {
        void onQuantityChanged(Map<Long, Integer> quantities, int totalUnits);
    }

    private final Context context;
    private List<DailyProduct> originalList = new ArrayList<>();
    private List<DailyProduct> displayList = new ArrayList<>();
    private final Map<Long, Integer> quantities = new HashMap<>();
    private List<Long> preferredProductIds = new ArrayList<>();
    private OnQuantityChangeListener listener;

    public OrderEntryProductAdapter(Context context) {
        this.context = context;
    }

    public void setListener(OnQuantityChangeListener listener) {
        this.listener = listener;
    }

    public void setData(List<DailyProduct> products, List<Long> preferredIds, Map<Long, Integer> initialQuantities) {
        this.originalList = new ArrayList<>(products);
        this.displayList = new ArrayList<>(products);
        this.preferredProductIds = preferredIds != null ? new ArrayList<>(preferredIds) : new ArrayList<>();
        this.quantities.clear();
        if (initialQuantities != null) {
            this.quantities.putAll(initialQuantities);
        }
        notifyDataSetChanged();
        notifyTotalChanged();
    }

    public Map<Long, Integer> getQuantities() {
        return quantities;
    }

    public int getTotalUnits() {
        int sum = 0;
        for (int q : quantities.values()) {
            sum += q;
        }
        return sum;
    }

    private void notifyTotalChanged() {
        if (listener != null) {
            listener.onQuantityChanged(quantities, getTotalUnits());
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_order_entry_product, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DailyProduct product = displayList.get(position);
        holder.bind(product);
    }

    @Override
    public int getItemCount() {
        return displayList.size();
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                String query = constraint != null ? constraint.toString().trim().toLowerCase(Locale.US) : "";
                List<DailyProduct> filtered = new ArrayList<>();
                if (query.isEmpty()) {
                    filtered.addAll(originalList);
                } else {
                    for (DailyProduct p : originalList) {
                        if (p.getName().toLowerCase(Locale.US).contains(query)) {
                            filtered.add(p);
                        }
                    }
                }
                FilterResults results = new FilterResults();
                results.values = filtered;
                results.count = filtered.size();
                return results;
            }

            @SuppressWarnings("unchecked")
            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                displayList = (List<DailyProduct>) results.values;
                notifyDataSetChanged();
            }
        };
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        View rootLayout;
        TextView tvIcon, tvName, tvPrice, tvPrefBadge;
        ImageButton btnMinus, btnPlus;
        EditText etQuantity;
        TextWatcher activeWatcher;

        ViewHolder(View itemView) {
            super(itemView);
            rootLayout = itemView.findViewById(R.id.layout_product_row);
            tvIcon = itemView.findViewById(R.id.tv_product_icon);
            tvName = itemView.findViewById(R.id.tv_product_name);
            tvPrice = itemView.findViewById(R.id.tv_product_price);
            tvPrefBadge = itemView.findViewById(R.id.tv_pref_badge);
            btnMinus = itemView.findViewById(R.id.btn_minus);
            btnPlus = itemView.findViewById(R.id.btn_plus);
            etQuantity = itemView.findViewById(R.id.et_quantity);
        }

        void bind(DailyProduct product) {
            tvName.setText(product.getName());
            tvIcon.setText(product.getDisplayEmoji());

            if (product.getPrice() > 0) {
                tvPrice.setText("₹" + String.format(Locale.US, "%.2f", product.getPrice()));
                tvPrice.setVisibility(View.VISIBLE);
            } else {
                tvPrice.setVisibility(View.GONE);
            }

            boolean isPreferred = preferredProductIds.contains(product.getId());
            tvPrefBadge.setVisibility(isPreferred ? View.VISIBLE : View.GONE);

            int currentQty = quantities.getOrDefault(product.getId(), 0);

            // Detach any previous watcher to prevent unwanted triggers during reuse
            if (activeWatcher != null) {
                etQuantity.removeTextChangedListener(activeWatcher);
            }

            etQuantity.setText(String.valueOf(currentQty));
            updateRowStyle(currentQty > 0);

            // Minus button
            btnMinus.setOnClickListener(v -> {
                int q = quantities.getOrDefault(product.getId(), 0);
                if (q > 0) {
                    q--;
                    quantities.put(product.getId(), q);
                    etQuantity.setText(String.valueOf(q));
                    updateRowStyle(q > 0);
                    notifyTotalChanged();
                }
            });

            // Plus button
            btnPlus.setOnClickListener(v -> {
                int q = quantities.getOrDefault(product.getId(), 0);
                q++;
                quantities.put(product.getId(), q);
                etQuantity.setText(String.valueOf(q));
                updateRowStyle(q > 0);
                notifyTotalChanged();
            });

            // Direct Typing Watcher
            activeWatcher = new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable s) {
                    String str = s != null ? s.toString().trim() : "";
                    int val = 0;
                    if (!str.isEmpty()) {
                        try {
                            val = Math.max(0, Integer.parseInt(str));
                        } catch (NumberFormatException ignored) {}
                    }
                    quantities.put(product.getId(), val);
                    updateRowStyle(val > 0);
                    notifyTotalChanged();
                }
            };
            etQuantity.addTextChangedListener(activeWatcher);
        }

        private void updateRowStyle(boolean hasQty) {
            if (hasQty) {
                rootLayout.setBackground(ContextCompat.getDrawable(context, R.drawable.bg_item_product_active));
            } else {
                rootLayout.setBackground(ContextCompat.getDrawable(context, R.drawable.bg_item_product_normal));
            }
        }
    }
}
