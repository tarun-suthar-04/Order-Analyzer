package com.orderanalyzer.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class DailyRetailer implements Serializable {
    private long id;
    private String cloudId;
    private String name;
    private String phone;
    private String address;
    private String preferredProductIds; // comma-separated e.g. "1,2,4"
    private boolean isActive;

    public DailyRetailer() {}

    public DailyRetailer(long id, String name, String phone, String address, String preferredProductIds, boolean isActive) {
        this(id, null, name, phone, address, preferredProductIds, isActive);
    }

    public DailyRetailer(long id, String cloudId, String name, String phone, String address, String preferredProductIds, boolean isActive) {
        this.id = id;
        this.cloudId = cloudId != null && !cloudId.trim().isEmpty() ? cloudId.trim() : generateCloudId(name);
        this.name = name;
        this.phone = phone != null ? phone : "";
        this.address = address != null ? address : "";
        this.preferredProductIds = preferredProductIds != null ? preferredProductIds : "";
        this.isActive = isActive;
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
        return java.util.UUID.nameUUIDFromBytes(("ret:" + clean).getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString();
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getPreferredProductIds() { return preferredProductIds; }
    public void setPreferredProductIds(String preferredProductIds) { this.preferredProductIds = preferredProductIds; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public List<Long> getPreferredProductIdList() {
        List<Long> list = new ArrayList<>();
        if (preferredProductIds == null || preferredProductIds.trim().isEmpty()) {
            return list;
        }
        String[] parts = preferredProductIds.split(",");
        for (String part : parts) {
            try {
                list.add(Long.parseLong(part.trim()));
            } catch (NumberFormatException ignored) {}
        }
        return list;
    }

    public boolean isPreferred(long productId) {
        return getPreferredProductIdList().contains(productId);
    }

    public String getInitialLetter() {
        if (name == null || name.trim().isEmpty()) return "R";
        return name.trim().substring(0, 1).toUpperCase();
    }
}
