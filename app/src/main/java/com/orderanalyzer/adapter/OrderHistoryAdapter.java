package com.orderanalyzer.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.orderanalyzer.R;
import com.orderanalyzer.db.DailyOrderRepository;
import com.orderanalyzer.model.DashboardMetrics;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class OrderHistoryAdapter extends RecyclerView.Adapter<OrderHistoryAdapter.ViewHolder> {

    public static class DayItem {
        public String date;
        public String relativeLabel;
        public String displayDate;
        public int retailersCount;
        public int itemsCount;
        public double totalBill;

        public DayItem(String date, String relativeLabel, String displayDate, int retailersCount, int itemsCount, double totalBill) {
            this.date = date;
            this.relativeLabel = relativeLabel;
            this.displayDate = displayDate;
            this.retailersCount = retailersCount;
            this.itemsCount = itemsCount;
            this.totalBill = totalBill;
        }
    }

    public interface OnHistoryActionListener {
        void onViewOrders(String date);
        void onViewTotals(String date);
    }

    private List<DayItem> days = new ArrayList<>();
    private final OnHistoryActionListener listener;

    public OrderHistoryAdapter(OnHistoryActionListener listener) {
        this.listener = listener;
    }

    public void setData(List<DayItem> list) {
        this.days = new ArrayList<>(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_order_history_day, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DayItem item = days.get(position);

        String tag = item.relativeLabel.toUpperCase(Locale.US);
        holder.tvTag.setText(tag);
        boolean isToday = item.date.equals(DailyOrderRepository.getTodayDate());
        holder.tvTag.setTextColor(holder.itemView.getContext().getResources().getColor(
                isToday ? R.color.brand_blue : R.color.text_secondary
        ));

        holder.tvDate.setText(item.displayDate);
        holder.tvRetailers.setText(item.retailersCount + " Retailer" + (item.retailersCount == 1 ? "" : "s"));
        holder.tvItems.setText(item.itemsCount + " Item" + (item.itemsCount == 1 ? "" : "s"));
        holder.tvBill.setText(String.format(Locale.US, "₹%.2f", item.totalBill));

        holder.btnViewOrders.setOnClickListener(v -> {
            if (listener != null) listener.onViewOrders(item.date);
        });

        holder.btnViewTotals.setOnClickListener(v -> {
            if (listener != null) listener.onViewTotals(item.date);
        });
    }

    @Override
    public int getItemCount() {
        return days.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTag, tvDate, tvRetailers, tvItems, tvBill;
        Button btnViewOrders, btnViewTotals;

        ViewHolder(View itemView) {
            super(itemView);
            tvTag = itemView.findViewById(R.id.tv_history_tag);
            tvDate = itemView.findViewById(R.id.tv_history_date);
            tvRetailers = itemView.findViewById(R.id.tv_history_retailers);
            tvItems = itemView.findViewById(R.id.tv_history_items);
            tvBill = itemView.findViewById(R.id.tv_history_bill);
            btnViewOrders = itemView.findViewById(R.id.btn_history_view_orders);
            btnViewTotals = itemView.findViewById(R.id.btn_history_view_totals);
        }
    }
}
