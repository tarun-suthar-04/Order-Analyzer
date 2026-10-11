package com.orderanalyzer.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.orderanalyzer.R;
import com.orderanalyzer.model.DailyOrderItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class OrderDetailProductsAdapter extends RecyclerView.Adapter<OrderDetailProductsAdapter.ViewHolder> {

    private List<DailyOrderItem> list = new ArrayList<>();

    public void setData(List<DailyOrderItem> items) {
        this.list = new ArrayList<>();
        if (items != null) {
            for (DailyOrderItem item : items) {
                if (item.getQuantity() > 0) {
                    this.list.add(item);
                }
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_order_detail_row, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DailyOrderItem item = list.get(position);
        holder.tvIndex.setText(String.valueOf(position + 1));
        holder.tvName.setText(item.getProductName());

        int qty = item.getQuantity();
        double price = item.getUnitPrice();
        double amount = item.getLineAmount();

        if (price > 0) {
            holder.tvCalc.setText(String.format(Locale.US, "%d × ₹%.2f", qty, price));
            holder.tvAmount.setText(String.format(Locale.US, "₹%.2f", amount));
        } else {
            holder.tvCalc.setText(qty + " units");
            holder.tvAmount.setText("₹0.00");
        }
        holder.tvQty.setText(qty + " unit" + (qty == 1 ? "" : "s"));
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvIndex, tvName, tvCalc, tvAmount, tvQty;

        ViewHolder(View itemView) {
            super(itemView);
            tvIndex = itemView.findViewById(R.id.tv_detail_index);
            tvName = itemView.findViewById(R.id.tv_detail_product_name);
            tvCalc = itemView.findViewById(R.id.tv_detail_calc);
            tvAmount = itemView.findViewById(R.id.tv_detail_amount);
            tvQty = itemView.findViewById(R.id.tv_detail_qty);
        }
    }
}
