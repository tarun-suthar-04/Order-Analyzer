package com.orderanalyzer.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.orderanalyzer.R;
import com.orderanalyzer.model.DailyProduct;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class PreferredProductSelectAdapter extends RecyclerView.Adapter<PreferredProductSelectAdapter.ViewHolder>
        implements Filterable {

    private List<DailyProduct> originalList = new ArrayList<>();
    private List<DailyProduct> displayList = new ArrayList<>();
    private final Set<Long> selectedIds = new HashSet<>();

    public void setData(List<DailyProduct> products, List<Long> initialSelected) {
        this.originalList = new ArrayList<>(products);
        this.displayList = new ArrayList<>(products);
        this.selectedIds.clear();
        if (initialSelected != null) {
            this.selectedIds.addAll(initialSelected);
        }
        notifyDataSetChanged();
    }

    public List<Long> getSelectedIds() {
        return new ArrayList<>(selectedIds);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_preferred_product_select, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DailyProduct product = displayList.get(position);
        holder.tvIcon.setText(product.getDisplayEmoji());
        holder.tvName.setText(product.getName());

        holder.cbSelect.setOnCheckedChangeListener(null);
        holder.cbSelect.setChecked(selectedIds.contains(product.getId()));

        holder.cbSelect.setOnCheckedChangeListener((btn, isChecked) -> {
            if (isChecked) {
                selectedIds.add(product.getId());
            } else {
                selectedIds.remove(product.getId());
            }
        });

        holder.itemView.setOnClickListener(v -> {
            boolean newState = !holder.cbSelect.isChecked();
            holder.cbSelect.setChecked(newState);
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
        CheckBox cbSelect;
        TextView tvIcon, tvName;

        ViewHolder(View itemView) {
            super(itemView);
            cbSelect = itemView.findViewById(R.id.cb_product_select);
            tvIcon = itemView.findViewById(R.id.tv_product_icon);
            tvName = itemView.findViewById(R.id.tv_product_name);
        }
    }
}
