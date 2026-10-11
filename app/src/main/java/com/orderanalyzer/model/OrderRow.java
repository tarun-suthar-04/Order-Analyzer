package com.orderanalyzer.model;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Represents a single processed row from the uploaded file.
 * Contains the retailer name, vehicle name, per-product quantities,
 * and the pre-calculated row total.
 */
public class OrderRow {

    private String retailerName;
    private String vehicleName;

    // Preserves insertion order of product columns
    private final Map<String, Double> productQuantities = new LinkedHashMap<>();

    private double rowTotal = 0.0;

    // ── Getters & Setters ──────────────────────────────────────────────────

    public String getRetailerName()                    { return retailerName; }
    public void   setRetailerName(String retailerName) { this.retailerName = retailerName; }

    public String getVehicleName()                   { return vehicleName; }
    public void   setVehicleName(String vehicleName) { this.vehicleName = vehicleName; }

    public Map<String, Double> getProductQuantities() { return productQuantities; }

    public double getRowTotal() { return rowTotal; }

    /**
     * Adds a product quantity and increments the running row total.
     *
     * @param productName the column header (product name)
     * @param quantity    the numeric value from that cell
     */
    public void addProduct(String productName, double quantity) {
        productQuantities.put(productName, quantity);
        rowTotal += quantity;
    }
}
