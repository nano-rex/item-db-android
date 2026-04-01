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
import java.util.Locale;

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
                        "(SELECT COUNT(*) FROM item_topics t WHERE t.item_id = i.id) " +
                        "FROM items i " +
                        "WHERE (? = '' OR i.title LIKE ? OR COALESCE(i.body, '') LIKE ? " +
                        "OR EXISTS (SELECT 1 FROM item_topics t2 WHERE t2.item_id = i.id AND t2.topic LIKE ?) " +
                        "OR EXISTS (SELECT 1 FROM item_rows r2 WHERE r2.item_id = i.id AND COALESCE(r2.progress_text, '') LIKE ?)) " +
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
                "SELECT id, price, location, entry_date, ranking, progress_text, sort_order FROM item_rows WHERE item_id = ? ORDER BY sort_order, id",
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
            row.progressText = rowCursor.getString(5);
            row.sortOrder = rowCursor.getInt(6);
            detail.rows.add(row);
        }
        rowCursor.close();

        detail.topics.addAll(readNamedEntries("item_topics", "topic", itemId));
        detail.item.rowCount = detail.rows.size();
        detail.item.topicCount = detail.topics.size();
        return detail;
    }

    public void addRow(long itemId, String priceText, String location, String entryDate, String ranking, String progressText) {
        ContentValues values = new ContentValues();
        values.put("item_id", itemId);
        Double price = parseDouble(priceText);
        if (price != null) values.put("price", price);
        values.put("location", emptyToNull(location));
        values.put("entry_date", emptyToNull(entryDate));
        values.put("ranking", emptyToNull(ranking));
        values.put("progress_text", emptyToNull(progressText));
        values.put("sort_order", nextSortOrder("item_rows", itemId));
        db().insertOrThrow("item_rows", null, values);
        touchItem(itemId);
    }

    public void addTopic(long itemId, String topic) {
        addNamedEntry("item_topics", "topic", itemId, topic);
    }

    public void deleteRow(long rowId) {
        long itemId = parentItemId("item_rows", rowId);
        db().delete("item_rows", "id = ?", new String[]{String.valueOf(rowId)});
        if (itemId > 0) touchItem(itemId);
    }

    public void deleteTopic(long entryId) {
        deleteNamedEntry("item_topics", entryId);
    }

    public List<String> listDistinctTopics() {
        ArrayList<String> result = new ArrayList<>();
        Cursor c = db().rawQuery("SELECT DISTINCT topic FROM item_topics WHERE TRIM(topic) <> '' ORDER BY topic COLLATE NOCASE", new String[]{});
        while (c.moveToNext()) {
            result.add(c.getString(0));
        }
        c.close();
        return result;
    }

    public List<String> listDistinctLocations() {
        ArrayList<String> result = new ArrayList<>();
        Cursor c = db().rawQuery("SELECT DISTINCT location FROM item_rows WHERE location IS NOT NULL AND TRIM(location) <> '' ORDER BY location COLLATE NOCASE", new String[]{});
        while (c.moveToNext()) {
            result.add(c.getString(0));
        }
        c.close();
        return result;
    }

    public String buildAnalysisReport(String topicsText, String locationText, boolean matchAllTopics) {
        List<String> topics = parseFilters(topicsText);
        String location = locationText == null ? "" : locationText.trim();
        List<ItemRecord> items = listItems("");
        StringBuilder out = new StringBuilder();
        out.append("Analysis\n");
        out.append("Topics: ").append(topics.isEmpty() ? "Any" : String.join(", ", topics)).append("\n");
        out.append("Topic mode: ").append(matchAllTopics ? "All selected topics" : "Any selected topic").append("\n");
        out.append("Location: ").append(location.isEmpty() ? "Any" : location).append("\n\n");

        int matchedItems = 0;
        int matchedRows = 0;
        int rowsWithProgress = 0;
        for (ItemRecord item : items) {
            ItemDetail detail = getItemDetail(item.id);
            if (!matchesTopics(detail, topics, matchAllTopics)) {
                continue;
            }

            List<ItemRowEntry> rows = new ArrayList<>();
            for (ItemRowEntry row : detail.rows) {
                if (location.isEmpty() || location.equalsIgnoreCase(safe(row.location))) {
                    rows.add(row);
                }
            }
            if (!location.isEmpty() && rows.isEmpty()) {
                continue;
            }

            matchedItems++;
            matchedRows += rows.size();

            out.append(item.title).append("\n");
            out.append("  Topics: ").append(joinValues(detail.topics)).append("\n");
            out.append("  Rows matched: ").append(rows.size()).append("\n");
            if (!rows.isEmpty()) {
                ItemRowEntry latest = rows.get(rows.size() - 1);
                out.append("  Latest price: ").append(latest.hasPrice ? formatPrice(latest.price) : "-").append("\n");
                out.append("  Latest rank: ").append(safe(latest.ranking)).append("\n");
                out.append("  Latest progress: ").append(safe(latest.progressText)).append("\n");
                out.append("  Latest location: ").append(safe(latest.location)).append("\n");
                out.append("  Latest date: ").append(safe(latest.entryDate)).append("\n");
                out.append("  Timeline:\n");
                for (ItemRowEntry row : rows) {
                    if (row.progressText != null && !row.progressText.trim().isEmpty()) {
                        rowsWithProgress++;
                    }
                    out.append("    - ")
                            .append(safe(row.entryDate))
                            .append(" | ")
                            .append(safe(row.location))
                            .append(" | price ")
                            .append(row.hasPrice ? formatPrice(row.price) : "-")
                            .append(" | rank ")
                            .append(safe(row.ranking))
                            .append(" | progress ")
                            .append(safe(row.progressText))
                            .append("\n");
                }
            }
            out.append("\n");
        }

        if (matchedItems == 0) {
            out.append("No matching items.");
        } else {
            out.append("Summary\n");
            out.append("Matched items: ").append(matchedItems).append("\n");
            out.append("Matched rows: ").append(matchedRows).append("\n");
            out.append("Rows with progress updates: ").append(rowsWithProgress).append("\n");
        }
        return out.toString();
    }

    public String exportJsonToFile() throws Exception {
        JSONObject root = new JSONObject();
        root.put("items", queryArray("SELECT id, title, body, created_at, updated_at FROM items ORDER BY id"));
        root.put("item_rows", queryArray("SELECT id, item_id, price, location, entry_date, ranking, progress_text, sort_order FROM item_rows ORDER BY item_id, sort_order, id"));
        root.put("item_topics", queryArray("SELECT id, item_id, topic, sort_order FROM item_topics ORDER BY item_id, sort_order, id"));

        File outDir = new File(context.getFilesDir(), "exports");
        if (!outDir.exists()) {
            outDir.mkdirs();
        }
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
                " topics=" + length(parsed, "item_topics");
    }

    public void importJson(String json, boolean replaceFirst) throws Exception {
        JSONObject parsed = new JSONObject(json);
        SQLiteDatabase db = db();
        db.beginTransaction();
        try {
            if (replaceFirst) {
                db.execSQL("DELETE FROM item_topics");
                db.execSQL("DELETE FROM item_rows");
                db.execSQL("DELETE FROM items");
            }
            insertArray(db, parsed.optJSONArray("items"), "INSERT OR IGNORE INTO items(id, title, body, created_at, updated_at) VALUES(?, ?, ?, ?, ?)", new String[]{"id", "title", "body", "created_at", "updated_at"});
            insertArray(db, parsed.optJSONArray("item_rows"), "INSERT OR IGNORE INTO item_rows(id, item_id, price, location, entry_date, ranking, progress_text, sort_order) VALUES(?, ?, ?, ?, ?, ?, ?, ?)", new String[]{"id", "item_id", "price", "location", "entry_date", "ranking", "progress_text", "sort_order"});
            insertArray(db, parsed.optJSONArray("item_topics"), "INSERT OR IGNORE INTO item_topics(id, item_id, topic, sort_order) VALUES(?, ?, ?, ?)", new String[]{"id", "item_id", "topic", "sort_order"});
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
        if (itemId > 0) {
            touchItem(itemId);
        }
    }

    private long parentItemId(String table, long entryId) {
        Cursor c = db().rawQuery("SELECT item_id FROM " + table + " WHERE id = ?", new String[]{String.valueOf(entryId)});
        long itemId = 0;
        if (c.moveToFirst()) {
            itemId = c.getLong(0);
        }
        c.close();
        return itemId;
    }

    private int nextSortOrder(String table, long itemId) {
        Cursor c = db().rawQuery("SELECT COALESCE(MAX(sort_order), -1) + 1 FROM " + table + " WHERE item_id = ?", new String[]{String.valueOf(itemId)});
        int value = 0;
        if (c.moveToFirst()) {
            value = c.getInt(0);
        }
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
                    if (type == Cursor.FIELD_TYPE_INTEGER) {
                        o.put(col, c.getLong(i));
                    } else if (type == Cursor.FIELD_TYPE_FLOAT) {
                        o.put(col, c.getDouble(i));
                    } else {
                        o.put(col, c.getString(i));
                    }
                }
            }
            arr.put(o);
        }
        c.close();
        return arr;
    }

    private void insertArray(SQLiteDatabase db, JSONArray arr, String sql, String[] keys) {
        if (arr == null) {
            return;
        }
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.optJSONObject(i);
            if (o == null) {
                continue;
            }
            Object[] args = new Object[keys.length];
            for (int k = 0; k < keys.length; k++) {
                Object value = o.opt(keys[k]);
                args[k] = value == JSONObject.NULL ? null : value;
            }
            db.execSQL(sql, args);
        }
    }

    private List<String> parseFilters(String raw) {
        ArrayList<String> result = new ArrayList<>();
        if (raw == null) {
            return result;
        }
        for (String part : raw.split(",")) {
            String value = part.trim();
            if (!value.isEmpty()) {
                result.add(value);
            }
        }
        return result;
    }

    private boolean matchesTopics(ItemDetail detail, List<String> filters, boolean matchAll) {
        if (filters.isEmpty()) {
            return true;
        }
        ArrayList<String> itemTopics = new ArrayList<>();
        for (NamedEntry entry : detail.topics) {
            itemTopics.add(entry.value == null ? "" : entry.value.trim().toLowerCase(Locale.US));
        }
        if (matchAll) {
            for (String filter : filters) {
                if (!itemTopics.contains(filter.toLowerCase(Locale.US))) {
                    return false;
                }
            }
            return true;
        }
        for (String filter : filters) {
            if (itemTopics.contains(filter.toLowerCase(Locale.US))) {
                return true;
            }
        }
        return false;
    }

    private String joinValues(List<NamedEntry> entries) {
        if (entries.isEmpty()) {
            return "-";
        }
        ArrayList<String> values = new ArrayList<>();
        for (NamedEntry entry : entries) {
            values.add(safe(entry.value));
        }
        return String.join(", ", values);
    }

    private String formatPrice(double value) {
        return String.format(Locale.US, "%.2f", value);
    }

    private Double parseDouble(String raw) {
        String value = raw == null ? "" : raw.trim();
        if (value.isEmpty()) {
            return null;
        }
        return Double.parseDouble(value);
    }

    private String safe(String value) {
        return value == null || value.trim().isEmpty() ? "-" : value.trim();
    }

    private String emptyToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }
}
