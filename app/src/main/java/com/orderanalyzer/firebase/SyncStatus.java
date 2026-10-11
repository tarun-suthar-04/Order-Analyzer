package com.orderanalyzer.firebase;

public enum SyncStatus {
    SYNCED("Synced", "#059669", "#D1FAE5"),
    SYNCING("Syncing...", "#2563EB", "#DBEAFE"),
    OFFLINE("Offline — will sync when online", "#D97706", "#FEF3C7"),
    ERROR("Sync error", "#EF4444", "#FEE2E2");

    private final String label;
    private final String textColorHex;
    private final String bgColorHex;

    SyncStatus(String label, String textColorHex, String bgColorHex) {
        this.label = label;
        this.textColorHex = textColorHex;
        this.bgColorHex = bgColorHex;
    }

    public String getLabel() {
        return label;
    }

    public String getTextColorHex() {
        return textColorHex;
    }

    public String getBgColorHex() {
        return bgColorHex;
    }
}
