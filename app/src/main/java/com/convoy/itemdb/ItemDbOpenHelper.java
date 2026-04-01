package com.convoy.itemdb;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class ItemDbOpenHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "itemdb.db";
    private static final int DB_VERSION = 4;

    public ItemDbOpenHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createSchema(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 4) {
            db.execSQL("DROP TABLE IF EXISTS item_progress");
            db.execSQL("DROP TABLE IF EXISTS item_topics");
            db.execSQL("DROP TABLE IF EXISTS item_rows");
            db.execSQL("DROP TABLE IF EXISTS item_history");
            db.execSQL("DROP TABLE IF EXISTS topics");
            db.execSQL("DROP TABLE IF EXISTS items");
            db.execSQL("DROP TABLE IF EXISTS item_groups");
            createSchema(db);
        }
    }

    private void createSchema(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE items (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "title TEXT NOT NULL, " +
                "body TEXT, " +
                "created_at TEXT NOT NULL, " +
                "updated_at TEXT NOT NULL" +
                ");");

        db.execSQL("CREATE TABLE item_rows (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "item_id INTEGER NOT NULL, " +
                "price REAL, " +
                "location TEXT, " +
                "entry_date TEXT, " +
                "ranking TEXT, " +
                "sort_order INTEGER NOT NULL DEFAULT 0, " +
                "FOREIGN KEY(item_id) REFERENCES items(id) ON DELETE CASCADE" +
                ");");

        db.execSQL("CREATE TABLE item_topics (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "item_id INTEGER NOT NULL, " +
                "topic TEXT NOT NULL, " +
                "sort_order INTEGER NOT NULL DEFAULT 0, " +
                "FOREIGN KEY(item_id) REFERENCES items(id) ON DELETE CASCADE" +
                ");");

        db.execSQL("CREATE TABLE item_progress (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "item_id INTEGER NOT NULL, " +
                "progress_text TEXT NOT NULL, " +
                "sort_order INTEGER NOT NULL DEFAULT 0, " +
                "FOREIGN KEY(item_id) REFERENCES items(id) ON DELETE CASCADE" +
                ");");

        db.execSQL("CREATE INDEX idx_items_title ON items(title);");
        db.execSQL("CREATE INDEX idx_item_rows_item ON item_rows(item_id);");
        db.execSQL("CREATE INDEX idx_item_topics_item ON item_topics(item_id);");
        db.execSQL("CREATE INDEX idx_item_progress_item ON item_progress(item_id);");
    }
}
