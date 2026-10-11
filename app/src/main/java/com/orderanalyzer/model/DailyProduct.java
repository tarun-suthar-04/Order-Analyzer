package com.orderanalyzer.model;

import java.io.Serializable;

public class DailyProduct implements Serializable {
    private long id;
    private String cloudId;
    private String name;
    private boolean isActive;
    private int sortOrder;
    private String iconKey;
    private String colorKey;
    private double price; // configurable unit price (₹)

    public DailyProduct() {}

    public DailyProduct(long id, String name, boolean isActive, int sortOrder, String iconKey, String colorKey) {
        this(id, null, name, isActive, sortOrder, iconKey, colorKey, 0.0);
    }

    public DailyProduct(long id, String name, boolean isActive, int sortOrder, String iconKey, String colorKey, double price) {
        this(id, null, name, isActive, sortOrder, iconKey, colorKey, price);
    }

    public DailyProduct(long id, String cloudId, String name, boolean isActive, int sortOrder, String iconKey, String colorKey, double price) {
        this.id = id;
        this.cloudId = cloudId != null && !cloudId.trim().isEmpty() ? cloudId.trim() : generateCloudId(name);
        this.name = name;
        this.isActive = isActive;
        this.sortOrder = sortOrder;
        this.iconKey = iconKey != null ? iconKey : "📦";
        this.colorKey = colorKey != null ? colorKey : "blue";
        this.price = price;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getCloudId() {
        if (cloudId == null || cloudId.trim().isEmpty()) {
            cloudId = generateCloudId(name);
        }
        return cloudId;
    }

    public void setCloudId(String cloudId) {
        this.cloudId = cloudId;
    }

    public static String generateCloudId(String name) {
        String clean = (name != null) ? name.trim().toLowerCase(java.util.Locale.US) : "";
        return java.util.UUID.nameUUIDFromBytes(("prod:" + clean).getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString();
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }

    public String getIconKey() { return iconKey; }
    public void setIconKey(String iconKey) { this.iconKey = iconKey; }

    public String getColorKey() { return colorKey; }
    public void setColorKey(String colorKey) { this.colorKey = colorKey; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    /** Returns formatted price string like ₹25 or "Not set" */
    public String getFormattedPrice() {
        if (price <= 0) return "Price not set";
        if (price == (long) price) {
            return "₹" + (long) price;
        }
        return String.format("₹%.2f", price);
    }

    public String getDisplayEmoji() {
        if (name == null) return "📦";
        String lower = name.toLowerCase();
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
