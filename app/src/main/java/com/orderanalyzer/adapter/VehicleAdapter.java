package com.orderanalyzer.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.orderanalyzer.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * RecyclerView adapter for the Vehicle Summary section.
 * Displays vehicle name (left) and total order units (right).
 */
public class VehicleAdapter extends RecyclerView.Adapter<VehicleAdapter.VehicleViewHolder> {

    private List<Map.Entry<String, Double>> vehicleList;

    public VehicleAdapter(List<Map.Entry<String, Double>> vehicleList) {
        this.vehicleList = new ArrayList<>(vehicleList);
    }

    /** Replaces data and refreshes the list. */
    public void updateData(List<Map.Entry<String, Double>> newList) {
        this.vehicleList = new ArrayList<>(newList);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VehicleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_vehicle, parent, false);
        return new VehicleViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VehicleViewHolder holder, int position) {
        Map.Entry<String, Double> entry = vehicleList.get(position);
        holder.tvVehicleName.setText(entry.getKey());
        holder.tvVehicleTotal.setText(
                String.format(Locale.getDefault(), "%.0f units", entry.getValue()));
    }

    @Override
    public int getItemCount() { return vehicleList.size(); }

    // ── ViewHolder ────────────────────────────────────────────────────────

    static class VehicleViewHolder extends RecyclerView.ViewHolder {
        final TextView tvVehicleName;
        final TextView tvVehicleTotal;

        VehicleViewHolder(@NonNull View itemView) {
            super(itemView);
            tvVehicleName  = itemView.findViewById(R.id.tv_vehicle_name);
            tvVehicleTotal = itemView.findViewById(R.id.tv_vehicle_total);
        }
    }
}
