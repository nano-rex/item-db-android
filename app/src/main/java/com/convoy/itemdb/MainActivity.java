package com.convoy.itemdb;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    private ItemDbRepository repository;
    private ItemListAdapter adapter;
    private EditText etQuickSearch;
    private TextView tvEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        repository = new ItemDbRepository(this);
        etQuickSearch = findViewById(R.id.etQuickSearch);
        tvEmpty = findViewById(R.id.tvEmpty);
        ListView listView = findViewById(R.id.lvItems);
        adapter = new ItemListAdapter(this, item -> openEditor(item.id), item -> {
            repository.deleteItem(item.id);
            refresh();
        });
        listView.setAdapter(adapter);

        findViewById(R.id.btnNewItem).setOnClickListener(v -> openEditor(0));
        findViewById(R.id.btnSearch).setOnClickListener(v -> openSearch());
        findViewById(R.id.btnAnalysis).setOnClickListener(v -> openSearch());
        findViewById(R.id.btnSettings).setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        findViewById(R.id.btnFilter).setOnClickListener(v -> openSearch());
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        List<ItemRecord> items = repository.listItems(etQuickSearch.getText().toString());
        adapter.setItems(items);
        tvEmpty.setText(items.isEmpty() ? "No items yet. Create one from the top bar." : "");
    }

    private void openEditor(long itemId) {
        Intent intent = new Intent(this, ItemEditorActivity.class);
        intent.putExtra("item_id", itemId);
        startActivity(intent);
    }

    private void openSearch() {
        Intent intent = new Intent(this, SearchActivity.class);
        intent.putExtra("query", etQuickSearch.getText().toString());
        startActivity(intent);
    }
}
