package com.orderanalyzer.model;

public class DashboardMetrics {
    private int retailersCount;
    private int totalItems;
    private int productsOrderedCount;
    private int pendingRetailersCount;
    private double totalBillAmount; // Grand total bill for selected date

    public DashboardMetrics(int retailersCount, int totalItems, int productsOrderedCount, int pendingRetailersCount) {
        this.retailersCount = retailersCount;
        this.totalItems = totalItems;
        this.productsOrderedCount = productsOrderedCount;
        this.pendingRetailersCount = pendingRetailersCount;
        this.totalBillAmount = 0.0;
    }

    public DashboardMetrics(int retailersCount, int totalItems, int productsOrderedCount, int pendingRetailersCount, double totalBillAmount) {
        this.retailersCount = retailersCount;
        this.totalItems = totalItems;
        this.productsOrderedCount = productsOrderedCount;
        this.pendingRetailersCount = pendingRetailersCount;
        this.totalBillAmount = totalBillAmount;
    }

    public int getRetailersCount() { return retailersCount; }
    public int getTotalItems() { return totalItems; }
    public int getProductsOrderedCount() { return productsOrderedCount; }
    public int getPendingRetailersCount() { return pendingRetailersCount; }
    public double getTotalBillAmount() { return totalBillAmount; }

    public String getFormattedTotalBill() {
        if (totalBillAmount <= 0) return "₹0";
        if (totalBillAmount == (long) totalBillAmount) return "₹" + (long) totalBillAmount;
        return String.format("₹%.2f", totalBillAmount);
    }
}
