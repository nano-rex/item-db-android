package com.convoy.itemdb;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

public class SearchActivity extends AppCompatActivity {
    private ItemDbRepository repository;
    private ItemListAdapter adapter;
    private EditText etSearch;
    private TextView tvEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        repository = new ItemDbRepository(this);
        etSearch = findViewById(R.id.etSearch);
        tvEmpty = findViewById(R.id.tvEmpty);
        ListView listView = findViewById(R.id.lvResults);
        adapter = new ItemListAdapter(this, item -> {
            Intent intent = new Intent(this, ItemEditorActivity.class);
            intent.putExtra("item_id", item.id);
            startActivity(intent);
        }, item -> {
            repository.deleteItem(item.id);
            refresh();
        });
        listView.setAdapter(adapter);
        findViewById(R.id.btnRunSearch).setOnClickListener(v -> refresh());
        refresh();
    }

    private void refresh() {
        List<ItemRecord> items = repository.listItems(etSearch.getText().toString());
        adapter.setItems(items);
        tvEmpty.setText(items.isEmpty() ? "No matching notes." : "");
    }
}
