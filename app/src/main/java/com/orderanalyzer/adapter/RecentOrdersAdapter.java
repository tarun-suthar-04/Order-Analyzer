package com.orderanalyzer.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.orderanalyzer.R;
import com.orderanalyzer.model.DailyOrder;

import java.util.ArrayList;
import java.util.List;

public class RecentOrdersAdapter extends RecyclerView.Adapter<RecentOrdersAdapter.ViewHolder> {

    public interface OnRecentOrderClickListener {
        void onRecentOrderClick(DailyOrder order);
    }

    private List<DailyOrder> list = new ArrayList<>();
    private final OnRecentOrderClickListener listener;

    public RecentOrdersAdapter(OnRecentOrderClickListener listener) {
        this.listener = listener;
    }

    public void setData(List<DailyOrder> orders) {
        this.list = new ArrayList<>(orders);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_recent_order, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DailyOrder order = list.get(position);
        holder.tvName.setText(order.getRetailerName());
        holder.tvTime.setText(order.getFormattedTime());
        holder.tvUnits.setText(order.getTotalUnits() + " units");

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onRecentOrderClick(order);
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvAvatar, tvName, tvTime, tvUnits;

        ViewHolder(View itemView) {
            super(itemView);
            tvAvatar = itemView.findViewById(R.id.tv_avatar);
            tvName = itemView.findViewById(R.id.tv_retailer_name);
            tvTime = itemView.findViewById(R.id.tv_order_time);
            tvUnits = itemView.findViewById(R.id.tv_item_count);
        }
    }
}
