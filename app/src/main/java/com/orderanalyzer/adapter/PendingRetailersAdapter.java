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
import com.orderanalyzer.model.DailyRetailer;

import java.util.ArrayList;
import java.util.List;

public class PendingRetailersAdapter extends RecyclerView.Adapter<PendingRetailersAdapter.ViewHolder> implements Filterable {

    public interface OnPendingRetailerClickListener {
        void onRetailerClick(DailyRetailer retailer);
    }

    private List<DailyRetailer> originalList = new ArrayList<>();
    private List<DailyRetailer> displayList = new ArrayList<>();
    private final OnPendingRetailerClickListener listener;

    public PendingRetailersAdapter(OnPendingRetailerClickListener listener) {
        this.listener = listener;
    }

    public void setData(List<DailyRetailer> retailers) {
        this.originalList = new ArrayList<>(retailers);
        this.displayList = new ArrayList<>(retailers);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_pending_retailer, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DailyRetailer r = displayList.get(position);

        String name = r.getName();
        String initial = (name != null && !name.trim().isEmpty()) ? name.trim().substring(0, 1).toUpperCase() : "R";
        holder.tvAvatar.setText(initial);
        holder.tvName.setText(name);

        String phone = r.getPhone();
        if (phone != null && !phone.trim().isEmpty()) {
            holder.tvPhone.setVisibility(View.VISIBLE);
            holder.tvPhone.setText(phone);
        } else {
            holder.tvPhone.setVisibility(View.GONE);
        }

        String address = r.getAddress();
        if (address != null && !address.trim().isEmpty()) {
            holder.tvAddress.setVisibility(View.VISIBLE);
            holder.tvAddress.setText(address);
        } else {
            holder.tvAddress.setVisibility(View.GONE);
        }

        View.OnClickListener clickListener = v -> {
            if (listener != null) listener.onRetailerClick(r);
        };

        holder.itemView.setOnClickListener(clickListener);
        holder.btnTakeOrder.setOnClickListener(clickListener);
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
                List<DailyRetailer> filtered = new ArrayList<>();
                if (constraint == null || constraint.length() == 0) {
                    filtered.addAll(originalList);
                } else {
                    String query = constraint.toString().toLowerCase().trim();
                    for (DailyRetailer r : originalList) {
                        if (r.getName().toLowerCase().contains(query)
                                || (r.getPhone() != null && r.getPhone().toLowerCase().contains(query))
                                || (r.getAddress() != null && r.getAddress().toLowerCase().contains(query))) {
                            filtered.add(r);
                        }
                    }
                }
                FilterResults results = new FilterResults();
                results.values = filtered;
                return results;
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
        TextView tvAvatar, tvName, tvPhone, tvAddress;
        View btnTakeOrder;

        ViewHolder(View itemView) {
            super(itemView);
            tvAvatar = itemView.findViewById(R.id.tv_avatar);
            tvName = itemView.findViewById(R.id.tv_retailer_name);
            tvPhone = itemView.findViewById(R.id.tv_retailer_phone);
            tvAddress = itemView.findViewById(R.id.tv_retailer_address);
            btnTakeOrder = itemView.findViewById(R.id.btn_take_retailer_order);
        }
    }
}
