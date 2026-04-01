package com.convoy.itemdb;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.btnOpenItems).setOnClickListener(v -> startActivity(new Intent(this, ItemsActivity.class)));
        findViewById(R.id.btnOpenHistory).setOnClickListener(v -> startActivity(new Intent(this, HistoryActivity.class)));
        findViewById(R.id.btnOpenSearch).setOnClickListener(v -> startActivity(new Intent(this, SearchActivity.class)));
        findViewById(R.id.btnOpenImportExport).setOnClickListener(v -> startActivity(new Intent(this, ImportExportActivity.class)));
        findViewById(R.id.btnOpenGraph).setOnClickListener(v -> startActivity(new Intent(this, GraphActivity.class)));
    }
}
