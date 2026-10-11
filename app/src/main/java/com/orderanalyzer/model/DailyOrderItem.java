package com.orderanalyzer.model;

import java.io.Serializable;

public class DailyOrderItem implements Serializable {
    private long orderId;
    private long productId;
    private String productName;
    private int quantity;
    private double unitPrice; // price per unit at time of order (from products table)

    public DailyOrderItem() {}

    public DailyOrderItem(long orderId, long productId, String productName, int quantity) {
        this.orderId = orderId;
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.unitPrice = 0.0;
    }

    public DailyOrderItem(long orderId, long productId, String productName, int quantity, double unitPrice) {
        this.orderId = orderId;
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    public long getOrderId() { return orderId; }
    public void setOrderId(long orderId) { this.orderId = orderId; }

    public long getProductId() { return productId; }
    public void setProductId(long productId) { this.productId = productId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public double getUnitPrice() { return unitPrice; }
    public void setUnitPrice(double unitPrice) { this.unitPrice = unitPrice; }

    /** Calculated amount: Quantity × Unit Price */
    public double getAmount() {
        return quantity * unitPrice;
    }

    public double getLineAmount() {
        return getAmount();
    }

    /** Formatted amount string */
    public String getFormattedAmount() {
        double amt = getAmount();
        if (unitPrice <= 0) return "—";
        if (amt == (long) amt) return "₹" + (long) amt;
        return String.format("₹%.2f", amt);
    }

    /** Formatted unit price string */
    public String getFormattedUnitPrice() {
        if (unitPrice <= 0) return "—";
        if (unitPrice == (long) unitPrice) return "₹" + (long) unitPrice;
        return String.format("₹%.2f", unitPrice);
    }
}
