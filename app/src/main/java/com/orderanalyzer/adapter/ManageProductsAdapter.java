package com.orderanalyzer.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.orderanalyzer.R;
import com.orderanalyzer.model.DailyProduct;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ManageProductsAdapter extends RecyclerView.Adapter<ManageProductsAdapter.ViewHolder>
        implements Filterable {

    public interface OnProductActionCallback {
        void onToggleActive(DailyProduct product, boolean active);
        void onEdit(DailyProduct product);
        void onDelete(DailyProduct product);
    }

    private List<DailyProduct> originalList = new ArrayList<>();
    private List<DailyProduct> displayList = new ArrayList<>();
    private final OnProductActionCallback callback;

    public ManageProductsAdapter(OnProductActionCallback callback) {
        this.callback = callback;
    }

    public void setData(List<DailyProduct> products) {
        this.originalList = new ArrayList<>(products);
        this.displayList = new ArrayList<>(products);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_manage_product, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DailyProduct product = displayList.get(position);
        holder.tvIcon.setText(product.getDisplayEmoji());
        holder.tvName.setText(product.getName());

        if (product.getPrice() > 0) {
            holder.tvPrice.setText("₹" + String.format(Locale.US, "%.2f", product.getPrice()));
            holder.tvPrice.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.brand_blue));
        } else {
            holder.tvPrice.setText("No price set");
            holder.tvPrice.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.text_secondary));
        }

        holder.switchActive.setOnCheckedChangeListener(null);
        holder.switchActive.setChecked(product.isActive());
        holder.switchActive.setOnCheckedChangeListener((btn, isChecked) -> {
            if (callback != null) callback.onToggleActive(product, isChecked);
        });

        holder.btnEdit.setOnClickListener(v -> {
            if (callback != null) callback.onEdit(product);
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (callback != null) callback.onDelete(product);
        });
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
                FilterResults res = new FilterResults();
                res.values = filtered;
                res.count = filtered.size();
                return res;
            }

            @SuppressWarnings("unchecked")
            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                displayList = (List<DailyProduct>) results.values;
                notifyDataSetChanged();
            }
        };
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvIcon, tvName, tvPrice;
        SwitchCompat switchActive;
        ImageButton btnEdit, btnDelete;

        ViewHolder(View itemView) {
            super(itemView);
            tvIcon = itemView.findViewById(R.id.tv_product_icon);
            tvName = itemView.findViewById(R.id.tv_product_name);
            tvPrice = itemView.findViewById(R.id.tv_product_price);
            switchActive = itemView.findViewById(R.id.switch_product_active);
            btnEdit = itemView.findViewById(R.id.btn_edit_product);
            btnDelete = itemView.findViewById(R.id.btn_delete_product);
        }
    }
}
