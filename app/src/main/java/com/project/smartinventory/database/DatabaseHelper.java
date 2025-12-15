package com.project.smartinventory.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

import com.project.smartinventory.InventoryItem;

import java.util.ArrayList;
import java.util.List;

/**
 * DatabaseHelper
 *
 * Thin SQLiteOpenHelper managing two tables:
 *  - users(username,password)   → simple demo auth (plaintext; DO NOT use in production)
 *  - inventory(name,desc,cat,qty,price)
 *
 * Responsibilities:
 *  - Create/upgrade schema.
 *  - Provide simple CRUD for users and inventory.
 *
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    // ---- DB Meta ----
    private static final String DB_NAME    = "inventoryApp.db";
    private static final int    DB_VERSION = 1;

    // ---- users table ----
    private static final String T_USERS     = "users";
    private static final String U_ID        = "id";
    private static final String U_USERNAME  = "username";
    private static final String U_PASSWORD  = "password";

    // ---- inventory table ----
    private static final String T_INV   = "inventory";
    private static final String I_ID    = "id";
    private static final String I_NAME  = "name";
    private static final String I_DESC  = "description";
    private static final String I_CAT   = "category";
    private static final String I_QTY   = "quantity";
    private static final String I_PRICE = "price";

    // ---- SQL (schema) ----
    private static final String SQL_CREATE_USERS =
            "CREATE TABLE " + T_USERS + " (" +
                    U_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    U_USERNAME + " TEXT UNIQUE, " +
                    U_PASSWORD + " TEXT" + // DEMO ONLY: hash in real apps
                    ")";

    private static final String SQL_CREATE_INVENTORY =
            "CREATE TABLE " + T_INV + " (" +
                    I_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    I_NAME + " TEXT, " +
                    I_DESC + " TEXT, " +
                    I_CAT + " TEXT, " +
                    I_QTY + " INTEGER, " +
                    I_PRICE + " REAL" +
                    ")";

    public DatabaseHelper(@Nullable Context ctx) {
        super(ctx, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_USERS);
        db.execSQL(SQL_CREATE_INVENTORY);
        // Consider adding indices if data grows:
        // db.execSQL("CREATE INDEX idx_inventory_name ON " + T_INV + "(" + I_NAME + ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldV, int newV) {
        // Simple destructive migration (OK for demos)
        db.execSQL("DROP TABLE IF EXISTS " + T_USERS);
        db.execSQL("DROP TABLE IF EXISTS " + T_INV);
        onCreate(db);
    }

    // ------------------------------------------------------------------------
    // Auth (demo-level; plaintext passwords)
    // ------------------------------------------------------------------------

    /**
     * @return true if a row exists with the given username+password.
     *         (Passwords are plaintext here for simplicity; do NOT ship like this.)
     */
    public boolean userExists(String username, String password) {
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor c = db.query(
                T_USERS,
                new String[]{U_ID},
                U_USERNAME + "=? AND " + U_PASSWORD + "=?",
                new String[]{username, password},
                null, null, null
        )) {
            return c.moveToFirst();
        }
    }

    /** @return true if a user with the given username already exists. */
    public boolean usernameTaken(String username) {
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor c = db.query(
                T_USERS,
                new String[]{U_ID},
                U_USERNAME + "=?",
                new String[]{username},
                null, null, null
        )) {
            return c.moveToFirst();
        }
    }

    /**
     * Inserts a new user.
     * @return true if insert succeeded.
     */
    public boolean registerUser(String username, String password) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put(U_USERNAME, username);
        v.put(U_PASSWORD, password); // DEMO ONLY: hash in real apps
        return db.insert(T_USERS, null, v) != -1;
    }

    // ------------------------------------------------------------------------
    // Inventory CRUD
    // ------------------------------------------------------------------------

    /** Inserts an inventory item. @return rowId (or -1 on failure). */
    public long addItem(InventoryItem it) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put(I_NAME,  it.getName());
        v.put(I_DESC,  it.getDescription());
        v.put(I_CAT,   it.getCategory());
        v.put(I_QTY,   it.getQuantity());
        v.put(I_PRICE, it.getPrice());
        return db.insert(T_INV, null, v);
    }

    /** Updates an inventory item by id. @return true if any row was updated. */
    public boolean updateItem(InventoryItem it) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put(I_NAME,  it.getName());
        v.put(I_DESC,  it.getDescription());
        v.put(I_CAT,   it.getCategory());
        v.put(I_QTY,   it.getQuantity());
        v.put(I_PRICE, it.getPrice());
        int rows = db.update(T_INV, v, I_ID + "=?", new String[]{String.valueOf(it.getId())});
        return rows > 0;
    }

    /** Deletes an inventory item by id. @return true if any row was deleted. */
    public boolean deleteItem(int id) {
        SQLiteDatabase db = getWritableDatabase();
        return db.delete(T_INV, I_ID + "=?", new String[]{String.valueOf(id)}) > 0;
    }

    /** @return all items ordered by name ASC. */
    public List<InventoryItem> getAllItems() {
        ArrayList<InventoryItem> res = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor c = db.query(
                T_INV,
                null,
                null, null, null, null,
                I_NAME + " ASC"
        )) {
            if (c.moveToFirst()) {
                int idIdx = c.getColumnIndexOrThrow(I_ID);
                int nIdx  = c.getColumnIndexOrThrow(I_NAME);
                int dIdx  = c.getColumnIndexOrThrow(I_DESC);
                int cIdx  = c.getColumnIndexOrThrow(I_CAT);
                int qIdx  = c.getColumnIndexOrThrow(I_QTY);
                int pIdx  = c.getColumnIndexOrThrow(I_PRICE);
                do {
                    InventoryItem it = new InventoryItem();
                    it.setId(c.getInt(idIdx));
                    it.setName(c.getString(nIdx));
                    it.setDescription(c.getString(dIdx));
                    it.setCategory(c.getString(cIdx));
                    it.setQuantity(c.getInt(qIdx));
                    it.setPrice(c.getDouble(pIdx));
                    res.add(it);
                } while (c.moveToNext());
            }
        }
        return res;
    }

    // ------------------------------------------------------------------------
    // Optional: convenience getters (handy for edit flows)
    // ------------------------------------------------------------------------

    /** Fetches a single item by id, or null if not found. */
    public InventoryItem getItemById(int id) {
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor c = db.query(
                T_INV,
                null,
                I_ID + "=?",
                new String[]{String.valueOf(id)},
                null, null, null
        )) {
            if (c.moveToFirst()) {
                InventoryItem it = new InventoryItem();
                it.setId(c.getInt(c.getColumnIndexOrThrow(I_ID)));
                it.setName(c.getString(c.getColumnIndexOrThrow(I_NAME)));
                it.setDescription(c.getString(c.getColumnIndexOrThrow(I_DESC)));
                it.setCategory(c.getString(c.getColumnIndexOrThrow(I_CAT)));
                it.setQuantity(c.getInt(c.getColumnIndexOrThrow(I_QTY)));
                it.setPrice(c.getDouble(c.getColumnIndexOrThrow(I_PRICE)));
                return it;
            }
        }
        return null;
    }
}
