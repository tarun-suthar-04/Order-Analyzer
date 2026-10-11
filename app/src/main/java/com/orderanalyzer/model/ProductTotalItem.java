package com.orderanalyzer.model;

public class ProductTotalItem implements java.io.Serializable {
    private long productId;
    private String productName;
    private int totalQuantity;
    private String iconKey;
    private String colorKey;
    private double unitPrice;    // per-unit price from products table
    private double totalAmount;  // totalQuantity × unitPrice

    public ProductTotalItem(long productId, String productName, int totalQuantity, String iconKey, String colorKey, double unitPrice) {
        this.productId = productId;
        this.productName = productName;
        this.totalQuantity = totalQuantity;
        this.iconKey = iconKey;
        this.colorKey = colorKey;
        this.unitPrice = unitPrice;
        this.totalAmount = totalQuantity * unitPrice;
    }

    public long getProductId() { return productId; }
    public String getProductName() { return productName; }
    public int getTotalQuantity() { return totalQuantity; }
    public String getIconKey() { return iconKey; }
    public String getColorKey() { return colorKey; }
    public double getUnitPrice() { return unitPrice; }
    public double getTotalAmount() { return totalAmount; }

    /** Formatted unit price */
    public String getFormattedPrice() {
        if (unitPrice <= 0) return "—";
        if (unitPrice == (long) unitPrice) return "₹" + (long) unitPrice;
        return String.format("₹%.2f", unitPrice);
    }

    /** Formatted total amount */
    public String getFormattedTotalAmount() {
        if (unitPrice <= 0) return "—";
        if (totalAmount == (long) totalAmount) return "₹" + (long) totalAmount;
        return String.format("₹%.2f", totalAmount);
    }

    public String getDisplayEmoji() {
        if (productName == null) return "📦";
        String lower = productName.toLowerCase();
        if (lower.contains("milk")) return "🥛";
        if (lower.contains("coin")) return "🪙";
        if (lower.contains("bun") || lower.contains("burger")) return "🍔";
        if (lower.contains("pav") || lower.contains("bread")) return "🍞";
        if (lower.contains("muffin") || lower.contains("cake")) return "🧁";
        if (lower.contains("atta")) return "🌾";
        if (lower.contains("other")) return "⋯";
        return "📦";
    }
}
