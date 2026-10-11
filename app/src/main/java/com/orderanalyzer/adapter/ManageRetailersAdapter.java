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
import com.orderanalyzer.model.DailyRetailer;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ManageRetailersAdapter extends RecyclerView.Adapter<ManageRetailersAdapter.ViewHolder>
        implements Filterable {

    public interface OnRetailerActionCallback {
        void onToggleActive(DailyRetailer retailer, boolean active);
        void onEdit(DailyRetailer retailer);
        void onDelete(DailyRetailer retailer);
    }

    private List<DailyRetailer> originalList = new ArrayList<>();
    private List<DailyRetailer> displayList = new ArrayList<>();
    private final OnRetailerActionCallback callback;

    public ManageRetailersAdapter(OnRetailerActionCallback callback) {
        this.callback = callback;
    }

    public void setData(List<DailyRetailer> retailers) {
        this.originalList = new ArrayList<>(retailers);
        this.displayList = new ArrayList<>(retailers);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_manage_retailer, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DailyRetailer retailer = displayList.get(position);
        holder.tvAvatar.setText(retailer.getInitialLetter());
        holder.tvName.setText(retailer.getName());

        String phone = retailer.getPhone();
        if (phone != null && !phone.isEmpty()) {
            holder.tvPhone.setVisibility(View.VISIBLE);
            holder.tvPhone.setText(phone);
        } else {
            holder.tvPhone.setVisibility(View.GONE);
        }

        holder.switchActive.setOnCheckedChangeListener(null);
        holder.switchActive.setChecked(retailer.isActive());
        holder.switchActive.setOnCheckedChangeListener((btn, isChecked) -> {
            if (callback != null) callback.onToggleActive(retailer, isChecked);
        });

        holder.btnEdit.setOnClickListener(v -> {
            if (callback != null) callback.onEdit(retailer);
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (callback != null) callback.onDelete(retailer);
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
                List<DailyRetailer> filtered = new ArrayList<>();
                if (query.isEmpty()) {
                    filtered.addAll(originalList);
                } else {
                    for (DailyRetailer r : originalList) {
                        if (r.getName().toLowerCase(Locale.US).contains(query)) {
                            filtered.add(r);
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
                displayList = (List<DailyRetailer>) results.values;
                notifyDataSetChanged();
            }
        };
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvAvatar, tvName, tvPhone;
        SwitchCompat switchActive;
        ImageButton btnEdit, btnDelete;

        ViewHolder(View itemView) {
            super(itemView);
            tvAvatar = itemView.findViewById(R.id.tv_avatar_initial);
            tvName = itemView.findViewById(R.id.tv_retailer_name);
            tvPhone = itemView.findViewById(R.id.tv_retailer_phone);
            switchActive = itemView.findViewById(R.id.switch_retailer_active);
            btnEdit = itemView.findViewById(R.id.btn_edit_retailer);
            btnDelete = itemView.findViewById(R.id.btn_delete_retailer);
        }
    }
}
