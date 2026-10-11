package com.orderanalyzer.firebase;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.PersistentCacheSettings;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.orderanalyzer.db.DailyOrderRepository;
import com.orderanalyzer.model.DailyOrder;
import com.orderanalyzer.model.DailyOrderItem;
import com.orderanalyzer.model.DailyProduct;
import com.orderanalyzer.model.DailyRetailer;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class FirebaseSyncManager {

    private static FirebaseSyncManager instance;
    private final Context appContext;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private SyncStatus currentStatus = SyncStatus.SYNCED;
    private long lastSyncTimestamp;
    private boolean isNetworkAvailable = true;
    private boolean isSyncingFromRemote = false;

    // Active listener registrations
    private ListenerRegistration productsRegistration;
    private ListenerRegistration retailersRegistration;
    private final List<ListenerRegistration> retainedOrdersRegistrations = new ArrayList<>();

    public interface OnSyncStatusChangeListener {
        void onSyncStatusChanged(SyncStatus status, String lastSyncTimeFormatted);
    }

    private final List<OnSyncStatusChangeListener> listeners = new ArrayList<>();

    private FirebaseSyncManager(Context context) {
        this.appContext = context.getApplicationContext();
        lastSyncTimestamp = appContext.getSharedPreferences("firebase_sync_status", Context.MODE_PRIVATE)
                .getLong("last_sync_timestamp", 0L);
        initNetworkMonitoring();
        configureFirestoreOfflineSettings();
    }

    public static synchronized FirebaseSyncManager getInstance(Context context) {
        if (instance == null) {
            instance = new FirebaseSyncManager(context);
        }
        return instance;
    }

    public void addListener(OnSyncStatusChangeListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
            listener.onSyncStatusChanged(currentStatus, getLastSyncFormatted());
        }
    }

    public void removeListener(OnSyncStatusChangeListener listener) {
        listeners.remove(listener);
    }

    private void notifyStatus(SyncStatus status) {
        this.currentStatus = status;
        if (status == SyncStatus.SYNCED) {
            lastSyncTimestamp = System.currentTimeMillis();
            appContext.getSharedPreferences("firebase_sync_status", Context.MODE_PRIVATE)
                    .edit().putLong("last_sync_timestamp", lastSyncTimestamp).apply();
        }
        mainHandler.post(() -> {
            for (OnSyncStatusChangeListener l : new ArrayList<>(listeners)) {
                l.onSyncStatusChanged(currentStatus, getLastSyncFormatted());
            }
        });
    }

    public SyncStatus getCurrentStatus() {
        return currentStatus;
    }

    public String getLastSyncFormatted() {
        if (lastSyncTimestamp <= 0) return "Never";
        return new SimpleDateFormat("hh:mm a, dd MMM", Locale.US).format(new Date(lastSyncTimestamp));
    }

    private void configureFirestoreOfflineSettings() {
        try {
            if (!FirebaseShopAuthManager.getInstance(appContext).isFirebaseAvailable()) return;
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                    .setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
                    .build();
            db.setFirestoreSettings(settings);
        } catch (Exception ignored) {}
    }

    private void initNetworkMonitoring() {
        try {
            ConnectivityManager cm = (ConnectivityManager) appContext.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) return;

            NetworkCapabilities caps = cm.getNetworkCapabilities(cm.getActiveNetwork());
            isNetworkAvailable = caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
            if (!isNetworkAvailable) {
                notifyStatus(SyncStatus.OFFLINE);
            }

            NetworkRequest request = new NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .build();

            cm.registerNetworkCallback(request, new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(@NonNull Network network) {
                    isNetworkAvailable = true;
                    if (currentStatus == SyncStatus.OFFLINE) {
                        notifyStatus(SyncStatus.SYNCING);
                        // Trigger re-sync
                        startSync();
                    }
                }

                @Override
                public void onLost(@NonNull Network network) {
                    isNetworkAvailable = false;
                    notifyStatus(SyncStatus.OFFLINE);
                }
            });
        } catch (Exception ignored) {}
    }

    /**
     * Starts real-time Firestore listeners for shop products, retailers, and daily orders.
     */
    public void startSync() {
        FirebaseShopAuthManager auth = FirebaseShopAuthManager.getInstance(appContext);
        if (!auth.isLoggedIn()) {
            return;
        }

        String shopId = auth.getCurrentShopId();
        if (shopId.isEmpty()) return;

        if (!isNetworkAvailable) {
            notifyStatus(SyncStatus.OFFLINE);
        } else {
            notifyStatus(SyncStatus.SYNCING);
        }

        stopSync(); // Clean up existing listeners

        try {
            FirebaseFirestore db = FirebaseFirestore.getInstance();

            // 1. Initial Local Data Migration (if cloud is fresh/empty)
            migrateLocalDataIfEmpty(shopId);

            // 2. Real-time Products Sync
            productsRegistration = db.collection("shops").document(shopId)
                    .collection("products")
                    .addSnapshotListener((snapshots, error) -> {
                        if (error != null) {
                            if (!isNetworkAvailable) notifyStatus(SyncStatus.OFFLINE);
                            else notifyStatus(SyncStatus.ERROR);
                            return;
                        }
                        if (snapshots != null) {
                            handleRemoteProducts(snapshots);
                            updateStatusFromMetadata(snapshots.getMetadata().hasPendingWrites());
                        }
                    });

            // 3. Real-time Retailers Sync
            retailersRegistration = db.collection("shops").document(shopId)
                    .collection("retailers")
                    .addSnapshotListener((snapshots, error) -> {
                        if (error != null) {
                            if (!isNetworkAvailable) notifyStatus(SyncStatus.OFFLINE);
                            else notifyStatus(SyncStatus.ERROR);
                            return;
                        }
                        if (snapshots != null) {
                            handleRemoteRetailers(snapshots);
                            updateStatusFromMetadata(snapshots.getMetadata().hasPendingWrites());
                        }
                    });

            // 4. Real-time 7-Day Retained Orders Sync
            for (ListenerRegistration reg : retainedOrdersRegistrations) {
                reg.remove();
            }
            retainedOrdersRegistrations.clear();

            List<String> retainedDates = DailyOrderRepository.getLastNDates(DailyOrderRepository.ORDER_RETENTION_DAYS);
            for (String orderDate : retainedDates) {
                ListenerRegistration reg = db.collection("shops").document(shopId)
                        .collection("dailyOrders").document(orderDate)
                        .collection("retailers")
                        .addSnapshotListener((snapshots, error) -> {
                            if (error != null) {
                                if (!isNetworkAvailable) notifyStatus(SyncStatus.OFFLINE);
                                else notifyStatus(SyncStatus.ERROR);
                                return;
                            }
                            if (snapshots != null) {
                                handleRemoteOrders(orderDate, snapshots);
                                updateStatusFromMetadata(snapshots.getMetadata().hasPendingWrites());
                            }
                        });
                retainedOrdersRegistrations.add(reg);
            }

            // Purge any older orders in cloud retention
            purgeOldFirestoreOrders(shopId);

        } catch (Exception e) {
            notifyStatus(SyncStatus.ERROR);
        }
    }

    public void stopSync() {
        if (productsRegistration != null) {
            productsRegistration.remove();
            productsRegistration = null;
        }
        if (retailersRegistration != null) {
            retailersRegistration.remove();
            retailersRegistration = null;
        }
        for (ListenerRegistration reg : retainedOrdersRegistrations) {
            reg.remove();
        }
        retainedOrdersRegistrations.clear();
    }

    private void updateStatusFromMetadata(boolean hasPendingWrites) {
        if (!isNetworkAvailable) {
            notifyStatus(SyncStatus.OFFLINE);
        } else if (hasPendingWrites) {
            notifyStatus(SyncStatus.SYNCING);
        } else {
            notifyStatus(SyncStatus.SYNCED);
        }
    }

    private void updateStatusFromTask(com.google.android.gms.tasks.Task<?> task) {
        if (!task.isSuccessful()) {
            notifyStatus(isNetworkAvailable ? SyncStatus.ERROR : SyncStatus.OFFLINE);
        } else {
            updateStatusFromMetadata(false);
        }
    }

    // ── Incoming Cloud Handlers ───────────────────────────────────────────

    private void handleRemoteProducts(QuerySnapshot snapshots) {
        isSyncingFromRemote = true;
        try {
            DailyOrderRepository repo = DailyOrderRepository.getInstance(appContext);

            for (com.google.firebase.firestore.DocumentChange dc : snapshots.getDocumentChanges()) {
                String cloudId = dc.getDocument().getId();
                if (dc.getType() == com.google.firebase.firestore.DocumentChange.Type.REMOVED) {
                    DailyProduct existing = repo.getProductByCloudId(cloudId);
                    if (existing != null) {
                        repo.deleteProductLocally(existing.getId());
                    }
                } else {
                    DocumentSnapshot doc = dc.getDocument();
                    String name = doc.getString("name");
                    Double price = doc.getDouble("price");
                    Boolean isActive = doc.getBoolean("isActive");
                    Long sortOrder = doc.getLong("sortOrder");
                    String iconKey = doc.getString("iconKey");
                    String colorKey = doc.getString("colorKey");
                    if (name != null && !name.trim().isEmpty()) {
                        repo.saveProductFromCloud(
                                cloudId,
                                name,
                                price != null ? price : 0.0,
                                isActive != null ? isActive : true,
                                sortOrder != null ? sortOrder.intValue() : 99,
                                iconKey,
                                colorKey
                        );
                    }
                }
            }
        } catch (Exception ignored) {
        } finally {
            isSyncingFromRemote = false;
        }
    }

    private void handleRemoteRetailers(QuerySnapshot snapshots) {
        isSyncingFromRemote = true;
        try {
            DailyOrderRepository repo = DailyOrderRepository.getInstance(appContext);

            for (com.google.firebase.firestore.DocumentChange dc : snapshots.getDocumentChanges()) {
                String cloudId = dc.getDocument().getId();
                if (dc.getType() == com.google.firebase.firestore.DocumentChange.Type.REMOVED) {
                    DailyRetailer existing = repo.getRetailerByCloudId(cloudId);
                    if (existing != null) {
                        repo.deleteRetailerLocally(existing.getId());
                    }
                } else {
                    DocumentSnapshot doc = dc.getDocument();
                    String name = doc.getString("name");
                    String phone = doc.getString("phone");
                    String address = doc.getString("address");
                    Boolean isActive = doc.getBoolean("isActive");

                    // Map preferred product cloud IDs to local IDs
                    List<String> preferredCloudIds = (List<String>) doc.get("preferredProductCloudIds");
                    StringBuilder preferredLocalIds = new StringBuilder();
                    if (preferredCloudIds != null) {
                        for (String pCloudId : preferredCloudIds) {
                            DailyProduct prod = repo.getProductByCloudId(pCloudId);
                            if (prod != null) {
                                if (preferredLocalIds.length() > 0) preferredLocalIds.append(",");
                                preferredLocalIds.append(prod.getId());
                            }
                        }
                    } else {
                        String legacy = doc.getString("preferredProductIds");
                        if (legacy != null) preferredLocalIds.append(legacy);
                    }

                    if (name != null && !name.trim().isEmpty()) {
                        repo.saveRetailerFromCloud(
                                cloudId,
                                name,
                                phone != null ? phone : "",
                                address != null ? address : "",
                                preferredLocalIds.toString(),
                                isActive != null ? isActive : true
                        );
                    }
                }
            }
        } catch (Exception ignored) {
        } finally {
            isSyncingFromRemote = false;
        }
    }

    private void handleRemoteOrders(String date, QuerySnapshot snapshots) {
        isSyncingFromRemote = true;
        try {
            DailyOrderRepository repo = DailyOrderRepository.getInstance(appContext);

            for (com.google.firebase.firestore.DocumentChange dc : snapshots.getDocumentChanges()) {
                String retailerCloudId = dc.getDocument().getId();
                if (dc.getType() == com.google.firebase.firestore.DocumentChange.Type.REMOVED) {
                    DailyRetailer retailer = repo.getRetailerByCloudId(retailerCloudId);
                    if (retailer != null) {
                        repo.deleteOrderLocally(date, retailer.getId());
                    }
                } else {
                    DocumentSnapshot doc = dc.getDocument();
                    String notes = doc.getString("notes");
                    Long remoteUpdatedAt = doc.getLong("updatedAt");
                    if (remoteUpdatedAt == null) remoteUpdatedAt = doc.getLong("timestamp");

                    List<Map<String, Object>> itemsList = (List<Map<String, Object>>) doc.get("items");
                    Map<String, Integer> productCloudQuantities = new HashMap<>();
                    if (itemsList != null) {
                        for (Map<String, Object> itemMap : itemsList) {
                            String pCloudId = (String) itemMap.get("productCloudId");
                            if (pCloudId == null || pCloudId.isEmpty()) {
                                // Fallback to product name or legacy numeric ID
                                String pName = (String) itemMap.get("productName");
                                if (pName != null && !pName.isEmpty()) {
                                    pCloudId = DailyProduct.generateCloudId(pName);
                                } else {
                                    Number legacyId = (Number) itemMap.get("productId");
                                    if (legacyId != null) {
                                        DailyProduct p = repo.getProductById(legacyId.longValue());
                                        if (p != null) pCloudId = p.getCloudId();
                                    }
                                }
                            }

                            Number qty = (Number) itemMap.get("quantity");
                            if (pCloudId != null && qty != null && qty.intValue() > 0) {
                                productCloudQuantities.put(pCloudId, qty.intValue());
                            }
                        }
                    }

                    repo.saveOrderFromCloud(date, retailerCloudId, notes != null ? notes : "", productCloudQuantities, remoteUpdatedAt != null ? remoteUpdatedAt : 0);
                }
            }
        } catch (Exception ignored) {
        } finally {
            isSyncingFromRemote = false;
        }
    }

    // ── Outgoing Cloud Handlers ───────────────────────────────────────────

    public void uploadProduct(DailyProduct product) {
        if (isSyncingFromRemote || product == null) return;
        FirebaseShopAuthManager auth = FirebaseShopAuthManager.getInstance(appContext);
        if (!auth.isLoggedIn()) return;

        try {
            String cloudId = product.getCloudId();
            Map<String, Object> map = new HashMap<>();
            map.put("cloudId", cloudId);
            map.put("id", product.getId());
            map.put("name", product.getName());
            map.put("price", product.getPrice());
            map.put("isActive", product.isActive());
            map.put("sortOrder", product.getSortOrder());
            map.put("iconKey", product.getIconKey());
            map.put("colorKey", product.getColorKey());
            map.put("updatedAt", System.currentTimeMillis());

            notifyStatus(SyncStatus.SYNCING);
            FirebaseFirestore.getInstance().collection("shops").document(auth.getCurrentShopId())
                    .collection("products").document(cloudId)
                    .set(map, SetOptions.merge())
                    .addOnCompleteListener(this::updateStatusFromTask);
        } catch (Exception e) {
            notifyStatus(SyncStatus.ERROR);
        }
    }

    public void deleteProduct(String productCloudId) {
        if (isSyncingFromRemote || productCloudId == null) return;
        FirebaseShopAuthManager auth = FirebaseShopAuthManager.getInstance(appContext);
        if (!auth.isLoggedIn()) return;

        try {
            notifyStatus(SyncStatus.SYNCING);
            FirebaseFirestore.getInstance().collection("shops").document(auth.getCurrentShopId())
                    .collection("products").document(productCloudId)
                    .delete()
                    .addOnCompleteListener(this::updateStatusFromTask);
        } catch (Exception ignored) {}
    }

    public void deleteProduct(long productId) {
        DailyProduct p = DailyOrderRepository.getInstance(appContext).getProductById(productId);
        deleteProduct(p != null ? p.getCloudId() : String.valueOf(productId));
    }

    public void uploadRetailer(DailyRetailer retailer) {
        if (isSyncingFromRemote || retailer == null) return;
        FirebaseShopAuthManager auth = FirebaseShopAuthManager.getInstance(appContext);
        if (!auth.isLoggedIn()) return;

        try {
            DailyOrderRepository repo = DailyOrderRepository.getInstance(appContext);
            String cloudId = retailer.getCloudId();

            List<String> preferredProductCloudIds = new ArrayList<>();
            for (long pId : retailer.getPreferredProductIdList()) {
                DailyProduct p = repo.getProductById(pId);
                if (p != null) {
                    preferredProductCloudIds.add(p.getCloudId());
                }
            }

            Map<String, Object> map = new HashMap<>();
            map.put("cloudId", cloudId);
            map.put("id", retailer.getId());
            map.put("name", retailer.getName());
            map.put("phone", retailer.getPhone());
            map.put("address", retailer.getAddress());
            map.put("preferredProductCloudIds", preferredProductCloudIds);
            map.put("preferredProductIds", retailer.getPreferredProductIds());
            map.put("isActive", retailer.isActive());
            map.put("updatedAt", System.currentTimeMillis());

            notifyStatus(SyncStatus.SYNCING);
            FirebaseFirestore.getInstance().collection("shops").document(auth.getCurrentShopId())
                    .collection("retailers").document(cloudId)
                    .set(map, SetOptions.merge())
                    .addOnCompleteListener(this::updateStatusFromTask);
        } catch (Exception e) {
            notifyStatus(SyncStatus.ERROR);
        }
    }

    public void deleteRetailer(String retailerCloudId) {
        if (isSyncingFromRemote || retailerCloudId == null) return;
        FirebaseShopAuthManager auth = FirebaseShopAuthManager.getInstance(appContext);
        if (!auth.isLoggedIn()) return;

        try {
            notifyStatus(SyncStatus.SYNCING);
            FirebaseFirestore.getInstance().collection("shops").document(auth.getCurrentShopId())
                    .collection("retailers").document(retailerCloudId)
                    .delete()
                    .addOnCompleteListener(this::updateStatusFromTask);
        } catch (Exception ignored) {}
    }

    public void deleteRetailer(long retailerId) {
        DailyRetailer r = DailyOrderRepository.getInstance(appContext).getRetailerById(retailerId);
        deleteRetailer(r != null ? r.getCloudId() : String.valueOf(retailerId));
    }

    public void uploadDailyOrder(DailyOrder order) {
        if (isSyncingFromRemote || order == null) return;
        FirebaseShopAuthManager auth = FirebaseShopAuthManager.getInstance(appContext);
        if (!auth.isLoggedIn()) return;

        try {
            DailyOrderRepository repo = DailyOrderRepository.getInstance(appContext);
            String date = order.getOrderDate();
            DailyRetailer r = repo.getRetailerById(order.getRetailerId());
            String retailerCloudId = (r != null) ? r.getCloudId() : DailyRetailer.generateCloudId(order.getRetailerName());

            Map<String, Object> map = new HashMap<>();
            map.put("retailerCloudId", retailerCloudId);
            map.put("retailerId", order.getRetailerId());
            map.put("retailerName", order.getRetailerName());
            map.put("orderDate", date);
            map.put("notes", order.getNotes());
            map.put("timestamp", order.getTimestamp());
            map.put("updatedAt", System.currentTimeMillis());
            map.put("totalUnits", order.getTotalUnits());

            List<Map<String, Object>> itemsList = new ArrayList<>();
            if (order.getItems() != null) {
                for (DailyOrderItem item : order.getItems()) {
                    if (item.getQuantity() > 0) {
                        DailyProduct p = repo.getProductById(item.getProductId());
                        String pCloudId = (p != null) ? p.getCloudId() : DailyProduct.generateCloudId(item.getProductName());

                        Map<String, Object> itemMap = new HashMap<>();
                        itemMap.put("productCloudId", pCloudId);
                        itemMap.put("productId", item.getProductId());
                        itemMap.put("productName", item.getProductName());
                        itemMap.put("quantity", item.getQuantity());
                        itemMap.put("unitPrice", item.getUnitPrice());
                        itemsList.add(itemMap);
                    }
                }
            }
            map.put("items", itemsList);

            notifyStatus(SyncStatus.SYNCING);
            DocumentReference dateRef = FirebaseFirestore.getInstance().collection("shops")
                    .document(auth.getCurrentShopId()).collection("dailyOrders").document(date);
            dateRef.set(java.util.Collections.singletonMap("date", date), SetOptions.merge());
            dateRef
                    .collection("retailers").document(retailerCloudId)
                    .set(map, SetOptions.merge())
                    .addOnCompleteListener(this::updateStatusFromTask);
        } catch (Exception e) {
            notifyStatus(SyncStatus.ERROR);
        }
    }

    public void deleteDailyOrder(String date, String retailerCloudId) {
        if (isSyncingFromRemote || retailerCloudId == null) return;
        FirebaseShopAuthManager auth = FirebaseShopAuthManager.getInstance(appContext);
        if (!auth.isLoggedIn()) return;

        try {
            notifyStatus(SyncStatus.SYNCING);
            FirebaseFirestore.getInstance().collection("shops").document(auth.getCurrentShopId())
                    .collection("dailyOrders").document(date)
                    .collection("retailers").document(retailerCloudId)
                    .delete()
                    .addOnCompleteListener(this::updateStatusFromTask);
        } catch (Exception ignored) {}
    }

    public void deleteDailyOrder(String date, long retailerId) {
        DailyRetailer r = DailyOrderRepository.getInstance(appContext).getRetailerById(retailerId);
        String retailerCloudId = (r != null) ? r.getCloudId() : String.valueOf(retailerId);
        deleteDailyOrder(date, retailerCloudId);
    }

    // ── Safe Data Migration ───────────────────────────────────────────────

    /**
     * Inspects existing local Products, Retailers and Orders.
     * If cloud database is fresh/empty for this shop, uploads all local records safely.
     * Never deletes local data.
     */
    private void migrateLocalDataIfEmpty(String shopId) {
        try {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("shops").document(shopId).collection("products").limit(1).get()
                    .addOnSuccessListener(snap -> {
                        if (snap.isEmpty()) {
                            DailyOrderRepository repo = DailyOrderRepository.getInstance(appContext);

                            // Migrate Products
                            List<DailyProduct> products = repo.getProducts(false);
                            for (DailyProduct p : products) {
                                uploadProduct(p);
                            }

                            // Migrate Retailers
                            List<DailyRetailer> retailers = repo.getRetailers(false);
                            for (DailyRetailer r : retailers) {
                                uploadRetailer(r);
                            }

                            // Migrate Retained Orders (last 7 days)
                            List<String> retainedDatesToMigrate = DailyOrderRepository.getLastNDates(DailyOrderRepository.ORDER_RETENTION_DAYS);
                            for (String d : retainedDatesToMigrate) {
                                List<DailyOrder> orders = repo.getOrdersForDate(d);
                                for (DailyOrder o : orders) {
                                    uploadDailyOrder(o);
                                }
                            }
                        }
                    });
        } catch (Exception ignored) {}
    }

    /**
     * Enforces the Firestore retention requirement:
     * Daily orders retain only the latest 7 days (today + previous 6 days) in cloud storage.
     * Products and retailers are permanent and never deleted.
     */
    private void purgeOldFirestoreOrders(String shopId) {
        try {
            String cutoffDate = DailyOrderRepository.getRetentionCutoffDate();
            FirebaseFirestore.getInstance().collection("shops").document(shopId)
                    .collection("dailyOrders")
                    .get()
                    .addOnSuccessListener(oldOrders -> {
                        for (DocumentSnapshot dateDoc : oldOrders.getDocuments()) {
                            String orderDate = dateDoc.getId();
                            if (orderDate.compareTo(cutoffDate) < 0) {
                                // Firestore does not cascade deletes into subcollections.
                                dateDoc.getReference().collection("retailers").get()
                                        .addOnSuccessListener(retailers -> {
                                            for (DocumentSnapshot order : retailers.getDocuments()) {
                                                order.getReference().delete().addOnFailureListener(e -> notifyStatus(
                                                        isNetworkAvailable ? SyncStatus.ERROR : SyncStatus.OFFLINE));
                                            }
                                            dateDoc.getReference().delete().addOnFailureListener(e -> notifyStatus(
                                                    isNetworkAvailable ? SyncStatus.ERROR : SyncStatus.OFFLINE));
                                        })
                                        .addOnFailureListener(e -> notifyStatus(
                                                isNetworkAvailable ? SyncStatus.ERROR : SyncStatus.OFFLINE));
                            }
                        }
                    })
                    .addOnFailureListener(e -> notifyStatus(
                            isNetworkAvailable ? SyncStatus.ERROR : SyncStatus.OFFLINE));
        } catch (Exception ignored) {}
    }
}
