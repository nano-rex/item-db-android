package com.convoy.itemdb;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class ItemDbRepository {
    private final Context context;
    private final ItemDbOpenHelper helper;

    public ItemDbRepository(Context context) {
        this.context = context.getApplicationContext();
        this.helper = new ItemDbOpenHelper(this.context);
    }

    private SQLiteDatabase db() {
        return helper.getWritableDatabase();
    }

    public long createItem(String title, String body) {
        String now = Instant.now().toString();
        ContentValues values = new ContentValues();
        values.put("title", title.trim());
        values.put("body", emptyToNull(body));
        values.put("created_at", now);
        values.put("updated_at", now);
        return db().insertOrThrow("items", null, values);
    }

    public void updateItem(long itemId, String title, String body) {
        ContentValues values = new ContentValues();
        values.put("title", title.trim());
        values.put("body", emptyToNull(body));
        values.put("updated_at", Instant.now().toString());
        db().update("items", values, "id = ?", new String[]{String.valueOf(itemId)});
    }

    public void deleteItem(long itemId) {
        db().delete("items", "id = ?", new String[]{String.valueOf(itemId)});
    }

    public List<ItemRecord> listItems(String query) {
        ArrayList<ItemRecord> result = new ArrayList<>();
        String term = query == null ? "" : query.trim();
        String like = "%" + term + "%";
        Cursor c = db().rawQuery(
                "SELECT i.id, i.title, i.body, i.created_at, i.updated_at, " +
                        "(SELECT COUNT(*) FROM item_rows r WHERE r.item_id = i.id), " +
                        "(SELECT COUNT(*) FROM item_topics t WHERE t.item_id = i.id), " +
                        "(SELECT COUNT(*) FROM item_progress p WHERE p.item_id = i.id) " +
                        "FROM items i " +
                        "WHERE (? = '' OR i.title LIKE ? OR COALESCE(i.body, '') LIKE ? " +
                        "OR EXISTS (SELECT 1 FROM item_topics t2 WHERE t2.item_id = i.id AND t2.topic LIKE ?) " +
                        "OR EXISTS (SELECT 1 FROM item_progress p2 WHERE p2.item_id = i.id AND p2.progress_text LIKE ?)) " +
                        "ORDER BY i.updated_at DESC, i.id DESC",
                new String[]{term, like, like, like, like}
        );
        while (c.moveToNext()) {
            ItemRecord item = new ItemRecord();
            item.id = c.getLong(0);
            item.title = c.getString(1);
            item.body = c.getString(2);
            item.createdAt = c.getString(3);
            item.updatedAt = c.getString(4);
            item.rowCount = c.getInt(5);
            item.topicCount = c.getInt(6);
            item.progressCount = c.getInt(7);
            result.add(item);
        }
        c.close();
        return result;
    }

    public ItemDetail getItemDetail(long itemId) {
        ItemDetail detail = new ItemDetail();
        Cursor itemCursor = db().rawQuery(
                "SELECT id, title, body, created_at, updated_at FROM items WHERE id = ?",
                new String[]{String.valueOf(itemId)}
        );
        if (itemCursor.moveToFirst()) {
            detail.item.id = itemCursor.getLong(0);
            detail.item.title = itemCursor.getString(1);
            detail.item.body = itemCursor.getString(2);
            detail.item.createdAt = itemCursor.getString(3);
            detail.item.updatedAt = itemCursor.getString(4);
        }
        itemCursor.close();

        Cursor rowCursor = db().rawQuery(
                "SELECT id, price, location, entry_date, ranking, sort_order FROM item_rows WHERE item_id = ? ORDER BY sort_order, id",
                new String[]{String.valueOf(itemId)}
        );
        while (rowCursor.moveToNext()) {
            ItemRowEntry row = new ItemRowEntry();
            row.id = rowCursor.getLong(0);
            row.hasPrice = !rowCursor.isNull(1);
            row.price = rowCursor.isNull(1) ? 0 : rowCursor.getDouble(1);
            row.location = rowCursor.getString(2);
            row.entryDate = rowCursor.getString(3);
            row.ranking = rowCursor.getString(4);
            row.sortOrder = rowCursor.getInt(5);
            detail.rows.add(row);
        }
        rowCursor.close();

        detail.topics.addAll(readNamedEntries("item_topics", "topic", itemId));
        detail.progressEntries.addAll(readNamedEntries("item_progress", "progress_text", itemId));
        detail.item.rowCount = detail.rows.size();
        detail.item.topicCount = detail.topics.size();
        detail.item.progressCount = detail.progressEntries.size();
        return detail;
    }

    public void addRow(long itemId, String priceText, String location, String entryDate, String ranking) {
        ContentValues values = new ContentValues();
        values.put("item_id", itemId);
        Double price = parseDouble(priceText);
        if (price != null) values.put("price", price);
        values.put("location", emptyToNull(location));
        values.put("entry_date", emptyToNull(entryDate));
        values.put("ranking", emptyToNull(ranking));
        values.put("sort_order", nextSortOrder("item_rows", itemId));
        db().insertOrThrow("item_rows", null, values);
        touchItem(itemId);
    }

    public void addTopic(long itemId, String topic) {
        addNamedEntry("item_topics", "topic", itemId, topic);
    }

    public void addProgress(long itemId, String progressText) {
        addNamedEntry("item_progress", "progress_text", itemId, progressText);
    }

    public void deleteRow(long rowId) {
        long itemId = parentItemId("item_rows", rowId);
        db().delete("item_rows", "id = ?", new String[]{String.valueOf(rowId)});
        if (itemId > 0) touchItem(itemId);
    }

    public void deleteTopic(long entryId) {
        deleteNamedEntry("item_topics", entryId);
    }

    public void deleteProgress(long entryId) {
        deleteNamedEntry("item_progress", entryId);
    }

    public String exportJsonToFile() throws Exception {
        JSONObject root = new JSONObject();
        root.put("items", queryArray("SELECT id, title, body, created_at, updated_at FROM items ORDER BY id"));
        root.put("item_rows", queryArray("SELECT id, item_id, price, location, entry_date, ranking, sort_order FROM item_rows ORDER BY item_id, sort_order, id"));
        root.put("item_topics", queryArray("SELECT id, item_id, topic, sort_order FROM item_topics ORDER BY item_id, sort_order, id"));
        root.put("item_progress", queryArray("SELECT id, item_id, progress_text, sort_order FROM item_progress ORDER BY item_id, sort_order, id"));

        File outDir = new File(context.getFilesDir(), "exports");
        if (!outDir.exists()) outDir.mkdirs();
        File outFile = new File(outDir, "itemdb_export.json");
        try (FileOutputStream fos = new FileOutputStream(outFile)) {
            fos.write(root.toString(2).getBytes(StandardCharsets.UTF_8));
        }
        return outFile.getAbsolutePath() + "\n\n" + root.toString(2);
    }

    public String previewImport(String json) throws Exception {
        JSONObject parsed = new JSONObject(json);
        return "Import preview ready. items=" + length(parsed, "items") +
                " rows=" + length(parsed, "item_rows") +
                " topics=" + length(parsed, "item_topics") +
                " progress=" + length(parsed, "item_progress");
    }

    public void importJson(String json, boolean replaceFirst) throws Exception {
        JSONObject parsed = new JSONObject(json);
        SQLiteDatabase db = db();
        db.beginTransaction();
        try {
            if (replaceFirst) {
                db.execSQL("DELETE FROM item_progress");
                db.execSQL("DELETE FROM item_topics");
                db.execSQL("DELETE FROM item_rows");
                db.execSQL("DELETE FROM items");
            }
            insertArray(db, parsed.optJSONArray("items"), "INSERT OR IGNORE INTO items(id, title, body, created_at, updated_at) VALUES(?, ?, ?, ?, ?)", new String[]{"id", "title", "body", "created_at", "updated_at"});
            insertArray(db, parsed.optJSONArray("item_rows"), "INSERT OR IGNORE INTO item_rows(id, item_id, price, location, entry_date, ranking, sort_order) VALUES(?, ?, ?, ?, ?, ?, ?)", new String[]{"id", "item_id", "price", "location", "entry_date", "ranking", "sort_order"});
            insertArray(db, parsed.optJSONArray("item_topics"), "INSERT OR IGNORE INTO item_topics(id, item_id, topic, sort_order) VALUES(?, ?, ?, ?)", new String[]{"id", "item_id", "topic", "sort_order"});
            insertArray(db, parsed.optJSONArray("item_progress"), "INSERT OR IGNORE INTO item_progress(id, item_id, progress_text, sort_order) VALUES(?, ?, ?, ?)", new String[]{"id", "item_id", "progress_text", "sort_order"});
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    private List<NamedEntry> readNamedEntries(String table, String valueColumn, long itemId) {
        ArrayList<NamedEntry> result = new ArrayList<>();
        Cursor c = db().rawQuery(
                "SELECT id, " + valueColumn + ", sort_order FROM " + table + " WHERE item_id = ? ORDER BY sort_order, id",
                new String[]{String.valueOf(itemId)}
        );
        while (c.moveToNext()) {
            NamedEntry entry = new NamedEntry();
            entry.id = c.getLong(0);
            entry.value = c.getString(1);
            entry.sortOrder = c.getInt(2);
            result.add(entry);
        }
        c.close();
        return result;
    }

    private void addNamedEntry(String table, String valueColumn, long itemId, String value) {
        ContentValues values = new ContentValues();
        values.put("item_id", itemId);
        values.put(valueColumn, value.trim());
        values.put("sort_order", nextSortOrder(table, itemId));
        db().insertOrThrow(table, null, values);
        touchItem(itemId);
    }

    private void deleteNamedEntry(String table, long entryId) {
        long itemId = parentItemId(table, entryId);
        db().delete(table, "id = ?", new String[]{String.valueOf(entryId)});
        if (itemId > 0) touchItem(itemId);
    }

    private long parentItemId(String table, long entryId) {
        Cursor c = db().rawQuery("SELECT item_id FROM " + table + " WHERE id = ?", new String[]{String.valueOf(entryId)});
        long itemId = 0;
        if (c.moveToFirst()) itemId = c.getLong(0);
        c.close();
        return itemId;
    }

    private int nextSortOrder(String table, long itemId) {
        Cursor c = db().rawQuery("SELECT COALESCE(MAX(sort_order), -1) + 1 FROM " + table + " WHERE item_id = ?", new String[]{String.valueOf(itemId)});
        int value = 0;
        if (c.moveToFirst()) value = c.getInt(0);
        c.close();
        return value;
    }

    private void touchItem(long itemId) {
        ContentValues values = new ContentValues();
        values.put("updated_at", Instant.now().toString());
        db().update("items", values, "id = ?", new String[]{String.valueOf(itemId)});
    }

    private int length(JSONObject object, String key) {
        JSONArray array = object.optJSONArray(key);
        return array == null ? 0 : array.length();
    }

    private JSONArray queryArray(String sql) throws Exception {
        Cursor c = db().rawQuery(sql, new String[]{});
        JSONArray arr = new JSONArray();
        while (c.moveToNext()) {
            JSONObject o = new JSONObject();
            for (int i = 0; i < c.getColumnCount(); i++) {
                String col = c.getColumnName(i);
                if (c.isNull(i)) {
                    o.put(col, JSONObject.NULL);
                } else {
                    int type = c.getType(i);
                    if (type == Cursor.FIELD_TYPE_INTEGER) o.put(col, c.getLong(i));
                    else if (type == Cursor.FIELD_TYPE_FLOAT) o.put(col, c.getDouble(i));
                    else o.put(col, c.getString(i));
                }
            }
            arr.put(o);
        }
        c.close();
        return arr;
    }

    private void insertArray(SQLiteDatabase db, JSONArray arr, String sql, String[] keys) {
        if (arr == null) return;
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.optJSONObject(i);
            if (o == null) continue;
            Object[] args = new Object[keys.length];
            for (int k = 0; k < keys.length; k++) {
                Object value = o.opt(keys[k]);
                args[k] = value == JSONObject.NULL ? null : value;
            }
            db.execSQL(sql, args);
        }
    }

    private Double parseDouble(String raw) {
        String value = raw == null ? "" : raw.trim();
        if (value.isEmpty()) return null;
        return Double.parseDouble(value);
    }

    private String emptyToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }
}
