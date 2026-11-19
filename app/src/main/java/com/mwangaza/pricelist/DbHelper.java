package com.mwangaza.pricelist;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

/** Stores items permanently on the phone (SQLite). */
public class DbHelper extends SQLiteOpenHelper {

    public static class Item {
        public long id;
        public String item;
        public String brand;
        public int quantity;
        public double wholesale;
        public double retail;
    }

    public DbHelper(Context c) {
        super(c, "mwangaza.db", null, 2);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE items (_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "item TEXT NOT NULL, brand TEXT, "
                + "quantity INTEGER DEFAULT 0, wholesale REAL DEFAULT 0, retail REAL DEFAULT 0)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldV, int newV) {
        if (oldV < 2) {
            // keep existing items, add the new columns
            db.execSQL("ALTER TABLE items ADD COLUMN quantity INTEGER DEFAULT 0");
            db.execSQL("ALTER TABLE items ADD COLUMN wholesale REAL DEFAULT 0");
            db.execSQL("ALTER TABLE items ADD COLUMN retail REAL DEFAULT 0");
        }
    }

    private ContentValues values(String item, String brand, int qty, double wholesale, double retail) {
        ContentValues v = new ContentValues();
        v.put("item", item);
        v.put("brand", brand);
        v.put("quantity", qty);
        v.put("wholesale", wholesale);
        v.put("retail", retail);
        return v;
    }

    public void add(String item, String brand, int qty, double wholesale, double retail) {
        getWritableDatabase().insert("items", null, values(item, brand, qty, wholesale, retail));
    }

    public void update(long id, String item, String brand, int qty, double wholesale, double retail) {
        getWritableDatabase().update("items", values(item, brand, qty, wholesale, retail),
                "_id=?", new String[]{String.valueOf(id)});
    }

    public void delete(long id) {
        getWritableDatabase().delete("items", "_id=?", new String[]{String.valueOf(id)});
    }

    public void deleteAll() {
        getWritableDatabase().delete("items", null, null);
    }

    public List<Item> search(String q) {
        List<Item> list = new ArrayList<>();
        String cols = "SELECT _id, item, brand, quantity, wholesale, retail FROM items ";
        Cursor c;
        if (q == null || q.isEmpty()) {
            c = getReadableDatabase().rawQuery(cols + "ORDER BY _id ASC", null);
        } else {
            String like = "%" + q + "%";
            c = getReadableDatabase().rawQuery(
                    cols + "WHERE item LIKE ? OR brand LIKE ? ORDER BY _id ASC",
                    new String[]{like, like});
        }
        while (c.moveToNext()) {
            Item i = new Item();
            i.id = c.getLong(0);
            i.item = c.getString(1);
            i.brand = c.getString(2);
            i.quantity = c.getInt(3);
            i.wholesale = c.getDouble(4);
            i.retail = c.getDouble(5);
            list.add(i);
        }
        c.close();
        return list;
    }
}
