package com.orderanalyzer.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.orderanalyzer.R;
import com.orderanalyzer.db.DailyOrderRepository;
import com.orderanalyzer.model.DailyOrder;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TodayRetailersAdapter extends RecyclerView.Adapter<TodayRetailersAdapter.ViewHolder>
        implements Filterable {

    public interface OnItemActionCallback {
        void onItemClick(DailyOrder order);
        void onEdit(DailyOrder order);
        void onDelete(DailyOrder order);
    }

    private List<DailyOrder> originalList = new ArrayList<>();
    private List<DailyOrder> displayList = new ArrayList<>();
    private final OnItemActionCallback callback;

    public TodayRetailersAdapter(OnItemActionCallback callback) {
        this.callback = callback;
    }

    public void setData(List<DailyOrder> orders) {
        this.originalList = new ArrayList<>(orders);
        this.displayList = new ArrayList<>(orders);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_today_retailer, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DailyOrder order = displayList.get(position);
        String name = order.getRetailerName();
        String initial = (name != null && !name.trim().isEmpty()) ? name.trim().substring(0, 1).toUpperCase() : "R";

        holder.tvAvatar.setText(initial);
        holder.tvName.setText(name);
        holder.tvItemCount.setText(order.getTotalUnits() + " items");

        boolean isToday = order.getOrderDate() != null && order.getOrderDate().equals(DailyOrderRepository.getTodayDate());
        holder.btnEdit.setVisibility(isToday ? View.VISIBLE : View.GONE);
        holder.btnDelete.setVisibility(isToday ? View.VISIBLE : View.GONE);

        holder.itemView.setOnClickListener(v -> {
            if (callback != null) callback.onItemClick(order);
        });

        holder.btnEdit.setOnClickListener(v -> {
            if (callback != null) callback.onEdit(order);
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (callback != null) callback.onDelete(order);
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
                List<DailyOrder> filtered = new ArrayList<>();
                if (query.isEmpty()) {
                    filtered.addAll(originalList);
                } else {
                    for (DailyOrder o : originalList) {
                        if (o.getRetailerName() != null && o.getRetailerName().toLowerCase(Locale.US).contains(query)) {
                            filtered.add(o);
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
                displayList = (List<DailyOrder>) results.values;
                notifyDataSetChanged();
            }
        };
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvAvatar, tvName, tvItemCount;
        ImageButton btnEdit, btnDelete;

        ViewHolder(View itemView) {
            super(itemView);
            tvAvatar = itemView.findViewById(R.id.tv_avatar_initial);
            tvName = itemView.findViewById(R.id.tv_retailer_name);
            tvItemCount = itemView.findViewById(R.id.tv_item_count);
            btnEdit = itemView.findViewById(R.id.btn_edit_retailer);
            btnDelete = itemView.findViewById(R.id.btn_delete_retailer);
        }
    }
}
