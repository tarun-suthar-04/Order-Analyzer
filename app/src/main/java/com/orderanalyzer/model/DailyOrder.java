package com.orderanalyzer.model;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DailyOrder implements Serializable {
    private long id;
    private String orderDate; // "yyyy-MM-dd"
    private long retailerId;
    private String retailerName;
    private String retailerPhone;
    private String retailerAddress;
    private long timestamp;
    private String notes;
    private List<DailyOrderItem> items = new ArrayList<>();

    public DailyOrder() {}

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getOrderDate() { return orderDate; }
    public void setOrderDate(String orderDate) { this.orderDate = orderDate; }

    public long getRetailerId() { return retailerId; }
    public void setRetailerId(long retailerId) { this.retailerId = retailerId; }

    public String getRetailerName() { return retailerName; }
    public void setRetailerName(String retailerName) { this.retailerName = retailerName; }

    public String getRetailerPhone() { return retailerPhone; }
    public void setRetailerPhone(String retailerPhone) { this.retailerPhone = retailerPhone; }

    public String getRetailerAddress() { return retailerAddress; }
    public void setRetailerAddress(String retailerAddress) { this.retailerAddress = retailerAddress; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public List<DailyOrderItem> getItems() { return items; }
    public void setItems(List<DailyOrderItem> items) { this.items = items; }

    public int getTotalUnits() {
        int sum = 0;
        if (items != null) {
            for (DailyOrderItem item : items) {
                sum += item.getQuantity();
            }
        }
        return sum;
    }

    public int getDistinctProductsCount() {
        int count = 0;
        if (items != null) {
            for (DailyOrderItem item : items) {
                if (item.getQuantity() > 0) count++;
            }
        }
        return count;
    }

    public String getFormattedTime() {
        if (timestamp <= 0) return "";
        return new SimpleDateFormat("hh:mm a", Locale.US).format(new Date(timestamp));
    }
}
