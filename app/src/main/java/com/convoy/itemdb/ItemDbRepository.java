package com.convoy.itemdb;

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

    public void addItem(String name, String description, String topicsCsv) {
        db().execSQL(
                "INSERT INTO items(name, description, created_at) VALUES(?, ?, ?)",
                new Object[]{name, emptyToNull(description), Instant.now().toString()}
        );
        int itemId = queryInt("SELECT id FROM items ORDER BY id DESC LIMIT 1", new String[]{});
        saveTopics(itemId, topicsCsv);
    }

    public void addHistory(int itemId, Double price, String date, String location) {
        db().execSQL(
                "INSERT INTO item_history(item_id, price, observed_date, location, note) VALUES(?, ?, ?, ?, ?)",
                new Object[]{itemId, price, emptyToNull(date), emptyToNull(location), null}
        );
    }

    public String listItems(String query, String sort) {
        String orderBy = "i.id DESC";
        if ("price".equalsIgnoreCase(sort)) orderBy = "COALESCE(h.price, 0) DESC";
        if ("location".equalsIgnoreCase(sort)) orderBy = "COALESCE(h.location, '') ASC";
        if ("topic".equalsIgnoreCase(sort)) orderBy = "COALESCE(t.name, '') ASC";

        String sql = "SELECT i.id, i.name, i.description, h.price, h.location, GROUP_CONCAT(DISTINCT t.name) AS topics " +
                "FROM items i " +
                "LEFT JOIN item_history h ON h.id = (SELECT h2.id FROM item_history h2 WHERE h2.item_id = i.id ORDER BY h2.id DESC LIMIT 1) " +
                "LEFT JOIN item_topics it ON it.item_id = i.id " +
                "LEFT JOIN topics t ON t.id = it.topic_id " +
                "WHERE (? = '' OR i.name LIKE ? OR COALESCE(i.description, '') LIKE ? OR COALESCE(h.location, '') LIKE ? OR COALESCE(t.name, '') LIKE ?) " +
                "GROUP BY i.id " +
                "ORDER BY " + orderBy;

        String like = "%" + (query == null ? "" : query.trim()) + "%";
        Cursor c = db().rawQuery(sql, new String[]{query == null ? "" : query.trim(), like, like, like, like});
        StringBuilder out = new StringBuilder();
        out.append("ID | Name | Topics | LastPrice | LastLocation\n");
        while (c.moveToNext()) {
            out.append(c.getInt(0)).append(" | ")
                    .append(c.getString(1)).append(" | ")
                    .append(nullToDash(c.getString(5))).append(" | ")
                    .append(c.isNull(3) ? "-" : String.format("%.2f", c.getDouble(3))).append(" | ")
                    .append(nullToDash(c.getString(4))).append("\n");
        }
        c.close();
        return out.toString();
    }

    public List<String> itemChoices() {
        ArrayList<String> result = new ArrayList<>();
        Cursor c = db().rawQuery("SELECT id, name FROM items ORDER BY id DESC", new String[]{});
        while (c.moveToNext()) {
            result.add(c.getInt(0) + " - " + c.getString(1));
        }
        c.close();
        return result;
    }

    public String exportJsonToFile() throws Exception {
        JSONObject root = new JSONObject();
        root.put("items", queryArray("SELECT id, name, description, created_at FROM items ORDER BY id"));
        root.put("topics", queryArray("SELECT id, name FROM topics ORDER BY id"));
        root.put("item_topics", queryArray("SELECT item_id, topic_id FROM item_topics ORDER BY item_id, topic_id"));
        root.put("item_history", queryArray("SELECT id, item_id, price, observed_date, location, note FROM item_history ORDER BY id"));

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
        int itemCount = parsed.optJSONArray("items") != null ? parsed.optJSONArray("items").length() : 0;
        int topicCount = parsed.optJSONArray("topics") != null ? parsed.optJSONArray("topics").length() : 0;
        int historyCount = parsed.optJSONArray("item_history") != null ? parsed.optJSONArray("item_history").length() : 0;
        return "Import preview ready. items=" + itemCount + " topics=" + topicCount + " history=" + historyCount;
    }

    public void importJson(String json, boolean replaceFirst) throws Exception {
        JSONObject parsed = new JSONObject(json);
        SQLiteDatabase db = db();
        db.beginTransaction();
        try {
            if (replaceFirst) {
                db.execSQL("DELETE FROM item_history");
                db.execSQL("DELETE FROM item_topics");
                db.execSQL("DELETE FROM topics");
                db.execSQL("DELETE FROM items");
            }
            insertArray(db, parsed.optJSONArray("items"), "INSERT OR IGNORE INTO items(id, name, description, created_at) VALUES(?, ?, ?, ?)", new String[]{"id", "name", "description", "created_at"});
            insertArray(db, parsed.optJSONArray("topics"), "INSERT OR IGNORE INTO topics(id, name) VALUES(?, ?)", new String[]{"id", "name"});
            insertArray(db, parsed.optJSONArray("item_topics"), "INSERT OR IGNORE INTO item_topics(item_id, topic_id) VALUES(?, ?)", new String[]{"item_id", "topic_id"});
            insertArray(db, parsed.optJSONArray("item_history"), "INSERT OR IGNORE INTO item_history(id, item_id, price, observed_date, location, note) VALUES(?, ?, ?, ?, ?, ?)", new String[]{"id", "item_id", "price", "observed_date", "location", "note"});
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public String renderGraph(int itemId) {
        Cursor c = db().rawQuery("SELECT COALESCE(price, 0) FROM item_history WHERE item_id = ? ORDER BY id", new String[]{String.valueOf(itemId)});
        List<Double> values = new ArrayList<>();
        while (c.moveToNext()) values.add(c.getDouble(0));
        c.close();
        if (values.isEmpty()) {
            return "No history values for item " + itemId;
        }
        double min = values.stream().mapToDouble(v -> v).min().orElse(0);
        double max = values.stream().mapToDouble(v -> v).max().orElse(0);
        double span = Math.max(0.0001, max - min);
        StringBuilder sb = new StringBuilder();
        sb.append("Price graph for item ").append(itemId).append("\n");
        sb.append("min=").append(String.format("%.2f", min)).append(" max=").append(String.format("%.2f", max)).append("\n");
        for (double v : values) {
            int bars = (int) Math.round(((v - min) / span) * 30);
            sb.append(String.format("%8.2f | ", v));
            for (int i = 0; i < bars; i++) sb.append('#');
            sb.append('\n');
        }
        return sb.toString();
    }

    private void saveTopics(int itemId, String topicsCsv) {
        String[] topics = topicsCsv == null ? new String[]{} : topicsCsv.split(",");
        for (String topicRaw : topics) {
            String topic = topicRaw.trim();
            if (topic.isEmpty()) continue;
            db().execSQL("INSERT OR IGNORE INTO topics(name) VALUES(?)", new Object[]{topic});
            int topicId = queryInt("SELECT id FROM topics WHERE name = ?", new String[]{topic});
            db().execSQL("INSERT OR IGNORE INTO item_topics(item_id, topic_id) VALUES(?, ?)", new Object[]{itemId, topicId});
        }
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

    private int queryInt(String sql, String[] args) {
        Cursor c = db().rawQuery(sql, args);
        int value = 0;
        if (c.moveToFirst()) value = c.getInt(0);
        c.close();
        return value;
    }

    private String nullToDash(String value) {
        return value == null || value.trim().isEmpty() ? "-" : value;
    }

    private String emptyToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }
}
