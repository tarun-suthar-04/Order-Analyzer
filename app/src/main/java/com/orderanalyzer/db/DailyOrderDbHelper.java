package com.orderanalyzer.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DailyOrderDbHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "daily_orders.db";
    // Version 3: Added 'cloud_id' columns to products and retailers tables for cross-device cloud sync
    private static final int DATABASE_VERSION = 3;

    public DailyOrderDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Master Products Table
        db.execSQL("CREATE TABLE products (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "cloud_id TEXT UNIQUE, " +
                "name TEXT UNIQUE NOT NULL, " +
                "is_active INTEGER NOT NULL DEFAULT 1, " +
                "sort_order INTEGER NOT NULL DEFAULT 0, " +
                "icon_key TEXT, " +
                "color_key TEXT, " +
                "price REAL NOT NULL DEFAULT 0" +
                ");");

        // Master Retailers Table
        db.execSQL("CREATE TABLE retailers (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "cloud_id TEXT UNIQUE, " +
                "name TEXT UNIQUE NOT NULL, " +
                "phone TEXT, " +
                "address TEXT, " +
                "preferred_product_ids TEXT, " +
                "is_active INTEGER NOT NULL DEFAULT 1" +
                ");");

        // Daily Orders Table (Strict 1 order per retailer per date)
        db.execSQL("CREATE TABLE daily_orders (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "order_date TEXT NOT NULL, " +
                "retailer_id INTEGER NOT NULL, " +
                "timestamp INTEGER NOT NULL, " +
                "notes TEXT, " +
                "UNIQUE(order_date, retailer_id), " +
                "FOREIGN KEY(retailer_id) REFERENCES retailers(id)" +
                ");");

        // Daily Order Items Table
        db.execSQL("CREATE TABLE daily_order_items (" +
                "order_id INTEGER NOT NULL, " +
                "product_id INTEGER NOT NULL, " +
                "quantity INTEGER NOT NULL DEFAULT 0, " +
                "PRIMARY KEY(order_id, product_id), " +
                "FOREIGN KEY(order_id) REFERENCES daily_orders(id) ON DELETE CASCADE, " +
                "FOREIGN KEY(product_id) REFERENCES products(id)" +
                ");");

        // Seed Initial Default Products (Removed "Others")
        seedDefaultProducts(db);

        // Seed Initial Retailers from Reference
        seedDefaultRetailers(db);
    }

    private void seedDefaultProducts(SQLiteDatabase db) {
        String[][] initialProducts = {
                {"Gold Coin", "🪙", "yellow", "15"},
                {"125 gm Milk", "🥛", "blue", "0"},
                {"Burger Bun", "🍔", "orange", "10"},
                {"Pav", "🍞", "purple", "0"},
                {"Maruti 500 Gram", "📦", "green", "25"},
                {"City Special", "🥛", "pink", "0"},
                {"200 Gram", "📦", "blue", "0"},
                {"Brown", "📦", "brown", "0"},
                {"Atta Bread", "🌾", "coral", "0"},
                {"Muffins", "🧁", "violet", "0"}
        };

        for (int i = 0; i < initialProducts.length; i++) {
            ContentValues cv = new ContentValues();
            String name = initialProducts[i][0];
            cv.put("cloud_id", com.orderanalyzer.model.DailyProduct.generateCloudId(name));
            cv.put("name", name);
            cv.put("is_active", 1);
            cv.put("sort_order", i + 1);
            cv.put("icon_key", initialProducts[i][1]);
            cv.put("color_key", initialProducts[i][2]);
            cv.put("price", Double.parseDouble(initialProducts[i][3]));
            db.insert("products", null, cv);
        }
    }

    private void seedDefaultRetailers(SQLiteDatabase db) {
        String[][] initialRetailers = {
                {"Rupal B Modi", "+91 98765 43210", "Shajapur, MP", "1,2,3,4"},
                {"Vrindawan Dudh Dairy", "+91 98260 11223", "Station Road", "1,2,4,5"},
                {"Sanideep Restourent", "+91 94250 88776", "Bus Stand Square", "3,4,6"},
                {"Shri Shankar Dairy", "+91 99812 33445", "Main Bazaar", "1,2,7"},
                {"New Krishna Sweets", "+91 98930 55667", "Gandhi Chowk", "2,4,9"},
                {"Nakoda Restaurant & Traders", "+91 97550 44332", "AB Road", "3,4,10"},
                {"Purva Kirana Store", "+91 91112 66778", "Mandi Gate", "1,4,8"},
                {"Mahir dairy", "+91 98270 99881", "Old Town", "2,5,6"},
                {"Tanuj Gandhi", "+91 96300 22114", "Civil Lines", "1,3,4"}
        };

        for (String[] r : initialRetailers) {
            ContentValues cv = new ContentValues();
            cv.put("cloud_id", com.orderanalyzer.model.DailyRetailer.generateCloudId(r[0]));
            cv.put("name", r[0]);
            cv.put("phone", r[1]);
            cv.put("address", r[2]);
            cv.put("preferred_product_ids", r[3]);
            cv.put("is_active", 1);
            db.insert("retailers", null, cv);
        }
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        // Ensure "Others" is removed if it previously existed
        try {
            db.delete("products", "name = 'Others'", null);
        } catch (Exception ignored) {}

        // Backfill cloud_id if null on upgrade
        try {
            android.database.Cursor cp = db.rawQuery("SELECT id, name FROM products WHERE cloud_id IS NULL OR cloud_id = ''", null);
            if (cp != null) {
                while (cp.moveToNext()) {
                    long id = cp.getLong(0);
                    String name = cp.getString(1);
                    ContentValues cv = new ContentValues();
                    cv.put("cloud_id", com.orderanalyzer.model.DailyProduct.generateCloudId(name));
                    db.update("products", cv, "id = ?", new String[]{String.valueOf(id)});
                }
                cp.close();
            }
        } catch (Exception ignored) {}

        try {
            android.database.Cursor cr = db.rawQuery("SELECT id, name FROM retailers WHERE cloud_id IS NULL OR cloud_id = ''", null);
            if (cr != null) {
                while (cr.moveToNext()) {
                    long id = cr.getLong(0);
                    String name = cr.getString(1);
                    ContentValues cv = new ContentValues();
                    cv.put("cloud_id", com.orderanalyzer.model.DailyRetailer.generateCloudId(name));
                    db.update("retailers", cv, "id = ?", new String[]{String.valueOf(id)});
                }
                cr.close();
            }
        } catch (Exception ignored) {}

        try {
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_products_cloud_id ON products(cloud_id)");
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_retailers_cloud_id ON retailers(cloud_id)");
        } catch (Exception ignored) {}
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            // Add price column to products table (non-destructive migration)
            try {
                db.execSQL("ALTER TABLE products ADD COLUMN price REAL NOT NULL DEFAULT 0");
            } catch (Exception ignored) {
                // Column may already exist if somehow re-run
            }
        }
        if (oldVersion < 3) {
            try {
                db.execSQL("ALTER TABLE products ADD COLUMN cloud_id TEXT");
            } catch (Exception ignored) {}
            try {
                db.execSQL("ALTER TABLE retailers ADD COLUMN cloud_id TEXT");
            } catch (Exception ignored) {}
        }
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }
}
