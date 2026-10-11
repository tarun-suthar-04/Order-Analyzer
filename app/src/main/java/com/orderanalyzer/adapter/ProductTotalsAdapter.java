package com.orderanalyzer.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.orderanalyzer.R;
import com.orderanalyzer.model.ProductTotalItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ProductTotalsAdapter extends RecyclerView.Adapter<ProductTotalsAdapter.ViewHolder>
        implements Filterable {

    private List<ProductTotalItem> originalList = new ArrayList<>();
    private List<ProductTotalItem> displayList = new ArrayList<>();

    public void setData(List<ProductTotalItem> items) {
        this.originalList = new ArrayList<>(items);
        this.displayList = new ArrayList<>(items);
        notifyDataSetChanged();
    }

    /**
     * Returns the full unfiltered list for clipboard/share operations.
     */
    public List<ProductTotalItem> getItems() {
        return originalList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_product_total_row, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ProductTotalItem item = displayList.get(position);
        holder.tvIndex.setText(String.valueOf(position + 1));
        holder.tvIcon.setText(item.getDisplayEmoji());
        holder.tvName.setText(item.getProductName());

        double price = item.getUnitPrice();
        if (price > 0) {
            holder.tvPrice.setText(String.format(Locale.US, "₹%.2f / unit", price));
            holder.tvAmount.setText(String.format(Locale.US, "₹%.2f", item.getTotalAmount()));
        } else {
            holder.tvPrice.setText("No price set");
            holder.tvAmount.setText("₹0.00");
        }

        holder.tvQty.setText(String.valueOf(item.getTotalQuantity()));
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
                List<ProductTotalItem> filtered = new ArrayList<>();
                if (query.isEmpty()) {
                    filtered.addAll(originalList);
                } else {
                    for (ProductTotalItem item : originalList) {
                        if (item.getProductName().toLowerCase(Locale.US).contains(query)) {
                            filtered.add(item);
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
                displayList = (List<ProductTotalItem>) results.values;
                notifyDataSetChanged();
            }
        };
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvIndex, tvIcon, tvName, tvPrice, tvQty, tvAmount;

        ViewHolder(View itemView) {
            super(itemView);
            tvIndex = itemView.findViewById(R.id.tv_index);
            tvIcon = itemView.findViewById(R.id.tv_product_icon);
            tvName = itemView.findViewById(R.id.tv_product_name);
            tvPrice = itemView.findViewById(R.id.tv_product_price);
            tvQty = itemView.findViewById(R.id.tv_product_qty);
            tvAmount = itemView.findViewById(R.id.tv_product_amount);
        }
    }
}
