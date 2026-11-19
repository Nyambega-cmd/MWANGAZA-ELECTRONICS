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
    }

    public DbHelper(Context c) {
        super(c, "mwangaza.db", null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE items (_id INTEGER PRIMARY KEY AUTOINCREMENT, item TEXT NOT NULL, brand TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldV, int newV) {
        db.execSQL("DROP TABLE IF EXISTS items");
        onCreate(db);
    }

    public void add(String item, String brand) {
        ContentValues v = new ContentValues();
        v.put("item", item);
        v.put("brand", brand);
        getWritableDatabase().insert("items", null, v);
    }

    public void update(long id, String item, String brand) {
        ContentValues v = new ContentValues();
        v.put("item", item);
        v.put("brand", brand);
        getWritableDatabase().update("items", v, "_id=?", new String[]{String.valueOf(id)});
    }

    public void delete(long id) {
        getWritableDatabase().delete("items", "_id=?", new String[]{String.valueOf(id)});
    }

    public void deleteAll() {
        getWritableDatabase().delete("items", null, null);
    }

    public List<Item> search(String q) {
        List<Item> list = new ArrayList<>();
        Cursor c;
        if (q == null || q.isEmpty()) {
            c = getReadableDatabase().rawQuery(
                    "SELECT _id, item, brand FROM items ORDER BY _id ASC", null);
        } else {
            String like = "%" + q + "%";
            c = getReadableDatabase().rawQuery(
                    "SELECT _id, item, brand FROM items WHERE item LIKE ? OR brand LIKE ? ORDER BY _id ASC",
                    new String[]{like, like});
        }
        while (c.moveToNext()) {
            Item i = new Item();
            i.id = c.getLong(0);
            i.item = c.getString(1);
            i.brand = c.getString(2);
            list.add(i);
        }
        c.close();
        return list;
    }
}
