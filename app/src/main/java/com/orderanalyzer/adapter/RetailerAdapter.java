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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * RecyclerView adapter for the Retailer Summary section.
 * Implements {@link Filterable} to support real-time search filtering.
 */
public class RetailerAdapter
        extends RecyclerView.Adapter<RetailerAdapter.RetailerViewHolder>
        implements Filterable {

    /** Currently displayed (possibly filtered) list */
    private List<Map.Entry<String, Double>> retailerList;

    /** Master backup list — always holds full unfiltered data */
    private List<Map.Entry<String, Double>> retailerListFull;

    public RetailerAdapter(List<Map.Entry<String, Double>> retailerList) {
        this.retailerList     = new ArrayList<>(retailerList);
        this.retailerListFull = new ArrayList<>(retailerList);
    }

    /**
     * Completely replaces data (e.g. after sort or new file load).
     * Resets the full backup list too.
     */
    public void updateData(List<Map.Entry<String, Double>> newList) {
        this.retailerList     = new ArrayList<>(newList);
        this.retailerListFull = new ArrayList<>(newList);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RetailerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_retailer, parent, false);
        return new RetailerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RetailerViewHolder holder, int position) {
        Map.Entry<String, Double> entry = retailerList.get(position);

        holder.tvRetailerName.setText(entry.getKey());
        holder.tvRetailerTotal.setText(
                String.format(Locale.getDefault(), "%.0f", entry.getValue()));

        // Subtle zebra striping for large lists
        holder.itemView.setBackgroundColor(
                position % 2 == 0 ? 0xFFFFFFFF : 0xFFF8FAFF);
    }

    @Override
    public int getItemCount() { return retailerList.size(); }

    // ── Filterable ────────────────────────────────────────────────────────

    @Override
    public Filter getFilter() { return retailerFilter; }

    private final Filter retailerFilter = new Filter() {

        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            List<Map.Entry<String, Double>> filtered = new ArrayList<>();

            if (constraint == null || constraint.length() == 0) {
                filtered.addAll(retailerListFull);          // no query → show all
            } else {
                String query = constraint.toString()
                        .toLowerCase(Locale.getDefault()).trim();
                for (Map.Entry<String, Double> entry : retailerListFull) {
                    if (entry.getKey().toLowerCase(Locale.getDefault()).contains(query)) {
                        filtered.add(entry);
                    }
                }
            }

            FilterResults results = new FilterResults();
            results.values = filtered;
            results.count  = filtered.size();
            return results;
        }

        @Override
        @SuppressWarnings("unchecked")
        protected void publishResults(CharSequence constraint, FilterResults results) {
            retailerList = (List<Map.Entry<String, Double>>) results.values;
            notifyDataSetChanged();
        }
    };

    // ── ViewHolder ────────────────────────────────────────────────────────

    static class RetailerViewHolder extends RecyclerView.ViewHolder {
        final TextView tvRetailerName;
        final TextView tvRetailerTotal;

        RetailerViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRetailerName  = itemView.findViewById(R.id.tv_retailer_name);
            tvRetailerTotal = itemView.findViewById(R.id.tv_retailer_total);
        }
    }
}
