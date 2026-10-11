package com.orderanalyzer.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.orderanalyzer.model.DailyOrder;
import com.orderanalyzer.model.DailyOrderItem;
import com.orderanalyzer.model.DailyProduct;
import com.orderanalyzer.model.DailyRetailer;
import com.orderanalyzer.model.DashboardMetrics;
import com.orderanalyzer.model.ProductTotalItem;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DailyOrderRepository {

    private static DailyOrderRepository instance;
    private final Context appContext;
    private final DailyOrderDbHelper dbHelper;

    private DailyOrderRepository(Context context) {
        this.appContext = context.getApplicationContext();
        this.dbHelper = new DailyOrderDbHelper(this.appContext);
        // Retain 7 days of daily orders locally (today + previous 6 days); older orders are automatically purged.
        purgeOldOrders();
    }

    public static synchronized DailyOrderRepository getInstance(Context context) {
        if (instance == null) {
            instance = new DailyOrderRepository(context);
        }
        return instance;
    }

    // ── Date Formatting Utilities ─────────────────────────────────────────

    public static final int ORDER_RETENTION_DAYS = 7;

    public static String getTodayDate() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    }

    public static String getYesterdayDate() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -1);
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.getTime());
    }

    /**
     * Returns a list of the last N dates (today + N-1 previous days),
     * most recent first. Used for the retained date-history selector.
     */
    public static List<String> getLastNDates(int n) {
        List<String> dates = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        Calendar cal = Calendar.getInstance();
        for (int i = 0; i < n; i++) {
            dates.add(sdf.format(cal.getTime()));
            cal.add(Calendar.DAY_OF_YEAR, -1);
        }
        return dates;
    }

    /** Returns date that is 'daysAgo' days before today */
    public static String getDateDaysAgo(int daysAgo) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -daysAgo);
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.getTime());
    }

    /**
     * Returns the cutoff date string (oldest retained date, 6 days before today).
     * Any daily order with order_date < getRetentionCutoffDate() is older than 7 days and expires.
     */
    public static String getRetentionCutoffDate() {
        return getDateDaysAgo(ORDER_RETENTION_DAYS - 1);
    }

    public static String formatDisplayDate(String dateStr) {
        try {
            Date d = new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dateStr);
            if (d != null) {
                return new SimpleDateFormat("EEE, dd MMM yyyy", Locale.US).format(d);
            }
        } catch (Exception ignored) {}
        return dateStr;
    }

    /** Human-friendly label for a date relative to today */
    public static String getRelativeLabel(String dateStr) {
        String today = getTodayDate();
        String yesterday = getYesterdayDate();
        if (dateStr.equals(today)) return "Today";
        if (dateStr.equals(yesterday)) return "Yesterday";
        try {
            Date d = new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dateStr);
            if (d != null) {
                return new SimpleDateFormat("dd MMM", Locale.US).format(d);
            }
        } catch (Exception ignored) {}
        return dateStr;
    }

    // ── 7-Day Rolling Window Purge ────────────────────────────────────────

    public void purgeOldOrders() {
        try {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            // Retain today + previous 6 days (7 days total); do not touch the permanent product/retailer catalogs.
            String cutoffDate = getRetentionCutoffDate();
            db.delete("daily_orders", "order_date < ?", new String[]{cutoffDate});
            // ON DELETE CASCADE on daily_order_items handles orphan cleanup automatically
        } catch (Exception ignored) {}
    }

    // ── Products Master ───────────────────────────────────────────────────

    public List<DailyProduct> getProducts(boolean activeOnly) {
        List<DailyProduct> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String selection = activeOnly ? "is_active = 1" : null;
        Cursor c = db.query("products", null, selection, null, null, null, "sort_order ASC, name ASC");

        if (c != null) {
            while (c.moveToNext()) {
                double price = 0;
                int priceIdx = c.getColumnIndex("price");
                if (priceIdx >= 0) price = c.getDouble(priceIdx);

                int cloudIdx = c.getColumnIndex("cloud_id");
                String cloudId = (cloudIdx >= 0 && !c.isNull(cloudIdx)) ? c.getString(cloudIdx) : null;

                DailyProduct p = new DailyProduct(
                        c.getLong(c.getColumnIndexOrThrow("id")),
                        cloudId,
                        c.getString(c.getColumnIndexOrThrow("name")),
                        c.getInt(c.getColumnIndexOrThrow("is_active")) == 1,
                        c.getInt(c.getColumnIndexOrThrow("sort_order")),
                        c.getString(c.getColumnIndexOrThrow("icon_key")),
                        c.getString(c.getColumnIndexOrThrow("color_key")),
                        price
                );
                list.add(p);
            }
            c.close();
        }
        return list;
    }

    public DailyProduct getProductById(long id) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.query("products", null, "id = ?", new String[]{String.valueOf(id)}, null, null, null);
        DailyProduct p = null;
        if (c != null && c.moveToFirst()) {
            double price = 0;
            int priceIdx = c.getColumnIndex("price");
            if (priceIdx >= 0) price = c.getDouble(priceIdx);
            int cloudIdx = c.getColumnIndex("cloud_id");
            String cloudId = (cloudIdx >= 0 && !c.isNull(cloudIdx)) ? c.getString(cloudIdx) : null;
            p = new DailyProduct(
                    c.getLong(c.getColumnIndexOrThrow("id")),
                    cloudId,
                    c.getString(c.getColumnIndexOrThrow("name")),
                    c.getInt(c.getColumnIndexOrThrow("is_active")) == 1,
                    c.getInt(c.getColumnIndexOrThrow("sort_order")),
                    c.getString(c.getColumnIndexOrThrow("icon_key")),
                    c.getString(c.getColumnIndexOrThrow("color_key")),
                    price
            );
            c.close();
        }
        return p;
    }

    public DailyProduct getProductByCloudId(String cloudId) {
        if (cloudId == null || cloudId.trim().isEmpty()) return null;
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.query("products", null, "cloud_id = ?", new String[]{cloudId.trim()}, null, null, null);
        DailyProduct p = null;
        if (c != null && c.moveToFirst()) {
            double price = 0;
            int priceIdx = c.getColumnIndex("price");
            if (priceIdx >= 0) price = c.getDouble(priceIdx);
            int cloudIdx = c.getColumnIndex("cloud_id");
            String cId = (cloudIdx >= 0 && !c.isNull(cloudIdx)) ? c.getString(cloudIdx) : cloudId;
            p = new DailyProduct(
                    c.getLong(c.getColumnIndexOrThrow("id")),
                    cId,
                    c.getString(c.getColumnIndexOrThrow("name")),
                    c.getInt(c.getColumnIndexOrThrow("is_active")) == 1,
                    c.getInt(c.getColumnIndexOrThrow("sort_order")),
                    c.getString(c.getColumnIndexOrThrow("icon_key")),
                    c.getString(c.getColumnIndexOrThrow("color_key")),
                    price
            );
            c.close();
        }
        return p;
    }

    public DailyProduct getProductByName(String name) {
        if (name == null || name.trim().isEmpty()) return null;
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.query("products", null, "name = ?", new String[]{name.trim()}, null, null, null);
        DailyProduct p = null;
        if (c != null && c.moveToFirst()) {
            double price = 0;
            int priceIdx = c.getColumnIndex("price");
            if (priceIdx >= 0) price = c.getDouble(priceIdx);
            int cloudIdx = c.getColumnIndex("cloud_id");
            String cId = (cloudIdx >= 0 && !c.isNull(cloudIdx)) ? c.getString(cloudIdx) : null;
            p = new DailyProduct(
                    c.getLong(c.getColumnIndexOrThrow("id")),
                    cId,
                    c.getString(c.getColumnIndexOrThrow("name")),
                    c.getInt(c.getColumnIndexOrThrow("is_active")) == 1,
                    c.getInt(c.getColumnIndexOrThrow("sort_order")),
                    c.getString(c.getColumnIndexOrThrow("icon_key")),
                    c.getString(c.getColumnIndexOrThrow("color_key")),
                    price
            );
            c.close();
        }
        return p;
    }

    public long addProduct(String name) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        String trimmed = name.trim();
        cv.put("cloud_id", DailyProduct.generateCloudId(trimmed));
        cv.put("name", trimmed);
        cv.put("is_active", 1);
        cv.put("sort_order", 99);
        cv.put("icon_key", "📦");
        cv.put("color_key", "blue");
        cv.put("price", 0.0);
        long id = db.insertWithOnConflict("products", null, cv, SQLiteDatabase.CONFLICT_IGNORE);
        if (id > 0) {
            DailyProduct p = getProductById(id);
            if (p != null) {
                com.orderanalyzer.firebase.FirebaseSyncManager.getInstance(appContext).uploadProduct(p);
            }
        }
        return id;
    }

    public void updateProductName(long id, String newName) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("name", newName.trim());
        db.update("products", cv, "id = ?", new String[]{String.valueOf(id)});
        DailyProduct p = getProductById(id);
        if (p != null) {
            com.orderanalyzer.firebase.FirebaseSyncManager.getInstance(appContext).uploadProduct(p);
        }
    }

    public void updateProductPrice(long id, double price) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("price", price);
        db.update("products", cv, "id = ?", new String[]{String.valueOf(id)});
        DailyProduct p = getProductById(id);
        if (p != null) {
            com.orderanalyzer.firebase.FirebaseSyncManager.getInstance(appContext).uploadProduct(p);
        }
    }

    public void setProductActive(long id, boolean isActive) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("is_active", isActive ? 1 : 0);
        db.update("products", cv, "id = ?", new String[]{String.valueOf(id)});
        DailyProduct p = getProductById(id);
        if (p != null) {
            com.orderanalyzer.firebase.FirebaseSyncManager.getInstance(appContext).uploadProduct(p);
        }
    }

    public boolean deleteProductLocally(long id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete("daily_order_items", "product_id = ?", new String[]{String.valueOf(id)});
        int rows = db.delete("products", "id = ?", new String[]{String.valueOf(id)});
        return rows > 0;
    }

    public boolean deleteProduct(long id) {
        DailyProduct existing = getProductById(id);
        boolean deleted = deleteProductLocally(id);
        if (deleted && existing != null) {
            com.orderanalyzer.firebase.FirebaseSyncManager.getInstance(appContext).deleteProduct(existing.getCloudId());
        }
        return deleted;
    }

    public long saveProductFromCloud(String cloudId, String name, double price, boolean isActive, int sortOrder, String iconKey, String colorKey) {
        if (name == null || name.trim().isEmpty()) return -1;
        String cleanName = name.trim();
        String cId = (cloudId != null && !cloudId.trim().isEmpty()) ? cloudId.trim() : DailyProduct.generateCloudId(cleanName);

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        DailyProduct existing = getProductByCloudId(cId);
        if (existing == null) {
            existing = getProductByName(cleanName);
        }

        ContentValues cv = new ContentValues();
        cv.put("cloud_id", cId);
        cv.put("name", cleanName);
        cv.put("price", price);
        cv.put("is_active", isActive ? 1 : 0);
        cv.put("sort_order", sortOrder);
        if (iconKey != null) cv.put("icon_key", iconKey);
        if (colorKey != null) cv.put("color_key", colorKey);

        if (existing != null) {
            db.update("products", cv, "id = ?", new String[]{String.valueOf(existing.getId())});
            return existing.getId();
        } else {
            return db.insertWithOnConflict("products", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
        }
    }

    // ── Retailers Master ──────────────────────────────────────────────────

    public List<DailyRetailer> getRetailers(boolean activeOnly) {
        List<DailyRetailer> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String selection = activeOnly ? "is_active = 1" : null;
        Cursor c = db.query("retailers", null, selection, null, null, null, "name ASC");

        if (c != null) {
            while (c.moveToNext()) {
                int cloudIdx = c.getColumnIndex("cloud_id");
                String cloudId = (cloudIdx >= 0 && !c.isNull(cloudIdx)) ? c.getString(cloudIdx) : null;
                DailyRetailer r = new DailyRetailer(
                        c.getLong(c.getColumnIndexOrThrow("id")),
                        cloudId,
                        c.getString(c.getColumnIndexOrThrow("name")),
                        c.getString(c.getColumnIndexOrThrow("phone")),
                        c.getString(c.getColumnIndexOrThrow("address")),
                        c.getString(c.getColumnIndexOrThrow("preferred_product_ids")),
                        c.getInt(c.getColumnIndexOrThrow("is_active")) == 1
                );
                list.add(r);
            }
            c.close();
        }
        return list;
    }

    public DailyRetailer getRetailerById(long id) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.query("retailers", null, "id = ?", new String[]{String.valueOf(id)}, null, null, null);
        DailyRetailer r = null;
        if (c != null && c.moveToFirst()) {
            int cloudIdx = c.getColumnIndex("cloud_id");
            String cloudId = (cloudIdx >= 0 && !c.isNull(cloudIdx)) ? c.getString(cloudIdx) : null;
            r = new DailyRetailer(
                    c.getLong(c.getColumnIndexOrThrow("id")),
                    cloudId,
                    c.getString(c.getColumnIndexOrThrow("name")),
                    c.getString(c.getColumnIndexOrThrow("phone")),
                    c.getString(c.getColumnIndexOrThrow("address")),
                    c.getString(c.getColumnIndexOrThrow("preferred_product_ids")),
                    c.getInt(c.getColumnIndexOrThrow("is_active")) == 1
            );
            c.close();
        }
        return r;
    }

    public DailyRetailer getRetailerByCloudId(String cloudId) {
        if (cloudId == null || cloudId.trim().isEmpty()) return null;
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.query("retailers", null, "cloud_id = ?", new String[]{cloudId.trim()}, null, null, null);
        DailyRetailer r = null;
        if (c != null && c.moveToFirst()) {
            int cloudIdx = c.getColumnIndex("cloud_id");
            String cId = (cloudIdx >= 0 && !c.isNull(cloudIdx)) ? c.getString(cloudIdx) : cloudId;
            r = new DailyRetailer(
                    c.getLong(c.getColumnIndexOrThrow("id")),
                    cId,
                    c.getString(c.getColumnIndexOrThrow("name")),
                    c.getString(c.getColumnIndexOrThrow("phone")),
                    c.getString(c.getColumnIndexOrThrow("address")),
                    c.getString(c.getColumnIndexOrThrow("preferred_product_ids")),
                    c.getInt(c.getColumnIndexOrThrow("is_active")) == 1
            );
            c.close();
        }
        return r;
    }

    public DailyRetailer getRetailerByName(String name) {
        if (name == null || name.trim().isEmpty()) return null;
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.query("retailers", null, "name = ?", new String[]{name.trim()}, null, null, null);
        DailyRetailer r = null;
        if (c != null && c.moveToFirst()) {
            int cloudIdx = c.getColumnIndex("cloud_id");
            String cId = (cloudIdx >= 0 && !c.isNull(cloudIdx)) ? c.getString(cloudIdx) : null;
            r = new DailyRetailer(
                    c.getLong(c.getColumnIndexOrThrow("id")),
                    cId,
                    c.getString(c.getColumnIndexOrThrow("name")),
                    c.getString(c.getColumnIndexOrThrow("phone")),
                    c.getString(c.getColumnIndexOrThrow("address")),
                    c.getString(c.getColumnIndexOrThrow("preferred_product_ids")),
                    c.getInt(c.getColumnIndexOrThrow("is_active")) == 1
            );
            c.close();
        }
        return r;
    }

    public long addRetailer(String name, String phone, String address, String preferredIds) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        String trimmed = name.trim();
        cv.put("cloud_id", DailyRetailer.generateCloudId(trimmed));
        cv.put("name", trimmed);
        cv.put("phone", phone != null ? phone.trim() : "");
        cv.put("address", address != null ? address.trim() : "");
        cv.put("preferred_product_ids", preferredIds != null ? preferredIds : "");
        cv.put("is_active", 1);
        long id = db.insertWithOnConflict("retailers", null, cv, SQLiteDatabase.CONFLICT_IGNORE);
        if (id > 0) {
            DailyRetailer r = getRetailerById(id);
            if (r != null) {
                com.orderanalyzer.firebase.FirebaseSyncManager.getInstance(appContext).uploadRetailer(r);
            }
        }
        return id;
    }

    public void updateRetailer(long id, String name, String phone, String address, String preferredIds, boolean isActive) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("name", name.trim());
        cv.put("phone", phone != null ? phone.trim() : "");
        cv.put("address", address != null ? address.trim() : "");
        cv.put("preferred_product_ids", preferredIds != null ? preferredIds : "");
        cv.put("is_active", isActive ? 1 : 0);
        db.update("retailers", cv, "id = ?", new String[]{String.valueOf(id)});
        DailyRetailer r = getRetailerById(id);
        if (r != null) {
            com.orderanalyzer.firebase.FirebaseSyncManager.getInstance(appContext).uploadRetailer(r);
        }
    }

    public void setRetailerActive(long id, boolean isActive) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("is_active", isActive ? 1 : 0);
        db.update("retailers", cv, "id = ?", new String[]{String.valueOf(id)});
        DailyRetailer r = getRetailerById(id);
        if (r != null) {
            com.orderanalyzer.firebase.FirebaseSyncManager.getInstance(appContext).uploadRetailer(r);
        }
    }

    public boolean deleteRetailerLocally(long id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        Cursor c = db.query("daily_orders", new String[]{"id"}, "retailer_id = ?", new String[]{String.valueOf(id)}, null, null, null);
        if (c != null) {
            while (c.moveToNext()) {
                long orderId = c.getLong(0);
                db.delete("daily_order_items", "order_id = ?", new String[]{String.valueOf(orderId)});
            }
            c.close();
        }
        db.delete("daily_orders", "retailer_id = ?", new String[]{String.valueOf(id)});
        int rows = db.delete("retailers", "id = ?", new String[]{String.valueOf(id)});
        return rows > 0;
    }

    public boolean deleteRetailer(long id) {
        DailyRetailer existing = getRetailerById(id);
        boolean deleted = deleteRetailerLocally(id);
        if (deleted && existing != null) {
            com.orderanalyzer.firebase.FirebaseSyncManager.getInstance(appContext).deleteRetailer(existing.getCloudId());
        }
        return deleted;
    }

    public long saveRetailerFromCloud(String cloudId, String name, String phone, String address, String preferredIds, boolean isActive) {
        if (name == null || name.trim().isEmpty()) return -1;
        String cleanName = name.trim();
        String cId = (cloudId != null && !cloudId.trim().isEmpty()) ? cloudId.trim() : DailyRetailer.generateCloudId(cleanName);

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        DailyRetailer existing = getRetailerByCloudId(cId);
        if (existing == null) {
            existing = getRetailerByName(cleanName);
        }

        ContentValues cv = new ContentValues();
        cv.put("cloud_id", cId);
        cv.put("name", cleanName);
        cv.put("phone", phone != null ? phone.trim() : "");
        cv.put("address", address != null ? address.trim() : "");
        cv.put("preferred_product_ids", preferredIds != null ? preferredIds : "");
        cv.put("is_active", isActive ? 1 : 0);

        if (existing != null) {
            db.update("retailers", cv, "id = ?", new String[]{String.valueOf(existing.getId())});
            return existing.getId();
        } else {
            return db.insertWithOnConflict("retailers", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
        }
    }

    public List<DailyRetailer> getPendingRetailersForDate(String date) {
        List<DailyRetailer> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String sql = "SELECT id, name, phone, address, preferred_product_ids, is_active " +
                "FROM retailers " +
                "WHERE is_active = 1 AND id NOT IN (" +
                "    SELECT retailer_id FROM daily_orders WHERE order_date = ?" +
                ") ORDER BY name ASC";
        Cursor c = db.rawQuery(sql, new String[]{date});
        if (c != null) {
            while (c.moveToNext()) {
                list.add(new DailyRetailer(
                        c.getLong(c.getColumnIndexOrThrow("id")),
                        c.getString(c.getColumnIndexOrThrow("name")),
                        c.getString(c.getColumnIndexOrThrow("phone")),
                        c.getString(c.getColumnIndexOrThrow("address")),
                        c.getString(c.getColumnIndexOrThrow("preferred_product_ids")),
                        c.getInt(c.getColumnIndexOrThrow("is_active")) == 1
                ));
            }
            c.close();
        }
        return list;
    }

    // ── Orders & Order Items ──────────────────────────────────────────────

    public DailyOrder getOrderForRetailer(String date, long retailerId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String sql = "SELECT o.id, o.order_date, o.retailer_id, o.timestamp, o.notes, " +
                "r.name as retailer_name, r.phone as retailer_phone, r.address as retailer_address " +
                "FROM daily_orders o JOIN retailers r ON o.retailer_id = r.id " +
                "WHERE o.order_date = ? AND o.retailer_id = ?";
        Cursor c = db.rawQuery(sql, new String[]{date, String.valueOf(retailerId)});
        DailyOrder order = null;
        if (c != null && c.moveToFirst()) {
            order = new DailyOrder();
            order.setId(c.getLong(c.getColumnIndexOrThrow("id")));
            order.setOrderDate(c.getString(c.getColumnIndexOrThrow("order_date")));
            order.setRetailerId(c.getLong(c.getColumnIndexOrThrow("retailer_id")));
            order.setTimestamp(c.getLong(c.getColumnIndexOrThrow("timestamp")));
            order.setNotes(c.getString(c.getColumnIndexOrThrow("notes")));
            order.setRetailerName(c.getString(c.getColumnIndexOrThrow("retailer_name")));
            order.setRetailerPhone(c.getString(c.getColumnIndexOrThrow("retailer_phone")));
            order.setRetailerAddress(c.getString(c.getColumnIndexOrThrow("retailer_address")));
            c.close();
            order.setItems(getOrderItems(order.getId()));
        }
        return order;
    }

    public DailyOrder getOrderById(long orderId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String sql = "SELECT o.id, o.order_date, o.retailer_id, o.timestamp, o.notes, " +
                "r.name as retailer_name, r.phone as retailer_phone, r.address as retailer_address " +
                "FROM daily_orders o JOIN retailers r ON o.retailer_id = r.id " +
                "WHERE o.id = ?";
        Cursor c = db.rawQuery(sql, new String[]{String.valueOf(orderId)});
        DailyOrder order = null;
        if (c != null && c.moveToFirst()) {
            order = new DailyOrder();
            order.setId(c.getLong(c.getColumnIndexOrThrow("id")));
            order.setOrderDate(c.getString(c.getColumnIndexOrThrow("order_date")));
            order.setRetailerId(c.getLong(c.getColumnIndexOrThrow("retailer_id")));
            order.setTimestamp(c.getLong(c.getColumnIndexOrThrow("timestamp")));
            order.setNotes(c.getString(c.getColumnIndexOrThrow("notes")));
            order.setRetailerName(c.getString(c.getColumnIndexOrThrow("retailer_name")));
            order.setRetailerPhone(c.getString(c.getColumnIndexOrThrow("retailer_phone")));
            order.setRetailerAddress(c.getString(c.getColumnIndexOrThrow("retailer_address")));
            c.close();
            order.setItems(getOrderItems(order.getId()));
        }
        return order;
    }

    /** Fetches order items joining with current product price */
    public List<DailyOrderItem> getOrderItems(long orderId) {
        List<DailyOrderItem> items = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String sql = "SELECT oi.order_id, oi.product_id, oi.quantity, p.name as product_name, " +
                "COALESCE(p.price, 0) as unit_price " +
                "FROM daily_order_items oi JOIN products p ON oi.product_id = p.id " +
                "WHERE oi.order_id = ? ORDER BY oi.quantity > 0 DESC, p.sort_order ASC";
        Cursor c = db.rawQuery(sql, new String[]{String.valueOf(orderId)});
        if (c != null) {
            while (c.moveToNext()) {
                double unitPrice = 0;
                int priceIdx = c.getColumnIndex("unit_price");
                if (priceIdx >= 0) unitPrice = c.getDouble(priceIdx);

                DailyOrderItem item = new DailyOrderItem(
                        c.getLong(c.getColumnIndexOrThrow("order_id")),
                        c.getLong(c.getColumnIndexOrThrow("product_id")),
                        c.getString(c.getColumnIndexOrThrow("product_name")),
                        c.getInt(c.getColumnIndexOrThrow("quantity")),
                        unitPrice
                );
                items.add(item);
            }
            c.close();
        }
        return items;
    }

    public long saveOrder(String date, long retailerId, String notes, Map<Long, Integer> productQuantities) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        long orderId = -1;
        try {
            int totalUnits = 0;
            if (productQuantities != null) {
                for (int q : productQuantities.values()) totalUnits += q;
            }

            // Check if existing order exists
            Cursor c = db.rawQuery("SELECT id FROM daily_orders WHERE order_date = ? AND retailer_id = ?",
                    new String[]{date, String.valueOf(retailerId)});
            if (c != null && c.moveToFirst()) {
                orderId = c.getLong(0);
                c.close();
            }

            if (totalUnits == 0) {
                // If total units is 0 and order existed, delete it
                if (orderId != -1) {
                    db.delete("daily_order_items", "order_id = ?", new String[]{String.valueOf(orderId)});
                    db.delete("daily_orders", "id = ?", new String[]{String.valueOf(orderId)});
                    com.orderanalyzer.firebase.FirebaseSyncManager.getInstance(appContext).deleteDailyOrder(date, retailerId);
                }
                db.setTransactionSuccessful();
                return -1;
            }

            ContentValues cv = new ContentValues();
            cv.put("order_date", date);
            cv.put("retailer_id", retailerId);
            cv.put("timestamp", System.currentTimeMillis());
            cv.put("notes", notes != null ? notes.trim() : "");

            if (orderId == -1) {
                orderId = db.insert("daily_orders", null, cv);
            } else {
                db.update("daily_orders", cv, "id = ?", new String[]{String.valueOf(orderId)});
                db.delete("daily_order_items", "order_id = ?", new String[]{String.valueOf(orderId)});
            }

            if (productQuantities != null) {
                for (Map.Entry<Long, Integer> entry : productQuantities.entrySet()) {
                    if (entry.getValue() > 0) {
                        ContentValues itemCv = new ContentValues();
                        itemCv.put("order_id", orderId);
                        itemCv.put("product_id", entry.getKey());
                        itemCv.put("quantity", entry.getValue());
                        db.insert("daily_order_items", null, itemCv);
                    }
                }
            }

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        if (orderId > 0) {
            DailyOrder saved = getOrderById(orderId);
            if (saved != null) {
                com.orderanalyzer.firebase.FirebaseSyncManager.getInstance(appContext).uploadDailyOrder(saved);
            }
        }
        return orderId;
    }

    public void deleteOrderLocally(String date, long retailerId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        Cursor c = db.rawQuery("SELECT id FROM daily_orders WHERE order_date = ? AND retailer_id = ?",
                new String[]{date, String.valueOf(retailerId)});
        if (c != null && c.moveToFirst()) {
            long orderId = c.getLong(0);
            c.close();
            db.delete("daily_order_items", "order_id = ?", new String[]{String.valueOf(orderId)});
            db.delete("daily_orders", "id = ?", new String[]{String.valueOf(orderId)});
        }
    }

    public void deleteOrder(long orderId) {
        DailyOrder existing = getOrderById(orderId);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete("daily_order_items", "order_id = ?", new String[]{String.valueOf(orderId)});
        db.delete("daily_orders", "id = ?", new String[]{String.valueOf(orderId)});
        if (existing != null) {
            com.orderanalyzer.firebase.FirebaseSyncManager.getInstance(appContext)
                    .deleteDailyOrder(existing.getOrderDate(), existing.getRetailerId());
        }
    }

    public long saveOrderFromCloud(String date, String retailerCloudId, String notes, Map<String, Integer> productCloudQuantities, long remoteTimestamp) {
        DailyRetailer retailer = getRetailerByCloudId(retailerCloudId);
        if (retailer == null) return -1;
        long retailerId = retailer.getId();

        DailyOrder localOrder = getOrderForRetailer(date, retailerId);
        if (localOrder != null && localOrder.getTimestamp() >= remoteTimestamp) {
            // Local version is newer or identical, don't overwrite
            return localOrder.getId();
        }

        // Map productCloudId -> local productId
        java.util.Map<Long, Integer> localProductQuantities = new java.util.HashMap<>();
        if (productCloudQuantities != null) {
            for (java.util.Map.Entry<String, Integer> entry : productCloudQuantities.entrySet()) {
                String pCloudId = entry.getKey();
                int qty = entry.getValue() != null ? entry.getValue() : 0;
                if (qty > 0) {
                    DailyProduct p = getProductByCloudId(pCloudId);
                    if (p != null) {
                        localProductQuantities.put(p.getId(), qty);
                    }
                }
            }
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        long orderId = -1;
        try {
            if (localProductQuantities.isEmpty()) {
                if (localOrder != null) {
                    db.delete("daily_order_items", "order_id = ?", new String[]{String.valueOf(localOrder.getId())});
                    db.delete("daily_orders", "id = ?", new String[]{String.valueOf(localOrder.getId())});
                }
                db.setTransactionSuccessful();
                return -1;
            }

            ContentValues cv = new ContentValues();
            cv.put("order_date", date);
            cv.put("retailer_id", retailerId);
            cv.put("timestamp", remoteTimestamp > 0 ? remoteTimestamp : System.currentTimeMillis());
            cv.put("notes", notes != null ? notes.trim() : "");

            if (localOrder == null) {
                orderId = db.insert("daily_orders", null, cv);
            } else {
                orderId = localOrder.getId();
                db.update("daily_orders", cv, "id = ?", new String[]{String.valueOf(orderId)});
                db.delete("daily_order_items", "order_id = ?", new String[]{String.valueOf(orderId)});
            }

            for (java.util.Map.Entry<Long, Integer> entry : localProductQuantities.entrySet()) {
                ContentValues itemCv = new ContentValues();
                itemCv.put("order_id", orderId);
                itemCv.put("product_id", entry.getKey());
                itemCv.put("quantity", entry.getValue());
                db.insert("daily_order_items", null, itemCv);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        return orderId;
    }

    public List<DailyOrder> getOrdersForDate(String date) {
        List<DailyOrder> orders = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String sql = "SELECT o.id, o.order_date, o.retailer_id, o.timestamp, o.notes, " +
                "r.name as retailer_name, r.phone as retailer_phone, r.address as retailer_address " +
                "FROM daily_orders o JOIN retailers r ON o.retailer_id = r.id " +
                "WHERE o.order_date = ? ORDER BY o.timestamp DESC";
        Cursor c = db.rawQuery(sql, new String[]{date});
        if (c != null) {
            while (c.moveToNext()) {
                DailyOrder order = new DailyOrder();
                order.setId(c.getLong(c.getColumnIndexOrThrow("id")));
                order.setOrderDate(c.getString(c.getColumnIndexOrThrow("order_date")));
                order.setRetailerId(c.getLong(c.getColumnIndexOrThrow("retailer_id")));
                order.setTimestamp(c.getLong(c.getColumnIndexOrThrow("timestamp")));
                order.setNotes(c.getString(c.getColumnIndexOrThrow("notes")));
                order.setRetailerName(c.getString(c.getColumnIndexOrThrow("retailer_name")));
                order.setRetailerPhone(c.getString(c.getColumnIndexOrThrow("retailer_phone")));
                order.setRetailerAddress(c.getString(c.getColumnIndexOrThrow("retailer_address")));
                orders.add(order);
            }
            c.close();
            for (DailyOrder order : orders) {
                order.setItems(getOrderItems(order.getId()));
            }
        }
        return orders;
    }

    public List<DailyOrder> getRecentOrdersForDate(String date, int limit) {
        List<DailyOrder> all = getOrdersForDate(date);
        if (all.size() <= limit) return all;
        return all.subList(0, limit);
    }

    // ── Calculations & Metrics ────────────────────────────────────────────

    /** Calculates total bill amount (SUM of qty * price) for a given date */
    public double getTotalBillForDate(String date) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String sql = "SELECT COALESCE(SUM(oi.quantity * COALESCE(p.price, 0)), 0) " +
                "FROM daily_order_items oi " +
                "JOIN daily_orders o ON oi.order_id = o.id " +
                "JOIN products p ON oi.product_id = p.id " +
                "WHERE o.order_date = ?";
        Cursor c = db.rawQuery(sql, new String[]{date});
        double total = 0;
        if (c != null && c.moveToFirst()) {
            total = c.getDouble(0);
            c.close();
        }
        return total;
    }

    public DashboardMetrics getDashboardMetrics(String date) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        int retailersCount = 0;
        int totalItems = 0;
        int productsOrderedCount = 0;
        int totalActiveRetailers = 0;

        // Retailers with orders count
        Cursor c1 = db.rawQuery("SELECT COUNT(*) FROM daily_orders WHERE order_date = ?", new String[]{date});
        if (c1 != null && c1.moveToFirst()) {
            retailersCount = c1.getInt(0);
            c1.close();
        }

        // Total items
        Cursor c2 = db.rawQuery("SELECT COALESCE(SUM(oi.quantity), 0) FROM daily_order_items oi " +
                "JOIN daily_orders o ON oi.order_id = o.id WHERE o.order_date = ?", new String[]{date});
        if (c2 != null && c2.moveToFirst()) {
            totalItems = c2.getInt(0);
            c2.close();
        }

        // Products ordered count
        Cursor c3 = db.rawQuery("SELECT COUNT(DISTINCT oi.product_id) FROM daily_order_items oi " +
                "JOIN daily_orders o ON oi.order_id = o.id WHERE o.order_date = ? AND oi.quantity > 0", new String[]{date});
        if (c3 != null && c3.moveToFirst()) {
            productsOrderedCount = c3.getInt(0);
            c3.close();
        }

        // Total active retailers
        Cursor c4 = db.rawQuery("SELECT COUNT(*) FROM retailers WHERE is_active = 1", null);
        if (c4 != null && c4.moveToFirst()) {
            totalActiveRetailers = c4.getInt(0);
            c4.close();
        }

        int pendingRetailers = Math.max(0, totalActiveRetailers - retailersCount);
        double totalBill = getTotalBillForDate(date);
        return new DashboardMetrics(retailersCount, totalItems, productsOrderedCount, pendingRetailers, totalBill);
    }

    /**
     * Accurately calculates Product Totals strictly for the given date.
     * Includes unit price and total amount per product.
     */
    public List<ProductTotalItem> getProductTotalsForDate(String date) {
        List<ProductTotalItem> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String sql = "SELECT p.id, p.name, p.icon_key, p.color_key, COALESCE(p.price, 0) as unit_price, " +
                "COALESCE(SUM(oi.quantity), 0) as total_qty " +
                "FROM products p " +
                "LEFT JOIN (" +
                "    SELECT oi.product_id, oi.quantity " +
                "    FROM daily_order_items oi " +
                "    JOIN daily_orders o ON oi.order_id = o.id " +
                "    WHERE o.order_date = ?" +
                ") oi ON p.id = oi.product_id " +
                "WHERE p.is_active = 1 " +
                "GROUP BY p.id " +
                "ORDER BY total_qty > 0 DESC, total_qty DESC, p.sort_order ASC";
        Cursor c = db.rawQuery(sql, new String[]{date});
        if (c != null) {
            while (c.moveToNext()) {
                double unitPrice = 0;
                int priceIdx = c.getColumnIndex("unit_price");
                if (priceIdx >= 0) unitPrice = c.getDouble(priceIdx);

                list.add(new ProductTotalItem(
                        c.getLong(c.getColumnIndexOrThrow("id")),
                        c.getString(c.getColumnIndexOrThrow("name")),
                        c.getInt(c.getColumnIndexOrThrow("total_qty")),
                        c.getString(c.getColumnIndexOrThrow("icon_key")),
                        c.getString(c.getColumnIndexOrThrow("color_key")),
                        unitPrice
                ));
            }
            c.close();
        }
        return list;
    }
}
